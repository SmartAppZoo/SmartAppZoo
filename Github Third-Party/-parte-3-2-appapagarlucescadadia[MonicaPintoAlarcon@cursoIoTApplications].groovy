/**
 *  AppApagarLucesCadaDia
 *
 *  Copyright 2017 Monica Pinto
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
    name: "(Parte 3) 2. AppApagarLucesCadaDia",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Apaga las luces cada d\u00EDa a una hora concreta, usando schedule",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
    	input "luces", "capability.switch", title: "Introduce las luces que quieres apagar", required:true, multiple:true
		input "hora", "time", title:"Introduce hora del dia a la que se apagaran las luces", required:true
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
	log.debug "Planificando tarea..."
    //Vamos a planificarlo para que lo haga cada dia 2 minutos despues de la hora indicada: 2 minutos = 120000 msegundos
    schedule(hora+120000, tareaDiaria)
}

// TODO: implement event handlers
def tareaDiaria(){
	log.debug "Tarea diara en ejecucion..."
	luces.off()
}