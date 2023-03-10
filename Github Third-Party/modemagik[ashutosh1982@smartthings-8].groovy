/**   Name: Nobody Home Improved with Away Night/Day Modes V2.0
 *   
 *    Author: Tim Slagle/ImBrian
 *    
 *    Monitor a set of presence sensors and change mode based on when your home is empty or occupied.  
 *	  Included Night and Day modes for both an occupied and unoccupied house.
 */

// Automatically generated. Make future change here.
definition(
	name: "Auto away/home with night modes",
	namespace: "pursual",
	author: "TonyG",
	description: "Monitor a set of presence sensors and change mode based on when your home is empty or occupied.  Included Night and Day modes for both an occupied and unoccupied house.",
	category: "Mode Magic",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
	)

preferences {
	page(name: "selectPhrases")
	
	page( name:"Settings", title:"Settings", uninstall:true, install:true ) {
		section("False alarm threshold (defaults to 10 min)") {
		 input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
	 }

	 section("Notifications") {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
	}
}
}

def selectPhrases() {
	def configured = (settings.awayDay && settings.awayNight && settings.homeDay && settings.awayDay)
	dynamicPage(name: "selectPhrases", title: "Configure", nextPage:"Settings", uninstall: true) {		
		section("Who?") {
			input "people", "capability.presenceSensor", title: "Monitor These Presences", required: true, multiple: true,  refreshAfterSelection:true
		}

		def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
			phrases.sort()
			section("Run This Phrase When...") {
				log.trace phrases
				input "awayDay", "enum", title: "Everyone Is Away And It's Day", required: true, options: phrases,  refreshAfterSelection:true
				input "awayNight", "enum", title: "Everyone Is Away And It's Night", required: true, options: phrases,  refreshAfterSelection:true
				input "homeDay", "enum", title: "At Least One Person Is Home And It's Day", required: true, options: phrases,  refreshAfterSelection:true
				input "homeNight", "enum", title: "At Least One Person Is Home And It's Night", required: true, options: phrases,  refreshAfterSelection:true
			}
		}
	}
}

def installed() {
	init()
	subscribe(app)
}

def updated() {
	unsubscribe()  
	init()
    state.sunMode = null
    runIn(8, "doUpdate")
}

def init() {
	subscribe(people, "presence", presence)
	subscribe(location, "sunrise",  setSunrise)
	subscribe(location, "sunset",   setSunset)
}

def uninstalled() {
	unsubscribe()
}

def initialSunPosition() {  
	def s = getSunriseAndSunset()
	def now = new Date()
    def riseTime = s.sunrise
	def setTime = s.sunset


    if(setTime.before(now) || riseTime.after(now)) {
        state.sunMode = "sunset"
        log.info("init sunset")
    } else {
    	state.sunMode = "sunrise"
        log.info("init sunrise")
    }															//at 12:13PM every day
}

def setSunrise(evt) {
	state.sunMode = "sunrise";
	doUpdate()
}

def setSunset(evt) {
	state.sunMode = "sunset";
	doUpdate()
}

def presence(evt) {
	if(evt.value == "not present" && !anyoneIsHome()) {
        log.info("Nobody is home, running away sequence")
        def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60 
        //runIn(delay, "doUpdate")
        doUpdate()
	} else {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= 1 * 60000) {
			log.info("Someone is home, running home sequence")
			doUpdate()
		} else {
        	send("Would have repeated home")
        }
		state[evt.deviceId] = now()
	}
}

def doUpdate() {
	if (state.sunMode == null) {
    	initialSunPosition()
    }
	
    if (anyoneIsHome()) {
    	if(state.sunMode == "sunset" && location.mode != "HomeNight"){
			def message = "Performing \"${homeNight}\" for you as requested."
			log.info(message)
            send(message)
			location.helloHome.execute(settings.homeNight)
		} else if(state.sunMode == "sunrise" && location.mode != "HomeDay"){
			def message = "Performing \"${homeDay}\" for you as requested."
			log.info(message)
            send(message)
			location.helloHome.execute(settings.homeDay)
		} else {
        	log.debug("home already?")
        }
    } else {
    	if(state.sunMode == "sunset" && location.mode != "AwayNight") {
        	def message = "Performing \"${awayNight}\" for you as requested."
			log.info(message)
			send(message)
			location.helloHome.execute(settings.awayNight)
        } else if (state.sunMode == "sunrise" && location.mode != "AwayDay") {
        	def message = "Performing \"${awayDay}\" for you as requested."
			log.info(message)
			send(message)
			location.helloHome.execute(settings.awayDay)
        } else {
        	log.debug("away already?")
        }
    }
}

private anyoneIsHome() {
	def result = false

	if(people.findAll { it?.currentPresence == "present" }) {
		result = true
	}

	log.debug("anyoneIsHome: ${result}")

	return result
}

private send(msg) {
	if(sendPushMessage != "No") {
		log.debug("Sending push message")
		sendPush(msg)
	}

	log.debug(msg)
}
