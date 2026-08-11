#version 150

uniform sampler2D Sampler0;
uniform vec2 Resolution;
uniform float Radius;

out vec4 fragColor;

#define TAU 6.28318530718

void main() {
    vec2 uv = gl_FragCoord.xy / Resolution;
    vec2 step_size = vec2(Radius) / Resolution;

    // 1 center + 8 directions × 3 rings = 25 samples total
    vec4 result = texture(Sampler0, uv);

    float angleStep = TAU / 8.0;
    for (float d = 0.0; d < TAU; d += angleStep) {
        vec2 dir = vec2(cos(d), sin(d));
        result += texture(Sampler0, uv + dir * step_size * 0.33);
        result += texture(Sampler0, uv + dir * step_size * 0.67);
        result += texture(Sampler0, uv + dir * step_size);
    }

    fragColor = result / 25.0;
}
