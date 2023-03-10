/**
 *  Auto Office
 *
 *  Copyright 2017 Joe Angell
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
 *  Listens for specific device to turn on/off, automatically turning other devices on/off and waking/sleeping
 *   computers.  Computer wake/sleep is handled by events sent to us over HTTP, and by us sending message to them.
 *
 */
definition(
    name: "Auto Office",
    namespace: "jangellx",
    author: "Joe Angell",
    description: "Automatically sleeps/wakes computers and turns on/off lights and other modules by listening for when those modules are turned on/off and when the computers sleep/wake.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Auto Office", displayLink: ""])


// Our preferences are pretty simple, and consist of a single page to select the on devices, off devices,
//  computer IPs, and the devices we control.  We also provide a way to get the API keys so the computers
//  can access the app when they wake/sleep.  We store the API key in state, but that's not available from
//  preferences(), so we need to create a dynamic page so that state() is available when we finally do need
//  it.  This implementation is liberally borrowed from the SmartThings HomeBridge app.
preferences {
	page( name: "prefsPage" )
}

def prefsPage() {
	if(!state.accessToken)
         createAccessToken()

    dynamicPage(name: "prefsPage", title: "Config", install:true, uninstall:true) {
    	section("When off, nothing is controlled.  Useful when traveling with your laptops, or otherwise away from home:") {
            input "masterEnable",        "bool",                   title:"Master Enable",           required:true
        }

        section("Wake when any of these switches turn on, detect motion or open:") {
            input "onSwitches",        "capability.switch",        title:"\"On\" Switches",         multiple:true,  required:false
            input "onMotionSensors",   "capability.motionSensor",  title:"\"On\" Motion Sensors",   multiple:true,  required:false
            input "onContactSensors",  "capability.contactSensor", title:"\"On\" Contact Sensors",  multiple:true,  required:false
        }

        section("Sleep when any of these switches turn off, stop detecting motion or close:") {
            input "offSwitches",       "capability.switch",        title:"\"Off\" Switches",        multiple:true,  required:false
            input "offMotionSensors",  "capability.motionSensor",  title:"\"Off\" Motion Sensors",  multiple:true,  required:false
            input "offContactSensors", "capability.contactSensor", title:"\"Off\" Contact Sensors", multiple:true,  required:false
        }

        section("Turn on or off these switches when waking or sleeping:") {
            input "doSwitches",        "capability.switch",        title:"Switches to Control",     multiple:true,  required:false
        }

        section("Computers to wake and sleep.  The address of each must be in the form of ip:port, such as 192.168.1.50:8080.  Each computer must be running a compatible daemon:") {
            input "computer1",        "text",                      title:"First Computer",          required:false
            input "computer2",        "text",                      title:"Second Computer",         required:false
            input "computer3",        "text",                      title:"Third Computer",          required:false
            input "computer4",        "text",                      title:"Fourth Computer",         required:false
            input "computer5",        "text",                      title:"Fifth Computer",          required:false
        }

        section( "View this SmartApp's configuration to use it in other places:" ) {
            href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Tap, select, copy, then click \"Done\""
        }
    }
}

// On install/unpdate, we just cal initialize() to subscribe to everything.
def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

// initialize() subscribes to our on and off devices.  We use the same handler
//  for all "on" devices, and another for all "off" devices, so that we can keep
//  the logic simple.
def initialize() {
	// Create the access token for URL access
	if(!state.accessToken)
         createAccessToken()

	if(!state.lastWakeTime)
    	state.lastWakeTime = 0

	// Subscribe to all of our "on" devices with the same handler
	subscribe( onSwitches,        "switch.on",             deviceOnHandler  );
	subscribe( onMotionSensors,   "motion.active",         motionOnHandler  );
	subscribe( onContactSensors,  "contactSensor.open",    deviceOnHandler  );

	// Subscribe to all of our "off" devices with the same handler
	subscribe( offSwitches,       "switch.off",            deviceOffHandler );
	subscribe( offMotionSensors,  "motion.inactive",       deviceOffHandler );
	subscribe( offContactSensors, "contactSensor.closed",  deviceOffHandler );
}

