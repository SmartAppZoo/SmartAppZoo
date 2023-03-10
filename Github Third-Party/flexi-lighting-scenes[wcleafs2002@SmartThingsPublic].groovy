/**
 * FLEX LIGHTING SCENES 
 *
 *
 *
 *
*  Version 1.4 (2015-11-9)
 *
 *  The latest version of this file can be found at:
 *  https://github.com/infofiend/FLEXi_Lighting/blob/master/FLEXi_Scenes
 * 
 * 	or at
 *
 *  https://github.com/infofiend/SmartThings/blob/master/smartapps/info-fiend/flexi-lighting-scenes.groovy
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 Anthony Pastor
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

definition(
    name: "FLEXi Lighting Scenes",
    namespace: "info_fiend",
    author: "Anthony Pastor",
    description: "FLEXI Lighting Scenes App:  Set-up & control lighting scenes with " +
    			 "Hues (use FlexiHue device type), Dimmers (use FlexiDimmer device type), " +
                 "and/or Switches.  For use with accompanying FLEXI Trigger App.  Find these" +
                 "devices and smartapps at https://github.com/infofiend/FLEXI_Lighting ", 
    category: "Convenience",
    
	iconUrl: "https://dl.dropboxusercontent.com/u/2403292/LR%20Scene.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2403292/LR%20Scene%20large.png",
    oauth: true
)

preferences {
    page name:"pageSelect"
    page name:"pageAbout"
    page name:"pageSettings"
	page name:"pageChildApps"
    page name:"pageOptions"    
}

private def	pageChildApps() {

    TRACE("pageChildApps()")
    
    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
            return pageAbout()
    }

	def pageProperties = [
        name        : "pageChildApps",
        title       : "Config Page 3: Customize / Edit The Triggers Used.    " +
        			  "Click the 'Customize / Select Events' button to create a Child " +
                      "SmartApp - you can then choose the motion or contact events that " +
                      "cause the lights to turn on or off.  Each of the Triggers can be " +
                      "limited so they apply in different times.  For instance, you create " +
                      "a first Trigger child app for 'Daytime' mode and a second Trigger " +
                      "child app for the 'Nighttime' mode until 11:30.",
                      
        nextPage    : "pageOptions",
        install     : false,
        uninstall   : state.installed
	]
    
    return dynamicPage(pageProperties) {

		section {
			app(name: "childApps", appName: "FLEXi Triggers", namespace: "info_fiend", title: "Customize / Select Triggers:", multiple: true)
	    }
	}
}


// Show "Selection of Scenes and Devices" preference page
private def pageSelect() {
    TRACE("pageSelect()")
    
    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
        return pageAbout()
    }

	def textSelect =	
        "1: Select the Scenes (Modes) that you want this app to use,             " +
        "2: Select a 'Master' Hue Light,                                              " +
        "3: (optional) Select 'Slave' Hues (that will use the color and " +
        "level settings of the 'Master' Hue) and/or any 'Free' Hues (that will " +
        "have their own settings),       " +
        "4: (optional) Select 'Slave' Dimmers (that will follow the color and " +
        "level settings of the 'Master' Hue) and/or any 'Free' Dimmers (that will " +
        "have their own color and level settings), and                                                " +
        "5: (optional) Select any non-FLEXi Switches (functionality limited to simple on/off)."

    def pageProperties = [
        name        : "pageSelect",
        title       : "Config Page 1: Select Scenes and Devices",
        nextPage    : "pageSettings",
        install     : false,
            uninstall   : state.installed
    ]
    
	def inputModes = [
        name        : "theModes",
        type        : "mode",
        title       : "Select the MODES (set up manually) that you want to use to control Lighting Scenes.  " +
        				"IMPORTANT: Selecting 'All Modes' will cause this app to not work properly.",
        multiple:   true,
        required:   true
    ]
    
    def inputHueMaster = [
        name        : "hueMaster",
        type        : "device.flexihueBulb",
        title       : "Select a 'Master' Hue light (required):",
        multiple:   false,
        required:   true
    ]

    def inputHueSlaves = [
        name        : "hueSlaves",
        type        : "device.flexihueBulb",
        title       : "Select 'Slave' Hue lights (optional):",
        multiple:   true,
        required:   false
    ]

    def inputHueFrees = [
        name        : "hueFrees",
        type        : "device.flexihueBulb",
        title       : "Select 'Free' Hue lights (optional):",
        multiple:   true,
        required:   false
    ]

    def inputDimSlaves = [
        name        : "dimSlaves",
        type        : "device.flexidimmer",
        title       : "Select Slave Dimmers (optional):",
        multiple:   true,
        required:   false
    ]

    def inputDimFrees = [
        name        : "dimFrees",
        type        : "device.flexidimmer",
        title       : "Select Free Dimmers (optional):",
        multiple:   true,
        required:   false
    ]
    
    def inputSwitches = [
        name        : "switches",
        type        : "capability.switch",
        title       : "Select non-FLEXi Switches (optional):",
        multiple:   true,
        required:   false
    ]

    return dynamicPage(pageProperties) {
		section("Selection of Scenes and Devices", hideable:true, hidden: state.installed) {
    		 paragraph textSelect 
        }     
    
		section("1: Scenes") {
            input inputModes
        }
        section("2: Master Hue") {
            input inputHueMaster
        }
        section("3: Slave / Free Hues") {
            input inputHueSlaves
            input inputHueFrees
        }
        section("4: Slave / Free Dimmers") {
            input inputDimSlaves
            input inputDimFrees
        }
        section("5: Switches") {        
            input inputSwitches
		}
        
    }
}

        

// Show "About" page
private def pageAbout() {
    TRACE("pageAbout()")

    def textAbout =
        "FLEXI Lighting Scenes App:  Set-up & control of " +
    	"lighting scenes (a ST mode = a scene).  For use with " +
        "Hue Bulbs (use the FlexiHue (Connect) SmartApp to create " +
        "FlexiHue devices), Dimmers (use a FlexiDimmer device " +
        "type), and/or Switches (use any). " +
		"        -------------------------------------                                        " +
		"You can find the code for the FLEXI devices and the FLEXI Trigger SmartApp at "+
        "https://github.com/infofiend/FLEXI_Lighting   ." 

    def pageProperties = [
        name        : "pageAbout",
        title       : "About FLEXi Lighting",
        nextPage    : getNextPage(),
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section("Summary") {
            paragraph textAbout            
        }
        section("Copyright") {
	        paragraph "${textVersion()}\n${textCopyright()}"
        }    
        section("License") {
            paragraph textLicense()
        }
    }
}

def getNextPage() {

def thePage = "pageSelect"
    if (settings.hueMaster) {
    	thePage = "pageOptions"
    }
    thePage
}    


// Show "Scene Settings" preference page
private def pageSettings() {
    TRACE("pageSettings()")

    def textSettings =
        "For each Scene (Mode) you selected (prior page):                                       " +
        "    --------------------------------------------------------------------- " +        

        "1) Set the desired no-motion time limit;                      " +
        "2) Set the 'Master' settings (Color, Level, and Mode Trigger - see below)                                                      " +
        "3) Set the settings for any 'Free' Hues or 'Free' Dimmers; and                                                               " +
        "4) Set the settings for any Switches.                                     " +
        "    --------------------------------------------------------------------- " +        
        " -- LEVEL: Choose value between 0 (off) and 99 (default full brightness).                                                   " +
        " -- COLOR: Choose any color (default is 'Warm'.            " +        
        " -- SCENE TRIGGER: If you want a Scene change to turn on lights that were " +
        "off at time of change, then select 'yes'. If you select 'no', " +
        "any lights that were already on will immediately change to correct level & color, while any " +
        "lights that were off will just save the correct level & color."

    def pageProperties = [
        name        : "pageSettings",
        title       : "Scene Settings for Master Hue and any Free Hues, Free Dimmers, and Switches.",
        nextPage    : "pageChildApps",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section("Setting up your scenes", hideable:true, hidden: state.installed) {
            paragraph textSettings
        }
	//	def theModes = sendTheChildModes()
        
        theModes?.each() {
    	        def name = it as String
        	    section("${name} Scene:", hideable:true, hidden: state.installed) {
            	    name = name.tr(' !+', '___')
                
					input "${name}_offTime", "number", title: "Lights should turn OFF after how many minutes without motion in this scene?", required: true, defaultValue: 60
                    if (hueSlaves || dimSlaves) {
                    	input "${name}_slavePercent", "number", title: "What percentage of Master level should any Slave lights use in this scene?", required: true, defaultValue: 100 
                	}
                    
	                settings.hueMaster?.each() {
                    	paragraph "Master Hue ${it.displayName}"
    	                input "${it.displayName}_${name}_HMlevel", "number", title: "Level:", required:true, defaultValue: 99
        	            input "${it.displayName}_${name}_HMcolor", "enum", title: "Color:", required: true, multiple:false, metadata: [values:
						["Warm", "Soft", "Normal", "Daylight", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]], defaultValue: "Warm"
                	    input "${it.displayName}_${name}_HMtrig", "enum", title: "If above MODE is selected, should lights turn on?",
                    	    metadata:[values: ["yes", "no"]], required:true, defaultValue: "no"
	                }
                    
    	            settings.hueFrees?.each() {
                    	paragraph "Free Hue ${it.displayName}"
        	            input "${it.displayName}_${name}_HFlevel", "number", title: "Level:", required:true, defaultValue: 99
            	        input "${it.displayName}_${name}_HFcolor", "enum", title: "Color:", required: true, multiple:false, metadata: [values:
						["Warm", "Soft", "Normal", "Daylight", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]], defaultValue: "Warm"
                    	input "${it.displayName}_${name}_HFtrig", "enum", title: "If above MODE is selected, should lights turn on?",
                        	metadata:[values: ["yes", "no"]], required:true, defaultValue: "no"
	                }
    	            settings.dimFrees?.each() {
                    	paragraph "Free Dimmer ${it.displayName}"                    
        	            input "${it.displayName}_${name}_Dlevel", "number", title: "Level:", required:false
            	        input "${it.displayName}_${name}_Dtrig", "enum", title: "If above MODE is selected, should lights turn on?",
                	        metadata:[values: ["yes", "no"]], required:true, defaultValue: "no"                    
	                }
    	            settings.switches?.each() {
                    	paragraph "Switch ${it.displayName}"    
                        def swDispName = it.displayName as String
                        swDispName = swDispName.tr(' !+', '___')
        	            input "${swDispName}_${name}_Strig", "enum", title: "Switch On?",
            	            metadata:[values: ["yes", "no"]], required:true, defaultValue: "no"
  					
					}
                }
            
        }
    }
}

private def pageOptions() {
	TRACE("pageOptions()")
   
	def pageProperties = [
        name        : "pageOptions",
        title       : "Options.",
        nextPage    : null,
        install     : true,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {

        section([title:"Options", mobileOnly:true]) {
           	label title:"Assign a name", required:false
            
            icon(title: "Assign an icon", required: false)
        }

		section {
            href "pageAbout", title:"About Flexi Lighting", description:"Tap to open"
        }
	}
}

def installed() {
	state.installed = true

    initialize()
    TRACE("installed()") 	
}

def updated() {
	state.installed = true
    unsubscribe()
    initialize()
    TRACE("updated()")
    
}

private def initialize() {
    log.trace "${app.name}. ${textVersion()}. ${textCopyright()}"

	log.debug "CHILD APPS: there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }

	state.defaultColor = [hue: 20, saturation: 80, level: 99, transitionTime: 2]
    state.defaultHue = 20
	state.defaultSat = 80
    state.defaultLevel = 99
    
    subscribe(location, onLocation)

   	onLocation(location.mode)
    
    state.uninstalled = false
	STATE()

}

def sendChildTheModes() {

	def childModes = theModes
    return childModes

}

def sendChildLightsByType (String inType) {

	String theType = inType
    def theLights = []

    settings.${inType}.each() {theLights << "$it"}
    
    return theLights
    
	log.debug "${app.label}:  The ${theType} Light(s) is/are ${theLights}."
}

def sendChildSwitchState(String incomingName, String inMode) {
	log.debug "${app.label}: sendChildSwitchState received ${incomingName}."
    
    String mode = inMode
	  
    def name = "${incomingName}_${mode.tr(' !+', '___')}"	
    def nameTrig = name +"_Strig" as String                
	def valueTrig = settings[nameTrig]

	log.debug "${app.label} has a value of ${valueTrig} for ${incomingName} during mode ${inMode}."
    
	return valueTrig 
    
}

def sendChildDimLevel (String incomingName, String inMode) {
	log.debug "${app.label}: sendChildDimLevel received ${incomingName}."
	
    String mode = inMode

    def name = "${incomingName}_${mode.tr(' !+', '___')}"	
    def nameLevel = name +"_Dtrig" as String                
	def valueLevel = settings[nameLevel]
    
   	log.debug "${app.label} has a value of ${valueLevel} for ${incomingName} during mode ${inMode}."
    
    return valueLevel

}

def sendChildOffInfo (String inMode) {
	log.debug "${app.label}: sendChildOffLevel received ${inMode}."

	String theMode = inMode.tr(' !+', '___')
    
	def sendOffTime = theMode + "_offTime"
    def sendOffValue = settings[sendOffTime] as Number

   	log.debug "${app.label} has an offTime of ${sendOffValue} for mode ${inMode}."

	return sendOffValue
    
}


def sendChildMasterInfo (String incomingName, inMode) {
	log.debug "${app.label}: sendChildMasterLevel received ${incomingName} and ${inMode}."
	
    String mode = inMode

    
    def name = "${incomingName}_${mode.tr(' !+', '___')}"	

	def nameLevel = name +"_HMlevel" as String                

	def valueLevel = settings[nameLevel]

    def newValueLevel = valueLevel 
	if (newValueLevel > 99) {
        newValueLevel = 99             
	}

    
	def nameColor = name +"_HMcolor" as String

   	def valueColor = settings[nameColor]   		

	def newHue = state.defaultHue 

 	def	newSat = state.defaultSat 

        
	if (valueColor != null) {            

		colorCheck(valueColor)
        newHue = state.theColorHue
        newSat = state.theColorSat

	}

	def newValueColor = [hue: newHue, saturation: newSat, level: newValueLevel, transitiontime: 2, switch: on]

	return newValueColor
}

def sendChildFreeInfo (String incomingName, inMode) {
	log.debug "${app.label}: sendChildFreeLevel received ${incomingName}."
	
    String mode = inMode

    def name = "${incomingName}_${mode.tr(' !+', '___')}"	
        def nameLevel = name +"_HFlevel" as String                
	def valueLevel = settings[nameLevel]
      
    def newValueLevel = valueLevel 
	if (newValueLevel > 99) {
        newValueLevel = 99             
	}
    
	def nameColor = name +"_HFcolor" as String
   	def valueColor = settings[nameColor]   		
	def newHue = state.defaultHue 
 	def	newSat = state.defaultSat 
                
	if (valueColor != null) {            

		colorCheck(valueColor)
        newHue = state.theColorHue
        newSat = state.theColorSat
            			                   
	}

	def newValueColor = [hue: newHue, saturation: newSat, level: newValueLevel, transitiontime: 2, switch: on]
	log.debug "${app.label} has a value of ${newValueColor} for ${incomingName} during mode ${inMode}."

	return newValueColor
}

def masterSlave(inMode) {

// Set Master     
	hueMaster.setScSwitch("Master")
    
// Set Slaves
    if (hueSlaves) {
		hueSlaves?.each {
    	    it.setScSwitch("Slave")
	    }
    }

	if (dimSlaves) {    
	    dimSlaves?.each {
    		it.setScSwitch("Slave")
    	}
    }    
    
    
// Set Frees

    if (hueFrees) {
		hueFrees?.each {
    	    it.setScSwitch("Freebie")
	   	}
	}
    
    if (dimFrees) {    
		dimFrees?.each {
    	    it.setScSwitch("Freebie")
	   	}
	}

}

def setTheHueGroup(inMode) {


	String mode = inMode

	def name = "${hueMaster.displayName}_${mode.tr(' !+', '___')}" as String

	def nameTrig = name +"_HMtrig" as String                
    def valueTrig = settings[nameTrig]
	

// Determine settings from Master 
             
	def nameLevel = name +"_HMlevel" as String                
   	def valueLevel = settings[nameLevel] as Integer
    log.trace "nameLevel is ${nameLevel} and valueLevel is ${valueLevel}."

    def newValueLevel = valueLevel 
	if (newValueLevel > 99) {
        newValueLevel = 99             
	}		                
    
	log.trace "newValueLevel is ${newValueLevel}."		
	def nameColor = name +"_HMcolor" as String
   	def valueColor = settings[nameColor]   		
	def newHue = state.defaultHue 
 	def	newSat = state.defaultSat 
                
	if (valueColor != null) {            

		colorCheck(valueColor)
        newHue = state.theColorHue
        newSat = state.theColorSat
             			                   
	}



// Set & Save Scene Settings 
	            
	// Master                
	def newValueColor = [hue: newHue, saturation: newSat, level: newValueLevel, transitiontime: 2]
	def offTimeName = mode +"_offTime" as String
    def offTime = state.offTime
	log.debug "saving scene - newValueColor is ${newValueColor}."
	hueMaster.saveScene(newValueColor, mode, offTime)
    
    log.debug "Master ${hueMaster.label} newValueLevel is ${newValueLevel}, valueTrig is ${valueTrig}, and is currently ${hueMaster.currentValue('switch')}."
    if (hueMaster.currentValue("level") > 0 ) {
    
	    log.debug "Past level > 0, so setting ${hueMaster.label} to ${newValueColor}."
        hueMaster.setColor(newValueColor)
        
    }
    
    if ( valueTrig == "yes" ) { 
    
		newValueColor.switch == "on"
        hueMaster.setColor(newValueColor)
    	log.debug "valueTrig set to 'Yes', so setting ${hueMaster.label} to ${newValueColor}."

	} 
    

    // Slaves
    if (hueSlaves || dimSlaves) {
	   
    	def percName = mode.tr(' !+', '___') + "_slavePercent" as String
        log.debug "percName is ${percName}."
		def percValue = settings[percName] as Number
        log.debug "percValue is ${percValue}."        
        
        if (percValue > 100 || percValue <= 0 ) {
        	percValue = 100
        }    
        log.debug "newValueLevel is ${newValueLevel}; percValue is ${percValue}."
        def percLevel1 =  ( (newValueLevel as Number) /100 ) 
        log.debug "percLevel1 is ${percLevel1}."
        def percLevel = percLevel1 * percValue 
//        log.debug "percLevel2 is ${percLevel2}."
        // percLevel = percLevel2.toInteger()
        log.debug "percLevel is ${percLevel}."
        
        def newColor = [hue: newHue, saturation: newSat, level: percLevel, transitiontime: 2]
		def newScene = [level: percLevel, transitiontime: 2]
        
        // Slave Hues
        if (hueSlaves) {
			log.debug "newColor is ${newColor}, mode is ${mode}, and offTime is ${offTime}."		        
			hueSlaves.saveScene(newColor, mode, offTime)

			hueSlaves.each {
			    log.debug "Slave ${it.label} percLevel is ${percLevel}, valueTrig is ${valueTrig}, and is currently ${it.currentValue('switch')}."            
			    if (it.currentValue("level") > 0 ) {
			      	it.setColor(newColor)
				    log.debug "Past Level > 0, so immediately setting ${it.label} to ${newColor}."

			    } else if ( valueTrig == "yes"  ) { 
			    	newColor.switch == "on"    
					it.setColor(newColor)
			    	log.debug "valueTrig set to 'Yes', so immediately setting ${it.label} to ${newColor}."
				} 
        	}
        }    
        

		// Slave Dimmers
		if (dimSlaves) { 

	        dimSlaves.saveScene(newScene, mode, offTime)
            
            dimSlaves.each {
            
			    log.debug "Slave ${it.label} percLevel is ${percLevel} and is currently ${it.currentValue('switch')}."                        
    	    	
                if (it.currentValue("level") > 0 ) {
					
                    dimSlaves.setLevel(percLevel)		            
			        log.debug "Past level > 0, so immediately setting Slave ${it.label} to ${newColor}."                    
				
                } else if ( valueTrig == "yes"  ) { 
			    	newColor.switch == "on"    
					it.setColor(newColor)
			    	log.debug "valueTrig set to 'Yes', so immediately setting Slave ${it.label} to ${newColor}."
				} 
                              	
        	}
        }    
    }    
}

def setTheFreeHues(inMode) {

	String mode = inMode
   
    def curFState = null
    
	hueFrees?.each() {

// Determine Free Hue Settings       	         
        
		def name = "${it.displayName}_${mode.tr(' !+', '___')}" as String

		def nameTrig = name +"_HFtrig" as String                
	    def valueTrig = settings[nameTrig]
		def nameLevel = name +"_HFlevel" as String  
		log.debug "nameLevel is ${nameLevel}."        
        
   		def valueLevel = settings[nameLevel] 
		log.debug "valueLevel is ${valueLevel}."
		
	    def nameColor = name +"_HFcolor" as String
		def valueColor = settings[nameColor]

	    def newValueLevel = valueLevel 
        newValueLevel = newValueLevel.toInteger()

        if (newValueLevel > 99) {
            newValueLevel = 99 
        }
   		log.debug "newValueLevel is ${newValueLevel}."
            
		def newHue = state.defaultHue 
 		def	newSat = state.defaultSat 
                
		if (valueColor != null) {            

			colorCheck(valueColor)
            newHue = state.theColorHue
            newSat = state.theColorSat
             			                   
		}    	
        

// Save Scene Settings 
                 
		def newValueColor = [hue: newHue, saturation: newSat, level: newValueLevel, transitiontime: 2]
	    def offTime = state.offTime
		log.debug "newValueColor is ${newValueColor}, mode is ${mode}, and offTime is ${offTime}."		        

        it.saveScene(newValueColor, mode, offTime)
	    log.debug "Free ${it.label} newValueLevel is ${newValueLevel}, valueTrig is ${valueTrig}, and is currently ${it.currentValue('switch')}."                                
        
	// -- and use those settings *IF* levelof light is currently > 0 or Scene Trigger is 'Yes'.	
        if (it.currentValue("level") > 0 ) {    
			it.setColor(newValueColor)	
           
	        log.debug "Past level > 0, so immediately setting Free ${it.label} to ${newValueColor}."                    
            
		} else if ( valueTrig == "yes" ) { 
	    	newValueColor.switch == "on"    
			it.setColor(newValueColor)
	    	log.debug "valueTrig set to 'Yes', so immediately setting Free ${it.label} to ${newValueColor}."

		} 
	}
}

def colorCheck(inColor) {

	def valueColor = inColor as String
	def newHue = state.defaultHue
    def newSat = state.defaultSat
    
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

	state.theColorHue = newHue
    state.theColorSat = newSat
    
}

def setTheFreeDimmers(inMode) {

	String mode = inMode
    
	dimFrees?.each() {

		def name = "${it.displayName}_${mode.tr(' !+', '___')}" as String

		def nameTrig = name +"_Dtrig" as String                
	    def valueTrig = settings[nameTrig]
        def nameLevel = name +"_Dlevel" as String  
       	def valueLevel = settings[nameLevel] 

	    def newValueLevel = valueLevel 
		newValueLevel = newValueLevel.toInteger()
        
        if (newValueLevel > 99) {
            newValueLevel = 99 
        }

// Save Scene Settings 
                 
		def newScene = [level: newValueLevel, transitiontime: 2]

	    def offTime = state.offTime

        it.saveScene(newScene, mode, offTime)
	    log.debug "Free ${it.label} newValueLevel is ${newValueLevel}, valueTrig is ${valueTrig}, and is currently ${it.currentValue('switch')}."
        
	// -- and use those settings *IF* levelof light is currently > 0 or Scene Trigger is 'Yes'.	
        
        if (it.currentValue("level") > 0 ) {
			
            it.setLevel(newValueLevel)		            
	        log.debug "Past level > 0, so immediately setting Free Dimmer ${it.label} to ${newValueLevel}."                    
            
		} else if ( valueTrig == "yes" ) {

			newValueLevel.switch == "on"    
			it.setLevel(newValueLevel)
	    	log.debug "valueTrig set to 'Yes', so immediately setting Free ${it.label} to ${newValueLevel}."        
        
        }
        
    }    
}
            

def setTheSwitches(inMode) {


	String mode = inMode
	switches.each() {

		def theSwitchName = it.displayName as String
    	theSwitchName = theSwitchName.tr(' !+', '___')
        
		def name = "${theSwitchName}_${mode.tr(' !+', '___')}"	
       	def nameTrig = name +"_Strig" as String                
	    def valueTrig = settings[nameTrig]
	    log.debug "Switch ${it.label}: nameTrig is ${nameTrig}, valueTrig is ${valueTrig}."

		if (valueTrig == "yes") {     
   	    	it.on()
	    } else {
        	it.off()
        }    
	}            
}

// Handle location event.
def onLocation(evt) {
    
	state.currentMode = evt.value
    def curMode = state.currentMode as String
    def lastMode = state.priorMode
    
    log.debug "Current mode is ${curMode}."
    
		if ( theModes.contains(curMode) ) {          
			          
            def offName = curMode.tr(' !+', '___') + "_offTime" as String 
            log.debug "offName is ${offName}."
			def offValue = settings[offName]
            log.debug "offValue is ${offValue}."            
            state.offTime = offValue
		    log.debug "${curMode} offTime is ${offValue}."            
            
			masterSlave(curMode)
        
    		if (settings.hueMaster) {
				setTheHueGroup(curMode)
	        }
        
    	    pause(300)
        
        	if (settings.hueFrees) {
				setTheFreeHues(curMode)
	        }
                
    	    if (settings.dimFrees) {
        	   setTheFreeDimmers(curMode)
	        }    
                
    	    if (settings.switches) {
				setTheSwitches(curMode)
			}
        
			state.priorMode = evt.value as String
    
		}
//	}
}    


private def textVersion() {
    def text = "Version 1.4"
}

private def textCopyright() {
    def text = "Copyright (c) October 2015 Anthony Pastor"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def TRACE(message) {
    log.debug message
}

private def STATE() {
    log.trace "settings: ${settings}"
    log.trace "state: ${state}"
}