/**
 *  HomeAuto-FirstApp
 *
 *  Copyright 2017 Jeremy Brooks
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
    name: "HomeAuto-FirstApp",
    namespace: "jcbrooks-homeautomation",
    author: "Jeremy Brooks",
    description: "Home auto app for testing",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Allow what services users have access to") {
		input "switches", "capability.switch", multiple: true, required: true
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

mappings {
  path("/switches") {
    action: [
      GET: "listLights"
    ]
  }
  path("/switches/:command/") {
    action: [
      PUT: "updateLights"
    ]
  }
  
  /*path("/switches/:command/") {
    action: [
      PUT: "updateLights"
    ]
  }*/
}

def listLights() {
 def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp

}

def updateLights() {
// use the built-in request object to get the command parameter
    def command = params.command
	def room = params.room
    int i = 0;

    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    switch(command) {
    	case "off":
            switch(room){
            	case "all":
                	switches.off()
                    switches.each {
      					log.debug "$it.displayName is off"
    				}
                    return
               	case params.room:
                   	while (switches[i].displayName!=params.room)
                    {
                        i++;
                        //log.debug switches[i].displayName;
                    }
                    switches[i].off()
                    log.debug "$room is off..."
                    return switches[i];
                default:
                    httpError(400, "$command is not a valid room")
                    return
          }

        case "on":
            switch(room){
            	case "all":
                	switches.on()
                    switches.each {
      					log.debug "$it.displayName is on"
    				}
                    return 
               	case params.room:
                    while (switches[i].displayName!=params.room)
                    {
                        i++;
                        log.debug switches[i].displayName;
                    }
                    switches[i].on()
                    log.debug "$room is on.."
                    return
                default:
                    httpError(400, "$command is not a valid room")
                    return 
              }
         
        default:
			httpError(400, "$command is not a valid command for all switches specified")
            return  
		}
}
