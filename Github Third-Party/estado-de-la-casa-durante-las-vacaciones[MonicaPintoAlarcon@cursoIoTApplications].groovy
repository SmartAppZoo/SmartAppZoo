/**
 *  Estado de la casa durante las vacaciones
 *
 *  Copyright 2016 Monica Pinto
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
 
//La idea de este ejercicio es usar el servidor web que hay instalado en la máquina del profesor (abrir NetBeans y arrancar proyecto Web)
//Pero no funciona porque da un problema de timeout intentando la conexión
definition(
    name: "Estado de la casa durante las vacaciones",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Si la casa se encuentra en modo vacaciones SmartThing me enviará una vez al día un informe del estado de una serie de sensores.",
    category: "My Apps",
    )

 /*section("Selecciona los sensores que quiere usar para crear el informe") {
    	input "agua", "capability.waterSensor", title: "Inundacion", required: true
        input "humo", "capability.smokeDetector", title: "Detector Humos", required: true
   }*/
preferences {
   section("Selecciona los sensores que quieres usar para crear el informe") {
     	input "alarma", "capability.alarm", title: "Selecciona la alarma", required:true
        input "agua", "capability.waterSensor", title: "Selecciona el sensor de humedad", required:true
     	input "humedad", "capability.relativeHumidityMeasurement", title: "Humedad", required: true
        input "temperatura", "capability.temperatureMeasurement", title: "Temperatura", required: true
   } 
   
   section("Selecciona la hora a la que quieres que se genere el informe") {
    	input "hora", "time", title: "Hora Informe", required: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}


def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}


def initialize() {
    
    runOnce(hora, avisarInforme)
    
}

def datosInforme() {
	def resp = []
    def eventos = alarma.eventsBetween(new Date()-1, new Date())
    def valores = []
    for (ev in eventos){
       	valores = valores + ev.getEvent()
    }
    resp << ["${humedad.displayName}": "${humedad.currentValue("humidity")}", "${temperatura.displayName}": "${temperatura.currentValue("temperature")}"
             , "${alarma.displayName}": "${valores}", "${agua.displayName}": "${agua.currentValue("water")}"]
    resp
}

def avisarInforme() {
 
 if (location.mode == "Holiday"){
    def params = [
        uri: "http://150.214.108.144:8080/web/InformeVacaciones",
        query: [
            "nombre": "Monica Pinto",
            "informe": datosInforme()
        ]
    ]
    
    try {
        httpGet(params) {resp ->
        	log.debug "${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: ${e}"
    }
 }
}

