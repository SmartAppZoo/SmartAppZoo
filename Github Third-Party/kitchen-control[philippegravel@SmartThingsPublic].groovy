/**
 *	Button Controller App for ZWN-SC7
 *
 *	Author: Philippe Gravel
 *	Date Created: 2016-03-04
 *
 */
definition(
    name: "Kitchen Control",
    namespace: "philippegravel",
    author: "Philippe Gravel",
    description: "Control of all the kitchen",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Food & Dining/dining5-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Food & Dining/dining5-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Food & Dining/dining5-icn@2x.png"
)

preferences {
	page(name: "Controller")
    page(name: "Cuisine")
    page(name: "Other")
}

def Controller() {
  dynamicPage(name: "Controller", title: "First, select which ZWN-SC7", nextPage: "Cuisine", uninstall: true) {
    section ("Set the controller") {
      input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
    }
    section(title: "Other", mobileOnly:true, hideable: false, hidden: false) {
		label title: "Assign a name", required: false
    }
    
	section(title: "Send Notifications?", mobileOnly:true, hideable: true, hidden: true) {
    	input("recipients", "contact", title: "Send notifications to", multiple: true, required: false)
    }
  }
}

def Cuisine() {
  dynamicPage(name: "Cuisine", title: "Cuisine setup", nextPage: "Other", uninstall: true) {
    section {
	  input "table", "capability.switch", title: "Table:", required: true
      input "comptoir", "capability.switch", title: "Comptoir:", required: true
      input "pont", "capability.switch", title: "Pont:", required: true
      input "strip", "capability.switch", title: "Dessous comptoir:", required: true
      input "top", "capability.switch", title: "Planfond couleur:", required: true
      input "dresser", "capability.switch", title: "Vaisselier:", required: true
      input "portelock", "capability.lock", title: "Porte:", required: true
      input "shades", "capability.windowShade", title: "Fenetres:", required: true
    }
  }
}

