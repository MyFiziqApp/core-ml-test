precision mediump float;

// Attributes
attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;

// Code parameters
uniform mat4 uMVPMatrix;
uniform mat4 uMVMatrix;
uniform vec4 uColor;

// Fragment shader parameters
varying vec2 vTexCoord;
varying vec3 vPosition;
varying vec3 vNormal;
varying vec3 vLight;
varying vec4 vColor;

void main()
{
    vec3 uLightPos = vec3(0.0, 0.0, 0.0);

	vColor = uColor;
	vTexCoord = aTexCoord;

    vPosition = vec3(uMVPMatrix * vec4(aPosition, 1.0));
    vNormal = vec3(uMVPMatrix * vec4(aNormal, 0.0));
    vLight = vec3(uMVPMatrix * vec4(uLightPos, 1.0));
	gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}
