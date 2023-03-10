/**
 * DirecThings v0.32
 *
 * Created by Joe Beeson <jbeeson@gmail.com>
 *
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

definition(
	author	   : "Joe Beeson",
	category   : "Convenience",
	description: "Control your DirecTV receiver",
	iconUrl    : "http://a4.mzstatic.com/us/r30/Purple18/v4/41/93/a8/4193a8ee-0863-0458-7d03-fd050517c400/icon175x175.jpeg",
	iconX2Url  : "http://a4.mzstatic.com/us/r30/Purple18/v4/41/93/a8/4193a8ee-0863-0458-7d03-fd050517c400/icon175x175.jpeg",
	iconX3Url  : "http://a4.mzstatic.com/us/r30/Purple18/v4/41/93/a8/4193a8ee-0863-0458-7d03-fd050517c400/icon175x175.jpeg",
	name	   : "DirecThings",
	namespace  : "joebeeson"
)

preferences {
	section("Receiver") {
		href(
			description: "For help with setting up your receivers to be remotely controlled, click here.",
			name	   : "hrefNotRequired",
			required   : false,
			style      : "external",
			title      : "Receiver remote control help",
			url 	   : "https://dtv.custhelp.com/app/answers/detail/a_id/3150/related/1"
		)
		input(
			name	: "receiverIpStr", 
			required: true,
			type	: "text", 
			title   : "IP Address"
		)
		input(
			defaultValue: 8080,
			name	    : "receiverPortInt", 
			range	    : "1..65535",
			required    : true,
			type        : "number", 
			title	    : "Port"
		)
		paragraph "You only need to set the below field if attempting to control a client (not primary) receiver."
		input(
			defaulValue: "0", 
			name	   : "receiverMacStr", 
			required   : false,
			type	   : "text", 
			title	   : "MAC Address"
		)
	}
	section("Control, monitor power with this switch") {
		input(
			name	: "monitorSwitchObj", 
			required: true,
			type	: "capability.switch"
		)
	}
}

/**
 * Subscription callabck for "switch.off" event.
 *
 * @param	eventWrapperObj		EventWrapper object.
 */
def doHandleSwitchOffEvent(eventWrapperObj) {
	if (state.receiverIsOn) {
		log.debug "[doHandleSwitchOffEvent] Received 'switch.off' event, triggering 'poweroff' key"
		doReceiverRequestKey("poweroff")
    } else {
    	log.debug "[doHandleSwitchOffEvent] Received 'switch.off' event but receiver is already off"
    }
}

/**
 * Subscription callabck for "switch.on" event.
 *
 * @param	eventWrapperObj		EventWrapper object.
 */
def doHandleSwitchOnEvent(eventWrapperObj) {
	if (!state.receiverIsOn) {
		log.debug "[doHandleSwitchOffEvent] Received 'switch.on' event, triggering 'poweron' key"
		doReceiverRequestKey("poweron")
    } else {
    	log.debug "[doHandleSwitchOffEvent] Received 'switch.on' event but receiver is already on"
    }
}

/**
 * Sends a `physicalgraph.device.HubAction` hub command to request the status of
 * the receiver. Responses are received in the `requestResponseHandler` function
 *
 * @see		requestResponseHandler
 */
def doReceiverModeRequest() {
	return sendHubCommand(
		new physicalgraph.device.HubAction(
			headers: [
				HOST: "${settings.receiverIpStr}:${receiverPortInt}"
			],
			method : "GET",
			path   : "/info/mode",
			query  : []
		)
	)
}

/**
 * Sends a receiver request to press a key.
 *
 * @param	keyNameStr			Name of the key to send.
 * @see		doReceiverRequest
 */
def doReceiverRequestKey(keyNameStr) {
	log.debug "[doReceiverRequestKey] Sending key '${keyNameStr}'"
	return doReceiverRequest(
		"/remote/processKey",
		[
			hold: "keyPress",
			key : keyNameStr
		]
	)
}

/**
 * Send a receiver request.
 *
 * @param	requestUrlStr		The path, without query parameters, to send. Required.
 * @param	requestQueryMap		Query parameters to send. Default empty.
 * @param	requestMethodStr	HTTP Method to use for the request. Default "GET"
 */
def doReceiverRequest(requestUrlStr, requestQueryMap = [], requestMethodStr = "GET") {
	log.debug "[doReceiverRequest] Requesting '${requestUrlStr}' with query " +
			  "'${requestQueryMap}' as '${requestMethodStr}' to " + 
			  "'${settings.receiverIpStr}:${receiverPortInt}'"
	sendHubCommand(
		new physicalgraph.device.HubAction(
			headers: [
				HOST: "${settings.receiverIpStr}:${receiverPortInt}"
			],
			method : requestMethodStr,
			path   : requestUrlStr,
			query  : requestQueryMap + [
				clientAddr: (settings.receiverMacStr ?: "0").replace(":", "").toUpperCase()
			]
		)
	)
}

/**
 * Triggered to handle HTTP responses. 
 *
 * @param	eventWrapperObj		EventWrapper object.
 */
def requestResponseHandler(eventWrapperObj) {
    def responseMap = new groovy.json.JsonSlurper().parseText(
    	parseLanMessage(eventWrapperObj.description).body
   )
   if (responseMap.mode == 1) {
   	   state.receiverIsOn = false
       if (settings.monitorSwitchObj.currentSwitch == "on") {
       	   log.debug "[requestResponseHandler] Device appears off, but switch is on. Changing switch state..."
           settings.monitorSwitchObj.off()
       }
   } else {
   	   state.receiverIsOn = true
       if (settings.monitorSwitchObj.currentSwitch == "off") {
           log.debug "[requestResponseHandler] Device appears on, but switch is off. Changing switch state..."
           settings.monitorSwitchObj.on()
       }
   }
   runIn(15, doReceiverModeRequest)
}

def initialize() {
	subscribe(location, null, requestResponseHandler, [filterEvents:false])
    subscribe(settings.monitorSwitchObj, "switch.on", doHandleSwitchOnEvent)
    subscribe(settings.monitorSwitchObj, "switch.off", doHandleSwitchOffEvent)
   	runIn(15, doReceiverModeRequest)
}

def installed() {
	log.debug "[installed] Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "[updated] Updated with settings: ${settings}"
    unschedule()
	unsubscribe()
	initialize()
}