/**
 *  casa eficinete
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
    name: "casa eficinete",
    namespace: "IOT",
    author: "yusef",
    description: "solo puede haber una luz encendida, si hay mas, se apagan las primeras y se deja la ultima",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
        input "luz", "capability.switch", multiple: true
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
	luz[0].off()
    luz[1].off()
    luz[2].off()
	// TODO: subscribe to attributes, devices, locations, etc.
    runIn( 120,man)
}

// TODO: implement event handlers
def man () {
	log.debug "acta: 123 ${settings}"
	def encendidas = 0
   	for (int i=0;i < 3; i++){
    	if(luz[i].currentValue("switch") == "on"){
    		encendidas = encendidas + 1;
        }
    }
    if(encendidas > 1){
    log.debug "Updated with settings: 11465654654654 ${settings}"
    	/*if ( luz[0].currentValue("switch") == "on"){
        	if (luz[1].currentValue("switch") == "on"){
            	if(luz[0].currentState.rawDateCreated.time > luz[1].currentState.rawDateCreated.time){
                	luz[1].off()
                }else{ luz[0].off() }
        	}
            if(luz[0].currentValue("switch") == "on" && luz[2].currentValue("switch") == "on"){
            	if(luz[0].currentState("switch").rawDateCreated.time > luz[2].currentState("switch").rawDateCreated.time){
                	luz[2].off()
                }else{ luz[0].off() }
            }
            if(luz[1].currentValue("switch") == "on" && luz[2].currentValue("switch") == "on"){
            	if(luz[1].currentState("switch").rawDateCreated.time > luz[2].currentState("switch").rawDateCreated.time){
                	luz[2].off()
                }else{ luz[1].off() }
            }
    	}else if (luz[1].currentValue("switch") == "on"){
        	if (luz[2].currentValue("switch") == "on"){
            	if(luz[1].currentState("switch").rawDateCreated.time > luz[2].currentState("switch").rawDateCreated.time){
                	luz[2].off()
                }else{ luz[1].off() }
            }
        }*/
        def orden = luz.sort(it?.latestState.date)
        for(int i=1; i< orden.size; i++){
        	luz[i].off()
        }
	}
}