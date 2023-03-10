/**
 */
definition(
    name: "Arduino Relay Controller",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "Control 8 relays with smartthings",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

 
preferences {
    page(name:"controllerSetup")
   	page(name:"relaySetup")
}


def controllerSetup() {
	dynamicPage(name: "controllerSetup",nextPage: "relaySetup", title: "Controller Setup", uninstall:true) {
        section("Which Arduino shield?") {
            input "arduino", title: "Shield","capability.switch"
        }    
        section("Relays") {
            input "relayCount", title: "How many relays?","number"
        }    
    }
}

def relaySetup() {
   	dynamicPage(name: "relaySetup", title: "Relay Setup", install:true) {
    	for (int i=0;i<settings.relayCount;i++) {
        	section("Relay " + (i+1)) {
                input "relay" + i, title: "Name", "string", description:"Relay " + (i+1), required: false
//                input "typezone" + i, "enum", title: "Type", options:["Open/Closed Sensor","Motion Detector"], required: false
            }
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}


def initialize() {
    // Listen to anything which happens on the device
    subscribe(arduino, "response", statusUpdate)
    
    for (int i=0;i<settings.relayCount;i++) {
    	
    	def dni = "${app.id}:relay${i}"
		def value = settings["relay${i}"]

        log.debug "checking device: ${dni}, value: $value"

        def existingDevice = getChildDevice(dni)
        if (!existingDevice) {
            log.debug "creating device: ${dni}"
            def childDevice = addChildDevice("r3dey3", "Child Switch", dni, null, [
            	name: value, 
                label: value, 
                completedSetup: true,
                "data": [
					"idx": i
                ]
                ])
        }
        else {
            //log.debug existingDevice.deviceType
            //existingDevice.type = zoneType
            existingDevice.label = value
            //existingDevice.name = value
            existingDevice.take()
            existingDevice.updateDataValue("idx", "$i")
            //existingDevice.updateDataValue("Asdasd", "asdasd")
            log.debug "device already exists: ${existingDevice}"
        }
    }
    
    
    def delete = getChildDevices().findAll { settings[it.device.getDataValue("name")] }

    delete.each {
        log.debug "deleting child device: ${it.deviceNetworkId}"
        deleteChildDevice(it.deviceNetworkId)
    }
    refresh()
}

def uninstalled() {
    //removeChildDevices(getChildDevices())
}
def on(child) {
	def idx = child.getDataValue('idx').toInteger()
    arduino.send("R${idx}1Q")
    runIn(20, refresh);
}
def off(child) {
	def idx = child.getDataValue('idx').toInteger()
    arduino.send("R${idx}0Q")
    runIn(20, refresh);
}

def refresh() {
	arduino.send("Q")
}

def statusUpdate(evt)
{
	log.debug "${evt.description}"
    log.debug "statusUpdate ${evt.value} "
	
    def val = evt.value
	def idx = 0
    
    if (evt.value == null) return
    
    while (idx < evt.value.length()) {
    	if (val[idx] == 'R') {
        	8.times { i ->
            	idx++;
            	def child = getChildDevice("${app.id}:relay${i}")
                def curVal = val[idx].toInteger()
                if (child) {
	                def strVal = curVal?"on":"off"
                	log.debug "Sending $child ${strVal}"
                	child.sendEvent(name:"switch", value: strVal)
                }
            }
        }
        else {
        	break;
        }
        idx++
    }
}
