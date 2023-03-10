/**
 *  Copyright 2019 Roger Selwyn
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
 *	RogerSelwyn ASUS Wifi Presence
 *
 *	Author: Roger Selwyn, Based on original work by Stuart Buchanan with thanks
 *
 *	Date: 2019-02-21 v1.0 Initial Release
 */
definition(
    name: "ASUS Wifi Presence",
    singleInstance: true,
    namespace: "RogerSelwyn",
    author: "Roger Selwyn",
    description: "Triggers Presence Status when HTTP GET Request is recieved",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

def presenceDeviceType() { "ASUS Presence Sensor" }



preferences {
    page(name: "mainPage")

}

def mainPage() {
    dynamicPage(name: "mainPage", uninstall:true, install:true) { 
        section(title: "No of People") {
            input "noPeople", "number", title: "Enter number of people", range: "1..20", required: true, multiple:false, submitOnChange: true	
        }

		if (noPeople) {
            for (int i = 1; i < noPeople +1; i++) {
                section(title: "Person ${i}") {
                    input "personName${i}", "text", title: "Name", required: true, submitOnChange: false
                    input "personMAC${i}", "text", title: "MAC addresses - delimited by comma", required: true, submitOnChange: false
                }
            }
        }

		section("Customize Application Label:") {
			label title:"Application Label (optional)", required:false
		}

    }
}

def installed() {
	createAccessToken()
	getToken()
	DEBUG("Installed with rest api: $app.id")
    DEBUG("Installed with token: $state.accessToken")
	addDeleteDevice(true, "All", null, 0)
    updateDevices()
}

def updated() {
	DEBUG("Updated with rest api: $app.id")
    DEBUG("Updated with token: $state.accessToken")
    updateDevices()
}

mappings {
  path("/Phone/:pState/:pInstance") {
    action: [
      GET: "updatePState"
    ]
  }
  path("/People") {
    action: [
      GET: "getPeople"
    ]
  }
}


def updatePState() {
	processState()
    def oldPeopleData = state.peopleData
    def newPeopleData = createPeopleData ()

	if (oldPeopleData != newPeopleData) {
    	render contentType: "text/html", data: newPeopleData, status: 200
    } else {
    	render contentType: "text/html", data: oldPeopleData, status: 200
    }

}

def getPeople() {
    def responseData = createPeopleData ()
    state.peopleData = responseData
    render contentType: "text/html", data: responseData, status: 200
}


def processState() {
	def pInstance = params.pInstance
    def personDevice = getChildDevice(getDeviceID(pInstance))
    def allPresence = getChildDevice(getDeviceID(0))

    def pState = params.pState
    DEBUG("pInstance: $personDevice.label, pState: $pState")
    switch(pState) {
    	case "home":
        	if (personDevice.presenceState.value == "not present") {
            	personDevice.setHome()
	            break
            }
            break
		case "away":
        	if (personDevice.presenceState.value == "present") {
	            personDevice.setAway()
	            break
            }
            break
    }
    
    if (someonePresent ()) {
    	if (allPresence.presenceState.value == "not present") {
	        allPresence.setHome()
    		DEBUG("Presence All: Home")
        }
    } else {
    	if (allPresence.presenceState.value == "present") {
      		DEBUG("Presence All: Away")
     		allPresence.setAway()
        }
    }
}

def someonePresent () {
    for (int i = 1; i < noPeople + 1; i++) {
    	def deviceID = getDeviceID(i)
   		if (getChildDevice(deviceID).presenceState.value == "present") { 
        	return true
        }
    }
    return false
}

def createPeopleData () {
    def peopleData = ""
    def seperator = ""
    for (int i = 1; i < noPeople + 1; i++) {
    	def personName = settings."personName${i}"
    	def personMAC = settings."personMAC${i}"
    	peopleData += "${seperator}${i} ${personName} ${personMAC}"
        seperator = " @ "
    }
   	return peopleData
}

def updateDevices () {
    def processPeople = getChildDevices().size() - 1
    DEBUG("Old People: ${processPeople}; New People: ${noPeople}")
    if (noPeople > processPeople) { processPeople = noPeople}
    for (int i = 1; i < processPeople + 1; i++) {
    	def person = true
        if (i > noPeople) { person = false }
    	def personName = settings["personName${i}"]
    	def personMAC = settings["personMAC${i}"]
		addDeleteDevice(person, personName, personMAC, i)
    }
}

def addDeleteDevice (person, personName, personMAC, personInstance) {
    def deviceID = getDeviceID(personInstance)
  	def childDevice = getChildDevice(deviceID)
    def personDevice = "personDevice${personInstance}"

	if (person && !childDevice) {
  		def newDevice = addChildDevice(app.namespace, presenceDeviceType(), deviceID, null, [name: deviceID, label: "Presence - ${personName}", completedSetup: true])
        newDevice.setAway()
        DEBUG("Added Device: ${deviceID}")
    	settingUpdate(personDevice, "${personName} - ${deviceID}", "text")
    } else if(!person && childDevice) {
  		deleteChildDevice(deviceID)
        DEBUG("Deleted Device: ${deviceID}")
    	settingUpdate(personDevice, null)
    	settingUpdate("personName${personInstance}", null)
	   	settingUpdate("personMAC${personInstance}", null)
    }
}

def getDeviceID (personInstance) {
	return "${app.namespace}_${app.id}_${personInstance}"
}

void settingUpdate(name, value, type=null) {
	DEBUG("settingUpdate($name, $value, $type)...")
	if(name) {
		if(value == "" || value == null || value == []) {
			settingRemove(name)
			return
		}
	}
	if(name && type) {
		app?.updateSetting("$name", [type: "$type", value: value])
	}
	else if (name && type == null){ app?.updateSetting(name.toString(), value) }
}

void settingRemove(name) {
	DEBUG("settingRemove($name)...")
	if(name) { app?.deleteSetting("$name") }
}



def getToken(){
if (!state.accessToken) {
		try {
			getAccessToken()
			DEBUG("Creating new Access Token: $state.accessToken")
		} catch (ex) {
			DEBUG("Did you forget to enable OAuth in SmartApp IDE settings")
            DEBUG(ex)
		}
	}
}

private def DEBUG(message){
	log.debug message
}

