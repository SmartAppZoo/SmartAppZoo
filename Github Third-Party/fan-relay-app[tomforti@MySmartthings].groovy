/*
App is middle man to be used by the vitural fan switch and the arduino fan relay. It takes the setleves from the vitural
switch and based on the level triggers the arduino fans on or off.
To set up you will need to go into preferences and set the vitural fans to the app as well as the arduino shield.
Look at the arduino code to match the fans to the correct pin config.
If all code is kept the same
fan1 has all pins off
fan2 has pin 1 on
fan3 has pin 2 on
fan4 has pin 3 on
*/
definition(
    name: "Fan Relay App",
    namespace: "Fan Control",
    author: "Tom Forti",
    description: "Arduino Relay",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

//set the fans and arduino to the app
preferences {
	section("Connect these virtual Fans to the Arduino") {
		input "fan1", title: "Fan 1 for arduino", "capability.switchLevel"
        input "fan2", title: "Fan 2 for arduino", "capability.switchLevel", required: false
        input "fan3", title: "Fan 3 for arduino", "capability.switchLevel", required: false
        input "fan4", title: "Fan 4 for arduino", "capability.switchLevel", required: false 
	}
    section("Which Arduino relay board to control?") {
		input "arduino", "device.ArduinoFans"
    }    
}

def installed() {
	subscribe(fan1, "switch.setLevel", fan1val)
    subscribe(fan1, "switch", fan1val)
    subscribe(fan2, "switch.setLevel", fan2val)
    subscribe(fan2, "switch", fan2val)
    subscribe(fan3, "switch.setLevel", fan3val)
    subscribe(fan3, "switch", fan3val)
    subscribe(fan4, "switch.setLevel", fan4val)
    subscribe(fan4, "switch", fan4val)
}

//checks for changes from the vitural fan switch
def updated() {
	unsubscribe()
	subscribe(fan1, "switch.setLevel", fan1val)
    subscribe(fan1, "switch", fan1val)
    subscribe(fan2, "switch.setLevel", fan2val)
    subscribe(fan2, "switch", fan2val)
    subscribe(fan3, "switch.setLevel", fan3val)
    subscribe(fan3, "switch", fan3val)
    subscribe(fan4, "switch.setLevel", fan4val)
    subscribe(fan4, "switch", fan4val)
    log.info "subscribed to all of Fan events"
}

//takes value from vitrual device and uses it to trigger fan
def fan1val(evt)
{
	if ((evt.value == "on") || (evt.value == "off" ))
		return //if value is on or off than it stops
	log.debug "UpdateLevel: $evt"
	int level = fan1.currentValue("level")
	log.debug "level: $level"	
    
    if (level == 0) {
    	arduino.fan1off() //tell the arduino to turn trigger this arduino switch
    	
    }
     if (level == 30) {
    	arduino.fan1low()
    	
    }
     if (level == 60) {
    	arduino.fan1med()
    	
    }
     if (level == 100) {
    	arduino.fan1hi()
    	
    }
}


def fan2val(evt)
{
	if ((evt.value == "on") || (evt.value == "off" ))
		return
	log.debug "UpdateLevel: $evt"
	int level = fan2.currentValue("level")
	log.debug "level: $level"
    
    if (level == 0) {
    	arduino.fan2off()
    	
    }
     if (level == 30) {
    	arduino.fan2low()
    	
    }
     if (level == 60) {
    	arduino.fan2med()
    	
    }
     if (level == 100) {
    	arduino.fan2hi()
    	
    }
}

def fan3val(evt)
{
	if ((evt.value == "on") || (evt.value == "off" ))
		return
	log.debug "UpdateLevel: $evt"
	int level = fan3.currentValue("level")
	log.debug "level: $level"
    
    if (level == 0) {
    	arduino.fan3off()
    	
    }
     if (level == 30) {
		arduino.fan3low()
    	
    }
     if (level == 60) {
    	arduino.fan3med()
    	
    }
     if (level == 100) {
    	arduino.fan3hi()
    
    }
}


def fan4val(evt)
{
	if ((evt.value == "on") || (evt.value == "off" ))
		return
	log.debug "UpdateLevel: $evt"
	int level = fan4.currentValue("level")
	log.debug "level: $level"

    if (level == 0) {
    	arduino.fan4off()
    }
    
     if (level == 30) {
    	arduino.fan4low()
    }
    
     if (level == 60) {
    	arduino.fan4med()
    }
    
     if (level == 100) {
    	arduino.fan4hi()
    }
}

