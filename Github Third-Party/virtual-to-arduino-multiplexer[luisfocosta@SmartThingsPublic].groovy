definition(
    name: "Virtual to Arduino Multiplexer",
    namespace: "luisfocosta",
    author: "Daniel Ogorchock/Luis Costa",
    description: "Virtual to Arduino Multiplexer",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Connect this Garage contact sensor") {
        input "garagedoorsensor", title: "Garage Door Sensor", "capability.contactSensor"
	}

    section("Which Arduino board to control") {
		input "arduino", "capability.switch"
    }    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribe()
}

def subscribe() {

//    subscribe(arduino, "momentary.pushed", leftDoorOpen)
// subscreve comandos enviados diretamente no Arduino??
    //subscribe(GarageDoorButton, "momentary.pushed", PushButton)

    //subscribe(arduino, "relay.push", ArduinoGaragePushButton)

    subscribe(GarageDoorButton, "garagedoor", GarageDoorPush)
    
    subscribe(garagedoorsensor, "contact.open", garageDoorOpen)
    subscribe(garagedoorsensor, "contact.closed", garageDoorClosed)
}

def GarageDoorPush(evt)
{
    log.debug "virtualGarageDoor($evt.name: $evt.value: $evt.deviceId)"
    log.debug "Button was pushed"
    //arduino.push()
    //GarageDoorButton.pushButton()
    //sendevent(deviceid: GarageDoorButton, value: "Open")

    //sendevent(deviceid: arduino, value: "on")

    //corresponds to Arduino groovy push function
}

def garageDoorOpen(evt)
{
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    //GarageDoorButton.open()
    sendevent(deviceid: GarageDoorButton, value: "open")
    //envia para o multiplexer comando para mudar estado
}

def garageDoorClosed(evt)
{
    log.debug "arduinoevent($evt.name: $evt.value: $evt.deviceId)"
    //GarageDoorButton.closed()
    sendevent(deviceid: GarageDoorButton, value: "closed")
    //envia para o multiplexer comando para mudar estado
}
