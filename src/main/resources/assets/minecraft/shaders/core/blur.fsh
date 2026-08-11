#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;

uniform sampler2D InputSampler;
uniform vec2 BlurRegionOffset;
uniform vec2 BlurRegionSize;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;

out vec4 fragColor;

// ── SDF ──────────────────────────────────────────────────────────────

float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - halfSize + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

vec4 gradient(vec2 uv) {
    vec4 c = mix(mix(color1, color2, uv.y), mix(color3, color4, uv.y), uv.x);
    c += mix(0.002, -0.002, fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453));
    return c;
}

// ── Main ─────────────────────────────────────────────────────────────

void main() {
    vec2 halfSize = size * 0.5;
    vec2 p = gl_FragCoord.xy - location - halfSize;

    float dist = roundedBoxSDF(p, halfSize, radius);
    float aa = max(fwidth(dist), 0.001);

    // Outer edge
    float outer = 1.0 - smoothstep(-aa, aa, dist);

    // Sample blur texture (region-based)
    vec2 uv = (gl_FragCoord.xy - BlurRegionOffset) / BlurRegionSize;
    vec4 blurColor = texture(InputSampler, uv);

    // Gradient overlay on top of blur
    vec2 gradUV = (gl_FragCoord.xy - location) / size;
    vec4 rectColor = gradient(gradUV);
    vec4 combined = vec4((blurColor * (1.0 - rectColor.a)).rgb, rectColor.a) + rectColor;

    if (thickness <= 0.0) {
        // Simple blurred fill, no outline
        fragColor = vec4(combined.rgb, combined.a * outer);
        return;
    }

    // ── Outline mode ─────────────────────────────────────────────────
    float inner = 1.0 - smoothstep(-aa, aa, dist + thickness);

    // Blend factor: 1.0 in fill, 0.0 in outline band
    float t = (outer > 0.001) ? clamp(inner / outer, 0.0, 1.0) : 0.0;

    // Interpolate between outline and blurred fill
    vec4 blended = mix(outlineColor, combined, t);

    fragColor = vec4(blended.rgb, blended.a * outer);
}
