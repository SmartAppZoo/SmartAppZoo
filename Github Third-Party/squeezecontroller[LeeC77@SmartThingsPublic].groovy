/**
 *  squeezeController
 *
 *  Copyright 2014 Mike Maxwell
 *  Orginal updated by Lee Charlton to handle volume control, status refresh, sync and unsync and playlists.
 *  Updated again by Lee Charlton to add auto refresh of status on all players.
 *	And again to add music library rescan
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "squeezeController",
    namespace: "LeeC77",
    author: "Lee Charlton",
    description: "SqueezeBox virtual supervisor.",
    category: "My Apps",
    iconUrl: "https://static.thenounproject.com/png/2728152-200.png",
    iconX2Url: "https://static.thenounproject.com/png/2728152-200.png",
    iconX3Url: "https://static.thenounproject.com/png/2728152-200.png")


/**
 *  VirtualSwitchParent
 *
 *  Author: badgermanus@gmail.com
 *  Date: 2014-03-26
 */
preferences {
    section("Squeeze Box Server replies"){ 
    	input "slurp", "capability.Sensor", title: "Which JSON Slurper reports connection status?", multiple: false, required: true
    }
	section("Connect these virtual switches to the squeeze players") {
	    input "switch1", title: "Player 1", "capability.switch", required: true
        input "switch2", title: "Player 2", "capability.switch", required: true
        input "switch3", title: "Player 3", "capability.switch", required: true
        input "switch4", title: "Player 4", "capability.switch", required: true
        input "switch5", title: "Player 5", "capability.switch", required: true

	}
    section("Which squeeze server?") {
		input "squeeze", title: "Server","capability.switch", required: true
    }    
}

def installed() {
	//log.debug "Installed with settings: ${settings}"
    
	subscribe()
}

def updated() {
	//log.debug "Updated with settings: ${settings}"
    unsubscribe()
	subscribe()
    litsentoserver() //turn on realtime (suscription) listen to SB server
    runEvery3Hours(litsentoserver) //keep subscription going
}

def subscribe() {
	// switch on / off each player via these events
	subscribe(switch1, "switch.turningOn", p1On)
	subscribe(switch1, "switch.turningOff", p1Off)
    subscribe(switch2, "switch.turningOn", p2On)
	subscribe(switch2, "switch.turningOff", p2Off)
    subscribe(switch3, "switch.turningOn", p3On)
	subscribe(switch3, "switch.turningOff", p3Off)
    subscribe(switch4, "switch.turningOn", p4On)
	subscribe(switch4, "switch.turningOff", p4Off)
    subscribe(switch5, "switch.turningOn", p5On)
	subscribe(switch5, "switch.turningOff", p5Off)
    // refresh on/off status of each player via these events
    subscribe(switch1, "refresh.refresh", p1refresh)
    subscribe(switch2, "refresh.refresh", p2refresh)
    subscribe(switch3, "refresh.refresh", p3refresh)
    subscribe(switch4, "refresh.refresh", p4refresh)
    subscribe(switch5, "refresh.refresh", p5refresh)
    //subscribe(squeeze, "refresh.refresh", allrefresh)
    // volume up/down of each player via these events
    subscribe(switch1, "volume.up", p1volup)
    subscribe(switch1, "volume.down", p1voldn)
    subscribe(switch2, "volume.up", p2volup)
    subscribe(switch2, "volume.down", p2voldn)
    subscribe(switch3, "volume.up", p3volup)
    subscribe(switch3, "volume.down", p3voldn)
    subscribe(switch4, "volume.up", p4volup)
    subscribe(switch4, "volume.down", p4voldn)
    subscribe(switch5, "volume.up", p5volup)
    subscribe(switch5, "volume.down", p5voldn)
    // raw slurper response via this event
    subscribe(slurp, "sbsresponse", respHandler)
    // response per player via these events
	subscribe(squeeze, "p1response", p1Resp) 
    subscribe(squeeze, "p2response", p2Resp)
    subscribe(squeeze, "p3response", p3Resp)
    subscribe(squeeze, "p4response", p4Resp)
    subscribe(squeeze, "p5response", p5Resp)
    //subscribe(squeeze, "p5response", p5Resp)
}

