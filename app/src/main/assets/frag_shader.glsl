precision mediump float;
varying vec2 vTexSamplingCoord;
uniform sampler2D uTexSampler;
uniform float uBrightness;

const float blacksPreserve = 2.0;
const vec3 W = vec3(0.2126, 0.7152, 0.0722);

//soft clipping, blacks-preserving brightness function
vec3 brightness(in vec3 inp) {
    float luminance = dot(inp, W);
    float shadowed = pow(luminance, blacksPreserve);
    float changedLuminance = (shadowed * uBrightness) / (1.0 + shadowed * (uBrightness - 1.0));
    float sinFac = sin(clamp(luminance, 0.0, 1.0) * 3.1415926 * 0.5);
    changedLuminance = mix(
        luminance,
        pow(changedLuminance, 1.0 / blacksPreserve),
        //sin(clamp(luminance, 0.0, 1.0) * 3.1415926 * 0.5)
        mix(
            sinFac,
            sin(clamp(sinFac, 0.0, 1.0) * 3.1415926 * 0.5),
            0.2
        )
    );
    return inp * (changedLuminance / max(luminance, 0.000001));
}

void main() {
    vec2 uv = vTexSamplingCoord;
    vec4 texColor = texture2D(uTexSampler, uv);
    gl_FragColor = vec4(brightness(texColor.rgb), texColor.a);
}