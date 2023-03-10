/**
 *  Manage Blue Iris
 *
 *  Author: minollo@minollo.com
 *  Date: 2013-12-06
 */

// Automatically generated. Make future change here.
definition(
    name: "Manage Blue Iris",
    namespace: "",
    author: "minollo@minollo.com",
    description: "Manage Blue Iris",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

{
	appSetting "recordingOnTimeout"
}

preferences {
	section("Poller device...") {
    	input "pollerDevice", "capability.battery", required: false
    }
	section("Server settings..."){
		input "serverURL", "text", title: "URL"
		input "username", "text", title: "User"
		input "password", "password", title: "Password"
		input "virtualSwitch", "capability.switch", title: "Virtual switch", required: false
	}
    section("Camera names...") {
    	input "camera1", "text", title: "Camera #1"
    	input "camera2", "text", title: "Camera #2", required: false
    	input "camera3", "text", title: "Camera #3", required: false
    }
	section("Camera switches... "){
		input "switches", "capability.switch", title: "Switches", multiple: true, required: false
	}
    section("Modes...") {
		input "awayMode", "mode", title: "Away mode"    	
		input "sleepAlarmMode", "mode", title: "Sleep alarm mode", required: false    	
    }
}


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
    unschedule()
    subscribe(location, modeHandler)
    subscribe(virtualSwitch, "switch", virtualSwitchHandler)
	subscribe(app, appTouch)
    if (pollerDevice) subscribe(pollerDevice, "battery", pollerEvent)
	updateMode(location.currentMode.name)
}

def pollerEvent(evt) {
    log.debug "[PollerEvent] timerLatest==${state.timerLatest}; now()==${now()}"
    if (state.timerLatest && (now() - state.timerLatest) > (getRecordingTimeout() + 60) * 1000) {
        log.error "Activating cameras (timer was asleep?)"
        activateCameras()
    }
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
    state.timerLatest = null
    login()
	if (state.session && camera1) {
    	def currentStatus = getStatusDetails(camera1)
        logout()
        if (currentStatus && currentStatus.data && currentStatus.data.pause == 0) {	//recording
        	if (virtualSwitch)
            	virtualSwitch.off()
            else {
                try {unschedule(activateCameras)} catch(e) {log.error "Ignoring error: ${e}"}
                log.info "Disabling cameras at ${location}"
                deActivateCameras()
                switches?.off()
            }
        } else {
        	if (virtualSwitch)
            	virtualSwitch.on()
            else {
                log.info "Enabling cameras at ${location}"
                log.debug "Turning on switches and waiting for ${getRecordingTimeout()} seconds"
                switches?.on()
                state.timerLatest = now()
                runIn(getRecordingTimeout(), activateCameras)
            }
        }
    }
}

def virtualSwitchHandler(evt) {
	log.debug "virtualSwitchHandler($evt)"
    state.timerLatest = null
	if (evt.value == "on") {
        log.info "Enabling cameras at ${location}"
        log.debug "Turning on switches and waiting for ${getRecordingTimeout()} seconds"
        switches?.on()
        state.timerLatest = now()
        runIn(getRecordingTimeout(), activateCameras)
    } else {
    	try { unschedule(activateCameras) } catch(e) { log.error "Ignoring exception: ${e}" }
        log.info "Disabling cameras at ${location}"
        deActivateCameras()
        switches?.off()
    }
}

def modeHandler(evt) {
	updateMode(evt.value)
}

private getStatus(cameraX) {
	if (state.session && cameraX) {
    	def response = getStatusDetails(cameraX)
        log.debug "getStatus(${cameraX}), response: ${response}"
    	if(response && response.data) {
        	return "\n${cameraX} is ${if (response.data.pause == 0) "recording" else "paused"} and motion is ${if(response.data.motion == true) "enabled" else "disabled"}"
        } else {
        	return "\nUnable to get status for ${cameraX}: ${response}"
        }
    } else {
    	return ""
    }
}

