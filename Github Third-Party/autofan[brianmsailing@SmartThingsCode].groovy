/**
 *  autofan
 *
 *  Copyright 2016 Brian Murphy
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
    name: "autofan",
    namespace: "brianmsailing",
    author: "Brian Murphy",
    description: "auto furnace fan",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {	page(name: "selectThermostats", title: "Thermostats", install: false, uninstall: true, nextPage: "selectProgram") {
	
				section("Set the ecobee thermostat(s)") {
			input "ecobee", "device.myEcobeeDevice", title: "Which ecobee thermostat(s)?", multiple: true

		
                   	input(
                        name			: "tStat"
                        ,title			: "Main Thermostat"
                        ,multiple		: false
                        ,required		: true
                        ,type			: "capability.thermostat"
                        ,submitOnChange	: false
                    )
					input(
            			name			: "tempSensor1"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
						input(
            			name			: "tempSensor2"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    					input(
            			name			: "tempSensor3"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    					input(
            			name			: "tempSensor4"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    					input(
            			name			: "tempSensor5"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    input(
            			name			: "tempSensor6"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                          input(
            			name			: "TempSpan"
                		,title			: "Temp Span Output to Virtual Switch"
                		,multiple		: true
                		,required		: false
                		,type			: "capability.switchLevel"
                        ,submitOnChange	: false
            		)
                    
        input "button", "capability.switch", required: true,
              title: "Which switch?"
    
               	input "starttime", "time", title: "Start time", required: true
        input "endtime", "time", title: "End time", required: true
        input "initializestate", "bool", title: "initializestate", required:true

}
}

	page(name: "selectProgram", title: "Ecobee Programs", content: "selectProgram")
    	page(name: "Notifications", title: "Notifications Options", install: true, uninstall: true) {
		section("Notifications") {
			input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes", "No"]], required:
				false
			input "phone", "phone", title: "Send a Text Message?", required: false
		}
        	section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}



def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
unschedule()
	unsubscribe()
	initialize()
}
def climateListHandler(evt) {
	log.debug "thermostat's Climates List: $evt.value, $settings"
}

def setClimateNow(evt) {
	setClimate()
}

def setClimateon() {
def mainState = tStat.currentValue("thermostatOperatingState")

		if (mainState == "idle"){
		def climateName = (givenClimate ?: 'Home').capitalize()
		state.fanon= true
		ecobee.setThisTstatClimate(climateName)
		log.debug"ecobeeSetClimate>set ecobee thermostat(s) to ${climateName} program as requested"
	}else {log.debug "furnace not idle will try again in 1 min"
    runIn(60, setClimateon)
    }
    
}

def setClimateBasement() {
def mainState = tStat.currentValue("thermostatOperatingState")

		if (mainState == "idle"){
		def climateName = (Basement ?: 'Basement').capitalize()
		state.fanon= true
		ecobee.setThisTstatClimate(climateName)
		log.debug"ecobeeSetClimate>set ecobee thermostat(s) to ${climateName} program as requested"
          def message = "ecobeeSetClimate>set ecobee thermostat(s) to ${climateName} program as requested"
  
        sendPush(message)
	}else {log.debug "furnace not idle will try again in 1 min"
    runIn(60, setClimateBasement)
    }
    
}

def setClimateoff() {
	//def climateName = (givenClimateoff ?: 'Home').capitalize()
		state.fanon= false
		ecobee.resumeThisTstat()
		log.debug"ecobeeSetClimate>set ecobee thermostat(s) to resume as requested"
	
}



def initialize() {
	subscribe(ecobee, "climateList", climateListHandler)
   subscribe(button, "switch.on", buttonHandler)
schedule(starttime, swon)
schedule(endtime, swoff)
state.on = initializestate
state.fanon = false
// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "Installed with settings: ${settings}"
	//subscribe(tStat, "thermostatMode", main)
    //subscribe(tStat, "thermostatFanMode", main)
   // subscribe(tStat, "thermostatOperatingState", main)
   // subscribe(tStat, "heatingSetpoint", main)
   // subscribe(tStat, "coolingSetpoint", main)
    //tempSensors
    subscribe(tempSensor1, "temperature", main)
   // subscribe(tempSensor2, "temperature", main)
  //  subscribe(tempSensor3, "temperature", main)
  //  subscribe(tempSensor4, "temperature", main)
  //  subscribe(tempSensor5, "temperature", main)
    
    
    state.mainState = state.mainState ?: tStat.currentValue("thermostatOperatingState")
    state.mainMode = state.mainMode ?: tStat.currentValue("thermostatMode")
    state.mainFan = state.mainFan ?: tStat.currentValue("thermostatFanMode")
    state.mainCSP = state.mainCSP ?: tStat.currentValue("coolingSetpoint").toFloat()
    state.mainHSP = state.mainHSP ?: tStat.currentValue("heatingSetpoint").toFloat()
    state.tempSensor1 = state.tempSensor1 ?:tempSensor1.currentValue("temperature").toFloat()
    state.tempSensor2 = state.tempSensor2 ?:tempSensor2.currentValue("temperature").toFloat()
    state.tempSensor3 = state.tempSensor3 ?:tempSensor3.currentValue("temperature").toFloat()
    state.tempSensor4 = state.tempSensor4 ?:tempSensor4.currentValue("temperature").toFloat()
    state.tempSensor5 = state.tempSensor5 ?:tempSensor5.currentValue("temperature").toFloat()
    state.tempSensor6 = state.tempSensor6 ?:tempSensor6.currentValue("temperature").toFloat()

}

def main (evt){

def mainState = tStat.currentValue("thermostatOperatingState")
def    mainMode = tStat.currentValue("thermostatMode")
def    mainFan = tStat.currentValue("thermostatFanMode")
def    mainCSP = tStat.currentValue("coolingSetpoint").toFloat()
def    mainHSP = tStat.currentValue("heatingSetpoint").toFloat()
def    tempSensor1 = tempSensor1.currentValue("temperature").toFloat()
def    tempSensor2 = tempSensor2.currentValue("temperature").toFloat()
def    tempSensor3 = tempSensor3.currentValue("temperature").toFloat()
def    tempSensor4 = tempSensor4.currentValue("temperature").toFloat()
def    tempSensor5 = tempSensor5.currentValue("temperature").toFloat()
def    tempSensor6 = tempSensor6.currentValue("temperature").toFloat()

//log.debug "temp sensor 1 ${tempSensor1}"
//log.debug "temp sensor 2 ${tempSensor2}"
//log.debug "temp sensor 3 ${tempSensor3}"
//log.debug "temp sensor 4 ${tempSensor4}"
//log.debug "temp sensor 5 ${tempSensor5}"
def temp = [tempSensor1,tempSensor2,tempSensor3,tempSensor4,tempSensor5,tempSensor6]
def max = temp.max()
def min = temp.min()
TempSpan.setLevel((max-min).round(3))
log.info "Temp Span ${(max-min).round(3)}, max ${max}, min${min}"
//log.debug "tempaverage ${tempaverage}"
if (state.on == true){
if (mainMode == "heat"){

if (min+3.0< mainHSP || max-min >3.0){

log.debug "Zone temp min ${min} < 3.0 from main heat set point ${mainHSP} or temp span ${(max-min).round(3)} >3.0 fan on"
if (state.fanon == false){
setClimateon()
}
if (state.fanon == true){
log.debug "nothing to do fan on"
}
}else {log.debug "Zone temp min ${min} > 3.0 from main heat set point ${mainHSP} or temp span ${(max-min).round(3)} <3.0 fan off"
if (state.fanon == true){
setClimateoff()
}
if (state.fanon == false){
log.debug "nothing to do fan off"
}

}

}
}else {log.debug "state off"}
}

def buttonHandler(evt) {
log.debug "basement zone mode"
setClimateBasement()
runIn(60*60, setClimateoff)
}


def swon(){
state.on = true
}

def swoff(){
setClimateoff()
state.on = false
}

def selectProgram() {
	def ecobeePrograms = ecobee.currentClimateList.toString().minus('[').minus(']').tokenize(',')
	log.debug "programs: $ecobeePrograms"


	return dynamicPage(name: "selectProgram", title: "Select Ecobee Program", install: false, uninstall: true, nextPage:
		"Notifications") {
		section("Select Program for fan on") {
			input "givenClimate", "enum", title: "Which program?", options: ecobeePrograms, required: true
		}
        section("Select Program for fan off") {
			input "givenClimateoff", "enum", title: "Which program?", options: ecobeePrograms, required: true
		}
          section("Select Program for Basement") {
			input "Basement", "enum", title: "Which program?", options: ecobeePrograms, required: true
		}
	}
}