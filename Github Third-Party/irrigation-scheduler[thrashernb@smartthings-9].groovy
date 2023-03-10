/**
*  
* 
*  Irrigation Scheduler SmartApp Smarter Lawn Contoller
**
*  Copyright 2014 Stan Dotson and Matthew Nichols
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
* / Based on code from matt@nichols.name and stan@dotson.info
**/

definition(
    name: "Irrigation Scheduler",
    namespace: "r3dey3",
    author: "Kenny K",
    description: "Schedule sprinklers to run unless there is rain.",
    version: "1",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png"
)

preferences {
	page(name: "schedulePage", title: "Create An Irrigation Schedule", nextPage: "zonePage", uninstall: true) {
        
        section("Preferences") {
        	label name: "title", title: "Name this irrigation schedule...", required: false, multiple: false, defaultValue: "Irrigation Scheduler"
        	input "notificationEnabled", "boolean", title: "Send Push Notification When Irrigation Starts", description: "Do You Want To Receive Push Notifications?", defaultValue: "true", required: false
        }
        
        section {
        	input name:"numZones", type:"number", title:"Number of zones"
        }
        section {
            input (
            name: "wateringDays",
            type: "enum",
            title: "Water on which days?",
            required: false,
            multiple: true, // This must be changed to false for development (known ST IDE bug)
            metadata: [values: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']])
        }

        section("Minimum interval between waterings...") {
            input "days", "number", title: "Days?", description: "minimum # days between watering", defaultValue: "1", required: false
        }

        section("Start watering at what times...") {
            input name: "waterTimeOne",  type: "time", required: true, title: "Turn them on at..."
        }

    }
    
	page(name: "zonePage", title: "Select sprinkler switches", nextPage:"timesPage")
	page(name: "timesPage", title: "Select zone run times", install: true)
}		


def zonePage() {
    dynamicPage(name: "zonePage") {
        section {
            settings.numZones.times { i ->
                def num = i +1 
                input "zone$i", "capability.switch", title: "Zone $num switch"
            }
        }
 	}
}

def timesPage() {
    dynamicPage(name: "timesPage") {
        section ("Length to run:") {
        
            settings.numZones.times { i ->
                def num = i +1 
                def dev = settings["zone${i}"]
                input "zone${i}time", "number", title: "$dev", range: "0..120", default:20
            }
        }
    }
}


def installed() {
    scheduling()
}
def updated() {
    log.trace "updated()"
    unschedule()
    scheduling()
}
// Scheduling
def scheduling() {
	log.debug "Scheduling"
    schedule(waterTimeOne, scheduleCheck)
}

def updateCtrl(newState) {
	def ctrl = getCtrl()
    newState.each {
        ctrl.sendEvent(name: it.key, value: it.value)
    }
	//ctrl.update(newState)
}


def getCtrl() {
	def child = getChildDevice("irgctrl")
    if (child) {
   		return child;
    }
    log.debug "Creating child"
    addChildDevice("r3dey3", "Irrigation Control", "irgctrl", hub, [
        "label": "Irrigation Control"
    ])
    def d = new Date()
    d.clearTime()
    child = getChildDevice("irgctrl")
    updateCtrl(["date":d])
    return child
}

def nextDate() {
	def ctrl = getCtrl()
    def cur = getDate(ctrl)
	cur = cur.plus(1)
    updateCtrl(["date": cur.format("EEE MMM dd yyyy")])
}
def prevDate() {
	def ctrl = getCtrl()
    def cur = getDate(ctrl)
    def today = (new Date()).clearTime()
    if (cur > today) {
    	cur = cur.minus(1)
    }
    updateCtrl(["date": cur.format("EEE MMM dd yyyy")])
}
def getDate(ctrl) {
    def cur = ctrl.currentState("date")?.value
    if (cur) {
    	cur = Date.parse("EEE MMM dd yyyy", cur)
    }
    else {
    	cur = (new Date()).clearTime()
    }
    return cur
}

def scheduleCheck() {
    ctrl.log "Running Irrigation Schedule: ${app.label}"
    def ctrl = getCtrl()
    def today = (new Date()).clearTime()
    def cur = getDate(ctrl)
    
    if (today >= cur && ctrl.currentState("enabled").value == "on" && isWateringDay()) {
        if (isNotificationEnabled) {
        	sendPush("${app.label} Is Watering Now!" ?: "Irrigation schedule is watering")
        }
        def next = today.plus(settings.days)
        //ctrl.log("NEW DATE $next")
        updateCtrl(["date": next.format("EEE MMM dd yyyy")])
        startWatering()
    }
    else {
	    ctrl.log "Not  watering"
        updateCtrl(["state":"scheduled"])
    }
}

def isWateringDay() {
    if(!wateringDays) return true

    def today = new Date().format("EEEE", location.timeZone)
    if (wateringDays.contains(today)) {
        return true
    }
    log.info "${app.label} watering is not scheduled for today"
    return false
}

def startWatering() {
	if (atomicState.currentZone != null && atomicState.currentZone < settings.numZones) {
    	log.debug "Not watering because schedule in progress"
        getCtrl().log "Not watering because schedule in progress"
    	return;
    }
    log.debug "Saving -1 as current zone"
    atomicState.currentZone = -1;
    nextZone();
}
def nextZone() {
	log.trace "nextZone() - ${atomicState.currentZone}"
	def curZone = atomicState.currentZone
    def curDev = settings["zone${curZone}"]
    
    if (curDev?.currentState("switch")?.value == "on") {
    	curDev.off();
        runIn(30, nextZone);
        return;
    }
    
    curZone = curZone + 1
    while (curZone < settings.numZones) {
    	log.debug "Check zone $curZone"
        def dev = settings["zone${curZone}"]
        def t = settings["zone${curZone}time"]
        if (t > 0) {
		    updateCtrl(["state":"watering"])
            dev?.on()
            runIn(t*60, "endZone");
            runIn(30, "ensureOn");
            log.debug("Start watering with ${dev} for $t minutes");
            break;
        }
    	curZone = curZone + 1
	}
    
    if (curZone >= settings.numZones) {
        updateCtrl(["state":"scheduled"])
    }
    atomicState.currentZone = curZone

}
def ensureOn() {
	log.debug "ensureOn() - ${atomicState.currentZone}"
	def curZone = atomicState.currentZone
	def dev = settings["zone${curZone}"]
    if (dev) {
        if (dev.currentState("switch").value != "on") {
            log.debug "Turning $dev on"
            dev.on()
            runIn(30, "ensureOn");
        }
    }
}

def endZone() {
	log.debug "endZone() - ${atomicState.currentZone}"
	def curZone = atomicState.currentZone
	def dev = settings["zone${curZone}"]
    if (dev) {
    	log.debug "Turning $dev off"
		dev.off()
    }
    runIn(60, "nextZone")
}

def doStop() {
	def stopZone = atomicState.stopZone
	def dev = settings["zone${stopZone}"]
    if (dev) {
    	if (dev.currentState("switch").value == "on") {
        	dev.off()
            runIn(10, doStop)
        }
        else {
        	updateCtrl(["state":"scheduled"])
        }
    }
    else {
    	updateCtrl(["state":"scheduled"])
    }
}

def stop() {
	def curZone = atomicState.currentZone
    atomicState.stopZone = curZone
	atomicState.currentZone = settings.numZones
	doStop();
}
def start() {
	atomicState.currentZone = 10000;
	scheduleCheck();
}