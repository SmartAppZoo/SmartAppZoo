/**
 *  Barn Automation
 *
 *  Copyright 2017 Rob Whapham
 *
 */
definition(
    name: "Barn Automation",
    namespace: "rwhapham",
    author: "Rob Whapham",
    description: "Automation for various devices in the barn",
    category: "Convenience",
    iconUrl: "https://github.com/rwhapham/smartthings/raw/master/BarnAutomation/resources/9014-128x128x32.png",
    iconX2Url: "https://github.com/rwhapham/smartthings/raw/master/BarnAutomation/resources/9014-256x256x32.png",
    iconX3Url: "https://github.com/rwhapham/smartthings/raw/master/BarnAutomation/resources/9014-256x256x32.png")


preferences {
	page(name: "page1", title: "Setup", nextPage: "page2", install: false, uninstall: true)
    {
        section("Audio notification on open/left open:")
        {
            input "notifyOnOpenSensors", "capability.contactSensor", title: "Which?", multiple: true, required: false
            input "leftOpenSensors", "capability.contactSensor", title: "Which left open?", multiple: true, required: false
            input "leftOpenDuration", "number", title: "For how long (mins)?", defaultValue: 15, range: "1..30", required: false
        }

        section("Doorbell triggers:")
        {
        	input "doorbellSwitch", "capability.switch", title: "Which?", multiple: false, required: false
        	input "doorbellSensors", "capability.contactSensor", title: "Which triggers?", multiple: true, required: false
            input "doorbellNotify", "bool", title: "Push notfication?", defaultValue: false
            input "doorbellPausePlayers", "capability.musicPlayer", title: "Pause playback on?", multiple: true, required: false
        }
        
        section("Monitor indoor temperature:")
        {
            input "tempSensors", "capability.temperatureMeasurement", title: "Which?", multiple: true, required: false
            input "tempLow", "number", title: "For temperatures below?", defaultValue: 45, range: "0..100", required: false
            input "tempHigh", "number", title: "For temperatures above?", defaultValue: 85, range: "0..100", required: false
        }
        
        section("Pipe freeze warning:")
        {
            input "pipeTempSensors", "capability.temperatureMeasurement", title: "Which?", multiple: true, required: false
            input "pipeTempLow", "number", title: "For temperatures below?", defaultValue: 35, range: "0..100", required: false
        }
        
        section("Monitor outdoor temperature:")
        {
            input "outdoorTempSensor", "capability.temperatureMeasurement", title: "Which?", required: false
            input "outdoorTempLow", "number", title: "For temperatures below?", defaultValue: 35, range: "0..100", required: false
        	input "outdoorTempLowSwitches", "capability.switch", title: "Turn on which switches?", multiple: true, required: false
        	input "outdoorTempLowOutlets", "capability.outlet", title: "Turn on which outlets?", multiple: true, required: false
            input "outdoorTempLowNotify", "bool", title: "Push notfication?", defaultValue: false
            input "outdoorTempHigh", "number", title: "For temperatures above?", defaultValue: 85, range: "0..100", required: false
        	input "outdoorTempHighSwitches", "capability.switch", title: "Turn on which switches?", multiple: true, required: false
        	input "outdoorTempHighOutlets", "capability.outlet", title: "Turn on which outlets?", multiple: true, required: false
            input "outdoorTempHighNotify", "bool", title: "Push notfication?", defaultValue: false
        }
        
        section("Monitor smoke/CO2:")
        {
        	input "smokeSensors", "capability.smokeDetector", title: "Which?", multiple: true, required: false
            input "smokeStopPlayers", "capability.musicPlayer", title: "Stop playback on?", multiple: true, required: false
        	input "smokeSwitches", "capability.switch", title: "Turn on which switches?", multiple: true, required: false
        	input "smokeOutlets", "capability.outlet", title: "Turn on which outlets?", multiple: true, required: false
        }
        
        section("Monitor leaks:")
        {
        	input "waterSensors", "capability.waterSensor", title: "Which?", multiple: true, required: false
            input "waterStopPlayers", "capability.musicPlayer", title: "Stop playback on?", multiple: true, required: false
        }
        
        section("Night lights:")
        {
        	input "nightLightSwitches", "capability.switch", title: "Which switches?", multiple: true, required: false
        	input "nightLightOutlets", "capability.outlet", title: "Which outlets?", multiple: true, required: false
            input "nightLightLuxSensor", "capability.illuminanceMeasurement", title: "Use light sensor?", required: false
            input "nightLightLuxLevel", "number", title: "On at light level (lux)?", defaultValue: 200, range: "1..1000", required: false
            input "nightLightOffLuxLevel", "number", title: "Off at light level (lux)?", defaultValue: 300, range: "1..1000", required: false
            input "nightLightEntrySensors", "capability.contactSensor", title: "Entry lights for which?", multiple: true, required: false
        }
        
        section("Audio notification player:")
        {
            input "audioDevice", "capability.audioNotification", title: "Which?", required: false
        }
        
        section("Do not disturb:")
        {
        	input "quietHoursEnabled", "bool", title: "Quiet hours enabled?", required: true
            input "quietHoursStart", "time", title: "Start at?", required: true
            input "quietHoursEnd", "time", title: "End at?", required: true
            input "quietModes", "mode", title: "During which modes?", multiple: true, required: false
        }
        
        section("EventGhost logging:")
        {
            input "egServer", "text", title: "Server?", description: "EventGhost Web Server IP", required: false
            input "egPort", "number", title: "Port?", description: "EventGhost Web Server Port", required: false, defaultValue: 80
            input "egPrefix", "text", title: "Command prefix?", required: false, defaultValue: "ST"
            input "logContactSensors", "capability.contactSensor", title: "Which contact sensors?", multiple: true, required: false
            input "logTempSensors", "capability.temperatureMeasurement", title: "Which temperature sensors?", multiple: true, required: false
            input "logHumiditySensors", "capability.relativeHumidityMeasurement", title: "Which humidity sensors?", multiple: true, required: false
            input "logLuxSensors", "capability.illuminanceMeasurement", title: "Which light sensors?", multiple: true, required: false
        	input "logSmokeSensors", "capability.smokeDetector", title: "Which smoke/CO2 sensors?", multiple: true, required: false
        	input "logWaterSensors", "capability.waterSensor", title: "Which water sensors?", multiple: true, required: false
        	input "logSwitches", "capability.switch", title: "Which switches?", multiple: true, required: false
        	input "logOutlets", "capability.outlet", title: "Which outlets?", multiple: true, required: false
        }
    }
    
    page(name: "page2", title: "Open/Left Open Audio Notifications", nextPage: "page3", install: false, uninstall: true)
    page(name: "page3", title: "Temperature Warning Audio Notifications", nextPage: "page4", install: false, uninstall: true)
    page(name: "page4", title: "Water Leak Audio Notifications", nextPage: "page5", install: false, uninstall: true)
    page(name: "page5", title: "Night Light Entry Lights", install: true, uninstall: true)
}

