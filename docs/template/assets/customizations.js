
$(document).ready(function() {
	
	// If we're in an iFrame
	if (window.top != window.self) {
		applyLightStyling();
	}
});


function applyLightStyling() {
	$("#header").addClass("lite-mode");	
}
