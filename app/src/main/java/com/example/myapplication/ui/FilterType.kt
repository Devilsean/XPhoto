package com.example.myapplication.ui

/**
 * 滤镜类型枚举
 */
enum class FilterType(val displayName: String, val fragmentShader: String) {
    NONE("原图", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        uniform float u_Brightness;
        uniform float u_Contrast;
        uniform float u_Saturation;
        uniform float u_Highlights;
        uniform float u_Shadows;
        uniform float u_Temperature;
        uniform float u_Tint;
        uniform float u_Clarity;
        uniform float u_Sharpen;
        
        vec3 rgb2hsv(vec3 c) {
            vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
            vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
            vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
            float d = q.x - min(q.w, q.y);
            float e = 1.0e-10;
            return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
        }
        
        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }
        
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            vec3 rgb = color.rgb;
            
            // 亮度调整
            rgb += u_Brightness * 0.5;
            
            // 对比度调整
            rgb = (rgb - 0.5) * (1.0 + u_Contrast) + 0.5;
            
            // 饱和度调整
            vec3 hsv = rgb2hsv(rgb);
            hsv.y *= (1.0 + u_Saturation);
            rgb = hsv2rgb(hsv);
            
            // 高光调整
            float luminance = dot(rgb, vec3(0.299, 0.587, 0.114));
            float highlightMask = smoothstep(0.5, 1.0, luminance);
            rgb += highlightMask * u_Highlights * 0.3;
            
            // 阴影调整
            float shadowMask = smoothstep(0.5, 0.0, luminance);
            rgb += shadowMask * u_Shadows * 0.3;
            
            // 色温调整 (负值偏冷，正值偏暖)
            rgb.r += u_Temperature * 0.1;
            rgb.b -= u_Temperature * 0.1;
            
            // 色调调整 (负值偏绿，正值偏品红)
            rgb.r += u_Tint * 0.1;
            rgb.g -= u_Tint * 0.05;
            
            // 清晰度和锐化在 OpenGL ES 2.0 中简化处理
            // 因为 textureSize() 是 GLSL ES 3.0 的函数
            // 这里使用固定的 texel 大小近似值
            vec2 texelSize = vec2(1.0 / 1024.0, 1.0 / 1024.0);
            
            // 清晰度调整 (中频增强) - 简化版
            if (u_Clarity > 0.01) {
                vec3 blurred = vec3(0.0);
                blurred += texture2D(u_Texture, v_TexCoord + vec2(-1.0, -1.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2( 0.0, -1.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2( 1.0, -1.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2(-1.0,  0.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2( 1.0,  0.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2(-1.0,  1.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2( 0.0,  1.0) * texelSize).rgb;
                blurred += texture2D(u_Texture, v_TexCoord + vec2( 1.0,  1.0) * texelSize).rgb;
                blurred /= 9.0;
                rgb = mix(rgb, rgb + (rgb - blurred) * 0.5, u_Clarity);
            }
            
            // 锐化调整 - 简化版
            if (u_Sharpen > 0.01) {
                vec3 sharp = rgb * 5.0
                    - texture2D(u_Texture, v_TexCoord + vec2(-1.0, 0.0) * texelSize).rgb
                    - texture2D(u_Texture, v_TexCoord + vec2( 1.0, 0.0) * texelSize).rgb
                    - texture2D(u_Texture, v_TexCoord + vec2( 0.0,-1.0) * texelSize).rgb
                    - texture2D(u_Texture, v_TexCoord + vec2( 0.0, 1.0) * texelSize).rgb;
                rgb = mix(rgb, sharp, u_Sharpen * 0.5);
            }
            
            gl_FragColor = vec4(clamp(rgb, 0.0, 1.0), color.a);
        }
    """),
    
    GRAYSCALE("黑白", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
            gl_FragColor = vec4(gray, gray, gray, color.a);
        }
    """),
    
    SEPIA("复古", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
            float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
            float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
            gl_FragColor = vec4(r, g, b, color.a);
        }
    """),
    
    COOL("冷色调", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            gl_FragColor = vec4(color.r * 0.8, color.g * 0.9, color.b * 1.1, color.a);
        }
    """),
    
    WARM("暖色调", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            gl_FragColor = vec4(color.r * 1.1, color.g * 0.95, color.b * 0.8, color.a);
        }
    """),
    
    VIVID("鲜艳", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float saturation = 1.5;
            float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
            float r = gray + saturation * (color.r - gray);
            float g = gray + saturation * (color.g - gray);
            float b = gray + saturation * (color.b - gray);
            gl_FragColor = vec4(r, g, b, color.a);
        }
    """),
    
    FADE("淡化", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float saturation = 0.5;
            float gray = 0.299 * color.r + 0.587 * color.g + 0.114 * color.b;
            float r = gray + saturation * (color.r - gray);
            float g = gray + saturation * (color.g - gray);
            float b = gray + saturation * (color.b - gray);
            gl_FragColor = vec4(r, g, b, color.a);
        }
    """),
    
    INVERT("反色", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            gl_FragColor = vec4(1.0 - color.r, 1.0 - color.g, 1.0 - color.b, color.a);
        }
    """),
    
    BRIGHTNESS("增亮", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float brightness = 0.2;
            gl_FragColor = vec4(color.r + brightness, color.g + brightness, color.b + brightness, color.a);
        }
    """),
    
    CONTRAST("对比度", """
        precision mediump float;
        varying vec2 v_TexCoord;
        uniform sampler2D u_Texture;
        void main(){
            vec4 color = texture2D(u_Texture, v_TexCoord);
            float contrast = 1.3;
            float r = (color.r - 0.5) * contrast + 0.5;
            float g = (color.g - 0.5) * contrast + 0.5;
            float b = (color.b - 0.5) * contrast + 0.5;
            gl_FragColor = vec4(r, g, b, color.a);
        }
    """);
    companion object {
        fun fromOrdinal(ordinal: Int): FilterType {
            return values().getOrNull(ordinal) ?: NONE
        }
    }
}