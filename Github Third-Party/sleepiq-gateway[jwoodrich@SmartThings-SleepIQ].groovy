/**
 *  SleepIQ Gateway
 *
 *  Copyright 2018 Jason Woodrich (@jwoodrich on github)
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
    name: "SleepIQ Gateway",
    namespace: "jwoodrich",
    author: "Jason Woodrich",
    description: "Gateway to access SleepIQ data.",
    category: "My Apps",
    iconUrl: "https://i.imgur.com/SGXlm5j.png",
    iconX2Url: "https://i.imgur.com/NRN8SMo.png",
    iconX3Url: "https://i.imgur.com/NRN8SMo.png")


preferences {
	section("Credentials") {
		input(name:"login",type:"string",title:"E-mail Address",required:true)
		input(name:"password",type:"password",title:"Password",required:true)
	}
	section("Timing") {
		input(name:"pollfreq",type:"number",title:"Frequency (mins)",required:true,defaultValue:5)
	}
}

def updateSessionId() {
    state.sessionId=null
    state.cookies=[]
    def params = [
        uri: "https://api.sleepiq.sleepnumber.com/rest/login",
        body: [ login: login, password: password]
    ]
	log.debug "Logging into SleepIQ as ${login} at ${params.uri} ..."
    
    try {
        httpPutJson(params) { resp ->
            log.debug "response: ${resp}"
            resp.headers.each {
            	log.debug "response header - ${it.name}: ${it.value}"
                if (it.name.equalsIgnoreCase("set-cookie")) {
                    String[] tmp=it.value.split(";")
                    state.cookies.add(tmp[0])
                }
        	}
        	log.debug "response contentType: ${resp.contentType}"
        	log.debug "response data: ${resp.data}"
            
            if (resp.data["key"]) {
                state.sessionId=resp.data.key
            }
            return resp.data.key
        }
    } catch (e) {
        log.error "something went wrong: $e", e
    }
}

def poll() {
    def sides=["leftSide","rightSide"]
    log.debug "Poll: sessionId=${state['sessionId']} ..."
    if (state["sessionId"]==null) {
        updateSessionId()
    }
    def cookies = ""
    state.cookies.each { cookie -> cookies+=cookie+"; " }
    
    def params = [
        uri: "https://prod-api.sleepiq.sleepnumber.com/rest/bed/familyStatus?_k=${state.sessionId}",
        headers: [ "Cookie": cookies ]
    ]
	log.debug "Retrieving SleepIQ details at ${params.uri} ..."
    
    try {
        httpGet(params) { resp ->
            resp.headers.each {
            	log.debug "response header - ${it.name}: ${it.value}"
        	}
        	log.debug "response contentType: ${resp.contentType}"
        	log.debug "response data: ${resp.data}"
            
            resp.data.beds.each { bed ->
                sides.each { side ->
                    def devId="${bed.bedId}${side}"
                    def childDevice=getChildDevice(devId);
                    if (childDevice==null) {
                        log.debug "Failed to find child bed ${devId}, creating it ..."
                        childDevice=addChildDevice("jwoodrich", "SleepIQ Bed", devId, null, [name: devId, label: "SleepIQ Bed ${side}", completedSetup: true])
                    }
              		childDevice.sendEvent(name: "presence", value:bed[side].isInBed?"present":"not present")
              		childDevice.sendEvent(name: "sleeping", value:bed[side].isInBed?"sleeping":"not sleeping")
              		childDevice.sendEvent(name: "pressure", value:bed[side].pressure)
              		childDevice.sendEvent(name: "sleepNumber", value:bed[side].sleepNumber)
                }
            }
        }
    } catch (e) {
        log.error "Failed to retrieve SleepIQ data: $e", e
        if (e.getMessage().contains("Unauthorized")) {
            state.sessionId=null;
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
    schedule("0 */${pollfreq} * * * ?",poll)
    poll()
}
