definition(
    name: "iDigiThermostat",
    namespace: "iDigiHome",
    author: "iDigiHome, LLC",
    description: "iDigiThermostat programs your Z wave thermostat based on motion sensors and home modes to optimize your HVAC run time while keeping your house cozy",
    category: "My Apps",
    iconUrl: "http://ecx.images-amazon.com/images/I/71tPMvM2S0L._SL1500_.jpg",
    iconX2Url: "http://ecx.images-amazon.com/images/I/71tPMvM2S0L._SL1500_.jpg"
)

preferences {
    
    section("Set this thermostat") {
		input "regCd", "text", title: "Registration Code", required: true
        input "thermostat1", "capability.thermostat"
	}
    section("Use sensors to manage thermostat") {
		input "motionSenors", "capability.motionSensor", title:"Motion sensor", multiple: true
        input "falseAlarmThreshold", "number", title: "Motion sensor threshold (default 5)", required: false
        input "tempSensors", "capability.temperatureMeasurement", title:"Minimum temperature sensor", multiple: true, required:false
        
	}
    section("Based on mode change") {
         input "cozyModes", "mode", title: "Cozy Modes", required: true, multiple: true
         input "esModes", "mode", title: "Energy Saving Modes", required: true, multiple: true
         input "wakeupTime", "time", title: "Wakeup from night mode?"
         input "forceWakeup", "bool", title:"Ingore motion during wake up?"
    }
    section("When heat is on") {
       input "cozyHeatPoint", "number", title: "Cozy Temperature"
       input "esHeatPoint", "number", title: "Energy Saving Temperature"
       
    }    
    section("When A/C is on") {
       input "cozyCoolPoint", "number", title: "Cozy Temperature"
       input "esCoolPoint", "number", title: "Energy Saving Temperature"
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	init()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
	init()
}

private init() {
	subscribe(app, appTouch)
    subscribe(tempSensors, "temperature", tempSensorHandler)  
    subscribe(motionSenors, "motion", motionHandler)
    subscribe(location, modeHandler)
    subscribe(thermostat1, "thermostatSetpoint", thermostatHandler)
    subscribe(thermostat1, "thermostatOperatingState", opStateHandler)
    subscribe(thermostat1, "temperature", tempHandler)
    state.thermostatState = 'saveMode'
    thermostat1.poll()
    state.thermostatMode = thermostat1.currentValue("thermostatMode")
    state.forceWakeup = false
    state.lastMotionTime = now()
    state.lastActiveMotionTime = now()
    state.motionActive = false
    state.threshold = ((falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold : 5)*60 
    state.operatingState = 0
    state.currentTemp = thermostat1.currentValue("temperature")
    if (state.thermostatMode == "cool") {
        state.coolingSetpoint = cozyCoolPoint
    	state.setPoint = cozyCoolPoint
        setCooling()
    } else {
        state.heatingSetpoint = cozyHeatPoint
        state.setPoint = cozyHeatPoint
        setHeating()
    }
    register()
}
private register() {
	def wakeupHour = timeToday(settings.wakeupTime).format('k',TimeZone.getTimeZone('GMT'))
    def wakeupMin  = timeToday(settings.wakeupTime).format('mm',TimeZone.getTimeZone('GMT'))
    log.debug "wakeupHour=$wakeupHour | wakeupMin=$wakeupMin"
    def successClosure = { response ->
    	if (response.statusLine.statusCode != 200) {
        	//TODO: Internal server error
         	sendPush("Server error for $app.name")
        } else {
        	switch (response.data.errorCode) {
            case 0:
            	log.info "RESPONSE=${response.data}"
                break
            case 1:
            	sendPush("Please register your device at http://www.idigihome.com to enable $app.name")
                break
            case 2:
            //TODO: Internal server error
            	log.debug "errorMessage=${response.data.errorMessage}"
            }
        }
	} 
    def thermostatSetting="regCd=${settings.regCd}&zipcode=$location.zipCode&wakeupHourNightMode=$wakeupHour&wakeupMinNightMode=$wakeupMin&clientDeviceId=$thermostat1.id&clientDeviceName=$thermostat1.displayName&cozyCoolPoint=${settings.cozyCoolPoint}&cozyHeatPoint=${settings.cozyHeatPoint}&esCoolPoint=${settings.esCoolPoint}&esHeatPoint=${settings.esHeatPoint}&threshold=${state.threshold}"
 	thermostatSetting = "$thermostatSetting&cozyModes=${settings.cozyModes}&esModes=${settings.esModes}"
    def params = [
       	uri: "http://www.idigihome.com/idigihome/Register?action=registerDevice",
  	    contentType:"application/json",
        body: thermostatSetting,
        success: successClosure       
	]
    log.info "REQUEST=${params.uri}&$thermostatSetting" 
	httpPost(params)	
}

def tempSensorHandler(evt) {
	log.debug "$evt.displayName $evt.name: $evt.value"
    if (state.thermostatMode == "heating") {
    	if ($evt.value < cozyHeatPoint ) {
       		state.heatingSetpoint = state.heatingSetpoint + 1
        	state.setPoint = state.heatingSetpoint + 1
        	setHeating()
            log.info "minumum temperature alert: $evt.name: $evt.value"
        }
    }
}

def opStateHandler(evt) {
	state.action = "run"
    log.debug "opStateHandler: $evt.value"
    switch  (evt.value) {
    	case "idle" : 
    		state.operatingState = 0
            break
        case "heating" : 
            state.operatingState = 1
            //state.heatingSetpoint = thermostat1.currentValue("heatingSetpoint")
            break
        case "cooling" : 
            state.operatingState = 1
           // state.coolingSetpoint = thermostat1.currentValue("coolingSetpoint")
            break 
    } 
    findAction()
}

def tempHandler(evt) {
    state.action = "temp"
    state.currentTemp = thermostat1.currentValue("temperature")
    findAction()
    if (state.errorCode == 0) {
        takeAction(state.action)
    } 
}

def scheduleWakeup(){
	if (state.operatingState == 0) {
    	unschedule(wakeup)
		if (state.wakeupTime > 0 ) {
    		log.info "scheduleWakeup in ${state.wakeupTime/60} min"
        	runIn(state.wakeupTime, wakeup)
    	} else if ( state.wakeupTime < 0 ) {
    		log.info "scheduleWakeup time is now"
    		wakeup()
    	} 
    }
}
def wakeup(){
    state.thermostatMode = thermostat1.currentValue("thermostatMode")
    state.action = "wakeup"
    findAction()
    if (state.errorCode == 0) {
		log.info "wakeup taking action $state.action"
        takeAction(state.action)
    }
}

/*
private testWakeup() {
	log.info "testWakeup"
    state.action = "getWakeTime"
	findAction()
        if (state.errorCode == 0) {
        takeAction(state.action)
    } 
}
*/
def modeHandler(evt) {
	state.action = "mode"
    state.homeMode = evt.value
    findAction()
    if (state.errorCode == 0) {
		//log.info "modeHandler taking action $state.action"
        takeAction(state.action)
    } 
}

private appTouch(evt){
	//state.action = "touch"
    state.action = "getWakeTime"
    findAction()
    if (state.errorCode == 0) {
        takeAction(state.action)
       // sendPush("$app.name was touched. Setting $thermostat1.displayName to cozy temnperature")
       log.info "$app.name will wake up in ${state.wakeupTime/60} min for action $state.action"
    }
}


def motionHandler(evt) {
	def elapsed = now(); 
  	state.motionActive = false
    state.lastMotionTime = now()
    for(sensor in motionSenors) {
    	if( sensor.currentValue('motion') == "active") {
			state.motionActive = true
           
        }
    }  
    log.debug "state.thermostatState=$state.thermostatState state.motionActive=$state.motionActive"
    if (state.thermostatState == 'saveMode' && state.motionActive ) {
    	state.action = "motion"
        findAction()
        if (state.errorCode == 0) {
            log.info "motionHandler taking action $state.action"
            takeAction(state.action)
        }      
    } else if (state.thermostatState == 'cozy' && ! state.motionActive){
    	runIn(state.threshold, "noMotion")
    } else {
      log.debug "no action"
      unschedule()
    }
}

def noMotion() {
	state.action = "motion"
   	findAction()
    if (state.errorCode == 0) {
		log.info "checkforAction taking action $state.action"
        takeAction(state.action)
    } 
}

def thermostatHandler(evt) {
	log.info "thermostatHandler - setPoint=$state.setPoint"
    unschedule(checkActionThermostat)
    runIn (30, checkActionThermostat)
}
def checkActionThermostat() {
  	def setPoint = thermostat1.currentValue("thermostatSetpoint")
	log.info "checkActionThermostat - $setPoint - $state.setPoint"
	if ( setPoint != state.setPoint) {
       state.action = "thermostat"
       findAction()
	}
}
private takeAction(action) {
	switch  (action) {
    	case "changeTemp": 
        	changeTemp()
            break
        case "setHeating": 
        	setHeating()
            break
        case "setCooling": 
        	setCooling()
            break
        case "scheduleWakeup":
        	scheduleWakeup()
            break
        case "recheckMotion":
        	runIn(state.threshold, "recheckMotion")
            break
    }
}

private setHeating() {
	thermostat1.setHeatingSetpoint(state.setPoint)
    log.info "change heating temp to $state.setPoint"
}
private setCooling() {
	thermostat1.setCoolingSetpoint(state.setPoint)
    log.info "change cooling temp to $state.setPoint"
}
private changeTemp() {
	thermostat1.setHeatingSetpoint(state.setPoint)
    log.info "change temp to $state.setPoint"
}
private findAction() {
	//thermostat1.poll()
    state.wakeupTime = 0
	def stateString = "{\"action\":\"$state.action\",\"thermostatMode\":\"$state.thermostatMode\",\"homeMode\":\"$state.homeMode\",\"motionActive\":$state.motionActive"
    stateString = "$stateString,\"operatingState\":$state.operatingState"
    stateString = "state=$stateString,\"lastMotionTime\":\"$state.lastMotionTime\",\"setPoint\":$state.setPoint,\"currentTemp\":$state.currentTemp,\"heatingSetpoint\":$state.heatingSetpoint,\"coolingSetpoint\":$state.coolingSetpoint,\"forceWakeup\":$state.forceWakeup}"
    def successClosure = { response ->
    	if (response.statusLine.statusCode != 200) {
        	//TODO: Internal server error
         	sendPush("Server Error for $app.name")
        } else if (response.data.errorCode == 1) {
        	log.info "data posted! status=${response.statusLine.statusCode} | errorCode=${response.data}"
            sendPush("Please register your device at http://www.idigihome.com to enable $app.name");
        } else if ( response.data.errorCode == 0 ) { 
           log.info "RESPONSE=${response.data}"
            state.heatingSetpoint = response.data.heatingSetpoint
            state.lastMotionTime = response.data.lastMotionTime
            state.currentTemp = response.data.currentTemp
            state.coolingSetpoint = response.data.coolingSetpoint
            state.errorCode = response.data.errorCode
            state.action = response.data.action
            state.motionActive = response.data.motionActive
            state.homeMode = response.data.homeMode
            state.forceWakeup = response.data.forceWakeup
            state.setPoint = response.data.setPoint
            state.wakeupTime = response.data.wakeupTime
            if (response.data.state != null ) {
            	state.thermostatState = response.data.state
            }
        } else { //TODO: Send notification to someone!!!
        	log.debug "RESPONSE=${response.statusLine.statusCode} | errorCode=${response.data}"
        } 
  
	} 
    def params = [
       	uri: "http://www.idigihome.com/idigihome/FindAction?regCd=$settings.regCd&deviceId=$thermostat1.id",
  	    contentType:"application/json",
        body: stateString,
        success: successClosure       
	]
    log.info "REQUEST=${params.uri}&$stateString"   
	httpPost(params)
}
