#version 330 core

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform int u_Width;
uniform float u_FillOpacity;
uniform int u_ShapeMode;
uniform float u_GlowMultiplier;

out vec4 color;

void main() {
    vec4 center = texture(u_Texture, v_TexCoord);

    if (center.a != 0.0) {
        if (u_ShapeMode == 0) discard;
        center = vec4(center.rgb, center.a * u_FillOpacity);
    }
    else {
        if (u_ShapeMode == 1) discard;

        float dist = u_Width * u_Width * 4.0;

        for (int x = -u_Width; x <= u_Width; x++) {
            for (int y = -u_Width; y <= u_Width; y++) {
                vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));

                if (offset.a != 0) {
                    float ndist = x * x + y * y - 1.0;
                    dist = min(ndist, dist);
                    center = offset;
                }
            }
        }

        float minDist = u_Width * u_Width;

        float falloff = clamp(1.0 - (dist / minDist), 0.0, 1.0);

        if (dist > minDist || u_GlowMultiplier <= 0.0) {
            center.a = 0.0;
        } else {
            // Use gamma-style amplification to keep the full multiplier range meaningful.
            float exponent = 1.0 / max(u_GlowMultiplier, 0.0001);
            center.a = pow(falloff, exponent);
        }
    }

    color = center;
}
