definition(
    name: "Netatmo Presence Sensor",
    namespace: "AntoineGuilbaud",
    author: "Antoine Guilbaud",
    description: "switch on a virtual switch if the value of a custom attribute is above or equals the specified threshold",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  section("This Netatmo Base Station") {
    input name: "netatmoBaseStation", type: "device.netatmoBasestation", title: "This Netatmo Base Station"
  }
  
    section("This Netatmo Additional Module") {
    input name: "netatmoAdditionalModule", type: "device.netatmoAdditionalModule", title: "This Netatmo Additional Module"
  }
  
  section("Noise Threshold") {
    input name: "noiseThreshold", type: "number", title: "Threshold in dB", required: false
  }

  section("CO2 Threshold") {
    input name: "co2Threshold", type: "number", title: "Threshold in ppm", required: false
  }

  section("This Switch") {
    input name: "theSwitch", type: "capability.switch", title: "switch this switch"
  }
  section("Send Push Notification?") {
        input "sendPush", "bool", required: false,
              title: "Send Notifications?"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    init()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    init()
}

def init()
{
	subscribe(netatmoBaseStation, "noise", handleEvent)
    subscribe(netatmoBaseStation, "carbonDioxide", handleEvent)
    subscribe(netatmoAdditionalModule, "carbonDioxide", handleEvent)
    subscribe(theSwitch, "switch.on", handleEvent)
    subscribe(theSwitch, "switch.off", handleEvent)
}

def handleEvent(evt) 
{
  	def co2 = co2Threshold ?: 400
  	def currCo2a = netatmoBaseStation.currentValue("carbonDioxide").toInteger()
  	def currCo2b = netatmoAdditionalModule.currentValue("carbonDioxide").toInteger()
  	def noise = noiseThreshold ?: 50
  	//log.debug "entering handle event method, evaluating against a threshold of ${co2}"
  
	if(currCo2a >= co2.toInteger())
	{
      	if (currCo2b >= co2.toInteger())
      	{
          	if (theSwitch.currentSwitch == "off")
          	{
              	log.debug("CO2 levels are greater than " + co2 + ", Turning on the switch")        
              	theSwitch.on()
              	def msg = "Humans detected at home: " + currCo2a + " "  + currCo2b
              	log.debug msg
                setLocationMode("Home")
              	if (sendPush)
                {
                  sendPush(msg)
              	}
          	}
      	}
  	}
    else 
    {
        if (currCo2b < co2.toInteger())
        {
            if (theSwitch.currentSwitch == "on")
            {
                log.debug("All CO2 levels are below " + co2 + ", turning off the switch.")
                theSwitch.off()
                def msg = "No Human detected at home: " + currCo2a + " "  + currCo2b
                log.debug msg
                setLocationMode("Away")
                if (sendPush)
                {
                    sendPush(msg)
                }
        	}
    	}
	}
}