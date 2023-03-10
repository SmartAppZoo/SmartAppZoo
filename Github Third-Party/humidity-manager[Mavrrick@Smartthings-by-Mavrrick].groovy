/**
 *  Dewpoint Aware Humidifity control using an external temp sensor or weather underground
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
 /**
 * 10/31/2018
 * Updated required fields so the application can be used simply to humidify or dehumidify and not require both functions
 * Corrected value used for humidification low value to turn on assigned switch
 */
definition(
    name: "Humidity Manager",
    namespace: "Mavrrick",
    author: "Craig King",
    description: "Manages humidity in your home by turning on exhast fans, humidifiers, and de humidifiers for you",
    category: "Convenience",
   iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Weather.weather2-icn?displaySize=2x"
)


preferences { 
page(name: "page1", title: "Select sensors and set confortable humidity range", nextPage: "page2", uninstall: true){ 
	 section("Give this app a name?") {
     label(name: "label",
     title: "Give this app a name?",
     required: false,
     multiple: false)
	}
	section("Humidity measurement supplied by:") {
	input ("humidityInput", "capability.relativeHumidityMeasurement", required: true)
	}   
    section("Which switch controlls our humidifier?") {
		input ("switch2", "capability.switch", required: false)
	}
        section("Which switch controlls your dehumidifer or exhast fan?") {
		input ("switch1", "capability.switch", required: false)
	}
       section("Humidity offset modifier") {
        input ("humoffset1", "number", title: "Desired humidity setting offset.  By default the humidity at 40 degrees F outdoor temperature will be 45% the humidity at -20 degrees F outdoor temperature will be 15%.  This setting moves the range linearly by the number input here.", required: false)
    }
    section("Humidity On/Off range") {
        input ("humrange", "number", title: "Desired humidifier on/off range above and below the setpoint.  Default is 1%.", required: false)
    }
    section("Min allowed humidity") {
        input ("humMin", "number", title: "Desired minimum humidity.  Default is 45%.", required: false)
    }
     section("Max allowed humidity") {
        input ("humMax", "number", title: "Desired maxiumum humidity.  Default is 55%.", required: false)
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
	subscribe(switch2, "switch.on", scheduleHumidity)
    subscribe(switch1, "switch.on", scheduleHumidity)
    log.debug("Installed Humidifier manager application")
}

def updated() {
	unsubscribe()
	subscribe(humidityInput, "humidity", humidityActivate)
	subscribe(switch2, "switch.on", scheduleHumidity)
    subscribe(switch1, "switch.on", scheduleHumidity)
    log.debug("Updated Humidifier manager application")
}
     
def scheduleHumidity(evt) {
    log.debug("Starting 5 min refresh cycle")
runEvery5Minutes(humidityActivate)
}
     
     
def humidityActivate(evt) {

    // bunch of poorly written default values.
    if (settings.humoffset1) {
    }
    else {
    settings.humoffset1 = (0)
    }
  	log.debug("Humidity offset value is ${settings.humoffset1}")  
    if (settings.humrange) {
    }
    else {
    settings.humrange = (1)
    }
    log.debug("Humidity range value is ${settings.humrange}")
     if (settings.humMin) {
    }
    else {
    settings.humMin = (45).toDouble()
    }
    log.debug("Humidity prefered Minimum value is ${settings.humMin}")   
     if (settings.humMax) {
    }
    else {
    settings.humMax = (55).toDouble()
    }
    log.debug("Humidity prefered Maximum value is ${settings.humMax}")    
    def currentHumidity = settings.humidityInput?.latestValue("humidity")
    log.debug("Current Humidity is ${currentHumidity}")
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
    def humidityMinH = ((((currentTemp*0.4785714286)+24.64285714)+settings.humrange)+settings.humoffset1).toDouble()
    def humidityLow3 = humidityMinH.round()
    def humidityMinL = ((((currentTemp*0.4785714286)+24.64285714)-settings.humrange)+settings.humoffset1).toDouble()
    def humidityLow2 = humidityMinL.round()
    def humidityMaxH = ((((currentTemp*0.4785714286)+24.64285714)+settings.humrange)+settings.humoffset1).toDouble()
    def humidityHigh2 = humidityMaxH.round()
    def humidityMaxL = ((((currentTemp*0.4785714286)+24.64285714)-settings.humrange)+settings.humoffset1).toDouble()
    def humidityHigh3 = humidityMaxL.round()
    log.debug("Low humidity range is ${humidityLow3} to ${humidityLow2}. High Humidity range is ${humidityHigh3} to ${humidityHigh2}")
 
    // the following line evauluates the current max humidity and establishes new set points if the auto set points attempt to go above this.
    if (humidityLow2 > humMin){
    humidityLow2 = (humMin-settings.humrange).toDouble()
    humidityLow3 = (humMin+settings.humrange).toDouble()
    log.debug("Low humidity range is ${humidityLow2} to ${humidityLow3} adjusted")
    }
    if (humidityHigh2 > humMax){
    humidityHigh2 = (humMax-settings.humrange).toDouble()
    humidityHigh3 = (humMax+settings.humrange).toDouble()
    log.debug("High Humidity range is ${humidityHigh2} to ${humidityHigh3} adjusted")
    }
    	if (switch2) {
    		if (currentHumidity >= humidityLow3  && "on" == switch2.currentSwitch) {
                switch2.off()
                log.debug("Humidity is currently ${currentHumidity} and has risen above the current high humidity setpoint of ${humidityLow3}, the humidifier is off.")
            	}
            
    		if (currentHumidity <= humidityLow2 && "off" == switch2.currentSwitch) {
    			switch2.on()
                log.debug("Humidity is currently ${currentHumidity} and has dropped below the current low humidity setpoint of ${humidityLow2}, the humidifier is on.")
            	 	 }
                }
        if (switch1) {
        	if (currentHumidity >= humidityHigh3 && "off" == switch1.currentSwitch) {
    			switch1.on()
                log.debug("Humidity is currently ${currentHumidity} and has risen above the current high humidity setpoint of ${humidityHigh3}, the fan/dehumidifier is on.")
            	 	 }
        	if (currentHumidity <= humidityHigh2 && "on" == switch1.currentSwitch) {
    			switch1.off()
                log.debug("Humidity is currently ${currentHumidity} and has dropped below the current high humidity setpoint of ${humidityHigh2}, the fan/dehumidifier is Off.")
            	 	 }
                    }
        if (currentHumidity > humidityLow2 && currentHumidity < humidityHigh2) {
        log.debug("Humidity is withing prefered comfortable range. Nothing to do.")
        }
}