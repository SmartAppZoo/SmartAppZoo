/**
 *  Conexion con servicios externos
 *
 *  Copyright 2018 Jose Carlos Sanchez
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
    name: "Conexion con servicios externos",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "Consiste en consultar el tiempo haciendo una llamada Web desde nuestra aplicacion SmartThing al servidor openweathermap.org a traves de su API. Cada 30 minutos se chequea el tiempo y si la temperatura es menor de 10, encendemos la calefaccion.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input("Interruptor","capability.switch",title:"selecciona interruptor", multiple: false, required: true);
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
	// TODO: subscribe to attributes, devices, locations, etc.
    runEvery1Minute(consultarTiempoJSON);
}

// TODO: implement event handlers
def consultarTiempoJSON(){
    //Construimos la petición HTTP
    def params = [
        uri: 'http://api.openweathermap.org/data/2.5/',
        path: 'weather',
        contentType: 'application/json',
        query: [q:"Málaga", units: "metric",
        APPID: '9d4dd35c074a641a842ac67076227143']
    //q:'Málaga' para buscar por Ciudad – sin tilde es Australia
    ]
    //Ejecutamos la petición HTTP y consultamos la respuesta
    def temp = 0
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
            temp = resp.data.main.temp
            log.debug "temp: $temp";
        }
        if (Interruptor.currentSwitch == "off" && temp < 10){
            Interruptor.on()
        }
    } catch (e) { log.error "error: $e" }
}