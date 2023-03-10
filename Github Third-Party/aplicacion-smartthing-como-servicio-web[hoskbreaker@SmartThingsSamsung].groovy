/**
 *  Aplicacion SmartThing como servicio web
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
    name: "Aplicacion SmartThing como servicio web",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "Desarrollo de una aplicacion donde el sensor de luminosidad del movil detectara el nivel de luz en la habitacion. Si est\u00E1 por debajo de un determinado valor encender\u00E1 las luces a trav\u00E9s de SmartThings. Si est\u00E1 por encima de otro nivel apagara las luces.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input("switches", "capability.switch",title: "luces a usar", multiple: false, required: true);
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
}

// TODO: implement event handlers
mappings{
	path("/switches"){
    	action:[GET: "listswitches"]        	
    }
    path("/switches/:command"){
    	action:[PUT: "updateswitches"]
    }
}

def updateswitches(){
	def cmd = params.command;
    log.debug "command: $cmd";
    switch(cmd){
    case "on":
    	switches.on();
        break;
    case "off":
    	switches.off();
        break;
    default: httpError(501, "comando no reconocido");
    };
}
def listswitches(){
	def resp=[]
    switches.each{
    	resp <<[value: it.currentValue("switch")]
    }
    return resp;
}