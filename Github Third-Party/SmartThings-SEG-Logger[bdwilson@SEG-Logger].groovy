/**
 * 
 *  SmartThings SmartApp Energy Logger for SmartEnergyGroup (SEG) 
 *
 *  Copyright 2015 Brian Wilson
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License  for 
 *  the specific language governing permissions and limitations under the License.
 *
 *  Thanks to those on the SmartThings community board for sample code and great examples (mainly
 *  http://community.smartthings.com/t/aeon-home-energy-meter-xively-logging-integration) 
 *
 *  Note: This should work for any SmartThings "EnergyMeter" device, however I wrote it for a HEMv1
 * 
 *  For installation notes, please see https://github.com/bdwilson/SEG-Logger
 *
 */


// Automatically generated. Make future change here.
definition (
                name: "SEG Energy Logger SmartApp for HEMv1",
                namespace: "bdwilson",
                author: "Brian Wilson",
                description: "SEG Energy Logger SmartApp for HEMv1",
                category: "My Apps",
                iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize",
                iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Electronics.electronics13-icn?displaySize=2x")

preferences {
    section("Log devices...") {
        input "energymeters", "capability.energyMeter", title: "Energy Meter", required: false, multiple: true
    }

    section ("SEG API ID...") {
        input "channelId", "text", title: "API ID"
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	/* 
	* This can either be used as a subscription-based app that waits for a power event
        * from your device, or you can schedule it to run every 1, 5, 15 minutes. To 
        * schedule it based on minutes, uncomment the 3 lines below and comment the 
        * energymeters subscription line.
        */
        //state.clear()
        //unschedule(checkSensors)
        //schedule("0 */1 * * * ?", "checkSensors")
       subscribe(energymeters, "power", appTouch)
	
       subscribe(app, appTouch)
       log.debug("Initalized")
}

def appTouch(evt) {
    log.debug "appTouch: $evt"
    checkSensors()
}



def checkSensors() {

    def logitems = []

    log.debug("Settings")
    log.debug settings

    for (t in settings.energymeters) {
        log.debug("Looking through energy meters")
        log.debug t.displayName
        log.debug t.latestValue("power")
        log.debug t.latestValue("energy")
        logitems.add([t.displayName, "power", t.latestValue("power")] )
        logitems.add([t.displayName, "energy", t.latestValue("energy")] )
        state[t.displayName + ".power"] = t.latestValue("power")
        state[t.displayName + ".energy"] = t.latestValue("energy")
    }

    logField2(logitems)

}

private getFieldMap(channelInfo) {
    def fieldMap = [:]
    channelInfo?.findAll { it.key?.startsWith("field") }.each { fieldMap[it.value?.trim()] = it.key }
    return fieldMap
}


private logField2(logItems) {
    def fieldvalues = ""
    log.debug("in logField2")
    log.debug logItems

    def postdata = ""
    def channelname = ""
    def count=0
    logItems.eachWithIndex() {item, i ->
		channelname = item[0].replace(" ","_")
                channelname = channelname.replace("(","")
                channelname = channelname.replace(")","")
                channelname = channelname.toLowerCase()
                def node = item[1]
                def val = item[2]
                postdata += "($node $val)"
                count++
		if (count == 2) { // publish on every power/energy combo.
    			// format https://smartenergygroups.com/api
    			// HTTP PUT "(site ID (node segmeter ? (p_1 567.00)(e_1 2.70)(p_2 402.00)(e_2 2.2)))"
    			postdata = "(site ${channelId} (node ${channelname} ? " + postdata + "))"
			log.debug postdata
                    	postData(postdata)	
                	postdata = ""
                    	log.debug count
                    	count=0
                }
    }
}

def postData(body) {
	def uri = "http://api.smartenergygroups.com/api_sites/stream"
       def params = [
        uri: uri,
        body: body
    ] 
    httpPut(params) {response -> parseHttpResponse(response)}
}

def parseHttpResponse(response) {
	log.debug "Request was successful, $response.status"
   	def doc = response.data
    log.debug "HTTP Data: ${doc}"
}
