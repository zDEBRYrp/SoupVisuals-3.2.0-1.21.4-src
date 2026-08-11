#version 150

in vec2 TexCoord;
in vec4 FragColor;
in vec2 GlobalPos;

uniform sampler2D Sampler0;
uniform float Range;
uniform float Thickness;
uniform float Smoothness;
uniform int Outline;
uniform float OutlineThickness;
uniform vec4 OutlineColor;
uniform vec4 ColorModulator;

uniform int EnableFadeout;
uniform float FadeoutStart;
uniform float FadeoutEnd;
uniform float MaxWidth;
uniform float TextPosX;

out vec4 OutColor;

float median(vec3 color) {
    return max(min(color.r, color.g), min(max(color.r, color.g), color.b));
}

void main() {
    // MTSDF: rgb = MSDF channels, a = pure SDF. Используем median(rgb) для острых углов.
    vec4 sample = texture(Sampler0, TexCoord);
    float dist = median(sample.rgb) - 0.5 + Thickness;

    // Анти-алиасинг через производные UV: чем ближе фрагмент к границе глифа в
    // экранных пикселях, тем плавнее переход. Range — distanceRange из MSDF JSON.
    //
    // Используем rotation-invariant формулу: считаем длину градиента UV
    // по обоим осям screen-space (dFdx + dFdy) и берём максимум. Это работает
    // и для axis-aligned UV (обычный текст), и для повёрнутых UV (иконки
    // с rotated UV-mapping в soup_icons). Прежняя формула dFdx(U)/dFdy(V)
    // давала нули при повороте UV и ломала AA → ступенчатые края.
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    vec2 dxv = dFdx(TexCoord) * texSize;
    vec2 dyv = dFdy(TexCoord) * texSize;
    float pixelLen = max(length(dxv), length(dyv));
    float pixels = Range / max(pixelLen, 1e-4);
    float alpha = smoothstep(-Smoothness, Smoothness, dist * pixels);
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (Outline == 1) {
        color = mix(OutlineColor, FragColor, alpha);
        color.a *= smoothstep(-Smoothness, Smoothness, (dist + OutlineThickness) * pixels);
    }

    if (EnableFadeout == 1) {
        float fadeAlpha = 1.0;
        float relativeX = GlobalPos.x - TextPosX;
        float normalizedX = relativeX / MaxWidth;
        if (normalizedX > FadeoutStart) {
            fadeAlpha = 1.0 - smoothstep(FadeoutStart, FadeoutEnd, normalizedX);
        }
        color.a *= fadeAlpha;
    }

    vec4 finalColor = color * ColorModulator;
    if (finalColor.a <= 0.01) {
        discard;
    }

    OutColor = finalColor;
}
