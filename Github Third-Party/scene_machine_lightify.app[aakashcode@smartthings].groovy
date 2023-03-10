/**
 *  App Name:   Scene Machine with Lightify ColorTemp
 *
 *  Author: 	Aakash
 *				
 *  Date: 		2015-03-21
 *  Version: 	0.1
 *  
 *  Updated:    2013-03-21
 *  
 *  Based on Todd Wackford's Scene Machine Orig version 1.1
 *  I added a getter/setter for the color temperature in Scott Gibson's Lighify Bulb v1
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


 * Use License: Non-Profit Open Software License version 3.0 (NPOSL-3.0)
 *              http://opensource.org/licenses/NPOSL-3.0
 */
// Automatically generated. Make future change here.
definition(
    name: "Scene Machine with Lightify Color Temperature",
    namespace: "aakashcode",
    author: "Aakash",
    description: "This app lets the user select from a list of switches or dimmers and record their currents states as a Scene, including recording and restoring Lightify color temperature. It is suggested that you name the app during install something like 'Scene - Romantic Dinner' or 'Scene - Movie Time'. Switches can be added removed or set at new levels by editing and updating the app from the smartphone interface.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
) 
 
preferences {
	section("Select switches ...") {
		input "switches", "capability.switch", multiple: true
	}
    
    //Uncomment this section below to test/change on the IDE. Smartphone does 
	//not need it.
	
    //section("Record New Scene?") {  	
	//	input "record", "enum", title: "New Scene...", multiple: false, 
	//         required: true, metadata:[values:['No','Yes']]
	//}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribe(app, appTouch) 
    getDeviceSettings()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    subscribe(app, appTouch)
    
   //if(record == "Yes") //uncomment this line to test/change stuff on the IDE
      getDeviceSettings()
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	setScene()
}

private setScene() {

	def i = 0
    def switchName  = ""
    def switchType  = ""
    def switchState = ""
    def dimmerValue = ""
    def tempValue = ""  //ADDED FOR COLORTEMP
    
	for(myData in state.lastSwitchData) {

    	switchName  = myData.switchName
    	switchType  = myData.switchType
    	switchState = myData.switchState
        if(myData.dimmerValue != "null")					//
   			dimmerValue = myData.dimmerValue.toInteger() 	//BF #1
        else												//
			dimmerValue = 0									//
        
        //ADDED FOR COLORTEMP
        if(myData.tempValue != "null")
                        tempValue = myData.tempValue.toInteger()
        else
                        tempValue = -1

        log.info "switchName: $switchName"
        log.info "switchType: $switchType"
        log.info "switchState: $switchState"
        log.info "dimmerValue: $dimmerValue"
        log.info "tempValue: $tempValue"  //ADDED FOR COLORTEMP

		if(switchState == "on")
        	switches[i].on()
            
        if(dimmerValue > 0)
            switches[i].setLevel(dimmerValue)
        
        if(switchState == "off")
        	switches[i].off()

        //ADDED FOR COLORTEMP
        if(tempValue >= 0)
            switches[i].setColorTemp(tempValue)  

        i++
        log.info "Device setting is Done-------------------"
    }
}

private getDeviceSettings() {

    def cnt = 0
    for(myCounter in switches) {
    	switches[cnt].refresh() //this was a try to get dimmer values (bug)
        //switches[cnt].poll()
    	cnt++
    }
    
    state.lastSwitchData = [cnt]
    
	def i = 0
    def switchName  = ""
    def switchType  = ""
    def switchState = ""
    def dimmerValue = ""
    def tempValue = ""  //ADDED FOR COLORTEMP

	for(mySwitch in switches) {
        switchName  = mySwitch.device.toString()
        switchType  = mySwitch.name.toString()
        switchState = mySwitch.latestValue("switch").toString()
		//dimmerValue below returns null if it is not a dimmer
        dimmerValue = mySwitch.latestValue("level").toString()
        tempValue = mySwitch.latestValue("colorTemp").toString()  //ADDED FOR COLORTEMP   
        
        state.lastSwitchData[i] = [switchName: switchName,
        						   switchType: switchType,
                                   switchState: switchState,
                                   dimmerValue: dimmerValue,
                                   tempValue: tempValue]  //ADDED FOR COLORTEMP

        log.debug "SwitchData: ${state.lastSwitchData[i]}"
        i++   
	}  
}
