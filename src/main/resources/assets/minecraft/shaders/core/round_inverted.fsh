#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float softness;

out vec4 fragColor;

float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - halfSize + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2 halfSize = size * 0.5;
    vec2 p = gl_FragCoord.xy - location - halfSize;

    float dist = roundedBoxSDF(p, halfSize, radius);
    float aa = max(fwidth(dist), 0.001);
    float alpha = 1.0 - smoothstep(-aa, aa, dist);

    fragColor = vec4(1.0, 1.0, 1.0, alpha);
}
