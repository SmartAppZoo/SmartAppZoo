/**
 *  Intelligent Entities
 *
 *  Copyright 2015 Timothy Fenton TESTING 123
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
    name: "SmartThings Connector",
    namespace: "venumx",
    author: "Timothy Fenton",
    description: "This is a SmartThings SmartApp that will allow you to connect to smartthings more easily!",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "SmartThings Connector", displayLink: "http://www.timothyfenton.com/smartconnector"])


preferences {
	section("Allow Endpoint to Control These Things...") {
		input "switches", "capability.switch", title: "Which Switches?", multiple: true
        input "locks", "capability.lock", title: "Which Locks?", multiple: true
	}
}

mappings {

	path("/switches") {
		action: [
			GET: "listSwitches"
		]
	}
	path("/switches/:id") {
		action: [
			GET: "showSwitch"
		]
	}
	path("/switches/:id/:command") {
		action: [
			GET: "updateSwitch"
		]
	}
    path("/switches/:id/:command/:value") {
		action: [
			GET: "setSwitchLevel"
		]
	}
	path("/locks") {
		action: [
			GET: "listLocks"
		]
	}
	path("/locks/:id") {
		action: [
			GET: "showLock"
		]
	}
	path("/locks/:id/:command") {
		action: [
			GET: "updateLock"
		]
	}    
    
}

def installed() {}

def updated() {}


//switches
def listSwitches() {
	switches.collect{device(it,"switch")}
}

def showSwitch() {
	show(switches, "switch")
}
def updateSwitch() {
	update(switches)
}
void setSwitchLevel(){
    setlevel(switches)
}

//locks
def listLocks() {
	locks.collect{device(it,"lock")}
}

def showLock() {
	show(locks, "lock")
}

void updateLock() {
	update(locks)
}

def deviceHandler(evt) {}

private void setlevel(devices){
	def command = params.command
    
    if(command)
    {
    	def device = devices.find { it.id == params.id }
        if(!device) {
        	httpError(404, "Device not found")
        } else {
        	if(command == "level")
            {
            	device.setLevel(params.value as int)
            }
        }
    }
}

private update(devices) {
	log.debug "update, request: params: ${params}, devices: $devices.id"
    
    
	//def command = request.JSON?.command
    def command = params.command
    //let's create a toggle option here
	if (command) 
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
        	if(command == "toggle")
       		{
            	if(device.currentValue('switch') == "on")
                {
                  device.off()
                  [status: false]
                  }
                else{
                  device.on()
                  [status: true]
                  }
       		}
       		else
       		{
				device."$command"()
                def returnVal = params.command == "on" ? true : false;
                [status: returnVal]
            }
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, level: device.currentValue("level"), unitTime: s?.date?.time, type: type]
	}
}


private device(it, type) {
	it ? [id: it.id, label: it.label, level: it.currentValue("level"), value: it.currentValue("switch"), type: type] : null
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers
