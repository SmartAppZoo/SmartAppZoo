/**
 *  Hue Lights
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
    name: "Cycle Hue Lights",
    namespace: "herbcarroll",
    author: "Herbert Carroll",
    description: "cycles through scenes on hue lights when the zwave switch is on and on is pressed",
    category: "My Apps",
    iconUrl: "https://cdn-cx-images.dynamite.myharmony.com/mh/ssv2/devices/philips-hue-glamour-v1.png",
    iconX2Url: "https://cdn-cx-images.dynamite.myharmony.com/mh/ssv2/devices/philips-hue-glamour-v1.png",
    iconX3Url: "https://cdn-cx-images.dynamite.myharmony.com/mh/ssv2/devices/philips-hue-glamour-v1.png")


preferences {
	page( name: "selectDevices", install : false, uninstall : true, nextPage : "viewRestUrl" ){
    section ("Control switch...") {
        input "switch1", "capability.switch", required : false
    }
    
    section ("Control button...") {
        input "button", "capability.momentary", required : false
    }
    
    section("Control these bulbs...") {
			input "hues", "capability.colorControl", title: "Which Hue Bulbs?", required:true, multiple:true
		}
        
     section([mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
        
    }
    
   
    
    page(name: "viewRestUrl")
}

mappings {
        path("/cycle") {action: [GET: "cycle"]}
		path("/link") {action: [GET: "link"]}
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
	
    if( switch1 )
		subscribe(switch1, "switch", switchHandler,  [filterEvents: false]);
        
    if ( button )
    	subscribe(button, "momentary.pushed", buttonHandler,  [filterEvents: false]);
    
   subscribe (app, appTouch ); 
}

def appTouch(evt)
{
	log.debug "touch! $evt";
    cycle();
}

def buttonHandler(evt) {
   cycle();
}

def switchHandler(evt) {
    log.debug "Turning $evt.value!!!"
    
    //log.debug "switch value as a string: ${switch1}"

    if (evt.value == "on") {
    
       log.debug "${evt.isStateChange()}"
        
    	if (state.switch == 1) 
        {	
        	log.debug "$switch1 was already on"
            
        }
        else if ( state.switch == 0 )
        {
        	log.debug "$switch1 was off"
        	state.switch = 1;
            state.cycle=0  //this allows us to reset every time to the same color when turning off and on
		}
        else
        {
        	log.debug "i dunno!"
        }
        
        cycle();
        state.switch = 1
            
    } else if (evt.value == "off") {
    
    	state.switch = 0
        
    }
}


def cycle() 
{			
	log.debug "Cycling...  current state ${state.cycle}"
    if ( state.cycle!=null )
    {
    	state.cycle = state.cycle+1
    	state.cycle = state.cycle % 12
    }
    else
    {
    	state.cycle=0;
    }
    
    log.debug "Cycling...  new state ${state.cycle}"
 
	def hueColor = 0
	def saturation = 100
    def effect = "none"

	switch(state.cycle) {
		case 0: 
        	state.color="White"
			hueColor = 52
			saturation = 19
			break;
		case 1:  
        	state.color="Daylight"
			hueColor = 53
			saturation = 91
			break;
		case 2 :
        	state.color="Soft White"
			hueColor = 23
			saturation = 56
			break;
		case 3 : 
        	state.color="Warm White"
			hueColor = 20
			saturation = 80 //83
			break;
		case 4: 
        	state.color="Blue"
			hueColor = 70
			break;
		case 5: 
        	state.color="Green"
			hueColor = 39
			break;
		case 6: 
        	state.color="Yellow"
			hueColor = 25
			break;
		case 7: 
        	state.color="Orange"
			hueColor = 10
			break;
		case 8: 
        	state.color="Purple"
			hueColor = 75
			break;
		case 9: 
        	state.color="Pink"
			hueColor = 83
			break;
		case 10 :
        	state.color="Red"
			hueColor = 100
			break;
        case 11 :
        	state.color="White"
			hueColor = 52
			saturation = 19
            effect = "colorloop"
        	break;
	}
	state.previous = [:]

	hues.each {
		state.previous[it.id] = [
			"switch": it.currentValue("switch"),
			"level" : it.currentValue("level"),
			"hue": it.currentValue("hue"),
			"saturation": it.currentValue("saturation"),
            "effect" : it.currentValue("effect")
		]
	}

	log.debug "current values = $state.previous"

	def newValue = [hue: hueColor, saturation: saturation, effect :effect, level: lightLevel as Integer ?: 100]
	log.debug "new value = $newValue"

	hues*.setColor(newValue)
   sendNotificationEvent("Cycling lights color to ${state.color}");
}


def viewRestUrl() {
	def returnPath ="selectDevices";
	
    dynamicPage(name: "viewRestUrl", title: "Refresh URL", install:true, nextPage: null) {
		section() {
			paragraph "Here you can aquire the URL to trigger a refresh of all devices.  It can be used in a browser or third party poller."
			href url:"${generateURL("link").join()}", style:"embedded", required:false, title:"URL", description:"Tap to view, then click \"Done\""
			}
		
		section() {
			href returnPath, title:"Return to settings"
		}
	}
}


def link() {
	def appCommand="cycle";
	if (!params.access_token) 
    	return ["You are not authorized to view OAuth access token"];
	render contentType: "text/html", data: """<!DOCTYPE html><html><head><meta charset="UTF-8"/><meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width, height=device-height, target-densitydpi=device-dpi" /></head><body style="margin: 0;"><div style="padding:10px">${app.name} URL:</div><textarea rows="9" cols="30" style="font-size:10px; width: 100%">${generateURL(appCommand).join()}</textarea><div style="padding:10px">Copy the URL above and tap Done.</div></body></html>"""
}


def generateURL(path) {
	log.debug "resetOauth: $settings.resetOauth, $resetOauth, $settings.resetOauth"
    
	if (settings.resetOauth) {
		log.debug "Reseting Access Token"
		state.accessToken = null
	}
	
	if (settings.resetOauth || !state.accessToken) {
		try {
			createAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.error "Did you forget to enable OAuth?"
			log.error ex
		}
	}
	
	["https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/$path", "?access_token=${state.accessToken}"]
}