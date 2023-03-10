/** FLEXi Triggers NEW
	
    
	Version 1.4 (2015-11-15)
 
   The latest version of this file can be found at:
   https://github.com/infofiend/FLEXi_Lighting/FLEXi_Triggers
 
 
   --------------------------------------------------------------------------
 
   Copyright (c) 2015 Anthony Pastor
 
   This program is free software: you can redistribute it and/or modify it
   under the terms of the GNU General Public License as published by the Free
   Software Foundation, either version 3 of the License, or (at your option)
   any later version.
 
   This program is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
   or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
   for more details.
 
   You should have received a copy of the GNU General Public License along
   with this program.  If not, see <http://www.gnu.org/licenses/>.
   
 **/

definition(
    name: "FLEXi Triggers NEW",
    namespace: "info_fiend",
    author: "Anthony Pastor",
   description: "ChildApp to FLEXi Lighting Scenes. " + 
                 "         ---------------------------------------------------------------       " +
                 "Enter MODE(s), then Presence(s), then Motion and/or Contact trigger(s), then        " +
                 "light(s).  Once a triggering event occurs, the Lighting Scene from Parent      " +
                 "App will be applied. " ,
    category: "Convenience",
    parent: "info_fiend:FLEXi Lighting Scenes NEW",
	iconUrl: "https://dl.dropboxusercontent.com/u/2403292/Lightbulb.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2403292/Lightbulb%20large.png")

preferences {
    page name:"pageSettings"
    page name:"pageOptions"    
}


private def	pageSettings() {

    TRACE("pageSettings()")

	def parentModes = []
    parentModes = parent.sendChildTheModes()
    log.debug "Parent Modes = ${parentModes}"
    
	def textSettings =	
                 "Enter (1) the MODE(s), (2) the Presence Sensors, (3) the Motion and/or Contact " +
                 "trigger(s), and finally (4) the Light(s).             " +
                 "         ---------------------------------------------------------------       " +
                 "Once a triggering event occurs, the Lighting Scene from the ParentApp will be applied." +
				 "         ---------------------------------------------------------------       "  +                
                 "Next Page will provide additional options."


	def pageProperties = [
        name        : "pageSettings",
        title       : "Choose Settings",
        nextPage    : "pageOptions",
        install     : false,
        uninstall   : true
	]

	def inputModes = [
        name        : "theModes",
        type        : "mode",
        title       : "MODES:",
        multiple:   true,
        required:   true, 
        submitOnChange: true 
    ]
   

	def inputHues = [
        name        : "hues",
        type        : "device.flexihueBulb",
        title       : "Select FLEXiHue Bulbs(s):",
        multiple:   true,
        required:   false
    ]
    
    def inputDimmers = [
        name        : "dimmers",
        type        : "device.flexidimmer",
        title       : "Select FLEXiDimmer Lights(s):",
        multiple:   true,
        required:   false
    ]
    
    def inputSwitches = [
        name        : "switches",
        type        : "capability.switch",
        title       : "Select any other On/Off Switches:",
        multiple:   true,
        required:   false
    ]
        
     return dynamicPage(pageProperties) {
		section("Child Trigger App Options:  MODEs, Sensors, and Lights", hideable:true, hidden: state.installed) {
    		 paragraph textSettings 
        }     
		
        section("1. Select the MODES that this Child Trigger App should use to control Lighting Scenes.") {
     
        	def m = location.mode
			def myModes = []
			parentModes.each {myModes << "$it"}
            input "xModes", "enum", multiple: true, title: "Select mode(s)", submitOnChange: true, options: myModes.sort(), required: true 
			//  defaultValue: [m],    
        }    

		if ( xModes ) {
        
			section("2. Select Presence Sensors: ALL need to be home for triggers to fire:") {
    	        input "presence", "capability.presenceSensor", title: "Presence Sensors:", multiple:   true, required: true, submitOnChange: true

	        }
 			
            if ( presence ) {
            
	    	    section("3. Select Motion Sensors to trigger lights:") {
    	    	    input "motions", "capability.motionSensor", title: "Select motion detector(s):", multiple: true, required: false, submitOnChange: true 
 				}   
        
    	    	section("4. Select Contact Sensors to trigger lights:") {
        	    	input "contacts", "capability.contactSensor", title: "Select contact detector(s):", multiple: true, required: false, submitOnChange: true 
		        }

	            if (motions || contacts) {				
	    	    	section("5. Select Lights Triggered by the Above:") {
    	    	    	input inputHues
	    	        	input inputDimmers
		    	        input inputSwitches
    		    	}
        		}
            }
        }
    }

}

