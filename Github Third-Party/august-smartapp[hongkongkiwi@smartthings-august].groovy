/**
 *  Raspberry Pi
 *
 *  Copyright 2016 Andy
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
    name: "August Lock Service Manager",
    namespace: "hongkongkiwi",
    author: "Andy",
    description: "SmartApp to connect with local August Smart App Server",
    //category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "August Smart App", displayLink: ""])


preferences {
	section("Title") {
    page(name: "Credentials", title: "Sample Authentication", content: "authPage", nextPage: "sampleLoggedInPage", install: false)
	}
}

mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

def authPage() {
    // Check to see if our SmartApp has it's own access token and create one if not.
    if(!state.accessToken) {
        // the createAccessToken() method will store the access token in state.accessToken
        createAccessToken()
    }

    def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    // Check to see if we already have an access token from the 3rd party service.
    if(!state.authToken) {
        return dynamicPage(name: "auth", title: "Login", nextPage: "", uninstall: false) {
            section() {
                paragraph "tap below to log in to the 3rd party service and authorize SmartThings access"
                href url: redirectUrl, style: "embedded", required: true, title: "3rd Party product", description: "Click to enter credentials"
            }
        }
    } else {
        // We have the token, so we can just call the 3rd party service to list our devices and select one to install.
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
  settings.devices.each {deviceId->
      def device = state.devices.find{it.id==deviceId}
        if (device) {
          def childDevice = addChildDevice("smartthings", "Device Name", deviceId, null, [name: "Device.${deviceId}", label: device.name, completedSetup: true])
    }
  }
}

def getState() {
  def pollParams = [
    uri: "https://${settings.serverAddress}",
    path: "/device",
    requestContentType: "application/json",
    //query: [format:"json",body: jsonRequestBody
  ]

  // httpGet(pollParams) { resp ->
  //   state.devices = resp.data.devices { collector, stat ->
  //     def dni = [ app.id, stat.identifier ].join('.')
  //     def data = [
  //       attribute1: stat.attributeValue,
  //       attribute2: stat.attribute2Value
  //     ]
  //     collector[dni] = [data:data]
  //     return collector
  //   }
  // }

  return ["lock": "locked"]
}

// def sendApiCall() {
//     def deviceListParams = [
//         uri: "https://api.thirdpartysite.com",
//         path: "/get-devices",
//         requestContentType: "application/json",
//         query: [token:"XXXX",type:"json" ]
//
//     httpGet(deviceListParams) { resp ->
//             //Handle the response here
//     }
// }
