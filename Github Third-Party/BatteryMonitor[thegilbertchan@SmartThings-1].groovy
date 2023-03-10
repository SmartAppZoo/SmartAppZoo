/**
 *  Modified code posted on forum to add pushover notification
 *
 */

// Automatically generated. Make future change here.
definition(
    name: "Battery Monitor",
    namespace: "",
    author: "tierneykev@gmail.com",
    description: "Monitor battery level of devices",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Battery Alarm Level") {
		input "alarmAt", "number", title: "Alert when below...", required: true
        input "batteryDevices", "capability.battery", title: "Which devices?", multiple: true
	} 
    
    section("Send Pushover alert (optional)"){
        input "apiKey", "text", title: "Pushover API Key", required: false
        input "userKey", "text", title: "Pushover User Key", required: false
        input "deviceName", "text", title: "Pushover Device Name", required: false
        input "priority", "enum", title: "Pushover Priority", required: false,
        metadata :[values: [ 'Low', 'Normal', 'High', 'Emergency']]
    } 
} // end preferences

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}


def updated() {
	log.debug "Updated with settings: ${settings}"    
	unschedule()
	initialize()
}


def initialize() {
    //schedule the job
    schedule("0 0 10am 1,15 * ?", doBatteryCheck)
    
    //run at install too
    doBatteryCheck()
}

def doBatteryCheck() {

	def belowLevelCntr = 0  
    def pushMsg = ""
    
	
    for (batteryDevice in batteryDevices) {
       	def batteryLevel = batteryDevice.currentValue("battery")

	
        if ( batteryLevel <= settings.alarmAt.toInteger() ) {
            pushMsg += "${batteryDevice.name} named ${batteryDevice.label} is at: ${batteryLevel}% \n"
            belowLevelCntr++
        } // end if
    } // end for batteryDevices
    
    
    //update push message based on # devices below level
    if ( belowLevelCntr ){
    	pushMsg = "You have ${belowLevelCntr} devices below the set alarm level. \n" + pushMsg
    } else {	
        pushMsg = "Battery Check App executed with no devices below alarm level"
    }
    
    
    log.debug(pushMsg)
    
    
    //sendPush(pushMsg)
     if(apiKey && userKey){
		log.debug "Sending Pushover with API Key [$apiKey] and User Key [$userKey]"
      
      	def postBody = []
      	def pushPriority = 0
      
      	// Set Priority for Pushover Notification
      	if(priority == "Low"){
        	pushPriority = -1
      	}      
        else if(priority == "Normal"){
        pushPriority = 0
      	}      	
        else if(priority == "High"){
        	pushPriority = 1
      	}      
      	else if(priority == "Emergency"){
        	pushPriority = 2
     	}
      
      
      	if(deviceName){
        
        	log.debug "Sending Pushover to Device: $deviceName"
        
        	if(pushPriority == 2){
          		postBody = [token: "$apiKey", user: "$userKey", device: "$deviceName", message: "$pushMsg", priority: "$pushPriority", retry: "60", expire: "3600"]
        	} else {
          		postBody = [token: "$apiKey", user: "$userKey", device: "$deviceName", message: "$pushMsg", priority: "$pushPriority"]
        	}
            
            log.debug postBody
            
      	} else {
        	log.debug "Sending Pushover to All Devices"
        
        	if(pushPriority == 2){
          		postBody = [token: "$apiKey", user: "$userKey", message: "$pushMsg", priority: "$pushPriority", retry: "60", expire: "3600"]
        	} else {
          		postBody = [token: "$apiKey", user: "$userKey", message: "$pushMsg", priority: "$pushPriority"]
        	}
        
        	log.debug postBody
        } // end else (deviceName)
      
      	def params = [
      		uri: 'https://api.pushover.net/1/messages.json',
            body: postBody
            ]
      
      	httpPost(params){ response ->
          log.debug "Response Received: Status [$response.status]"
          
          if(response.status != 200){
          	sendPush("Received HTTP Error Response. Check Install Parameters.")
          }      
		} //end httpPost
	} // end if apiKey & userKey
	
	else {
		//no pushover info specified
		log.debug(pushMsg)
		sendPush(pushMsg)
	}
	
    
} // end DoBatteryCheck
