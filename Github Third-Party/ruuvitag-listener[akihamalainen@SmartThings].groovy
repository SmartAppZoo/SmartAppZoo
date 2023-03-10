/*****************************************************************************************************************
 *  Copyright Aki Hämäläinen
 *
 *  Name: RuuviTag Listener
 *
 *  Date: 2019-12-27
 *
 *  Version: 0.1
 *
 *  Source: https://github.com/akihamalainen/SmartThings/tree/master/smartapps/ruuvitag-listener/ruuvitag-listener.groovy
 *
 *  Author: Aki Hämäläinen
 *
 *  Description: A SmartApp to periodically retrieve RuuviTag measurements from InfluxDB database. 
 * 				 Should be used together with RuuviTag device handler:  
 *               https://github.com/akihamalainen/SmartThings/tree/master/devices/ruuvitag/ruuvitag.groovy
 *
 *  			 More information about RuuviTags: https://ruuvi.com/
 *
 *  			 Big Kudos to David Lomas (codersaur) and to his 'InfluxDB logger' SmartApp from which this SmartApp was derived from
 *  			 More info: https://github.com/codersaur/SmartThings/blob/master/smartapps/influxdb-logger/influxdb-logger.groovy and
 *  			 https://community.smartthings.com/t/release-influxdb-logger/64919
 *  License:
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
 /*****************************************************************************************************************/
definition(
    name: "RuuviTag Listener",
    namespace: "aki.hamalainen",
    author: "Aki Hamalainen",
    description: "Get RuuviTag sensor data out of InfluxDB to be used in Smartthings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("General:") {
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
        	defaultValue: "5",
            displayDuringSetup: true,
        	required: false
        )
    }

    section ("InfluxDB Database:") {
        input "prefDatabaseHost", "text", title: "Host", defaultValue: "192.168.68.102", required: true
        input "prefDatabasePort", "text", title: "Port", defaultValue: "8086", required: true
        input "prefDatabaseName", "text", title: "Database Name", defaultValue: "ruuvi", required: true
        input "prefDatabaseUser", "text", title: "Username", required: false
        input "prefDatabasePass", "text", title: "Password", required: false
    }
    
    section("Polling:") {
        input "prefSoftPollingInterval", "number", title:"Soft-Polling interval (minutes)", defaultValue: 1, required: true
    }
    
    section("Devices To Monitor:") {
        input "ruuviTags", "capability.temperatureMeasurement", title: "RuuviTags", multiple: true, required: false
    }
}

def installed() {
	logger("Installed with settings: ${settings}", "info")
	state.installedAt = now()
    state.loggingLevelIDE = 5    
}

def updated() {
	logger("Updated with settings: ${settings}","info")

    // Update internal state:
    state.loggingLevelIDE = (settings.configLoggingLevelIDE) ? settings.configLoggingLevelIDE.toInteger() : 3
    
    // Database config:
    state.databaseHost = settings.prefDatabaseHost
    state.databasePort = settings.prefDatabasePort
    state.databaseName = settings.prefDatabaseName
    state.databaseUser = settings.prefDatabaseUser
    state.databasePass = settings.prefDatabasePass 
    
    state.path = "/query"
    state.query = [:] 
    state.query.put("pretty", "true")
    state.query.put("db", "${state.databaseName}")
    state.query.put("q","select \"mac\", \"name\", \"rssi\", \"batteryVoltage\", \"temperature\", \"humidity\", \"absoluteHumidity\", \"dewPoint\", \"airDensity\", \"pressure\" from \"ruuvi_measurements\" where \"time\" > now() - 7d group by \"mac\" order by \"time\" desc limit 1")
    
    state.headers = [:] 
    state.headers.put("HOST", "${state.databaseHost}:${state.databasePort}")
    if (state.databaseUser && state.databasePass) {
        state.headers.put("Authorization", encodeCredentialsBasic(state.databaseUser, state.databasePass))
    }

    unsubscribe()

	state.softPollingInterval = settings.prefSoftPollingInterval.toInteger()
    startPolling()
}

/*****************************************************************************************************************
 *  Main Commands:
 *****************************************************************************************************************/
