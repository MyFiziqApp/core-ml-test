precision mediump float;

uniform sampler2D texture0;
uniform mat4 uVIMatrix;

varying vec4 vPosition;
varying vec3 vNormal;
varying vec3 vLight;
varying vec2 vTexCoord;
varying vec4 vColor;
varying float vShininess;
varying float vAmbient;
varying float vDiffuse;
varying float vSpecular;

varying vec3 eyeSpaceNormal;

void main()
{
    // Uses the Phong Reflection Model
    // See: https://en.wikipedia.org/wiki/Phong_reflection_model#/media/File:Phong_components_version_4.png

    // Higher values = More light. Lower values = Less light

    // We want lots of vDiffuse lighting to show the contours on the person's body, so we set matDiffuse to be as high as it can be

    //vShininess = 2.0;
    //vAmbient = 0.5;
    //vDiffuse = 0.8;
    //vSpecular = 1.0;

    vec4 matAmbient = vec4(vAmbient, vAmbient, vAmbient, 1.0);
    vec4 matDiffuse = vec4(vDiffuse, vDiffuse, vDiffuse, 1.0);
    vec4 matSpecular = vec4(vSpecular, vSpecular, vSpecular, 1.0);

    // texture0 is set in the Java code. Probably in the "Texture(Context context, int textureId)" constructor.
    vec4 color = texture2D(texture0,vTexCoord);

	vec3 N = normalize(eyeSpaceNormal);
	vec3 viewDirection = normalize(vec3(uVIMatrix * vec4(0.0, 0.0, 0.0, 1.0) - vPosition));

    vec3 positionToLightSource = vec3(vLight - vec3(vPosition));
    vec3 L = vec3(normalize(positionToLightSource));

    // Reflect the vector. Use this or reflect(incidentV, N);
    vec3 reflectV = reflect(-L, N);

    // Get lighting terms
    vec4 ambientTerm = matAmbient * color;
    vec4 diffuseTerm = matDiffuse * max(dot(N, L), 0.0);
    vec4 specularTerm;
    if (dot(N, L) < 0.0) // light source behind
    {
        specularTerm = vec4(0, 0, 0, 0);
    }
    else
    {
        specularTerm = matSpecular * pow(max(dot(reflectV, viewDirection), 0.0), vShininess);
    }

    // gl_FragColor = vec4(N, 1.0) * vec4(0.5) + vec4(0.5); // show normals
    gl_FragColor = ambientTerm + diffuseTerm + specularTerm;
}
