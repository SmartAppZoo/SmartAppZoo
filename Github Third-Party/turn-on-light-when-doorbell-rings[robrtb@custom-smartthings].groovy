definition(
    name: "Turn On Light when Doorbell Rings",
    namespace: "robrtb",
    author: "Robert Boyd",
    description: "Turn On Light when Doorbell Rings",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
		section("Select Button") {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true
		}
        
        section("Offsets") {
			paragraph "This will be enabled at sunset and disabled at sunrise."
			
        	input "sunsetOffset", "number", title: "Sunset Offset (minutes)", required: false, range: "-1439..1439", defaultValue: 0
			input "sunriseOffset", "number", title: "Sunrise Offset (minutes)", required: false, range: "-1439..1439", defaultValue: 0
        }
        
        section("Select Light") {
			input "light", "capability.switch", title: "Light", multiple: false, required: true
		}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize()
{
	log.debug "'Turn On Light when Doorbell Rings' initializing..."
    subscribe(location, "sunsetTime", sunsetTimeHandler)
	subscribe(location, "sunriseTime", sunriseTimeHandler)
    subscribe(buttonDevice, "button.pushed", buttonEvent)
    
    setInitialState()
    
    log.debug "'Turn On Light when Doorbell Rings' initialized.  state.enabled = ${state.enabled}"
}

def setInitialState()
{
    if (isItNight())
    {
    	log.trace "setInitialState() - setting state.enabled = true"
        state.enabled = true
        scheduleEnabledDisabled(location.currentValue("sunsetTime"), sunsetOffset, true)
        scheduleEnabledDisabled(location.currentValue("sunriseTime"), sunriseOffset, false)
    }
    else
    {
    	log.trace "setInitialState() - setting state.enabled = false"
        state.enabled = false
        scheduleEnabledDisabled(location.currentValue("sunriseTime"), sunriseOffset, false)
        scheduleEnabledDisabled(location.currentValue("sunsetTime"), sunsetOffset, true)
    }
}

def sunsetTimeHandler(evt)
{
	scheduleEnabledDisabled(evt.value, sunsetOffset, true)
}

def sunriseTimeHandler(evt)
{
    scheduleEnabledDisabled(evt.value, sunriseOffset, false)
}

def scheduleEnabledDisabled(scheduleTime, offset, enabledState)
{
	def parsedTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", scheduleTime)
    def parsedOffsetTime
    
    if (offset == null)
    	offset = 0
    
    if (offset == 0)
    {
    	parsedOffsetTime = parsedTime
    }
    else
    {
    	parsedOffsetTime = new Date(parsedTime.time + (offset * 60 * 1000))
    }
	
    log.debug "Scheduling 'changeStateEnabledValue'='$enabledState' for: $parsedOffsetTime"
    runOnce(parsedOffsetTime, changeStateEnabledValue, [overwrite: false, data: [flag: enabledState]])
}

def changeStateEnabledValue(data) {
    log.debug "Setting state.enabled to ${data.flag}"
    state.enabled = data.flag
}

def buttonEvent(evt)
{
	log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
    
	if(state.enabled == true)
    {
		def buttonNumber = evt.data // why doesn't jsonData work? always returning [:]
		def value = evt.value
		
		def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
		log.debug "Found ${recentEvents.size()?:0} events in past 3 seconds"
		
		if(recentEvents.size <= 1)
        {
        	if (light.currentValue('switch').contains('off'))
            {
            	turnOn()
            }
            else
            {
            	//light is on already, set timer to turn off
            	scheduleTurnOff()
            }
		}
        else
        {
			log.debug "Found recent button press events for $buttonNumber with value $value"
		}
	}
}

def turnOn()
{
	light.on()
    log.debug "Lights turned on."
    scheduleTurnOff()
}

def turnOff()
{
	light.off()
    log.debug "Lights turned off."
}

def scheduleTurnOff()
{
	log.debug "Scheduling lights off..."
	runIn(5*60, turnOff)
}

private isItNight()
{
	if(getSunriseAndSunset().sunrise.time < now() || getSunriseAndSunset().sunset.time > now())
	{
	return true
	}
	else
	{
		return false
	}
}