def litsentoserver(){
	squeeze.serverlistenoff() //turn off realtime listen to SB server
    squeeze.serverlistenon()  //turn on realtime listen to SB server
}

//Handles the raw return fron the Slurper
def respHandler(evt) {
    //log.debug "In SBS event handler"
	//log.debug "Event: $evt.name $evt.value"
    
    // Simply passes response onto Squeeze Switch
    squeeze.SBSResp("${evt.value}")
}

// Handles the responses for each player switch
def p1Resp(evt) {
	//log.debug " in player 1 response handler"
	//log.debug "$evt.name $evt.value"
    // Simply passes response onto sqVS
    switch1.PlayerResp("${evt.value}")
}

def p2Resp(evt) {
	//log.debug " in player 2 response handler"
	//log.debug "$evt.name $evt.value"
    switch2.PlayerResp("${evt.value}")
}

def p3Resp(evt) {
	///log.debug " in player 3 response handler"
	//log.debug "$evt.name $evt.value"
    switch3.PlayerResp("${evt.value}")
}

def p4Resp(evt) {
	//log.debug " in player 4 response handler"
	//log.debug "$evt.name $evt.value"
    switch4.PlayerResp("${evt.value}")
}

def p5Resp(evt) {
	//log.debug " in player 5 response handler"
	//log.debug "$evt.name $evt.value"
    switch5.PlayerResp("${evt.value}")
}

// Hnadles the play / off for each player via the SBS
def p1On(evt)
{
	//log.debug "switchOn1($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1On()
}

def p1Off(evt)
{
	//log.debug "switchOff1($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1Off()
}
def p2On(evt)
{
	//log.debug "switchOn2($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2On()
}

def p2Off(evt)
{
	//log.debug "switchOff2($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2Off()
}

def p3On(evt)
{
	//log.debug "switchOn3($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3On()
}

def p3Off(evt)
{
	//log.debug "switchOff3($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3Off() 
}
def p4On(evt)
{
	//log.debug "switchOn4($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p4On()
}

def p4Off(evt)
{
	//log.debug "switchOff4($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p4Off()
}

def p5On(evt)
{
	//log.debug "switchOn5($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p5On()
}

def p5Off(evt)
{
	//log.debug "switchOff5($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p5Off()
}

def p1refresh(evt)
{
	//log.debug "refresh1($evt.name: $evt.value: $evt.deviceId)"
	squeeze.p1refresh()
}
def p2refresh(evt)
{
	//log.debug "refresh2($evt.name: $evt.value: $evt.deviceId)"
	squeeze.p2refresh()
}
def p3refresh(evt)
{
	//log.debug "refresh3($evt.name: $evt.value: $evt.deviceId)"
	squeeze.p3refresh()
}
def p4refresh(evt)
{
	//log.debug "refresh4($evt.name: $evt.value: $evt.deviceId)"
	squeeze.p4refresh()
}
def p5refresh(evt)
{
	//log.debug "refresh5($evt.name: $evt.value: $evt.deviceId)"
	squeeze.p5refresh()
}
/*def allrefresh(evt)
{
	squeeze.serverlistenon()// turn on listen to SB server
    // replies for the server are solicited 
}*/




// *************** handles volume up/down ***********************
def p1volup(evt)
{
 	//log.debug "Vol1 + ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1Volup()
}
def p1voldn(evt)
{
 	//log.debug "Vol1 - ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p1Voldn()
}
def p2volup(evt)
{
 	//log.debug "Vol2 + ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2Volup()
}
def p2voldn(evt)
{
	//log.debug "Vol2 - ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p2Voldn()
}
def p3volup(evt)
{
	//log.debug "Vol3 + ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3Volup()
}
def p3voldn(evt)
{
	//log.debug "Vol3 - ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p3Voldn()
}
def p4volup(evt)
{
	//log.debug "Vol4 + ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p4Volup()
}
def p4voldn(evt)
{
	//log.debug "Vol4 - ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p4Voldn()
}
def p5volup(evt)
{
	//log.debug "Vol5 + ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p5Volup()
}
def p5voldn(evt)
{
	//log.debug "Vol5 - ($evt.name: $evt.value: $evt.deviceId)"
    squeeze.p5Voldn()
}