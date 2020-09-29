precision mediump float;

uniform sampler2D texture0;
uniform mat4 uVIMatrix;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec3 vLight;
varying vec2 vTexCoord;
varying vec4 vColor;
varying float vShininess;
varying float vAmbient;
varying float vDiffuse;
varying float vSpecular;

void main()
{
    vec4 matAmbient = vec4(vAmbient, vAmbient, vAmbient, 1.0);
    vec4 matDiffuse = vec4(vDiffuse, vDiffuse, vDiffuse, 1.0);
    vec4 matSpecular = vec4(vSpecular, vSpecular, vSpecular, 1.0);
    vec4 color = texture2D(texture0,vTexCoord);

    float distance = length(vLight - vPosition);
    vec3 lightVector = normalize(vLight - vPosition);
    vec3 reflectV = reflect(-lightVector, vNormal);
    float diffuse = max(dot(vNormal, lightVector), 0.0);
    //diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));

    //vec4 ambientTerm = matAmbient * color;
    vec4 diffuseTerm = matDiffuse * diffuse;
    float specular = 0.0;
    vec4 specularTerm;
    if (diffuse > 0.0)
    {
        vec3 H = normalize(-vPosition);
        specular = pow(max(dot(reflectV, H), 0.0), vShininess);
    }
    specularTerm = matSpecular * specular;

    gl_FragColor = (matAmbient + diffuseTerm) * color + specularTerm;
}