// Most devices we handle as soon as they go on or off, but for motion sensors we
//  ignore any "on" events if we received an "off" event in the last minute.  This
//  ensures that if we manually turned everything off (by turning off an appliance
//  module or sleeping the display ourselves), it won't automatically wake back up
//  as we're leaving the room.
def deviceOnHandler(event) {
	if( !masterEnable ) {
    	log.debug "Ignoring deviceOnHandler(); master enable false"
    	return
    }

	log.debug "\"On\" event received"
	doWake( true );
}

def motionOnHandler(event) {
	// Make sure we didn't go to sleep in the last 60 seconds
	if( !masterEnable ) {
    	log.debug "Ignoring motionOnHandler(); master enable false"
    	return
    }

	if( (state.lastWakeTime + (60 * 1000)) > now() ) {
		log.debug "Motion \"On\" event received too soon after \"off\" events; ignoring"
    	return;
    }

	log.debug "Motion \"On\" event received (after 60 second delay)"
	doWake( true );
}

def deviceOffHandler(event) {
	if( !masterEnable ) {
    	log.debug "Ignoring deviceOffHandler(); master enable false"
    	return
    }

    log.debug "\"Off\" event received"

	// Store when the "off" time for use with motion sensors
	state.lastWakeTime = now()

	doWake( false );
}

// This toggles the state of the doSwitches to the value passed in.
//  It also tells the computers to wake up.
def doWake( boolean wake ) {
	if( !masterEnable ) {
    	log.debug "Ignoring doWake(); master enable false"
    	return
    }

	log.debug "doWake() called with: $wake"

	// Turn on/off the switches
    if( wake )
	    doSwitches.on()
    else
        doSwitches.off()

	// Wake/sleep the computers
    def command = wake ? "wake" : "sleep"
	[ computer1, computer2, computer3, computer4, computer5 ].each { ipAndPort ->
		if( ipAndPort ) {
        	// Basic code from https://community.smartthings.com/t/lan-device-hubaction-post-w-json-using-rest-call/5665/3
			// HTTP JSON headers
			def headers = [:] 
            headers.put("HOST", "$ipAndPort")
            headers.put("Content-Type", "application/json")

			log.debug "Sending command $command to $ipAndPort"

			// Body just contains the command to issue as JSON
            def jsonData = [ command: command ]
            def json     = new groovy.json.JsonOutput().toJson(jsonData)

			log.debug json

            try {
            	// Create the action
                def hubAction = new physicalgraph.device.HubAction (
                    method:  "PUT",
                    path:    "/do",
                    body:    json,
                    headers: headers,
                )

                // Send the command
				sendHubCommand( hubAction )

			} catch (Exception e) {
                log.debug "Hit Exception $e on $hubAction"
            }
        }
    }
}

// HTTP mappings to control switches and get the config 
mappings {
    path( "/config"      )                  { action: [GET: "renderConfig" ] }
	path( "/do/:command" )                  { action: [PUT: "setSwitchesTo"] }
}

def setSwitchesTo() {
	if( !masterEnable ) {
    	log.debug "Ignoring setSwitchesTo(); master enable false"
    	return
    }

	def command = params.command

	if( command == "wake" ) {
		log.debug( "do/wake received" )
		doWake( true )

	} else if( command == "sleep" ) {
		log.debug( "do/sleep received" )
    	doWake( false )

	} else {
   		log.debug( "do/unknown -- error" )
    	httpError( 400, "$command is not valid" )
    }
}

// This provides the API strings needed for the computers to access the SmartApp.
def renderConfig() {
    def configJson = new groovy.json.JsonOutput().toJson([
        description:  "Auto Office Settings",
        app_url:       apiServerUrl("/api/smartapps/installations/"),
        app_id:        app.id,
        access_token:  state.accessToken,
        sleep_delay:   0
    ])

    def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
    render contentType: "text/plain", data: configString
}
