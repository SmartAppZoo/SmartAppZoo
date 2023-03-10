/**
 *  Leave The Light On
 *
 *  Copyright 2017 Todd Trivette
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
    name: "Leave The Light On",
    namespace: "toddtriv",
    author: "Todd Trivette",
    description: "Turn on a switch when a device is not home and it gets dark",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When this device is away..."){
		input "myMobileDevice", "capability.presenceSensor", required: true
	}
	section("Turn something on if it's dark..."){
		input "switch1", "capability.switch", multiple: true
	}
	section ("Sunset offset (optional)...") {
		input "sunsetOffsetValue", "text", title: "HH:MM", required: false
		input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"]
	}
}

def installed() {	
	subscribe(myMobileDevice, "presence", presenceHandler)    
    subscribe(switch1, "switch", switchHandler)
}

def updated() {	
	unsubscribe()
	subscribe(myMobileDevice, "presence", presenceHandler)
    subscribe(switch1, "switch", switchHandler)
    runOnce(new Date(), presenceHandler)
}

def presenceHandler(evt) {			
    manageSwitch()        
}

def switchHandler(evt){	
	def switchIsOn = (evt.value=="on")
    // if someone turns off the light switch, make sure it should be off
    if( !switchIsOn ){
    	manageSwitch()
    }
}

def manageSwitch(){
	def deviceIsAway = (myMobileDevice.currentPresence=="not present")
    def switchIsOn = (switch1.currentSwitch[0]=="on")         
    def now = new Date()    
    // This may be the actual sunset or adjusted depending on user selection
    def adjustedTimeOfSunset = getSunriseAndSunset(sunsetOffset: getSunsetOffset()).sunset 	      
    def paddedSunsetTime = null;
    
    // 15 minutes before the time the user has set as darkness
    use( groovy.time.TimeCategory ) {
    	paddedSunsetTime = adjustedTimeOfSunset - 15.minutes        
	}        
            
	if(deviceIsAway && !switchIsOn) {
    	if( now >= adjustedTimeOfSunset ){        
            switch1.on()
            log.info "$switch1.label device was turned on because $myMobileDevice.label is not present"
        }         
        else{
        	// If it is close to dark, check back frequently, otherwise don't check frequently
        	def secondsInAMinute = 60
            def minutes = (now >= paddedSunsetTime ? 5 : 30)
            runIn(secondsInAMinute * minutes, presenceHandler)
            log.info "Not time to turn on the $switch1.label switch yet. Check again in $minutes minutes."
        }
	} 
    else{    	
    	log.info "No need to turn on the $switch1.label switch because ${switchIsOn ? "it is already on" : "$myMobileDevice is present"}."
    }
}


private getSunsetOffset() {	    
	if (sunsetOffsetDir == null)
    	return null
      
	sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null              
}
