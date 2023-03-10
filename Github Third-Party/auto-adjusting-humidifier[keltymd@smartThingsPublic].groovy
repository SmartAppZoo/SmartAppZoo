/**
 *  Dewpoint Aware Humidifier using an external temp sensor or weather underground
 *
 *  Copyright 2015 Bryan Greffin
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
    name: "Auto adjusting humidifier",
    namespace: "Greffinthenerd",
    author: "Bryan Greffin",
    description: "Turn your humidifier on or off based on the outdoor temps to keep condensation down.",
    category: "Convenience",
   iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x"
)


preferences { 
page(name: "page1", title: "Select sensors and humidifier", nextPage: "page2", uninstall: true){ 
	 section("Give this app a name?") {
     label(name: "label",
     title: "Give this app a name?",
     required: false,
     multiple: false)
	}
	section("Humidity measurement supplied by:") {
	input ("humidityInput", "capability.relativeHumidityMeasurement", required: true)
	}   
    section("Which unit to turn on or off?") {
		input "switch2", "capability.switch"
	}
       section("Humidity offset modifier") {
        input ("humoffset1", "number", title: "Desired humidity setting offset.  By default the humidity at 40 degrees F outdoor temperature will be 45% the humidity at -20 degrees F outdoor temperature will be 15%.  This setting moves the range linearly by the number input here.", required: false)
    }
    section("Humidity On/Off range") {
        input ("humrange", "number", title: "Desired humidifier on/off range above and below the setpoint.  Default is 1%.", required: false)
    }
     section("Max allowed humidity") {
        input ("humMax", "number", title: "Desired maxiumum humidity.  Default is 45%.", required: false)
    }
}

page(name: "page2", title: "Select sensor and actuator types", install: true, uninstall: true) 

}
def page2() {
    dynamicPage(name: "page2") {
     section( "Temperature measurement supplied by" ) {
        input ("tempSupply", "enum", title: "which source?",
              options: ["WeatherUnderground", "ExternalSensor"], required: true, submitOnChange: true)
    }
    
    if (tempSupply == "WeatherUnderground") {
	    section("Zip code") {
        input ("zip", "number", title: "Zip Code", required: false)
        }
}
	else {
    section("Which sensor will supply the temperature?") { 
    input ("temperatureInput", "capability.temperatureMeasurement", required: false)
    }
 }
}
}
        
def installed() {
	subscribe(humidityInput, "humidity", humidityActivate)
}

def updated() {
	unsubscribe()
	subscribe(humidityInput, "humidity", humidityActivate)
}
     
def scheduleHumidity(evt) {
runEvery30Minutes(humidityActivate)
}
     
     
def humidityActivate(evt) {

    // bunch of poorly written default values.
    if (settings.humoffset1) {
    }
    else {
    settings.humoffset1 = (0)
    }
    
    if (settings.humrange) {
    }
    else {
    settings.humrange = (1)
    }
    
     if (settings.humMax) {
    }
    else {
    settings.humMax = (45).toDouble()
    }
    
    def currentHumidity = settings.humidityInput?.latestValue("humidity")
    if (currentHumidity) {
    }
    else {
    currentHumidity = (100)
    }
    // setting values for variable to be defined below, because my coding sucks.  
    def currentTemp = (-30)
    
    if (tempSupply == "WeatherUnderground") {
    currentTemp = getWeatherFeature("conditions", settings.zip as String)?.current_observation.temp_f
    }   
    else {
    currentTemp = settings.temperatureInput?.latestValue("temperature")
    }
// If the service or sensor is unavailable sets the outdoor temp to -30 to keep the app from crashing.
    if (currentTemp) {
    }
    else if (currentTemp == 0){
    }
    else {
    currentTemp = (-30)
    }
 
    // evauluates the current temperature outdoors and runs it on a slope and intercept to find the ideal indoor humidity.
    def humidityHigh = ((((currentTemp*0.4785714286)+24.64285714)+settings.humrange)+settings.humoffset1).toDouble()
    def humidityHigh2 = humidityHigh.round()
    def humidityLow = ((((currentTemp*0.4785714286)+24.64285714)-settings.humrange)+settings.humoffset1).toDouble()
    def humidityLow2 = humidityLow.round()
    
    // the following line evauluates the current max humidity and establishes new set points if the auto set points attempt to go above this.
    if (humidityHigh2 > humMax){
    humidityLow2 = (humMax-settings.humrange).toDouble()
    humidityHigh2 = (humMax+settings.humrange).toDouble()
    }
    else{
    }
    
    	if (currentHumidity >= humidityHigh2  && "on" == switch2.currentSwitch) {
                switch2.off()
                log.debug("Humidity is currently ${currentHumidity} and has risen above the current high humidity setpoint of ${humidityHigh2}, the humidifier is off.")
            	}
            
    		else if (currentHumidity <= humidityLow2 && "off" == switch2.currentSwitch) {
    			switch2.on()
                log.debug("Humidity is currently ${currentHumidity} and has dropped below the current low humidity setpoint of ${humidityLow2}, the humidifier is on.")
            	 	 }
}