#version 150

#moj_import <fog.glsl>

// The Gods — shader dell'acqua divina (fragment).
// Tutto è procedurale: niente texture, solo rumore animato nel tempo.

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;
in vec3 worldNormal;
in vec3 worldPos;
flat in int material;

out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int k = 0; k < 4; k++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

// Reticolo luminoso tipo caustiche: prodotto di due rumori "a cresta" che scorrono.
float caustics(vec2 p, float t) {
    float r1 = 1.0 - abs(noise(p + vec2(t * 0.3, -t * 0.5)) * 2.0 - 1.0);
    float r2 = 1.0 - abs(noise(p * 1.7 + vec2(-t * 0.4, t * 0.25) + r1) * 2.0 - 1.0);
    return pow(r1 * r2, 6.0) * 2.2;
}

void main() {
    float t = GameTime * 1200.0;
    vec3 N = normalize(worldNormal);
    vec3 V = normalize(-worldPos);
    float ndv = clamp(abs(dot(N, V)), 0.0, 1.0);
    float fres = pow(1.0 - ndv, 2.2);
    vec3 base = vertexColor.rgb;
    float a = vertexColor.a;
    vec3 col;
    float alpha;

    if (material == 4) {
        // bagliore (passaggio additivo): più intenso al centro
        col = base * (1.0 + 0.8 * (1.0 - fres)) + vec3(0.15);
        alpha = a;
    } else {
        vec2 uv = texCoord0;
        float flow = fbm(uv * vec2(7.0, 4.0) + vec2(0.0, -t * 0.45));
        float c = caustics(uv * vec2(9.0, 6.0) + vec2(flow * 1.6), t);
        float diffuse = 0.55 + 0.45 * max(abs(dot(N, Light0_Direction)), abs(dot(N, Light1_Direction)));
        col = base * (0.72 + 0.5 * flow);

        if (material == 1) {
            // cristallo d'acqua: più chiaro, bordi brillanti, scintille che saltano
            col = mix(col, vec3(0.82, 0.96, 1.0), 0.4) + fres * vec3(0.5, 0.8, 1.0);
            float sparkle = step(0.993, hash(floor(uv * 38.0) + floor(t * 5.0)));
            col += sparkle * vec3(1.0);
            alpha = a * (0.78 + 0.22 * fres);
        } else if (material == 2) {
            // scaglie: file sfalsate di semicerchi con il bordo illuminato
            vec2 s = vec2(uv.x * 14.0, uv.y * 12.0);
            float row = floor(s.y);
            s.x += mod(row, 2.0) * 0.5;
            vec2 f = fract(s) - vec2(0.5, 0.0);
            float d = length(f * vec2(1.0, 1.25));
            float aa = clamp(fwidth(d) * 1.5, 0.02, 0.3);   // antialiasing: niente moiré da lontano
            float shape = smoothstep(0.62 + aa, 0.48 - aa, d);
            float edge = smoothstep(0.40 - aa, 0.52 + aa, d) * shape * (1.0 - clamp(aa * 4.0, 0.0, 1.0));
            col = col * mix(0.62, 1.08, shape) + edge * vec3(0.45, 0.8, 1.0) * 0.55 + c * vec3(0.3, 0.6, 0.9) * 0.3;
            alpha = a * (0.9 + 0.1 * fres);
        } else if (material == 3) {
            // membrana: venature sottili lungo la lunghezza
            float vein = pow(abs(sin(uv.y * 3.14159 * 14.0 + flow * 2.0)), 18.0);
            col += vein * vec3(0.45, 0.8, 1.0) * 0.5 + c * vec3(0.4, 0.7, 1.0) * 0.4;
            alpha = a * (0.5 + 0.5 * fres) + vein * 0.15;
        } else {
            // acqua viva
            col += c * vec3(0.55, 0.85, 1.0) * 0.85;
            alpha = a * (0.58 + 0.42 * fres) + c * 0.12;
        }
        col += fres * vec3(0.55, 0.85, 1.0) * (material == 2 ? 0.35 : 0.75);
        col *= diffuse;
        col *= mix(lightMapColor.rgb, vec3(1.0), 0.55);
    }

    fragColor = linear_fog(vec4(col, clamp(alpha, 0.0, 1.0)), vertexDistance, FogStart, FogEnd, FogColor) * ColorModulator;
}
