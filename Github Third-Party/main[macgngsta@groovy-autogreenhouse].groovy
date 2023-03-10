/**
 *  AutoGreenhouse
 *
 *  Copyright 2021 greg tam
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
    name: "AutoGreenhouse",
    namespace: "macgngsta",
    author: "greg tam",
    description: "automatically water daily, turn on heater when temperature is low",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {

   	section("What Time") {
    	input "myTime", "time", title: "Time to execute"
    }
    
    section("On Which Days") {
        input "days", "enum", title: "Select Days of the Week", required: true, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday"]
    }
    
	section("Select a humidity sensor"){
    	input "humiditySensor","capability.relativeHumidityMeasurement",required:true, title:"Humidity Sensor"
    }
	section("Select the water pump outlet") {
		input "waterSwitch","capability.switch",required:true, title:"Water Pump Outlet"
	}
    
    section("Watering Threshold") {
			input "waterPeriodInSeconds", "number",
				title: "Water Period in Seconds:",
				required: false, defaultValue: 30,
				range:"1..300"
    }
    
    //typical humidity thresholds should be 75-80
    section("Humidity Thresholds") {
			input "lowHumidityThreshold", "number",
				title: "Enter Low Humidity:",
				required: false, defaultValue:75,
				range:"0..100"
			input "highHumidityThreshold", "number",
				title: "Enter High Humidity:",
				required: false, defaultValue: 80,
				range:"0..100"
	}
    
    section("Select a temperature sensor"){
    	input "tempSensor","capability.temperatureMeasurement",required:true,title:"Temperature Sensor"
    }
	section("Select the heater outlet") {
		input "heaterSwitch","capability.switch",required:true, title:"Heater Outlet"
	}
    
     section("Heating Threshold") {
			input "heatPeriodInSeconds", "number",
				title: "Heat Period in Seconds:",
				required: false, defaultValue:600,
				range:"1..3600"
    }
    
    //typical temperature thresholds should be 64-75
	section("Temperature Thresholds") {
			input "lowTempThreshold", "number",
				title: "Enter Low Temperature:",
				required: false, defaultValue: 64,
				range:"0..100"
			input "highTempThreshold", "number",
				title: "Enter High Temperature:",
				required: false, defaultValue: 75,
				range:"0..100"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(humiditySensor, "humidity", humidityHandler)
    subscribe(tempSensor, "temperature", tempHandler)
    schedule(myTime, timeHandler) 
}

//humidity
def humidityHandler(evt){
	log.debug "humidityController called: $evt"
    def humidityState = humiditySensor.currentState("humidity")
    log.debug "humidity value: ${humidityState.value}"
    //if humidity is below low threshold, turn water on for 30 secs every 10 minutes
    if(humidityState.value <= lowHumidityThreshold){
    	runWaterCycle()
    }
    else if(humidityState.value > highHumidityTreshold){
    	turnOffWater()
    }
}

//temperature
def tempHandler(evt){
	log.debug "temperatureController called: $evt"
    def tempState = tempSensor.currentState("temperature")
    log.debug "temperature value: ${tempState.value}"
    
    //if temperature is below low threshold, turn on
    if(tempState.value <= lowTempThreshold){
    	runHeaterCycle()
    }
    else if(tempState.value > highTempTreshold){
     //if temperature is above high threshold, turn off
    	turnOffHeater()
    }
}

//scheduled
def timeHandler(evt){
	//schedule water to be turned on for 30 seconds every day
    def df = new java.text.SimpleDateFormat("EEEE")
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    
    def dayCheck=days.contains(day)
    if(dayCheck){
    	log.debug "running scheduled water"
    	runWaterCycle()
	}
}

/** WATER CYCLE **/

def runWaterCycle(){
	if( waterSwitch.currentSwitch == "off"){
    	log.debug "turning ON water for: $waterPeriodInSeconds"
    	waterSwitch.on()
        runIn(waterPeriodInSeconds, turnOffWater)
    }
}

def turnOffWater(){
	if( waterSwitch.currentSwitch == "on"){
    	log.debug "turning OFF water"
    	waterSwitch.off()
    }
	
}

/** HEATER CYCLE **/

def runHeaterCycle(){
	if(heaterSwitch.currentSwitch == "off"){
    	log.debug "turning ON heater for: $heatPeriodInSeconds"
    	heaterSwitch.on()
        runIn(heatPeriodInSeconds, turnOffHeater)
    }
}

def turnOffHeater(){
	if(heaterSwitch.currentSwitch == "on"){
    	log.debug "turning OFF heater"
    	heaterSwitch.off()
    }
}