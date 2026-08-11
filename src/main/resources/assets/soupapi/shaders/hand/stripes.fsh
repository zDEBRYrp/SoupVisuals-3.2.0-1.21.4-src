#version 330 core

in vec2 uv;
out vec4 outColor;

uniform sampler2D ColorTexture;
uniform sampler2D DepthTexture;
uniform float time;
uniform vec2 resolution;
uniform vec3 stripesColor1;
uniform vec3 stripesColor2;
uniform float stripesWidth;
uniform float stripesSpeed;

void main() {
    vec4 originalColor = texture(ColorTexture, uv);
    vec2 texelSize = 1.0 / textureSize(DepthTexture, 0);

    // Optimized depth-based masking - cross pattern
    float centerDepth = texture(DepthTexture, uv).r;
    float minDepth = centerDepth;

    minDepth = min(minDepth, texture(DepthTexture, uv + vec2(-texelSize.x, 0.0)).r);
    minDepth = min(minDepth, texture(DepthTexture, uv + vec2(texelSize.x, 0.0)).r);
    minDepth = min(minDepth, texture(DepthTexture, uv + vec2(0.0, -texelSize.y)).r);
    minDepth = min(minDepth, texture(DepthTexture, uv + vec2(0.0, texelSize.y)).r);

    float mask = smoothstep(0.99, 0.98, minDepth);

    if (mask < 0.01) {
        discard;
    }

    // Create diagonal stripes effect
    vec2 effectUv = uv;
    effectUv.x *= resolution.x / resolution.y;

    float diagonal = (effectUv.x + effectUv.y + time * stripesSpeed) / stripesWidth;

    float pattern = mod(floor(diagonal), 2.0);

    vec3 col = mix(stripesColor1, stripesColor2, pattern);

    vec4 effectColor = vec4(col, 1.0);

    // Mix original color with stripes effect
    vec4 finalHandColor = mix(originalColor, effectColor, 0.85);

    outColor = vec4(finalHandColor.rgb, mask);
}