def Other() {
  dynamicPage(name: "Other", title: "Other setup", install: true, uninstall: true) {
  	section {
      input "outside", "capability.switch", title: "Lumiere avant exterieur:", required: true
      input "living", "capability.switch", title: "Lumiere du salon:", required: true
    }
    section(title: "Virtual Controller") {
    	input "virtualController", "capability.switch", title: "Virtual Controller", required: true
    }    
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

  subscribe(buttonDevice, "button", buttonEvent)

    if (relayDevice) {
        log.debug "Kicthen Control: Associating ${relayDevice.deviceNetworkId}"
        if (relayAssociate == true) {
            buttonDevice.associateLoad(relayDevice.deviceNetworkId)
        }
        else {
            buttonDevice.associateLoad(0)
        }
    }

	subscribe(pont, "switch", pontEvent)
   
    subscribe(virtualController, "level", virtualLevelEvent)
    subscribe(virtualController, "switch", virtualOffEvent)
}

def buttonEvent(evt){
 	log.debug "Kicthen Control: buttonEvent"

	def buttonNumber = evt.jsonData.buttonNumber
    def firstEventId = 0
    def value = evt.value
    log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
 //   log.debug "Kicthen Control: button: $buttonNumber, value: $value"
    def recentEvents = buttonDevice.eventsSince(new Date(now() - 3000)).findAll{it.value == evt.value && it.data == evt.data}
    log.debug "Kicthen Control: Found ${recentEvents.size()?:0} events in past 3 seconds"
    if (recentEvents.size() != 0){
        log.debug "Kicthen Control: First Event ID: ${recentEvents[0].id}"
        firstEventId = recentEvents[0].id
    }
    else {
        firstEventId = 0
    }

    log.debug "Kicthen Control: This Event ID: ${evt.id}"

    if(firstEventId == evt.id){
        switch(buttonNumber) {
            case ~/.*1.*/:
            button1Handlers()
            break
            case ~/.*2.*/:
            button2Handlers()
            break
            case ~/.*3.*/:
            button3Handlers()
            break
            case ~/.*4.*/:
            button4Handlers()
            break
            case ~/.*5.*/:
            button5Handlers()
            break
            case ~/.*6.*/:
            button6Handlers()
            break
            case ~/.*7.*/:
            button7Handlers()
            break
            case ~/.*8.*/:
            button8Handlers()
            break
        }
    } else if (firstEventId == 0) {
        log.debug "Kicthen Control: No events found. Possible SmartThings latency"
    } else {
        log.debug "Kicthen Control: Duplicate button press found. Not executing handlers"
    }    
}

def button1Handlers() {
	log.debug "Kitchen Control: Button 1 Handler"

// On 
    table.setLevel(100)
	comptoir.setLevel(100)
    top.setColor(hex: "#FFFFFF")

    strip.offBlue()
    strip.offRed()
    strip.offGreen()
    strip.setLevelWhite(100)

	atomicState.noPontEvent = true
	pont.setLevel(100)
    
    if (living.currentSwitch == "off") {
    	living.setLevel(15)
	}        
// Off
	dresser.off()
}

def button2Handlers() {
	log.debug "Kitchen Control: Button 2 Handler"

	shades.close()
    top.off()
	location.helloHome.execute("All Off")
    setLocationMode("Night")
}

def button3Handlers() {
	log.debug "Kitchen Control: Button 3 Handler"

// On
	atomicState.noPontEvent = true
    pont.setLevel(40)
    
    table.setLevel(8)
    dresser.setLevel(100)
	comptoir.setLevel(2)
	
    strip.offBlue()
    strip.offRed()
    strip.offGreen()
    strip.setLevelWhite(40)

// Off
	top.off()
}

def button4Handlers() {
	def currentValue = table.currentValue("switch")
	log.debug "Kitchen Control: Button 4 Handler ($currentValue)"
    
	if (table.currentValue("switch") == "off") {
	    table.setLevel(100)
    } else {
    	table.off()
    }
}

def button5Handlers() {
	log.debug "Kitchen Control: Button 5 Handler"

// On
	top.setColor(hex: "#1F2440")

	strip.setColor(hex: "#1F2440")
    strip.offWhite()
    dresser.setLevel(25)

//  Off 
	comptoir.off()
    table.off()
    living.off()
    
    atomicState.noPontEvent = true
    pont.off()
}

def button6Handlers() {
	log.debug "Kitchen Control: Button 6 Handler"
	
	if (outside.currentValue("switch") == "off") {
    	outside.on()
    } else {
        outside.off()
    }
}

def button7Handlers() {
	log.debug "Kitchen Control: Button 7 Handler"

	def lockstatus = portelock.currentValue("lock")
	log.debug "Kitchen Control: Lock Status -> $lockstatus"
    
    if (lockstatus == "locked") {
    	log.debug "Kitchen Control: Try to unlock"
    	portelock.unlock()
	} else {
		log.debug "Kitchen Control: Try to lock"
    	portelock.lock()
    }
}

def button8Handlers() {
	log.debug "Kitchen Control: Virtual Button 8 Handler"

// On
	atomicState.noPontEvent = true
	pont.setLevel(100)
    comptoir.setLevel(100)
    strip.offBlue()
    strip.offRed()
    strip.offGreen()
    strip.setLevelWhite(100)
    
// Off
    table.off()
    top.off()
    dresser.off()
}    

def pontEvent(evt) {
	log.debug "Kicthen Control: Pont - $evt.name: $evt.value"
	
	if (atomicState.noPontEvent) {
    	log.debug "Kicthen Control: Event sent by the app, nothing to do"        
        atomicState.noPontEvent = false
    } else {
       	if (evt.value == "on") {
        	log.debug "Kicthen Control: Turn On strip"
            
            strip.offBlue()
            strip.offRed()
            strip.offGreen()
			strip.setLevelWhite(100)
		} else {
        	log.debug "Kicthen Control: Turn Off strip"
			strip.off()
        }
    }
}

def virtualLevelEvent(evt) {
	log.debug "Kitchen Control: Virtual Controller - $evt.name: $evt.value"
  
   if (atomicState.noVirtualEvent) {
    	log.debug "Kicthen Control: Event sent by the app, nothing to do"        
        atomicState.noVirtualEvent = false     
    } else {
        def level = Integer.parseInt(evt.value)

        if (level == 1 || (level >= 10 && level <= 19)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button1Handlers()
        } else if (level == 4 || (level >= 20 && level <= 29)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button2Handlers()
        } else if (level == 2 || (level >= 30 && level <= 39)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button3Handlers()
        } else if (level == 5 || (level >= 40 && level <= 49)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button4Handlers()
        } else if (level == 3 || (level >= 50 && level <= 59)) {
            button5Handlers()
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
        } else if (level == 6 || (level >= 60 && level <= 69)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button6Handlers()
        } else if (level == 7 || (level >= 70 && level <= 79)) {
            atomicState.noVirtualEvent = true
            virtualController.setLevel(99)
            button7Handlers()
        } else if (level == 92) {
        	atomicState.noVirtualEvent = true
            colorChangeMode(1) // Christmass
            virtualController.setLevel(99)
        } else if (level == 93) {
            colorChangeMode(2) // Frozen
        } else if (level == 94) { 
            colorChangeMode(3) // Spider Man
        } else if (level == 95) { 
            colorChangeMode(4) // Hulk
        } else if (level == 96) { 
            colorChangeMode(5) // Pastel
        }    
	}
}

def virtualOffEvent(evt) {
	log.debug "Kitchen Control: Virtual Controller - $evt.name: $evt.value"

	if (evt.value == "off") {
    
    	if (atomicState.colorChangeMode != 0) {
    		atomicState.colorChangeMode = 0
            strip.off()
            top.off()
        }
        atomicState.noPontEvent = false
    }
}

def colorChangeMode(mode) {

	log.debug "Kitchen Control: Color Change Mode: $mode"

	atomicState.inKitchenEvent = true
    
	atomicState.colorChangeMode = mode
	atomicState.currentColorNumber = -1
    
    if (mode == 5) {
    	strip.onWhite()
    }
    
	colorChangeSwitch()

	atomicState.inKitchenEvent = false    
}

def colorChangeSwitch() {

    log.debug "Color change Switch"

    def mode = atomicState.colorChangeMode

    if (mode != 0) {
        def currentColorTop = getNextColor(mode, true)
        def currentColorStrip = getNextColor(mode, false)

        if (currentColorTop && currentColorStrip) {
            top.setColor(hex: currentColorTop)
            strip.setColor(hex: currentColorStrip)    
            runIn(60, colorChangeSwitch, [overwrite: false])
        } else { 
            log.debug "No color found - stop"
        }
    } else {
        log.debug "Mode unset - stop"
    }
}

def getNextColor(mode, updateCurrentColor) {
	
    log.debug "Mode [$mode]"
    
    def newColor = atomicState.currentColorNumber + 1
  	def colorToReturn 
    
    log.debug "Color Current[$atomicState.currentColorNumber] New[$newColor]"

    if (mode == 1) { // Christmass
        if (newColor > 1) {
            newColor = 0
        }

        switch (newColor) {
            case 0 : colorToReturn = "#ff0000"; break
            case 1 : colorToReturn = "#00ff00"; break
        }
    } else if (mode == 2) { // Frozen 
        if (newColor > 3) {
            newColor = 0
        }
        switch (newColor) {
            case 0 : colorToReturn = "#C6F2FC"; break
            case 1 : colorToReturn = "#A9E4F9"; break
            case 2 : colorToReturn = "#F47DA5"; break
            case 3 : colorToReturn = "#C47FB6"; break
        }
    } else if (mode == 3) { // SpiderMan 
        if (newColor > 3) {
            newColor = 0
        }
        switch (newColor) {
            case 0 : colorToReturn = "#3E5A70"; break
            case 1 : colorToReturn = "#314256"; break
            case 2 : colorToReturn = "#973C3C"; break
            case 3 : colorToReturn = "#713E3E"; break
        }
    } else if (mode == 4) { // Hulk 
        if (newColor > 4) {
            newColor = 0
        }
        switch (newColor) {
            case 0 : colorToReturn = "#8BFA4D"; break
            case 1 : colorToReturn = "#49FF07"; break
            case 2 : colorToReturn = "#38E92E"; break
            case 3 : colorToReturn = "#8A2C9A"; break
        }
    } else if (mode == 5) { // Pastel 
        if (newColor > 4) {
            newColor = 0
        }
        switch (newColor) {
            case 0 : colorToReturn = "#93DFB8"; break
            case 1 : colorToReturn = "#FFC8BA"; break
            case 2 : colorToReturn = "#E3AAD6"; break
            case 3 : colorToReturn = "#B5D8EB"; break
            case 4 : colorToReturn = "#FFBDD8"; break
        }
    }

	if (updateCurrentColor) {
		atomicState.currentColorNumber = newColor
    }
    
    return colorToReturn
}