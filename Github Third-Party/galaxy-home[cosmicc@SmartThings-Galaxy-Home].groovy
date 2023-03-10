/**
 *  Galaxy Home
 *
 *  Copyright 2016 cosmicc
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
    name: "Galaxy Home",
    namespace: "cosmicc",
    author: "cosmicc",
    description: "Galaxy Home SpartApp",
    category: "Convenience",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)
   
mappings {
  path("/command/:command") {
    action: [
      GET: "processmotion"
    ]
  }
}

preferences {
    section {
        input "alldevices", "capability.beacon", title: "All Devices", multiple: true
        //input "kitchen", "capability.beacon", title: "Kitchen"
        //input "livingroom", "capability.beacon", title: "Living Room"
        //input "stairway", "capability.beacon", title: "Stairway"
    }
}

void processmotion() {
    def device = params.device
    log.trace params
    switch(device) {
        case "LivingRoom":
            log.debug "HOLY SHIT!!"
            break
        case "motion_ended":
            switches.off()
            break
        default:
            httpError(400, "$command is not a valid command for all switches specified")
    }
}


def installed()
{
    log.debug "Installed: $settings"
    log.trace state
    state.appURL = "https://graph-na02-useast1.api.smartthings.com/api/smartapps/installations/${app.id}/command?access_token=${state.accessToken}"
    createWebhook()
    initialize()
}

def updated()
{
    log.debug "Updated: $settings"

    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    log.trace "Uninstalled called"
    deleteWebhook()
    getChildDevices().each {device ->
        log.info "Deleting Device $device.displayName"
        deleteChildDevice(device.deviceNetworkId)
    }
}

def initialize() {
  schedule("0 */3 * * * ?", poll_devices)
}

def poll_devices() {
 alldevices.poll()
}

void createWebhook() {
	log.debug "Creating a Particle webhook "
    try {         
		httpPost(uri: "https://api.particle.io/v1/webhooks",
      			body: [access_token: "c38ecbb487154838b9a7f0d991c45d536d625cda", 
        		event: "command",
                url: state.appURL, 
                requestType: "PUT", 
                mydevices: true]
      			) {response -> log.debug (response.data)}
    }
 	catch (e) {
   		log.error "error: $e"
    }
}

void deleteWebhook() {
try{
httpGet(uri:"https://api.particle.io/v1/webhooks?access_token=c38ecbb487154838b9a7f0d991c45d536d625cda",
    ) {response -> response.data.each { 
    		hook ->
				//log.debug hook.event
                if (hook.event == "command") {
                	httpDelete(uri:"https://api.particle.io/v1/webhooks/${hook.id}?access_token=c38ecbb487154838b9a7f0d991c45d536d625cda")
                    log.debug "Deleted the existing webhook with the id: ${hook.id} and the event name: command"
                }
           }
 }
 } catch (all) {log.debug "Couldn't delete webhook, moving on"}
}
