/**
 *  Simple Web API
 *  CRUDish REST interface to ST
 *
 *  Copyright 2015 John Schettino schettj@gmail.com
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
 
import groovy.json.JsonSlurper
 
definition(
        name: "EchoWebApi",
        namespace: "schettj",
        author: "John schettino",
        description: "web interface to SmartThings",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        oauth: [displayName: "WebApi", displayLink: ""])

// you could get more devices than these, start with just switch

preferences {
    section("Allow a web application to control these things...") {
        paragraph "You should allow devices that you want to access via the web interface. Also revisit this preference when you add or remove devices to your home."
        input "d_switch", "capability.switch", title: "Switch", required: true, multiple: true
    }
}

// our web API

mappings {

	// all devices
	path("/list") {  // all
        action:
        [
                GET: "deviceRecord" 
        ]
    }
    
	// all devices with a given capability
	path("/list/:cap") { 
        action:
        [
                GET: "deviceRecordForCap" 
        ]
    }

    // all devices, with details
	path("/details") {  // all
        action:
        [
                GET: "detailedList" 
        ]
    }

    // all devices with a given capability, with details
	path("/details/:cap") {  // all
        action:
        [
                GET: "detailedForCap" 
        ]
    }

	// all capabilities
	path("/capabilities") {
        action:
        [
                GET: "capabilitiesList" 
        ]
    }

	// GET SET details/status for a device
    
    path("/:id") {
        action:
        [
                GET: "showItem",
                PUT: "updateItem"
        ]
    }

}

// on install

def installed() { 
}

// on update

def updated() { 
}

def deviceRecord() {    
    // log.debug "call count: $state.count"
	[devices: allDevices()]
}

def detailedList() {
	[devices: allDevices().collect { entry -> entry = getDetails(entry) }]
}

def detailedForCap() {
	def devs = deviceRecordForCap()
    [devices: devs.devices.collect { entry -> entry = getDetails(entry) }]
}

def capabilitiesList() {
	def cap = []
    allDevices().each {
	    it.capabilities.each { cap.add(it.toString().toLowerCase()) }
    }
    [capabilities : cap.unique { a, b -> a <=> b }]
    
}

def deviceRecordForCap() {
    [devices: deviceForCapRegistration(params.cap)]
}

// base method raw value
def deviceForCapRegistration(String cap) {
	def devices = allDevices()
    def matches = []
    
    devices.each {
    	if (it.capabilities.findResult(false) {it.toString().toLowerCase() == cap ? true : null })
        	matches.add(it)
    }
	matches
}

// create an unique array of all devices the user has given us access to
def allDevices() {
	def valuesMap = [];
    
    if (d_switch) valuesMap.addAll(d_switch);

    valuesMap.unique { a, b -> a.id <=> b.id };
}

// generic dump all attributes of an item
def showItem() {
	// log.debug state.devices
    def device = allDevices().find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {
    	getDetails(device)
    }
}

// get all (relevant) details for a device 
def getDetails(physicalgraph.app.DeviceWrapper device) {
        // fetch capabilities
        def cap = []
        device.capabilities.each { cap.add(it.toString().toLowerCase()) }

        // create return object
        def retval = [id: device.id, name: device.name, label: device.displayName, capabilities: cap] // , commands: command]

        // fetch current value of each attribute
        def now = new Date()
        def retstate = [time: now];
        device.supportedAttributes.each {
            try {
                def attrib = it.toString()
                def latestValue = device.latestValue(attrib) ?: '';
                retstate[attrib] = latestValue ///device.currentValue(it).value
            } catch (e) {
                log.debug "$it error $e"
            }
        }
        retval.state = retstate
        retval
}

// parms.id is the id of the device
def updateItem() {
    def retstat = [status: 0]
    def success = []
    def fail = []
    def slurper = new JsonSlurper()

    def device = allDevices().find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {

        // state should just be the attributes we want to change...
        try {
            def updatestate = request.JSON?.state
 
            updatestate.each { k, v ->
                try {
                    // depending on key, select desired command
                    // if K = switch then V is the command
					// log.debug "command $k val $v"
					// MORE CAN BE DONE HERE ONLY DO ON/OFF/LEVEL for now
                    switch (k) {
                        case 'switch':
                            device."$v"()
                            break
                        case 'level':
                            device.setLevel(v)
                            break
                        default:
                            fail.add(k)
                    }
                } catch (e) {
                    log.debug e
                    fail.add(k)
                }
            }
        } catch (e) {
            fail.add("no state property supplied")
        }

        if (fail.size() > 0) {
            httpError(404, "invalid state(s): " + fail.join(", "))
        }
    }
}