#version 150

// Batched arc — данные упакованы в один shapeData[]. См. round_batch.fsh
// про обоснование (NVIDIA fp5 лимиты на c[] и relative-offset).
//
// Layout per shape:
//   0..1   location.xy
//   2..3   size.xy
//   4      radius
//   5      thickness
//   6      start
//   7      end
//   8..11  color1 RGBA
//   12..15 color2 RGBA

#define MAX_BATCH 32
#define STRIDE 16

uniform float shapeData[MAX_BATCH * STRIDE];

flat in int v_idx;
out vec4 fragColor;

#define PI 3.141592653589793
#define RAD 0.0174533

void main() {
    int b = v_idx * STRIDE;

    vec2 location = vec2(shapeData[b + 0], shapeData[b + 1]);
    vec2 size     = vec2(shapeData[b + 2], shapeData[b + 3]);
    float radius    = shapeData[b + 4];
    float thickness = shapeData[b + 5];
    float start     = shapeData[b + 6];
    float end       = shapeData[b + 7];
    vec4 color1 = vec4(shapeData[b + 8],  shapeData[b + 9],  shapeData[b + 10], shapeData[b + 11]);
    vec4 color2 = vec4(shapeData[b + 12], shapeData[b + 13], shapeData[b + 14], shapeData[b + 15]);

    // ── Точная копия arc.fsh main() ─────────────────────────────────
    float startAngle = start * RAD;
    float endAngle = startAngle + min(end * RAD, PI * 2);

    float smoothThresh = 6.0 * (1.0 / length(size));
    vec2 centerPos = ((gl_FragCoord.xy - location) / size.xy) * 2.0 - 1.0;

    float dist = length(centerPos);
    float bandAlpha = smoothstep(radius, radius + smoothThresh, dist) * smoothstep(radius + thickness, (radius + thickness) - smoothThresh, dist);
    float angle = (atan(centerPos.y, centerPos.x) + PI);
    float angleAlpha = smoothstep(angle, angle - smoothThresh, startAngle - 0.1) * smoothstep(angle, angle + smoothThresh, endAngle + 0.1);

    float angle2 = (angle / PI * 180.);
    angle2 = angle2 - 360. * floor(angle2 / 360.);
    if (angle2 >= 180.) {
        angle2 = (360. - angle2) * 2.;
    } else {
        angle2 = angle2 * 2.;
    }
    fragColor = mix(color1, color2, angle2 / 360.) * bandAlpha * angleAlpha;
}