def softPoll() {
    logger("softPoll()","trace")
        
    try {
        def hubAction = new physicalgraph.device.HubAction(
        	[
                method: "GET",
                path: state.path,
                headers: state.headers,
                query: state.query
            ],
            null,
            [ callback: handleInfluxResponse ]
        )
        
        logger("Posting data to InfluxDB: Host: ${state.databaseHost}, Port: ${state.databasePort}, Database: ${state.databaseName}, query: [${state.query}]","debug")
        sendHubCommand(hubAction)
    }
    catch (Exception e) {
		logger("postToInfluxDB(): Exception ${e} on ${hubAction}","error")
    }
}

def handleInfluxResponse(physicalgraph.device.HubResponse hubResponse) {
    logger("handleInfluxResponse()","trace")

	if(hubResponse.status == 200) {
        if(hubResponse.body != null) {
            def results = new groovy.json.JsonSlurper().parseText(hubResponse.body)
            
            ruuviTags.eachWithIndex{it, i ->
                logger("Processing RuuviTag $i: Name: ${it.getName()}, Label: ${it.getDisplayName()}, NetworkId: ${it.getDeviceNetworkId()}", "debug");
                
                if(it.getDeviceNetworkId().length() == 12) {
                	def macAddress = it.getDeviceNetworkId().substring(0)
                    def item = results?.results?.series[0].find{it.tags?.mac == macAddress}
                    
                    if(item && item.values[0] != null) {
                    	def value = getMeasurement(it, item, "temperature")
						if(value != null) {
                        	it.setTemperature(value)
                        }
                        
                        value = getMeasurement(it, item, "humidity")
						if(value != null) {
                        	it.setRelativeHumidity(value)
                        }
                        
                        value = getMeasurement(it, item, "batteryVoltage")
						if(value != null) {
                        	it.setVoltage(value)
                        }
                        
                        value = getMeasurement(it, item, "rssi")
						if(value != null) {
                        	it.setRSSI(value)
                        }
                    }
                    else{
                    	logger("Could not find measurements for the following RuuviTag: Name: ${it.getName()}, Label: ${it.getDisplayName()}, NetworkId: ${it.getDeviceNetworkId()}", "warn");
                    }
                }else {
                	logger("RuuviTag network ID must use the following pattern: 'v.MACADDRESS' (e.g., 'v.DF540BBBBF55')", "error")
                }
            }
        } 
    }
    else {
        logger("handleInfluxResponse(): Something went wrong! Response from InfluxDB: Status: ${hubResponse.status}, Headers: ${hubResponse.headers}, Body: ${hubResponse.body}","error")
    }
}


/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

def getMeasurement(it, item, measurementName) {
	def value = null

	if(item.columns.indexOf(measurementName) != -1) {
        value = item.values[0][item.columns.indexOf(measurementName)]
        logger("Set ${measurementName} of '${it.getName()}': ${value}", "trace")
    }
    else {
        logger("${app.label}: could not get temperature for '${it.getName()}'", "warn")
    }
    
    return value
}

private startPolling() {
	logger("startPolling()","trace")

    // Generate a random offset (1-60):
    Random rand = new Random(now())
    def randomOffset = 0
    
    if (state.softPollingInterval > 0) {
        randomOffset = rand.nextInt(60)
        logger("manageSchedules(): Scheduling softpoll to run every ${state.softPollingInterval} minutes (offset of ${randomOffset} seconds).","debug")
        schedule("${randomOffset} 0/${state.softPollingInterval} * * * ?", "softPoll")
    }
    
}

private logger(msg, level = "debug") {

    switch(level) {
        case "error":
            if (state.loggingLevelIDE >= 1) log.error("${app.label}: ${msg}")
            break

        case "warn":
            if (state.loggingLevelIDE >= 2) log.warn("${app.label}: ${msg}")
            break

        case "info":
            if (state.loggingLevelIDE >= 3) log.info("${app.label}: ${msg}")
            break

        case "debug":
            if (state.loggingLevelIDE >= 4) log.debug("${app.label}: ${msg}")
            break

        case "trace":
            if (state.loggingLevelIDE >= 5) log.trace("${app.label}: ${msg}")
            break

        default:
            log.debug("${app.label}: ${msg}")
            break
    }
}

/**
 *  encodeCredentialsBasic()
 *
 *  Encode credentials for HTTP Basic authentication.
 **/
private encodeCredentialsBasic(username, password) {
    return "Basic " + "${username}:${password}".encodeAsBase64().toString()
}