precision mediump float;

// Attributes
attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec2 aTexCoord;

// Code parameters
uniform vec3 uLightPos;
uniform float uShininess;
uniform float uAmbient;
uniform float uDiffuse;
uniform float uSpecular;

uniform mat4 uMVPMatrix;
uniform mat4 uVPMatrix;
uniform mat4 uMMatrix;
uniform mat4 uMVPNormalMatrix;
uniform vec4 uColor;

// Fragment shader parameters
varying vec2 vTexCoord;
varying vec4 vPosition;
varying vec3 vNormal;
varying vec3 vLight;
varying vec4 vColor;
varying vec3 eyeSpaceNormal;
varying float vShininess;
varying float vAmbient;
varying float vDiffuse;
varying float vSpecular;

void main()
{
	vColor = uColor;
	vTexCoord = aTexCoord;
    vShininess = uShininess;
    vAmbient = uAmbient;
    vDiffuse = uDiffuse;
    vSpecular = uSpecular;
    vLight = uLightPos;

	eyeSpaceNormal = vec3(uMVPNormalMatrix * vec4(aNormal, 1.0));
    vPosition = uMMatrix * vec4(aPosition, 1.0);
    vNormal = vec3(uMVPMatrix * vec4(aNormal, 0.0));
	gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
}
