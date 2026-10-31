"""Libreria per generare le mesh 3D di The Gods.

Due modi di costruire geometria:
  * SDF + marching cubes: forme organiche e lisce (corpo del Titano, cranio del Leviatano)
  * superfici parametriche a quad: penne, pinne, tubi, cristalli

Formato di uscita "thegods-mesh-1" (JSON), letto da client/mesh/MeshLibrary.java:
{
  "format": 1,
  "bones": [{"name", "parent", "pivot": [x,y,z]}],
  "parts": {
     "<nome>": {
        "material": "water" | "crystal" | "scales" | "membrane" | "glow",
        "bone": "<osso>"           (parte rigida)  oppure
        "j": [b0,b1,...], "w": [w0,...]   (skinning: 2 ossa per vertice, peso del primo),
        "v": [x,y,z, u,v, nx,ny,nz, s0,s1, ...]   (stride 10)
        "t": [...] triangoli  e/o  "q": [...] quad
     }
  }
}
s0 = tinta (0 = profondo, 1 = schiuma), s1 = "finestra" (1 = più trasparente).
Coordinate in blocchi, Y in alto, +Z avanti.
"""
import json
import math

import numpy as np

STRIDE = 10


# ============================================================================ SDF

def length(v):
    return np.sqrt(np.sum(v * v, axis=-1))


def sd_sphere(p, c, r):
    return length(p - np.asarray(c, float)) - r


def sd_ellipsoid(p, c, r):
    r = np.asarray(r, float)
    q = (p - np.asarray(c, float)) / r
    k0 = length(q)
    k1 = length(q / r)
    return k0 * (k0 - 1.0) / np.maximum(k1, 1e-6)


def sd_round_cone(p, a, b, r1, r2):
    """Capsula rastremata (raggio r1 in a, r2 in b). Approssimata ma liscia."""
    a = np.asarray(a, float)
    b = np.asarray(b, float)
    pa = p - a
    ba = b - a
    h = np.clip(np.sum(pa * ba, axis=-1) / np.dot(ba, ba), 0.0, 1.0)
    return length(pa - ba * h[..., None]) - (r1 + (r2 - r1) * h)


def sd_round_box(p, c, half, r, rot=None):
    q = p - np.asarray(c, float)
    if rot is not None:
        q = q @ rot  # rot è una matrice 3x3 (colonne = assi locali)
    q = np.abs(q) - (np.asarray(half, float) - r)
    outside = length(np.maximum(q, 0.0))
    inside = np.minimum(np.max(q, axis=-1), 0.0)
    return outside + inside - r


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0.0, 1.0)
    return b + (a - b) * h - k * h * (1.0 - h)


def smax(a, b, k):
    return -smin(-a, -b, k)


def rot_y(deg):
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rot_x(deg):
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def rot_z(deg):
    t = math.radians(deg)
    c, s = math.cos(t), math.sin(t)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def polygonize(sdf, bmin, bmax, step, target_faces=None, smooth_iters=6):
    """Campiona l'SDF su una griglia, estrae la superficie e la ripulisce."""
    import trimesh
    from skimage import measure

    bmin = np.asarray(bmin, float)
    bmax = np.asarray(bmax, float)
    xs = np.arange(bmin[0], bmax[0] + step, step)
    ys = np.arange(bmin[1], bmax[1] + step, step)
    zs = np.arange(bmin[2], bmax[2] + step, step)
    gx, gy, gz = np.meshgrid(xs, ys, zs, indexing="ij")
    pts = np.stack([gx, gy, gz], axis=-1)
    vol = sdf(pts)
    verts, faces, _, _ = measure.marching_cubes(vol, level=0.0, spacing=(step, step, step))
    verts += bmin
    mesh = trimesh.Trimesh(verts, faces, process=True)
    mesh.remove_unreferenced_vertices()
    if smooth_iters:
        trimesh.smoothing.filter_taubin(mesh, lamb=0.5, nu=0.53, iterations=smooth_iters)
    if target_faces and len(mesh.faces) > target_faces:
        mesh = mesh.simplify_quadric_decimation(face_count=target_faces)
    mesh.fix_normals()
    return mesh