def page2() {
	dynamicPage(name: "page2")
    {
    	if (audioDevice)
        {
            if (notifyOnOpenSensors || leftOpenSensors)
            {
                def pos = 0

                notifyOnOpenSensors.each {
                    section("$it.displayName open notification:")
                    {
                        input "onOpenAudioTrack" + pos, "text", title: "Audio track?", required: false
                    }

                    pos++
                }

                pos = 0

                leftOpenSensors.each {
                    section("$it.displayName left open notification:")
                    {
                        input "leftOpenAudioTrack" + pos, "text", title: "Audio track?", required: false
                    }

                    pos++
                }
            }
            else
            {
                section()
                {
                	paragraph "No open/left open devices selected"
                }
            }
        }
        else
        {
        	section()
            {
            	paragraph "No audio notification player selected"
            }
        }
	}
}

def page3() {
	dynamicPage(name: "page3")
    {
    	if (audioDevice)
        {
            if (tempSensors || pipeTempSensors)
            {
                def pos = 0

                tempSensors.each {
                    section("$it.displayName temperature notifications:")
                    {
                        input "lowTempAudioTrack" + pos, "text", title: "Low temp audio track?", required: false
                        input "highTempAudioTrack" + pos, "text", title: "High temp audio track?", required: false
                    }

                    pos++
                }

                pos = 0

                pipeTempSensors.each {
                    section("$it.displayName pipe freeze warning notifications:")
                    {
                        input "lowPipeTempAudioTrack" + pos, "text", title: "Low temp audio track?", required: false
                    }

                    pos++
                }
			}
            else
            {
                section()
                {
                	paragraph "No temperature sensors selected"
                }
            }
        }
        else
        {
        	section()
            {
            	paragraph "No audio notification player selected"
            }
        }
    }
}

