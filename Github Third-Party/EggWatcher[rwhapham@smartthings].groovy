/**
 *  Egg Watcher
 *
 *  Copyright 2017 Rob Whapham
 *
 */
definition(
    name: "Egg Watcher",
    namespace: "rwhapham",
    author: "Rob Whapham",
    description: "Monitor egg incubator conditions",
    category: "Convenience",
    iconUrl: "https://github.com/rwhapham/smartthings/raw/master/EggWatcher/resources/chicken-egg-md.png",
    iconX2Url: "https://github.com/rwhapham/smartthings/raw/master/EggWatcher/resources/chicken-egg-md.png",
    iconX3Url: "https://github.com/rwhapham/smartthings/raw/master/EggWatcher/resources/chicken-egg-md.png")

preferences {
	page(name: "page1", title: "Setup", install: true, uninstall: true)
    {
        section("Device Notifications:")
        {
            input "pushNotifications", "bool", title: "Push notfications?", defaultValue: false
            input "smsNumber", "phone", title: "Send text message to?", defaultValue: false
        }

        section("Audio Notifications:")
        {
            input "audioDevice", "capability.audioNotification", title: "Which audio device?", required: false
            input "quietHoursEnabled", "bool", title: "Quiet hours enabled?", required: true
            input "quietHoursStart", "time", title: "Start at?", required: true
            input "quietHoursEnd", "time", title: "End at?", required: true
            input "quietModes", "mode", title: "During which modes?", multiple: true, required: false
        }

        section("Schedule:")
        {
            input "startMonth", type: "number", title: "Start month", range: "1..12", required: true
            input "startDay", type: "number", title: "Start day", range: "1..31", required: true
            input "startYear", type: "number", description: "Format (yyyy)", title: "Start year", range: "2017..2050", required: true
            input "activeDays", "number", title: "Active incubation days?", defaultValue: 18, range: "1..60", required: true
            input "turnTime1", "time", title: "First turn at?", required: true
            input "turnTime2", "time", title: "Second turn at?", required: true
            input "turnTime3", "time", title: "Third turn at?", required: true
            input "turnTimeAudioTrack", "text", title: "Turn time audio track?", required: false
        }
        
        section("Monitor temperature:")
        {
            input "tempSensors", "capability.temperatureMeasurement", title: "Which?", multiple: true, required: false
            input "initialTempLow", "number", title: "For initial temperatures below?", defaultValue: 97, range: "0..150", required: false
            input "initialTempHigh", "number", title: "For initial temperatures above?", defaultValue: 101, range: "0..150", required: false
            input "finalTempLow", "number", title: "For final temperatures below?", defaultValue: 96, range: "0..150", required: false
            input "finalTempHigh", "number", title: "For final temperatures above?", defaultValue: 100, range: "0..150", required: false
            input "lowTempAudioTrack", "text", title: "Low temp audio track?", required: false
            input "highTempAudioTrack", "text", title: "High temp audio track?", required: false
        }

        section("Monitor humidity:")
        {
            input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Which?", multiple: true, required: false
            input "initialHumidityLow", "number", title: "For initial humidity below?", defaultValue: 35, range: "0..100", required: false
            input "initialHumidityHigh", "number", title: "For initial humidity above?", defaultValue: 55, range: "0..100", required: false
            input "finalHumidityLow", "number", title: "For final humidity below?", defaultValue: 60, range: "0..100", required: false
            input "finalHumidityHigh", "number", title: "For final humidity above?", defaultValue: 80, range: "0..100", required: false
            input "lowHumidityAudioTrack", "text", title: "Low humidity audio track?", required: false
            input "highHumidityTempAudioTrack", "text", title: "High humidity audio track?", required: false
        }
    }
}

