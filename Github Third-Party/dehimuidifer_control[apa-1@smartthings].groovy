/**
 *  Dehumidifier controls
 *  Controls dehumidifier power
 */
definition(
        name: "Dehumidifier controls",
        namespace: "apa-1",
        author: "alex",
        description: "Turns on and off dehumidifier",
        category: "My Apps",
        iconUrl: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn.png",
        iconX2Url: "http://cdn.device-icons.smartthings.com/Appliances/appliances11-icn@2x.png"
)

preferences {
    section("Temp - Humidity sensor") {
        input("humiditysensor", "capability.sensor", title: "Select the sensor to monitor:", required: true, multiple: false)
    }

    section("DeHumidifier Control") {
        input("humiditypercent", "number", title: "Control at what %", required: true, defaultValue: 60)
        input("humidityshutoff", "number", title: "Turn off at what %", required: true, defaultValue: 58)
        input("dehumidifier", "capability.switch", title: "Select Dehumidifier", required: true, multiple: false)
        input("mintemp", "number", title: "Minimum temperature to run dehumidifier (Default 42)", required: false, multiple: false, defaultValue: 42)
        input("maxtemp", "number", title: "Maximum temperature to run dehumidifier (Default 100)", required: false, multiple: false, defaultValue: 100)
 		input("contactsensor", "capability.contactSensor", title: "Door/Window sensor", multiple: true, required: false)
 }
}

def installed() {
    initialized()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialized()
}

def initialized() {
    subscribe(humiditysensor, "polling", airHandler)
    subscribe(humiditysensor, "refresh", airHandler)
    subscribe(humiditysensor, "humidity", airHandler)
    subscribe(humiditysensor, "temperature", airHandler)

}

def check_open_state() {
    if (contactsensor != null) {
        for (opensensor in contactsensor) {
            if (opensensor.currentValue("contact") == "open") {
                log.debug("$opensensor is open")
                return true
            }
            else {
                log.debug("$opensensor is closed")
            }
        }
    }
    return false
}
def airHandler(evt) {
    def humidity = humiditysensor.currentValue("humidity")
    def temperature = humiditysensor.currentValue("temperature")
    log.debug("Current Humidity is: $humidity")
    log.debug("Current Temperature is: $temperature")
    def currstate = dehumidifier.currentValue("switch")
    log.debug("Power state is: $currstate")
    def isopen = check_open_state()
    log.debug("isopen state: $isopen")
    if (humiditypercent != null && dehumidifier != null) {
       
           if (humidity >= humiditypercent && !isopen) {
                if (temperature >= mintemp || temperature <= maxtemp) {
                    
                    if (currstate == 'off') {
                    	log.debug("Humdifier set to on")
                    	dehumidifier.on()
                    }
                    else {
                    	log.debug("Humdifier is already on")
                    }
                    
                } else {
                    log.debug("Humdifier set to off - temp below 45")
                }
            } 
            else if (humidity >= humidityshutoff && !isopen) {
            // do nothing
            	log.debug("Humdity not less then shutoff - ignoring")
            }
            else {
                
                if (currstate == 'on') {
                		log.debug("Humdifier set to off")
                    	dehumidifier.off()
                    }
                else {
                	log.debug("Humdifier is already off")
                }
            }
        }
  
}

