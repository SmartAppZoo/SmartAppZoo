/**
 *  estacion meteorologica
 *
 *  Copyright 2016 yusef
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
    name: "estacion meteorologica",
    namespace: "IOT",
    author: "yusef",
    description: "conecta con una estacion meteorologica ",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input "termostato","capability.thermostat",required:true
        input "interruptor","capability.switch",required:true
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
    //COMPLETAR
    runEvery5Minutes(consultarTiempoJSON)
    
}
def consultarTiempoJSON(){
//Construimos la petición HTTP
def params = [
    uri: "http://weatherstation.wunderground.com",
    path: "/weatherstation/updateweatherstation.php",
    query: [
        "ID": "IANDALUC230", //variable
        "PASSWORD": "csnkm3d9", //variable
        "dateutc": "now",
        "tempf": '39', //variable
        //“humidity”: currentHumidity, //variable
        "action": "updateraw",
        "softwaretype": "SmartThings"
    	]
    ]
    //Ejecutamos la petición HTTP y consultamos la respuesta
    def temp = 0
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
        }
    } catch (e) { log.error "error: $e" }
}