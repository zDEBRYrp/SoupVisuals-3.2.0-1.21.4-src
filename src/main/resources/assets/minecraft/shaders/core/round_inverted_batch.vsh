#version 150

// Batched inverted rounded-rect. См. round_batch.vsh — та же логика
// instance ID через gl_VertexID / 4.

in vec3 Position;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
flat out int v_idx;

void main() {
    v_idx = gl_VertexID / 4;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
