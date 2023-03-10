/**
 *  Author: Baur
 */

 

definition(
    name: "Event Based Chimes",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Play chimes based on events",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Chime when this button is pressed"){
		input "button1", "capability.button", title: "Which button?"
	}
	section("Which Chime"){
		input "chime", "capability.tone", multiple: false
	}
}

def installed() {
	subscribe(button1, "button", buttonHandler)
}

def updated() {
	unsubscribe()
	subscribe(button1, "button", buttonHandler)
}

def buttonHandler(evt) {
	log.debug "$evt.name: $evt.value"
    if (evt.value == "pushed"){ //Button pushed
    	chime.bell1()
	}
}