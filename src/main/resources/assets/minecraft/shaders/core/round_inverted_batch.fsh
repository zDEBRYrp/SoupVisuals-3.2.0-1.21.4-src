#version 150

// Batched inverted rounded-rect — белый output (с альфой по SDF), под
// специальный blend-mode 1-dst-color / 1-src-color, который объявлен в
// round_inverted_batch.json. Логика SDF — точная копия round_inverted.fsh.

#define MAX_BATCH 32

uniform float locations[MAX_BATCH * 2];
uniform float sizes[MAX_BATCH * 2];
uniform float radii[MAX_BATCH * 4];

flat in int v_idx;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - halfSize + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    int i = v_idx;
    vec2 location = vec2(locations[i*2 + 0], locations[i*2 + 1]);
    vec2 size     = vec2(sizes[i*2 + 0], sizes[i*2 + 1]);
    vec4 radius   = vec4(radii[i*4 + 0], radii[i*4 + 1], radii[i*4 + 2], radii[i*4 + 3]);

    vec2 halfSize = size * 0.5;
    vec2 p = gl_FragCoord.xy - location - halfSize;

    float dist = roundedBoxSDF(p, halfSize, radius);
    float aa = max(fwidth(dist), 0.001);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    fragColor = vec4(1.0, 1.0, 1.0, alpha);
}
