/**
 *  SmartThingsLogger
 *
 *  Copyright 2017 Peter Sun
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "SmartThingsLogger",
    namespace: "psun03",
    author: "Peter Sun",
    description: "Captures the events posted by devices and sends them to an Arduino",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

    section("General:") {
        //input "prefDebugMode", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: true
        input (
        	name: "configLoggingLevelIDE",
        	title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
        	type: "enum",
        	options: [
        	    "0" : "None",
        	    "1" : "Error",
        	    "2" : "Warning",
        	    "3" : "Info",
        	    "4" : "Debug",
        	    "5" : "Trace"
        	],
        	defaultValue: "3",
            displayDuringSetup: true,
        	required: false
        )
    }

    section ("Server Information:") {
        input "prefHost", "text", title: "Host", defaultValue: "10.10.10.10", required: true
        input "prefPort", "text", title: "Port", defaultValue: "8086", required: true
    }
    
    section("Polling:") {
        input "prefSoftPollingInterval", "number", title:"Soft-Polling interval (minutes)", defaultValue: 10, required: true
    }
    
    section("System Monitoring:") {
        input "prefLogModeEvents", "bool", title:"Log Mode Events?", defaultValue: true, required: true
        input "prefLogHubProperties", "bool", title:"Log Hub Properties?", defaultValue: true, required: true
        input "prefLogLocationProperties", "bool", title:"Log Location Properties?", defaultValue: true, required: true
    }
    
    section("Devices To Monitor:") {
        input "locks", "capability.lock", title: "Locks", multiple: true, required: false
        input "switches", "capability.switch", title: "Switches", multiple: true, required: false
    }

}

/*****************************************************************************************************************
 *  SmartThings System Commands:
 *****************************************************************************************************************/

/**
 *  installed()
 *
 *  Runs when the app is first installed.
 **/
def installed() {
    state.installedAt = now()
    state.loggingLevelIDE = 5
    log.debug "${app.label}: Installed with settings: ${settings}" 
}

/**
 *  uninstalled()
 *
 *  Runs when the app is uninstalled.
 **/
def uninstalled() {
    logger("uninstalled()","trace")
}

/**
 *  updated()
 * 
 *  Runs when app settings are changed.
 * 
 *  Updates device.state with input values and other hard-coded values.
 *  Builds state.deviceAttributes which describes the attributes that will be monitored for each device collection 
 *  (used by manageSubscriptions() and softPoll()).
 *  Refreshes scheduling and subscriptions.
 **/
def updated() {
    logger("updated()","trace")

    // Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    
    // Database config:
    state.serverHost = settings.prefHost
    state.serverPort = settings.prefPort
    
    state.headers = [:] 
    state.headers.put("HOST", "${state.serverHost}:${state.serverPort}")
    state.headers.put("Content-Type", "application/x-www-form-urlencoded")
    
    // Build array of device collections and the attributes we want to report on for that collection:
    //  Note, the collection names are stored as strings. Adding references to the actual collection 
    //  objects causes major issues (possibly memory issues?).
    state.deviceAttributes = []
    state.deviceAttributes << [ devices: 'locks', attributes: ['lock']]
	state.deviceAttributes << [ devices: 'switches', attributes: ['switch']]
    // Configure Scheduling:
    state.softPollingInterval = settings.prefSoftPollingInterval.toInteger()
    manageSchedules()
    
    // Configure Subscriptions:
    manageSubscriptions()
}

/*****************************************************************************************************************
 *  Event Handlers:
 *****************************************************************************************************************/

/**
 *  handleAppTouch(evt)
 * 
 *  Used for testing.
 **/
def handleAppTouch(evt) {
    logger("handleAppTouch()","trace")
    
    softPoll()
}

/**
 *  handleModeEvent(evt)
 * 
 *  Log Mode changes.
 **/
def handleModeEvent(evt) {
    logger("handleModeEvent(): Mode changed to: ${evt.value}","info")

    def locationId = location.id
    def locationName = location.name
    def mode = '"' + evt.value + '"'
	def data = "_stMode,locationId=${locationId},locationName=${locationName} mode=${mode}"
    postToServer(data)
}

/**
 *  handleEvent(evt)
 *
 *  Builds data to send to InfluxDB.
 *   - Escapes and quotes string values.
 *   - Calculates logical binary values where string values can be 
 *     represented as binary values (e.g. contact: closed = 1, open = 0)
 * 
 *  Useful references: 
 *   - http://docs.smartthings.com/en/latest/capabilities-reference.html
 *   - https://docs.influxdata.com/influxdb/v0.10/guides/writing_data/
 **/
