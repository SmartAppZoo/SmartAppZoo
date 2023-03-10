/**
 *  Ubibot Smart app
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
 *  V1.0  First release
 *  V1.1. Added unit temperature selection
 *  V1.1.1 added Battery level
 */
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

definition(
    name: "UbibotSmartApp",
    namespace: "Chubaca01",
    author: "Philippe Tourn",
    description: "Get Temperature, Humidity ... from Ubibot device: Tested with WS1 model",
    category: "My Apps",
   
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Select a sensor") {
        input "temp", "capability.temperatureMeasurement", title: "Ubibot temperature Devices ",required: true
    }
    section("How often do you want to send the request") {
        input "valMin", "number", title: "Value in hours or mins",required: true,defaultValue:1,displayDuringSetup: true
        input name: "unitSelected", defaultValue:"hour",required: true, type: "enum", title: "Unit", options: ["min","hour"]
    }
    section("Select unit for temperature")  {
    	input name: "degreeSelected", defaultValue:"fahrenheit",required: true, type: "enum", title: "Degree", options: ["fahrenheit","celcius"]
    }
    section("Enter your Ubibot channel ID & API key") {
    	input "channelId", "text", title: "channel ID", required: true
        input "accountKey", "text", title: "Account Key", required: true
    }
     
    
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(temp,"refreshPushed", temperatureHandler)
    initialize()
    
}


def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(temp,"refreshPushed", temperatureHandler)
    initialize()
    
}


def initialize() {
    log.debug "Initialize"
    state.jsonMap = ""
    state.sensorInfo =[channelId: ""]
    state.sensorList = [1: [deviceName:"Light",sensorName:"illuminance",fieldName:"", currentValue: 0, unit: "lux" ],
              2: [deviceName:"Temperature",sensorName:"extemperature",fieldName:"", currentValue: 0, unit: "F"],
              3: [deviceName:"External Temperature Probe",sensorName:"temperature",fieldName:"", currentValue: 0, unit: "F"],
              4: [deviceName:"Humidity",sensorName:"humidity",fieldName:"", currentValue: 0, unit: "%"],
              5: [deviceName:"Voltage",sensorName:"voltage",fieldName:"", currentValue: 0, unit: "V"],
              6: [deviceName:"WIFI RSSI",sensorName:"wifiRSSI",fieldName:"", currentValue: 0, unit: "dB"],
              7: [deviceName:"Voltage",sensorName:"battery",fieldName:"", currentValue: 0, unit: "%"]]
    state.timeCounter = 1
    updateWithDegreeSelected()
    updateParameters()  
    if (unitSelected == 'min'){
    	runEvery1Minute(handlerMethod)
    }
    else
    {
    	runEvery1Hour(handlerMethod)
    }
}

def updateWithDegreeSelected(){
	state.sensorList.eachWithIndex { entry, i ->
    	def mapSensor = entry.value
        if ( mapSensor.unit == "F"){
        	if (degreeSelected != "fahrenheit"){
            	mapSensor.unit = "C"
            }          
        }
   	}
}

def handlerMethod() {
	log.debug "handlerMethod1"
    if (state.timeCounter >= valMin) {
    	updateParameters()
        state.timeCounter = 1
    }
    else
    {
    	state.timeCounter = state.timeCounter + 1
    }
}

def temperatureHandler(evt) {
	log.debug "Refresh"
    updateParameters()
}

def updateParameters() {
	state.jsonMap = getDataFromServer()
    if (state.jsonMap)
    {
    	updateCurrentValue(state.jsonMap)
        updateDevice(state.sensorList)
        updateDevice(state.sensorInfo)
    }
}


def getDataFromServer() {
    def updateParams = [
        method: 'GET',
        uri: "https://api.ubibot.io",
        //uri: "https://90395da6.ngrok.io",
        path: "/channels",
        query: ["account_key": accountKey],
    ]
    try{
        httpGet(updateParams) { resp ->
            if(resp.status == 200)
            {
                if (resp.data) {
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


def getValuefromField(jsonMapped,field)
{
		def temp = jsonMapped.get(field)
        def res = temp.value as Float
        return res
}

def updateCurrentValue(jsonMap) {
	def channelsMap
    
    def result = jsonMap.get('result')
    if (result == 'success') {
        channelsMap = null
        jsonMap.channels.eachWithIndex { entry, i ->
            if (entry.channel_id == channelId){
            	channelsMap = entry
                state.sensorInfo.channelId = entry.channel_id
            }
        }
        if(!channelsMap){
        	log.debug "Channel id not found"
        }
        else{
            // Get field name for each device
            state.sensorList.eachWithIndex { entry, i ->
                def mapSensor = entry.value
                mapSensor.fieldName = channelsMap.find{it.value == mapSensor.deviceName}.key
            }

            // get all last values from channel
            def lastValues = channelsMap.get('last_values')

            // Maping of last values
            def jsonSlurper = new JsonSlurper()
            def lastvalMap = jsonSlurper.parseText(lastValues)

            // read all sensors values
            state.sensorList.eachWithIndex { entry, i ->
                def mapSensor = entry.value
                mapSensor.currentValue = getValuefromField(lastvalMap,mapSensor.fieldName)
            }
        }
    }
    else{
    	log.debug "JsonMap result Error: $result"+'\r'
    }
}


def updateDevice(toSend){ 
        def builder = new JsonBuilder()
		builder(toSend)
		def message = builder.toString()
        settings.temp.parse(message)      
		}


    