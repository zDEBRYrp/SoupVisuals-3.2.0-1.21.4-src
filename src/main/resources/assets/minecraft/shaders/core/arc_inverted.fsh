#version 150

uniform float radius;
uniform float thickness;
uniform float start;
uniform float end;
uniform vec2 size;
uniform vec2 location;

out vec4 fragColor;

#define PI 3.141592653589793
#define RAD 0.0174533

void main() {
    float startAngle = start * RAD;
    float endAngle = startAngle + min(end * RAD, PI * 2);

    float smoothThresh = 6.0 * (1.0 / length(size));
    vec2 centerPos = ((gl_FragCoord.xy - location) / size.xy) * 2.0 - 1.0;

    float dist = length(centerPos);
    float bandAlpha = smoothstep(radius, radius + smoothThresh, dist) * smoothstep(radius + thickness, (radius + thickness) - smoothThresh, dist);
    float angle = (atan(centerPos.y, centerPos.x) + PI);
    float angleAlpha = smoothstep(angle, angle - smoothThresh, startAngle - 0.1) * smoothstep(angle, angle + smoothThresh, endAngle + 0.1);

    // Белый цвет для инверсии
    fragColor = vec4(1.0, 1.0, 1.0, bandAlpha * angleAlpha);
}