# ============================================================================ costruzione parti

class Part:
    """Accumula vertici (stride 10), triangoli e quad di una parte."""

    def __init__(self, material="water", bone=None):
        self.material = material
        self.bone = bone
        self.v = []
        self.t = []
        self.q = []
        self.j = []
        self.w = []

    def vertex(self, p, uv, n, s0=0.5, s1=0.0):
        self.v.append([*map(float, p), float(uv[0]), float(uv[1]), *map(float, n), float(s0), float(s1)])
        return len(self.v) - 1

    def tri(self, a, b, c):
        self.t += [a, b, c]

    def quad(self, a, b, c, d):
        self.q += [a, b, c, d]

    def add_trimesh(self, mesh, uv_fn, shade_fn):
        base = len(self.v)
        normals = mesh.vertex_normals
        for p, n in zip(mesh.vertices, normals):
            s0, s1 = shade_fn(p)
            self.vertex(p, uv_fn(p), n, s0, s1)
        for f in mesh.faces:
            self.tri(base + int(f[0]), base + int(f[1]), base + int(f[2]))

    def positions(self):
        return np.array([v[:3] for v in self.v])

    def to_json(self):
        out = {"material": self.material, "v": [round(x, 4) for vert in self.v for x in vert]}
        if self.t:
            out["t"] = self.t
        if self.q:
            out["q"] = self.q
        if self.bone:
            out["bone"] = self.bone
        if self.j:
            out["j"] = self.j
            out["w"] = [round(x, 3) for x in self.w]
        return out


def grid_surface(part, fn, nu, nv, n_fn=None, s_fn=None, flip=False):
    """Superficie parametrica fn(u,v)->punto con u,v in [0,1]; normali per differenze finite."""
    idx = [[0] * (nv + 1) for _ in range(nu + 1)]
    eps = 1e-3
    for i in range(nu + 1):
        for k in range(nv + 1):
            u, v = i / nu, k / nv
            p = np.asarray(fn(u, v), float)
            du = np.asarray(fn(min(u + eps, 1), v), float) - np.asarray(fn(max(u - eps, 0), v), float)
            dv = np.asarray(fn(u, min(v + eps, 1)), float) - np.asarray(fn(u, max(v - eps, 0)), float)
            n = np.cross(du, dv)
            ln = np.linalg.norm(n)
            n = n / ln if ln > 1e-9 else np.array([0, 1, 0])
            if flip:
                n = -n
            if n_fn:
                n = n_fn(u, v, n)
            s0, s1 = s_fn(u, v) if s_fn else (0.5, 0.0)
            idx[i][k] = part.vertex(p, (u, v), n, s0, s1)
    for i in range(nu):
        for k in range(nv):
            a, b, c, d = idx[i][k], idx[i + 1][k], idx[i + 1][k + 1], idx[i][k + 1]
            if flip:
                part.quad(a, d, c, b)
            else:
                part.quad(a, b, c, d)
    return idx


def sweep_tube(part, centers, radii, sides=10, s0_fn=None, cap=True):
    """Tubo lungo una polilinea (frame a rotazione minima)."""
    centers = [np.asarray(c, float) for c in centers]
    n = len(centers)
    tangents = []
    for i in range(n):
        a = centers[max(i - 1, 0)]
        b = centers[min(i + 1, n - 1)]
        t = b - a
        tangents.append(t / max(np.linalg.norm(t), 1e-9))
    ref = np.array([0, 1, 0]) if abs(tangents[0][1]) < 0.9 else np.array([1, 0, 0])
    normal = np.cross(tangents[0], ref)
    normal /= np.linalg.norm(normal)
    frames = []
    for i in range(n):
        if i > 0:
            # trasporto parallelo della normale
            t0, t1 = tangents[i - 1], tangents[i]
            axis = np.cross(t0, t1)
            s = np.linalg.norm(axis)
            if s > 1e-9:
                axis /= s
                ang = math.atan2(s, np.dot(t0, t1))
                normal = rotate_vec(normal, axis, ang)
        binormal = np.cross(tangents[i], normal)
        frames.append((normal.copy(), binormal))
    rings = []
    for i in range(n):
        nrm, bin_ = frames[i]
        ring = []
        for k in range(sides + 1):
            a = 2 * math.pi * k / sides
            d = nrm * math.cos(a) + bin_ * math.sin(a)
            p = centers[i] + d * radii[i]
            s0 = s0_fn(i / (n - 1), k / sides) if s0_fn else 0.5
            ring.append(part.vertex(p, (k / sides, i / (n - 1)), d, s0, 0))
        rings.append(ring)
    for i in range(n - 1):
        for k in range(sides):
            part.quad(rings[i][k], rings[i][k + 1], rings[i + 1][k + 1], rings[i + 1][k])
    if cap and radii[-1] > 1e-4:
        tip = part.vertex(centers[-1] + tangents[-1] * radii[-1] * 0.6, (0.5, 1), tangents[-1], 1, 0)
        for k in range(sides):
            part.tri(rings[-1][k], rings[-1][k + 1], tip)


