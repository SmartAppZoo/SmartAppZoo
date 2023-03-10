/**
 *  SmartMotionProxySwitch
 *
 *  Copyright 2015 Michael Lasevich
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
		name: "SmartMotionProxySwitch",
		namespace: "com.legrig",
		author: "Michael Lasevich",
		description: "Smarter Motion Proxy Switch",
		category: "Convenience",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
		)

preferences {
	section("Switch To Control") {
		input "realswitch", "capability.switch", title: "Switch To Control...", required: true
	}
	section("Motion Sensor") {
		input "sensor", "capability.motionSensor", title: "Motion Sensor", required: true
		input "auto_off", "boolean", title: "Auto Off",  required: true, defaultValue: true
		input "auto_off_delay", "number", title: "Auto Off Delay", required: true, defaultValue: 5
	}
	section("Active Window") {
		input "active_window", "boolean", title: "Active Window", required: true
		input "from_time", "time", title: "From", required: false
		input "to_time", "time", title: "Until", required: false
	}
}

private createChildDevice(String childName){
	log.debug("Creating device: "+ childName)
	def childDevice = getChildDevice(childName)
	if (!childDevice) {
		childDevice = addChildDevice("smartthings", "On/Off Button Tile", childName, null,
				["name": childName, "label": childName, "completedSetup": true])
	}
	return childDevice
}

private createChildDevices(){
	state.conditionalProxy = realswitch.displayName + " Conditional Proxy"
	state.conditionalProxyId = createChildDevice(state.conditionalProxy).id
	state.forcedProxy = realswitch.displayName + " Forced Proxy"
	state.forcedProxyId = createChildDevice(state.forcedProxy).id
	return getChildDevices()
}

private subscribe_all(devices){
	devices.each{
		subscribe(it, "switch.on", switchOnHandler)
		subscribe(it, "switch.off", switchOffHandler)
	}
	subscribe(sensor, "motion.active", motionDetected)
}

def installed() {
	log.debug "Installed"
	if (! getChildDevices() ) createChildDevices()
	subscribe_all(getChildDevices())
	state.isActive = false
}

def updated() {
	log.debug "Updated"
	unsubscribe()
	subscribe_all(getChildDevices())
	//state.isActive = false
}

def switchOnHandler(evt) {
	if (evt.deviceId == state.conditionalProxyId){
		log.debug("Conditional Switch Was Turned On")
		turnOn()
	}else if (evt.deviceId == state.forcedProxyId){
		log.debug("Forced Switch Was Turned On")
		state.isActive = false
		realswitch.on()
	}else{
		log.debug("Unknown Device Id: " + evt.deviceId)
	}
}

def switchOffHandler(evt) {
	if (evt.deviceId == state.conditionalProxyId){
		log.debug("Conditional Switch Was Turned Off")
		turnOff()
	}else if (evt.deviceId == state.forcedProxyId){
		log.debug("Forced Switch Was Turned Off")
		state.isActive = false
		realswitch.off()
	}
}

def isEnabled(){
	def ret = true
	def now = new Date()
	if (active_window){
		def from_time_today = timeToday(from_time, location?.timeZone)
		def to_time_today = timeToday(to_time, location?.timeZone)
		if (to_time > from_time){
			ret = ((now >= from_time_today) && ( now <= to_time_today))
			log.debug(" AA: From: "+from_time+"  Now: "+ now+" Until: "+to_time+" :: " + ret)
		}else{
			ret = ((now <= to_time_today) || ( now >= from_time_today))
			log.debug(" BB: From: "+from_time+"  Now: "+ now+" Until: "+to_time+" :: " + ret)
		}
	}else{
		log.debug(" CC: From: "+from_time+" Until: "+to_time)
	}
	return ret
}

def turnOn(){
	if (! state.isActive){
		state.wasOff = realswitch.currentValue("switch") == "off"
		if(state.wasOff){
			realswitch.on()
			state.isActive = true
		}
		return state.wasOff
	}
	return false
}

def turnOff(){
	if(state.isActive && state.wasOff){
		realswitch.off()
		state.isActive = false
	}
	return state.wasOff
}

def motionDetected(evt) {
	def now=new Date()
	state.motionLastSeen=now.getTime()
	log.debug("Motion last seen: " + now +" ("+state.motionLastSeen+")")
	if (isEnabled()) turnOn()

	if (auto_off) runIn((60*auto_off_delay + 1), checkIfNeedToTerminate)
}


def checkIfNeedToTerminate(evt){
	def now = new Date().getTime()
	def elapsed = ( now - state.motionLastSeen )
	log.debug("Time since last motion: " + elapsed)
	log.debug("Waiting from: " + (auto_off_delay*60)+ " secs" )
	if (elapsed >= (auto_off_delay*60*1000)) {
		log.debug("Time to turn this off...")
		turnOff()
	}else{
		log.debug("Not Yet...")
	}
}


def uninstalled() {
	unsubscribe()
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each { deleteChildDevice(it.deviceNetworkId) }
}