def installed() {
	logInfo "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	logInfo "Updated with settings: ${settings}"

	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
    atomicState.notifyInactive = false

    schedule(turnTime1, turn1Handler)
    schedule(turnTime2, turn2Handler)
    schedule(turnTime3, turn3Handler)

    if (tempSensors)
		subscribe(tempSensors, "temperature", temperatureHandler)

    if (humiditySensors)
		subscribe(humiditySensors, "humidity", humidityHandler)
}

def startDate() {
  if (startDay && startMonth && startYear) {
    return Date.parse("yyyy-MM-dd", "${startYear}-${startMonth}-${startDay}")
  } else {
    // Start Date Time not set
    return new Date ()
  }
}

def isScheduledDay()
{
    def today = new Date ().clearTime ()
    
    return (today >= startDate ())
}

def isActiveDay()
{
    def today = new Date ().clearTime ()
    def activeEnds = startDate ().plus (activeDays)

    return (today <= activeEnds)
}

def isDoNotDisturb()
{
	def duringQuietHours = (quietHoursEnabled && timeOfDayIsBetween(quietHoursStart, quietHoursEnd, new Date(), location.timeZone))
    def duringQuietMode = (quietModes && quietModes.find {it == location.mode})
    
	return (duringQuietHours || duringQuietMode)
}

def turn1Handler()
{
    turnHandler()
}

def turn2Handler()
{
    turnHandler()
}

def turn3Handler()
{
    turnHandler()
}

def turnHandler()
{
    if (isScheduledDay ())
    {
        if (isActiveDay ())
        {
            logInfo "Time to turn the eggs"

            if (pushNotifications)
                sendPush("Time to turn the eggs")
                
            if (smsNumber)
                sendSms(smsNumber, "Time to turn the eggs")
                
            if (audioDevice && turnTimeAudioTrack && !isDoNotDisturb ())
                audioDevice.playTrack(turnTimeAudioTrack)
        }
        else
        {
            if (atomicState.notifyInactive == false)
            {
                atomicState.notifyInactive = true
                
                logInfo "Hatching time...leave the eggs alone"

                if (pushNotifications)
                    sendPush("Hatching time...leave the eggs alone")


                if (smsNumber)
                    sendSms(smsNumber, "Hatching time...leave the eggs alone")
            }
        }
    }
}

def temperatureHandler(evt)
{
    def pos = tempSensors.findIndexOf { it.id == evt.deviceId }
    
    if (pos >= 0)
    {
        def recentTempEvents = evt.device.events ()?.findAll { it.name == "temperature" }
        def actualTempLow = 0
        def actualTempHigh = 0
        def lastTemp = (recentTempEvents && (recentTempEvents.size () > 1)) ? recentTempEvents[1].floatValue : -1
        def prevTemp = (recentTempEvents && (recentTempEvents.size () > 2)) ? recentTempEvents[2].floatValue : -1
        
        logTrace "Last temperatures from $evt.displayName: $evt.value, $lastTemp, $prevTemp"

		if (isActiveDay ())
        {
            actualTempLow   = (initialTempLow) ? initialTempLow : 97
            actualTempHigh  = (initialTempHigh) ? initialTempHigh : 101

	        logTrace "Test temperature against active day: $actualTempLow - $actualTempHigh"
		}
        else
        {
            actualTempLow   = (finalTempLow) ? finalTempLow : 97
            actualTempHigh  = (finalTempHigh) ? finalTempHigh : 101

	        logTrace "Test temperature against inactive day: $actualTempLow - $actualTempHigh"
		}

        if ((evt.floatValue <= actualTempLow) && ((lastTemp == -1) || (lastTemp > actualTempLow)) && ((prevTemp == -1) || (prevTemp > actualTempLow)))
        {
            logInfo "Low incubator temperature warning from $evt.displayName: $evt.value"

            if (pushNotifications)
                sendPush("Low incubator temperature warning: $evt.value")

            if (smsNumber)
                sendSms(smsNumber, "Low incubator temperature warning: $evt.value")

            if (audioDevice && lowTempAudioTrack && !isDoNotDisturb ())
                audioDevice.playTrack(lowTempAudioTrack)
        }
        else if ((evt.floatValue >= actualTempHigh) && ((lastTemp == -1) || (lastTemp < actualTempHigh)) && ((prevTemp == -1) || (prevTemp < actualTempHigh)))
        {
            logInfo "High incubator temperature warning from $evt.displayName: $evt.value"

            if (pushNotifications)
                sendPush("High incubator temperature warning: $evt.value")

            if (smsNumber)
                sendSms(smsNumber, "High incubator temperature warning: $evt.value")

            if (audioDevice && highTempAudioTrack && !isDoNotDisturb ())
                audioDevice.playTrack(highTempAudioTrack)
        }
    }
}

