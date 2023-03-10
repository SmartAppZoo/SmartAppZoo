/**
 *  CasaEnergeticamenteEficiente
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
definition(
    name: "(Parte 3) 8. CasaEnergeticamenteEficiente",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Usando planificacion de tareas y la variable static; nos aseguramos de apagar las luces en caso de que haya m\u00E1s de una encendida y dejar solo una. Dejamos la \u00FAltima en encenderse",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input "luces", "capability.switch", multiple:true
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
	subscribe(luces, "switch.on", encenderLuz)
    subscribe(luces, "switch.off", apagarLuz)
    
    state.numLuces = 0
    //En el simulador aparece un botón para forzar la ejecución y no tener que esperar 30 minutos
    runEvery30Minutes(comprobarLuces)
}

def encenderLuz(evt){
	state.numLuces = state.numLuces + 1
    log.debug "numLuces = $state.numLuces"
}

def apagarLuz(evt){
	state.numLuces = state.numLuces - 1
    log.debug "numLuces = $state.numLuces"    
}

def comprobarLuces(){

	//Se podría hacer también sin la variable de estado, usando un cierre para contar los elementos de la lista
    def c = luces.count {it?.latestValue("switch") == 'on'}
    def c2 = luces.count {it?.latestState("switch")?.getValue() == 'on' }
    log.debug "numLuces = $c"
    log.debug "numLuces = $c2"
    log.debug "numLuces = $state.numLuces"
	
    if (state.numLuces > 1) {
    	log.debug "Apaga alguna luz"
        def luzOn = luces.findAll {it?.latestValue("switch") == 'on' }
        log.debug "numLuces = ${luzOn.size()}" 
        def orden = luzOn.sort{it?.latestState?.date}
        def times = orden*.latestState.date
        log.debug "times: $times orden:$orden"
        def i = 0
        while(i < luzOn.size - 1 ){
        	orden[i].off()
            i = i + 1
        }
    }
}