def handleEvent(evt) {
    logger("handleEvent(): $evt.displayName($evt.name:$evt.unit) $evt.value","info")
    
    // Build data string to send to InfluxDB:
    //  Format: <measurement>[,<tag_name>=<tag_value>] field=<field_value>
    //    If value is an integer, it must have a trailing "i"
    //    If value is a string, it must be enclosed in double quotes.
    def measurement = evt.name
    // tags:
    def deviceId = evt.deviceId
    def deviceName = evt.displayName
    def groupId = evt?.device.device.groupId
    def groupName = getGroupName(evt?.device.device.groupId)
    def hubId = evt?.device.device.hubId
    def hubName = evt?.device.device.hub.toString()
    
    // Don't pull these from the evt.device as the app itself will be associated with one location.
    def locationId = location.id
    def locationName = location.name

    def unit = evt.unit
    def value = evt.value
    def valueBinary = ''
    
    def data = "${measurement},deviceId=${deviceId},deviceName=${deviceName},groupId=${groupId},groupName=${groupName},hubId=${hubId},hubName=${hubName},locationId=${locationId},locationName=${locationName}"
     logger("handleEvent() data: ${data}","info")
    // Unit tag and fields depend on the event type:
    //  Most string-valued attributes can be translated to a binary value too.
    
    // door: Calculate a binary value (locked = 1, unlocked = 0)
    if ('lock' == evt.name) { 
        unit = 'lock'
        value = '"' + value + '"'
        data += ",unit=${unit} value=${value}"
    }
    else if ('switch' == evt.name) { // switch: Calculate a binary value (on = 1, off = 0)
        unit = 'switch'
        value = '"' + value + '"'
        data += ",unit=${unit} value=${value}"
    }
    // Post data to InfluxDB:
    postToServer(data)

}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/

/**
 *  softPoll()
 *
 *  Executed by schedule.
 * 
 *  Forces data to be posted to InfluxDB (even if an event has not been triggered).
 *  Doesn't poll devices, just builds a fake event to pass to handleEvent().
 *
 *  Also calls LogSystemProperties().
 **/
def softPoll() {
    logger("softPoll()","trace")
    
    logSystemProperties()
    
    // Iterate over each attribute for each device, in each device collection in deviceAttributes:
    def devs // temp variable to hold device collection.
    state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"
        if (devs && (da.attributes)) {
            devs.each { d ->
                da.attributes.each { attr ->
                    if (d.hasAttribute(attr) && d.latestState(attr)?.value != null) {
                        logger("softPoll(): Softpolling device ${d} for attribute: ${attr}","info")
                        // Send fake event to handleEvent():
                        handleEvent([
                            name: attr, 
                            value: d.latestState(attr)?.value,
                            unit: d.latestState(attr)?.unit,
                            device: d,
                            deviceId: d.id,
                            displayName: d.displayName
                        ])
                    }
                }
            }
        }
    }

}

/**
 *  logSystemProperties()
 *
 *  Generates measurements for SmartThings system (hubs and locations) properties.
 **/
def logSystemProperties() {
    logger("logSystemProperties()","trace")

    def locationId = '"' + location.id + '"'
    def locationName = '"' + location.name + '"'

	// Location Properties:
    if (prefLogLocationProperties) {
        try {
            def tz = '"' + location.timeZone.ID + '"'
            def mode = '"' + location.mode + '"'
            def hubCount = location.hubs.size()
            def times = getSunriseAndSunset()
            def srt = '"' + times.sunrise.format("HH:mm", location.timeZone) + '"'
            def sst = '"' + times.sunset.format("HH:mm", location.timeZone) + '"'

            def data = "_stLocation,locationId=${locationId},locationName=${locationName},latitude=${location.latitude},longitude=${location.longitude},timeZone=${tz} mode=${mode},hubCount=${hubCount}i,sunriseTime=${srt},sunsetTime=${sst}"
            postToServer(data)
        } catch (e) {
		    logger("logSystemProperties(): Unable to log Location properties: ${e}","error")
        }
	}

	// Hub Properties:
    if (prefLogHubProperties) {
       	location.hubs.each { h ->
        	try {
                def hubId = '"' + h.id + '"'
                def hubName = '"' + h.name + '"'
                def hubIP = '"' + h.localIP + '"'
                def hubStatus = '"' + h.status + '"'
                def batteryInUse = ("false" == h.hub.getDataValue("batteryInUse")) ? "0i" : "1i"
                def hubUptime = h.hub.getDataValue("uptime") + 'i'
                def zigbeePowerLevel = h.hub.getDataValue("zigbeePowerLevel") + 'i'
                def zwavePowerLevel =  '"' + h.hub.getDataValue("zwavePowerLevel") + '"'
                def firmwareVersion =  '"' + h.firmwareVersionString + '"'

                def data = "_stHub,locationId=${locationId},locationName=${locationName},hubId=${hubId},hubName=${hubName},hubIP=${hubIP} "
                data += "status=${hubStatus},batteryInUse=${batteryInUse},uptime=${hubUptime},zigbeePowerLevel=${zigbeePowerLevel},zwavePowerLevel=${zwavePowerLevel},firmwareVersion=${firmwareVersion}"
                postToServer(data)
            } catch (e) {
				logger("logSystemProperties(): Unable to log Hub properties: ${e}","error")
        	}
       	}

	}

}

