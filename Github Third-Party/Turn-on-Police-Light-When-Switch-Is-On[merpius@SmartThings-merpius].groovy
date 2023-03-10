/**
 *  Turn on Police Light When Switch Is On
 *
 *  Author: Todd Wackford
 */
definition(
    name: 			"Turn on Police Light When Switch Is On",
    namespace: 		"smartthings",
    author: 		"twack",
    description: 	"Turn Fibaro Controller to Police lights program when a switch, real or virtual, is turned on.",
    category:		"My Apps",
    iconUrl: 		"https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: 		"https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)
preferences {
	section("When a Switch is turned on..."){
		input "switchMaster", "capability.switch", title: "Which?"
	}
	section("Turn on this/these Fibaro Police Light(s)..."){
		input "fibaros", "capability.color control", multiple: true
	}
}
def installed() {
	subscribe(switchMaster, "switch.on", switchOnHandler)
    subscribe(switchMaster, "switch.off", switchOffHandler)
}
def updated() {
	unsubscribe()
	subscribe(switchMaster, "switch.on", switchOnHandler)
    subscribe(switchMaster, "switch.off", switchOffHandler)
}
def switchOnHandler(evt) {
	log.trace "Turning on Fibaro RGBW: $fibaros"
	fibaros.police()
}
def switchOffHandler(evt) {
	log.trace "Turning off Fibaro RGBW: $fibaros"
	fibaros.off()
}
