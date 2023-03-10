/**
 *  MyButton Handler
 *
 *  Copyright 2015 Kenny Keslar
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
    name: "MyButton Handler",
    namespace: "r3dey3",
    author: "Kenny Keslar",
    description: "MyButtonHandler",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: false
)

def ACTIONS = [ "Turn Off", 
 "Soft White", "White", "Daylight", "Warm White", 
 "Red","Green","Blue","Yellow","Orange","Purple","Pink",
 "Dim Soft White", "Dim White", "Dim Daylight", "Dim Warm White", 
 "Dim Red","Dim Green","Dim Blue","Dim Yellow","Dim Orange","Dim Purple","Dim Pink" 
 ]

preferences {
	section("Which Devices") {
        input "remote", "capability.button", title: "Remote", multiple: false, required: true
        input "light", "capability.switch", title: "Light", multiple: false, required: true
    }
    section("Which Actions") {
    	input "button1_pushed", "enum", title: "Button 1 Pushed", required: false, multiple: false, options: ACTIONS
    	input "button1_held", "enum", title: "Button 1 Held", required: false, multiple: false, options: ACTIONS
    	input "button2_pushed", "enum", title: "Button 2 Pushed", required: false, multiple: false, options: ACTIONS
    	input "button2_held", "enum", title: "Button 2 Held", required: false, multiple: false, options: ACTIONS
    	input "button3_pushed", "enum", title: "Button 3 Pushed", required: false, multiple: false, options: ACTIONS
    	input "button3_held", "enum", title: "Button 3 Held", required: false, multiple: false, options: ACTIONS
    	input "button4_pushed", "enum", title: "Button 4 Pushed", required: false, multiple: false, options: ACTIONS
    	input "button4_held", "enum", title: "Button 4 Held", required: false, multiple: false, options: ACTIONS
    }
}

def installed() {
  // subscribe to any change to the "button" attribute
  // if we wanted to only subscribe to the button be held, we would use
  // subscribe(thebutton, "button.held", buttonHeldHandler), for example.
	log.debug "Installed with settings: ${settings}"
	initialize()
}
def colorNameToVal(color) {
	def ret = [:]
	ret.saturation = 100
	ret.level = 100
	switch(color) {
		case ~/.*White/:
			ret.hue = 52
			ret.saturation = 19
			break;
		case ~/.*Daylight/:
			ret.hue = 53
			ret.saturation = 91
			break;
		case ~/.*Soft White/:
			ret.hue = 23
			ret.saturation = 56
			break;
		case ~/.*Warm White/:
			ret.hue = 20
			ret.saturation = 80 //83
			break;
		case ~/.*Blue/:
			ret.hue = 66
			break;
		case ~/.*Green/:
			ret.hue = 33
			break;
		case ~/.*Yellow/:
			ret.hue = 16
			break;
		case ~/.*Orange/:
			ret.hue = 10
			break;
		case ~/.*Purple/:
			ret.hue = 75
			break;
		case ~/.*Pink/:
			ret.hue = 83
			break;
		case ~/.*Red/:
			ret.hue = 100
			break;
	}
    switch (color) {
    	case ~/Dim.*/:
           ret.level = 50
           break;
    }
	return ret
}

def get_action(num, value) {
	def preferenceName = "button" + num + "_" + value
	def pref = settings[preferenceName]
    log.debug "Found: $pref for $preferenceName"

	return pref
}


def execute(num, value) {
    def action = get_action(num, value)
    log.debug "execute($num, $value) = $action"
    if (action == null)
        return
        
    if (action == "Turn Off") {
    	light.off()
        return
    }
    def val = colorNameToVal(action)
    log.debug("Value = $val")
    light.setColor(val)
    light.on()
}

def buttonHandler(evt) {
  if (evt.value == "held") {
    log.debug "button was held"
  } else if (evt.value == "pushed") {
    log.debug "button was pushed"
  }
    def value = evt.value
    def buttonNumber = evt.data
    def recentEvents = remote.eventsSince(new Date(now() - 1000)).findAll{it.value == evt.value && it.data == evt.data}
    log.debug "Found ${recentEvents.size()?:0} events in past 1 second"

    if(recentEvents.size <= 1){
        switch(buttonNumber) {
            case ~/.*1.*/:
            execute(1, value)
            break
            case ~/.*2.*/:
            execute(2, value)
            break
            case ~/.*3.*/:
            execute(3, value)
            break
            case ~/.*4.*/:
            execute(4, value)
            break
        }
    } else {
        log.debug "Found recent button press events for $buttonNumber with value $value"
    }

  // Some button devices may have more than one button. While the
  // specific implementation varies for different devices, there may be
  // button number information in the jsonData of the event:
  try {
    def data = evt.jsonData
    //def buttonNumber = data.buttonNumber as Integer
    log.debug "evt.jsonData: $data"
    log.debug "button number: $buttonNumber"
  } catch (e) {
    log.warn "caught exception getting event data as json: $e"
  }
}


def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
  subscribe(remote, "button", buttonHandler)
	log.debug "Installed with settings: ${settings}"
}

// TODO: implement event handlers