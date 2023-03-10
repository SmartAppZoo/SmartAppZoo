/**
 *  houseFanController V1.0
 *
 *  Author: Mike Maxwell
 
 */
definition(
    name		: "houseFanController",
    namespace	: "MikeMaxwell",
    author		: "Mike Maxwell",
    description	: "Runs whole house fan.",
    category	: "Convenience",
    iconUrl		: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url	: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan@2x.png"
)

preferences {
        section ("Fan related...") {
            input(
            	name		: "fan"
                ,title		: "aeon Fan switch"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.switch"
            )
            input(
                name		: "vsFan"
                ,title		: "virtual Fan Switch"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.switch"
            )
		}
        section ("Control related...") {
        	input(
          		name		: "contacts"
            	,title		: "Select contact sensor(s) for fan control..."
            	,multiple	: true
            	,required	: true
            	,type		: "capability.contactSensor"
        	)
            input(
            	name		: "thermostat"
                ,title		: "Select house thermostat"
                ,multiple	: false
                ,required	: true
                ,type		: "capability.thermostat"
            )
            input(
          		name		: "externalTemp"
            	,title		: "Select external reference temperature sensor"
            	,multiple	: false
            	,required	: true
            	,type		: "capability.temperatureMeasurement"
        	)
            input(
          		name		: "internalTemp"
            	,title		: "Select internal temperature sensor(s)"
            	,multiple	: true
            	,required	: true
            	,type		: "capability.temperatureMeasurement"
        	)
            
            
        }
       	section ("Fan set points...") {
        	input(
                name		: "fanLowTemp" 
                ,title		: "Fan low speed setpoint degrees."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["68","69","70","71","72","73","74","75"]
            )
        	input(
                name		: "fanHighTemp" 
                ,title		: "Fan high speed setpoint degrees"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["73","74","75","76","77","78","79","80"]
            )
            input(
                name		: "fanEnableOffset" 
                ,title		: "Internal/external enable offset degrees"
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: ["0","1","2","3","4","5","6","7","8","9","10"]
            )
            
        }
}



def installed() {
	init()
}

def updated() {
	unsubscribe()
    init()
}

def init() {
    //subscribe(vsFan,"threeAxis",loadHandler)
    subscribe(contacts,"contact",contactHandler)
    subscribe(internalTemp,"temperature",internalTempHandler)
    subscribe(vsFan,"switch",vsHandler)
    subscribe (thermostat,"thermostatMode",statHandler)
    subscribe(app, main)
    state.tempEnable = false
    state.statEnable = false
    state.manOn = false
	state.speedHigh = false
    state.auto = false
}
def main(evt) {
	//state.auto = true
	def stats = [:]
    def set = ["low":settings.fanLowTemp.toInteger(),"high":settings.fanHighTemp.toInteger(),"delta":settings.fanEnableOffset.toInteger()]
    def stat = [:]
    //loadSensor
    //stat = ["loadSensor":loadSensor.currentValue("threeAxis")]
    //stats << stat
    //externalTemp
    def eTemp = externalTemp.currentValue("temperature").toInteger()
    stat = ["externalTemp":eTemp]
    stats << stat
    
    //internalTemps
    stat = ["internalTemps":internalTemp.currentValue("temperature")]
    stats << stat
    
    //temp enable section
    def avgT = internalTemp.currentValue("temperature").sum() / internalTemp.currentValue("temperature").size()
    stat = ["avg":avgT.toInteger()]
    stats << stat
    if (eTemp + set.delta <= avgT) {
		if (avgT > set.high) {
    		//set fan to high
        	stat = ["tempAction":"Temp met (High)"]
            state.speedHigh = true
            state.tempEnable = true
    	} else if (avgT >= set.low) {
    		//set fan to low
        	stat = ["tempAction":"Temp met (Low)"]
            state.speedHigh = false
            state.tempEnable = true
    	} else {
    		//turn fan off
        	stat = ["tempAction":"None (set point is met)"]
            state.tempEnable = false
    	}
    } else {
    	stat = ["tempAction":"None (failed delta check)"]
        state.tempEnable = false
    }
    stats << stat
    stat = ["tempEnable":"${state.tempEnable}"]
    stats << stat
    
    //contact enable section
    if (contacts.currentValue("contact").contains("open")) {
        state.contactEnable = true
    } else {
    	state.contactEnable = false
    }
    //log.info "contacts: ${contacts.currentValue("contact")}"
	stat = ["contactEnable":"${state.contactEnable}"]    
    stats << stat
    
    //house thermostat section
    /*
	thermostatMode 				String "auto" "emergency heat" "heat" "off" "cool" 
	thermostatFanMode 			String "auto" "on" "circulate" 
	thermostatOperatingState 	String "heating" "idle" "pending cool" "vent economizer" "cooling" "pending heat" "fan only" 
    */
    if (thermostat.currentValue("thermostatMode") != "heat") {
    	state.statEnable = true
    } else {
    	state.statEnable = false
    }
	stat = ["statEnable":"${state.statEnable}"]    
    stats << stat
    
    stat = ["statMode":"${thermostat.currentValue("thermostatMode")}"]
    stats << stat
	stat = ["fanMode":"${thermostat.currentValue("thermostatFanMode")}"]
    stats << stat
    stat = ["statState":"${thermostat.currentValue("thermostatOperatingState")}"]
    stats << stat

    
    //fan control
    def fanIsOff = fan.currentValue("switch") == "off"
    if (!state.manOn) {
    	if (state.contactEnable && state.tempEnable && fanIsOff && state.statEnable) {
    		if (state.speedHigh) {
    	    	stat = ["Auto fanAction":"turn fan on High"]
                state.auto = true
				fan.onHigh()
    	        vsFan.onHigh()
    	    } else {
    	    	stat = ["Auto fanAction":"turn fan on Low"]
                state.auto = true
    	    	fan.on()
    	        vsFan.onLow()
    	    }
    	} else if ((!state.contactEnable || !state.tempEnable || !state.statEnable) && !fanIsOff){
    		stat = ["Auto fanAction":"turn fan Off"]
            state.auto = false
    		fan.off()  
    	    vsFan.off()
    	} else {
    		stat = ["Auto fanAction":"fan is:${fan.currentValue("switch")}"]
    	}
    } else if (state.contactEnable && state.statEnable) {
    	stat = ["Manual fanAction":"fan is:${vsFan.currentValue("switch")}"]
		if (vsFan.currentValue("switch") != fan.currentValue("switch")) {
        	if (vsFan.currentValue("switch") == "onLow") {
            	fan.on()
            } else if (vsFan.currentValue("switch") == "onHigh") {
            	fan.onHigh()
            } else {
            	fan.off()
            }
        }
        
    } else {
    	//turn it off
        stat = ["Manual fanAction":"fan is:off"]
        fan.off()
        vsFan.off()
        state.manOn = false
    }
    stats << stat
       
    log.info "set:${set}"
	log.info "stats:${stats}"
}
def vsHandler(evt){
	log.info "vsHandler- name:${evt.displayName} value:${evt.value} state.auto:${state.auto}"
	//state.auto for both on conditions on auto
    log.info "vsHandler: isDigital ${evt.isDigital()}"
    if (!state.auto && evt.value != "off") {
    	state.manOn = true	
    }
}
def statHandler(evt) {
	main(evt)
}

def contactHandler(evt){
	//log.info "contactHandler- name:${evt.displayName} value:${evt.value}"
    main(evt)
}

def internalTempHandler(evt){
	//log.info "internalTempHandler- name:${evt.displayName} value:${evt.value}"
    main(evt)
}

