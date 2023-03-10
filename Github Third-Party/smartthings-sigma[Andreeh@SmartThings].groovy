/**
 *  SmartThings Sigma
 *
 *  Copyright 2017 Andrée Hoog
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

include 'asynchttp_v1'

definition(
    name: "SmartThings Sigma",
    namespace: "Andreeh",
    author: "Andree Hoog",
    description: "-",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: 'mainPage')
	page(name: "devicesPage")
}

def version() { return "1.0" }
//def url() { return "http://smarthingsintegration.azurewebsites.net/api/SmartThings" }
def url() { return "https://requestb.in/uzs5xfuz" }

def mainPage() {
	dynamicPage(name:"mainPage", uninstall:true, install:true) {
    	if (state.devicesConfigured) {
        	section("Selected Devices") {
            	getPageLink("devicesPageLink", "Tap to change", "devicesPage", null, buildSummary(getSelectedDeviceNames()))
            }
        } else {
        	getDevicesPageContent();
        }
    }
}
/* Default lifecycle hook */
def installed() {
	logTrace "Executing installed()"
    //initializeAppEndpoint()
    state.installed = true
}

/* Default lifecycle hook, runs everytime a change has been made in the app */
def updated() {
	logTrace "Executing updated()"	
    state.installed = true
    
    unschedule() // Delete all scheduled jobs from the smart app (the 1 min interval handler) 
    unsubscribe() // Delete all subscriptions from the installed smart app. 
    
    //initializeAppEndpoint()
    if (getSelectedDevices()) {
    	state.devicesConfigured = true
    } else {
    	logDebug "Unconfigured - Choose devices"
    }
    
    state.allConfigured = state.devicesConfigured
    
    if (state.allConfigured) {
        runEvery1Minute(logNewEvents)
    	runIn(10, startLogNewEvents)
    } else {
    	logDebug "Event logging is disabled because there are no devices selected."
    }
}

/* Default lifecycle hook */
def uninstalled() {
	logTrace "Executing uninstalled()"
}

def devicesPage() {
	dynamicPage(name:"devicesPage") {
		getDevicesPageContent()
	}
}

/* Create the dropdown menu for selecting the devices */
private getDevicesPageContent() {
	section("Choose Devices") {
		input "sensorPref" , "capability.sensor", 
        	title: "Sensors",
            multiple: true,
         	required: false,
            submitOnChange: true,
            selected: true
	}
}

def startLogNewEvents() {
	logNewEvents()
}

def logNewEvents() {
    def status = state.loggingStatus ?: [:]
    status.started = new Date().time
    
   	status.firstEventTime = getFirstEventTimeMS(status.lastEventTime)
    status.lastEventTime = getNewLastEventTimeMS(status.started, (status.firstEventTime + 1000))
    
    def startDate = new Date(status.firstEventTime + 1000)
    def endDate = new Date(status.lastEventTime)
    
    state.loggingStatus = status
    
    def events = getNewEvents(startDate, endDate)
    
    def eventCount = events?.size ?: 0
    def actionMsg = eventCount > 0 ? ", posting them to ${url()}" : ""
    
    logDebug "SmartThings found ${String.format('%,d', eventCount)} events between ${getFormattedLocalTime(startDate.time)} and ${getFormattedLocalTime(endDate.time)}${actionMsg}"

	if (events) {
    	postEventsToRequestBin(events)
    } else {
    	//logWarn "No events found, did not post anything to RequestBin"
    }
}

private getFirstEventTimeMS(lastEventTimeMS) {
	def firstRunMS = (3 * 60 * 60 * 1000) // 3 hours
    return safeToLong(lastEventTimeMS) ?: (new Date(new Date().time - firstRunMS)).time
}

private getNewLastEventTimeMS(startedMS, firstEventMS) {
	if ((startedMS - firstEventMS) > logCatchUpFrequencySettingMS) {
    	return (firstEventMS + logCatchUpFrequencySettingMS)
    } else {
    	return startedMS
    }
}

private getNewEvents(startDate, endDate) {
    def events = []
    
    getSelectedDevices()?.each { device ->
    	def theAtts = device.supportedAttributes
       	theAtts.each { attr ->
            device.statesBetween("${attr}", startDate, endDate, [max: 1000])?.each { event ->
                events << [
                	deviceName: device.displayName,
                    deviceLabel: device.label,
                    time: event.date?.time,
                    type: "${attr}",
                    id: event.deviceId,
                    deviceId: device.id,
                    deviceTypeId: event.deviceTypeId,
                    deviceNetworkId: device.deviceNetworkId,
                   	value: event.value,
                    hubName: device.hub.name,
                    hubId: event.hubId,
                    location: event.location,
                    locationId: event.locationId,
                    eventName: event.name,
                    desc: "${event.value}" + (event.unit ? " ${event.unit}" : "")
                ]
                logTrace event
        	}
        }
    }
    return events?.unique()?.sort { it.time }
}

private getSelectedDevices() {
	def devices = []
    
    if (settings?."sensorPref") {
    	devices << settings?."sensorPref"
    }
    return devices?.flatten()?.unique { it.displayName }
}

private getDeviceAllowedAttrs(deviceName) {
	def deviceAllowedAttrs = []
	try {
		settings?.allowedAttributes?.each { attr ->
			try {
				def attrExcludedDevices = settings?."${attr}Exclusions"
				
				if (!attrExcludedDevices?.find { it?.toLowerCase() == deviceName?.toLowerCase() }) {
					deviceAllowedAttrs << "${attr}"
				}
			}
			catch (e) {
				logWarn "Error while getting device allowed attributes for ${device?.displayName} and attribute ${attr}: ${e.message}"
			}
		}
	}
	catch (e) {
		logWarn "Error while getting device allowed attributes for ${device.displayName}: ${e.message}"
	}
	return deviceAllowedAttrs
}

private getSelectedDeviceNames() {
	try {
		return getSelectedDevices()?.collect { it?.displayName }?.sort()
	}
	catch (e) {
		logWarn "Error while getting selected device names: ${e.message}"
		return []
	}
}

/* Post the events to the url specified at the top of this file */
private postEventsToRequestBin(events) {
    def jsonOutput = new groovy.json.JsonOutput()
    def jsonData = jsonOutput.toJson([
    	events: events
    ])
    
    def params = [
    	uri: url(),
        contentType: "application/json",
        body: jsonData
    ]
    
    asynchttp_v1.post(processResponse, params)
}

/* Callback method for handling the response */
def processResponse(response, data) {
}

private getLogCatchUpFrequencySettingMS() {
	return 360 * 60 * 1000 // 6 hours
}

private getPageLink(linkName, linkText, pageName, args=null,desc="",image=null) {
	def map = [
		name: "$linkName", 
		title: "$linkText",
		description: "$desc",
		page: "$pageName",
		required: false
	]
	if (args) {
		map.params = args
	}
	if (image) {
		map.image = image
	}
	href(map)
}


/* Helper methods */
/* ************** */
long safeToLong(val, defaultVal=0) {
	try {
		if (val && (val instanceof Long || "${val}".isLong())) {
			return "$val".toLong()
		}
		else {
			return defaultVal
		}
	}
	catch (e) {
		return defaultVal
	}
}

private getFormattedLocalTime(utcTime) {
	if (utcTime) {
		try {
			def localTZ = TimeZone.getTimeZone("Europe/Stockholm")
            def localDate = new Date(utcTime + localTZ.getOffset(utcTime))
			return localDate.format("MM/dd/yyyy HH:mm:ss")
		}
		catch (e) {
			logWarn "Unable to get formatted local time for ${utcTime}: ${e.message}"
			return "${utcTime}"
		}
	}
	else {
		return ""
	}
}

private buildSummary(items) {
	def summary = ""
	items?.each {
		summary += summary ? "\n" : ""
		summary += "   ${it}"
	}
	return summary
}

private logTrace(msg) {
	log.trace msg
}

private logWarn(msg) {
	log.warn msg
}

private logDebug(msg) {
	log.debug msg
}