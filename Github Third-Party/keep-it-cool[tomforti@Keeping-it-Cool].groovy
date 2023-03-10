definition(
    name: "Keep-it-cool",
    namespace: "tomforti",
    author: "Tom",
    description: "Run circulate every X minutes if AC or heat has not been on",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")


preferences {
	section("Title") {
		paragraph "Run circulate in HVAC and trigger switch or outlet"
	}
	section("About") {
        	paragraph "Run circulate every X minutes if AC or heat has not been on"
            paragraph "Turn on a switch or outlet every X minutes if AC or heat has not been on"
            paragraph "Additional setpoint can be used to determine minimum run temperature."
          
    }
    	section("Controlled Devices") {
        	input "thermostat", "capability.thermostat", title:"Select thermostat to be controlled"
            input "switches", "capability.switch", title:"Select switch or outlet to be controlled", multiple: true
        	input "interval", "number", title:"Set time between circulation cycles (in minutes)", defaultValue:30
        	input "length", "number", title:"Set of length of circulation cycle (in minutes)", defaultValue:5
		}
        section("Choose a temperature sensor... "){
			input "sensor", "capability.temperatureMeasurement", title: "Temperature Sensor used to establish minimum run temperature"
		}
		section("Operation") {
			input "runTemp", "number", title:"Choose a temperature to set the minimum run temperature for circulate.", defaultValue:70
            input "swTemp", "number", title:"Choose a temperature to set the minimum run temperature for switch or outlet.", defaultValue:70
			input "onoff", "capability.switch", title:"Select switch to control operation.  Typically a virtual switch created in the IDE" 
        }
}

def installed() {
	DEBUG("Installed with settings: ${settings}")
    initialize()
}

def updated() {
	DEBUG("Updated with settings: ${settings}")
	unsubscribe()
	unschedule()
   	initialize()

}

def onHandler(evt) {
	DEBUG(evt.value)
	LOG("Running Switch On Event")
    scheduler()
}

def offHandler(evt) {
	DEBUG(evt.value)
	LOG("Running Switch Off Event")
    unschedule()
    thermostat.fanAuto()
    switches.off()
}

def scheduler(){
	DEBUG ("scheduler()")
	def interval = settings.interval.toInteger() * 60
	def length = settings.length.toInteger() * 60
	DEBUG("Interval in seconds: ${interval}, Length in seconds: ${length}")
    start_circulate()
	runIn(length, stop_circulate)
    runIn(interval+length, scheduler)
}

def stop_circulate(){
	DEBUG("stop_circulate()")
	thermostat.fanAuto()
	switches.off()
    }
    
def start_circulate(){
	DEBUG("start_circulate()")
      	if (sensor.currentValue("temperature") >= runTemp)
			{	DEBUG ("into start_circulate() if statement")
				thermostat.fanOn()
   			}
    
    	if (sensor.currentValue("temperature") < runTemp)
			{	DEBUG ("into start_circulate() if statement")
				thermostat.fanAuto()
   			}
            
    	if (sensor.currentValue("temperature") >= swTemp)
			{	DEBUG ("into start_circulate() if statement")
				switches.on()
   			}
            
       	if (sensor.currentValue("temperature") < swTemp)
			{	DEBUG ("into start_circulate() if statement")
				switches.off()
   			}  
}


def initialize() {
	DEBUG("initialize()")
    subscribe(onoff, "switch.on", onHandler)
    subscribe(onoff, "switch.off", offHandler)
    subscribe(device, "thermostatOperatingState", eventHandler)
    DEBUG ("running_state: ${thermostat.currentValue("thermostatOperatingState")}")
    DEBUG ("On/Off Switch: ${onoff.currentswitch}")
	if(thermostat.currentValue("thermostatOperatingState") == "idle" && onoff.currentSwitch == "on"){
		scheduler()
    }
}
    
// TODO: implement event handlers

def eventHandler(evt){
	DEBUG("eventHandler: ${evt.value}: ${evt}, ${settings}")
	if(evt.value == "idle"){
    	LOG("idle - running scheduler()")
		scheduler()
	}
	if(evt.value == "heating"|| evt.value == "cooling"){
		LOG("not idle - running unschedule()")
    		unschedule()
    		thermostat.fanAuto()
            switches.off()
   }
}

private def LOG(message){
	log.info message
}

private def DEBUG(message){
	//log.debug message
}
