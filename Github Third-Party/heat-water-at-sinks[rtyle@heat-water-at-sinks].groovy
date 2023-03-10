// vim: ts=4:sw=4
/**
 *	Heat Water at Sinks
 *
 *	Copyright 2019 Ross Tyler
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
**/

definition(
	name			: 'Heat Water at Sinks',
	namespace		: 'rtyle',
	author			: 'Ross Tyler',
	description		: 'Run a hot water recirculation pump when needed',
	category		: 'Convenience',
	singleInstance	: false,
	iconUrl			: 'https://raw.githubusercontent.com/rtyle/heat-water-at-sinks/master/smartapps/rtyle/heat-water-at-sinks.src/app.png',
	iconX2Url		: 'https://raw.githubusercontent.com/rtyle/heat-water-at-sinks/master/smartapps/rtyle/heat-water-at-sinks.src/app@2x.png',
	iconX3Url		: 'https://raw.githubusercontent.com/rtyle/heat-water-at-sinks/master/smartapps/rtyle/heat-water-at-sinks.src/app@3x.png',
)

def triggersPage() {
	dynamicPage(name: 'triggersPage') {
		for (Integer index = 0; index < count; ++index) {
			section("Trigger $index") {
				input "threshold$index"		, 'number'								, title: 'Temperature Threshold'
				input "temperature$index"	, 'capability.temperatureMeasurement'	, title: 'Temperature Sensor'
				input "switches$index"		, 'capability.switch'					, title: 'Switches'		, multiple: true
			}
		}
	}
}

preferences {
	page(name: 'main', nextPage: 'triggersPage', uninstall: true) {
		section('Home') {
			input 'pump'	, 'capability.switch'	, title: 'Pump'	
			input 'valves'	, 'capability.valve'	, title: 'Valves'	, required: false, multiple: true
			input 'count'	, 'number'				, title: 'Triggers', default: '1', range: '1..9', submitOnChange: 'true'
			label title: 'Assign a name', required: false
		}
	}
	page(name: 'triggersPage', install: true)
}

// return the value of the first closure that returns non-null
private def findValue(List<Closure> closures) {
	def result
	closures.find {result = it(); null != result}
	result
}

// return the value if the predicate() is true; otherwise, null
private def valueIf(value, Closure predicate) {predicate() ? value : null}

// return the first valve
// whose currentValve is closed; otherwise, null
private physicalgraph.app.DeviceWrapper findClosed(physicalgraph.app.DeviceWrapperList valves) {
	valves.find {'closed' == it.currentValve}
}

// return the first temperatureMeasurement
// whose currentTemperature is less than threshold; otherwise, null
private physicalgraph.app.DeviceWrapper findCooler(Number threshold, physicalgraph.app.DeviceWrapper... temperatureMeasurements) {
	temperatureMeasurements.find {threshold > it.currentTemperature}
}

// return the first switch
// whose currentSwitch is on; otherwise, null
private physicalgraph.app.DeviceWrapper findOn(physicalgraph.app.DeviceWrapperList switches) {
	switches.find {'on' == it.currentSwitch}
}

// turn a Switch on or off
// if it is different from the state remembered before.
def setSwitch(physicalgraph.app.DeviceWrapper device, Boolean on) {
	Boolean		newOn = true && on
	String		id = device.id
	Boolean		oldOn = true && state.get(id)
	if (oldOn != newOn) {
		if (newOn)	{device.on()	; log.info "○ ⬅ $oldOn $device"}
		else		{device.off()	; log.info "● ⬅ $oldOn $device"}
		state.put id, newOn
	}
}

private void respond(message) {
	log.info message
	List<Closure> values = []
	values << {valueIf false, {findClosed valves}}
	for (Integer index = 0; index < count; ++index) {
		Integer i = index	// capture
		values << {valueIf true, {findOn(settings["switches$i"]) && findCooler(settings["threshold$i"], settings["temperature$i"])}}
	}
	setSwitch pump, findValue(values)
}

private String getIndent() {/* non-breaking space */ '\u00a0' * 8}

void respondToSwitch(physicalgraph.app.EventWrapper e) {
	respond indent + "⚡ $e.value $e.name $e.device"
}

void respondToTemperature(physicalgraph.app.EventWrapper e) {
	respond indent + "° $e.value $e.name $e.device"
}

void respondToValve(physicalgraph.app.EventWrapper e) {
	respond indent + "◒ $e.value $e.name $e.device"
}

def initialize() {
	subscribe valves, 'valve', respondToValve
	for (Integer index = 0; index < count; ++index) {
		subscribe settings["temperature$index"], 'temperature', respondToTemperature
		subscribe settings["switches$index"], 'switch', respondToSwitch
	}
}

def updated() {
	log.info indent + "✓ updated ${settings}"
	unsubscribe()
	initialize()
}

def installed() {
	log.info indent + "✔ installed ${settings}"
	initialize()
}