private def	pageOptions() {

    TRACE("pageOptions()")
 	def defAppName = "Trigger for (" 
    xModes.each {
    	defAppName = defAppName + "$it" + " "
    }    
	defAppName = defAppName + ")"
    
 	log.debug "defAppName is ${defAppName}."
    
	def pageProperties = [
        name        : "pageOptions",
        title       : "Config Page 2: Child App Name & Default Options",
        nextPage    : null,
        install     : true,
        uninstall   : state.installed
	]


	def inputDefLevel = [
        name        : "defLevel",
        type        : "number",
        title       : "Default Hue / Dimmer Level:",
        required:   true,
        defaultValue: 	99        
    ]
    
    def inputDefColor = [
        name        : "defColor",
        type        : "enum",
        title       : "Default Hue Color (in case Trigger cannot find any other):",
        multiple:   false,
        required:   true,
        metadata: [values:
					["Warm", "Soft", "Normal", "Daylight", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]], 
        defaultValue: "Warm"
    ]
    
    def inputDefSwitch = [
        name        : "defSwitch",
        type        : "enum",
  	    title       : "Default Switch State:",
        required:true, 
        metadata: [values: ["yes", "no"]], 
        defaultValue: "no",
        multiple:   false
    ]
    
   	def inputDefOffTime = [
        name        : "defOffTime",
        type        : "number",
        title       : "Default amount of time to turn off lights after motion/contact event (minutes):",
        required:   true,
        defaultValue: 	30        
    ]

    return dynamicPage(pageProperties) {   
    	
       
        section([title:"1. Child App Label", mobileOnly:true]) {
           	label title:"Assign a Name", required:true, defaultValue: "${defAppName}", submitOnChange: true
        }               
        
        section("2. (Optional) defaults", hideable:true, hidden: true) {
            input inputDefLevel
            input inputDefColor
            input inputDefSwitch
            input inputDefOffTime
        }
        

    }

}

    

def installed()
{
	
	initialize()

}


def updated()
{
	unschedule()
	unsubscribe()
	initialize()
    

}


def initialize()
{
	state.abortTime = null
    state.abortWindow = null
	state.inactiveAt = null
	state.allGroup = []   
	state.offTime = defOffTime
    
    state.theParentModes = parent.sendChildTheModes()   
	log.debug "the Parent Modes are ${state.theParentModes}."
	state.theChildModes = xModes
	log.debug "the Child Modes are ${state.theChildModes}."
    
    
    subscribe(people, "presence", checkHome)
    
    if (hues) {
    	subscribe(hues, "switch.on", levelCheck) 
    }
    
    if (motions) {
    	subscribe(motions, "motion", motionHandler)
    }
    if (contacts) {
    	subscribe(contacts, "contact", contactHandler)
    }


	subscribe(location, onLocation)

	colorCheck()
	setActiveAndSchedule()     // schedule ("0 * * * * ?", "scheduleCheck")

}

def checkOff() {   	// Wictor Wictor Niner
	def theCurMode = location.mode
	def modeOffTime = parent.sendChildOffInfo(theCurMode) as Number
    log.debug "checkOFF:  Parent Lighting App returned offTime of ${modeOffTime} for mode ${theCurMode}."

    return modeOffTime  

}

def checkHome() {

	def result = false
    
   	if (allPeopleHome() ) {
    
        result = true
        log.debug "allHome is true"
	}
	return result
}      


def allPeopleHome() {

	def result = true
	for (person in people) {
		if (person.currentPresence == "not present") {
			result = false
			break
		}
	}
	log.debug "allPeopleHome: $result"
	return result
}