/**
 *  postToServer()
 *
 *  Posts data to Server.
 *
 *  Uses hubAction instead of httpPost() in case server is on the same LAN as the Smartthings Hub.
 **/
def postToServer(data) {
    logger("postToServer(): Posting data to Server: Host: ${state.databaseHost}, Port: ${state.databasePort}, Database: ${state.databaseName}, Data: [${data}]","debug")
    
    try {
        def hubAction = new physicalgraph.device.HubAction(
        	[
                method: "POST",
                path: state.path,
                body: data,
                headers: state.headers
            ],
            null,
            [ callback: handleInfluxResponse ]
        )
		
        sendHubCommand(hubAction)
    }
    catch (Exception e) {
		logger("postToServer(): Exception ${e} on ${hubAction}","error")
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  manageSchedules()
 * 
 *  Configures/restarts scheduled tasks: 
 *   softPoll() - Run every {state.softPollingInterval} minutes.
 **/
private manageSchedules() {
	logger("manageSchedules()","trace")

    // Generate a random offset (1-60):
    Random rand = new Random(now())
    def randomOffset = 0
    
    // softPoll:
    try {
        unschedule(softPoll)
    }
    catch(e) {
        // logger("manageSchedules(): Unschedule failed!","error")
    }

    if (state.softPollingInterval > 0) {
        randomOffset = rand.nextInt(60)
        logger("manageSchedules(): Scheduling softpoll to run every ${state.softPollingInterval} minutes (offset of ${randomOffset} seconds).","trace")
        schedule("${randomOffset} 0/${state.softPollingInterval} * * * ?", "softPoll")
    }
    
}

/**
 *  manageSubscriptions()
 * 
 *  Configures subscriptions.
 **/
private manageSubscriptions() {
	logger("manageSubscriptions()","trace")

    // Unsubscribe:
    unsubscribe()
    
    // Subscribe to App Touch events:
    subscribe(app,handleAppTouch)
    
    // Subscribe to mode events:
    if (prefLogModeEvents) subscribe(location, "mode", handleModeEvent)
    
    // Subscribe to device attributes (iterate over each attribute for each device collection in state.deviceAttributes):
    def devs // dynamic variable holding device collection.
    state.deviceAttributes.each { da ->
        devs = settings."${da.devices}"
        if (devs && (da.attributes)) {
            da.attributes.each { attr ->
                logger("manageSubscriptions(): Subscribing to attribute: ${attr}, for devices: ${da.devices}","info")
                // There is no need to check if all devices in the collection have the attribute.
                subscribe(devs, attr, handleEvent)
            }
        }
    }
}

/**
 *  logger()
 *
 *  Wrapper function for all logging.
 **/
private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error msg
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn msg
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info msg
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug msg
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace msg
            break

        default:
            log.debug msg
            break
    }
}

/**
 *  getGroupName()
 *
 *  Get the name of a 'Group' (i.e. Room) from its ID.
 *  
 *  This is done manually as there does not appear to be a way to enumerate
 *  groups from a SmartApp currently.
 * 
 *  GroupIds can be obtained from the SmartThings IDE under 'My Locations'.
 *
 *  See: https://community.smartthings.com/t/accessing-group-within-a-smartapp/6830
 **/
private getGroupName(id) {

    if (id == null) {return 'Home'}
    else if (id == 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX') {return 'Kitchen'}
    else if (id == 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX') {return 'Lounge'}
    else if (id == 'XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX') {return 'Office'}
    else {return 'Unknown'}    
}

