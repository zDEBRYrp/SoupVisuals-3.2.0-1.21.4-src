#version 150

#moj_import <minecraft:common.glsl>

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
