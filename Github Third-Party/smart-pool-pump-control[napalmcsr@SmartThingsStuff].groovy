/**
 *  Smart Pool Pump
 *
 *  Turns on a Pump when you say and runs different times during the season
 *
 *  Copyright 2017 Craig Romei
 *  GNU General Public License v2 (https://www.gnu.org/licenses/gpl-2.0.txt)
 *
 */

definition(
    name: "Smart Pool Pump Control",
    namespace: "Craig.Romei",
    author: "Craig Romei",
    description: "Control a pool Pump based on the Seasons.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/craig-romei/poolpump.jpg",
    iconX2Url: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/craig-romei/poolpump.jpg",
    iconX3Url: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/craig-romei/poolpump.jpg"
)

preferences
{
	section("Pool Pump Switch")
    {
    	paragraph "The Pool Pump Switch"
		input "PoolSwitch", "capability.switch", title: "Pool Switch:", required: true
    }
    section("Pool Run Limits")
    {
		input "HotHours", "number", title: "Number of Hours to run in Hot Months:", required: true, defaultValue:  8
		input "ColdHours", "number", title: "Number of Hours to run in Cold Months:", required: true, defaultValue:  8
        input "TimeToRun", "time", title: "Time to execute every day"
	}
    section("Manual Activation")
    {
    	paragraph "When should the fan turn off when turned on manually?"
        input "ManualControlMode", "boolean", title: "Off After Manual-On?", required: true, defaultValue: true
        paragraph "How many minutes until the fan is auto-turned-off?"
        input "ManualOffMinutes", "number", title: "Auto Turn Off Time (minutes)?", required: false, defaultValue: 60
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
	state.humpres = false
    subscribe(PoolSwitch, "switch", PoolSwitchHandler)
    schedule(TimeToRun, Timehandler)
}

def Timehandler()
{
	float RunTime
    def now = new Date()
    log.debug "Time to Start The Pool!"
    log.debug "ColdHours: ${ColdHours} "
    log.debug "HotHours: ${HotHours} "
    state.AutomaticallyTurnedOn = true
    PoolSwitch.on()
//    Today_Month = now.getMonth()
    def Today_Month = now.month + 1
    log.debug "Today's Month: ${Today_Month}"
    switch (Today_Month){
    	case[12,1,2]:
        	RunTime = ColdHours
        break
    	case[3,11]:
        	RunTime = ColdHours + (HotHours - ColdHours) / 4
        break
    	case[4,10]:
        	RunTime = ColdHours + (HotHours - ColdHours) / 2
        break
    	case[5,9]:
        	RunTime = ColdHours + (HotHours - ColdHours) * 3 / 4
        break
    	case[6,7,8]:
        	RunTime = HotHours
        break
    }
    log.debug "RunTime: ${RunTime}"
    sendNotificationEvent("Running the Pool Pump for ${RunTime} hours.")
    runIn(60 * 60 * RunTime, TurnOffPoolSwitch)
}

def PoolSwitchHandler(evt)
{
	switch(evt.value)
    {
    	case "on":
        	log.debug "Pump turned on"
            if(evt.isPhysical())
            {
        		log.debug "Pump turned on physically"
                if(!state.AutomaticallyTurnedOn && ManualControlMode)
                {
                    if(ManualOffMinutes == 0)
                    {
        				log.debug "Pump Off"
                        PoolSwitch.off()
                    }
                    else
                    {
        				log.debug "Automatically turn off pool later"
                        runIn(60 * ManualOffMinutes, TurnOffPoolSwitch)
                    }
                }
			    state.AutomaticallyTurnedOn = false
            }
	        break
        case "off":
        	log.debug "Pump turned off"
		    state.AutomaticallyTurnedOn = false
        	break
    }
}



def TurnOffPoolSwitch()
{
    log.debug "Auto Pump Off called"
    if(PoolSwitch.currentValue("switch") == "on")
    {
        log.debug "Pump Off"
        PoolSwitch.off()
		state.AutomaticallyTurnedOn = false
    	sendNotificationEvent("Turning off the Pool Pump")
    }
}

