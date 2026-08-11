#version 150

uniform vec2 size;
uniform vec2 location;
uniform vec4 radius;
uniform float thickness;
uniform float softness;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 outlineColor;
uniform vec4 outlineSides; // (top, right, bottom, left) 0=hide 1=show

out vec4 fragColor;

// ── SDF ──────────────────────────────────────────────────────────────
// Rounded box with per-corner radius. Supports concave (negative) radii
// for fillet cutouts at rect junctions.

float roundedBoxSDF(vec2 p, vec2 halfSize, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;

    if (r.x >= 0.0) {
        vec2 q = abs(p) - halfSize + r.x;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
    } else {
        float cr = -r.x;
        vec2 q = abs(p) - halfSize;
        float rect = min(max(q.x, q.y), 0.0) + length(max(q, 0.0));
        float disk = length(q) - cr;
        return max(rect, -disk);
    }
}

// ── Gradient + dither ────────────────────────────────────────────────

vec4 gradient(vec2 uv) {
    vec4 c = mix(mix(color1, color2, uv.y), mix(color3, color4, uv.y), uv.x);
    c += mix(0.002, -0.002, fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453));
    return c;
}

// ── Outline side mask ────────────────────────────────────────────────
// Returns 0..1 masking outline visibility per-side.
// At corners where two sides meet, uses min() so that disabling either
// adjacent side also hides the corner.

float sideMask(vec2 p, vec2 halfSize) {
    float hMask = (halfSize.x - p.x < halfSize.x + p.x) ? outlineSides.y : outlineSides.w;
    float vMask = (halfSize.y - p.y < halfSize.y + p.y) ? outlineSides.x : outlineSides.z;
    return min(hMask, vMask);
}

// ── Main ─────────────────────────────────────────────────────────────

void main() {
    vec2 halfSize = size * 0.5;
    vec2 p = gl_FragCoord.xy - location - halfSize;

    float dist = roundedBoxSDF(p, halfSize, radius);

    // Screen-space AA via fwidth(): adapts to any GUI scale automatically.
    // Full fwidth (not half) because the no-outline path uses quadratic alpha
    // (outer²) which compresses the effective transition by ~2×.
    float aa = max(fwidth(dist), 0.001);

    // Outer edge — everything inside the SDF boundary
    float outer = 1.0 - smoothstep(-aa, aa, dist);

    vec2 uv = (gl_FragCoord.xy - location) / size;
    vec4 grad = gradient(uv);

    if (thickness <= 0.0) {
        // Quadratic alpha falloff for softer edges — the entire UI was
        // designed around this curve (inherited from the old mix() pattern).
        fragColor = vec4(grad.rgb, grad.a * outer * outer);
        return;
    }

    // ── Outline mode ─────────────────────────────────────────────────
    // Inner edge sits `thickness` pixels inside the outer edge.
    float inner = 1.0 - smoothstep(-aa, aa, dist + thickness);

    float mask = sideMask(p, halfSize);

    // Blend factor: 1.0 deep inside (fill), 0.0 in the outline band.
    float t = (outer > 0.001) ? clamp(inner / outer, 0.0, 1.0) : 0.0;

    // Edge color: outline where mask=1, extended fill where mask=0
    vec4 edgeColor = mix(grad, outlineColor, mask);

    // Interpolate between edge color (band) and fill color (interior)
    vec4 blended = mix(edgeColor, grad, t);

    fragColor = vec4(blended.rgb, blended.a * outer);
}

