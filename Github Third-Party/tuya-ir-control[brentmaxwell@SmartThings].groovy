/**
 *  Tuya IR Control
 *
 *  Copyright 2019 Brent Maxwell
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
    name: "Tuya IR Control",
    namespace: "thebrent",
    author: "Brent Maxwell",
    description: "Control Tuya IR blaster",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
	}
    section("DeviceInfo") {
    	input "ipAddress", "string", title: "IP Address", defaultValue: "192.168.1.254", required: true
        input "deviceName", "string", title: "Name", defaultValue: "Tuya Device", required: true
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

def initialize() {
	unsubscribe()
	unschedule()
    
    if (selectedDevices) {
		addDevices()
	}
}

def addDevices() {
	def deviceIpAddress = state.ipAddress
    def deviceName = state.deviceName
    addChildDevice("thebrent","Tuya IR Blaster", deviceIpAddress, [
    	"label": deviceName,
        "data": [
        	"ip": deviceIpAddress,
            "port": 6668
        ]
    ])
}

def sendCommand(command) {
}

def receiveEvent(event) {
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}
