definition(
    name: "VirtualSwitchParent Orig",
    namespace: "badgermanus",
    author: "badgermanus",
    description: "VirtualSwitchParent Orig",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Connect these virtual switches to the Arduino's relays") {
		input "switch1", title: "Switch for relay 1", "capability.switch"
        input "switch2", title: "Switch for relay 2", "capability.switch", required: false
        input "switch3", title: "Switch for relay 3", "capability.switch", required: false
        input "switch4", title: "Switch for relay 4", "capability.switch", required: false 
        input "switch5", title: "Switch for relay 5", "capability.switch", required: false
        input "switch6", title: "Switch for relay 6", "capability.switch", required: false
        input "switch7", title: "Switch for relay 7", "capability.switch", required: false
        input "switch8", title: "Switch for relay 8", "capability.switch", required: false
        input "ledgreen", title: "Switch for ledgreen", "capability.switch", required: false
        input "ledyellow", title: "Switch for ledyellow", "capability.switch", required: false
	}
    section("Which Arduino relay board to control?") {
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

	// Listen to the virtual switches
	subscribe(switch1, "switch.on", switchOn1)
	subscribe(switch1, "switch.off", switchOff1)
    subscribe(switch2, "switch.on", switchOn2)
	subscribe(switch2, "switch.off", switchOff2)
    subscribe(switch3, "switch.on", switchOn3)
	subscribe(switch3, "switch.off", switchOff3)
    subscribe(switch4, "switch.on", switchOn4)
	subscribe(switch4, "switch.off", switchOff4)
    subscribe(switch5, "switch.on", switchOn5)
	subscribe(switch5, "switch.off", switchOff5)
    subscribe(switch6, "switch.on", switchOn6)
	subscribe(switch6, "switch.off", switchOff6)
    subscribe(switch7, "switch.on", switchOn7)
	subscribe(switch7, "switch.off", switchOff7)
    subscribe(switch8, "switch.on", switchOn8)
	subscribe(switch8, "switch.off", switchOff8)
	subscribe(ledgreen, "switch.on", ledgreenOn)
	subscribe(ledgreen, "switch.off", ledgreenOff)
    subscribe(ledyellow, "switch.on", ledyellowOn)
	subscribe(ledyellow, "switch.off", ledyellowOff)
}

def switchOn1(evt)
{
    	arduino.rcswitch1on()
}

def switchOff1(evt)
{
    	arduino.rcswitch1off()
}
def switchOn2(evt)
{
    	arduino.rcswitch2on()
}

def switchOff2(evt)
{
    	arduino.rcswitch2off()
}

def switchOn3(evt)
{
    	arduino.rcswitch3on()
}

def switchOff3(evt)
{
    	arduino.rcswitch3off()
}
def switchOn4(evt)
{
    	arduino.rcswitch4on()
}

def switchOff4(evt)
{
    	arduino.rcswitch4off()
}

def switchOn5(evt)
{
    	arduino.rcswitch5on()
}

def switchOff5(evt)
{
    	arduino.rcswitch5off()
}
def switchOn6(evt)
{
    	arduino.RelayOn6()
}

def switchOff6(evt)
{
    	arduino.RelayOff6()
}

def switchOn7(evt)
{
    	arduino.RelayOn7()
}

def switchOff7(evt)
{
    	arduino.RelayOff7()
}
def switchOn8(evt)
{
    	arduino.RelayOn8()
}
def switchOff8(evt)
{
    	arduino.RelayOff8()
}
def ledgreenOn(evt)
{
    	arduino.ledgreenon()
}

def ledgreenOff(evt)
{
    	arduino.ledgreenoff()
}

def ledyellowOn(evt)
{
    	arduino.ledyellowon()
}

def ledyellowOff(evt)
{
    	arduino.ledyellowoff()
}