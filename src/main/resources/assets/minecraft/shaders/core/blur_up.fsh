#version 150

uniform sampler2D InputSampler;
uniform vec2 InputSize;   // размер INPUT текстуры (маленький)
uniform vec2 OutputSize;  // размер OUTPUT текстуры (большой)
uniform float Strength;   // множитель смещения (1.0 = стандарт)

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / OutputSize;
    vec2 hp = (0.5 * Strength) / InputSize;

    vec4 sum = vec4(0.0);

    // Диагонали (вес 2)
    sum += texture(InputSampler, uv + vec2(-hp.x,  hp.y)) * 2.0;
    sum += texture(InputSampler, uv + vec2( hp.x,  hp.y)) * 2.0;
    sum += texture(InputSampler, uv + vec2( hp.x, -hp.y)) * 2.0;
    sum += texture(InputSampler, uv + vec2(-hp.x, -hp.y)) * 2.0;

    // Кардинальные (вес 1, двойное смещение)
    sum += texture(InputSampler, uv + vec2(0.0,        hp.y * 2.0));
    sum += texture(InputSampler, uv + vec2(0.0,       -hp.y * 2.0));
    sum += texture(InputSampler, uv + vec2( hp.x * 2.0, 0.0));
    sum += texture(InputSampler, uv + vec2(-hp.x * 2.0, 0.0));

    fragColor = sum / 12.0;
}
