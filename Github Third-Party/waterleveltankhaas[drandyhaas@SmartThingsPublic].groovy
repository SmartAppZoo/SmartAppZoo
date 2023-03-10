definition(
    name: "WaterLevelTankHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Warn when the water level in a tank gets low.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
	section("Choose sensor... ") {
		input "sensor", "capability.sensor"
	}
	section("Warning level...") {
		input "warnlevel", "number", title: "Warning distance at least?"
	}
}

def checkforchanges(){
    log.debug("checking for changes")
}

def installed()
{
    log.debug("installed")
    state.warned = 0
	subscribe(sensor, "greeting", handler)
}

def updated()
{
	unsubscribe()
	installed()
}

def handler(evt)
{
	log.debug "water level warning app: $evt.name, $evt.value, $settings"
    
    log.debug("warnlevel is $warnlevel")
    
    def level = evt.value.substring(0, evt.value.length() - 2)
    log.debug("current level is $level")
    
    if (level.toInteger() > warnlevel){
       log.debug("level above warnlevel and warned = $state.warned")

       //should send a warning!
	   if (state.warned==0) sendPush("water dist is large: $level")
       state.warned = 1 // record that we already warned about this

    }
    else if (level.toInteger() > 1){
       log.debug("level not above warnlevel")
       if (level.toInteger() < (warnlevel-50)){
           //should send a warning!
	   	   if (state.warned==1) sendPush("water dist is good now: $level")
           state.warned = 0 // record that we already warned about this
       }
    }
    
}
