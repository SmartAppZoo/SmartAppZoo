/**
 *  toPIUFL
 *
 *  Copyright 2019 Joseph Langan
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
    name: "toPIUFL",
    namespace: "langanjp",
    author: "Joseph Langan",
    description: "Smartapp to Send Events to OSIsoft PI UFL Connector Rest Server",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section ("Sensors that should go to PI") {
		input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
		input "switches", "capability.switch", title: "Switches", required: false, multiple: true
		input "switchLevels", "capability.switchLevel", title: "Switch Levels", required: false, multiple: true        
		input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
	}
    section ("PI UFL Connector Details") {
		input "uflConnector", "text", title: "PI UFL Connector URL with port (https://server:5460)", required: true
		input "uflName", "text", title: "PI UFL Connector Data Source Name (TTVRestServer)", required: true
		input "basicauth", "text", title: "base64 encoded username password", require: true
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
    subscribe(switches, "switch", genericHandler)
    subscribe(waterSensors, "water", genericHandler)
    subscribe(presence, "presence", genericHandler)
}

def genericHandler(evt) {
	

	
	def evtValue = evt.value
  def evtTime = evt.date.getTime()/1000
	def tagName = evt.location.toString() + "." + evt.displayName + "." + evt.name
 
  def datarecord = tagName + "," + evtTime + "," + evtValue
	log.debug datarecord
	piuflcWriter(datarecord)


}

def piuflcWriter(datarecord) {

	def ufluri = uflConnector + "/ConnectorData/" + uflName + "/Post"


    def params = [
		uri: ufluri,
		headers: [
    	Authorization: "Basic " + "$basicauth"
 		],
		body: datarecord
    ]

    try {
    	log.debug params;
        httpPost(params) { resp ->
        	log.debug resp.status
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }  
  
}
