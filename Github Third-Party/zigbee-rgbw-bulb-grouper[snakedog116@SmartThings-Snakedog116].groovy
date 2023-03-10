/**
 *  ZigBee RGBW Bulb Grouper
 *
 *  Copyright 2016 Joshua Moore
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

import groovy.json.JsonSlurper

definition(
    name: "ZigBee RGBW Bulb Grouper",
    namespace: "joshua-moore",
    author: "Joshua Moore",
    description: "Make a group of ZigBee RGBW Bulbs respond to a master control (virtual).",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Using this ...") {
    	input "master", "capability.colorControl", multiple: false, required: true, title: "Master device"
	}
    section("Control these ...") {
    	input "slaves", "capability.colorControl", multiple: true, required: true, title: "Slave devices"
    }
}

def installed() {
	// log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	// log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
	subscribe(master, "level", setLevelHandler)
    subscribe(master, "colorTemperature", setColorTempHandler)
    subscribe(master, "color", setColorHandler)
}
def onHandler(evt) {
	slaves.on()
}

def offHandler(evt) {
	slaves.off()
}

def setLevelHandler(evt) {
	def level = evt.value.toFloat()
	level = level.toInteger()
	slaves.setLevel(level)
}

def setColorTempHandler(evt) {
	def temp = evt.value.toFloat()
	temp = temp.toInteger()
	slaves.setColorTemperature(temp)
}

def setColorHandler(evt) {
	log.debug "${(new Date())} Setting color using event data: ${evt.value}"
	log.debug "${(new Date())} DATA contains: ${evt.data}"
	
    def jsonSlurper = new JsonSlurper()
    def payload = jsonSlurper.parseText(evt.data)
    slaves.setColor(payload)
    
    /*
    // inbound data is going to be a hex value, so we need hue, sat, and level from it
    def hsb = hexToHSB(evt.value)
	log.debug "HSB: ${hsb}"
	def level = hsb.b.toFloat()
	level = level.toInteger()
	log.debug "Level: ${level}"
	*/
	/*
   	def dimLevel = master.currentValue("level")
    def hueLevel = master.currentValue("hue")
    def saturationLevel = master.currentValue("saturation")
	def newValue = [hue: hueLevel, saturation: saturationLevel, level: dimLevel as Integer]
    log.debug "New value is: ${newValue}"
    slaves?.setColor(newValue)
	*/
	// not sure if the above values are valid, that's currently my issue. I think ...

	// not even sure if this is right, but it's worth a shot ...
    // slaves.on() + slaves.setLevel(level) + "delay 300" + slaves.setHue(hsb.h) + "delay 300" + slaves.setSaturation(hsb.s)
    
    // another attempt using setColor instead (per the docs)
    // slaves.setColor([hue: hsb.h, satuation: hsb.s, switch: 'on', hex: evt.value, level: level])

	// and yet another, from logging the actual OSRAM device changes
    // level is currently always set to 100 until I can figure out how to sync it with the master
	// slaves.setColor([level: 100, red: hsb.red, hex: evt.value, saturation: hsb.s, blue: hsb.blue, green: hsb.green, hue: hsb.h, alpha: (level / 100)])
}

/*
def rgbToHSB(rgb) {
	def r = rgb.r
	def g = rgb.g
	def b = rgb.b
	float hue
    float saturation
    float brightness

	float cmax = (r > g) ? r : g
	if (b > cmax) cmax = b
	float cmin = (r < g) ? r : g
	if (b < cmin) cmin = b

	brightness = cmax / 255
	if (cmax != 0) saturation = (cmax - cmin) / cmax
	else saturation = 0
		
	if (saturation == 0) hue = 0
	else hue = 0.60 * ((g - b) / (255 -  cmin)) % 360

	return [red: r, green: g, blue: b, h: hue * 100, s: saturation * 100, b: brightness * 100]
}

def hexToHSB(value) {
	value = value.replaceAll("#", "")
	def hexRed = value.substring(0, 2)
	def hexGreen = value.substring(2, 4)
	def hexBlue = value.substring(4, 6)

	int red = Integer.parseInt(hexRed, 16)
	int green = Integer.parseInt(hexGreen, 16)
	int blue = Integer.parseInt(hexBlue, 16)

	rgbToHSB([r: red, g: green, b: blue])
}
*/