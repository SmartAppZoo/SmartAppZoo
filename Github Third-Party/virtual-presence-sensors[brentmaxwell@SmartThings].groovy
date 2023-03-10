/**
 *  Virtual Device Creator
 *
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
        name: "Virtual Presence Sensors",
        namespace: "thebrent",
        author: "Brent Maxwell",
        description: "Creates virtual presence sensors",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        singleInstance: true,
        pausable: false
)


preferences {
    page name: "mainPage", title: "Click save to create a new virtual device.", install: true, uninstall: true
}

mappings {
  path("/devices") {
    action: [
        GET: "listDevices"
    ]
  }
  path("/update") {
    action: [
        POST: "updatePresence"
    ]
  }
}

def mainPage() {
    dynamicPage(name: "mainPage") {
//        section ("New Device") {
//            input "theHub", "hub", title: "Select the hub (required for local execution) (Optional)", multiple: false, required: false
//        }
        section("Device Name") {
          input "deviceName", title: "Enter device name", required: true
        }
        section("Devices Created") {
          paragraph "${getAllChildDevices().inject("") {result, i -> result + (i.label + "\n")} ?: ""}"
        }
        section("Endpoint") {
          paragraph "${state.endpoint}"
        }
        remove("Remove (Includes Devices)", "This will remove all virtual devices created through this app.")
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
}

def uninstalled() {
    getAllChildDevices().each {
        deleteChildDevice(it.deviceNetworkId, true)
    }
}

def updated() {
    initialize()
}

def initialize() {
    def d = addChildDevice("thebrent", "Virtual Presence Sensor", "virtualPresence-${deviceName}", null, [label: deviceName])
    setupEndpoint();
}

def setupEndpoint() {
  if(!state.endpoint) {
    def accessToken = createAccessToken()
    if (accessToken) {
      state.endpoint = apiServerUrl("/api/token/${accessToken}/smartapps/installations/${app.id}/")				
    }
  }
}

def listDevices() {
	def resp = []
    getChildDevices().each {
      resp << [id: it.id, dni: it.dni, name: it.displayName]
    }
    return resp
}

def updatePresence() {
	def body = request.JSON
    def device = getChildDevice("virtualPresence-${body.name}")
    if(device != null){
    	device.setPresence(body.isPresent);
        return [error:false, type:"Device updated", message:"Sucessfully updated device: ${body.name}"];
    }
}

def getDeviceById(id) {
  return getChildDevices().find { it.id == id }
}