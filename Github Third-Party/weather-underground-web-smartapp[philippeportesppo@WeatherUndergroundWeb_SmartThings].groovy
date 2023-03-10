/**
 *  Weather Underground Web Smartapp
 *
 *  Copyright 2018 Philippe PORTES
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
    name: "Weather Underground Web Smartapp",
    namespace: "philippeportesppo",
    author: "Philippe PORTES",
    description: "SmartApp enabling weather alerts and weather triggered actions based on Weather Underground web API (no hardware is required)",
    category: "Green Living",
    iconUrl: "http://icons.wxug.com/graphics/wu2/logo_130x80.png",
    iconX2Url: "http://icons.wxug.com/graphics/wu2/logo_130x80.png",
    iconX3Url: "http://icons.wxug.com/graphics/wu2/logo_130x80.png")


preferences {


        section("Alert Settings") {
            input "wusnowalert", "bool", title: "Snow Alert"
            input "wustormalert", "bool", title: "Storm Alert" 
        	input "wurainalert", "bool", title: "Rain Alert"
			input "wulowtempalert", "number", title: "Low temperature Alert (C or F)", required: false
 			input "wuhightempalert", "number", title: "High temperature Alert (C or F)", required: false
			input "wulowhumidityalert", "decimal", title: "Low humidity Alert (0-100)", required: false
            input "wuhighhumidityalert", "decimal", title: "High humidity Alert (0-100)", required: false            
        }
        
        section("Switch On these on Snow Alert:")
        {
        	input "wusnowon", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on Snow Alert:")
        {
        	input "wusnowoff", "capability.switch", required: false, multiple: true
        }
        
        section("Switch On these on Rain Alert:")
        {
        	input "wurainon", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on Rain Alert:")
        {
        	input "wurainoff", "capability.switch", required: false, multiple: true
        }
        
        section("Switch On these on Storm Alert:")
        {
        	input "wustormon", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on Storm Alert:")
        {
        	input "wustormoff", "capability.switch", required: false, multiple: true
        }
        
		section("Switch On these on Low Temperature Alert:")
        {
        	input "wulowton", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on Low Temperature Alert:")
        {
        	input "wulowtoff", "capability.switch", required: false, multiple: true
        }
       	
        section("Switch On these on High Temperature Alert:")
        {
        	input "wuhighton", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on High Temperature Alert:")
        {
        	input "wuhightoff", "capability.switch", required: false, multiple: true
        }
        
        section("Switch On these on Low Humidity Alert:")
        {
        	input "wulowhon", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on Low Humidity Alert:")
        {
        	input "wulowhoff", "capability.switch", required: false, multiple: true
        }
        
        section("Switch On these on High Humidity Alert:")
        {
        	input "wuhighhon", "capability.switch", required: false, multiple: true
        }
        
        section("Switch Off these on High Humidity Alert:")
        {
        	input "wuhighhoff", "capability.switch", required: false, multiple: true
        }
        
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	state.deviceId="12345678AE"
    state.deviceName=""
    state.deviceRef= getAllChildDevices()?.find {
    it.device.deviceNetworkId == state.deviceId}
	log.debug "state.deviceRef installed with ${state.deviceRef}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe() 
    addDevices()
    //initialize()

}

def initialize() {
	log.debug "initialize"
    
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

def addDevices() {
	    
    if (childDevices)
    {
        def Ref= getAllChildDevices()?.find {
            it.device.deviceNetworkId == state.deviceId}
        log.debug "Devices installed before removal ${Ref}"
        // Make sure the settings are applied by removing the previous occurence
        removeChildDevices(getChildDevices())

        Ref= getAllChildDevices()?.find {
            it.device.deviceNetworkId == state.deviceId}
        log.debug "Devices installed after removal ${Ref}"
    }
    // and create it again with the new settings
    def mymap = getWeatherFeature("conditions")

    def wucity = mymap['current_observation']['display_location']['full']
    subscribe(addChildDevice("philippeportesppo", "Weather Underground Web", state.deviceId, null, [
        "label": "Weather in ${wucity}",
        "data": [
            "wusnowalert": wusnowalert,
            "wustormalert": wustormalert,
            "wurainalert": wurainalert,
            "wulowtempalert": wulowtempalert,
            "wuhightempalert": wuhightempalert,
            "wulowhumidityalert": wulowhumidityalert,
            "wuhighhumidityalert": wuhighhumidityalert,
            /*completedSetup: true*/]
    ]), "Alert", eventHandler)                           
}

def eventHandler(evt)
{
	Map options = [:]
	log.debug "WUW evt: ${evt}"
	if (evt.name == "Alert")
    {
    	if (evt.value.contains("Snow")) {
            if (wusnowon!=null)
        		wusnowon.on()
            if (wusnowoff!=null)
            	wusnowoff.off()
            }
            
    	if (evt.value.contains("Rain")) {
            if (wurainon!=null)
		       	wurainon.on()
            if (wurainoff!=null)
            	wurainoff.off()
            }    
            
		if (evt.value.contains("Storm")) {
            if (wurainon!=null)
        		wurainon.on()
            if (wurainoff!=null)
            	wurainoff.off()
            }     	

		if (evt.value.contains("Temperature")) {
        	if (evt.value.contains("High"))
            {
               	if (wuhighton!=null)
        			wuhighton.on()
                    
                if (wuhighton!=null)
            		wuhightoff.off()
            }
            else
            {
                if (wulowton!=null)
        			wulowton.on()

                if (wulowtoff!=null)
            		wulowtoff.off()            
            }
		}
        
        if (evt.value.contains("Humidity")) {
        	if (evt.value.contains("High"))
            {
            	if (wuhighhon!=null)
        			wuhighhon.on()
                    
                if (wuhighhoff!=null)
            		wuhighhoff.off()
            }
            else
            {
                if (wulowhon!=null)
        			wulowhon.on()
                    
                if (wulowhoff!=null)
	            	wulowhoff.off()            
            }
		}
        
    	options.method = 'push'
        sendNotification(evt.value, options) 

    }
 
                    
}
