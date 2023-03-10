/**
 *  StreamLabs Water Flow SM
 *  Smart App/ Service Manager for StreamLabs Water Flow Meter
 *  This will create a companion Device Handler for the StreamLabs device
 *  Version 1.0
 *
 *  You MUST enter the API Key value via 'App Settings'->'Settings'->'api_key' in IDE by editing this SmartApp code
 *  This key is provided by StreamLabs upon request
 *
 *  Copyright 2019 Bruce Andrews
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
    name: "StreamLabs Water Flow SM",
    namespace: "windsurfer99",
    author: "windsurfer99",
    description: "Service Manager for cloud-based API for StreamLabs Water Flow meter",
    category: "My Apps",
    iconUrl: "https://windsurfer99.github.io/ST_StreamLabs-Water-Flow/tap-water-icon-128.png",
    iconX2Url: "https://windsurfer99.github.io/ST_StreamLabs-Water-Flow/tap-water-icon-256.png",
    iconX3Url: "https://windsurfer99.github.io/ST_StreamLabs-Water-Flow/tap-water-icon-256.png",
 	singleInstance: true) {
    appSetting "api_key"
}

preferences {
	page(name: "pageOne", title: "Options", uninstall: true, install: true) {
		section("Inputs") {
        		paragraph ("You MUST set the API Key via App Settings in IDE")
            		label (title: "Assign a name for Service Manager", required: false, multiple: true)
            		input (name: "SL_awayModes", type: "mode", title: "Enter SmartThings modes when water meter should be Away",
                    		multiple: true, required: false)
            		input (name: "SL_locName", type: "text", title: "Enter Streamlabs location name assigned to Streamlabs flow meter",
                    		multiple: false, required: true)
                    input (name: "configLoggingLevelIDE",
                        title: "IDE Live Logging Level:\nMessages with this level and higher will be logged to the IDE.",
                        type: "enum",
                        options: [
                            "0" : "None",
                            "1" : "Error",
                            "2" : "Warn",
                            "3" : "Info",
                            "4" : "Debug",
                            "5" : "Trace"
                        ],
                        defaultValue: "3",
                        displayDuringSetup: true,
                        required: false
                    )
		}
	}
}

//required methods
def installed() {
	log.debug "StreamLabs SM installed with settings: ${settings}"
    state.enteredLocName = SL_locName //save off the location name entered by user
    runIn(3, "initialize")
}


def updated() {
    if (state.enteredLocName != SL_locName) { //if location name changed, need to make a new device
    	logger("StreamLabs SM updated() called- new device with settings: ${settings}","trace")
        unsubscribe()
        cleanup()
    	runIn(10, "initialize") //deleteChildDevice seems to take a while to delete; wait before re-creating
    } else {
	   	state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
		logger("StreamLabs SM updated() called- same name, no new device with settings: ${settings}","info")
    }
}

def initialize() {
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    logger("StreamLabs SM initialize() called with settings: ${settings}","trace")
	// get the value of api key
	def mySecret = appSettings.api_key
    if (mySecret.length() <40) {
    	logger("StreamLabs SM initialize- api_key value not set properly in IDE: ${mySecret}","error")
    }
    state.SL_location = null
    state.childDevice = null
    state.inAlert = false
    state.homeAway = "home"
    subscribe(location, "mode", modeChangeHandler)
    initSL_Locations() //determine Streamlabs location to use
    if (state.SL_location) {
    	//we have a device; put it into initial state
        def eventData = [name: "water", value: "dry"]
    	def existingDevice = getChildDevice(state.SL_location?.locationId)
        existingDevice?.generateEvent(eventData)
        state.inAlert =  false
        schedule("0 0/3 * * * ?", pollSLAlert) //Poll Streamlabs cloud for leak alert
        runIn(8,"initDevice") //update once things are initialized
    }
}

def uninstalled() {
    logger("StreamLabs SM uninstalled() called","trace")
    cleanup()
}


//remove things
def cleanup() {
    logger("StreamLabs SM cleanup() called","trace")
    def SL_Devices = getChildDevices()
    SL_Devices.each {
    	logger("StreamLabs SM cleanup- deleting SL deviceNetworkID: ${it.deviceNetworkId}","info")
    	try {
            deleteChildDevice(it.deviceNetworkId)
        }
    	catch (e) {
    		logger("StreamLabs SM cleanup- caught and ignored deleting child device: {it.deviceNetworkId}: $e","info")
       	}
    }
    state.SL_location = null
    state.childDevice = null
    state.inAlert = false
}

//Handler for schedule; determine if there are any alerts
def pollSLAlert() {
    logger("StreamLabs SM pollSLAlert() called","trace")
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	if (state.SL_location){
        def params = [
                uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
                headers: ['Authorization': 'Bearer ' + appSettings.api_key],
                contentType: 'application/json',
                ]
        try {
            httpGet(params) {resp ->
    			logger("StreamLabs SM pollSLAlert resp.data: ${resp.data}","debug")
                def SL_Leak_Found = false //initialize
                def resp_data = resp.data
                //def SL_locationsAlert = resp_data.alerts[0]
                def SL_locationsAlerts = resp_data.alerts
                if (SL_locationsAlerts.size > 0) {
                    logger("StreamLabs SM pollSLAlert # of alerts: ${SL_locationsAlerts.size}","trace")
                    SL_locationsAlerts.each{
                        //go through all active alerts to see if any are leaks
                        if (it.active ==true) {
                            logger("StreamLabs SM pollSLAlert, alert is active: ${it}","trace")
                            if(it.type.contains('Leak')){
                                //found a leak alert
                                //send wet event to child device handler every poll to ensure not lost due to handler pausing
                				logger("StreamLabs SM pollSLAlert Leak Alert received: ${it}; call changeWaterToWet","info")
                                existingDevice?.changeWaterToWet()
                                state.inAlert =  true
                                SL_Leak_Found = true
                            }
                        }
                    }
                }
//                if (SL_locationsAlert) {
                    //send wet event to child device handler every poll to ensure not lost due to handler pausing
//    				logger("StreamLabs SM pollSLAlert Alert0 received: ${SL_locationsAlert}; call changeWaterToWet","info")
//                    existingDevice?.changeWaterToWet()
//                    state.inAlert =  true
//                } else {
                if (state.inAlert && !SL_Leak_Found){
                    //alert removed, send dry event to child device handler only once
					logger("StreamLabs SM pollSLAlert Leak Alert deactivated ; call changeWaterToDry","info")
                    existingDevice?.changeWaterToDry()
                    state.inAlert =  false
                }
//                }
            }
        } catch (e) {
    		logger("StreamLabs SM pollSLAlert error retrieving alerts: $e","error")
        }
    }
}

//callback in order to initialize device
def initDevice() {
    logger("StreamLabs SM initDevice() called","trace")
	determineFlows()
    determinehomeAway()
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	existingDevice?.refresh()
}

//determine flow totals from cloud
def determineFlows() {
    logger("StreamLabs SM determineFlows() called","trace")
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	if (existingDevice){
        def params = [
                uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId + '/readings/water-usage/summary',
                headers: ['Authorization': 'Bearer ' + appSettings.api_key],
                contentType: 'application/json',
                ]
        try {
            httpGet(params) {resp ->
    			logger("StreamLabs SM determineFlows resp.data: ${resp.data}","debug")
                def resp_data = resp.data
                state.todayFlow = resp_data?.today
                state.thisMonthFlow = resp_data?.thisMonth
                state.thisYearFlow = resp_data?.thisYear
                state.unitsFlow = resp_data?.units
            }
        } catch (e) {
    		logger("StreamLabs SM determineFlows error retrieving summary data","error")
            state.todayFlow = 0
            state.thisMonthFlow = 0
            state.thisYearFlow = 0
            state.unitsFlow = "gallons"
        }
    }
}

//determine StreamLabs home/away from StreamLabs cloud
def determinehomeAway() {
    logger("StreamLabs SM determinehomeAway() called","trace")
    def existingDevice = getChildDevice(state.SL_location?.locationId)
	if (existingDevice){
        def params = [
                uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
                headers: ['Authorization': 'Bearer ' + appSettings.api_key],
                contentType: 'application/json',
                ]
		try {
            httpGet(params) {resp ->
    			logger("StreamLabs SM determinehomeAway resp.data: ${resp.data}; resp.status: ${resp.status}","debug")
                if (resp.status == 200){//successful retrieve
                    def resp_data = resp.data
                    state.homeAway = resp_data?.homeAway
                }
            }
        } catch (e) {
    		logger("StreamLabs SM determinehomeAway error: $e","error")
        }
    }
}


//Get desired location from Streamlabs cloud based on user's entered location's name
def initSL_Locations() {
    logger("StreamLabs SM initSL_Locations() called","trace")
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations',
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
    ]
    state.SL_location = null

	try {
        httpGet(params) {resp ->
            def resp_data = resp.data
            def SL_locations0 = resp_data.locations[0]
            def ttl = resp_data.total
		    logger("StreamLabs SM initSL_Locations- total SL_locations: ${ttl}","debug")
            resp.data.locations.each{ SL_loc->
                if (SL_loc.name.equalsIgnoreCase(SL_locName)) { //Let user enter without worrying about case
                	state.SL_location = SL_loc
                }
            }
            if (!state.SL_location) {
		    	logger("StreamLabs SM in initSL_Locations- StreamLabs location name: ${SL_locName} not found!","warn")
            } else {
            	//save current homeAway status
                state.homeAway = state.SL_location.homeAway
                //create device handler for this location (device)
                def existingDevice = getChildDevice(state.SL_location.locationId)
                if(!existingDevice) {
                    def childDevice = addChildDevice("windsurfer99", "StreamLabs Water Flow DH",
                          state.SL_location.locationId, null, [name: "Streamlabs Water Flow DH",
                          label: "Streamlabs Water Flow DH", completedSetup: true])
		    		logger("StreamLabs SM initSL_Locations- device created with Id: ${state.SL_location.locationId} for SL_location: ${state.SL_location.name}","info")
                } else {
		    		logger("StreamLabs SM initSL_Locations- device not created; already exists: ${existingDevice.getDeviceNetworkId()}","warn")
                }
            }
       }
    } catch (e) {
		logger("StreamLabs SM error in initSL_Locations retrieving locations: $e","error")
    }
}

//Method to set Streamlabs homeAway status; called with 'home' or 'away'
def updateAway(newHomeAway) {
	logger("StreamLabs SM updateAway() called with newHomeAway: ${newHomeAway}","trace")
    def cmdBody = [
			"homeAway": newHomeAway
	]
    def params = [
            uri:  'https://api.streamlabswater.com/v1/locations/' + state.SL_location.locationId,
            headers: ['Authorization': 'Bearer ' + appSettings.api_key],
            contentType: 'application/json',
			body : new groovy.json.JsonBuilder(cmdBody).toString()
            ]

	logger("StreamLabs SM updateAway params: ${params}","info")

	try {
        httpPutJson(params){resp ->
			logger("StreamLabs SM updateAway resp data: ${resp.data}","info")
			logger("StreamLabs SM updateAway resp status: ${resp.status}","info")
            if (resp.status == 200){//successful change
                def eventData = [name: "homeAway", value: newHomeAway]
                def existingDevice = getChildDevice(state.SL_location?.locationId)
                existingDevice?.generateEvent(eventData) //tell child device new home/away status
                state.homeAway = newHomeAway
           }
        }
    } catch (e) {
		logger("StreamLabs SM error in updateAway: $e","error")
    }
}

//handler for when SmartThings mode changes
//if new mode is one of the ones specified for a StreamLabs away mode, change Streamlabs to away
//Do nothing if the user hasn't selected any modes defined as being Streamlabs away.
def modeChangeHandler(evt) {
	logger("StreamLabs SM modeChangeHandler() called by SmartThings mode changing to: ${evt.value}","info")
    //log.debug "StreamLabs SM SmartThings mode changed to ${evt.value}"
    //log.debug "SL_awayModes: ${SL_awayModes}"
    //log.debug "location.currentMode: ${location.currentMode}"
	def foundmode = false
	logger("StreamLabs SM modeChangeHandler- user specified SL_awayModes: ${SL_awayModes}; # of modes: ${SL_awayModes?.size}","debug")
    if (SL_awayModes?.size() > 0) {//only do something if user specified some modes
        SL_awayModes?.each{ awayModes->
            if (location.currentMode == awayModes) {
                foundmode = true //new mode is one to set Streamlabs to away
            }
        }
        if (foundmode) {
            //change to away
			logger("StreamLabs SM modeChangeHandler- changing StreamLabs to away","info")
            updateAway("away")
        } else {
            //change to home; new mode isn't one specified for Streamlabs away
			logger("StreamLabs SM modeChangeHandler- changing StreamLabs to home","info")
            updateAway("home")
        }
    }
}

// Child called methods

// return current flow totals, etc.
Map retrievecloudData() {
	logger("StreamLabs SM retrievecloudData() called","trace")
    //get latest data from cloud
    determinehomeAway()
    determineFlows()
    pollSLAlert()
	return ["todayFlow":state.todayFlow, "thisMonthFlow":state.thisMonthFlow,
      "thisYearFlow":state.thisYearFlow, "homeAway":state.homeAway, "inAlert":state.inAlert]
}

//delete child device; called by child device to remove itself. Seems unnecessary but documentation says to do this
def	deleteSmartLabsDevice(deviceid) {
	logger("StreamLabs SM deleteSmartLabsDevice() called with deviceid: ${deviceid}","trace")
    def SL_Devices = getChildDevices()
    SL_Devices?.each {
    	if (it.deviceNetworkId == deviceid) {
			logger("StreamLabs SM deleteSmartLabsDevice- deleting SL deviceNetworkID: ${it.deviceNetworkId}","info")
            try {
                deleteChildDevice(it.deviceNetworkId)
                sendEvent(name: "DeviceDelete", value: "${it.deviceNetworkId} deleted")
            }
            catch (e) {
				logger("StreamLabs SM deleteSmartLabsDevice- caught and ignored deleting child device: {it.deviceNetworkId} during cleanup: $e","info")
            }
        }
    }
}

/**
 *  logger()
 *
 *  Wrapper function for all logging. Thanks codersaur.
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
