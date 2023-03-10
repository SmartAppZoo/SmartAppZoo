/**
 *  ecobee3RemoteSensorsInit
 *
 *  Copyright 2015 Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights, trade secret 
 *  in the Background technology. May be subject to consulting fees under the Agreement between the Developer and the Customer. 
 *  Developer grants a non exclusive perpetual license to use the Background technology in the Software developed for and delivered 
 *  to Customer under this Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
  *  Software Distribution is restricted and shall be done only with Developer's written approval.
 *
 *  For installation, please refer to readme file under
 *     https://github.com/yracine/device-type.myecobee/blob/master/smartapps/readme.ecobee3RemoteSensor
 *
 */
definition(
	name: "ecobee3RemoteSensorsInit",
	namespace: "yracine",
	author: "Yves Racine",
	description: "Create individual ST sensors for all selected ecobee3's remote sensors and update them on a regular basis (interval chosen by the user).",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)

preferences {

	page(name: "selectThermostat", title: "Ecobee Thermostat", install: false, uninstall: true, nextPage: "selectEcobeeSensors") {
		section("About") {
			paragraph "ecobeeRemoteSensorsInit, the smartapp that creates individual ST sensors for your ecobee3's remote Sensors and polls them on a regular basis"
			paragraph "Version 2.5"
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yracine%40yahoo%2ecom&lc=US&item_name=Maisons%20ecomatiq&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest",
					title:"Paypal donation..."
			paragraph "Copyright�2014 Yves Racine"
				href url:"http://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information..." 
		}
		section("Select the ecobee thermostat") {
			input "ecobee", "capability.thermostat", title: "Which ecobee thermostat?"

		}
		section("Polling ecobee3's remote3 sensor(s) at which interval in minutes (range=[5,10,15,30],default =5 min.)?") {
			input "givenInterval", "number", title: "Interval", required: false
		}

	}
	page(name: "selectEcobeeSensors", title: "Ecobee Remote Sensors", content: "selectEcobeeSensors", nextPage: "Notifications")
	page(name: "Notifications", title: "Other Options", install: true, uninstall: true) {
		section("Handle/Notify any exception proactively") {
			input "handleExceptionFlag", "bool", title: "Handle exceptions proactively?", required: false
		}
		section("What do I use as Master on/off switch to restart smartapp processing? [optional]") {
			input (name:"powerSwitch", type:"capability.switch", required: false, description: "Optional")
		}
		section("What do I use as temperature polling sensor to restart smartapp processing? [optional]") {
			input (name:"tempSensor", type:"capability.temperatureMeasurement", required: false, multiple:true, description: "Optional")
		}
		section("What do I use as energy polling sensor to restart smartapp processing? [optional]") {
			input (name:"energyMeter", type:"capability.powerMeter", required: false, multiple:true, description: "Optional")
		}
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required: false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
		section([mobileOnly: true]) {
			label title: "Assign a name for this SmartApp", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}




def selectEcobeeSensors() {

	def sensors = [: ]
	/* Generate the list of all remote sensors available 
	*/
	try {
		ecobee.generateRemoteSensorEvents("", false, true)
	} catch (e) {
		log.debug "selectEcobeeSensors>exception $e when getting list of Remote Sensors, exiting..."
		return sensors
	}    

	/* Get only the list of all occupancy remote sensors available 
	*/

	def ecobeeSensors = ecobee.currentRemoteSensorOccData.toString().minus('[').minus(']').split(",,")

	log.debug "selectEcobeeSensors>ecobeeSensors= $ecobeeSensors"

	if (!ecobeeSensors) {

		log.debug "selectEcobeeSensors>no values found"
		return sensors

	}

	for (i in 0..ecobeeSensors.size() - 1) {

		def ecobeeSensorDetails = ecobeeSensors[i].split(',')
		def ecobeeSensorId = ecobeeSensorDetails[0]
		def ecobeeSensorName = ecobeeSensorDetails[1]

		def dni = [app.id, ecobeeSensorName, getRemoteSensorChildName(), ecobeeSensorId].join('.')

		sensors[dni] = ecobeeSensorName

	}


	log.debug "selectEcobeeSensors> sensors= $sensors"


	def chosenSensors = dynamicPage(name: "selectEcobeeSensors", title: "Select Ecobee Sensors", install: false, uninstall: true) {
		section("Select Remote Sensors") {
			input(name: "remoteSensors", title: "", type: "enum", required: false, multiple: true, description: "Tap to choose", metadata: [values: sensors])
		}
	}
	return chosenSensors
}



def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()
	initialize()
}

def rescheduleHandler(evt) {
	log.debug "$evt.name: $evt.value"
	rescheduleIfNeeded()		
}



private def createRemoteSensors() {

	def devices = remoteSensors.collect {dni->
		def d = getChildDevice(dni)
		log.debug "initialize>Looping thru motion Sensors, found id $dni"

		if (!d) {
			def sensor_info = dni.tokenize('.')
			def sensorId = sensor_info.last()
			def sensorName = sensor_info[1]
			def tstatName = ecobee.currentThermostatName            
 
			def labelName = "${tstatName}:${sensorName}"
			log.debug "About to create child device with id $dni, sensorId = $sensorId, labelName=  ${labelName}"
			d = addChildDevice(getChildNamespace(), getRemoteSensorChildName(), dni, null, [label: "${labelName}"])
			log.debug "created ${d.displayName} with id $dni"
		} else {
			log.debug "initialize>found ${d.displayName} with id $dni already exists"
		}

	}
	log.trace("ecobeeRemoteSensorsInit>created ${devices.size()} MyEcobee's Remote Sensors" ) 

}


private def deleteRemoteSensors() {

	def delete
	// Delete any that are no longer in settings
	if (!remoteSensors) {
		log.debug "delete all Remote Sensors"
		delete = getChildDevices().findAll {
			it.device.deviceNetworkId.contains(getRemoteSensorChildName())
		}
	} else {
		delete = getChildDevices().findAll {
			((it.device.deviceNetworkId.contains(getRemoteSensorChildName())) && (!remoteSensors.contains(it.device.deviceNetworkId)))
		}
	}		
	log.trace("ecobeeRemoteSensorsInit>deleting ${delete.size()} MyEcobee's Remote Sensors")
	delete.each {
		try {    
			deleteChildDevice(it.deviceNetworkId)
		} catch(e) {
			log.error("ecobeeRemoteSensorsInit>exception $e while trying to delete Remote Sensor ${it.deviceNetworkId}")
			send("ecobeeRemoteSensorsInit>exception $e while trying to delete Remote Sensor ${it.deviceNetworkId}")
		}        
	}


}


def initialize() {
	log.debug "initialize>begin"

    
	state?.exceptionCount=0       
	state?.runtimeRevision=null
/*    
	subscribe(ecobee, "remoteSensorOccData", updateRemoteSensors)
	subscribe(ecobee, "remoteSensorTmpData", updateRemoteSensors)
	subscribe(ecobee, "remoteSensorHumData", updateRemoteSensors)
*/
	deleteRemoteSensors()
	createRemoteSensors()


	Integer delay = givenInterval ?: 5 // By default, do it every 5 min.
    
	log.trace("ecobeeRemoteSensorsInit>scheduling takeAction every ${delay} minutes")
	state?.poll = [ last: 0, rescheduled: now() ]

	//Subscribe to different events (ex. sunrise and sunset events) to trigger rescheduling if needed
	subscribe(location, "sunrise", rescheduleIfNeeded)
	subscribe(location, "sunset", rescheduleIfNeeded)
	subscribe(location, "mode", rescheduleIfNeeded)
	subscribe(location, "sunriseTime", rescheduleIfNeeded)
	subscribe(location, "sunsetTime", rescheduleIfNeeded)
	if (powerSwitch) {
		subscribe(powerSwitch, "switch.off", rescheduleHandler, [filterEvents: false])
		subscribe(powerSwitch, "switch.on", rescheduleHandler, [filterEvents: false])
	}
	if (tempSensor)	{
		subscribe(tempSensor,"temperature", rescheduleHandler,[filterEvents: false])
	}

	if (energyMeter)	{
		subscribe(energyMeter,"energy", rescheduleHandler,[filterEvents: false])
	}

	subscribe(app, appTouch)

	log.trace "initialize>polling delay= ${delay}..."
	rescheduleIfNeeded()   
}

def appTouch(evt) {
	rescheduleIfNeeded()
}


def rescheduleIfNeeded(evt) {
	if (evt) log.debug("rescheduleIfNeeded>$evt.name=$evt.value")
	Integer delay = givenInterval ?: 5 // By default, do it every 5 min.
	BigDecimal currentTime = now()    
	BigDecimal lastPollTime = (currentTime - (state?.poll["last"]?:0))  
	if (lastPollTime != currentTime) {    
		Double lastPollTimeInMinutes = (lastPollTime/60000).toDouble().round(1)      
		log.info "rescheduleIfNeeded>last poll was  ${lastPollTimeInMinutes.toString()} minutes ago"
	}
	if (((state?.poll["last"]?:0) + (delay * 60000) < currentTime) && canSchedule()) {
		log.info "rescheduleIfNeeded>scheduling takeAction in ${delay} minutes.."
		if ((delay >=0) && (delay <10)) {      
			runEvery5Minutes(takeAction)
		} else if ((delay >=10) && (delay <15)) {  
			runEvery10Minutes(takeAction)
		} else if ((delay >=15) && (delay <30)) {  
			runEvery15Minutes(takeAction)
		} else {  
			runEvery30Minutes(takeAction)
		}
		takeAction()
	}
    
	// Update rescheduled state
    
	if (!evt) state.poll["rescheduled"] = now()
}

def takeAction() {
	boolean handleException = (handleExceptionFlag)?: false
	log.trace "takeAction>begin"
	Integer delay = givenInterval ?: 5 // By default, do it every 5 min.
	state?.poll["last"] = now()
		
	//schedule the scheduleIfNeeded() function
    
	if (((state?.poll["rescheduled"]?:0) + (delay * 60000)) < now()) {
		log.info "takeAction>scheduling rescheduleIfNeeded() in ${delay} minutes.."
		schedule("0 0/${delay} * * * ?", rescheduleIfNeeded)
		// Update rescheduled state
		state?.poll["rescheduled"] = now()
	}

    
	def MAX_EXCEPTION_COUNT=25
	String exceptionCheck, msg 
	log.trace "takeAction>about to call generateRemoteSensorEvents()"
	ecobee.poll()
	ecobee.generateRemoteSensorEvents("", false)	
	exceptionCheck= ecobee.currentVerboseTrace.toString()
	if (handleException) {            
		if ((exceptionCheck) && ((exceptionCheck.contains("exception") || (exceptionCheck.contains("error")) && 
			(!exceptionCheck.contains("Java.util.concurrent.TimeoutException"))))) {  
			// check if there is any exception or an error reported in the verboseTrace associated to the device (except the ones linked to rate limiting).
			state?.exceptionCount=state.exceptionCount+1    
			log.error "takeAction>found exception/error after calling generateRemoteSensorEvents, exceptionCount= ${state?.exceptionCount}: $exceptionCheck" 
		} else {             
			// reset exception counter            
			state?.exceptionCount=0       
		}
	} /* end if handleException */            
	if (handleException) {            
		if (state?.exceptionCount>=MAX_EXCEPTION_COUNT) {
			// may need to authenticate again    
			msg="too many exceptions/errors, $exceptionCheck (${state?.exceptionCount} errors), you may need to re-authenticate at ecobee..." 
			send "ecobee3RemoteSensorInit> ${msg}"
			log.error msg
		}            
	}    
    
	updateRemoteSensors()

	log.trace "takeAction>end"
}


private def sendNotifDelayNotInRange() {

	send "MyEcobeeRemoteSensorsInit>scheduling delay (${givenInterval} min.) not in range, please restart..."

}

private updateRemoteSensors(evt) {
	log.debug "updateRemoteSensors>evt name=$evt.name, evt.value= $evt.value"

	updateRemoteSensors()
}

private updateRemoteSensors() {
	updateMotionValues()	
	updateTempValues()
//	updateHumidityValues()
}


private updateMotionValues() {

	def ecobeeSensors = ecobee.currentRemoteSensorOccData.toString().minus('[').minus(']').split(",,")
	log.debug "updateMotionValues>ecobeeRemoteSensorOccData= $ecobeeSensors"

	if ((!ecobeeSensors) || (ecobeeSensors==[null])) {

		log.debug "updateMotionValues>no values found"
		return
	}
	for (i in 0..ecobeeSensors.size() - 1) {
		def ecobeeSensorDetails = ecobeeSensors[i].split(',')
		def ecobeeSensorId = ecobeeSensorDetails[0]
		def ecobeeSensorName = ecobeeSensorDetails[1]
		def ecobeeSensorType = ecobeeSensorDetails[2]
		String ecobeeSensorValue = ecobeeSensorDetails[3].toString()

		def dni = [app.id, ecobeeSensorName, getRemoteSensorChildName(), ecobeeSensorId].join('.')

		def device = getChildDevice(dni)

		if (device) {
			log.debug "updateMotionValues>ecobeeSensorId=$ecobeeSensorId"
			log.debug "updateMotionValues>ecobeeSensorName=$ecobeeSensorName"
			log.debug "updateMotionValues>ecobeeSensorType=$ecobeeSensorType"
			log.debug "updateMotionValues>ecobeeSensorValue=$ecobeeSensorValue"

			String status = (ecobeeSensorValue.contains('false')) ? "inactive" : "active"
/*            
			boolean isChange = device.isStateChange(device, "motion", status)
			boolean isDisplayed = isChange
			log.debug "device $device, found $dni,statusChanged=${isChange}, value= ${status}"
*/            

			device.sendEvent(name: "motion", value: status)
		} else {

			log.debug "updateMotionValues>couldn't find device $ecobeeSensorName with dni $dni, probably not selected originally"
		}

	}

}

private updateTempValues() {

	String tempValueString=''    
	Double tempValue    
	def scale = getTemperatureScale()
	def ecobeeSensors = ecobee.currentRemoteSensorTmpData.toString().minus('[').minus(']').split(",,")

	log.debug "updateTempValues>ecobeeRemoteSensorTmpData= $ecobeeSensors"


	if ((!ecobeeSensors) || (ecobeeSensors==[null])) {

		log.debug "updateTempSensors>no values found"
		return
	}

	for (i in 0..ecobeeSensors.size() - 1) {

		def ecobeeSensorDetails = ecobeeSensors[i].split(',')
		def ecobeeSensorId = ecobeeSensorDetails[0]
		def ecobeeSensorName = ecobeeSensorDetails[1]
		def ecobeeSensorType = ecobeeSensorDetails[2]
		def ecobeeSensorValue = ecobeeSensorDetails[3]


		def dni = [app.id, ecobeeSensorName, getRemoteSensorChildName(), ecobeeSensorId].join('.')

		def device = getChildDevice(dni)

		if (device) {

			log.debug "updateTempValues>ecobeeSensorId= $ecobeeSensorId"
			log.debug "updateTempValues>ecobeeSensorName= $ecobeeSensorName"
			log.debug "updateTempValues>ecobeeSensorType= $ecobeeSensorType"
			log.debug "updateTempValues>ecobeeSensorValue= $ecobeeSensorValue"
            
			if (ecobeeSensorValue) {
				if (scale == "F") {
					tempValue = getTemperature(ecobeeSensorValue).round()
					tempValueString = String.format('%2d', tempValue.intValue())            
				} else {
					tempValue = getTemperature(ecobeeSensorValue).round(1)
					tempValueString = String.format('%2.1f', tempValue)
				}
/*
				boolean isChange = device.isTemperatureStateChange(device, "temperature", tempValueString)
				boolean isDisplayed = isChange
*/                
				log.debug "device $device, found $dni, value= ${tempValueString}"

				device.sendEvent(name: "temperature", value: tempValueString, unit: scale)
			}
		} else {
			log.debug "updateTempValues>couldn't find device $ecobeeSensorName with dni $dni, probably not selected originally"
		}

	}

}



private updateHumidityValues() {


	def ecobeeSensors = ecobee.currentRemoteSensorHumData.toString().minus('[').minus(']').split(",,")

	log.debug "updateHumidityValues>ecobeeRemoteSensorHumData= $ecobeeSensors"

	if (ecobeeSensors.size() < 1) {

		log.debug "updateHumidityValues>no values found"
		return
	}

	for (i in 0..ecobeeSensors.size() - 1) {

		def ecobeeSensorDetails = ecobeeSensors[i].split(',')
		def ecobeeSensorId = ecobeeSensorDetails[0]
		def ecobeeSensorName = ecobeeSensorDetails[1]
		def ecobeeSensorType = ecobeeSensorDetails[2]
		def ecobeeSensorValue = ecobeeSensorDetails[3]


		def dni = [app.id, ecobeeSensorName, getRemoteSensorChildName(), ecobeeSensorId].join('.')

		def device = getChildDevice(dni)

		if (device) {

			log.debug "updateHumidityValues>ecobeeSensorId= $ecobeeSensorId"
			log.debug "updateHumidityValues>ecobeeSensorName= $ecobeeSensorName"
			log.debug "updateHumidityValues>ecobeeSensorType= $ecobeeSensorType"
			log.debug "updateHumidityValues>ecobeeSensorValue= $ecobeeSensorValue"
            
			if (ecobeeSensorValue) {
				Double humValue = ecobeeSensorValue.toDouble().round()
				String humValueString = String.format('%2d', humValue.intValue())
/*
				boolean isChange = device.isStateChange(device, "humidity", humValueString)
				boolean isDisplayed = isChange
				log.debug "device $device, found $dni,statusChanged=${isChange}, value= ${humValue}"
*/
				device.sendEvent(name: "humidity", value: humValueString, unit: '%')
			}	
		} else {
				log.info "updateHumidityValues>couldn't find device $ecobeeSensorName with dni $dni, no child device found"
		}	
	}
}


private send(msg) {
	if (sendPushMessage != "No") {
		log.debug("sending push message")
		sendPush(msg)

	}

	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, msg)
	}

	log.debug msg
}

private def getTemperature(value) {
	Double farenheits = value.toDouble()
	if (getTemperatureScale() == "F") {
		return farenheits
	} else {
		return fToC(farenheits)
	}
}


// catchall
def event(evt) {
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}

def cToF(temp) {
	return (temp * 1.8 + 32)
}

def fToC(temp) {
	return (temp - 32) / 1.8
}

def getChildNamespace() {
	"yracine"
}
def getRemoteSensorChildName() {
	"My Remote Sensor"
}