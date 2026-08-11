#version 150

// Batched rounded-rect shader. Геометрия — N quads подряд в одном
// BufferBuilder'е (4 вершины × N). Per-shape данные лежат в uniform
// arrays и индексируются через gl_VertexID / 4.
//
// Mojang в 1.21 эмитит QUADS как индексированные TRIANGLES с pattern
// 0,1,2,0,2,3, 4,5,6,4,6,7, ... — gl_VertexID возвращает значение из
// index buffer, т. е. для q-го quad'а это 4q+0..4q+3.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

flat out int v_idx;

void main() {
    v_idx = gl_VertexID / 4;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
