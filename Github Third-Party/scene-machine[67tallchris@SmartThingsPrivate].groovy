/**
 *  App Name:   Scene Machine
 *
 *  Author: 	Todd Wackford // modifier by Antoine Mercadal
 *				twack@wackware.net
 *  Date: 		2013-06-14
 *  Version: 	1.1
 *  
 *  Updated:    2013-07-25
 *  
 *  Change #1	Fixed bug where string null was being returned for non-dimmers and
 *              was trying to assign to variable.
 * 
 *  Change #2	Updated setLevel setion to work with bulbs that were not defined as type "Dimmer Switch"
 *  
 *  
 *  This app lets the user select from a list of switches or dimmers and record 
 *  their currents states as a Scene. It is suggested that you name the app   
 *  during install something like "Scene - Romantic Dinner" or   
 *  "Scene - Movie Time". Switches can be added, removed or set at new levels   
 *  by editing and updating the app from the smartphone interface.
 *
 *  Usage Note: GE-Jasco dimmers with ST is real buggy right now. Sometimes the levels
 *              get correctly, sometimes not.
 *              On/Off is OK.
 *              Other dimmers should be OK.
 *  
 * Use License: Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 *              http://opensource.org/licenses/NPOSL-3.0
 */
// Automatically generated. Make future change here.
definition(
    name: "Scene Machine",
    namespace: "primalmotion",
    author: "todd@wackford.net",
    description: "This app lets the user select from a list of switches or dimmers and record their currents states as a Scene. It is suggested that you name the app during install something like 'Scene - Romantic Dinner' or 'Scene - Movie Time'. Switches can be added removed or set at new levels by editing and updating the app from the smartphone interface.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
) 
 
preferences
{
	section("Select lights ...") {
		input "switches", "capability.switch", multiple: true
	}

	section("trigger switch ...") {
		input "trigger", "capability.momentary", multiple: false, required: false
	}
}

def installed()
{
	log.debug "Installed with settings: ${settings}"
    
	subscribe(app, appTouch) 
    
    if (settings.trigger)
        subscribe(settings.trigger, "momentary", on_event)

    getDeviceSettings()
}

def updated()
{

	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    
    subscribe(app, appTouch)
    
    if (settings.trigger)
        subscribe(settings.trigger, "momentary", on_event)

    getDeviceSettings()
}

def on_event(evt)
{
	setScene()
}

def appTouch(evt)
{
	log.debug "appTouch: $evt"
	setScene()
}

private setScene()
{
	def i = 0
    
	for(myData in state.lastSwitchData)
    {
    	def dimmerValue = myData.dimmerValue
    	def switchName  = myData.switchName
    	def switchType  = myData.switchType
    	def switchState = myData.switchState
        def hueValue    = myData.hueValue
        def satValue    = myData.satValue
        
        if(myData.dimmerValue != "null")					//
   			dimmerValue = myData.dimmerValue.toInteger() 	//BF #1
        else												//
			dimmerValue = 0									//
        
        log.info "switchName: $switchName"
        log.info "switchType: $switchType"
        log.info "switchState: $switchState"
        log.info "dimmerValue: $dimmerValue"
        log.info "hueValue: $hueValue"
        log.info "satValue: $satValue"

		if(switchState == "on")
        	switches[i].on()
            
        if(dimmerValue > 0)
            switches[i].setLevel(dimmerValue)
        
        if(switchState == "off")
        	switches[i].off()
            
        switches[i].setHue(hueValue)
        switches[i].setSaturation(satValue)

        i++
        log.info "Device setting is Done-------------------"
    }
}

private getDeviceSettings()
{
    def cnt = 0
    for(myCounter in switches)
    {
    	switches[cnt].refresh() //this was a try to get dimmer values (bug)
    	cnt++
    }
    
    state.lastSwitchData = [cnt]
    
	def i 			= 0
    def switchName  = ""
    def switchType  = ""
    def switchState = ""
    def dimmerValue = ""
    def hueValue 	= ""
    def satValue 	= ""

	for(mySwitch in switches)
    {
        switchName  = mySwitch.device.toString()
        switchType  = mySwitch.name.toString()
        switchState = mySwitch.latestValue("switch").toString()
        dimmerValue = mySwitch.latestValue("level").toString()   
        hueValue    = mySwitch.latestValue("hue")
        satValue    = mySwitch.latestValue("saturation")
        
        state.lastSwitchData[i] = [switchName: switchName,
        						   switchType: switchType,
                                   switchState: switchState,
                                   dimmerValue: dimmerValue,
                                   hueValue: hueValue,
                                   satValue: satValue]

        log.debug "SwitchData: ${state.lastSwitchData[i]}"
        i++   
	}  
}