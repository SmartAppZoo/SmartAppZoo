/**
 *  Alarm arming switch
 */

definition(
    name: "Alarm Arming Switch",
    namespace: "smartthings",
    author: "Ted Reimer",
    description: "Allow a switch to arm an alarm",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")


preferences {
	section("When this switch is turned on or off...") {
		input name: "theSwitch", title: "Which Switch?", type: "capability.switch", required: true
	}
    section("Arm this alarm...") {
		input "alarmPanel", "capability.alarm", required: true
	}
        
    section("Select Away or Stay") { 
        input "armMode", "enum", title: "Arm Mode", required: false,
      metadata: [
       values: ["Away","Stay"]
      ]
	}
}

def installed() {
	subscribeToEvents()
}

def updated() {
	unsubscribe()
	subscribeToEvents()
    log.debug "Updated!"
}

def subscribeToEvents() {
	subscribe( theSwitch, "switch", switchHandler, [ filterEvents : false ] )
}

def switchHandler(evt) {
	log.debug "evt.value = ${evt.value}"
    if( evt.value == "on" ) {
       if( armMode == "Away" )
          alarmPanel.armAway()
       else
          alarmPanel.armStay()
    }
//    else if( evt.value == "off" ){
//	   alarmPanel.off()
//    }
}