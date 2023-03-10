/**
 *  Send me the Temperature  
 *
 *  Author: Phil Bianco
 *  Date: 2014-03-29
 */

// Automatically generated. Make future change here.
definition(
    name: "Send Me the Temperature",
    namespace: "",
    author: "Phil Bianco",
    description: "Program will capture the Temperature from a sensor and then text is a specific time.",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Select Temperature Device") {
		input name: "tempSensor1", type: "capability.temperatureMeasurement", multiple: false
	}
	section("Send Me the temperature at") {
		input name: "sendTime", title: "Notification Time?", type: "time"
	}
    section("Phone Number to Text") {
        input "phone", "phone", title: "Phone Number", required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    state.temp = 0
	subscribe(tempSensor1, "temperature", temperatureHandler)
    schedule(sendTime, "sendMessage")
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	subscribe(tempSensor1, "temperature", temperatureHandler)
    schedule(sendTime, "sendMessage")
}

def temperatureHandler(evt) {
	log.trace "temperature: ${evt.value}, ${evt}"
    log.debug "Temperature: ${evt.value}"
    state.temp = evt.value
    }
    
def sendMessage() {
	log.debug "State.temp= ${state.temp}"
    def msg="The Temperature in the ${tempSensor1} is ${state.temp}"
    sendPush(msg)
    if (phone){
       sendSms(phone,msg)
       }
	}