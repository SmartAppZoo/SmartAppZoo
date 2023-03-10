/**
*  LS Temperature
*
*  Author: kmugh
*  Date: 2014-06-26
*/

definition(
    name: "Snapshot: Temperature",
    namespace: "st.kmugh",
    author: "kmugh",
    category: "My Apps",
    description: "Snapshot of temperature charge across multiple sensors. Optional push/SMS notifications.",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Electronics/electronics13-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Electronics/electronics13-icn@2x.png"
)

preferences 
{
	log.debug "Preferences"
    page(name: "pickSensorsPageBeforeInstallation", title: "Choose Temp Sensors", content: "pickSensorsBefore")
    page(name: "showTemperaturePage", content: "listTempsPage", nextPage:"pickSensorsPageAfterInstallation")
    page(name: "pickSensorsPageAfterInstallation", title: "Choose Temp Sensors", content: "pickSensorsAfter", prevPage:"listTempsPage")
}

def pickSensorsBefore()
{
	log.debug "pickSensorsBefore:"
    log.debug app.installationState
	def tempSensorsAlreadyDefined = (app.installationState == "COMPLETE");
    
    if (!tempSensorsAlreadyDefined)
    {
		return dynamicPage(name: "pickSensorsBefore", title: "Choose Temperature Sensors ...", install:true, uninstall:true){
            section("Monitor remaining temperature charge...") 
            {
                input "temperatureSensors", "capability.temperature", title: "Choose Temperature Sensors", required: true, multiple: true
            }

            section( "Options" ) 
            {
                input "timerInterval", "number", title: "Update Duration (Default: 60) (in minutes) ", required:false
                input "enablePushNotification", "enum", title: "Send push notifications? (Default: No)", metadata:[values:["No","Yes"]], required:false
                input "enableSMSNotification", "enum", title: "Send SMS notifications? (Default: No)", metadata:[values:["No","Yes"]], required:false
                input "phone", "phone", title: "Phone number for text message?", required: false
            }
        }
    }
    else
    {
    	return dynamicPage(name: "showTemperaturePage", title: "Temperatures", nextPage:"pickSensorsPageAfterInstallation"){
            section() 
            {
                paragraph createTempReportText()
            }
        }
    }
}

def pickSensorsAfter()
{
	return dynamicPage(name: "pickSensorsPageAfterInstallation", title: "Choose Temperature Sensors ...", install:true, uninstall:true){
		section("Monitor remaining temperature charge...") 
		{
			input "temperatureSensors", "capability.temperature", title: "Choose Temperature Sensors", required: true, multiple: true
		}

		section( "Options" ) 
		{
			input "timerInterval", "number", title: "Update Duration (Default: 60) (in minutes) ", required:false
			input "enablePushNotification", "enum", title: "Send push notifications? (Default: No)", metadata:[values:["No","Yes"]], required:false
			input "enableSMSNotification", "enum", title: "Send SMS notifications? (Default: No)", metadata:[values:["No","Yes"]], required:false
			input "phone", "phone", title: "Phone number for text message?", required: false
		}
	}
}

def installed() 
{
	// log.debug "Installed with settings: ${settings}"
	// log.debug "Number of temperature sensors: ${temperatureSensors.size()}"
	
    unsubscribeAndUnschedule()
	subscribeAndSchedule()
}

def subscribeAndSchedule()
{
	def defaultPushSMSUpdateIntervalInMinutes = 60
	subscribe(temperatureSensors, "temperature", tempEventsManager)
	schedule(now() + ((timerInterval == null) ? defaultPushSMSUpdateIntervalInMinutes : timerInterval) * 60 * 1000, sendTempReport)
}

def unsubscribeAndUnschedule()
{
	unsubscribe()
	unschedule()
}

def tempEventsManager(evt)
{
}

def updated() 
{
	// log.debug "Updated with settings: ${settings}"	
    unsubscribeAndUnschedule()
	subscribeAndSchedule()
}

def createTempReportText()
{
	def noOfSensors = temperatureSensors.size()
	def tempReportText = "Remaining charge across $noOfSensors sensors:\n"
	for (tempSens in temperatureSensors)
	{
		tempReportText = tempReportText + getHumanReadableLabel(tempSens.displayName) + ": " + tempSens.currentValue("temperature") + "%  \n"
	}
    return tempReportText
}

def sendTempReport()
{	
	if ((enablePushNotification == "") ? 0 : ((enablePushNotification == "Yes") ? 1 : 0))
	{
		// log.debug( "sending push message" )
		sendPush(createTempReportText())
	}
	
	if ((enableSMSNotification == "") ? 0 : ((enableSMSNotification == "Yes") ? 1 : 0))
	{
		if (phone)
		{
			// log.debug( "sending text message" )
			sendSms(phone, createTempReportText())
		}
	}
	
	unsubscribeAndUnschedule()
	subscribeAndSchedule()
}

def getHumanReadableLabel(stringIn)
{
	if (stringIn.indexOf("[") == -1)
	{
		return stringIn
	}
	else
	{
		def strArray
		strArray = stringIn.split("\\[")
		def wantedStr = strArray[strArray.size() - 1]
		strArray = wantedStr.split("\\]")
		wantedStr = strArray[0]
		return wantedStr
	}
}