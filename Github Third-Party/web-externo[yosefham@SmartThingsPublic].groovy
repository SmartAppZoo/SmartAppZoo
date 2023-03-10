/**
 *  web externo
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
    name: "web externo",
    namespace: "IOT",
    author: "yusef",
    description: "prueba de conexion a servicio web externo",
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
    uri: 'http://api.openweathermap.org/data/2.5/',
    path: 'weather',
    contentType: 'application/json',
    query: [/* lat:'36.572344', lon:'-4.602985'*/ q: "Narnia", units: 'metric',
    APPID: '5d8f63a4f5a632aaed2ed858d4439385']
    //q:'Narnia' para buscar por Ciudad
    ]
    //Ejecutamos la petición HTTP y consultamos la respuesta
    def temp = 0
    try {
        httpGet(params) {resp ->
            log.debug "resp data: ${resp.data}"
         	temp = resp.data.main.temp
            log.debug "temp: ${temp}"
        }
    	if (temp > 27) //En ºC
   			interruptor.on()
   		 else interruptor.off()
    } catch (e) { log.error "error: $e" }
}