def colorCheck() {

	def valueColor = defColor as String
    def newHue = 23
    def newSat = 56
    
	switch(valueColor) {
				
		case "Normal":
			newHue = 52
			newSat = 19
			break;
						
		case "Daylight":
			newHue = 53
			newSat = 91
			break;
                            
		case "Soft":
			newHue = 23
			newSat = 56
			break;
        	                
		case "Warm":
			newHue = 20
			newSat = 80 //83
			break;
    	                    
		case "Blue":
			newHue =  70
			newSat = 100
           	break;
                        
		case "Green":
			newHue = 39
    	    newSat = 100
			break;
                        
		case "Yellow":
        	newHue = 25
			newSat = 100			
    	   	break;
        	                
		case "Orange":
			newHue  = 10
			newSat = 100
			break;
                        
		case "Purple":
			newHue = 75
			newSat = 100
	        break;
                        
		case "Pink":
			newHue = 83
			newSat = 100
		    break;
                        
		case "Red":
			newHue = 100
			newSat = 100                       
			break;
                        
	}

	state.defHue = newHue
    state.defSat = newSat
    
}

def levelCheck(evt) {
	
//   	log.debug "Reached levelCheck.  "
    
    def phyTest = evt.isPhysical()
	if ( phyTest ) {
		hues?.each { 
//    		it.poll()
//	    	def curLevel = it.currentValue("level")
//    		log.debug "Light ${it.label} is level ${curLevel}."
//        	if (curLevel == "100") {	      	
    		log.debug "Detected manual switch used - adjusting ${it.label} to current Scene settings."
			
	        def theLight = hues.find{it.id == evt.deviceId} 
    	    log.trace "LevelCheck: The switch for ${theLight} was physically turned on."
        
			pause(1000)
        turnON()
    	    state.lastCheck = now()
        }
    }    
}        
        

