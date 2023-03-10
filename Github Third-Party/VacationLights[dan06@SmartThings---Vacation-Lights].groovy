/**
 *  Vacation Lights
 *
 *  Author: dpvorster
 *  Date: 2015-04-20
 *
 *  Most of the code is written by Tim Slagle.  My modifications were to make it behave as I expected it to
 *  (not run outside of the time limits set), and added support to start and stop a Harmony activity when
 *  the app starts and ends.
 */


definition(
    name: "Vacation Lights",
    namespace: "dpvorster",
    author: "Daniel Vorster",
    description: "Randomly turn on/off lights to simulate the appearance of an occupied home while you are away.",
    iconUrl: "http://icons.iconarchive.com/icons/custom-icon-design/mono-general-2/512/settings-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/custom-icon-design/mono-general-2/512/settings-icon.png"
)

preferences {
	page(name: "Setup", title: "", nextPage:"settings") { 
		section("Which mode change triggers the simulator? (This app will only run in selected mode(s))") {
			input "modes", "mode", title: "Which?", multiple: true, required: false, refreshAfterSelection:true
		}
		section("Light switches to turn on/off"){
			input "switches", "capability.switch", title: "Switches", multiple: true, required: true, refreshAfterSelection:true
		}
		section("How often to cycle the lights"){
			input "frequency_minutes", "number", title: "Minutes?"
		}
		section("Number of active lights at any given time"){
			input "number_of_active_lights", "number", title: "Number of active lights"
		}
        section("Logitech Harmony"){
        	input "harmony", "capability.mediaController", title: "Harmony Hub", required: false
        }
	}
    
    page(name: "settings", title: "Settings")
    
    page(name: "timeIntervalInput", title: "Only during a certain time", refreshAfterSelection:true) {
		section {
			input "starting", "time", title: "Starting (both are required)", required: false, refreshAfterSelection:true
			input "ending", "time", title: "Ending (both are required)", required: false, refreshAfterSelection:true
		}
	}
 }
 
 def settings()
 {
	dynamicPage(name: "settings", title: "Settings", install:true, uninstall:true) { 
		section("Delay to start simulator... (defaults to 2 min)") {
			input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
		}

		section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		} 
		
		section("More options", refreshAfterSelection:true) {
			href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete", refreshAfterSelection:true
            	input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
		input "modes", "mode", title: "Set for specific mode(s)", multiple: true, required: false
		}
        
        if (harmony != null)
        {
        	harmony.getAllActivities()
        	section ("Harmony activity", refreshAfterSelection:true) {
        		input "activity", "enum", title: "Activity?", required: true, options: new groovy.json.JsonSlurper().parseText(harmony?.latestValue('activities') ?: "[]").collect { ["${it.id}": it.name] }
        	}
         }
	}
}

def installed() 
{
	initialize()
}

def updated() 
{
  unsubscribe();
  unschedule();
  initialize()
}

def initialize()
{
	if (modes != null) 
	{
		subscribe(location, modeChangeHandler)
		modeChangeHandler (location.mode)
    }
}

def modeChangeHandler(evt) 
{
	log.debug "Mode change to: ${evt.value}"
    if (modeOk) 
	{
		def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 2 * 60 
    	runIn(delay, initializeSchedule)
    } 
	else
	{
		unschedule()
		sendNotificationEvent("Vacation Lights inactive in mode $location.mode")
	}
}

def initializeSchedule()
{
	if (timeOk) 
    {
    	setSchedule()
    }
    else 
    {
    	resetSchedule()
    }
}

// Set schedule
private setSchedule()
{
	unschedule()
    log.debug "Starting schedule for vacation lights"
    sendNotificationEvent("Running Vacation Lights")
	
	// To simplify time calculations and get expected behavior, simply schedule 
	// a timer to stop when the ending time is reached.
	schedule(ending, 'stopSchedule')
	
	// And start today's vacation lights
	scheduleCheck()
}

// Reset schedule for the next day
private resetSchedule()
{
	unschedule()
    schedule(starting, 'setSchedule')
    log.debug "Setting Vacation Lights to start at $starting"
    sendNotificationEvent("Vacation lights will start at $starting")
}

// Called when we're done - turn off lights and set up for tomorrow.
private stopSchedule()
{
	unschedule()
	switches.off()
    harmony?.startActivity("off");
	resetSchedule()
}

// We want to turn off all the lights
// Then we want to take a random set of lights and turn those on
// Then run it again when the frequency demands it
def scheduleCheck() 
{
	if(allOk)
	{
		log.debug("Running")
        
        // Start harmony activity
        harmony?.startActivity(activity)
		
		// turn off all the switches
		switches.off()
		
		// Turn on random switches
		turnOnRandomSwitches()
		
		// re-run again when the frequency demands it
		runIn(frequency_minutes * 60, scheduleCheck)
	}
	else
	{
		// Suspenders and a belt
		resetSchedule()
	}
}    

def turnOnRandomSwitches()
{
	// grab a random switch
	def random = new Random()
	def inactive_switches = switches
	for (int i = 0 ; i < number_of_active_lights ; i++) 
	{
		// if there are no inactive switches to turn on then let's break
		if (inactive_switches.size() == 0){
		  break
		}

		// grab a random switch and turn it on
		def random_int = random.nextInt(inactive_switches.size())
		inactive_switches[random_int].on()

		// then remove that switch from the pool off switches that can be turned on
		inactive_switches.remove(random_int)
	}
}

//below is used to check restrictions
private getAllOk() 
{
	modeOk && daysOk && timeOk
}

private getModeOk() 
{
	def result = !modes || modes.contains(location.mode)
	log.trace "modeOk = $result"
	result
}

private getDaysOk() 
{
	def result = true
	if (days) {
		def df = new java.text.SimpleDateFormat("EEEE")
		if (location.timeZone) {
			df.setTimeZone(location.timeZone)
		}
		else {
			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
		}
		def day = df.format(new Date())
		result = days.contains(day)
	}
	log.trace "daysOk = $result"
	result
}

private getTimeOk() 
{
	def result = true
	if (starting && ending) 
	{
		def currTime = now()
		def start = timeToday(starting).time
		def stop = timeToday(ending).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}

private hhmm(time, fmt = "h:mm a")
{
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private getTimeLabel()
{
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