def page4() {
	dynamicPage(name: "page4")
    {
    	if (audioDevice)
        {
            if (waterSensors)
            {
                def pos = 0

                waterSensors.each {
                    section("$it.displayName leak notification:")
                    {
                        input "leakAudioTrack" + pos, "text", title: "Audio track?", required: false
                    }

                    pos++
                }
            }
            else
            {
                section()
                {
                	paragraph "No water sensors selected"
                }
            }
        }
        else
        {
        	section()
            {
            	paragraph "No audio notification player selected"
            }
        }
    }
}

def page5() {
	dynamicPage(name: "page5")
    {
    	if (nightLightEntrySensors)
        {
        	def pos = 0
            
            nightLightEntrySensors.each {
            	section("$it.displayName entry lights:")
                {
                	input "entryLightSwitches" + pos, "capability.switch", title: "Which switches?", multiple: true, required: false
                    input "entryLightOutlets" + pos, "capability.outlet", title: "Which outlets?", multiple: true, required: false
                    input "entryLightSwitchLevel" + pos, "number", title: "Dimmer level (%)?", defaultValue: 50, range: "1..100", required: false
                }
            
            	pos ++
          	}
        }
        else
        {
        	section()
            {
            	paragraph "No night light entry sensors selected"
            }
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
	atomicState.triggerDoorbell = false
    
	if (notifyOnOpenSensors)
    {
    	subscribe(notifyOnOpenSensors, "contact.open", onOpenHandler)
        
        notifyOnOpenSensors.each { atomicState[it.id + "_onOpen"] = null }
    }
        
	if (leftOpenSensors)
    {
    	leftOpenSensors.each {
            def contactState = it.currentState("contact")

            if (contactState.value == "open")
            {
                def elapsed = now() - contactState.rawDateCreated.time

                if (elapsed < (leftOpenDuration * 60000))
                {
                    logDebug "$it.displayName is currently open, monitoring for left open"

                    runIn(((leftOpenDuration * 60000) - elapsed) / 1000, leftOpenHandler, [overwrite: false, data: [deviceId: it.id]])
                }
            }
        }
        
		subscribe(leftOpenSensors, "contact.open", startLeftOpenHandler)
	}
    
    if (tempSensors)
    {
		subscribe(tempSensors, "temperature", temperatureHandler)
	}
    
    if (outdoorTempSensor)
		subscribe(outdoorTempSensor, "temperature", outdoorTemperatureHandler)
    
    if (pipeTempSensors)
    {
		subscribe(pipeTempSensors, "temperature", pipeTemperatureHandler)
	}
    
    if (doorbellSensors)
    	subscribe(doorbellSensors, "contact.closed", doorbellSensorHandler)
    
    if (doorbellSwitch)
    	subscribe(doorbellSwitch, "switch.on", doorbellHandler)
        
    if (smokeSensors)
    {
    	subscribe(smokeSensors, "smoke", smokeHandler)
        subscribe(smokeSensors, "carbonMonoxide", smokeHandler)
    }
    
    if (waterSensors)
    {
    	subscribe(waterSensors, "water", waterHandler)
    }

	if (nightLightSwitches || nightLightOutlets)
    {
    	if (nightLightLuxSensor)
        {
            def luxLevel = nightLightLuxSensor.latestValue("illuminance")
            
            if (luxLevel)
            {
				def luxOnThreshold = nightLightLuxLevel ?: 200
                
           		atomicState.nightLightNight = (luxLevel <= luxOnThreshold)
            }
            else
            {
            	atomicState.nightLightNight = false
            }

			subscribe(nightLightLuxSensor, "illuminance", nightLightLuxHandler)
        }
        else
        {
        	def sunriseSunset = getSunriseAndSunset ()
            
			atomicState.nightLightNight = (timeOfDayIsBetween(sunriseSunset.sunset, sunriseSunset.sunrise, new Date(), location.timeZone))

			subscribe(location, "sunset", nightLightSunsetHandler)
            subscribe(location, "sunrise", nightLightSunriseHandler)
        }
    }
    
    if (nightLightEntrySensors)
    	subscribe(nightLightEntrySensors, "contact.open", nightLightEntryHandler)
    
    subscribe(location, "mode", logValueHandler)

    if (logContactSensors)
    	subscribe(logContactSensors, "contact", logValueHandler)
        
    if (logTempSensors)
		subscribe(logTempSensors, "temperature", logValueHandler)

    if (logHumiditySensors)
		subscribe(logHumiditySensors, "humidity", logValueHandler)

    if (logLuxSensors)
		subscribe(logLuxSensors, "illuminance", logValueHandler)

    if (logSmokeSensors)
    {
    	subscribe(logSmokeSensors, "smoke", logValueHandler)
        subscribe(logSmokeSensors, "carbonMonoxide", logValueHandler)
    }
    
    if (logWaterSensors)
        subscribe(logWaterSensors, "water", logValueHandler)

    if (logSwitches)
        subscribe(logSwitches, "switch", logValueHandler)
    
    if (logOutlets)
        subscribe(logOutlets, "switch", logValueHandler)
}

def isDoNotDisturb()
{
	def duringQuietHours = (quietHoursEnabled && timeOfDayIsBetween(quietHoursStart, quietHoursEnd, new Date(), location.timeZone))
    def duringQuietMode = (quietModes && quietModes.find {it == location.mode})
    
	return (duringQuietHours || duringQuietMode)
}

def playNotificationTrack(data)
{
	if (audioDevice && data.audioTrack)
    {
        logDebug "playNotificationTrack - $audioDevice.displayName play track $data.audioTrack"

        atomicState.triggerDoorbell = true
        
        audioDevice.playTrack(data.audioTrack)
    }
}

def onOpenHandler(evt)
{
    def stateKey = evt.deviceId + "_onOpen"
    def elapsed = Integer.MAX_VALUE
    
    if (atomicState[stateKey])
    {
        elapsed = now() - atomicState[stateKey]
    }
        
    if ((elapsed >= 30000) && !isDoNotDisturb())
    {
    	atomicState[stateKey] = now ()
        
        if (audioDevice)
        {
            def pos = notifyOnOpenSensors.findIndexOf { it.id == evt.deviceId }

            if (pos >= 0)
            {
                def audioTrackName = "onOpenAudioTrack" + pos
                def audioTrack = this."$audioTrackName"

                if (audioTrack)
                    playNotificationTrack([audioTrack: audioTrack])
            }
        }
    }
}

def startLeftOpenHandler(evt)
{
	logDebug "Monitoring $evt.displayName for left open"

	runIn((leftOpenDuration) ? leftOpenDuration * 60 : 15 * 60, leftOpenHandler, [overwrite: false, data: [deviceId: evt.deviceId]])
}

def leftOpenHandler(data)
{
    def pos = leftOpenSensors.findIndexOf { it.id == data.deviceId }

    if (pos >= 0)
    {
    	def device = leftOpenSensors[pos]
        def contactState = device.currentState("contact")

        if (contactState.value == "open")
        {
            def elapsed = now() - contactState.rawDateCreated.time

            if (elapsed >= ((leftOpenDuration * 60000) - 1000))
            {
            	if (!isDoNotDisturb())
                {
                    logInfo "$device.displayName was left open"

                    sendPush ("$device.displayName was left open")

                    if (audioDevice)
                    {
                        def audioTrackName = "leftOpenAudioTrack" + pos
                        def audioTrack = this."$audioTrackName"

                        if (audioTrack)
                            playNotificationTrack([audioTrack: audioTrack])
                    }
                }
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
        def actualTempLow = (tempLow) ? tempLow : 45
        def actualTempHigh = (tempHigh) ? tempHigh : 85
        def lastTemp = (recentTempEvents && (recentTempEvents.size () > 1)) ? recentTempEvents[1].integerValue : -1
        def prevTemp = (recentTempEvents && (recentTempEvents.size () > 2)) ? recentTempEvents[2].integerValue : -1
        def audioTrackName = null
        def audioTrack = null

        logTrace "Last temperatures from $evt.displayName: $evt.value, $lastTemp, $prevTemp"

        if ((evt.integerValue <= actualTempLow) && ((lastTemp == -1) || (lastTemp > actualTempLow)) && ((prevTemp == -1) || (prevTemp > actualTempLow)))
        {
            logInfo "Low temperature warning from $evt.displayName: $evt.value"

            sendPush("Low temperature warning from $evt.displayName: $evt.value")

            audioTrackName = "lowTempAudioTrack" + pos
            audioTrack = this."$audioTrackName"
        }
        else if ((evt.integerValue >= actualTempHigh) && ((lastTemp == -1) || (lastTemp < actualTempHigh)) && ((lastTemp == -1) || (lastTemp < actualTempHigh)))
        {
            logInfo "High temperature warning from $evt.displayName: $evt.value"

            sendPush("High temperature warning from $evt.displayName: $evt.value")

            audioTrackName = "highTempAudioTrack" + pos
            audioTrack = this."$audioTrackName"
        }

        if (audioDevice && audioTrack)
            playNotificationTrack([audioTrack: audioTrack])
    }
}

def pipeTemperatureHandler(evt)
{
    def pos = pipeTempSensors.findIndexOf { it.id == evt.deviceId }
    
    if (pos >= 0)
    {
        def recentTempEvents = evt.device.events ()?.findAll { it.name == "temperature" }
        def actualTempLow = (tempLow) ? tempLow : 35
        def lastTemp = (recentTempEvents && (recentTempEvents.size () > 1)) ? recentTempEvents[1].integerValue : -1
        def prevTemp = (recentTempEvents && (recentTempEvents.size () > 2)) ? recentTempEvents[2].integerValue : -1
        def audioTrackName = null
        def audioTrack = null

        logTrace "Last pipe temperatures from $evt.displayName: $evt.value, $lastTemp, $prevTemp"

        if ((evt.integerValue <= actualTempLow) && ((lastTemp == -1) || (lastTemp > actualTempLow)) && ((prevTemp == -1) || (prevTemp > actualTempLow)))
        {
            logInfo "Low pipe temperature warning from $evt.displayName: $evt.value"

            sendPush("Low pipe temperature warning from $evt.displayName: $evt.value")

            audioTrackName = "lowPipeTempAudioTrack" + pos
            audioTrack = this."$audioTrackName"
        }

        if (audioDevice && audioTrack)
            playNotificationTrack([audioTrack: audioTrack])
    }
}

def outdoorTemperatureHandler(evt)
{
    def actualTempLow = (outdoorTempLow) ? outdoorTempLow : 35
    def actualTempHigh = (outdoorTempHigh) ? outdoorTempHigh : 85
    def cancelTempLow = actualTempLow + 5
    def cancelTempHigh = actualTempHigh - 5
    def tempHistory = atomicState.outdoorTempHistory ?: [-1, -1]

    logTrace "Last outdoor temperatures from $evt.displayName: $evt.value, ${tempHistory[0]}, ${tempHistory[1]}"

    if ((evt.integerValue <= actualTempLow) && ((tempHistory[0] == -1) || (tempHistory[0] > actualTempLow)) && ((tempHistory[1] == -1) || (tempHistory[1] > actualTempLow)))
    {
        logInfo "Low outdoor temperature warning: $evt.value"

		if (outdoorTempLowNotify)
        	sendPush("Low outdoor temperature warning: $evt.value")

        if (outdoorTempLowSwitches)
        {
            outdoorTempLowSwitches.each {
                if (it.latestValue("switch") == "off")
                {
                    logInfo "outdoorTemperatureHandler - turning on $it.displayName"

                    it.on()
                }
            }
        }

        if (outdoorTempLowOutlets)
        {
            outdoorTempLowOutlets.each {
                if (it.latestValue("switch") == "off")
                {
                    logInfo "outdoorTemperatureHandler - turning on $it.displayName"

                    it.on()
                }
            }
        }
    }
    else if ((evt.integerValue > cancelTempLow) && ((tempHistory[0] == -1) || (tempHistory[0] <= cancelTempLow)) && ((tempHistory[1] == -1) || (tempHistory[1] <= cancelTempLow)))
    {
    	if (lastTemp != -1)
        	logInfo "Low outdoor temperature warning ended: $evt.value"

        if (outdoorTempLowSwitches)
        {
            outdoorTempLowSwitches.each {
                if (it.latestValue("switch") == "on")
                {
                    logInfo "outdoorTemperatureHandler - turning off $it.displayName"

                    it.off()
                }
            }
        }

        if (outdoorTempLowOutlets)
        {
            outdoorTempLowOutlets.each {
                if (it.latestValue("switch") == "on")
                {
                    logInfo "outdoorTemperatureHandler - turning off $it.displayName"

                    it.off()
                }
            }
        }
    }

    if ((evt.integerValue >= actualTempHigh) && ((tempHistory[0] == -1) || (tempHistory[0] < actualTempHigh)) && ((tempHistory[1] == -1) || (tempHistory[1] < actualTempHigh)))
    {
        logInfo "High outdoor temperature warning: $evt.value"

		if (outdoorTempHighNotify)
	        sendPush("High outdoor temperature warning: $evt.value")

        if (outdoorTempHighSwitches)
        {
            outdoorTempHighSwitches.each {
                if (it.latestValue("switch") == "off")
                {
                    logInfo "outdoorTemperatureHandler - turning on $it.displayName"

                    it.on()
                }
            }
        }

        if (outdoorTempHighOutlets)
        {
            outdoorTempHighOutlets.each {
                if (it.latestValue("switch") == "off")
                {
                    logInfo "outdoorTemperatureHandler - turning on $it.displayName"

                    it.on()
                }
            }
        }
    }
    else if ((evt.integerValue < cancelTempHigh) && ((tempHistory[0] == -1) || (tempHistory[0] >= cancelTempHigh)) && ((tempHistory[1] == -1) || (tempHistory[1] >= cancelTempHigh)))
    {
    	if (lastTemp != -1)
        	logInfo "High outdoor temperature warning ended: $evt.value"

        if (outdoorTempHighSwitches)
        {
            outdoorTempHighSwitches.each {
                if (it.latestValue("switch") == "on")
                {
                    logInfo "outdoorTemperatureHandler - turning off $it.displayName"

                    it.off()
                }
            }
        }

        if (outdoorTempHighOutlets)
        {
            outdoorTempHighOutlets.each {
                if (it.latestValue("switch") == "on")
                {
                    logInfo "outdoorTemperatureHandler - turning off $it.displayName"

                    it.off()
                }
            }
        }
    }
    
    tempHistory[1] = tempHistory[0]
    tempHistory[0] = evt.integerValue
    atomicState.outdoorTempHistory = tempHistory
}

def doorbellSensorHandler(evt)
{
    logInfo "$evt.displayName was pressed"
    
    if (doorbellSwitch)
    {
        logInfo "doorbellSensorHandler - triggering $doorbellSwitch.displayName"

        doorbellSwitch.on()
    }
}

def doorbellHandler(evt)
{
    logTrace "doorbellHandler - triggerDoorbell: $atomicState.triggerDoorbell"
    
	if (atomicState.triggerDoorbell == false)
    {
        logInfo "$evt.displayName was pressed"

        if (doorbellPausePlayers)
        {
            doorbellPausePlayers.each {
                if (it.latestValue("status") == "playing")
                {
                    logInfo "doorbellHandler - pausing $it.displayName"

                    it.pause()
                }
            }
        }

		if (doorbellNotify)
			sendPushMessage("$evt.displayName was pressed")
    }
    else
    {
        atomicState.triggerDoorbell = false
    }
}

def smokeHandler(evt)
{
	if (evt.value != "clear")
    {
    	if (smokeStopPlayers)
        {
            smokeStopPlayers.each {
                if (it.latestValue("status") == "playing")
                {
                    logInfo "smokeHandler - stopping $it.displayName"

                    it.stop()
                }
            }
        }
        
        if (smokeSwitches)
        {
            smokeSwitches.each {
                if (it.latestValue("switch") == "off")
                {
                    if (it.hasCapability("Switch Level"))
                    {
                        logInfo "smokeHandler - turning on $it.displayName to 100%"

                        it.setLevel(100)
                    }
                    else
                    {
                        logInfo "smokeHandler - turning on $it.displayName"

                        it.on()
                    }
                }
            }
        }

        if (smokeOutlets)
        {
            smokeOutlets.each {
                if (it.latestValue("switch") == "off")
                {
                    logInfo "smokeHandler - turning on $it.displayName"

                    it.on()
                }
            }
        }
    }
}

def waterHandler(evt)
{
	if (evt.value != "dry")
    {
		if (waterStopPlayers)
        {
            waterStopPlayers.each {
                if (it.latestValue("status") == "playing")
                {
                    logInfo "waterHandler - stopping $it.displayName"

                    it.stop()
                }
            }
        }
        
        if (audioDevice)
        {
            def pos = waterSensors.findIndexOf { it.id == evt.deviceId }

            if (pos >= 0)
            {
                def audioTrackName = "leakAudioTrack" + pos
                def audioTrack = this."$audioTrackName"

                if (audioTrack)
                    playNotificationTrack([audioTrack: audioTrack])
            }
        }
    }
}

def nightLightLuxHandler(evt)
{
	def luxOnThreshold = nightLightLuxLevel ?: 200
    def luxOffThreshold = nightLightOffLuxLevel ?: 300
    def luxHistory = atomicState.nightLightLuxHistory ?: [-1, -1]
    def audioTrackName = null
    def audioTrack = null

    logTrace "Last lux values from $evt.displayName: $evt.value, ${luxHistory[0]}, ${luxHistory[1]}"

    //if ((evt.integerValue <= luxOnThreshold) && ((luxHistory[0] == -1) || (luxHistory[0] > luxOnThreshold)) && ((luxHistory[1] == -1) || (luxHistory[1] > luxOnThreshold)))
    if ((evt.integerValue <= luxOnThreshold) && ((luxHistory[0] == -1) || (luxHistory[0] > luxOnThreshold)))
	{
        logDebug "nightLightLuxHandler - below light threshold of $luxOnThreshold lux"
        
        atomicState.nightLightNight = true

        nightLightSwitches.each {
            if (it.latestValue("switch") == "off")
            {
                logInfo "nightLightLuxHandler - turning on $it.displayName"

                it.on()
            }
        }

        nightLightOutlets.each {
            if (it.latestValue("switch") == "off")
            {
                logInfo "nightLightLuxHandler - turning on $it.displayName"

                it.on()
            }
        }
    }
    //else if ((evt.integerValue >= luxOffThreshold) && ((luxHistory[0] == -1) || (luxHistory[0] < luxOffThreshold)) && ((luxHistory[1] == -1) || (luxHistory[1] < luxOffThreshold)))
    else if ((evt.integerValue >= luxOffThreshold) && ((luxHistory[0] == -1) || (luxHistory[0] < luxOffThreshold)))
	{
        logDebug "Above light threshold of $luxOffThreshold lux"

        atomicState.nightLightNight = false

		nightLightSwitches.each {
            if (it.latestValue("switch") == "on")
            {
                logInfo "nightLightLuxHandler - turning off $it.displayName"

                it.off()
            }
        }

        nightLightOutlets.each {
            if (it.latestValue("switch") == "on")
            {
                logInfo "nightLightLuxHandler - turning off $it.displayName"

                it.off()
            }
        }
    }
    
    luxHistory[1] = luxHistory[0]
    luxHistory[0] = evt.integerValue
    atomicState.nightLightLuxHistory = luxHistory
}

def nightLightSunsetHandler(evt)
{
	logDebug "Sunset has occurred"

    atomicState.nightLightNight = true

	if (nightLightSwitches)
    {
        nightLightSwitches.each {
            if (it.latestValue("switch") == "off")
            {
                logInfo "sunsetHandler - turning on $it.displayName"

                it.on()
            }
        }
    }

	if (nightLightOutlets)
    {
        nightLightOutlets.each {
            if (it.latestValue("switch") == "off")
            {
                logInfo "sunsetHandler - turning on $it.displayName"

                it.on()
            }
        }
    }
}

def nightLightSunriseHandler(evt)
{
	logDebug "Sunrise has occurred"

    atomicState.nightLightNight = false

	if (nightLightSwitches)
    {
        nightLightSwitches.each {
            if (it.latestValue("switch") == "on")
            {
                logInfo "sunriseHandler - turning off $it.displayName"

                it.off()
            }
        }
    }

	if (nightLightOutlets)
    {
        nightLightOutlets.each {
            if (it.latestValue("switch") == "on")
            {
                logInfo "sunriseHandler - turning off $it.displayName"

                it.off()
            }
        }
    }
}

def nightLightEntryHandler(evt)
{
	def pos = nightLightEntrySensors.findIndexOf { it.id == evt.deviceId }
    
	if (pos >= 0)
    {
        if (atomicState.nightLightNight)
        {
            def device = nightLightEntrySensors[pos]
            def switchesName = "entryLightSwitches" + pos
            def outletsName = "entryLightOutlets" + pos
            def switches = this."$switchesName"
            def outlets = this."$outletsName"
            
            if (switches || outlets)
            	logDebug "nightLightEntryHandler - $device.displayName was opened"

            if (switches)
            {
                switches.each {
                    if (it.latestValue("switch") == "off")
                    {
                        def switchState = it.currentState("switch")
                        def elapsed = now() - switchState.rawDateCreated.time

                        if (elapsed >= 10000)
                        {
                            if (it.hasCapability("Switch Level"))
                            {
                                def switchLevelName = "entryLightSwitchLevel" + pos
                                def switchLevel = this."$switchLevelName"
                                def levelValue = switchLevel ?: 50

                                logInfo "nightLightEntryHandler - turning on $it.displayName to $levelValue%"

                                it.setLevel(levelValue)
                            }
                            else
                            {
                                logInfo "nightLightEntryHandler - turning on $it.displayName"

                                it.on()
                            }
                        }
                    }
                }
            }

            if (outlets)
            {
                outlets.each {
                    if (it.latestValue("switch") == "off")
                    {
                        logInfo "nightLightEntryHandler - turning on $it.displayName"

                        it.on()
                    }
                }
            }
        }
    }
}

def logTrace (message)
{
    log.trace message
    
    smartLog ("trace", message)
}

def logDebug (message)
{
    log.debug message
    
    smartLog ("debug", message)
}

def logInfo (message)
{
    log.info message
    
    smartLog ("info", message)
}

def logWarn (message)
{
    log.warn message
    
    smartLog ("warning", message)
}

def logError (message)
{
    log.error message
    
    smartLog ("error", message)
}

def smartLog (level, message)
{
    if (egServer && egPort && egPrefix)
    {
        def egHost = "${settings.egServer}:${settings.egPort}"
        def egRawCommand = "${settings.egPrefix}.Log"
        def egRestCommand = java.net.URLEncoder.encode(egRawCommand)
        def egRestSource = java.net.URLEncoder.encode("BarnAutomation")
        def egRestLevel = java.net.URLEncoder.encode("$level")
        def egRestMessage = java.net.URLEncoder.encode("$message")
        def egRestCommandValue = "$egRestCommand&$egRestSource&$egRestLevel&$egRestMessage"

        sendHubCommand(new physicalgraph.device.HubAction("""GET /?$egRestCommandValue HTTP/1.1\r\nHOST: $egHost\r\n\r\n""", physicalgraph.device.Protocol.LAN))
    }
}

def logValueHandler(evt)
{
    deviceLog (evt.displayName, "$evt.name: $evt.value")
}

def deviceLog (name, message)
{
    log.info "$name - $message"

    if (egServer && egPort && egPrefix)
    {
        def egHost = "${settings.egServer}:${settings.egPort}"
        def egRawCommand = "${settings.egPrefix}.Log"
        def egRestCommand = java.net.URLEncoder.encode(egRawCommand)
        def egRestSource = java.net.URLEncoder.encode("$name")
        def egRestLevel = java.net.URLEncoder.encode("info")
        def egRestMessage = java.net.URLEncoder.encode("$message")
        def egRestCommandValue = "$egRestCommand&$egRestSource&$egRestLevel&$egRestMessage"

        sendHubCommand(new physicalgraph.device.HubAction("""GET /?$egRestCommandValue HTTP/1.1\r\nHOST: $egHost\r\n\r\n""", physicalgraph.device.Protocol.LAN))
    }
}
