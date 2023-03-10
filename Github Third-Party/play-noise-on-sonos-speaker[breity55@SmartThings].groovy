definition(
	name: "Play Noise on Sonos Speaker",
	namespace: "breity55",
	author: "Alex Breitenstein",
	description: "Play a sound on Sonos speaker when a switch is triggered.",
	category: "Fun & Social",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@3x.png"
)

preferences {
	section("When Switch Turns On...") {
		input "switchInput", "capability.switch", required: true, title: "What Switch?"
	}
    section("Play", hideable: true, hidden: false) {
		input "actionType", "enum", title: "What Sound?", required: true, options: [
				"Clue",
				"Lambeau Leap",
				"Lightsaber"]
    	input "sonos", "capability.musicPlayer", title: "What Sonos Speaker?", required: true
        input "volume", "number", title: "Temporarily Change Volume to What?", description: "0-100%", required: true
    }
    section("Additional Options", hideable: true, hidden: false) {
    	input "isToggleLights", "bool", title: "Toggle Lights On and Off?", required: true
        input "lights", "capability.switch", title: "Turn Off What Light(s)?", multiple: true, required: false
    }
}

def handleEvent(e) {

	if(isToggleLights) {
		lights.off()
    }
    
	sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
    
    if(isToggleLights){
    	def delay = Math.round(Double.parseDouble(state.sound.duration))
		delay = delay.toInteger()
    	runIn(delay, turnLightsOn)
    }
    
    switchInput.off()
}

def installed() {
   	subscribeEvents()
}

def updated() {
	unsubscribe()
	subscribeEvents()
}

def subscribeEvents() {
	loadSongData()
	subscribe(switchInput, "switch.on", handleEvent)
}

def turnLightsOn() {
	lights.on()
}

private loadSongData() {
	switch (actionType) {
    	case "Clue":
        	state.sound = [uri: "https://www.dropbox.com/s/7y5k5m50ifv498l/Clue.mp3?raw=1", duration: "20"]
        	break;
        case "Lambeau Leap":
        	state.sound = [uri: "https://www.dropbox.com/s/obvgyqcffsy9crt/Bang%20The%20Drum%20All%20Day.mp3?raw=1", duration: "212"]
        	break;
		default:
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/lightsaber.mp3", duration: "10"]
			break;
	}
}