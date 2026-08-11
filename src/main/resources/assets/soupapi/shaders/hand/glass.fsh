#version 330 core

in vec2 uv;
out vec4 outColor;

uniform sampler2D ColorTexture;
uniform sampler2D DepthTexture;
uniform float time;
uniform vec2 resolution;

// Glass settings
uniform float blurSize;
uniform float quality;
uniform float direction;
uniform float refraction;
uniform float brightness;
uniform bool enableChromatic;
uniform bool enableDistortion;
uniform bool hideHand;

// Background effect settings (when hand is hidden)
uniform float backgroundBlur;

#define PI 3.14159265

// Optimized liquid glass blur - reduced samples
vec4 liquidGlassBlur(sampler2D tex, vec2 uv, float dir, float qual, float size) {
    vec2 radius = (size / resolution.y) / resolution;
    vec4 color = texture(tex, uv);
    float total = 1.0;

    // Clamp quality and direction for performance
    float optQual = min(qual, 6.0);  // Max 6 samples per direction
    float optDir = min(dir, 8.0);    // Max 8 directions

    float angleStep = PI / optDir;
    float radiusStep = 1.0 / optQual;

    for (float d = 0.0; d < PI; d += angleStep) {
        vec2 direction = vec2(cos(d), sin(d));
        for (float i = radiusStep; i <= 1.0; i += radiusStep) {
            vec2 offset = direction * radius * i;
            color += texture(tex, uv + offset);
            total += 1.0;
        }
    }

    return color / total;
}

// Optimized matte blur - reduced samples
vec4 matteBlur(sampler2D tex, vec2 uv, float blurRadius) {
    const float TAU = 6.28318530718;
    vec2 radius = blurRadius / resolution.xy;
    vec4 blur = texture(tex, uv);

    float step = TAU / 8.0;  // 8 directions instead of 16

    for (float d = 0.0; d < TAU; d += step) {
        vec2 dir = vec2(cos(d), sin(d));
        for (float i = 0.33; i <= 1.0; i += 0.33) {  // 3 samples instead of 5
            blur += texture(tex, uv + dir * radius * i);
        }
    }

    blur /= 25.0;  // 8 directions * 3 samples + 1 center = 25
    return blur;
}

// Distortion pattern for glass effect
vec2 glassDistortion(vec2 uv, float time, float strength) {
    // Animated wave distortion
    float wave1 = sin(uv.y * 10.0 + time * 2.0) * 0.01 * strength;
    float wave2 = cos(uv.x * 8.0 - time * 1.5) * 0.01 * strength;

    // Radial distortion from center
    vec2 center = vec2(0.5, 0.5);
    vec2 toCenter = uv - center;
    float dist = length(toCenter);
    vec2 radial = toCenter * sin(dist * 15.0 - time * 3.0) * 0.005 * strength;

    return vec2(wave1 + radial.x, wave2 + radial.y);
}

void main() {
    vec4 originalColor = texture(ColorTexture, uv);
    vec2 texelSize = 1.0 / textureSize(DepthTexture, 0);

    // Optimized depth-based masking - reduced from 9 to 5 samples (cross pattern)
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

    // Apply glass distortion (if enabled)
    vec2 distortedUV = uv;
    if (enableDistortion) {
        distortedUV += glassDistortion(uv, time, refraction);
    }

    // Sample the hand texture with distortion applied
    // Note: If hideHand is enabled, hand is not drawn to color buffer (only depth)
    // via colorMask(false,false,false,false) in GameRendererMixin
    vec4 handColor = texture(ColorTexture, distortedUV);

    // Create glass effect
    vec3 glassColor;

    if (centerDepth < 0.99) {
        // This is hand area
        if (hideHand) {
            // Hand is hidden - apply background effects
            vec2 bgUV = uv;

            // Apply background blur (use matte blur for frosted glass effect)
            if (backgroundBlur > 0.1) {
                glassColor = matteBlur(ColorTexture, bgUV, backgroundBlur).rgb;
            } else {
                glassColor = texture(ColorTexture, bgUV).rgb;
            }

            // Apply brightness
            glassColor *= brightness;

        } else {
            // Hand is visible - apply glass effect to hand texture
            glassColor = liquidGlassBlur(ColorTexture, distortedUV, direction, quality, blurSize).rgb;

            // Add chromatic aberration for glass effect (if enabled)
            if (enableChromatic) {
                float aberration = 0.003 * refraction;
                float r = liquidGlassBlur(ColorTexture, distortedUV + vec2(aberration, 0.0), direction, quality, blurSize * 0.5).r;
                float b = liquidGlassBlur(ColorTexture, distortedUV - vec2(aberration, 0.0), direction, quality, blurSize * 0.5).b;
                glassColor.r = r;
                glassColor.b = b;
            }

            // Apply brightness to glass effect
            glassColor *= brightness;
        }
    } else {
        // Not hand area - just use original
        glassColor = handColor.rgb;
    }

    // Optimized edge highlights - use cross pattern instead of 3x3
    float edgeStrength = 0.0;
    if (centerDepth < 0.99) {
        float right = texture(DepthTexture, uv + vec2(texelSize.x, 0.0)).r;
        float left = texture(DepthTexture, uv + vec2(-texelSize.x, 0.0)).r;
        float top = texture(DepthTexture, uv + vec2(0.0, texelSize.y)).r;
        float bottom = texture(DepthTexture, uv + vec2(0.0, -texelSize.y)).r;

        if (right > 0.99) edgeStrength += 1.0;
        if (left > 0.99) edgeStrength += 1.0;
        if (top > 0.99) edgeStrength += 1.0;
        if (bottom > 0.99) edgeStrength += 1.0;

        edgeStrength = clamp(edgeStrength * 0.15, 0.0, 1.0);
    }

    // Add edge highlights
    vec3 highlight = vec3(1.0) * edgeStrength * 0.3;
    glassColor += highlight;

    // Subtle reflection effect
    float reflection = sin(time * 2.0 + uv.x * 20.0) * 0.5 + 0.5;
    reflection *= sin(time * 1.5 + uv.y * 15.0) * 0.5 + 0.5;
    glassColor += vec3(reflection * 0.1);

    // Mix distorted hand texture with glass effect
    vec3 finalColor = mix(handColor.rgb, glassColor, 0.7);

    outColor = vec4(finalColor, mask);
}
