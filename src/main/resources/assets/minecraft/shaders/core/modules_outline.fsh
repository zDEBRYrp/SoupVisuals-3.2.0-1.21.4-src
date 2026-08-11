#version 150

// Outline всего стека строк ModulesList — один draw call для всей фигуры.
// Каждая строка передаётся как 4 подряд лежащих float'а (x, y, width, height)
// в screen GL coords, per-corner радиусы — 4 float'а на строку (TR, BR, TL, BL).
// Фигура = union всех прямоугольников, её SDF = минимум по всем box-SDF.
// Outline = тонкая outset-полоса снаружи фигуры, шириной outlineWidth.
//
// fwidth(dist) даёт правильный AA на любой геометрии включая вогнутые углы
// «лесенки» — никаких артефактов на стыках.
//
// Массивы декларированы как float[128] (а не vec4[32]), чтобы соответствовать
// JSON-uniform'у с type=float, count=128. OpenGL setup на стороне Java тогда
// идёт через glUniform1fv и не выдаёт «Wrong component type» error.

#define MAX_ROWS 32

uniform vec4  outlineColor;
uniform float outlineWidth;
uniform int   rowCount;
uniform float rows[MAX_ROWS * 4];
uniform float radii[MAX_ROWS * 4];

out vec4 fragColor;

float roundedBoxSDF(vec2 center, vec2 halfSize, vec4 r) {
    r.xy = (center.x > 0.0) ? r.xy : r.zw;
    r.x  = (center.y > 0.0) ? r.x  : r.y;
    float rr = clamp(r.x, 0.0, min(halfSize.x, halfSize.y));
    vec2 q = abs(center) - halfSize + rr;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rr;
}

// SDF объединения N rounded-rect'ов = минимум по всем.
float sceneSDF(vec2 p) {
    float d = 1e9;
    for (int i = 0; i < MAX_ROWS; ++i) {
        if (i >= rowCount) break;
        int b = i * 4;
        vec4 rect = vec4(rows[b], rows[b+1], rows[b+2], rows[b+3]);
        vec4 rad  = vec4(radii[b], radii[b+1], radii[b+2], radii[b+3]);
        vec2 pos      = rect.xy;
        vec2 sz       = rect.zw;
        vec2 halfSize = sz * 0.5;
        vec2 center   = p - pos - halfSize;
        d = min(d, roundedBoxSDF(center, halfSize, rad));
    }
    return d;
}

void main() {
    float dist = sceneSDF(gl_FragCoord.xy);
    float fw   = fwidth(dist);

    // outset-outline: рисуем кольцо с d ∈ [0, outlineWidth].
    //   outerEdge — маска фигуры (d ≤ 0)
    //   innerEdge — маска (фигура ∪ outline-band) (d ≤ outlineWidth)
    //   разность даёт само кольцо.
    float outerEdge = 1.0 - smoothstep(-fw, fw, dist);
    float innerEdge = 1.0 - smoothstep(-fw, fw, dist - outlineWidth);
    float outlineMask = innerEdge - outerEdge;

    if (outlineMask <= 0.001) discard;
    fragColor = vec4(outlineColor.rgb, outlineColor.a * outlineMask);
}
