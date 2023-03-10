/**
*  LS Battery
*
*  Author: kmugh
*  Date: 2014-06-26
*/

definition(
    name: "Snapshot: Battery",
    namespace: "st.kmugh",
    author: "kmugh",
    category: "My Apps",
    description: "Snapshot of battery charge across multiple sensors. Optional push/SMS notifications.",
    iconUrl: "https://s3.amazonaws.com/smartthings-device-icons/Electronics/electronics13-icn.png",
    iconX2Url: "https://s3.amazonaws.com/smartthings-device-icons/Electronics/electronics13-icn@2x.png"
)

preferences 
{
	log.debug "Preferences"
    page(name: "pickSensorsPageBeforeInstallation", title: "Choose Batt Sensors", content: "pickSensorsBefore")
    page(name: "showBatteryPage", content: "listBattsPage", nextPage:"pickSensorsPageAfterInstallation")
    page(name: "pickSensorsPageAfterInstallation", title: "Choose Batt Sensors", content: "pickSensorsAfter", prevPage:"listBattsPage")
}

def pickSensorsBefore()
{
	log.debug "pickSensorsBefore:"
    log.debug app.installationState
	def battSensorsAlreadyDefined = (app.installationState == "COMPLETE");
    
    if (!battSensorsAlreadyDefined)
    {
		return dynamicPage(name: "pickSensorsBefore", title: "Choose Battery Sensors ...", install:true, uninstall:true){
            section("Monitor remaining battery charge...") 
            {
                input "batterySensors", "capability.battery", title: "Choose Battery Sensors", required: true, multiple: true
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
    	return dynamicPage(name: "showBatteryPage", title: "Batterys", nextPage:"pickSensorsPageAfterInstallation"){
            section() 
            {
                paragraph createBattReportText()
            }
        }
    }
}

def pickSensorsAfter()
{
	return dynamicPage(name: "pickSensorsPageAfterInstallation", title: "Choose Battery Sensors ...", install:true, uninstall:true){
		section("Monitor remaining battery charge...") 
		{
			input "batterySensors", "capability.battery", title: "Choose Battery Sensors", required: true, multiple: true
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
	// log.debug "Number of battery sensors: ${batterySensors.size()}"
	
    unsubscribeAndUnschedule()
	subscribeAndSchedule()
}

def subscribeAndSchedule()
{
	def defaultPushSMSUpdateIntervalInMinutes = 60
	subscribe(batterySensors, "battery", battEventsManager)
	schedule(now() + ((timerInterval == null) ? defaultPushSMSUpdateIntervalInMinutes : timerInterval) * 60 * 1000, sendBattReport)
}

def unsubscribeAndUnschedule()
{
	unsubscribe()
	unschedule()
}

def battEventsManager(evt)
{
}

def updated() 
{
	// log.debug "Updated with settings: ${settings}"	
    unsubscribeAndUnschedule()
	subscribeAndSchedule()
}

def createBattReportText()
{
	def noOfSensors = batterySensors.size()
	def battReportText = "Remaining charge across $noOfSensors sensors:\n"
	for (battSens in batterySensors)
	{
		battReportText = battReportText + getHumanReadableLabel(battSens.displayName) + ": " + battSens.currentValue("battery") + "%  \n"
	}
    return battReportText
}

def sendBattReport()
{	
	if ((enablePushNotification == "") ? 0 : ((enablePushNotification == "Yes") ? 1 : 0))
	{
		// log.debug( "sending push message" )
		sendPush(createBattReportText())
	}
	
	if ((enableSMSNotification == "") ? 0 : ((enableSMSNotification == "Yes") ? 1 : 0))
	{
		if (phone)
		{
			// log.debug( "sending text message" )
			sendSms(phone, createBattReportText())
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