/**
 *  NFC y modos
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
    name: "NFC y modos",
    namespace: "IOT",
    author: "yusef",
    description: "lee NFC, cambia a modo Home, apaga alarma y si hay poca luminosidad, enciende luces",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Title") {
		// TODO: put inputs here
        input "alarma", "capability.switch"
        input "luz", "capability.switch"
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
    location.setMode("Away")
    
}

// TODO: implement event handlers
mappings {
	path("/alarma/:command"){
    	action: [
        	PUT: "actualizaralarma"
        ]
    }
    path("/luz/:command"){
    	action: [
        	PUT: "actualizarluz"
        ]
    }
    path("/mode"){
    	action: [
        	GET: "peticionMode"
        ]
    }
    path("/mode/:command"){
    	action: [
        	PUT: "actualizarmodo"
        ]
    }
}
def actualizaralarma() {
	def modo = location.currentMode
    if(modo == "Away"){
        def cmd = params.command
        switch(cmd){
            case "on":
                alarma.on()
                break
            case "off":
                alarma.off()
                break
            default:
                httpError(501,"$command no es un un comando valido")
        }
    }
}
def actualizarluz() {
	def modo = location.currentMode
    if( modo == "Away"){
        def cmd = params.command
        switch(cmd){
            case "on":
                luz.on()
                break
            case "off":
                luz.off()
                break
            default:
                httpError(501,"$command no es un un comando valido")
        }
    }
}
def actualizarmodo() {
	def cmd = params.command
    log.debug "El modo actual es: $location.mode"
    switch(cmd){
    	case "Home":
        	location.setMode("Home")
        	break
        case "Away":
        	location.setMode("Away")
            break
        case "Night":
        	location.setMode("Night")
            break
        default:
        	httpError(501,"$command no es un un comando valido")
    }
    
    log.debug "Ahora estas en el modo: $location.mode"
}
def peticionMode(){
	def modo = location.currentMode
	return modo
}