// vim: ts=4:sw=4
/**
 * Light the Way
 *
 * Copyright 2018 Ross Tyler
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
**/
definition(
	name		: 'Light the Way',
	namespace	: 'rtyle',
	author		: 'Ross Tyler',
	description	: 'Light the way around this location',
	category	: 'Convenience',
	iconUrl		: 'https://raw.githubusercontent.com/rtyle/light-the-way/master/smartapps/rtyle/light-the-way.src/app.png',
	iconX2Url	: 'https://raw.githubusercontent.com/rtyle/light-the-way/master/smartapps/rtyle/light-the-way.src/app@2x.png',
	iconX3Url	: 'https://raw.githubusercontent.com/rtyle/light-the-way/master/smartapps/rtyle/light-the-way.src/app@3x.png',
)

import physicalgraph.app.DeviceWrapper
import physicalgraph.app.EventWrapper

def getContactSensors() {[
	FDCS	: 'Front Door',
	GDCS	: 'Garage Double Door',
	GSCS	: 'Garage Single Door',
	PDCS	: 'Patio Door',
	SPCS	: 'Side Porch',
]}
def getIlluminanceMeasurements() {[
	DWIM	: 'Driveway',
	FPIM	: 'Front Porch',
]}
def getMotionSensors() {[
	DWMS	: 'Driveway',
	FPMS	: 'Front Porch',
	SPMS	: 'Side Porch',
	WWMS	: 'Walkway',
]}
def getSwitchInputs() {[
	DTSI	: 'Daytime',
	VWSI	: 'Visitor Welcome',
]}
def getSwitches() {[
]}
def getSwitchLevels() {[
	BYSL	: 'Back Yard',
	DWSL	: 'Driveway',
	EWSL	: 'Entryway',
	FPSL	: 'Front Porch',
	GCSL	: 'Garage Ceiling',
	PDSL	: 'Patio Door',
	PWSL	: 'Patio Wall',
	SPSL	: 'Side Porch',
]}

// respond according to current state
def respond(message) {
	log.info message
	def daytime			= findOn DTSI
	def visitorWelcome	= findOn VWSI
	setLevel BYSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	DWIM) && !ignoreIlluminance}},
		{valueIf 100, {findOpen			PDCS}},
		{valueIf   5, {visitorWelcome}},
	)
	setLevel DWSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	DWIM) && !ignoreIlluminance}},
		{valueIf 100, {findMotion		DWMS}},
		{valueIf 100, {findOpen			GDCS, GSCS}},
		{valueIf  50, {findMotion		FPMS, SPMS, WWMS}},
		{valueIf  50, {findOpen			FDCS, SPCS}},
		{valueIf   5, {visitorWelcome}},
	)
	setLevel EWSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	FPIM) && !ignoreIlluminance}},
		{valueIf 100, {findMotion		FPMS, WWMS}},
		{valueIf 100, {findOpen			FDCS}},
		{valueIf  25, {findMotion		DWMS}},
		{valueIf  25, {findOpen			GDCS, GSCS}},
	)
	setLevel FPSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	FPIM) && !ignoreIlluminance}},
		{valueIf 100, {findMotion		FPMS, WWMS}},
		{valueIf 100, {findOpen			FDCS}},
		{valueIf  50, {findMotion		DWMS}},
		{valueIf  50, {findOpen			GDCS, GSCS}},
		{valueIf  10, {visitorWelcome}},
	)
	setLevel GCSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	FPIM) && !ignoreIlluminance}},
		{valueIf 100, {findOpen			GDCS, GSCS, SPCS}},
	)
	setLevel PDSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	FPIM) && !ignoreIlluminance}},
		{valueIf 100, {findOpen			PDCS}},
		{valueIf  50, {findMotion		SPMS}},
		{valueIf  50, {findOpen			SPCS}},
		{valueIf  10, {visitorWelcome}},
	)
	setLevel PWSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	FPIM) && !ignoreIlluminance}},
		{valueIf 100, {findOpen			PDCS}},
		{valueIf  50, {findMotion		SPMS}},
		{valueIf  50, {findOpen			SPCS}},
		{valueIf  30, {visitorWelcome}},
	)
	setLevel SPSL, findValue(
		{valueIf   0, {daytime}},
		{valueIf   0, {findBrighter(64,	DWIM) && !ignoreIlluminance}},
		{valueIf 100, {findMotion		SPMS}},
		{valueIf 100, {findOpen			SPCS}},
		{valueIf  50, {findOpen			GDCS, GSCS, PDCS}},
		{valueIf   5, {visitorWelcome}},
	)
}

