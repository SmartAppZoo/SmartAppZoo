/**
* Dlink SmartApp take picture
*
* Author: SeanStozki@me.com
* Date: 1-10-2014
*
*/


preferences {
        section("Pick your motion sensor.") {
                input "motion", "capability.motionSensor", title: "Motion Sensor", multiple: true;
 }       
        section("Pick your Door.") {
                input "contact1", "capability.contactSensor", title: "Contact Sensor", multiple: true;
                }
    section("Pick your Camera.") {
            input "Camera", "capability.imageCapture", title: "Camera", multiple: true;
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
        subscribe(motion, "motion.active", motionActive)
		subscribe(contact1, "contact.open", contactOpenHandler)
        
}

def motionActive(evt){
        capture()
}

def contactOpenHandler(evt){
        capture()
}


def capture(){
        Camera.take()
 sendPush("${Camera.label} just took a picture.")

}
