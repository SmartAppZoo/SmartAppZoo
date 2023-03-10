/**
 *  JSON
 *
 *  Original Copyright 2015 Jesse Newland
 *    - https://github.com/jnewland/SmartThings/blob/master/JSON.groovy
 *  Modification Copyright 2016 William Napier
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
    name: "JSON Bridge",
    namespace: "billnapier",
    author: "Bill Napier",
    description: "A JSON Bridge API for SmartThings",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    if (!state.accessToken) {
        createAccessToken()
    }
}

preferences {
    page(name: "copyConfig")
}

def capabilities() {
  return [
        "switch",
        "colorControl",
        "temperatureMeasurement",
    ]
}

def copyConfig() {
    if (!state.accessToken) {
        createAccessToken()
    }
    dynamicPage(name: "copyConfig", title: "Config", install:true) {
        section("Select devices to include in the /devices API call") {
            for (capability in capabilities()) {
              def fullCapability = "capability." + capability
              def varname = capability + "s"
              input varname, fullCapability, title: capability, multiple: true, required: false
            }
        }

        section() {
            paragraph "View this SmartApp's configuration to use it in other places."
            href url:"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
        }

        section() {
            href url:"https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/devices?access_token=${state.accessToken}", style:"embedded", required:false, title:"Debug", description:"View accessories JSON"
        }
    }
}

def deviceCommandMap(device, type) {
  device.supportedCommands.collectEntries { command->
      def commandUrl = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/${type}/${device.id}/command/${command.name}"
      [
        (command.name): commandUrl
      ]
  }
}

def deviceAttributeMap(device, type) {
  device.supportedAttributes.collectEntries { attribute->
      def attrUrl = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/${type}/${device.id}/attribute/${attribute.name}"
      [
        (attribute.name): attrUrl
      ]
  }
}

def authorizedDevices() {
	// Well, can't figure out how to do this dynamically....
    [
        colorControl: colorControls,
        temperatureMeasurement: temperatureMeasurements,
        switch: switchs
    ]
  //  def b = capabilities().collectEntries {
  //    [(it): evaluate("temperatureMeasurement")]
   // }
}

def renderDevices() {
    def deviceData = authorizedDevices().collectEntries { devices->
        [
            (devices.key): devices.value.collect { device->
                [
                    name: device.displayName,
                    id: device.id,
                    commands: deviceCommandMap(device, devices.key),
                    attributes: deviceAttributeMap(device, devices.key)
                ]
            }
        ]
    }
    def deviceJson    = new groovy.json.JsonOutput().toJson(deviceData)
    def deviceString  = new groovy.json.JsonOutput().prettyPrint(deviceJson)
    render contentType: "application/json", data: deviceString
}

def deviceCommand() {
  def device  = authorizedDevices()[params.type].find { it.id == params.id }
  def command = params.command
  if (!device) {
      httpError(404, "Device not found")
  } else {
      if (params.value) {
        device."$command"(params.value)
      } else {
        device."$command"()
      }
  }
}

def attributeCommand() {
  def device  = authorizedDevices()[params.type].find { it.id == params.id }
  def attribute = params.attribute
  if (!device) {
      httpError(404, "Device not found")
  } else {
      return device.currentState(attribute)
  }
}

mappings {
    path("/devices")                      { action: [GET: "renderDevices"]  }
    path("/:type/:id/command/:command")   { action: [PUT: "deviceCommand"] }
    path("/:type/:id/attribute/:attribute")   { action: [GET: "attributeCommand"] }
}