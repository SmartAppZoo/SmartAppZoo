/**
 *  Flashing-child SmartApp for SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  v1.6 / 2020-06-19 - Added switch actuator
 *  v1.5 / 2020-05-03 - Added Water Sensor
 *	v1.4 / 2020-02-20 - Added Presence Sensor
 *  v1.3 / 2020-01-27 - Added presence condition (Home, Away)
 *  v1.2 / 2020-01-20 - Dynamic preferences
 *  v1.1 / 2019-11-05 - Added time conditions (always, day, night and custom)
 *  v1.0 / 2019-10-15 - Initial Release
 */

//Definition - The defintion section of the SmartApp specifies the name of the app along with other information that identifies and describes it.

definition(
    name: "Flashing-child",
    namespace: "mST/child",
    author: "Mihail Stanculescu",
    description: "Flashing light",
    category: "My Apps",
    parent: "mST/parent:Flashing",
    iconUrl: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/flashing-light-bulb.png",
    iconX2Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/flashing-light-bulb.png",
    iconX3Url: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/flashing-light-bulb.png"
)

//Preferences - The preferences section is responsible for defining the screens that appear in the mobile app when a SmartApp is installed or updated.

preferences {
	page(name: "triggerpage")
	page(name: "actuatorpage")
	page(name: "settingspage")
	page(name: "timepage")
}

def triggerpage() {
	dynamicPage(name: "triggerpage", title: " ", nextPage: "actuatorpage", uninstall: true){
    	section([title:"Name of child app", mobileOnly:true]) {
			label title:"Assign a name for child app", required:true
		}
        section("When any of the following devices trigger...") {
    	}
        section(hideWhenEmpty: true, " "){
			input "accelerationTrigger", "capability.accelerationSensor", title: "Acceleration Sensor", required: false, multiple: true, submitOnChange: true
		}
        if (accelerationTrigger) {
            section("") {
            	input "accelerationValue", "enum", title: " ", required: true, multiple:false, options: ["active","inactive"], defaultValue: "active"
            }
        }
        section(hideWhenEmpty: true, " "){
			input "buttonTrigger", "capability.button", title: "Button", required: false, multiple: true, submitOnChange: true
		}
        if (buttonTrigger) {
            section("") {
            	input "buttonValue", "enum", title: " ", required: true, multiple:false, options: ["pushed","held"], defaultValue: "pushed"
            }
        }
        section(hideWhenEmpty: true, " "){
        	input "contactTrigger", "capability.contactSensor", title: "Contact Sensor", required: false, multiple: true, submitOnChange: true
		}
        if (contactTrigger) {
            section("") {
            	input "contactValue", "enum", title: " ", required: true, multiple:false, options: ["open","closed"], defaultValue: "open"
            }
        }
        section(hideWhenEmpty: true, " "){
        	input "motionTrigger", "capability.motionSensor", title: "Motion Sensor", required: false, multiple: true, submitOnChange: true
		}
        if (motionTrigger) {
            section("") {
            	input "motionValue", "enum", title: " ", required: true, multiple:false, options: ["active","inactive"], defaultValue: "active"
            }
        }
        section(hideWhenEmpty: true, " "){
        	input "presenceTrigger", "capability.presenceSensor", title: "Presence Sensor", required: false, multiple: true, submitOnChange: true
		}
        if (presenceTrigger) {
            section("") {
            	input "presenceValue", "enum", title: " ", required: true, multiple:false, options: ["present","not present"], defaultValue: "present"
            }
        }
        section(hideWhenEmpty: true, " "){
			input "switchTrigger", "capability.switch", title: "Switch", required: false, multiple: true, submitOnChange: true
		}
        if (switchTrigger) {
            section("") {
            	input "switchValue", "enum", title: " ", required: true, multiple:false, options: ["on","off"], defaultValue: "on"
            }
        }
        section(hideWhenEmpty: true, " "){
			input "windowShadeTrigger", "capability.windowShade", title: "Window Shade", required: false, multiple: true, submitOnChange: true
		}
        if (windowShadeTrigger) {
            section("") {
            	input "windowShadeValue", "enum", title: " ", required: true, multiple:false, options: ["open","closed"], defaultValue: "open"
            }
        }
        section(hideWhenEmpty: true, " "){
			input "waterSensorTrigger", "capability.waterSensor", title: "Water Sensor", required: false, multiple: true, submitOnChange: true
		}
        if (waterSensorTrigger) {
            section("") {
            	input "waterSensorValue", "enum", title: " ", required: true, multiple:false, options: ["dry","wet"], defaultValue: "dry"
            }
        }
    }
}

