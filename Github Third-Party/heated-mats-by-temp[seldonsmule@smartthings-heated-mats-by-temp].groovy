/**
 *  Heated Mats By Temp
 *
 *  This was created to turn on off a switch based on temperature ranges.  I am using it for portable outside heating mats (snow melt).
 *  Very simple app - but i suck a groovy, so it has to be simple :)
 * 
 *
 *  Copyright 2019 Eric Cunningham
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *
 *  NOTES:
 *
 *  Inital code came from Smartthings "Its to cold" template".  I then modified to turn on and off, have a seasonal pause and a signal light.
 * 
 */
 
 definition(
    name: "Heated Mats By Temp",
    namespace: "seldonsmule",
    author: "seldonsmule",
    description: "Monitor the temp, turn on/off switch within range for heating floor mats",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png",
    pausable: true
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("When the temperature drops below...") {
		input "belowTemperature", "number", title: "Below Temperature?"
	}
    
    section("When the temperature goes above...") {
		input "aboveTemperature", "number", title: "Above Temperature?"
	}


    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }
    
// added the seasonal pause so you don't have to delete the automation in the summer time.  For some causes
// the mats (if that is what you are using this for are removable.  So the act of testing and looking for a switch
// that has been removed makes no sense.
// totally not needed, but nice to have

    section("Seasonal Pause"){
      input "pauseForSummer", "enum", title: "Pause Automation", options: ["Yes", "No"], required: false
    }
    
	section("Turn on a heater...") {
		input "switch1", "capability.switch", required: false
	}
    
    section("Signal light - Turns on to let you know mats are on"){
      input "signalLight", "capability.switch", required: false
    }
}




def installed() {

    if(!checkPause()){
  	  subscribe(temperatureSensor1, "temperature", temperatureHandler)
    }else{
      log.trace "Not subscribing to temperature sensor, in summer pause mode"
    }
    
//    state.matsOn = false // not needed anymore using switch state.
        
}

def updated() {
	unsubscribe()
    
    if(!checkPause()){
  	  subscribe(temperatureSensor1, "temperature", temperatureHandler)
    }else{
      log.trace "Not subscribing to temperature sensor, in summer pause mode"
    }
}


def checkPause(){

  if(pauseForSummer == "Yes"){
    log.debug("Enable Pause for Summer mode")
    return true
  }else{
    log.debug("Disable Pause for Summer mode")
    return false
  }

}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def tooCold = belowTemperature
    def tooWarm = aboveTemperature
	def mySwitch = settings.switch1
    
    def myState = switch1.currentSwitch  // note this WILL NOT WORK if using a virtual switch in simulator
    
    log.debug("Mat Switch is [$myState]")

	if (evt.doubleValue <= tooCold) {
      log.debug("To Cold, checking to see if we already turned on switch")
      if(myState == "off"){
        switch1?.on()
        signalLight?.on() // the ? is some groovy thing for handling null (ie, not set)
        
        log.debug("Turned on [${switch1.displayName}]")
        send("${temperatureSensor1.displayName} is too cold, reporting a temperature of ${evt.value} Turned on [${switch1.displayName}]")

      }else{
        log.debug("Already turned them on")
      }
    } else if (evt.doubleValue >= tooWarm) {
       log.debug("To Warm, checking to see if we already turned of switch")

	   if(myState == "on"){
         switch1?.off()
         signalLight?.off()
         log.debug("Turned off [${switch1.displayName}]")
	     send("${temperatureSensor1.displayName} is too warm, reporting a temperature of ${evt.value} Turned off [${switch1.displayName}]")

       }else{
         log.debug("Already turned them off")
       }
    
    }


}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}
