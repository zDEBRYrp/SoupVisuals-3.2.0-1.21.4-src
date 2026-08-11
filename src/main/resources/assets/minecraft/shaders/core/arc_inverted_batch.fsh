#version 150

// Batched inverted arc — белый output под inverted blend (1-dstcolor /
// 1-srccolor). Логика идентична arc_inverted.fsh.

#define MAX_BATCH 32

uniform float locations[MAX_BATCH * 2];
uniform float sizes[MAX_BATCH * 2];
uniform float radii[MAX_BATCH];
uniform float thicknesses[MAX_BATCH];
uniform float starts[MAX_BATCH];
uniform float ends[MAX_BATCH];

flat in int v_idx;
out vec4 fragColor;

#define PI 3.141592653589793
#define RAD 0.0174533

void main() {
    int i = v_idx;
    vec2 location = vec2(locations[i*2 + 0], locations[i*2 + 1]);
    vec2 size     = vec2(sizes[i*2 + 0], sizes[i*2 + 1]);
    float radius    = radii[i];
    float thickness = thicknesses[i];
    float start     = starts[i];
    float end       = ends[i];

    float startAngle = start * RAD;
    float endAngle = startAngle + min(end * RAD, PI * 2);

    float smoothThresh = 6.0 * (1.0 / length(size));
    vec2 centerPos = ((gl_FragCoord.xy - location) / size.xy) * 2.0 - 1.0;

    float dist = length(centerPos);
    float bandAlpha = smoothstep(radius, radius + smoothThresh, dist) * smoothstep(radius + thickness, (radius + thickness) - smoothThresh, dist);
    float angle = (atan(centerPos.y, centerPos.x) + PI);
    float angleAlpha = smoothstep(angle, angle - smoothThresh, startAngle - 0.1) * smoothstep(angle, angle + smoothThresh, endAngle + 0.1);

    fragColor = vec4(1.0, 1.0, 1.0, bandAlpha * angleAlpha);
}