def actuatorpage() {
	dynamicPage(name: "actuatorpage", title: " ", nextPage: "settingspage"){
    	section(hideWhenEmpty: true, "These switches flashing..."){
            input "switches", "capability.switch", title: " ", required: false, multiple: true, submitOnChange: false
        }
        section(hideWhenEmpty: true, "These dimmers flashing..."){
            input "dimmers", "capability.switchLevel", title: " ", required: false, multiple: true, submitOnChange: true
        }
        if (dimmers) {
            // Do something here like update a message on the screen, or introduce more inputs. "submitOnChange" will refresh the page and allow the user to see the changes immediately.
            section("") {
            	input "dimmerlevel", "number", title: "Level", description: "1...100", required: false, multiple:false, range: "1..100", defaultValue: "50"
            }
        }
        section(hideWhenEmpty: true, "These bulbs flashing..."){
            input "bulbs", "capability.colorControl", title: " ", required: false, multiple: true, submitOnChange: true
		}
        if (bulbs) {
            // Do something here like update a message on the screen, or introduce more inputs. "submitOnChange" will refresh the page and allow the user to see the changes immediately.
            section("") {
            	input "bulbcolor", "enum", title: "Color", required: false, multiple:false, options: [
					["Soft White":"Soft White - Default"],
					["White":"White - Concentrate"],
					["Daylight":"Daylight - Energize"],
					["Warm White":"Warm White - Relax"],
					"Red","Orange","Yellow","Green","Cyan","Blue","Purple"],
                    defaultValue: "Soft White", image: "https://raw.githubusercontent.com/stanculescum/aplicatii-smarthome/master/pictures/color-icon.png"
                input "bulblevel", "number", title: "Level", description: "1...100", required: false, multiple:false, range: "1..100", defaultValue: "50"
            }
        }
    }
}

def settingspage() {
	dynamicPage(name: "settingspage", title: " ", nextPage: "timepage"){
    	section("Settings..."){
			input "numFlashes", "number", title: "This number of times (default 3)", defaultValue: "3", required: false
        	input "onFor", "number", title: "On for (min. 3s - default 3s)", defaultValue: "3", required: false
			input "offFor", "number", title: "Off for (min. 1s - default 1s)", defaultValue: "1", required: false
		}
    }
}

def timepage() {
	dynamicPage(name: "timepage", title: " ", install: true, uninstall: true){
    	section("Only") {
      		input "conditions", "enum", title: "When?", options: ["always":"Always", "sunrise":"Sunrise to Sunset", "sunset":"Sunset to Sunrise", "custom":"Custom time", "presence": "Presence"], defaultValue: "always", submitOnChange: true
      		switch(conditions) {
        		case "always":
          		break
        		case "sunrise":
          		break
        		case "sunset":
                break
                case "custom":
				break
                case "presence":
				break
      		}
    	}
        if (conditions) {
            switch(conditions) {
        	case "custom":
            	section("OPTION: only for Custom time"){
    				input "from", "time", title: "From", required: false
					input "until", "time", title: "Until", required: false
        		}
            }
    	}
        if (conditions) {
            switch(conditions) {
        	case "presence":
            	section("Presence"){
    				input "userpresence", "capability.presenceSensor", title: "Presence Sensor", required: false, multiple: true, submitOnChange: true
				}
        		if (userpresence) {
            		section("") {
            			input "userpresenceValue", "enum", title: " ", required: true, multiple:false, options: ["present":"Home","not present":"Away"], defaultValue: "present"
            		}
        		}
            }
    	}
    }
}

//Pre-defined callbacks - The following methods, if present, are automatically called at various times during the lifecycle of a SmartApp:
//Called when a SmartApp is first installed.
def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe()
}
//Called when the preferences of an installed smart app are updated.
def updated() {
	log.debug "Updated with settings: ${settings}"
    
	unsubscribe()
    unschedule()
	subscribe()
}

