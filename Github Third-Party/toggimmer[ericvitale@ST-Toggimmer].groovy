/**
 *  Toggimmer
 *  Version 1.0.0 - 07/07/16
 *
 *  1.0.0 - Initial release
 *
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
 *  Toggimmer is a SmartApp designed to work with wireless dimmers like the Cooper RF9500 (link to device handler below) 
 *  that operater wirelessly only and are not wired into your lights. Toggimmer allows you to select 1 to many dimmers and 
 *  control 1 to many dimmable lights without having to worry about keeping these dimmers in sync with each other or the lights. 
 *  You can 100% replicate the functionality of this SmartApp with something like CoRE. The reason for this apps existance is 
 *  that I felt something as powerful as CoRE was overkill for this kind of function.
 *
 *  You can find this smart app @ https://github.com/ericvitale/ST-Toggimmer
 *  You can find the reference Cooper RF9500 Beast device handler @ https://github.com/ericvitale/ST-CooperRF9500Beast
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
	name: "Toggimmer",
	namespace: "ericvitale",
	author: "ericvitale@gmail.com",
	description: "Toogle (On / Off) & dim wireless dimmers like the Cooper RF9500 that operater wirelessly only and are not wired into your lights.",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	page name: "mainPage"
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
    
    	section("Dimmers") {
			input "dimmers", "capability.switchLevel", title: "Dimmers", multiple: true, required: true
    	}
   	
    	section("Lights") {
	        input "lights", "capability.switchLevel", title: "Lights", multiple: true, required: true
		}
    
	    section([mobileOnly:true], "Options") {
			label(title: "Assign a name", required: false)
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "DEBUG", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
    	}
	}
}

def determineLogLevel(data) {
	if(data.toUpperCase() == "TRACE") {
    	return 0
    } else if(data.toUpperCase() == "DEBUG") {
    	return 1
    } else if(data.toUpperCase() == "INFO") {
    	return 2
    } else if(data.toUpperCase() == "WARN") {
    	return 3
    } else {
    	return 4
    }
}

def log(data, type) {
    
    data = "Toggimmer -- " + data
    
    try {
        if(determineLogLevel(type) >= determineLogLevel(logging)) {
            if(type.toUpperCase() == "TRACE") {
                log.trace "${data}"
            } else if(type.toUpperCase() == "DEBUG") {
                log.debug "${data}"
            } else if(type.toUpperCase() == "INFO") {
                log.info "${data}"
            } else if(type.toUpperCase() == "WARN") {
                log.warn "${data}"
            } else if(type.toUpperCase() == "ERROR") {
                log.error "${data}"
            } else {
                log.error "Toggimmer -- Invalid Log Setting"
            }
        }
    } catch(e) {
    	log.error ${e}
    }
}

def installed() {   
	log("Begin installed.", "DEBUG")
	initalization() 
    log("End installed.", "DEBUG")
}

def updated(){
	log("Begin updated().", "DEBUG")
	unsubscribe()
	initalization()
    log("End updated().", "DEBUG")
}

def initalization() {
	log("Begin intialization().", "DEBUG")
    
	subscribe(dimmers, "switch", switchHandler)
    subscribe(dimmers, "level", levelHandler)
    
    state.sw = [:]
    
    dimmers.each { it->
    	state.sw[it.label] = it.currentValue('level')
        log("Level = ${it.currentValue('level')}.", "DEBUG")
    }
    
    log("End initialization().", "DEBUG")
}

def switchHandler(evt) {
	log("Begin switchHandler(evt).", "DEBUG")
	lights.each { it->
    	if(it.currentValue("switch") == "on") {
        	it.off()
            log("${it.label} -- Turned off.", "INFO")
        } else {
        	it.on()
            log("${it.label} -- Turned on.", "INFO")
        }
    }
	log("End switchHandler(evt).", "DEBUG")
}

def levelHandler(evt) {
	log("Begin levelHandler(evt).", "DEBUG")
    
    if(compareValue(evt.value, "${state.sw[evt.displayName]}")) {
    	log("UP", "INFO")
        state.sw[evt.displayName] = evt.value
        setDimmers("UP")
    } else {
    	log("DOWN", "INFO")
        state.sw[evt.displayName] = evt.value
        setDimmers("DOWN")
    }
    
	log("End levelHandler(evt).", "DEBUG")
}

def setDimmers(direction) {
	log("Begin setDimmers(val)", "DEBUG")
    
    lights.each { it->
        def currentVal = it.currentValue("level")
        def newVal = getNextValue(direction, currentVal)
        it.setLevel(newVal.toInteger())
    }
    
    log("End setDimmers(val)", "DEBUG")
}

def compareValue(newVal, oldVal) {
	return newVal > oldVal
}

def getNextValue(direction, currentValue) {
	log("Begin getNextValue()", "DEBUG")
    
	def result = ""
    
	if(direction.toUpperCase() == "DOWN") {
    	switch(currentValue.toInteger()) {
        	case 100..86:
            	result = "80"
                break
            case 85..66:
            	result = "60"
                break
            case 65..46:
            	result = "40"
                break
            case 45..26:
            	result = "25"
                break
            case 25..21:
            	result = "20"
              	break
            case 20..16:
            	result = "15"
            	break
            case 15..11:
            	result = "10"
            	break
            case 10..6:
            	result = "5"
                break
            case 5..0:
            	result = "0"
                break
            
            default: 
            	result = "0"
        }
        return result
    } else {
    	switch(currentValue.toInteger()) {
        	case 100..80:
            	result = "100"
                break
            case 79..60:
            	result = "80"
                break
            case 59..40:
            	result = "60"
                break
            case 39..25:
            	result = "40"
                break
            case 24..20:
            	result = "25"
              	break
            case 19..15:
            	result = "20"
            	break
            case 14..10:
            	result = "15"
            	break
            case 9..5:
            	result = "10"
                break
            case 4..0:
            	result = "5"
                break
            
            default: 
            	result = "100"
        }
        return result
    }
    
    log("End getNextValue()", "DEBUG")
}