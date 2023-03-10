/**
 *  LGTVNotification
 *
 *  MIT License
 *  Copyright 2017 Sean Savage
 *  
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */
 
import groovy.json.JsonBuilder

definition(
    name: "LGTVNotification",
    namespace: "technothingy",
    author: "Sean Savage",
    description: "Smartthings SmartApp to send notification to LGTV",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("LGTVNotification") {
		paragraph "Choose the prefrences below to send a notification when there is motion detected."
	}
	section("Notify when motion detected:") {
        input "themotion", "capability.motionSensor", required: true, title: "Where?"
    }
    section("Message to send:") {
        input(name: "message", type: "text", title: "Message", required: true)
    }
	section("LGTV Rest Server") {
		input(name: "ip", type: "text", title: "WebService IP Address", required: true)
		input(name: "port", type: "text", title: "WebService Port", required: true)
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
	subscribe(themotion, "motion.active", motionDetectedHandler)
}

def motionDetectedHandler(evt) {
    log.debug "motionDetectedHandler called: $evt"
    log.debug "evt.displayName: $evt.displayName"
	def json = new JsonBuilder()
	json.call("msg":"$settings.message in $evt.displayName")

	def headers = [:] 
	headers.put("HOST", "$settings.ip:$settings.port")
	headers.put("Content-Type", "application/json")

	log.debug "The Header is $headers"

	def method = "POST"

	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: "/toast",
			body: json.content,
			headers: headers,
		)
	
		log.debug hubAction
		sendHubCommand(hubAction)
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}


