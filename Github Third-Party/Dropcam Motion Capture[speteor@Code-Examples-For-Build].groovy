/**
 *  Motion Dropcam
 *
 *  Author: zach@beamlee.com
 *  Date: 2013-07-30
 */
preferences {
	section("Pick your motion sensor.") {
		input "motion", "capability.motionSensor", title: "Motion Sensor", multiple: true;
	}
    section("Pick your Dropcam.") {
    	input "dropcam", "capability.imageCapture", title: "Dropcam", multiple: true;
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
}

def motionActive(evt){
	capture()
}

def capture(){
	dropcam.take()
    sendPush("${dropcam.label} just took a picture.")
}
