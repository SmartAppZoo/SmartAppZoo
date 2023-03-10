/**
 *  ecobeeSetBacklight
 *
 *  Copyright Yves Racine
 *  LinkedIn profile: ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/
 *
 *  Developer retains all right, title, copyright, and interest, including all copyright, patent rights, trade secret 
 *  in the Background technology. May be subject to consulting fees under the Agreement between the Developer and the Customer. 
 *  Developer grants a non exclusive perpetual license to use the Background technology in the Software developed for and delivered 
 *  to Customer under this Agreement. However, the Customer shall make no commercial use of the Background technology without
 *  Developer's written consent.
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *
 * Software Distribution is restricted and shall be done only with Developer's written approval.
 * 
 * You may want to create multiple instances of this smartapp (and rename them in SmartSetup) for each time
 * you want to set different backlight settings at a given day and time during the week.*
 *
 *  N.B. Requires MyEcobee device v5.9.9.x.3 available at 
 *          http://www.ecomatiqhomes.com/#!store/tc3yr 
 */
definition(
    name: "${get_APP_NAME()}",
	namespace: "yracine",
	author: "Yves Racine",
	description: "This script allows an user to set the ecobee backlight settings (backlightOnIntensity, backlightSleepIntensity, backlightOffTime)",
	category: "My Apps",
 	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/ecobee@2x.png"
)


def get_APP_VERSION() {return "1.1"}



preferences {

	page(name: "selectThermostats", title: "Thermostats", install: false, uninstall: true, nextPage: "selectBacklight") {
		section("About") {
			paragraph image:"${getCustomImagePath()}ecohouse.jpg","${get_APP_NAME()}, the smartapp that sets the ecobee's backlight settings"
			paragraph "Version ${get_APP_VERSION()}\n" 
			paragraph "If you like this smartapp, please support the developer via PayPal and click on the Paypal link below " 
				href url: "https://www.paypal.me/ecomatiqhomes",
					title:"Paypal donation..."
			paragraph "Copyright??2018-2020 Yves Racine"
				href url:"https://github.com/yracine/device-type.myecobee", style:"embedded", required:false, title:"More information..."  
					description: "https://github.com/yracine/device-type.myecobee/blob/master/README.md"
		}
		section("Set the ecobee thermostat(s)") {
			input "ecobee", "capability.thermostat", title: "My ecobee thermostat(s)?", multiple: true

		}
		section("Configuration") {
			input "dayOfWeek", "enum",
				title: "Which day of the week?",
				multiple: false,
				options: [
						'All Week',
						'Monday to Friday',
						'Saturday & Sunday',
						'Monday',
						'Tuesday',
						'Wednesday',
						'Thursday',
						'Friday',
						'Saturday',
						'Sunday'
				]
			input "begintime", "time", title: "Beginning time"
		}

	}
	page(name: "selectBacklight", title: "Ecobee Backligth Settings", content: "selectBacklight")
	def enumModes=location.modes.collect{ it.name }

	page(name: "Notifications", title: "Notification Options", install: true, uninstall: true) {
        if (isST()) {        
    		section("Notifications") {
	    		input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required:
		    		false
		        input "phone", "phone", title: "Send a Text Message?", required: false
		}
    		section("Enable Amazon Echo/Ask Alexa Notifications (optional)") {
	    		input (name:"askAlexaFlag", title: "Ask Alexa verbal Notifications [default=false]?", type:"bool",
		    		description:"optional",required:false)
    			input (name:"listOfMQs",  type:"enum", title: "List of the Ask Alexa Message Queues (default=Primary)", options: state?.askAlexaMQ, multiple: true, required: false,
	    			description:"optional")            
    			input "AskAlexaExpiresInDays", "number", title: "Ask Alexa's messages expiration in days (optional,default=5 days)?", required: false
	    	}
        }            
	section("Set for specific ST location mode(s) [default=all]")  {
			input (name:"selectedModes", type:"enum", title: "Choose ST Mode(s) to run the smartapp", options: enumModes, required: false, multiple:true) 
	}
        section([mobileOnly:true]) {
            label title: "Assign a name for this SmartApp", required: false
        }
	}
}

def selectBacklight() {


	return dynamicPage(name: "selectBacklight", title: "Select Ecobee Backlight Settings", install: false, uninstall: true, nextPage:
		"Notifications") {
		section("") {
			input (name: "backlightOnIntensity",  type: "number", title: "Backlight On Intensity [0-10]?", range: "0..10", required: false)
			input (name: "backlightSleepIntensity",  type: "number", title: "Backlight Sleep Intensity [0-10]?", range: "0..10", required: false)
			input (name: "backlightOffTime", type: "number", title: "BackLight off Time in seconds?", required: false)
		}
	}
}

boolean isST() { 
    return (getHub() == "SmartThings") 
}