def humidityHandler(evt)
{
    def pos = humiditySensors.findIndexOf { it.id == evt.deviceId }
    
    if (pos >= 0)
    {
        def recentHumidityEvents = evt.device.events ()?.findAll { it.name == "humidity" }
        def actualHumidityLow = 0
        def actualHumidityHigh = 0
        def lastHumidity = (recentHumidityEvents && (recentHumidityEvents.size () > 1)) ? recentHumidityEvents[1].floatValue : -1
        def prevHumidity = (recentHumidityEvents && (recentHumidityEvents.size () > 2)) ? recentHumidityEvents[2].floatValue : -1

        logTrace "Last humidity readings from $evt.displayName: $evt.value, $lastHumidity, $prevHumidity"

		if (isActiveDay ())
        {
            actualHumidityLow   = (initialHumidityLow) ? initialHumidityLow : 35
            actualHumidityHigh  = (initialHumidityHigh) ? initialHumidityHigh : 55

			logTrace "Test humidity against active day: $actualHumidityLow - $actualHumidityHigh"
        }
        else
        {
            actualHumidityLow   = (finalHumidityLow) ? finalHumidityLow : 60
            actualHumidityHigh  = (finalHumidityHigh) ? finalHumidityHigh : 80

            logTrace "Test humidity against inactive day: $actualHumidityLow - $actualHumidityHigh"
		}

        if ((evt.floatValue <= actualHumidityLow) && ((lastHumidity == -1) || (lastHumidity > actualHumidityLow)) && ((prevHumidity == -1) || (prevHumidity > actualHumidityLow)))
        {
            logInfo "Low incubator humidity warning from $evt.displayName: $evt.value"

            if (pushNotifications)
                sendPush("Low incubator humidity warning: $evt.value")

            if (smsNumber)
                sendSms(smsNumber, "Low incubator humidity warning: $evt.value")

            if (audioDevice && lowHumidityAudioTrack)
                audioDevice.playTrack(lowHumidityAudioTrack && !isDoNotDisturb ())
        }
        else if ((evt.floatValue >= actualHumidityHigh) && ((lastHumidity == -1) || (lastHumidity < actualHumidityHigh)) && ((prevHumidity == -1) || (prevHumidity < actualHumidityHigh)))
        {
            logInfo "High incubator humidity warning from $evt.displayName: $evt.value"

            if (pushNotifications)
                sendPush("High incubator humidity warning: $evt.value")

            if (smsNumber)
                sendSms(smsNumber, "High incubator humidity warning: $evt.value")

            if (audioDevice && highHumidityAudioTrack)
                audioDevice.playTrack(highHumidityAudioTrack && !isDoNotDisturb ())
        }
    }
}

def logTrace (message)
{
    log.trace message
    
    //smartLog ("trace", message)
}

def logDebug (message)
{
    log.debug message
    
    //smartLog ("debug", message)
}

def logInfo (message)
{
    log.info message
    
    //smartLog ("info", message)
}

def logWarn (message)
{
    log.warn message
    
    //smartLog ("warning", message)
}

def logError (message)
{
    log.error message
    
    //smartLog ("error", message)
}
