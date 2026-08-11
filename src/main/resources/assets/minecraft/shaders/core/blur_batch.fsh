#version 150

// Batched blur-composite. Down/up FBO-chain не батчится (он уже работает
// раз на frame через {@link Blur#setup}), батчим только финальный
// composite-pass — сэмпл blurResult'а + SDF-маска + opt. outline.
//
// Per-shape данные упакованы в один shapeData[] массив с страйдом STRIDE.
// См. round_batch.fsh — та же причина (NVIDIA fp5 лимиты на c[] и
// relative-offset).
//
// Layout per shape:
//   0..1   location.xy
//   2..3   size.xy
//   4..7   radius (TR, BR, TL, BL)
//   8      thickness
//   9..12  color1 (TL) RGBA
//   13..16 color2 (BL) RGBA
//   17..20 color3 (TR) RGBA
//   21..24 color4 (BR) RGBA
//   25..28 outlineColor RGBA
//
// Per-frame (не per-shape): InputSampler + BlurRegionOffset/Size.

#define MAX_BATCH 30
#define STRIDE 29

uniform sampler2D InputSampler;
uniform vec2 BlurRegionOffset;
uniform vec2 BlurRegionSize;

uniform float shapeData[MAX_BATCH * STRIDE];

flat in int v_idx;
out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - halfSize + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec4 gradient(vec2 uv, vec4 c1, vec4 c2, vec4 c3, vec4 c4) {
    vec4 c = mix(mix(c1, c2, uv.y), mix(c3, c4, uv.y), uv.x);
    c += mix(0.002, -0.002, fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453));
    return c;
}

void main() {
    int b = v_idx * STRIDE;

    vec2 location = vec2(shapeData[b + 0], shapeData[b + 1]);
    vec2 size     = vec2(shapeData[b + 2], shapeData[b + 3]);
    vec4 radius   = vec4(shapeData[b + 4], shapeData[b + 5], shapeData[b + 6], shapeData[b + 7]);
    float thickness = shapeData[b + 8];
    vec4 color1 = vec4(shapeData[b + 9],  shapeData[b + 10], shapeData[b + 11], shapeData[b + 12]);
    vec4 color2 = vec4(shapeData[b + 13], shapeData[b + 14], shapeData[b + 15], shapeData[b + 16]);
    vec4 color3 = vec4(shapeData[b + 17], shapeData[b + 18], shapeData[b + 19], shapeData[b + 20]);
    vec4 color4 = vec4(shapeData[b + 21], shapeData[b + 22], shapeData[b + 23], shapeData[b + 24]);
    vec4 outlineColor = vec4(shapeData[b + 25], shapeData[b + 26], shapeData[b + 27], shapeData[b + 28]);

    // ── Логика идентична blur.fsh main() ─────────────────────────────
    vec2 halfSize = size * 0.5;
    vec2 p = gl_FragCoord.xy - location - halfSize;

    float dist = roundedBoxSDF(p, halfSize, radius);
    float aa = max(fwidth(dist), 0.001);

    float outer = 1.0 - smoothstep(-aa, aa, dist);

    vec2 uv = (gl_FragCoord.xy - BlurRegionOffset) / BlurRegionSize;
    vec4 blurColor = texture(InputSampler, uv);

    vec2 gradUV = (gl_FragCoord.xy - location) / size;
    vec4 rectColor = gradient(gradUV, color1, color2, color3, color4);
    vec4 combined = vec4((blurColor * (1.0 - rectColor.a)).rgb, rectColor.a) + rectColor;

    if (thickness <= 0.0) {
        fragColor = vec4(combined.rgb, combined.a * outer);
        return;
    }

    float inner = 1.0 - smoothstep(-aa, aa, dist + thickness);
    float t = (outer > 0.001) ? clamp(inner / outer, 0.0, 1.0) : 0.0;
    vec4 blended = mix(outlineColor, combined, t);
    fragColor = vec4(blended.rgb, blended.a * outer);
}
