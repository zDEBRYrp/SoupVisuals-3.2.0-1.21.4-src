#version 150

uniform sampler2D InputSampler;
uniform vec2 InputSize;   // размер INPUT текстуры (пикселей)
uniform vec2 OutputSize;  // размер OUTPUT текстуры (пикселей)
uniform vec2 UVOffset;    // смещение региона в UV пространстве input [0..1]
uniform vec2 UVScale;     // размер региона в UV пространстве input [0..1]
uniform float Strength;   // множитель смещения (1.0 = стандарт)

out vec4 fragColor;

void main() {
    // localUV [0..1] в пространстве output FBO
    vec2 localUV = gl_FragCoord.xy / OutputSize;
    // Пересчёт в UV пространство input текстуры (кроп региона)
    vec2 uv = UVOffset + localUV * UVScale;
    vec2 hp = (0.5 * Strength) / InputSize;

    vec4 sum = texture(InputSampler, uv) * 4.0;
    sum += texture(InputSampler, uv - hp);
    sum += texture(InputSampler, uv + hp);
    sum += texture(InputSampler, uv + vec2( hp.x, -hp.y));
    sum += texture(InputSampler, uv + vec2(-hp.x,  hp.y));

    fragColor = sum / 8.0;
}