// set on/off and/or level of a Switch and/or Switch Level capable device
// if it is different from the state remembered before.
def setLevel(DeviceWrapper device, Integer value) {
	// signed integer values encode on or off (positive or not)
	// and level (magnitude)
	int			newValue	= value ?: 0
	boolean		newOn		= 0 < newValue
	int			newLevel	= Math.abs newValue
	String		id			= device.id
	Integer		oldValue	= state.get id
	// if (null == oldValue) {	// default values:
		Boolean	oldOn		= null
		Integer	oldLevel	= null
		boolean	setOn		= true
		boolean	setLevel	= true
	// } else {					// override defaults:
	if (null != oldValue) {
				oldOn		= 0 < oldValue
				oldLevel	= Math.abs oldValue
				setOn		= newOn != oldOn
				setLevel	= newOn && newLevel != oldLevel
	}
	if (setOn || setLevel) {
		boolean hasSwitch		= device.hasCapability 'Switch'
		boolean hasSwitchLevel	= device.hasCapability 'Switch Level'
		if (setOn) {
			if (hasSwitch) {
				if (newOn)	{device.on()	; log.info "○ ⬅ $oldLevel $device"}
				else		{device.off()	; log.info "● ⬅ $oldLevel $device"}
			} else {
				setLevel = true
			}
		}
		if (setLevel && hasSwitchLevel) {
			device.setLevel newLevel		; log.info "◐ $newLevel ⬅ $oldLevel $device"
		}
		state.put id, newOn || null == oldLevel ? newValue : -oldLevel
	}
}

// return the value of the first closure that returns non-null
def findValue(Closure... closures) {
	def result
	closures.find {result = it(); null != result}
	result
}

// return the value if the predicate() is true; otherwise, null
def valueIf(value, Closure predicate) {predicate() ? value : null}

// return the first contactSensor
// whose currentContact is open; otherwise, null
def findOpen(DeviceWrapper... contactSensors) {
	contactSensors.find {'open' == it.currentContact}
}

// return the first illuminanceMeasurement
// whose currentIlluminance is greater than threshold; otherwise, null
def findBrighter(Number threshold, DeviceWrapper... illuminanceMeasurements) {
	illuminanceMeasurements.find {threshold < it.currentIlluminance}
}

// return the first motionSensor
// whose currentMotion is active; otherwise, null
def findMotion(DeviceWrapper... motionSensors) {
	motionSensors.find {'active' == it.currentMotion}
}

// return the first switch
// whose currentSwitch is on; otherwise, null
def findOn(DeviceWrapper... switches) {
	switches.find {'on' == it.currentSwitch}
}

def respondToContact(EventWrapper e) {
	respond(indent + "⚡ $e.value $e.name $e.device")
}

def respondToIlluminance(EventWrapper e) {
	respond(indent + "☼ $e.value $e.name $e.device")
}

def respondToMotion(EventWrapper e) {
	respond(indent + "⚽ $e.value $e.name $e.device")
}

def respondToSwitch(EventWrapper e) {
	respond(indent + "⚡ $e.value $e.name $e.device")
}

def getIndent() {/* non-breaking space */ '\u00a0' * 8}

def logState(DeviceWrapper device) {
	Integer value = state.get device.id
	if (null == value) {
		log.info indent + "❓ null $device"
	} else {
		boolean on = 0 < value
		int level = Math.abs value
		log.info indent + "${on ? '○' : '●'} $level $device"
	}
}

def initialize() {
	if (clearState) {
		// state.clear	// this doesn't work. why? this does:
		(state.keySet() as String[]).each {state.remove it}
	}
	switches	.each {name, title -> logState settings.get(name)}
	switchLevels.each {name, title -> logState settings.get(name)}
	contactSensors.each {name, title ->
		subscribe	settings.get(name)	, 'contact'		, respondToContact}
	illuminanceMeasurements.each {name, title ->
		subscribe	settings.get(name)	, 'illuminance'	, respondToIlluminance}
	motionSensors.each {name, title ->
		subscribe	settings.get(name)	, 'motion'		, respondToMotion}
	switchInputs.each {name, title ->
		subscribe	settings.get(name)	, 'switch'		, respondToSwitch}
	respond(indent + "▶ initialize")
}

preferences {
	section('Flags') {
		input 'clearState', 'bool', title: 'Clear State'
		['Illuminance'].each {name ->
			input 'ignore' + name	, 'bool'								, title: 'Ignore ' + name}
	}
	section('Contact Sensors') {
		contactSensors.each {name, title ->
			input name				, 'capability.contactSensor'			, title: title + ' Contact Sensor'}
	}
	section('Illuminance Measurements') {
		illuminanceMeasurements.each {name, title ->
			input name				, 'capability.illuminanceMeasurement'	, title: title + ' Illuminance Measurement'}
	}
	section('Motion Sensors') {
		motionSensors.each {name, title ->
			input name				, 'capability.motionSensor'				, title: title + ' Motion Sensor'}
	}
	section('Switch Inputs') {
		switchInputs.each {name, title ->
			input name				, 'capability.switch'					, title: title + ' Switch Input'}
	}
	section('Switches') {
		switches.each {name, title ->
			input name				, 'capability.switch'					, title: title + ' Switch'}
		switchLevels.each {name, title	->
			input name				, 'capability.switchLevel'				, title: title + ' Switch Level'}
	}
}

def installed() {
	log.info indent + "✔ installed ${settings}"
	initialize()
}

def updated() {
	log.info indent + "✓ updated ${settings}"
	unsubscribe()
	initialize()
}