def rotate_vec(v, axis, ang):
    c, s = math.cos(ang), math.sin(ang)
    return v * c + np.cross(axis, v) * s + axis * np.dot(axis, v) * (1 - c)


def crystal(part, base, direction, length_, radius, sides=6, twist=0.0, s0=0.8):
    """Cristallo sfaccettato (bipiramide allungata) con normali piatte."""
    base = np.asarray(base, float)
    d = np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    ref = np.array([0, 1, 0]) if abs(d[1]) < 0.9 else np.array([1, 0, 0])
    e1 = np.cross(d, ref)
    e1 /= np.linalg.norm(e1)
    e2 = np.cross(d, e1)
    bottom = base - d * radius * 0.6
    ring_c = base + d * length_ * 0.18
    tip = base + d * length_
    ring = []
    for k in range(sides):
        a = 2 * math.pi * k / sides + twist
        ring.append(ring_c + (e1 * math.cos(a) + e2 * math.sin(a)) * radius)
    for k in range(sides):
        a, b = ring[k], ring[(k + 1) % sides]
        for tri in ((a, b, tip), (b, a, bottom)):
            n = np.cross(tri[1] - tri[0], tri[2] - tri[0])
            n /= max(np.linalg.norm(n), 1e-9)
            ids = [part.vertex(p, (k / sides, 1.0 if p is tip else 0.0), n, s0, 0) for p in tri]
            part.tri(*ids)


# ============================================================================ skinning

def point_segment_distance(p, a, b):
    a = np.asarray(a, float)
    b = np.asarray(b, float)
    ab = b - a
    t = np.clip(np.sum((p - a) * ab, axis=-1) / max(np.dot(ab, ab), 1e-9), 0, 1)
    return length(p - (a + ab * t[..., None]))


def skin(part, bone_names, segments, power=6.0):
    """Assegna a ogni vertice le 2 ossa più vicine (per distanza dai segmenti ossei)."""
    pos = part.positions()
    dists = np.stack([point_segment_distance(pos, a, b) for (a, b) in segments], axis=1)
    order = np.argsort(dists, axis=1)
    j0, j1 = order[:, 0], order[:, 1]
    d0 = np.maximum(dists[np.arange(len(pos)), j0], 1e-4)
    d1 = np.maximum(dists[np.arange(len(pos)), j1], 1e-4)
    w0 = (1 / d0 ** power) / (1 / d0 ** power + 1 / d1 ** power)
    part.j = [int(x) for pair in zip(j0, j1) for x in pair]
    part.w = [float(x) for x in w0]


# ============================================================================ esportazione

def export(path, parts, bones=None):
    data = {"format": 1, "bones": bones or [], "parts": {name: p.to_json() for name, p in parts.items()}}
    with open(path, "w") as f:
        json.dump(data, f, separators=(",", ":"))
    total_t = sum(len(p.t) // 3 for p in parts.values())
    total_q = sum(len(p.q) // 4 for p in parts.values())
    total_v = sum(len(p.v) for p in parts.values())
    print(f"  {path.split('/')[-1]}: {total_v} vertici, {total_t} triangoli, {total_q} quad")
