/**
 *  Loop Energy (Connect)
 *
 *  Copyright 2016 Phil Dye
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
    name: "Loop Energy (Connect)",
    namespace: "PhilDye",
    author: "Phil Dye",
    description: "Connect your Loop Energy monitor to SmartThings",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/PhilDye/loop-energy/master/devicetypes/phildye/loop-energy.src/images/loop-energy.png",
    iconX2Url: "https://raw.githubusercontent.com/PhilDye/loop-energy/master/devicetypes/phildye/loop-energy.src/images/loop-energy.png",
    singleInstance: true
    )
    {
        appSetting "apiUrl"
    }


preferences {
	section("Title") {
		// TODO: put inputs here
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	unschedule()    
	initialize()
}

def uninstalled() {
    log.info("Uninstalling, removing child devices...")
    removeChildDevices(getChildDevices())
    log.debug "Loop Energy (Connect) Uninstalled"
}

private removeChildDevices(devices) {
    devices.each {
        deleteChildDevice(it.deviceNetworkId) // 'it' is default
    }
}

def initialize() {
    log.debug "Initializing with settings: ${settings}"
        
    def deviceListParams = [
        uri: appSettings.apiUrl,
        // path: "/",
        requestContentType: "application/json"
        ]

    def devices = httpGet(deviceListParams) { resp ->
    
        log.debug "got req: ${deviceListParams} resp: ${resp.data}"
        
        if(resp.status == 200 && resp.data) {
        
        	resp.data.each { device -> 
            	def dni = "${device?.serial}"
                state.dni = dni
            	log.debug "looking for ${getLoopEnergyDisplayName(dni)}"
        
                //Check if the discovered devices are already created
                def d = getChildDevice(dni)
                if (!d){
                	log.debug "not found, calling addChildDevice"
                    d = addChildDevice(app?.namespace, "Loop Energy", dni, null, [label: getNamePrefix(), completedSetup: true])
    				d.take()
                    log.debug "created ${d.displayName} with id ${dni}"
                }
                else {
                    log.debug "found device with id ${dni} already exists"
                }
         	}
        }
    }
 
 	log.debug "created ${devices.size()} devices"
    schedule("0 0/1 * * * ?","poll")
    //runEvery5Minutes("poll")

}

def poll() {
	log.debug "poll called"
  	def children = getChildDevices()
    children.each {
    	//log.debug(it)
    	refresh(it)
    }
}

def refresh(child=null)  {
    log.debug "refresh() called at ${now()}"
    log.debug "child is ${child}"
    def result = false
    def pollParams = [
            uri: appSettings.apiUrl,
            //path: "/"
            ]

    try {
        httpGet(pollParams) { resp ->

            log.info "refresh(child) >> req = ${pollParams} resp.status = ${resp.status}, resp.data = ${resp.data}"

            if(resp.status == 200 && resp.data) {
                def dni = "${getNamePrefix()}:${resp.data.serial.toString()}"
                def data = resp.data.data[0]
                // TODO deal with the array
                
                def t = new Date().parse("yyyy-MM-dd'T'HH:mm:ss.'000Z'", data.latestData)

                log.debug "sendingEvent to child ${dni}; data = ${data}"
                child?.sendEvent(name: "power", unit: "W", value: data.power)
                child?.sendEvent(name: "energy", unit: "kWh", value: data.totalEnergy)

                child?.sendEvent(name: "lastUpdated", value: t.format( 'dd/MM/yyyy HH:mm' ), displayed: false)
                
               // child?.updateReadingData(data.power[0], data.latestData[0])

                result = true
            } else {
                log.error "API error"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error e
        return;
    }
}


//return String displayName of a device
def getLoopEnergyDisplayName(returnObject) {
    if(returnObject) {
        return "${getNamePrefix()}:${returnObject}"
    } else {
        return "${getNamePrefix()}"
    }
}

def getNamePrefix() {
	return "Loop Energy"
}


