/**
 *  AirQuality
 *
 *  Copyright 2020 Philippe Tourn
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
import groovy.json.JsonBuilder
 
 
definition(
    name: "AirQuality",
    namespace: " Chubaca01",
    author: "Philippe Tourn",
    description: "Get AirQuality for AirVisual",
    category: "Health & Wellness",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select a sensor") {
        input "airqual", "capability.IlluminanceMeasurement", title: "Air Quality Devices ",required: true
    }
    section("How often do you want to send the request") {
        input "valMin", "number", title: "Value in hours or mins",required: true,defaultValue:1,displayDuringSetup: true
        input name: "unitSelected", defaultValue:"hour",required: true, type: "enum", title: "Unit", options: ["min","hour"]
    }
    section("Select city")  {
    	input name: "citySelected", defaultValue:"Cambrian Park",required: true, type: "enum", title: "Cities", options: ["Cambrian Park","San Diego","Grass VAlley"]
    }
    //section("Enter your AirVisual API key") {
    //    input "accountKey", "text", title: "Account Key", required: true
    //}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(airqual,"refreshPushed", airQualityHandler)
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    subscribe(airqual,"refreshPushed", airQualityHandler)
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    log.debug "Initialize"
    state.jsonMap = ""
    state.sensorList = [1: [deviceName:"airQualityUS",sensorName:"illuminance", city:"Cambrian", currentValue: 0, unit: "AQI" ],
              2: [deviceName:"airQualityCN",sensorName:"aqicn", city:"Cambrian", currentValue: 0, unit: "AQI"]]
    state.timeCounter = 1
    updateParameters()
    if (unitSelected == 'min'){
    	runEvery1Minute(handlerMethod)
    }
    else
    {
    	runEvery1Hour(handlerMethod)
    }
}




// TODO: implement event handlers
def handlerMethod() {
	log.debug "handlerMethod1"
    if (state.timeCounter >= valMin) {
        initialize()
        state.timeCounter = 1
    }
    else
    {
    	state.timeCounter = state.timeCounter + 1
    }
}

def airQualityHandler(evt) {
	log.debug "Refresh"
    initialize()
}

def updateParameters() {
	state.jsonMap = ""
	state.jsonMap = getDataFromServer()
    if (state.jsonMap)
    {
    	updateCurrentValue(state.jsonMap)
        updateDevice(state.sensorList)
    }
}

def getDataFromServer() {
   def updateParams = [
        method: 'GET',
        uri: "https://api.airvisual.com",
        //uri: "https://b7d71a3f7179.ngrok.io",
        path: "/v2/city",
        query: [
             "city": citySelected,
             "state": "California",
             "country": "USA",
             "key" : "547e5310-24b0-46a1-8862-91f956d43e27"
             //"key": accountKey
        ],
    ]
    try{
        httpGet(updateParams) { resp ->
            if(resp.status == 200)
            {
                if (resp.data) {
                    TRACE("parseMap : $resp.data")
                    return resp.data
                }
                else {
                	log.debug "json empty "+'\r'
                    return null
                }
            }
        }
    }catch (e) 
    	{
    		log.debug "something went wrong: $e"
            return null
		}
}

def updateCurrentValue(jsonMap) {
	
    def result = jsonMap.get('status')
    if (result == 'success') {
        state.sensorList[1].currentValue = (Integer)jsonMap.data.current.pollution.aqius
        state.sensorList[1].city = jsonMap.data.city
        state.sensorList[2].currentValue = (Integer)jsonMap.data.current.pollution.aqicn
        state.sensorList[2].city = jsonMap.data.city
        TRACE("AQI US: $state.sensorList[1].currentValue ")
    }
    else{
    	log.debug "JsonMap result Error: $result"+'\r'
    }
}

def updateDevice(toSend){ 
        def builder = new JsonBuilder()
		builder(toSend)
		def message = builder.toString()
        TRACE("message : $message")
        settings.airqual.parse(message)      
		}

private def TRACE(message) {
    log.debug message
}