def turnON() {							// YEAH, baby!
 
	def theMode = location.mode as String // state.currentMode as String 
    def masterName = null    
    def slaveNames = null  
    def freeNames = null    

    masterName = parent.sendChildLightsByType("hueMaster") as String  		// hues.find{it.currentValue("sceneSwitch") == "Master"}
      	log.debug "masterName is ${masterName}."        
        
    def totalMSLights = []
    totalMSLights << masterName

	def masterData = parent.sendChildMasterInfo(masterName, theMode) 
    log.debug "${app.label}: turnON : masterData is ${masterData}."    
    
    slaveNames = parent.sendChildLightsByType("hueSlaves") as String  	
	log.debug "slaveNames is/are ${slaveNames}."            
        
	if (slaveNames) {
    
    	def slaveLights = []
            slaveNames?.each { slaveLights << "$it" }

        totalMSLights << slaveLights
        
    }

    def msLevel = null                         
    def msHue = null
	def msSat = null
                      
    msLevel = masterData.level 
    log.debug "${app.label}: turnON : masterData.level is ${msLevel}."                        
                       
					//            log.debug "${it.label} sceneSwitch is ${myScene}."                  
	msHue = masterData.hue 
    log.debug "${app.label}: turnON : masterData.hue is ${msHue}."                                        
	msSat = masterData.saturation 
	log.debug "${app.label}: turnON : masterData.saturation is ${msSat}."                                        

	def newValueColor = [hue: msHue, saturation: msSat, level: msLevel, transitiontime: 2, switch: "on"]
    masterName.setColor(newValueColor)
//    slaveNames.each?
    
	slaveNames?.each {
    
		log.debug "${it.displayName} is using ${masterName}'s settings of ${[newValueColor]}."                        
  	    it.setColor(newValueColor)      
	}
    

	freeNames = parent.sendChildLightsByType("hueFrees") as String  	
	    log.debug "freeNames is/are ${freeNames}." 
            
	if (freeNames) {
    
		def freeData = []
        
    	freeNames?.each {       

			freeData = parent.sendChildFreeInfo("$it", theMode)
                
            freeLevel = freeData.level as Number             
   			freeHue = freeData.hue as Number
			freeSat = freeData.saturation as Number
            
            newValueColor = [hue: freeHue, saturation: freeSat, level: freeLevel, transitiontime: 2, switch: "on"]
            it.setColor(newValueColor)
        }        
    }
   
/**		}
			} else if ( myScene == "Manual" ) {

				myHueLevel = it.currentValue("level")   		   			

           	    scnHue = it.currentValue("hue")    
				scnSat = it.currentValue("saturation")

				scnType = "Manual"



            } else { 
				myHueLevel = defLevel as Number 

  	            scnHue = state.defHue as Number    
				scnSat = state.defSat as Number    

   	        	if (myHueLevel > 99) {
       	       		myHueLevel = 99
           	    }    

				scnType = "Default"		            
            }    
            
          	def newValueColor = [hue: scnHue, saturation: scnSat, level: myHueLevel, transitiontime: 2, switch: "on"]
                log.debug "${it.label} is using ${scnType} settings of ${newValueColor}."                        
  	        it.setColor(newValueColor)                    

		}
    }

**/

    if (dimmers) {
            
       	dimmers?.each {
			def myDimLevel = null        
            def myDimType = null
                                        
	        myScene = it.currentValue("sceneSwitch")
            log.debug "${it.label} sceneSwitch is ${myScene}."
                
			if ( myScene == "Master" || myScene == "Slave" ) {
	
 	            myDimLevel = parent.sendChildDimLevel(it.displayName, theMode) 
				myDimType = "Master"              

			} else if ( myScene == "Freebie") {
            					
                myDimLevel = it.currentValue("sceneLevel") 
				myDimType = "Free"

			} else if ( myScene == "Manual" ) {
            
				myDimLevel = it.currentValue("level")   		   			
				myDimType = "Manual"

            } else { 
            	
				myDimLevel = defLevel as Number 
				myDimType = "Default"
                
	            if (myDimLevel > 99) {
    	          	myDimLevel = 99
        	    }    
                
			}
			
			log.debug "${it.label} is using ${myDimType} settings - myDimLevel is ${myDimLevel}."                        
	        it.setLevel(myDimLevel)		            
            
		}
    }

	if (switches) {
    	
  		switches?.each {
    		
            if (it.currentValue("switch") == "off") {
	            def theSwitchName = it.displayName as String
        	    theSwitchName = theSwitchName.tr(' !+', '___')
            	log.debug "${app.label}: the switch ID is ${it.id} and its name is ${theSwitchName}."
            
				def theSwitchState = parent.sendChildSwitchState(theSwitchName, theMode)
    	        log.debug "${app.label} retrieved ${theSwitchState} for ${theSwitchName}."
            
        	    if (theSwitchState == "yes") {
      				it.on()
		      	}    
			}
        }
	}   
}

// Handle motion event.
def motionHandler(evt) {

	def currentMode = location.mode
        
    
   	log.trace "${app.label}'s selected modes are ${xModes}.  Current mode is ${currentMode}." 

    if ( xModes.contains(currentMode) ) {
    	log.trace "onMotion:  Current mode of ${currentMode} is within ${app.label}'s selected modes}." 
        
		def theSensor = motions.find{it.id == evt.deviceId} // evt.deviceId
	
		if (checkHome) {

	    	if (evt.value == "active") {
				log.trace "${theSensor.label} detected motion - resetting state.inactiveAt to null & calling turnON()."
				state.inactiveAt = null
        	    turnON()   
		  		state.abortWindow = null
            
	        } else if (evt.value == "inactive") {
				log.trace "${theSensor.label} detected NO motion." 
				state.inactiveAt = now()
            	log.trace "- setting state.inactiveAt to ${now}."    
            
	       	  	if (state.timeOfAbort) {
   		            log.trace "...but abort active."    
        	    } else {
   	        	    log.trace "....and running setActiveAndSchedule."    
            		setActiveAndSchedule()
	            }
    	    }       			   
        
		} else {
    
			log.trace "Motion event, but checkHome is not true."
	    }   
        
	} else {        
    
    	log.trace "onMotion:  Current mode of ${currentMode} is NOT within ${app.label}'s selected modes}." 
	
    }        
}

