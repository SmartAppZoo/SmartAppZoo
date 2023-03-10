/**
 *  Automatic HVAC Program
 *
 *  Author: mwoodengr@hotmail.com
 *  Date: 2014-01-28
 */
preferences {
/*
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	section("Change HVAC mode when the outside temperature is...") {
		input "temperature1", "number", title: "Temp Degrees Fahrenheit?"
	}
    */
	section("Choose thermostat... ") {
		input "thermostat", "capability.thermostat"
	}
    	section("Monday thru Friday Schedule") {
		input ("time1", "time", title: "Wake Time of Day")	
		input ("tempSetpoint1", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
		input ("tempSetpointA", "number", title: "Wake Cool Temp Degrees Fahrenheit?")
		input ("time2", "time", title: "Leave Time of Day")
		input ("tempSetpoint2", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
		input ("tempSetpointB", "number", title: "Leave Cool Temp Degrees Fahrenheit?")
		input ("time3", "time", title: "Return Time of Day")
		input ("tempSetpoint3", "number", title: "Return Heat Degrees Fahrenheit?")
		input ("tempSetpointC", "number", title: "Return Cool Degrees Fahrenheit?")
		input ("time4", "time", title: "Sleep Time of Day")
		input ("tempSetpoint4", "number", title: "Sleep Heat Degrees Fahrenheit?")
		input ("tempSetpointD", "number", title: "Sleep Cool Degrees Fahrenheit?")
	}
    	section("Saturday and Sunday Schedule") {
		input ("time11", "time", title: "Wake Time of Day")	
		input ("tempSetpoint11", "number", title: "Wake Heat Temp Degrees Fahrenheit?")
		input ("tempSetpointAA", "number", title: "Wake Cool Temp Degrees Fahrenheit?")
		input ("time21", "time", title: "Leave Time of Day")
		input ("tempSetpoint21", "number", title: "Leave Heat Temp Degrees Fahrenheit?")
		input ("tempSetpointBB", "number", title: "Leave Cool Temp Degrees Fahrenheit?")
		input ("time31", "time", title: "Return Time of Day")
		input ("tempSetpoint31", "number", title: "Return Heat Degrees Fahrenheit?")
		input ("tempSetpointCC", "number", title: "Return Cool Degrees Fahrenheit?")
		input ("time41", "time", title: "Sleep Time of Day")
		input ("tempSetpoint41", "number", title: "Sleep Heat Degrees Fahrenheit?")
		input ("tempSetpointDD", "number", title: "Sleep Cool Degrees Fahrenheit?")
	}
}

def installed()
{
		schedule(time1, "initialize")
    	schedule(time2, "initialize")
    	schedule(time3, "initialize")
    	schedule(time4, "initialize")
		schedule(time11, "initialize")
    	schedule(time21, "initialize")
    	schedule(time31, "initialize")
    	schedule(time41, "initialize")
		//subscribe(temperatureSensor1, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostat", thermostatHandler)
   		subscribe(thermostat, "tempSetpoint1", HeatingSetpoint1Handler)
    	subscribe(thermostat, "tempSetpoint2", HeatingSetpoint2Handler)
		subscribe(thermostat, "tempSetpoint3", HeatingSetpoint3Handler)
		subscribe(thermostat, "tempSetpoint4", HeatingSetpoint4Handler)

		subscribe(thermostat, "tempSetpoint11", HeatingSetpoint11Handler)
    	subscribe(thermostat, "tempSetpoint21", HeatingSetpoint21Handler)
		subscribe(thermostat, "tempSetpoint31", HeatingSetpoint31Handler)
		subscribe(thermostat, "tempSetpoint41", HeatingSetpoint41Handler)
 
  		subscribe(thermostat, "tempSetpointA", CoolingSetpoint1Handler)
    	subscribe(thermostat, "tempSetpointB", CoolingSetpoint2Handler)
		subscribe(thermostat, "tempSetpointC", CoolingSetpoint3Handler)
		subscribe(thermostat, "tempSetpointD", CoolingSetpoint4Handler)

  		subscribe(thermostat, "tempSetpointAA", CoolingSetpointA1Handler)
    	subscribe(thermostat, "tempSetpointBB", CoolingSetpointA2Handler)
		subscribe(thermostat, "tempSetpointCC", CoolingSetpointA3Handler)
		subscribe(thermostat, "tempSetpointDD", CoolingSetpointA4Handler)
        initialize()
}

def updated()
{
		unsubscribe()
    	schedule(time1, "initialize")
    	schedule(time2, "initialize")
    	schedule(time3, "initialize")
    	schedule(time4, "initialize")
    	schedule(time11, "initialize")
    	schedule(time21, "initialize")
    	schedule(time31, "initialize")
    	schedule(time41, "initialize")
		//subscribe(temperatureSensor1, "temperature", temperatureHandler)
		subscribe(thermostat, "thermostat", thermostatHandler)
   		subscribe(thermostat, "tempSetpoint1", HeatingSetpoint1Handler)
    	subscribe(thermostat, "tempSetpoint2", HeatingSetpoint2Handler)
		subscribe(thermostat, "tempSetpoint3", HeatingSetpoint3Handler)
		subscribe(thermostat, "tempSetpoint4", HeatingSetpoint4Handler)

   		subscribe(thermostat, "tempSetpoint11", HeatingSetpoint11Handler)
    	subscribe(thermostat, "tempSetpoint21", HeatingSetpoint21Handler)
		subscribe(thermostat, "tempSetpoint31", HeatingSetpoint31Handler)
		subscribe(thermostat, "tempSetpoint41", HeatingSetpoint41Handler)
 
  		subscribe(thermostat, "tempSetpointA", CoolingSetpoint1Handler)
    	subscribe(thermostat, "tempSetpointB", CoolingSetpoint2Handler)
		subscribe(thermostat, "tempSetpointC", CoolingSetpoint3Handler)
		subscribe(thermostat, "tempSetpointD", CoolingSetpoint4Handler)

  		subscribe(thermostat, "tempSetpointAA", CoolingSetpointA1Handler)
    	subscribe(thermostat, "tempSetpointBB", CoolingSetpointA2Handler)
		subscribe(thermostat, "tempSetpointCC", CoolingSetpointA3Handler)
		subscribe(thermostat, "tempSetpointDD", CoolingSetpointA4Handler)
       	unschedule()
        initialize()
}
// This section sets the HVAC mode based outside temperature. HVAC fan mode is set to "auto".
def temperatureHandler(evt) {
	def lastTemp = temperatureSensor1.currentTemperature
	log.debug "temperature is: $lastTemp"
	if (lastTemp <= temperature1) {
		def thermostatState = thermostat.currentthermostatMode
        def thermostatFan = thermostat.currentthermostatFanMode
        	if (thermostatState == "cool"){
        		def hvacmode = "heat"
        		log.debug "HVAC mode set to $hvacmode"
                log.debug "HVAC Fan mode set to $thermostatFan"
            	thermostat.setThermostatMode(hvacmode)
                	if (thermostatFan != "auto"){
                    thermostat.setThermostatFanMode("auto")
                    log.debug "HVAC fan mode set to auto"
        			}
            }
            	else if (thermostatFan != "auto"){
        			thermostat.setThermostatFanMode("auto")
                    log.debug "HVAC fan mode set to auto"
				}
	else {          
            	if (thermostatState == "heat"){
            		def hvacmode = "cool"
        			log.debug "HVAC mode set to $hvacmode"
					thermostat.setThermostatMode(hvacmode)
	        	}
            		if (thermostatFan != "auto"){
                		thermostat.setThermostatFanMode("auto")
                    	log.debug "HVAC fan mode set to auto"
					}
         }
    }
}

// This section determines which day it is.
def initialize() {
	
	def calendar = Calendar.getInstance()
	calendar.setTimeZone(TimeZone.getTimeZone("GMT-5"))
	def today = calendar.get(Calendar.DAY_OF_WEEK)
	log.debug("today=${today}")

	def todayValid = null
	switch (today) {
		case Calendar.MONDAY:
			todayValid = days.find{it.equals("Monday")}
            today = "Monday"
			log.debug("today is Monday")
			break
		case Calendar.TUESDAY:
			todayValid = days.find{it.equals("Tuesday")}
            today = "Tuesday"
			log.debug("today is Tuesday")
			break
		case Calendar.WEDNESDAY:
			todayValid = days.find{it.equals("Wednesday")}
			log.debug("today is Wednesday")
            today = "Wednesday"
			break
		case Calendar.THURSDAY:
			todayValid = days.find{it.equals("Thursday")}
			today = "Thursday"
            log.debug("today is Thursday")
			break
		case Calendar.FRIDAY:
			todayValid = days.find{it.equals("Friday")}
			today = "Friday"
            log.debug("today is Friday")
			break
		case Calendar.SATURDAY:
			todayValid = days.find{it.equals("Saturday")}
			log.debug("today is Saturday")
			today = "Saturday"
            break
		case Calendar.SUNDAY:
			todayValid = days.find{it.equals("Sunday")}
			log.debug("today is Sunday")
			today = "Sunday"
            break
	}
    
log.debug("The day is $today")

// This section is where the time/temperature shcedule is set.
if (today == "Monday") {
	   	schedule(time1, changetemp1)
		schedule(time2, changetemp2)
		schedule(time3, changetemp3)
		schedule(time4, changetemp4)
		}
if (today =="Tuesday") {
    	schedule(time1, changetemp1)
		schedule(time2, changetemp2)
		schedule(time3, changetemp3)
		schedule(time4, changetemp4)
		}
if (today =="Wednesday") {
    	schedule(time1, changetemp1)
		schedule(time2, changetemp2)
		schedule(time3, changetemp3)
		schedule(time4, changetemp4)
		}
if (today =="Thrusday") {
    	schedule(time1, changetemp1)
		schedule(time2, changetemp2)
		schedule(time3, changetemp3)
		schedule(time4, changetemp4)
		}
if (today =="Friday") {
    	schedule(time1, changetemp1)
		schedule(time2, changetemp2)
		schedule(time3, changetemp3)
		schedule(time4, changetemp4)
		}
if (today =="Saturday") {
   	 	schedule(time11, changetemp11)
		schedule(time21, changetemp21)
		schedule(time31, changetemp31)
		schedule(time41, changetemp41)
		}
if (today =="Sunday") {
    	schedule(time11, changetemp11)
		schedule(time21, changetemp21)
		schedule(time31, changetemp31)
		schedule(time41, changetemp41)
		}
}

// This section is where the thermostat temperature settings are set. 
def changetemp1() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint1)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointA)
	}
}
def changetemp2() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint2)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointB)
	}
}
def changetemp3() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint3)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointC)
	}
}
def changetemp4() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint4)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointD)
	}
}

def changetemp11() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint11)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointAA)
	}
}
def changetemp21() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint21)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointBB)
	}
}
def changetemp31() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint31)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointCC)
	}
}
def changetemp41() {
	def thermostatState = thermostat.currentthermostatMode
	log.debug "checking mode request = $thermostatState"
	if (thermostatState == "heat"){
	thermostat.setHeatingSetpoint(tempSetpoint41)
	}
	else {
	thermostat.setCoolingSetpoint(tempSetpointDD)
	}
}