private updateMode(newMode) {
	if (newMode == awayMode || (sleepAlarmMode && newMode == sleepAlarmMode)) {
    	if (virtualSwitch)
        	virtualSwitch.on()
        else {
            log.info "Enabling cameras at ${location}"
            log.debug "Turning on switches and waiting for ${getRecordingTimeout()} seconds"
            switches?.on()
            state.timerLatest = now()
            runIn(getRecordingTimeout(), activateCameras)
       	}
    } else {
    	state.timerLatest = null
    	if (virtualSwitch)
        	virtualSwitch.off()
        else {
            try {unschedule(activateCameras)} catch(e) {log.error "Ignoring error: ${e}"}
            log.info "Disabling cameras at ${location}"
            deActivateCameras()
            switches?.off()
        }
    }
}

private deActivateCameras() {
    state.timerLatest = null
    try {unschedule(activateCameras)} catch(e) {log.error "Ignoring error: ${e}"}
    login()
    pauseRecording(camera1)
    pauseRecording(camera2)
    pauseRecording(camera3)
    disableMotion(camera1)
    disableMotion(camera2)
    disableMotion(camera3)
    disableCamera(camera1)
    disableCamera(camera2)
    disableCamera(camera3)
    logout()
}

def activateCameras() {
	log.info "Activating cameras at ${location}"
	try {unschedule(activateCameras)} catch(e) {log.error "Ignoring error: ${e}"}
    login()
    enableCamera(camera1)
    enableCamera(camera2)
    enableCamera(camera3)
    if (location.currentMode.name == awayMode) {	//enable motion alerts only when in away mode
        enableMotion(camera1)
        enableMotion(camera2)
        enableMotion(camera3)
	}        
    unpauseRecording(camera1)
    unpauseRecording(camera2)
    unpauseRecording(camera3)
    logout()
    state.timerLatest = null
}

private login() {

	httpPostJson("${serverURL}/json", [cmd: "login"]) {
		response1 ->
			log.debug "Login: $response1.data"
            def requestClean = "${username}:${response1.data.session}:${password}"
            log.debug "Plain: ${username}:${response1.data.session}:******"
            def myMD5 = requestClean.encodeAsMD5()
            log.debug "MD5: $myMD5"
            httpPostJson("${serverURL}/json", [cmd: "login", session: response1.data.session, response: myMD5]) {
            	response2 ->
                	log.debug "Login: $response2.data"
                    state.session = response2.data.session
            }
    }
}

private logout() {
	if (state.session) {
		httpPostJson("${serverURL}/json", [cmd: "logout", session: state.session]) {
        	response ->
            	log.debug "Logout: $response.data"
        }
        state.session = null
    }
}


private getCamConfig() {
	if (state.session) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: camera1, session: state.session]) {
        	response ->
            	log.debug "getCamConfig: $response.data"
        }
    }
}

private pauseRecording(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, pause: -1, session: state.session]) {
        	response ->
            	log.debug "pauseRecording(${cameraX}): $response.data"
        }
    }
}

private unpauseRecording(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, pause: 0, session: state.session]) {
        	response ->
            	log.debug "unpauseRecording(${cameraX}): $response.data"
        }
    }
}

private enableMotion(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, motion: true, session: state.session]) {
        	response ->
            	log.debug "enableMotion(${cameraX}): $response.data"
        }
    }
}

private disableMotion(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, motion: false, session: state.session]) {
        	response ->
            	log.debug "disableMotion(${cameraX}): $response.data"
        }
    }
}

private enableCamera(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, enable: true, session: state.session]) {
        	response ->
            	log.debug "enableCamera(${cameraX}): $response.data"
        }
    }
}

private disableCamera(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, enable: false, session: state.session]) {
        	response ->
            	log.debug "disableCamera(${cameraX}): $response.data"
        }
    }
}

private getStatusDetails(cameraX) {
	if (state.session && cameraX) {
		httpPostJson("${serverURL}/json", [cmd: "camconfig", camera: cameraX, session: state.session]) {
        	response ->
            	log.debug "getStatusDetails(${cameraX}): $response.data"
                return response.data
        }
    }
}

private getRecordingTimeout() { appSettings.recordingOnTimeout.toInteger() }

