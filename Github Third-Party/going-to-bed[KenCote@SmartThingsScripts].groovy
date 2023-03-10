definition(
    name: "Going to Bed",
    namespace: "KenCote",
    author: "Ken Cote",
    description: "What to do when going to bed?",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom10-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Bedroom/bedroom10-icn@2x.png"
)

preferences {
    section("Trigger Devices"){
		input "switchOn", "capability.switch", multiple: true, title: "Devices?"
	}
    section("Action Devices") {
		input "fanLevel1", "capability.switchLevel", multiple: true, title: "Fan?"
        input "wakeupLights", "capability.switchLevel", title: "Wakeup Lights", required: false, multiple: true
    }
    section ("Preferences") {
        input "minutesToWait", "number", title: "Delay fan for how many minutes"
        input "stepDuration", "number", title: "Seconds delay between light increases"
        input "sleepTime", "time", title: "When to go to sleep"
		input "wakeTimeEarly", "time", title: "When to wake up early"
        input "wakeTime", "time", title: "When to wake up normal"
        input "weekendWakeTime", "time", title: "When to wake up on weekends"
        input "sleepTimer", "enum", title: "Sleep timer duration?", defaultValue: 90,
        	options: [
            	[1: "1"],
            	[30: "30"],
                [60: "60"],
                [90: "90"],
                [120: "120"],
                [240: "240"]
       		]
        input "earlyWakeUpDays", "enum", 
			title: "Early Wake Up Days?",
			multiple: true,
			options: [ 
            	['Mon': 'Monday'],
                ['Tue': 'Tuesday'],
                ['Wed': 'Wednesday'],
                ['Thu': 'Thursday'],
                ['Fri': 'Friday']
			],
            required: false
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
    subscribe(switchOn, "switch.on", switchOnHandler, [filterEvents: false])
	schedule(sleepTime, SleepTimeHandler)
    log.debug "init"  
}

def wakeUpEarlyDay()
{
    def todayDate = new Date() 
    String today = todayDate.format("E", location.timeZone)
       
    log.debug "earlywakeupdays ${earlyWakeUpDays}" 
    log.debug "today:  ${today}"
    
	if (today == "Sat" || today == "Sun")
    {
    	log.debug "Weekend!"
    	return null
    }
    
    if (earlyWakeUpDays == null)
    {
        log.debug "wakeUpEarlyDay:  No days chosen"
        return false
    }

    for (int i = 0; i < earlyWakeUpDays.size(); i++)
    {       
    	if (today == earlyWakeUpDays[i])
        {
        	log.debug "wakeUpEarlyDay:  Today is early wake up day"
        	return true
        } 
    }
    
    log.debug "wakeUpEarlyDay:  Today is NOT early wake up day"
    return false
}

def IsNight()
{
 	def now = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    def sunTime = getSunriseAndSunset();  
    def sunset = sunTime.sunset.format("yyyy-MM-dd HH:mm:ss", location.timeZone);
    def sunrise = sunTime.sunrise.format("yyyy-MM-dd HH:mm:ss", location.timeZone);
 
 	log.debug "${now}"
    log.debug "${sunset}"
    
    return now > sunset || now < sunrise
}

def switchOnHandler(evt) 
{
    if (IsNight())
    {
    	if (sleepTimer != null)
    	{
        	def delay = sleepTimer.toInteger() * 60
        	log.debug "switchOnHandler: Setting sleep timer to ${sleepTimer.toInteger()}"
        	runIn(delay, ShutStuffOff) 
        }
        
        if (minutesToWait > 0)
        {
        	TurnFanDown()
            log.debug "switchOffHandler: Cranking up fan ${fanLevel1} in ${minutesToWait} minutes"
        }
        
        def fanDelay = minutesToWait * 60
        runIn(fanDelay, SleepTimeHandler) 
    }
    else
    {
    	log.debug "switchOnHandler: Not night time."
    }
}

def TurnFanUp()
{
    log.debug "Setting fan to 99."
    fanLevel1.setLevel(100)
    schedule("0 00 05 * * ?", FanHandler)
}

def FanHandler(evt)
{
	if (wakeUpEarlyDay() != null)
    {
        if (wakeUpEarlyDay())
        {
            log.debug "Scheduling early wake up time"
            schedule(wakeTimeEarly, WakeTimeHandler)
        }
        else
        {
            log.debug "Scheduling normal wake up time"
            schedule(wakeTime, WakeTimeHandler)
        }
    }
    else
    {
        log.debug "Scheduling weekend wake up time"
        schedule(weekendWakeTime, WakeTimeHandler)
    }
}

def TurnFanDown()
{
    log.debug "Setting fan to 1."
    fanLevel1.setLevel(1)
}

def SleepTimeHandler(evt)
{
	TurnFanUp()
    wakeupLights.setLevel(0)
}

def WakeTimeHandler(evt)
{
	TurnFanDown()
    Brighten()
}

def ShutStuffOff()
{
	log.debug "Shutting off stuff"
    
    for (int i = 0; i < switchOn.size(); i++)
    {
        if (switchOn[i].currentValue("switch") == "on")
        {
            log.info "Shutting ${switchOn[i]} off"
            switchOn[i].off()
        }
    }
}

def Brighten() 
{
    def keepBrightening = wakeupLights.currentLevel < 100
    def dimStep = 2
    state.currentLevel = state.currentLevel + dimStep 

    if (keepBrightening)
    {
        wakeupLights.setLevel(state.currentLevel)
        runIn(stepDuration, dimProcess)   
    }
}