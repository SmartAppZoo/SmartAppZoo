
/**
 *  Custom Web Services
 *
 *  Copyright 2016 Brendan Sapience
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
    name: "Custom Web Services",
    namespace: "brendanSapience",
    author: "Brendan Sapience",
    description: "Web Services End Points for home",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

// Give access to the Application to multiple devices
preferences {
  	section ("Allow external service to control these switches...") {
    input "switches", "capability.switch", multiple: true, required: true  // DONE
  }
    section ("Allow external service to control these alarms...") {
    input "alarms", "capability.alarm", multiple: true, required: false    // DONE
  }
    section ("Allow external service to control these color enabled devices...") {
    input "colors", "capability.colorControl", multiple: true, required: false // DONE
  }
    section ("Allow external service to control these contact sensors...") {
    input "contacts", "capability.contactSensor", multiple: true, required: false  // DONE
  }
    section ("Allow external service to control these illuminance sensors...") {
    input "illuminances", "capability.illuminanceMeasurement", multiple: true, required: false  // DONE
  }
    section ("Allow external service to control these locks ...") {
    input "locks", "capability.lock", multiple: true, required: false                    // DONE
  }
    section ("Allow external service to control these Motion sensors ...") {
    input "motions", "capability.motionSensor", multiple: true, required: false   // DONE
  }
    section ("Allow external service to control these presence sensors ...") {
    input "presences", "capability.presenceSensor", multiple: true, required: false  // DONE
  }
    section ("Allow external service to control these devices with refreshes ...") {
    input "refreshes", "capability.refresh", multiple: true, required: false           // Not Needed?
  }
    section ("Allow external service to control these temperature sensors ...") {
    input "temperatures", "capability.temperatureMeasurement", multiple: true, required: false  // Not Needed? Returned with MOTIONS
  }
    section ("Allow external service to control these vibration sensors ...") {
    input "shocks", "capability.shockSensor", multiple: true, required: false        // DONE
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

mappings {
//  General Event Retriever
  path("/events/motions") {action: [GET: "getMotionEvents"]}
  path("/events/locks") {action: [GET: "getLockEvents"]} 
  path("/events/lux") {action: [GET: "getLuxEvents"]}  
  path("/events/colors") {action: [GET: "getColorEvents"]}
  path("/events/alarms") {action: [GET: "getAlarmEvents"]}
  path("/events/switches") {action: [GET: "getSwitchEvents"]}
  path("/events/temperatures") {action: [GET: "getTemperatureEvents"]}
  path("/events/contacts") {action: [GET: "getContactEvents"]}
  path("/events/shocks") {action: [GET: "getShockEvents"]}
  
  path("/events/contacts/:id") {action: [GET: "getContactEventsWithID"]}
  path("/events/motions/:id") {action: [GET: "getMotionEventsWithID"]}
  path("/events/locks/:id") {action: [GET: "getLockEventsWithID"]}
  path("/events/lux/:id") {action: [GET: "getLuxEventsWithID"]}
  path("/events/colors/:id") {action: [GET: "getColorEventsWithID"]}
  path("/events/alarms/:id") {action: [GET: "getAlarmEventsWithID"]}
  path("/events/switches/:id") {action: [GET: "getSwitchEventsWithID"]}
  path("/events/temperatures/:id") {action: [GET: "getTemperatureEventsWithID"]}
  path("/events/shocks/:id") {action: [GET: "getShockEventsWithID"]}
  
path("/motions") {
    action: [
      GET: "listMotions"
    ]
  }
// - locks: /locks, /locks/lock or unlock, /locks/:id/lock or unlock
  path("/locks") {
    action: [
      GET: "listLocks"
    ]
  }
  path("/locks/:command") {
    action: [
      PUT: "updateLocks"
    ]
  }
  path("/locks/:id/:command") {
    action: [
      PUT: "updateLock"
    ]
  }

// - colors: /colors, /colors/:command/:parameter, /colors/:id/:command/:parameter
  path("/colors") {
    action: [
      GET: "listColors"
    ]
  }
  path("/colors/:command") {
    action: [
      PUT: "updateColorsNoParm"
    ]
  }
  path("/colors/preset/:color") {
    action: [
      PUT: "updateColorsToPreset"
    ]
  }
  path("/colors/:command/:parameter") {
    action: [
      PUT: "updateColors"
    ]
  }
  path("/colors/:id/:command/:parameter") {
    action: [
      PUT: "updateColor"
    ]
  }

// - alarms: /alarms, /alarms/strobe or siren or both or off, /alarms/:id/strobe or siren or both or off
  path("/alarms") {
    action: [
      GET: "listAlarms"
    ]
  }
  path("/alarms/:command") {
    action: [
      PUT: "updateAlarms"
    ]
  }
  path("/alarms/:id/:command") {
    action: [
      PUT: "updateAlarm"
    ]
  }

// switches. /switches or /switches/on - off or /switches/:id/on - off
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/switches/:id/:command") {
    action: [
      PUT: "updateSwitch"
    ]
  }
  
   // temperatures. /temperatures or /temperatures/:id
  path("/temperatures") {
    action: [
      GET: "listTemperatures"
    ]
  }
  path("/temperatures/:id") {
    action: [
      GET: "listTemperature"
    ]
  }
  
  // motions. /motions or /motions/:id
  path("/motions") {
    action: [
      GET: "listMotions"
    ]
  }
  path("/motions/:id") {
    action: [
      GET: "listMotion"
    ]
  }
  
  // contacts. /contacts or /contacts:id
  path("/contacts") {
    action: [
      GET: "listContacts"
    ]
  }
  path("/contacts/:id") {
    action: [
      GET: "listContact"
    ]
  }
  
  // lux. /lux or /lux/:id
  path("/lux") {
    action: [
      GET: "listLuxes"
    ]
  }
  path("/lux/:id") {
    action: [
      GET: "listLux"
    ]
  }

  // presences. /presences or /presences/:id
  path("/presences") {
    action: [
      GET: "listPresences"
    ]
  }
  
  path("/presences/:id") {
    action: [
      GET: "listPresences"
    ]
  }
  
  // shocks. /shocks or /shocks/:id
  path("/shocks") {
    action: [
      GET: "listShocks"
    ]
  }
  
  path("/shocks/:id") {
    action: [
      GET: "listShock"
    ]
  }
}

def GetAllEvents(things){
    def AllEvents = []
	things.each {
    	//Events += it.eventsSince(new Date() - 1)
      //Events  << [name: it.displayName, id:it.id]
          def Events = it.eventsSince(new Date() - 1)
          for(e in Events){
			AllEvents << [
             date: e.date, devicename: e.displayName, deviceid: e.deviceId,
             name: e.name, value: e.value,
             desc: e.descriptionText, rawdesc: e.description, device: e.device,
             hubid: e.hubId, evtid: e.id //,
            // location: e.location //, locationid: e.locationId, 
            ]
          }
	}
    return AllEvents
}

def GetAllEventsWithID(things){
	def MyId = params.id
    //log.debug "Device ID Retreived: ${MyId}"
    def AllEvents = []
	things.each {
    	//Events += it.eventsSince(new Date() - 1)
      //Events  << [name: it.displayName, id:it.id]
          def Events = it.eventsSince(new Date() - 1)
          for(e in Events){
           //log.debug "Content: ${MyId} : ${e.data}"
          	if(e.deviceId == MyId){  
                AllEvents << [
                
                 date: e.date, 
                 eventname: e.name, 
                 eventvalue: e.value,
                 eventunit: e.unit,
                 change: e.isStateChange,
                 devicename: e.displayName, 
                 deviceid: e.deviceId,
                 source: e.source,
                 desc: e.descriptionText, 
                 rawdesc: e.description, 
                 device: e.device,
                 hubid: e.hubId, 
                 evtid: e.id
                // location: e.location,
                // locationid: e.locationId
                ]
            }
          }
	}
    return AllEvents
}

def getContactEventsWithID(){return GetAllEventsWithID(contacts)}
def getMotionEventsWithID(){return GetAllEventsWithID(motions)}
def getLockEventsWithID(){return GetAllEventsWithID(locks)}
def getLuxEventsWithID(){return GetAllEventsWithID(illuminances)}
def getColorEventsWithID(){return GetAllEventsWithID(colors)}
def getAlarmEventsWithID(){return GetAllEventsWithID(alarms)}
def getSwitchEventsWithID(){return GetAllEventsWithID(switches)}
def getTemperatureEventsWithID(){return GetAllEventsWithID(temperatures)}
def getShockEventsWithID(){return GetAllEventsWithID(shocks)}

def getMotionEvents(){return GetAllEvents(motions)}
def getLockEvents(){return GetAllEvents(locks)}
def getLuxEvents(){return GetAllEvents(illuminances)}
def getColorEvents(){return GetAllEvents(colors)}
def getAlarmEvents(){return GetAllEvents(alarms)}
def getSwitchEvents(){return GetAllEvents(switches)}
def getTemperatureEvents(){return GetAllEvents(temperatures)}
def getContactEvents(){return GetAllEvents(contacts)}
def getShockEvents(){return GetAllEvents(shocks)}

def listLocks() {
    def resp = []
    locks.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("lock")]
    }
    return resp
}

void updateLock() {

    def MyId = params.id
    def command = params.command

    if (command) {
        locks.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
            log.debug "update, request: params: ${params}"
            if(it.id == MyId){
            	it."$command"()
            }
        }
    }
}

void updateLocks() {

    def command = params.command

    if (command) {
        locks.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }
        locks."$command"()
    }
}

def listColors() {
    def resp = []
    colors.each {
      resp << [name: it.displayName, id:it.id, hue: it.currentValue("hue"), saturation: it.currentValue("saturation"), color: it.currentValue("color")]
    }
    return resp
}


void updateColor() {

    def MyId = params.id
    def command = params.command
	def parameter = params.parameter
    
    if (command) {
        colors.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
            log.debug "update, request: params: ${params}"
            if(it.id == MyId){
            	it."$command"(parameter)
            }
        }
    }
}

void updateColorsToPreset(){
 // "/colors/preset/:color" 
  def color = params.color
  def hueColor = -99
  if(color == "pink"){hueColor=83}
  if(color == "purple"){hueColor=75}
  if(color == "blue"){hueColor=60}
  if(color == "green"){hueColor=39}
  if(color == "yellow"){hueColor=25}
  if(color == "orange"){hueColor=10}
  if(color == "red"){hueColor=100}
  
  if(color == "blink"){hueColor=-1}
    //hueColor = 83  //Pink
    //hueColor = 75  //Purple
    //hueColor = 60  //Blue
    //hueColor = 39  //Green
    //hueColor = 25  //Yellow
    //hueColor = 10  //Orange
    //hueColor = 100  //Red

  //colors.setColorTemperature(0)
  if(hueColor > 0){
  	colors.setHue(hueColor)
    colors.setSaturation(90)
  }
  else{
  	  colors.setSaturation(90)
      colors.setHue(100)
      colors.setHue(60)
      colors.setHue(100)
      colors.setHue(60)
      colors.setHue(100)
      colors.setHue(60)
      colors.setHue(100)
  }
  

  
}
void updateColorsNoParm() {

    def command = params.command
    
    if (command) {
        colors.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }
        colors."$command"()
    }
}

void updateColors() {

    def command = params.command
	def parameter = params.parameter
    
    if (command) {
        colors.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }
        colors."$command"(parameter)
    }
}

def listAlarms() {
    def resp = []
    alarms.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("alarm")]
    }
    return resp
}

void updateAlarm() {

    def MyId = params.id
    def command = params.command

    if (command) {
        alarms.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
            log.debug "update, request: params: ${params}"
            if(it.id == MyId){
            	it."$command"()
            }
        }
    }
}

void updateAlarms() {

    def command = params.command

    if (command) {
        alarms.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }
        alarms."$command"()
    }
}

def listShocks() {
    def resp = []
    shocks.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("shock")]
    }
    return resp
}

def listShock() {
 	def resp = []
    def MyId = params.id

    shocks.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, value: it.currentValue("shock")]
    	}
    }
    return resp
}

def listPresences() {
    def resp = []
    presences.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("presence")]
    }
    return resp
}

def listPresence() {
 	def resp = []
    def MyId = params.id

    presences.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, value: it.currentValue("presence")]
    	}
    }
    return resp
}

def listLuxes() {
    def resp = []
    illuminances.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("illuminance")]
    }
    return resp
}

def listLux() {
 	def resp = []
    def MyId = params.id

    illuminances.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, value: it.currentValue("illuminance")]
    	}
    }
    return resp
}

def listContacts() {
    def resp = []
    contacts.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("contact"), temp: it.currentValue("temperature")]
    }
    return resp
}

def listContact() {
 	def resp = []
    def MyId = params.id

    contacts.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, value: it.currentValue("contact"), temp: it.currentValue("temperature")]
    	}
    }
    return resp
}

def listTemperatures() {
    def resp = []
    temperatures.each {
      resp << [name: it.displayName, id:it.id, temp: it.currentValue("temperature")]
    }
    return resp
}

def listTemperature() {
 	def resp = []
    def MyId = params.id

    temperatures.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, temp: it.currentValue("temperature")]
    	}
    }
    return resp
}

def listMotions() {
    def resp = []
    motions.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("motion"), temp: it.currentValue("temperature")]
    }
    return resp
}

def listMotion() {
 	def resp = []
    def MyId = params.id

    motions.each {
    	if(MyId == it.id){
      		resp << [name: it.displayName, id:it.id, value: it.currentValue("motion"), temp: it.currentValue("temperature")]
    	}
    }
    return resp
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {
    def resp = []
    switches.each {
      resp << [name: it.displayName, id:it.id, value: it.currentValue("switch")]
    }
    return resp
}

void updateSwitch() {
    // use the built-in request object to get the command parameter
    def MyId = params.id
    def command = params.command

    if (command) {

        // check that the switch supports the specified command
        // If not, return an error using httpError, providing a HTTP status code.
        switches.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
            log.debug "update, request: params: ${params}"
            if(it.id == MyId){
            	it."$command"()
            }
        }

        // all switches have the comand
        // execute the command on all switches
        // (note we can do this on the array - the command will be invoked on every element
       // switches."$command"()
    }
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command

    if (command) {

        // check that the switch supports the specified command
        // If not, return an error using httpError, providing a HTTP status code.
        switches.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }

        // all switches have the comand
        // execute the command on all switches
        // (note we can do this on the array - the command will be invoked on every element
        switches."$command"()
    }
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers