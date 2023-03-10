/**
 *  The Big Switch
 *
 *  Author: SmartThings
 *  Modified by: Bill Napier <napier@pobox.com>
 *
 *  Date: 2015-05-11
 */
definition(
	name: "Bill's Big Switch",
	namespace: "billnapier",
	author: "Bill Napier",
	description: "Turns on, off and dim a collection of lights based on the state of a specific switch.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("When this switch is turned on or off") {
		input "master", "capability.switch", title: "Where?"
	}
	section("Turn on or off all of these switches as well") {
		input "switches", "capability.switch", multiple: true, required: false
	}
}

def installed()
{
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
}

def updated()
{
	unsubscribe()
	subscribe(master, "switch.on", onHandler)
	subscribe(master, "switch.off", offHandler)
}

def logHandler(evt) {
	log.debug evt.value
}

def onHandler(evt) {
	log.debug evt.value
  switches?.on()
  switches?.setLevel(100)
}

def offHandler(evt) {
	log.debug evt.value
  switches?.off()
}
