/**
 *  XTension Bridge
 *
 *  Copyright 2019 John Guineau
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
    name: "XTension Bridge",
    namespace: "guineau",
    author: "John Guineau",
    description: "Pass events such as button press to a JSON server in XTension. This allows a SmartThings event to control XTension devices (such as UPB)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "URL"
}

preferences 
{
	section("Title") {
		input "host", "text", title:"JSON Server ip:port"
        input "jsonRoot", "text", title:"JSON Root Path"
	}

	section() {
        input "buttons", "capability.button",  multiple:true 
    }
}

def installed() 
{
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() 
{
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() 
{
	log.debug "Initialize"
    subscribeToButtons()
}

def postXTension( name, value, data )
{
	def xtResp = []
    def path = "/${jsonRoot}/JSON_ST_${name}/${value}"
	post(path) { response ->
		xtResp = response.data.gip
		debugOut( "XTension replied with ${xtResp}" )
	}
}

def post(String path, Closure callback)
{
	log.debug "post..."
	def action = new physicalgraph.device.HubAction (
    	[
        	method: "GET",
    		path: "${path}",
    		headers: [
       			HOST: "${host}"
    		]
        ],
        null,
        [callback: httpPostCallback]
	)

	log.debug "sendHubCommand"
    sendHubCommand( action )
    log.debug "sent"
}

// the below calledBackHandler() is triggered when the device responds to the sendHubCommand() with "device_description.xml" resource
void httpPostCallback(physicalgraph.device.HubResponse hubResponse) 
{
    log.debug "httpPostCallback status is ${hubResponse.status}"
    def body = hubResponse.json
    if( body ) {
	    log.debug "httpPostCallback body is: ${body}"
    } else {
    	log.debug "httpPostCallback body is null"
    }
}

def subscribeToButtons()
{
	buttons.each { button ->
/*
		list = button.supportedCommands
        log.debug "list ${list}, button ${button}"
		list.each {
	        log.debug "IT: ${it}"
    		log.debug "arguments for button '${button}' command ${it.name}: ${it.arguments}"
        }
*/
		subscribe(button, "button", buttonHandler)
	}
}

def dumpEvent(msg, evt)
{
    log.debug "${msg}-evt name: ${evt.name}"
    log.debug "${msg}-evt value: ${evt.value}"
    log.debug "${msg}-evt desc: ${evt.descriptionText}"
    log.debug "${msg}-evt disp name (id): ${evt.displayName} (${evt.deviceId})"

	def data = parseJson(evt.data)
    if( data ) {
	    log.debug "${msg}-evt data: ${data}"
    	log.debug "${msg}-evt key1: ${data.key1}"
	    log.debug "${msg}-evt key2: ${data.key2}"
    }
}

def onCommand(evt) 
{
	dumpEvent( "onCommand", evt )
}

def buttonHandler(evt) 
{
	dumpEvent( "buttonHandler", evt )

	def dev = evt.getDevice()
    log.debug "dev = ${dev}"
    def name = dev.displayName.replaceAll("\\s","")
	def data = parseJson(evt.data)
    if( data ) {
		name = name + "_" + data['buttonNumber']
    }
    def state = (evt.value == "pushed") ? 1 : 0
    postXTension( "${name}", "${state}", null )
}

/*
def debugEvent(message, displayEvent)
{
	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	debugOut( "Generating AppDebug Event: ${results}" )
	sendEvent (results)
}

def debugOut(msg)
{
	log.debug msg
	sendNotificationEvent(msg) //Uncomment this for troubleshooting only
}
*/