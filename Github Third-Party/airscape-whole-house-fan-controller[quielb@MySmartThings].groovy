/**
 *  AirScape Whole House Fan Controller
 *
 *  Copyright 2017 Barry Quiel
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
    name: "AirScape Whole House Fan Controller",
    namespace: "quielb",
    author: "Barry Quiel",
    description: "To be filled in later",
    category: "Green Living",
    iconUrl: "https://airscapefans.com/images/site/airscape_logo.png",
    iconX2Url: "https://airscapefans.com/images/site/airscape_logo.png",
    iconX3Url: "https://airscapefans.com/images/site/airscape_logo.png")
	
preferences {
	page(name: "Fan Info", title: "Tell me about the fan", nextPage: "autoStartPage", uninstall: true ) {
		section("Basic Fan info") {
			input( name: "ipAddress", type: "text", title: "Fan IP Address", defaultValue: "192.168.10.134")
            input( name: "maxFanSpeed", type: "number", title: "Maximum fan speed for this model")
		}
    }
    page(name: "autoStartPage", title: "Automatic Start/Stop of WHF", uninstall: true, nextPage: "autoSpeedPage" )
    page(name: "autoSpeedPage", title: "Automatic Speed Control", uninstall: true, install: true)
}

def autoStartPage() {
    dynamicPage(name: "autoStartPage") {
    	section("Automatic Startup") {
        	paragraph "Automatically start the fan when outside temperature is less than the inside temperature" 
            input name: "autoStartFan", type: "bool", title: "Enable auto start"
            input name: "outsideTempSensor", type: "capability.temperatureMeasurement", title: "select Outside temp sensor", hideWhenEmpty: true, multiple: false
            input name: "insideTempSensor", type: "capability.temperatureMeasurement", title: "select Inside temp sensor", hideWhenEmpty: true, multiple: false
            input name: "minOpenWindows", type: "number", title: "Number of open windows to start fan", hideWhenEmpty: "windowWatchList"
            input name: "windowWatchList", type: "capability.contactSensor", title: "Select open windows to watch", hideWhenEmpty: true, multiple: true
            input "autoStartMode", "mode" , title: "Run during specific mode", multiple: true
        }
        section("Automatic Shutoff") {
        	paragraph "Auto shutoff of fan when inside/outside temperature are the same."
        	input(name: "autoStopFan", type: "boolean", title: "Enable auto shutoff", required: true)
            input(name: "autoStopLowTemp", type: "number", title: "Shutoff off low inside temperature", required: false)
        }
    }
}

def autoSpeedPage() {
	dynamicPage(name: "autoSpeedPage") {
    	section(name: "Automatic Speed Control") {
        	paragraph "Automatically speed up or slow down the fan based on the difference between outside and inside temperature"
            input(name: "autoSpeedFan", type: "bool", title: "Enable auto speed control")
            input(name: "autoSpeedDelta", type: "number", title: "Temperature difference to adjust fan speed")
            input "autoSpeedMode", "mode", title: "Adjust speed during specific mode", multiple: true
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    try {
    	def childWHF = addChildDevice("quielb", "AirScape Whole House Fan", convertIPtoHex(settings.ipAddress), location.hubs[0].getId(),
        	[ label: "Whole House Fan", 
        		componentLabel : "Whole House Fan",
        		componentName: "Whole House Fan",
        		completedSetup: true,
        	]
        )
        childWHF.refresh()
    }
    catch(e) {
    	log.debug "Failed to add AirScape WHF ${e}"
    }

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	if( settings.autoSpeedFan ) runEvery30Minutes(autoSpeedAdjust)
    subscribe(outsideTempSensor, "temperature", autoOnOff)
    subscribe(windowWatchList, "contact", autoOnOff)
}

def autoSpeedAdjust() {
   	log.debug "Executing 'autoSpeedAdjust'"
    
    def childWHF = getChildDevice(convertIPtoHex(settings.ipAddress))
    def waitForReturn = childWHF.refresh()
    
    if( childWHF.currentValue("switch") != "on" ) {
    	log.debug "autoSpeedAdjust fan not running... exit"
    }
    else {
    	def outsideTemp = outsideTempSensor.currentTemperature
    	def insideTemp = insideTempSensor.currentTemperature
        def tempDiff = insideTemp - outsideTemp
        def currentSpeed = childWHF.currentValue("fanSpeed").toInteger()

    	if( autoSpeedMode.contains(location.currentMode) ) {
    		if( tempDiff > settings.autoSpeedDelta && currentSpeed < settings.maxFanSpeed ) {
	           	log.debug "autoSpeedControl temperature difference is >= delta of ${settings.autoSpeedDelta} speeding up"
	            sendNotificationEvent("The Whole House Fan is speeding up because of the inside/outside temperature difference.")
	           	childWHF.speedUp()
	        }
        	else if( tempDiff <= settings.autoSpeedDelta && currentSpeed > 1 )  {
           		log.debug "autoSpeedControl temperature difference is not >= delta of ${settings.autoSpeedDelta} slowing down"
            	sendNotificationEvent("The Whole House Fan is slowing down because of the inside/outside temperature difference.")
           		childWHF.speedDown()
        	}
            else {
            	log.debug "autoSpeedControl in butter zone, nothing to adjust."
            }
        }
        else {
        	log.debug "autoSpeedControl current mode not selected for autoSpeedControl"
       }
    }
}

def autoOnOff(evt) {
	log.debug "Executing 'autoOnOff'"
    
    def childWHF = getChildDevice(convertIPtoHex(settings.ipAddress))
    if( childWHF.currentValue("switch") == "on" && !windowsOpenGo() )
    {
    	sendNotificationEvent("The whole house fan is shutting off becuase there aren't enough windows open.")
        childWHF.off()
        return
    }
    
    def outsideTemp = outsideTempSensor.currentTemperature
    def insideTemp = insideTempSensor.currentTemperature
        
    if( childWHF.currentValue("switch") == "on" && settings.autoStopFan ) {
    	if( outsideTemp >= insideTemp ) {
        	log.debug "autoOnOff is turing off the fan"
            sendNotificationEvent("Based on your auto stop preferences, the whole house fan is turning off")
        	childWHF.off()
            return
        }
        if( insideTemp <= settings.autoStopLowTemp ) {
        	log.debug "autoOnOff is turing off the fan because of low temp shutoff"
            sendNotificationEvent("The inside temperature is below the minimum inside temperature, the whole house fan is turning off")
        	childWHF.off()
            return
        }
    }
    else if( childWHF.currentValue("switch") == "off" && settings.autoStartFan ) {
    	if( outsideTemp < insideTemp && insideTemp > settings.autoStopLowTemp ) {
        	if( windowsOpenGo() && autoStartMode.contains(location.currentMode) ) {
            	log.debug "autoOnOff starting fan becuase all conditions met"
                sendNotificationEvent("The Whole House Fan is starting because its cooler outside.")
        		childWHF.on()
            }
            else {
            	log.debug "autoOnOff not starting because windows not open or incorrect mode"
            }
        }
        else {
        	log.debug "autoOnOff not starting because not cool enough outside or too cold inside"
        }
    }
    else {
    	log.debug "autoOnOff nothing to do."
    }
}
    

private windowsOpenGo() {
	def openWindows = windowWatchList.currentContact.findAll { contactVal ->
       	contactVal.contains('open')
    }
    
    log.debug "windowsOpenGo open windows: ${openWindows.size()}, required: ${settings.minOpenWindows}"
    if( openWindows.size() >= settings.minOpenWindows ) {
    	log.debug "windowsOpenGo returning true"
    	return true
    }
    else {
    	log.debug "windowsOpenGo returning false"
    	return false
    }
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {
    	String.format( '%02X', it.toInteger() ) 
    }.join()
    hex += ":0050"
    return hex

}
