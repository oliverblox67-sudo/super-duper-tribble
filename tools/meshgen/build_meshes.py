#!/usr/bin/env python3
"""Genera tutte le mesh 3D di The Gods in src/main/resources/assets/thegods/meshes/.

Requisiti (una volta sola):
    python3 -m venv .meshenv && .meshenv/bin/pip install numpy scipy scikit-image trimesh fast-simplification
Uso:
    .meshenv/bin/python tools/meshgen/build_meshes.py
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from meshlib import (Part, crystal, export, grid_surface, polygonize, sd_ellipsoid, sd_round_box,  # noqa: E402
                     sd_round_cone, sd_sphere, skin, smax, smin, sweep_tube)

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "thegods", "meshes")


def lerp(a, b, t):
    return a + (b - a) * t


def bezier(p0, p1, p2, t):
    p0, p1, p2 = (np.asarray(x, float) for x in (p0, p1, p2))
    return (1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t * t * p2


# =============================================================================== TITANO
# Alto 7 blocchi, guarda verso +Z, il suo braccio DESTRO è sul lato -X.

TITAN_BONES = [
    ("pelvis", -1, (0, 3.1, 0)),
    ("torso", 0, (0, 3.3, 0)),
    ("head", 1, (0, 5.8, 0.05)),
    ("arm_r", 1, (-1.85, 5.3, 0)),
    ("forearm_r", 3, (-2.02, 3.6, 0.05)),
    ("arm_l", 1, (1.85, 5.3, 0)),
    ("forearm_l", 5, (2.02, 3.6, 0.05)),
    ("thigh_r", 0, (-0.75, 3.05, 0)),
    ("shin_r", 7, (-0.82, 1.6, 0.05)),
    ("thigh_l", 0, (0.75, 3.05, 0)),
    ("shin_l", 9, (0.82, 1.6, 0.05)),
]

TITAN_SEGMENTS = [
    ((0, 2.9, 0), (0, 3.45, 0)),
    ((0, 3.5, 0), (0, 5.45, 0)),
    ((0, 5.85, 0.1), (0, 6.8, 0.15)),
    ((-1.8, 5.2, 0), (-2.0, 3.68, 0.05)),
    ((-2.02, 3.52, 0.05), (-2.12, 1.2, 0.25)),
    ((1.8, 5.2, 0), (2.0, 3.68, 0.05)),
    ((2.02, 3.52, 0.05), (2.12, 1.2, 0.25)),
    ((-0.75, 2.98, 0), (-0.8, 1.68, 0.05)),
    ((-0.82, 1.5, 0.05), (-0.86, 0.0, 0.3)),
    ((0.75, 2.98, 0), (0.8, 1.68, 0.05)),
    ((0.82, 1.5, 0.05), (0.86, 0.0, 0.3)),
]


def titan_sdf(p):
    d = sd_ellipsoid(p, (0, 3.25, 0), (1.0, 0.55, 0.68))                          # bacino
    d = smin(d, sd_round_cone(p, (0, 3.35, 0), (0, 4.25, 0.05), 0.78, 0.9), 0.22)   # addome
    d = smin(d, sd_ellipsoid(p, (0, 4.85, 0.0), (1.6, 0.95, 0.85)), 0.22)          # torace
    d = smin(d, sd_ellipsoid(p, (0, 4.75, -0.28), (1.35, 1.05, 0.62)), 0.22)       # dorsali
    for row in range(3):                                                           # addominali
        for s in (-1, 1):
            d = smin(d, sd_ellipsoid(p, (s * 0.24, 3.55 + row * 0.27, 0.62), (0.2, 0.12, 0.12)), 0.08)
    for s in (-1, 1):
        d = smin(d, sd_ellipsoid(p, (s * 0.58, 4.98, 0.42), (0.62, 0.46, 0.38)), 0.15)   # pettorali
        d = smin(d, sd_ellipsoid(p, (s * 0.78, 5.55, -0.05), (0.62, 0.32, 0.45)), 0.2)   # trapezi
        d = smin(d, sd_sphere(p, (s * 1.78, 5.22, 0.0), 0.64), 0.2)                      # deltoidi
        d = smin(d, sd_round_cone(p, (s * 1.85, 5.05, 0), (s * 2.02, 3.62, 0.05), 0.52, 0.42), 0.15)
        d = smin(d, sd_sphere(p, (s * 2.02, 3.58, 0.03), 0.42), 0.12)                    # gomito
        d = smin(d, sd_round_cone(p, (s * 2.03, 3.5, 0.05), (s * 2.1, 2.05, 0.16), 0.42, 0.5), 0.12)
        d = smin(d, sd_round_box(p, (s * 2.12, 1.62, 0.2), (0.4, 0.44, 0.44), 0.14), 0.12)  # pugno
        for kn in range(4):                                                              # nocche
            d = smin(d, sd_sphere(p, (s * 2.12 + (kn - 1.5) * 0.2, 1.3, 0.55), 0.12), 0.05)
        d = smin(d, sd_round_cone(p, (s * 0.75, 3.05, 0), (s * 0.82, 1.62, 0.05), 0.64, 0.5), 0.2)  # coscia
        d = smin(d, sd_sphere(p, (s * 0.82, 1.56, 0.1), 0.48), 0.1)                      # ginocchio
        d = smin(d, sd_round_cone(p, (s * 0.82, 1.5, 0.05), (s * 0.86, 0.38, 0.0), 0.5, 0.4), 0.12)
        d = smin(d, sd_round_box(p, (s * 0.86, 0.2, 0.28), (0.42, 0.2, 0.62), 0.14), 0.12)  # piede
    d = smin(d, sd_round_cone(p, (0, 5.4, 0.0), (0, 5.95, 0.08), 0.48, 0.42), 0.15)   # collo
    d = smin(d, sd_ellipsoid(p, (0, 6.32, 0.14), (0.5, 0.58, 0.55)), 0.15)            # testa
    d = smin(d, sd_round_box(p, (0, 6.02, 0.32), (0.36, 0.22, 0.32), 0.12), 0.1)      # mascella
    d = smin(d, sd_ellipsoid(p, (0, 6.45, 0.56), (0.42, 0.12, 0.13)), 0.08)           # arcata
    for s in (-1, 1):
        d = smax(d, -sd_sphere(p, (s * 0.2, 6.33, 0.66), 0.11), 0.04)                 # orbite
    return d


def build_titan():
    print("Titano degli Abissi...")
    mesh = polygonize(titan_sdf, (-2.8, -0.05, -1.05), (2.8, 7.05, 1.2), 0.04, target_faces=16000)
    body = Part("water")

    def uv(p):
        return (math.atan2(p[0], p[2]) / (2 * math.pi) + 0.5, p[1] / 7.0)

    def shade(p):
        tint = min(1.0, 0.25 + 0.6 * p[1] / 7.0)
        # "finestra" sul petto: lì l'acqua è più trasparente per vedere chi c'è dentro
        q = np.array(p) - np.array([0, 4.4, 0.55])
        q[1] *= 0.7
        window = max(0.0, 1.0 - np.linalg.norm(q) / 1.25)
        return tint, window

    body.add_trimesh(mesh, uv, shade)
    skin(body, [b[0] for b in TITAN_BONES], TITAN_SEGMENTS)

    parts = {"body": body}
    # ---- armatura di cristallo d'acqua (parti rigide)
    for side, suffix in ((-1, "r"), (1, "l")):
        pa = Part("crystal", bone="arm_" + suffix)
        crystal(pa, (side * 1.8, 5.62, 0.0), (side * 0.45, 1.0, -0.2), 1.05, 0.2, twist=0.3)
        crystal(pa, (side * 2.1, 5.5, 0.1), (side * 0.9, 0.65, 0.1), 0.85, 0.17, twist=0.8)
        crystal(pa, (side * 1.65, 5.62, 0.35), (side * 0.25, 1.0, 0.45), 0.7, 0.15)
        crystal(pa, (side * 1.7, 5.6, -0.35), (side * 0.3, 0.9, -0.6), 0.75, 0.15)
        parts["pauldron_" + suffix] = pa
        fb = Part("crystal", bone="forearm_" + suffix)
        crystal(fb, (side * 2.42, 2.95, -0.1), (side * 0.35, 0.25, -1.0), 1.1, 0.13, twist=0.5)
        crystal(fb, (side * 2.45, 2.45, -0.05), (side * 0.4, 0.1, -1.0), 0.9, 0.12)
        crystal(fb, (side * 2.1, 1.25, 0.62), (side * 0.1, -0.3, 1.0), 0.45, 0.09)
        parts["blade_" + suffix] = fb
        kn = Part("crystal", bone="shin_" + suffix)
        crystal(kn, (side * 0.82, 1.62, 0.48), (side * 0.15, 0.35, 1.0), 0.6, 0.15)
        crystal(kn, (side * 0.9, 0.45, -0.35), (side * 0.3, 0.2, -1.0), 0.5, 0.12)
        parts["greave_" + suffix] = kn

    crown = Part("crystal", bone="head")
    for i in range(7):
        a = (i - 3) / 3.0
        length = 0.95 - abs(a) * 0.35
        crystal(crown, (a * 0.32, 6.78 - abs(a) * 0.08, 0.05 - abs(a) * 0.1), (a * 0.55, 1.0, -0.35), length, 0.1, twist=i)
    parts["crown"] = crown

    spine = Part("crystal", bone="torso")
    for i in range(6):
        y = 3.7 + i * 0.36
        crystal(spine, (0, y, -0.78 + (i - 2.5) ** 2 * 0.012), (0, 0.55, -1.0), 0.55 - abs(i - 2.5) * 0.05, 0.13, twist=i)
    parts["spine"] = spine

    eyes = Part("glow", bone="head")
    for s in (-1, 1):
        def eye(u, v, s=s):
            th, ph = u * 2 * math.pi, v * math.pi
            return (s * 0.2 + 0.085 * math.sin(ph) * math.cos(th), 6.33 + 0.045 * math.cos(ph), 0.64 + 0.06 * math.sin(ph) * math.sin(th))
        grid_surface(eyes, eye, 8, 6)
    parts["eyes"] = eyes

    bones = [{"name": n, "parent": p, "pivot": list(pv)} for n, p, pv in TITAN_BONES]
    export(os.path.join(OUT, "titan.json"), parts, bones)


# =============================================================================== LEVIATANO
# Testa: guarda verso +Z, cerniera della mandibola in (0, -0.15, 0).

def skull_sdf(p):
    d = sd_ellipsoid(p, (0, 0.25, 0.5), (1.05, 0.85, 1.55))
    d = smin(d, sd_round_cone(p, (0, 0.2, 1.1), (0, 0.02, 3.3), 0.78, 0.36), 0.35)       # muso
    d = smin(d, sd_ellipsoid(p, (0, 0.95, 0.2), (0.25, 0.35, 0.9)), 0.2)                # cresta
    for s in (-1, 1):
        d = smin(d, sd_ellipsoid(p, (s * 0.55, 0.78, 1.05), (0.34, 0.2, 0.75)), 0.18)    # arcate
        d = smin(d, sd_ellipsoid(p, (s * 0.92, 0.05, 0.25), (0.26, 0.5, 0.65)), 0.2)     # guance
        d = smax(d, -sd_sphere(p, (s * 0.62, 0.52, 1.32), 0.24), 0.05)                   # orbite
        d = smax(d, -sd_sphere(p, (s * 0.18, 0.22, 3.25), 0.07), 0.03)                   # narici
        for k in range(3):                                                              # squame sul muso
            d = smin(d, sd_ellipsoid(p, (s * 0.32, 0.42 - k * 0.03, 1.9 + k * 0.42), (0.16, 0.06, 0.16)), 0.05)
    d = smax(d, -0.15 - p[..., 1], 0.06)                                                # linea della bocca
    return d


def jaw_sdf(p):
    q = p.copy()
    q[..., 1] = (q[..., 1] + 0.35) * 1.5 - 0.35
    d = sd_round_cone(q, (0, -0.35, 0.1), (0, -0.3, 3.0), 0.62, 0.3)
    for s in (-1, 1):
        d = smin(d, sd_ellipsoid(p, (s * 0.7, -0.35, 0.3), (0.3, 0.35, 0.6)), 0.15)
        d = smin(d, sd_ellipsoid(p, (s * 0.55, -0.62, 0.9), (0.08, 0.12, 0.5)), 0.08)       # barbe ossee
    d = smax(d, p[..., 1] + 0.22, 0.05)
    return d


def fan(part, root_a, root_b, tips, nu=10, nv=6, scallop=0.12, s_base=0.4):
    """Membrana a ventaglio tra una linea di attacco (root_a→root_b) e una serie di punte."""
    tips = [np.asarray(t, float) for t in tips]
    ra, rb = np.asarray(root_a, float), np.asarray(root_b, float)

    def tip_at(u):
        f = u * (len(tips) - 1)
        i = min(int(f), len(tips) - 2)
        return lerp(tips[i], tips[i + 1], f - i)

    def fn(u, v):
        root = lerp(ra, rb, u)
        tip = tip_at(u)
        # bordo festonato: tra una spina e l'altra la membrana rientra
        f = u * (len(tips) - 1)
        sc = 1 - scallop * math.sin(math.pi * (f - math.floor(f))) ** 2
        return lerp(root, tip, v * sc)

    grid_surface(part, fn, nu, nv, s_fn=lambda u, v: (s_base + 0.5 * v, 0.0))
    # spine luminose (raggi della pinna)
    spines = Part("crystal")
    for t in range(len(tips)):
        u = t / (len(tips) - 1)
        root = lerp(ra, rb, u)
        sweep_tube(spines, [lerp(root, tips[t], k / 6) for k in range(7)], [lerp(0.06, 0.012, k / 6) for k in range(7)], sides=5)
    return spines


def build_leviathan():
    print("Leviatano...")
    parts = {}
    skull = Part("scales", bone="head")
    mesh = polygonize(skull_sdf, (-1.4, -0.3, -1.3), (1.4, 1.45, 3.55), 0.035, target_faces=7000)
    skull.add_trimesh(mesh, lambda p: (p[0] * 0.5 + 0.5, p[2] / 3.5), lambda p: (0.3 + 0.4 * max(0.0, p[1]), 0.0))
    parts["skull"] = skull

    jaw = Part("scales", bone="jaw")
    mesh = polygonize(jaw_sdf, (-1.1, -1.1, -0.8), (1.1, -0.1, 3.2), 0.035, target_faces=4000)
    jaw.add_trimesh(mesh, lambda p: (p[0] * 0.5 + 0.5, p[2] / 3.5), lambda p: (0.75, 0.0))
    parts["jaw"] = jaw

    teeth_up = Part("crystal", bone="head")
    teeth_lo = Part("crystal", bone="jaw")
    for s in (-1, 1):
        for k in range(10):
            t = k / 9
            z = lerp(0.6, 3.05, t)
            x = s * lerp(0.74, 0.28, t)
            length = 0.5 if k == 7 else lerp(0.22, 0.32, math.sin(t * math.pi))
            crystal(teeth_up, (x, -0.1, z), (s * -0.12, -1.0, 0.18), length, 0.06, sides=5, s0=1.0)
        for k in range(8):
            t = k / 7
            z = lerp(0.7, 2.85, t)
            x = s * lerp(0.56, 0.24, t)
            length = 0.42 if k == 6 else lerp(0.18, 0.26, math.sin(t * math.pi))
            crystal(teeth_lo, (x, -0.24, z), (s * -0.1, 1.0, 0.12), length, 0.055, sides=5, s0=1.0)
    parts["teeth_upper"] = teeth_up
    parts["teeth_lower"] = teeth_lo

    eyes = Part("glow", bone="head")
    for s in (-1, 1):
        def eye(u, v, s=s):
            th, ph = u * 2 * math.pi, v * math.pi
            return (s * 0.6 + 0.16 * math.sin(ph) * math.cos(th), 0.52 + 0.11 * math.cos(ph), 1.34 + 0.15 * math.sin(ph) * math.sin(th))
        grid_surface(eyes, eye, 10, 8)
    parts["eyes"] = eyes

    horns = Part("crystal", bone="head")
    for s in (-1, 1):
        for (base, mid, tip, r) in (((s * 0.5, 0.9, 0.25), (s * 0.95, 1.7, -0.6), (s * 1.25, 1.85, -2.1), 0.24),
                                    ((s * 0.88, 0.55, -0.05), (s * 1.45, 0.75, -0.7), (s * 1.6, 0.6, -1.5), 0.15)):
            cs = [bezier(base, mid, tip, k / 12) for k in range(13)]
            sweep_tube(horns, cs, [lerp(r, 0.01, (k / 12) ** 0.8) for k in range(13)], sides=8)
    parts["horns"] = horns

    frills = Part("membrane", bone="head")
    spines_all = Part("crystal", bone="head")
    for s in (-1, 1):
        sp = fan(frills, (s * 1.0, -0.15, 0.0), (s * 0.95, 0.65, -0.3),
                 [(s * 1.5, -0.7, -0.9), (s * 1.95, -0.2, -1.2), (s * 2.1, 0.35, -1.25), (s * 1.95, 0.9, -1.1), (s * 1.5, 1.35, -0.9)])
        spines_all.v += [v for v in sp.v]
        off = len(spines_all.v) - len(sp.v)
        spines_all.q += [i + off for i in sp.q]
        spines_all.t += [i + off for i in sp.t]
    # cresta dorsale della testa
    sp = fan(frills, (0, 1.2, 1.2), (0, 1.15, -0.7),
             [(0, 1.55, 1.0), (0, 2.0, 0.4), (0, 2.1, -0.3), (0, 1.8, -0.9), (0, 1.3, -1.2)], s_base=0.5)
    off = len(spines_all.v)
    spines_all.v += sp.v
    spines_all.q += [i + off for i in sp.q]
    spines_all.t += [i + off for i in sp.t]
    parts["frills"] = frills
    parts["frill_spines"] = spines_all

    bones = [{"name": "head", "parent": -1, "pivot": [0, 0, 0]}, {"name": "jaw", "parent": 0, "pivot": [0, -0.15, 0.0]}]
    export(os.path.join(OUT, "leviathan_head.json"), parts, bones)

    # ---- pinne del corpo e coda (istanziate dal renderer)
    fins = {}
    pect = Part("membrane")
    sp = fan(pect, (0, 0, 0.35), (0, 0, -0.35),
             [(1.0, -0.2, 0.6), (1.9, -0.5, 0.1), (2.3, -0.7, -0.6), (2.0, -0.6, -1.3), (1.2, -0.3, -1.6)], nu=12)
    fins["pectoral"] = pect
    fins["pectoral_spines"] = sp
    fluke = Part("membrane")
    sp = fan(fluke, (0, 0.25, 0), (0, -0.25, 0),
             [(0, 2.2, -1.6), (0, 1.2, -2.2), (0, 0.0, -1.4), (0, -1.2, -2.2), (0, -2.2, -1.6)], nu=14, scallop=0.25)
    fins["fluke"] = fluke
    fins["fluke_spines"] = sp
    dorsal = Part("crystal")
    crystal(dorsal, (0, 0, 0), (0, 1.0, -0.55), 1.0, 0.16, sides=5)
    fins["dorsal_spike"] = dorsal
    export(os.path.join(OUT, "leviathan_fins.json"), fins)


# =============================================================================== PENNA

def build_feather():
    print("Penna d'acqua...")
    vane = Part("membrane")

    def fn(u, v):
        w = 0.135 * math.sin(math.pi * min(u * 1.04, 1.0)) ** 0.55 + 0.012
        side = (v - 0.5) * 2
        y = side * (w * 0.55 if side < 0 else w) + 0.07 * u * u
        z = 0.045 * math.sin(math.pi * u) - 0.035 * side * side
        x = u - 0.02 * side * side * u
        return (x, y, z)

    grid_surface(vane, fn, 16, 6, s_fn=lambda u, v: (0.2 + 0.7 * u, abs(v - 0.5) * 2))
    rachis = Part("crystal")
    cs = [(lerp(-0.08, 1.0, k / 10), 0.07 * lerp(-0.08, 1.0, k / 10) ** 2, 0.045 * math.sin(math.pi * max(0, lerp(-0.08, 1.0, k / 10))) + 0.004) for k in range(11)]
    sweep_tube(rachis, cs, [lerp(0.016, 0.003, k / 10) for k in range(11)], sides=5)
    export(os.path.join(OUT, "feather.json"), {"vane": vane, "rachis": rachis})


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    build_feather()
    build_leviathan()
    build_titan()
    print("fatto")
