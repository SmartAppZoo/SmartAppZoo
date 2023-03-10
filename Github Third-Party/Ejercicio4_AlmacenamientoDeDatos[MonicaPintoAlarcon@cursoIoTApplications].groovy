/**
 *  Almacenamiento de datos
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
    name: "Almacenamiento de datos",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "Desarrollo de una aplicaci\u00F3n que sea energ\u00E9ticamente eficiente, basado en una sala con varias luces que no se quiere que est\u00E9n todas encendidas al mismo tiempo. Cada 10 minutos se comprueba cuantas luces hay encendidas y en caso de que se detecte que se han encendido m\u00E1s de una se apagar\u00E1n las que se encendieron primero hasta dejar solo una. ",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input("Luces","capability.switch",multiple:true, required: true);
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
    state.lastSwitch = "";
	subscribe(Luces, "switch", LuzHandler)
    runEvery10Minutes(TimerHandler)
}

def LuzHandler(evt)
{
	//def Ultimo = Luces.find{it?.displayName == evt.displayName}
	if(	evt.value == "on"){
        state.lastSwitch = evt.displayName;
    }
    log.debug "ultimo switch: $state.lastSwitch";
}

def TimerHandler()
{
    def Encendidos = Luces.findAll{it?.switchState?.value =='on'};
    log.debug " Encendidos: $Encendidos.displayName";
    def Apagar = Encendidos.findAll{it?.displayName != (state.lastSwitch)};
    log.debug "Apagar: $Apagar.displayName";
    Apagar*.off()
}