/**
 *  Turn lights on if presence is not detected and door opens after sunset
 *
 *  Author: kris@linquist.net
 *  Date: 2013-04-15
 */
preferences {
	section("When one of these doors opens..."){
		input "contact", "capability.contactSensor", title: "Where?", multiple: true, required: true
	}
    section("And this presence is not detected...") {
		input "presence", "capability.presenceSensor", title: "Which tag", multiple: false, required: true
	}
    section("Turn on these lights after sunset!") {
    	input "lights", "capability.switch", multiple: true, required: true
    }
    section("Zip code..."){
		 input "zipcode", "text", title: "Zipcode?"
	}
	section("Wundergound API key..."){
		input "apikey", "text", title: "API Key?"
	}
}

def installed()
{
	subscribeToEvents()
}

def updated()
{
	unsubscribe()
	subscribeToEvents()
}

private subscribeToEvents()
{
	contact.each {
		subscribe(it.contact)
	}
	subscribe(app)
}


def checkNighttime() {
	httpGet("http://api.wunderground.com/api/${apikey}/astronomy/q/${zipcode}.json") { response ->
	    def sunsettime = response.data.moon_phase.sunset.hour + response.data.moon_phase.sunset.minute
	    def sunrisetime = response.data.moon_phase.sunrise.hour + response.data.moon_phase.sunrise.minute
	    def currenttime = response.data.moon_phase.current_time.hour + response.data.moon_phase.current_time.minute
	    if ((currenttime.toInteger() >= sunsettime.toInteger()) || (currenttime.toInteger() <= sunrisetime.toInteger())) {
	        return true
	    } else {
	        return false
	    }
    }
}


def contact(evt)
{
	log.debug "$evt.name: $evt.value"
	if (evt.value == "open") {
		if (presence.latestValue == "not present"){
			if (checkNighttime()) {
    			log.debug "It's night time so I'm turning on the lights!"
                lights?.on()
                sendPush("Welcome home, I'm turning on the lights for you!")
    		} else {
    			log.debug "It's during the day. No need to turn on the lights."
    		}
		} else {
			log.debug "Door is open but you're already home so I'm not doing anything"
		}

	}
}
