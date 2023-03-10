/**
 *  Turn On Only If I Arrive After Sunset
 *
 *  Author: Danny De Leo
 */
definition(
    name: "Coming Home",
    namespace: "KenCote",
    author: "Ken Cote",
    description: "What to do when you get home?",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home4-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home4-icn@2x.png"
)

preferences {
	section("When I arrive and leave..."){
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Turn on/off a light..."){
		input "dayLights", "capability.switch", multiple: true
    }
	section("Turn on/off a light at night..."){
		input "nightLights", "capability.switch", multiple: true
	}
    section("Open/close garage door..."){
		input "gdc1", "capability.garageDoorControl", multiple: true
	}
    section("Minutes to wait before reversing?") {
		input "minutesToWait", "number"
	}
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()
}

def initialize()
{
   	subscribe(presence1, "presence", presenceHandler)
    subscribe(nightLights, "switch.on", homeHandler, [filterEvents: false])
    subscribe(dayLights, "switch.on", homeHandler, [filterEvents: false])
    subscribe(gdc1, "door.open", homeHandler, [filterEvents: false])
}

def presenceHandler(evt)
{
	log.debug "Presence handler: Event Name: ${evt.name}, Value: ${evt.value}, State Changed ${evt.isStateChange()}"

	if(evt.isStateChange() && evt.value == "present") 
    {
        state.fromPresence = true
		dayLights.on()
        gdc1.open()

        def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
        def sunTime = getSunriseAndSunset();  
        def sunset = sunTime.sunset.format("yyyy-MM-dd HH:mm:ss", location.timeZone);

        if (now > sunset) 
        {  
			nightLights.on();
		}
	}
}

def homeHandler(evt) 
{
	if (state.fromPresence)
    {
        def delay = minutesToWait * 60  
        log.debug "Turning off ${switch1} and closing garage ${gdc1} in ${delay} seconds: Event Name: ${evt.name}, Value: ${evt.value}, State Changed ${evt.isStateChange()}"
        runIn(delay, closeUpHandler)
        state.fromPresence = false
    }
}

def closeUpHandler(evt)
{
	dayLights.off()
    nightLights.off()
    gdc1.close()
}