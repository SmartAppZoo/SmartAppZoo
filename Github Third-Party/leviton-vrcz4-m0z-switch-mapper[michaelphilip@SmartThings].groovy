/**
 *	Leviton VRCZ4-M0Z Switch Mapper
 *
 *	Author: Michael Philip Kaufman
 *	Date: 2018-06-02
 */

definition(
    name: "Leviton VRCZ4-M0Z Switch Mapper",
    namespace: "mpk",
    author: "Michael Philip Kaufman",
    description: "Map Leviton VRCZ4-M0Z Zone Controller buttons to any SmartThings-compatible loads",
    category: "Convenience",
    iconUrl: 'http://schemalive.com/vrcz4-m0z.png',
    iconX2Url: 'http://schemalive.com/vrcz4-m0z.png'
)

preferences {
	page(name: "selectButtons")
}

def selectButtons() {
	dynamicPage(name: "selectButtons", title: "Leviton VRCZ4-M0Z Switch Mapper", uninstall: true, install:true) {
		section {
			input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
		}
        (1..4).each {
            def buttonNum = it
            section(title: "Button ${buttonNum} will control:") {
                input "switches_${buttonNum}", "capability.switch", title: "Switches:", multiple: true, required: false
            }
            section(title: "Button ${buttonNum} LED Status Color (when on):") {
                input "color_${buttonNum}", "enum", title: "Choose LED Color:", required: false, default: 1, multiple: false, options: [1: 'Green', 17: 'Orange']
            }
        }
       section {
            label(title: "Label this SmartApp", required: false, defaultValue: "Leviton VRCZ4-M0Z Switch Mapper")
        }  
    }
}

def installed() {
	initialize()
    atomicState.installed = true
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	atomicState.dimmingNow = false
	subscribe(buttonDevice, "button", buttonEvent)
    // subscribe(buttonDevice, "level", levelEvent)
    (1..4).each {
    	def devices = settings['switches_'+it]
        // log.debug "Devices for Button ${it}: ${devices}"
        if (devices) {
        	// log.debug "${devices.deviceNetworkId.join(',')}"
        	buttonDevice.setButton(it, devices.deviceNetworkId.join(','))
    		subscribe(devices, 'switch', updateLights)
        }
    }
    buttonDevice.configure()
}

def configured() {
	return buttonDevice
}

def buttonEvent(evt){
	// log.debug "buttonEvent evt: ${evt}"
    def data = parseJson(evt.data)
    // log.debug "buttonEvent data: ${data} -- data.button: ${data.button} -- data.buttonNumber: ${data.buttonNumber}"
    data.button = (data.button != null) ? data.button : data.buttonNumber
    // log.debug "buttonEvent data: ${data} -- data.button: ${data.button} -- data.buttonNumber: ${data.buttonNumber}"
    
    switch (data.button) {
    	case "0":
            atomicState.dimmingNow = (data.status == "start") ? true : false
            // log.debug "atomicState.dimmingNow set to ${atomicState.dimmingNow}"
        	if (atomicState.dimmingNow) {
                startDimming(data.switch, data.direction)
            }
            break
    	case "1":
        	// log.debug "Turning ${data.status} switches for button 1"
        	(data.status == "on") ? switches_1.on() : switches_1.off()
        	break        
    	case "2":
        	// log.debug "Turning ${data.status} switches for button 2"
        	(data.status == "on") ? switches_2.on() : switches_2.off()
        	break        
    	case "3":
        	// log.debug "Turning ${data.status} switches for button 3"
        	(data.status == "on") ? switches_3.on() : switches_3.off()
        	break        
    	case "4":
        	// log.debug "Turning ${data.status} switches for button 4"
        	(data.status == "on") ? switches_4.on() : switches_4.off()
        	break
        default:
            // log.debug "Falling through to default case for buttonEvent() on event ${evt} with evt.data ${evt.data} parsed to ${data} with status ${data.status} for button ${data.button}"
        	break
    }
   
    /*
    (1..4).each {
      settings['switches_'+it].each {
      	if(it.hasCommand('poll')) {
          it.poll()
        } else if (it.hasCommand('ping')) {
          it.ping()
        }
      }
      //settings['switches_'+it]*.poll()
    }
     */
	return;
}

def startDimming(buttonNumber, direction) {
    // log.debug "Starting dimming ${direction} for button #${buttonNumber}"
    def increment = (direction == "up") ? 2 : -2
    def buttonDevices = switches_1
    def startLevels = switches_1*.currentValue("level")
    switch (buttonNumber) {
    	case "2":
        	buttonDevices = switches_2
            startLevels = switches_2.currentValue("level")
            break
    	case "3":
        	buttonDevices = switches_3
            startLevels = switches_3.currentValue("level")
            break
    	case "4":
        	buttonDevices = switches_4
            startLevels = switches_4.currentValue("level")
            break
    }
	doDimming(buttonDevices, startLevels, increment)
}

def doDimming(buttonDevices, startLevels, increment) {
    // log.debug "Dimming ${buttonDevices} from ${workingLevels} by ${increment}"
    for (;;) {
    	def newLevels = []
        buttonDevices.eachWithIndex {device, i -> 
        	def workingLevel = startLevels[i]
            // log.debug "Current level for ${device} is ${workingLevel}"
            workingLevel += increment
            // log.debug "New level for ${device} is ${workingLevel}"
            workingLevel = (workingLevel < 100) ? workingLevel : 100
            workingLevel = (workingLevel > 0) ? workingLevel : 0
            // log.debug "Final level for ${device} is ${workingLevel}"
            device.setLevel(workingLevel)
            newLevels << workingLevel
        }
        startLevels = newLevels
        (1..1000).each {}
        if (!atomicState.dimmingNow)
        	break
    }
}

def updateLights(evt)
{
	log.debug "updateLights evt = ${evt}"
    log.debug "Button 1: ${switches_1*.currentValue('switch')}"
    log.debug "Button 2: ${switches_2*.currentValue('switch')}"
    log.debug "Button 3: ${switches_3*.currentValue('switch')}"
    log.debug "Button 4: ${switches_4*.currentValue('switch')}"
	def one = switches_1*.currentValue('switch').contains('on')
	def two = switches_2*.currentValue('switch').contains('on')
	def three = switches_3*.currentValue('switch').contains('on')
	def four = switches_4*.currentValue('switch').contains('on')
  	buttonDevice.setLightStatus((one ? color_1 : 0),(two ? color_2 : 0),(three ? color_3 : 0) , (four ? color_4 : 0))
}