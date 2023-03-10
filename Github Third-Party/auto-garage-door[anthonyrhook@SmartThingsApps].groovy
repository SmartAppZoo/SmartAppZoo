/**
 *  Auto Garage Door
 *
 *  Copyright 2016 Anthony Hook
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
 */
definition(
    name: "Auto Garage Door",
    namespace: "anthonyrhook",
    author: "Anthony Hook",
    description: "Automatically open my garage door when I return",
    category: "Safety & Security",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@3x.png")

preferences {
	section("Which sensor is this tied to?") {
    	input "presence", "capability.presenceSensor", title: "Presence sensors", required: true, multiple: false
    }
    section("Which garage door is theirs?") {
        input "garageDoor", "capability.garageDoorControl", required: true, title: "Which garage door to open?", multiple: false
    }
    section("Close the garage door when an interior door opens?") {
    	input "houseDoor", "capability.contactSensor", required: false, title: "Interior door", multiple: false
    }
    section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Notify with text message (optional)",
                description: "Phone Number", required: false
        }
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
	subscribe(presence, "presence", garageToggleHandler)
    subscribe(houseDoor, "contact.open", garageCloserHandler)
}

def garageToggleHandler(evt) {
	
    def currentState = garageDoor.currentValue("door")
	log.debug "garageToggleHandler called: $evt"
	log.debug currentState
    
    //Presense detected and the door is closed, open it.
	if("present" == evt.value && currentState == "closed") {
		log.debug "Welcome home, opening $garageDoor."
		garageDoor.open()
	}
    //No presense and the door is open, close it.
	else if ("not present" == evt.value && currentState == "open") {
        log.debug "Bon voyage, closing $garageDoor."
        garageDoor.close()
        //Make sure it's closed after 15 seconds
        //This is currently a bandaid to go back and check after 15 seconds
        def now = new Date()
        def runTime = new Date(now.getTime() + (15 * 1000)) //15 seconds
        runOnce(runTime, checkDoor)
  	}
    else {
		log.debug "I didn't make any changes."
	}
}

def garageCloserHandler(evt) {
	def currentState = garageDoor.currentValue("door")
	log.debug "garageCloserHandler called: $evt"
    if("open" == evt.value && currentState == "open") {
    	log.debug "Welcome home, closing $garageDoor"
        garageDoor.close()
        //Make sure it's closed after 30 seconds
        //This is currently a bandaid to go back and check after 15 seconds
        def now = new Date()
        def runTime = new Date(now.getTime() + (15 * 1000)) //15 seconds
        runOnce(runTime, checkDoor)
    } else {
		log.debug "I didn't make any changes."
	}
}

//current bandaid - I'd rather not have it if I don't have to
//Maybe I'll refactor this sometime in the future.
def checkDoor() {
	def currentState = garageDoor.currentValue("door")
    //log.debug "The garage door is $currentState"
    if (currentState == "open") {
   		log.debug "Sorry, your door didn't close the first time. I'm trying again."
    	garageDoor.close()
    	def now = new Date()
		def runTime = new Date(now.getTime() + (15 * 1000)) //15 seconds
		runOnce(runTime, checkDoor)
      } else {
        def message = "$garageDoor is closed"
        if (location.contactBookEnabled && recipients) {
            log.debug "contact book enabled!"
            sendNotificationToContacts(message, recipients)
        } else {
            log.debug "contact book not enabled"
            if (phone) {
                sendSms(phone, message)
            }
        }
    }
}