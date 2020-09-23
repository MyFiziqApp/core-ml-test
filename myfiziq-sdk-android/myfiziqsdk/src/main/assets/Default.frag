precision mediump float;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec3 vLight;
varying vec2 vTexCoord;
varying vec4 vColor;

void main()
{
    vec4 uAmbientLight = vec4(0.1, 0.1, 0.1, 0.1);

    float distance = length(vLight - vPosition);
    vec3 lightVector = normalize(vLight - vPosition);
    float diffuse = max(dot(vNormal, lightVector), 0.2);
    diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));
    vec4 color = (vColor * diffuse) + (vColor * uAmbientLight);
    color.a = 1.0;
	gl_FragColor = vColor;
}
