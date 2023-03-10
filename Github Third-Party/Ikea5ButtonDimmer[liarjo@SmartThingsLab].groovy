definition(
    name: "IKEA 5 Button Dimmer",
    namespace: "jpgg",
    author: "Juan Pablo",
    description: "Dimmer app for light group control by Ikea 5 buttons control",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section ("Controls configurations"){
    	input "theswitch", "capability.button",  title: "Ikea 5 Buttons control", required: true
        input "Lights","capability.switchLevel",title: "Lights to manage", required: true, multiple: true
        input "step", "number", title: "Dimmer delta", required: true , description: "dimmer value to change" 
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	log.debug "initialize"
    subscribe(theswitch,"button", switchHandler)
}

def switchHandler (evt){
	def theDimmer=Lights[0]
    def previusLevel=theDimmer.currentLevel
    def currentLevel=theDimmer.currentLevel

    //IS light one?
    if (theDimmer.currentSwitch.contains('on')){
    	switch (evt.jsonData.buttonNumber) {
        case "1":
           //UP
           currentLevel= currentLevel + step
           if (currentLevel >100) {
           		currentLevel=100
           }
           Lights.setLevel(currentLevel)
           break
        case "3":
            //down
            currentLevel= currentLevel - step
            if (currentLevel<0) {
           		currentLevel=0
           }
           Lights.setLevel(currentLevel)
           break
        case "5":
			Lights.off()
        	break
        default:
            log.debug "unknown {$evt.jsonData.buttonNumber}"
        }
        
    } else {
    	//OFF
        switch (evt.jsonData.buttonNumber) {
        case "1":
           //UP
            break
        case "3":
            //down
            break
        case "5":
			Lights.on()
        	break
        default:
            log.debug "unknown {$evt.jsonData.buttonNumber}"
        }
    }
    log.debug "Devices $Lights | previus level $previusLevel | new level $currentLevel"

}