private getHub() {
    def result = "SmartThings"
    if(state?.hub == null) {
        try { [value: "value"]?.encodeAsJson(); } catch (e) { result = "Hubitat" }
        state?.hub = result
    }
    log.debug "hubPlatform: (${state?.hub})"
    return state?.hub
}


def installed() {
	// subscribe to these events
	initialize()
}

def updated() {
	// we have had an update
	// remove everything and reinstall
	unsubscribe()
	unschedule()    
	initialize()
}

def appTouch(evt) {
	setBacklight()
}

def askAlexaMQHandler(evt) {
	if (!evt) return
	switch (evt.value) {
		case "refresh":
		state?.askAlexaMQ = evt.jsonData && evt.jsonData?.queues ? evt.jsonData.queues : []
		log.debug "askAlexaMQHandler>new refresh value=$evt.jsonData?.queues"
  		break
	}
}


def initialize() {

	log.debug "Scheduling setBacklight settings for day " + dayOfWeek + " at begin time " + begintime

	subscribe(location, "askAlexaMQ", askAlexaMQHandler)

	schedule(begintime,setBacklight)
    
	subscribe(app, appTouch)    

}


def setBacklight() {
	def doChange = IsRightDayForChange()

	// If we have hit the condition to schedule this then lets do it

	if (doChange == true) {

		boolean foundMode=selectedModes.find{it == (location.currentMode as String)} 
		if ((selectedModes != null) && (!foundMode)) {
			log.debug "not the right mode to run the smartapp, location.mode= $location.mode, selectedModes=${selectedModes},foundMode=${foundMode}, exiting"
			return            
		}
		ecobee.each {
			if (backlightOffTime != null  && backlightOffTime>0) {
				log.debug "About to change backlightOffTime, new value=$backlightOffTime"
				it.setBacklightOffTime(backlightOffTime)                
				send("set ${it} with backlightOffTime=$backlightOffTime seconds as requested", settings.askAlexaFlag)
			}           
			if (backlightSleepIntensity != null) {
				log.debug "About to change backlightSleepIntensity, new value=$backlightSleepIntensity"
				it.setBacklightSleepIntensity(backlightSleepIntensity)                
				send("set ${it} with backlightSleepIntensity=$backlightSleepIntensity as requested", settings.askAlexaFlag)
			}            
			if (backlightOnIntensity!= null) {
				log.debug "About to change backlightOnIntensity, new value=$backlightOnIntensity"
				it.setBacklightOnIntensity(backlightOnIntensity)                
				send("set ${it} with backlightOnIntensity=$backlightOnIntensity as requested", settings.askAlexaFlag)
			}            
		}            
	} else {
		log.debug("setBacklight>not the right time to set ${ecobee}'s backlight settings")
	}
	log.debug "End of Fcn"
}


def IsRightDayForChange() {

	def makeChange = false
	String currentDay = new Date().format("E", location.timeZone)
    
	// Check the condition under which we want this to run now
	// This set allows the most flexibility.
	if (dayOfWeek == 'All Week') {
		makeChange = true
	} else if ((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Mon') {
		makeChange = true
	} else if ((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Tue') {
		makeChange = true
	} else if ((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Wed') {
		makeChange = true
	} else if ((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && currentDay == 'Thu') {
		makeChange = true
	} else if ((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') &&  currentDay == 'Fri') {
		makeChange = true
	} else if ((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && currentDay == 'Sat') {
		makeChange = true
	} else if ((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && currentDay == 'Sun' ) {
		makeChange = true
	}


	// some debugging in order to make sure things are working correclty
	log.debug "Calendar DOW: " + currentDay
	log.debug "SET DOW: " + dayOfWeek

	return makeChange
}


private send(msg, askAlexa=false) {

	def message = "${get_APP_NAME()}>${msg}"


	if (sendPushMessage == "Yes") {
		log.debug "about to send notifications"
		sendPush(message)
	}
	if (askAlexa) {
		def expiresInDays=(AskAlexaExpiresInDays)?:5    
		sendLocationEvent(
			name: "AskAlexaMsgQueue", 
			value: "${get_APP_NAME()}", 
			isStateChange: true, 
			descriptionText: msg, 
			data:[
				queues: listOfMQs,
				expires: (expiresInDays*24*60*60)  /* Expires after 5 days by default */
			]
		)
	} /* End if Ask Alexa notifications*/
	
	if (phoneNumber) {
		log.debug("sending text message")
		sendSms(phoneNumber, message)
	}
}




// catchall
def event(evt) {
	log.debug "value: $evt.value, event: $evt, settings: $settings, handlerName: ${evt.handlerName}"
}

def getCustomImagePath() {
	return "https://raw.githubusercontent.com/yracine/device-type.myecobee/master/icons/"
}    

private def get_APP_NAME() {
	return "ecobeeSetBacklight"
}