// Handle contact event.
def contactHandler(evt) {
	def currentMode = location.mode
   	log.trace "${app.label}'s selected modes are ${xModes}.  Current mode is ${currentMode}." 

    if ( xModes.contains(currentMode) ) {
    	log.trace "onContact:  Current mode of ${currentMode} is within ${app.label}'s selected modes}." 

		def theSensor = motions.find{it.id == evt.deviceId}
	
		if (checkHome) {
	    	if (evt.value == "open") {
				log.trace "${theSensor.label} opened -- resetting state.inactiveAt to null & calling turnON()."
				state.inactiveAt = null
				turnON()   
			  	state.abortWindow = null
            
	    	} else {
				log.trace "${theSensor.label} closed -- setting state.inactiveAt to ${now()}."        
					// When contact closes, reset the timer if not already set
 
 				state.inactiveAt = now()
        		setActiveAndSchedule()
	        }  

		} else {
    
			log.debug "Contact event, but checkHome is not true."

		}    
} else {        
    
    	log.trace "onContact:  Current mode of ${currentMode} is NOT within ${app.label}'s selected modes}." 
	
    }   
}


// Handle location event.
def onLocation(evt) {
    
    def currentMode = evt.value
   	log.trace "${app.label}'s selected modes are ${xModes}.  Current mode is ${currentMode}." 

    if ( xModes.contains(currentMode) ) {
    
    	log.trace "onLocation:  Current mode of ${currentMode} is within ${app.label}'s selected modes}." 
    	pause(500)
    	
    	state.lastMode = state.currentMode

		state.currentMode = currentMode
        
	    state.theOffTime = checkOff()
		
        state.inactiveAt = now()      
        
		log.debug "Mode offTime is ${state.theOffTime}."
	    log.trace "NEW MODE: state.inactiveAt = ${state.inactiveAt} & calling setActiveAndSchedule()."    
    	setActiveAndSchedule() 

	} else {
    
       	log.trace "onLocation:  Current mode of ${currentMode} is NOT within ${app.label}'s selected modes}." 
    
    }
}


def setActiveAndSchedule() {
    unschedule("scheduleCheck")
    
    def myOffTime = checkOff() as Number
    state.myOffTime = myOffTime
    log.debug "setActiveAndSchedule:  checkOff() sent ${myOffTime} as the offTime mins."
    def mySchTime = myOffTime * 15		// check monitored lights every 1/4 of offTime limit (in seconds)
    log.debug "setActiveAndSchedule:  mySchTime (myOffTime * 15) is ${mySchTime}."
    if (mySchTime < 120) {				// BUT check no more than every 2 minutes	    
            mySchTime = 120
    }    
    
    log.debug "setActiveAndSchedule: running scheduleCheck in ${mySchTime} seconds."    
	runIn (mySchTime, "scheduleCheck")   
}

def scheduleCheck() {
    log.debug "scheduleCheck:  "
    if(state.inactiveAt != null) {

        def minutesOff = state.myOffTime as Number 
        log.debug "scheduleCheck:  Mode offTime is ${minutesOff}."
	    def elapsed = now() - state.inactiveAt
        def threshold = 60000 * minutesOff 
        log.debug "scheduleCheck: elapsed = ${elapsed} / threshold = ${threshold}."		

        if (elapsed >= threshold) {                     
            log.debug "scheduleCheck: elapsed > threshold.  Running turningOff."
            
  	    	turningOff()
        
            
        } else {
        
            setActiveAndSchedule()
        
        }
        
    } else {
    	log.debug "scheduleCheck:  state.inactiveAt is null, so setting it = now()."
    	state.inactiveAt = now()
        setActiveAndSchedule()
    
    }    
}

def turningOff () {

		state.abortWindow = null
        state.inactiveAt = null        
        
		log.trace "Executing turningOff() . "
 

			if (hues) {
	            hues?.each {
    	          	it.off()
	    	    }
            }
            if (dimmers) {
	            dimmers?.each {
    	        	it.off()
        	    }    
	        }
            if (switches) {
	            switches?.each {
    	        	it.off()
        	    }    
	        }


}


private def TRACE(message) {
    log.debug message
}

private def STATE() {
    log.trace "settings: ${settings}"
    log.trace "state: ${state}"
}