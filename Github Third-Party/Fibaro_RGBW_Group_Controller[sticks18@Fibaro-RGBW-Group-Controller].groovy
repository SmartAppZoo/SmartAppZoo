/**
 *  Fibaro_RGBW_Multi_Control.groovy
 *  Fibaro RGBW Group Controller
 *
 *  Author: sgibson18@gmail.com
 *  Date: 2015-04-27
 */
/**
 *  
 *  Use this program with a virtual Fibaro as the master for best results.
 *
 *  This app lets the user control multiple Fibaro RGBW devices via a single 
 *  Virtual Fibaro RGBW device.
 *
******************************************************************************
 *                                Changes
 ******************************************************************************
 *
 ******************************************************************************
 */


// Automatically generated. Make future change here.
definition(
    name: "Fibaro RGBW Group Controller",
    namespace: "Sticks18",
    author: "sgibson18@gmail.com",
    description: "Follows the Virtual Fibaro RGBW",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("When this...") { 
		input "master", "capability.colorControl", 
			multiple: false, 
			title: "Master Virtual Fibaro...", 
			required: true
	}
	    
	section("And these Fibaro RGBW will follow...") {
		input "slaves", "capability.colorControl", 
			multiple: true, 
			title: "Slave Fibaro RGBWs...", 
			required: true
	}
}

def installed()
{
	subscribe(master, "switch.on", switchOnHandler)
	subscribe(master, "switch.off", switchOffHandler)
	subscribe(master, "switch.setLevel", switchSetLevelHandler)
    subscribe(master, "level", switchSetLevelHandler)
	subscribe(master, "switch", switchSetLevelHandler)
    subscribe(master, "adjustedColor", switchAdjustedColorHandler)
    subscribe(master, "presetColor", switchPresetColorHandler)
    subscribe(master, "whiteLevel", switchWhiteLevelHandler)
}

def updated()
{
	unsubscribe()
	subscribe(master, "switch.on", switchOnHandler)
	subscribe(master, "switch.off", switchOffHandler)
	subscribe(master, "switch.setLevel", switchSetLevelHandler)
    subscribe(master, "level", switchSetLevelHandler)
	subscribe(master, "switch", switchSetLevelHandler)
    subscribe(master, "adjustedColor", switchAdjustedColorHandler)
    subscribe(master, "presetColor", switchPresetColorHandler)
    subscribe(master, "whiteLevel", switchWhiteLevelHandler)
	log.info "subscribed to all of masters' events"
}

def switchSetLevelHandler(evt)
{	
	
	if ((evt.value == "on") || (evt.value == "off" ))
		return
	def level = evt.value.toFloat()
	level = level.toInteger()
	log.info "switchSetLevelHandler Event: ${level}"
	slaves?.setLevel(level)
  
}

def switchWhiteLevelHandler(evt)
{	
	
	def level = evt.value.toFloat()
	level = level.toInteger()
	log.info "switchWhiteLevelHandler Event: ${level}"
	slaves?.setWhiteLevel(level)
  
}

def switchAdjustedColorHandler(evt)
{	
	log.info "switchAdjustedColorHandler Event: ${evt.value}"
	      
    def colorMap = evt.value.replace("\"","")
    colorMap = colorMap.replace("{","")
    colorMap = colorMap.replace("}","")
    log.debug "Color Map: ${colorMap}"
    
    def map = [:]
    
    colorMap.split(",").each {param ->
    	def nameAndValue = param.split(":")
    	map[nameAndValue[0]] = nameAndValue[1]
    }
        
    slaves?.setAdjustedColor(map)
}

def switchPresetColorHandler(evt)
{	
	log.info "switchPresetColorHandler Event: ${evt.value}"
	    
    switch(evt.value) {
    
    	case "Soft White": 
        	slaves?.softwhite()
    		break
            
        case "Daylight": 
        	slaves?.daylight()
    		break
        
        case "Warm White": 
        	slaves?.warmwhite()
    		break
        
        case "Red": 
        	slaves?.red()
    		break
            
        case "Green": 
        	slaves?.green()
    		break
            
        case "Blue": 
        	slaves?.blue()
    		break
            
        case "Cyan": 
        	slaves?.cyan()
    		break
            
        case "Magenta": 
        	slaves?.magenta()
    		break
            
        case "Orange": 
        	slaves?.orange()
    		break
            
        case "Purple": 
        	slaves?.purple()
    		break
            
        case "Yellow": 
        	slaves?.yellow()
    		break
            
        case "White": 
        	slaves?.white()
    		break
            
        case "Fireplace": 
        	slaves?.fireplace()
    		break
            
        case "Storm": 
        	slaves?.storm()
    		break
            
        case "Deep Fade": 
        	slaves?.deepfade()
    		break
            
        case "Lite Fade": 
        	slaves?.litefade()
    		break
            
        case "Police": 
        	slaves?.police()
    		break    
	}            
            
            
}

def switchOffHandler(evt) {
	log.info "switchoffHandler Event: ${evt.value}"
	slaves?.off()
}

def switchOnHandler(evt) {
	log.info "switchOnHandler Event: ${evt.value}"
	def dimmerValue = masters.latestValue("level") //can be turned on by setting the level
	slaves?.on()
}