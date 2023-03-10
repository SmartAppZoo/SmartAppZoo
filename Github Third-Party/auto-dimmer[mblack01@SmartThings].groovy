/**
 *  Auto Dimmer V1.7
 *
 * 
 */
definition(
    name		: "Auto Dimmer",
    namespace	: "mmaxwell",
    author		: "Mike Maxwell",
    description	: "Automatically adjusts dimmer levels when dimmer(s) are turned on, levels are set based on lux sensor readings.",
    category	: "Convenience",
    iconUrl		: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance.png",
    iconX2Url	: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet-luminance@2x.png"
)

preferences {
    page(name: "page1", title: "AutoDim Configuration", nextPage: "page2", uninstall: true) {
        section {
            input(
            	name		: "luxOmatic"
                ,title		: "Use this lux Sensor..."
                ,multiple	: false
                ,required	: true
                ,type		: "capability.illuminanceMeasurement"
            )
            input(
                name		: "dimDark"
                ,title		: "Select default dim level to use when it's dark out..."
                ,multiple	: false
                ,required	: true
                ,type:       "number"
                
            )
            input(
            	name		: "luxDark"
                ,title		: "Select maximum lux level to be considered as Dark..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
             input(
                name		: "dimDusk"
                ,title		: "Select default dim level to use during dusk/dawn..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
            input(
            	name		: "luxDusk"
                ,title		: "Select maximum lux level to be considered as dusk/dawn..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
            input(
                name		: "dimDay" 
                ,title		: "Select default dim level to use during an overcast day..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
            input(
            	name		: "luxBright"
                ,title		: "Select maximum lux level to be considered as overcast..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
			input(
                name		: "dimBright" 
                ,title		: "Select default dim level to use when it's sunny outside..."
                ,multiple	: false
                ,required	: true
                ,type		: "number"
            )
			input(
            	name		: "dimmers"
                ,title		: "Manage these Dimmers..."
                ,multiple	: true
                ,required	: true
                ,type		: "capability.switchLevel"
            )
            input(
            	name		: "modes"
                ,type		: "mode"
                ,title		: "Set for specific mode(s)"
                ,multiple	: true
                ,required	: false
            )
        }
    }

    page(name: "page2", title: "Set individual dimmer levels to override the default settings.", install: true, uninstall: false)

}

def page2() {
    return dynamicPage(name: "page2") {
    	//loop through selected dimmers
        dimmers.each() { dimmer ->
        	def safeName = dimmer.displayName.replaceAll(/\W/,"")
            section ([hideable: true, hidden: true], "${dimmer.displayName} overrides...") {
                input(
                    name					: safeName + "_dark"
                    ,title					: "Dark level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "number"
                    ,refreshAfterSelection	:true
                )
                input(
                    name					: safeName + "_dusk" 
                    ,title					: "Dusk/Dawn level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "number"
                    ,refreshAfterSelection	:true
                )
                input(
                    name					: safeName + "_day" 
                    ,title					: "Day level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "number"
                    ,refreshAfterSelection	:true
                )
                input(
                    name					: safeName + "_bright" 
                    ,title					: "Bright level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "number"
                    ,refreshAfterSelection	:true
                )

			}
    	}
    }
}

def installed() {
   init()
}

def updated() {
	unsubscribe()
    init()
}
def init(){
   subscribe(dimmers, "switch.on", dimHandler)
}

def dimHandler(evt) {
	if (modeIsOK()) {
    	def newLevel = 0
    
		//get the dimmer that's been turned on
		def dimmer = dimmers.find{it.id == evt.deviceId}
    
    	//get its current dim level
    	def crntDimmerLevel = dimmer.currentValue("level").toInteger()
    
    	//get currentLux reading
    	def crntLux = luxOmatic.currentValue("illuminance").toInteger()
    	def prefVar = dimmer.displayName.replaceAll(/\W/,"")
    	def dimVar
    	if (crntLux < luxDark.toInteger()) {
    		//log.debug "mode:dark"
        	prefVar = prefVar + "_dark"
        	dimVar = dimDark
    	} else if (crntLux < luxDusk.toInteger()) {
    		//log.debug "mode:dusk"
            prefVar = prefVar + "_dusk"
            dimVar = dimDusk
  		} else if (crntLux < luxBright.toInteger()) {
    		//log.debug "mode:day"
            prefVar = prefVar + "_day"
            dimVar = dimDay
    	} else {
    		//log.debug "mode:bright"
    		prefVar = prefVar + "_bright"
        	dimVar = dimBright
    	}
   
    	if (!this."${prefVar}") log.debug "Auto Dimmer is using defaults..."
    	else log.debug "Auto Dimmer is using overrides..."
     
    	def newDimmerLevel = (this."${prefVar}" ?: dimVar).toInteger()
		if (newDimmerLevel == 100) newDimmerLevel = 99
    
    	log.debug "dimmer:${dimmer.displayName}, currentLevel:${crntDimmerLevel}%, requestedValue:${newDimmerLevel}%, currentLux:${crntLux}"
  
    	if ( newDimmerLevel != crntDimmerLevel ) dimmer.setLevel(newDimmerLevel)
    
	} else {
    	log.info 'skipping, current mode is not selected.'
    }
}
def modeIsOK() {
	def result = !modes || modes.contains(location.mode)
	return result
}