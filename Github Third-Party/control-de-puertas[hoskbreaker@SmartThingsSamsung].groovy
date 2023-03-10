/**
 *  Control de puertas
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
    name: "Control de puertas",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "desarrollo de una aplicacion para que si las puertas exteriores de una casa se abren y no se cierran. Despu\u00E9s de un numero determinado de minutos se manda una notificaci\u00F3n indicando qu\u00E9 puertas se han quedado abiertas y cu\u00E1nto tiempo llevan abiertas ",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input("Puertas","capability.contactSensor",title: "seleccione las puertas a usar", multiple: true, required: true);
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
    subscribe(Puertas,"contact",TimeHandler)
}
def TimeHandler(evt)
{
	runIn(10,DoorHandler);
}

def DoorHandler()
{
    def Estado = Puertas*.contactState;
    def Abiertas = Estado.findAll{it?.value == "open"};
    if(Abiertas?.size() >0){
        def TAbiertas = Abiertas*.collect{(now() - it.date.time)/1000}
        def Nombres = Abiertas*.displayName;
        log.debug("Las puertas $Nombres llevan abiertas $TAbiertas segundos");
    }
    
}
// TODO: implement event handlers