def subscribe() {
	if (accelerationTrigger) {
		subscribe(accelerationTrigger, "acceleration", accelerationHandler)
	}
    if (buttonTrigger) {
		subscribe(buttonTrigger, "button", buttonHandler)
	}
	if (contactTrigger) {
		subscribe(contactTrigger, "contact", contactHandler)
	}
	if (motionTrigger) {
		subscribe(motionTrigger, "motion", motionHandler)
	}
    if (presenceTrigger) {
		subscribe(presenceTrigger, "presence", presenceHandler)
	}
    if (switchTrigger) {
		subscribe(switchTrigger, "switch", switchHandler)
	}
    if (windowShadeTrigger) {
		subscribe(windowShadeTrigger, "windowShade", windowShadeHandler)
	}
    if (waterSensorTrigger) {
		subscribe(waterSensorTrigger, "water", waterSensorHandler)
	}
}

//Event Handlers

def accelerationHandler(evt) {
	log.debug "acceleration $evt.value"
	if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == accelerationValue) {
		flashLights()
	}
}

def buttonHandler(evt) {
	log.debug "button $evt.value"
	if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == buttonValue) {
		flashLights()
	}
}

def contactHandler(evt) {
	log.debug "contact $evt.value"
	if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == contactValue) {
		flashLights()
	}
}

def motionHandler(evt) {
	log.debug "motion $evt.value"
	if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == motionValue) {
		flashLights()
	}
}

def presenceHandler(evt) {
	log.debug "presence $evt.value"
	if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == presenceValue) {
		flashLights()
	}
}

def switchHandler(evt) {
	log.debug "switch $evt.value"
    if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == switchValue) {
		flashLights()
	}
}

def windowShadeHandler(evt) {
	log.debug "windowShade $evt.value"
    if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == windowShadeValue) {
		flashLights()
	}
}

def waterSensorHandler(evt) {
	log.debug "water $evt.value"
    if (!checkConditions()) {
    	log.debug("Conditions met")
    	return
  	}
	if (evt.value == waterSensorValue) {
		flashLights()
	}
}

//==========

private def checkConditions() {
  switch(conditions) {
	case "always":
    	return true
	case "sunset":
    	def day = getSunriseAndSunset()
      	return timeOfDayIsBetween(day.sunset, day.sunrise, new Date(), location.timeZone)
    case "sunrise":
      	def night = getSunriseAndSunset()
      	return timeOfDayIsBetween(night.sunrise, night.sunset, new Date(), location.timeZone)
    case "custom":
      	return timeOfDayIsBetween(from, until, new Date(), location.timeZone)
    case "presence":
    	if (userpresence.find{it.currentPresence == userpresenceValue}){
    	return true
        }
  }
}

private flashLights() {
	
    def hueColor = 15
	def saturation = 0
    
    switch(bulbcolor) {
		case "White":
			hueColor = 15
			saturation = 0
			break;
		case "Daylight":
			hueColor = 50
			saturation = 85
			break;
		case "Soft White":
			hueColor = 20
			saturation = 30
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80
			break;
		case "Blue":
			hueColor = 67
            saturation = 100
			break;
		case "Green":
			hueColor = 33
            saturation = 100
			break;
		case "Yellow":
			hueColor = 17
            saturation = 100
			break;
		case "Orange":
			hueColor = 6
            saturation = 100
			break;
		case "Purple":
			hueColor = 83
            saturation = 100
			break;
        case "Cyan":
			hueColor = 50
            saturation = 100
			break;
		case "Red":
			hueColor = 1
            saturation = 100
			break;
	}

	state.previous = [:]

	bulbs.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation")
		]
	}

	log.debug "current values = $state.previous"

	def newValue = [hue: hueColor, saturation: saturation]
	log.debug "new value = $newValue"
	bulbs*.setColor(newValue)
    
    def blevel = bulblevel
    log.debug "new level = $blevel"
    bulbs*.setLevel(blevel)
    
    def dlevel = dimmerlevel
    log.debug "new level = $dlevel"
    dimmers*.setLevel(dlevel)
    
    def doFlash = true
	def onFor = onFor * 1000 ?: 3000
	def offFor = offFor * 1000 ?: 3000
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	def dflash = dimmers
	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = dflash.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			dflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			dflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
    
    def sflash = switches
	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = sflash.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			sflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			sflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
   
	def bflash = bulbs
	if (doFlash) {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = bflash.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times {
			log.trace "Switch on after  $delay msec"
			bflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.on(delay: delay)
				}
				else {
					s.off(delay:delay)
				}
			}
			delay += onFor
			log.trace "Switch off after $delay msec"
			bflash.eachWithIndex {s, i ->
				if (initialActionOn[i]) {
					s.off(delay: delay)
				}
				else {
					s.on(delay:delay)
				}
			}
			delay += offFor
		}
	}
}
