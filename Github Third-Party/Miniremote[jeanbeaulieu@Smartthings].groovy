/**
 *  Minimote Dimmer Control
 *
 *  Copyright 2015 Jean Beaulieu
 *
 */
definition(
    name: "Minimote Dimmer Control",
    namespace: "jeanbeaulieu",
    author: "Jean Beaulieu",
    description: "Control 2 dimmer with one minimote",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light11-icn@2x.png")
 
preferences {
    section("Select Dimmer for left buttons") {
        input "LeftDimmer", "capability.switchLevel"
    }
    section("Select Dimmer for right buttons") {
        input "RightDimmer", "capability.switchLevel"
    }
    section("Select Minimote") {
        input "remote", "capability.button"
    }

    section("Initial level when light is turned on (0..100)") {
        input "OnLevel", "number", title: "Level"
    }
 
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(remote, "button", buttonHandler)
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(remote, "button", buttonHandler)
}

def buttonHandler(evt) {
    def pressedButtonNumber = evt.jsonData.buttonNumber as Integer
    def RightLevel = RightDimmer.currentValue("level")
    def LeftLevel = LeftDimmer.currentValue("level")
    def RightOn = RightDimmer.currentValue("switch")
    def LeftOn = LeftDimmer.currentValue("switch")  
    def NewLevel = 0
    
	log.debug "button number :  $pressedButtonNumber" 
    log.debug "niveau : $RightLevel  Status : $RightOn"
    log.debug "niveau : $LeftLevel  Status : $LeftOn"
    
    // Turn Off when button held
    if (evt.value == "held") {
    	if ( pressedButtonNumber == 1 || pressedButtonNumber == 3) LeftDimmer.off()
        else RightDimmer.off()
        return
  	}
    
    // Turn on or adjust level when pressed
    
    if ( pressedButtonNumber == 1 ) {
        if ( LeftOn == "off" ) {
        	LeftDimmer.setLevel(OnLevel)
            LeftDimmer.on()
            log.debug "was off"
        } else {
  		NewLevel = LeftLevel + 20
        if (NewLevel > 100) NewLevel = 100
        LeftDimmer.setLevel(NewLevel)
        log.debug "New level : $NewLevel"
		}  
    }
    
    if ( pressedButtonNumber == 2 ) {
        if ( RightOn == "off" ) {
        	RightDimmer.setLevel(OnLevel)
            RightDimmer.on()
        } else {
  		NewLevel = RightLevel + 20
        if (NewLevel > 100) NewLevel = 100
        RightDimmer.setLevel(NewLevel)
        log.debug "New level : $NewLevel"
		}  
    }
 
     if ( pressedButtonNumber == 3 ) {
        if ( LeftOn == "off" ) {
        	LeftDimmer.setLevel(OnLevel)
            LeftDimmer.on()
        } else {
  		NewLevel = LeftLevel - 20
        if (NewLevel < 0) NewLevel = 0
        LeftDimmer.setLevel(NewLevel)
        log.debug "New level : $NewLevel"
		}  
    }

    if ( pressedButtonNumber == 4 ) {
        if ( RightOn == "off" ) {
        	RightDimmer.setLevel(OnLevel)
            RightDimmer.on()
        } else {
  		NewLevel = RightLevel - 20
        if (NewLevel < 0) NewLevel = 0
        RightDimmer.setLevel(NewLevel)
        log.debug "New level : $NewLevel"
		}  
    }
}

