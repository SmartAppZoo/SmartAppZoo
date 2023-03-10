/**
 *  Copyright 2015 Terry Gauchat
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
definition(
	name: "Test Virtual Switch Sync",
	namespace: "CosmicPuppy",
	author: "Terry Gauchat",
	description: "Synchronizes two Virtual Switches without endless looping.",
	category: "My Apps",
	iconUrl: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg",
	iconX2Url: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg",
        iconX3Url: "http://thumb1.shutterstock.com/display_pic_with_logo/1943207/159822338/stock-vector--projector-screen-159822338.jpg"
)

preferences {
	section("When this Virtual Switch is turned on / off :") {
		input "virtualSwitchA", "device.cosmicpuppyVirtualSwitch", title: "VirtualSwitchA", multiple: false, required: true
	}
	section("Sync this Virtual Switch:") {
		input "virtualSwitchB", "device.cosmicpuppyVirtualSwitch", title: "VirtualSwitchB", multiple: false, required: true
	}
}

def installed()
{
	mySubscribes()
}

def updated()
{
	unsubscribe()
	mySubscribes()
}

def mySubscribes() {
    /* NB: Tried "subscribeToCommand", but results were wonky. Defer this method. */
	subscribe(virtualSwitchA, "switch.on", onHandlerA)
	subscribe(virtualSwitchA, "switch.off", offHandlerA)
    
	subscribe(virtualSwitchB, "switch.on", onHandlerB)
	subscribe(virtualSwitchB, "switch.off", offHandlerB)
}

def logHandler(evt) {
	log.debug evt.value
}

def onHandlerA(evt) {
    log.debug "Handler caught On request from virtualSwitchA."
	virtualSwitchB.on()
}

def offHandlerA(evt) {
    log.debug "Handler caught On request from virtualSwitchA."
	virtualSwitchB.off()
}

def onHandlerB(evt) {
    log.debug "Handler caught On request from virtualSwitchB."
    
    /*
       In theory: This could cause endless loop if the virtualSwitch device type doesn't first check State.
       In reality, it seems to only loop once (i.e., the first touched Switch gets hit with only ONE extra Command request.).
       Why no endless loop? Is the observed behavior deterministic? If so, then this is "manageable"... but still, weird.
    */
	// virtualSwitchA.on()
    
    /*
       Alternate method: To avoid a loop, why not try a direct 'sendEvent(device, map)' (per SmartApp Documentation).
       This seems appropriate for a Virtual Switch since all we want to do is update the internal State and Tile,
           No physical device is ever updated here, so it seems nice to avoid an actual Command call.
           NB: The real-world scenario is one Device is physical and one is virtual, and we want to carefully avoid any loops.
    */
    sendEvent( virtualSwitchA, [ name: "switch", value: "on" ] )
}

def offHandlerB(evt) {
    log.debug "Handler caught On request from virtualSwitchB."
	//virtualSwitchA.off()
    
    sendEvent( virtualSwitchA, [ name: "switch", value: "off" ] )
}


/* =========== */
/* End of File */
/* =========== */