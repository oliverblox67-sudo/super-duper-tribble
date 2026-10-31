#version 150

#moj_import <fog.glsl>

// The Gods — shader dell'acqua divina (vertex).
// Il materiale arriva nel canale "overlay" (UV1.x): 0 acqua, 1 cristallo, 2 scaglie, 3 membrana, 4 bagliore.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;
out vec3 worldNormal;
out vec3 worldPos;
flat out int material;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = Color;
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    worldNormal = Normal;
    worldPos = Position;
    material = UV1.x;
}
