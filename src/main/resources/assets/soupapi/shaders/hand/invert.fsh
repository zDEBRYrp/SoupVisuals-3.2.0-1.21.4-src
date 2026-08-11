#version 330 core

in vec2 uv;
out vec4 outColor;

uniform sampler2D ColorTexture;
uniform sampler2D DepthTexture;
uniform float time;
uniform vec2 resolution;

// Invert settings
uniform bool hideHand;
uniform float invertIntensity;
uniform bool invertHue;
uniform bool invertSaturation;
uniform bool invertBrightness;
uniform float edgeGlow;
uniform vec3 glowColor;

// RGB to HSV conversion
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// HSV to RGB conversion
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

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

    vec3 finalColor;

    if (centerDepth < 0.99) {
        // This is hand area
        if (hideHand) {
            // Hand is hidden - apply invert to background
            vec3 bgColor = originalColor.rgb;

            if (invertHue || invertSaturation || invertBrightness) {
                // Advanced invert using HSV
                vec3 hsv = rgb2hsv(bgColor);

                if (invertHue) {
                    hsv.x = fract(hsv.x + 0.5); // Shift hue by 180 degrees
                }
                if (invertSaturation) {
                    hsv.y = 1.0 - hsv.y; // Invert saturation
                }
                if (invertBrightness) {
                    hsv.z = 1.0 - hsv.z; // Invert brightness
                }

                finalColor = mix(bgColor, hsv2rgb(hsv), invertIntensity);
            } else {
                // Simple RGB invert
                vec3 inverted = vec3(1.0) - bgColor;
                finalColor = mix(bgColor, inverted, invertIntensity);
            }

        } else {
            // Hand is visible - invert the hand texture
            vec3 handColor = originalColor.rgb;

            if (invertHue || invertSaturation || invertBrightness) {
                // Advanced invert using HSV
                vec3 hsv = rgb2hsv(handColor);

                if (invertHue) {
                    hsv.x = fract(hsv.x + 0.5);
                }
                if (invertSaturation) {
                    hsv.y = 1.0 - hsv.y;
                }
                if (invertBrightness) {
                    hsv.z = 1.0 - hsv.z;
                }

                finalColor = mix(handColor, hsv2rgb(hsv), invertIntensity);
            } else {
                // Simple RGB invert
                vec3 inverted = vec3(1.0) - handColor;
                finalColor = mix(handColor, inverted, invertIntensity);
            }
        }
    } else {
        // Not hand area - just use original
        finalColor = originalColor.rgb;
    }

    // Add edge glow if enabled
    if (edgeGlow > 0.01 && centerDepth < 0.99) {
        float edgeStrength = 0.0;

        float right = texture(DepthTexture, uv + vec2(texelSize.x, 0.0)).r;
        float left = texture(DepthTexture, uv + vec2(-texelSize.x, 0.0)).r;
        float top = texture(DepthTexture, uv + vec2(0.0, texelSize.y)).r;
        float bottom = texture(DepthTexture, uv + vec2(0.0, -texelSize.y)).r;

        if (right > 0.99) edgeStrength += 1.0;
        if (left > 0.99) edgeStrength += 1.0;
        if (top > 0.99) edgeStrength += 1.0;
        if (bottom > 0.99) edgeStrength += 1.0;

        edgeStrength = clamp(edgeStrength * 0.25, 0.0, 1.0);

        // Pulsing effect
        float pulse = 0.8 + sin(time * 3.0) * 0.2;
        vec3 glow = glowColor * edgeStrength * edgeGlow * pulse;

        finalColor += glow;
    }

    outColor = vec4(finalColor, mask);
}
