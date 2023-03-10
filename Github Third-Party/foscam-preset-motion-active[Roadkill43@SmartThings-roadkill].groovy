/**
 *  Foscam Presence Alarm
 *
 *  Author: skp19
 *
 */
definition(
    name: "Foscam preset motion active",
    namespace: "roadkill",
    author: "roadkill",
    description: "moves foscam to  preset location and enables motion detection based on sensor",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Cameras") {
		input "cameras", "capability.imageCapture", multiple: false
	}
    section("How long to monitor after Moving to preset") {
        input "cameramon", "boolean", title: "Trigger camera motion monitor", required: true, defaultValue: false
        input "endTime", "number", title: "Number of Minutes, (Default 10)", required: false
    }
     section("Camera burst") {
   input "burston", "boolean", title: "Take burst photo", defaultValue: false
   input "burstCount", "number", title: "How many? (default 5)", defaultValue:5
   }
    section("Sensors and Camera Position") {
        input "preseta", "enum", title: "Sensor A - Preset", metadata: [values: ["1", "2", "3"]], required: false
        input "sensora", "capability.contactSensor", title: "Contact Sensor A", required: false, multiple: true
        input "presetb", "enum", title: "Sensor B - Preset", metadata: [values: ["1", "2", "3"]], required: false
        input "sensorb", "capability.contactSensor", title: "Contact Sensor B", required: false, multiple: true
        input "presetc", "enum", title: "Sensor C - Preset", metadata: [values: ["1", "2", "3"]], required: false
        input "sensorc", "capability.contactSensor", title: "Contact Sensor C", required: false, multiple: true
   }
   section("Notification"){
    input "notify", "boolean", title: "Push notification", defaultValue: false
   
   }
  
   
}

def installed() {
	subscribe(sensora, "contact", sensorAalarm)
    subscribe(sensorb, "contact", sensorAalarm)
    subscribe(sensorc, "contact", sensorAalarm)
}

def updated() {
	unsubscribe()
	subscribe(sensora, "contact", sensorAalarm)
    subscribe(sensorb, "contact", sensorAalarm)
    subscribe(sensorc, "contact", sensorAalarm)

}


def sensorAalarm(evt) {
	def ContactTrigger
   	def FriendlyContactName
    def DisplayName
    DisplayName = evt.displayName
    log.debug DisplayName
      switch (DisplayName) {
        	case sensora.label:
            	ContactTrigger = preseta
                FriendlyContactName = sensora.label
                break
            case sensorb.label:
            	ContactTrigger = presetb
                FriendlyContactName = sensorb.label
                break
            case sensorc.label:
            	ContactTrigger = presetc
                FriendlyContactName = sensorc.label
                break

            	
            }
    log.debug "$FriendlyContactName : $ContactTrigger"
        
    log.debug "${FriendlyContactName} Activity "
    if (evt.value == "open") {
        log.debug "${FriendlyContactName} has opened at ${location}"
     switch (ContactTrigger) {
        	case "1":
            	cameras.preset1()
                break
        	case "2":
            	cameras.preset2()
                break
            case "3":
            	cameras.preset3()
                break
        
        }
        enableAlarm()
        burstPicture()
        sendMessage("${FriendlyContactName} opened, alarm enabled")
    } else {
        log.debug "${FriendlyContactName} closed"
        disableAlarmHandler()
    }
}

def burstPicture() {
	if(burston == "true") {

		cameras.take()
		(1..((burstCount ?: 5) - 1)).each {
			cameras.take(delay: (3000 * it))
		}
    }
}

def sendMessage(msg) {
	if (notify == "true") {
		sendPush msg
	}
}

def enableAlarm() {
    log.debug "Alarm enabled"
    if(cameramon) {
    	cameras.alarmOn()
    }
}

def disableAlarmHandler() {
if(cameramon == "true") {
    def timeToOff
    if(endTime) {
	    timeToOff = endTime * 60
    } else {
		timeToOff = 600
	}
	runIn(timeToOff, disableAlarm)
    }
}

def disableAlarm() {

    log.debug "Alarm off"
    cameras.alarmOff()
}