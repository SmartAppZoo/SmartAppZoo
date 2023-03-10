/**
 *  Gatekeeper
 *
 *  Copyright 2015 Herbert Carroll
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
    name: "Gatekeeper",
    namespace: "herbcarroll",
    author: "Herbert Carroll",
    description: "Watches a gate or door and alerts if left open for longer than a defined number of seconds.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/herbcarroll/resources/master/stIcons/doors-active.png",
    iconX2Url: "https://raw.githubusercontent.com/herbcarroll/resources/master/stIcons/doors-active%402x.png",
    iconX3Url: "https://raw.githubusercontent.com/herbcarroll/resources/master/stIcons/doors-active%402x.png",
    )


preferences {

	section ("Which gate, door , or window sensor should we watch?")
    {
    	input "contactSensors", "capability.contactSensor", title: "Enter a contact sensor to monitor", required : true, multiple: false   
    }

	section("How many seconds do you want to allow the door to remain open before alerting?")
    {
		input "seconds", "number", title: "The number of seconds to wait before alerting the first time", range: "5..*", required: true;
        input "secsNext", "number", title: "The number of seconds to wait subsequent alerts", range: "10..*", required: true;
        
    }
 
    section("Which lights do you want to flash...")
    {
			input "switches", "capability.switch", title: "Flash these lights", multiple: true, required : false
			input "numFlashes", "number", title: "Flash them this many times.  (default 3)", range : "1..10", required: false
	}
    
    //on for 3 secs, off for 3 works best
	//section("Time settings in milliseconds (optional)...")
    //{
	//	input "onFor", "number", title: "On for (default 1000)", required: false
	//	input "offFor", "number", title: "Off for (default 1000)", required: false
	//}
    
	section( "Notifications" )
    {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(contactSensors, "contact.open", doorOpened );
    subscribe(contactSensors, "contact.closed", doorClosed );
    //subscribe(switches, "switch", flashingLightsHandler ); 
}


def doorOpened(evt) 
{  
	log.debug "door open checking in ${seconds} seconds!";
   	state.TimeOpened= now();
   	runIn(seconds, checkIfStillOpened, [overwrite : true] );
}

def doorClosed(evt) {
   
   log.debug "door closed!";
  
   if ( state.TimeOpened && state.alert )
   		reportClosed(now()-state.TimeOpened);
        
   state.alert=false;
   state.TimeOpened= null;
   state.flashing = false; //stop flashing that may be going on
}

def checkIfStillOpened( )
{
	log.debug "checking door... door " + (state.TimeOpened ? "still opened!" : "was closed!");
	if (!state.TimeOpened)
    	return;
    if ( contactSensors.currentValue("contact")=="closed")
    {	
    	//sendMessage( "Warning.  Contact was found to be closed by state.TimeOpened is not null ${state.TimeOpened}");
        state.TimeOpened=null;
        return;
    }

    def falseAlarm = now()-state.TimeOpened < seconds;
    log.debug "Time elapsed since opened is ${now()-state.TimeOpened} which should be more than ${seconds}.  Therefore false alaram = ${falseAlarm}"; 
    //runIn overwrite=true negates the need to check for false alarms

	reportOpened(now()-state.TimeOpened);
	
	state.alert=true;
	flashLights();
    runIn(secsNext, checkIfStillOpened ); 

}

private reportClosed( msecs )
{
	def secs = msecs/1000;
    if ( secs < 60 )
    {
		sendMessage ("${contactSensors.displayName} closed after ${secs} seconds!");
        return;
    }
    def mins = secs/60;
    sendMessage ("${contactSensors.displayName} closed after ${mins} minutes!");
}

 
private reportOpened( msecs )
{
	def secs = msecs/1000;
    if ( secs < 60 )
    {
		sendMessage ("${contactSensors.displayName} left opened ${secs} seconds!");
        return;
    }
    def mins = secs/60;
    sendMessage ("${contactSensors.displayName} left opened ${mins} minutes!");    
}
def sendMessage( msg )
{
	if ( sendPushMessage == "Yes" )
    	sendPush(msg)
    else
    	sendNotificationEvent( msg )
}



private flashLights() 
{
	
    def doFlash = true
	def onFor = 3000;
	def offFor = 3000; 
	def numFlashes = numFlashes ?: 3

	log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
	if (state.lastActivated) 
    {
		def elapsed = now() - state.lastActivated
		def sequenceTime = (numFlashes + 1) * (onFor + offFor)
		doFlash = elapsed > sequenceTime
		log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
	}

	if (doFlash) 
    {
		log.debug "FLASHING $numFlashes times"
		state.lastActivated = now()
		log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
		def initialActionOn = switches.collect{it.currentSwitch != "on"}
		def delay = 0L
		numFlashes.times 
        {
			log.trace "Switch on after  $delay msec"
			switches.eachWithIndex {s, i -> initialActionOn[i] ?  s.on(delay: delay) :s.off(delay:delay) }
			delay += onFor;
			log.trace "Switch off after $delay msec"
            switches.eachWithIndex {s, i -> initialActionOn[i] ?  s.off(delay: delay) :s.on(delay:delay) }
            
			delay += offFor
		}
	}
}

private flashLights_()  //event based not used
{
	state.flashing = true;
    def lights = switches.collect{it.currentSwitch == "on"};
    state.flashes = switches.collect{ (numFlashes?:3) * 2 };
    
    switches.eachWithIndex {s, i -> lights[i] ?  s.off() :s.on() };
    state.flashes.eachWithIndex{ s, i -> state.flashes[i]-- };
    
}

def flashingLightsHandler( evt )
{
	log.debug "flashing event!"
	if ( !state.flashing )
    	return;
    
   	def onFor = onFor ?: 3000
	def offFor = offFor ?: 1000

	def i  = switches.findIndexOf { it.displayName == evt.displayName };
    if ( i == -1 )
    {
    	log.debug "unexpected error, event for light no found - switches = ${switches} displayName = ${evt.displayName}, ${evt}";
        return;
	}
    
    def numOfFlashesLeft = state.flashes[i];
    if ( numOfFlashesLeft > 0 )
    	 evt.value=="on" ? switches[i].off(delay : offFor) : switches[i].on(delay : onFor);

    state.flashes[i]=numOfFlashesLeft-1;
    log.debug "${state.flashes[i]} flashes left for light ${evt.name} at index ${i}";

	state.flashing = state.flashes.any { it-> it > 0 };
    
    if ( state.flashing == false )
    	log.debug "flashing done for all lights!, state.flashes = ${state.flashes}";
	
}