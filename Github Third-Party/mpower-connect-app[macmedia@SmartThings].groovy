/**
*  mPower Connect App
*
*  Copyright 2016 Patrick Stuart
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
    name: "mPower Connect App",
    namespace: "lcf",
    author: "Patrick Stuart",
    description: "Connect SmartApp that takes the IP address and Port plus username and password for the already set up mPower 8 outlet smart switch",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/google/material-design-icons/master/image/2x_web/ic_flash_on_black_48dp.png",
    iconX2Url: "https://raw.githubusercontent.com/google/material-design-icons/master/image/2x_web/ic_flash_on_black_48dp.png",
    iconX3Url: "https://raw.githubusercontent.com/google/material-design-icons/master/image/2x_web/ic_flash_on_black_48dp.png")


preferences {
    section("Add a mPower Device") {
        //TODO Should we ask for mac address?  Or can we get mac address another way?
        input "IP", "text", title: "mPower Device IP Address", defaultValue: "192.168.101.162" //TODO remove for publication
        input "port", "text", title: "mPower Device Port", defaultValue: "80" //Do we really need this
        input "hub", "hub", title: "On which SmartThings Hub?", required: true
        input "username", "text", title: "Username", defaultValue: "pstuart" //TODO remove for publication
        input "password", "password", title: "Password", defaultValue: "winter12" //TODO remove for publication
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
    //install controller deviceType if not already installed
    log.debug "hit Initialize"

    try {
        def DNI = convertIPtoHex(IP) + ":" + convertPortToHex(port)
        def existingDevice = getChildDevice(DNI)

        def hubId = hub.id
        log.debug hubId

        if(!existingDevice) {
            //Add our controller device
            def childDevice = addChildDevice("lcf", "mPower Controller", DNI, hubId, [name: "mPower Controller", label: "mPower Controller", completedSetup: true]) 
            log.debug "mPower controller installed $childDevice"

        } else {
            //Device already exists
            log.debug "Already Installed"
        }

        //TODO check if virtual switch exist, if not, create 8 virtual switches
        def existingDevices = getChildDevices()
        (1..8).each {
            //it = number
            DNI = "mPowerVirtualSwitch${it}"
            existingDevice = getChildDevice(DNI)
            if (!existingDevice) {
                def childDevice = addChildDevice("lcf", "mPower Virtual Switch", DNI, hubId, [name: "mPower Virtual Switch", label: "mPower Virtual Switch ${it}", completedSetup: true]) 
                log.debug "mPower Virtual Switch${it} installed $childDevice"
            }
        }


    } catch (e) {
        log.error "Error creating device: ${e}"
    }
}

private getHostAddress() {
    return "${IP}:${port}"
}

def virtualOn(virtualSwitch) {
	log.debug "virtual on called with params $virtualSwitch"
    //TODO fix devices array
    if(!atomicState[virtualSwitch]) { atomicState[virtualSwitch] = "off"}
    atomicState[virtualSwitch] = "on"
    //TODO send controller command to turn on x switch
    def DNI = convertIPtoHex(IP) + ":" + convertPortToHex(port)
    def controller = getChildDevice(DNI)
    controller.on(virtualSwitch[-1].toInteger())
}

def virtualOff(virtualSwitch) {
    if(!atomicState[virtualSwitch]) { atomicState[virtualSwitch] = "off"}
    atomicState[virtualSwitch] = "off"
    def DNI = convertIPtoHex(IP) + ":" + convertPortToHex(port)
    def controller = getChildDevice(DNI)
    controller.off(virtualSwitch[-1].toInteger())
    //TODO send controller command to turn on x switch
}

def updateVirtual(virtualSwitch, cmd) {
    if(!atomicState[virtualSwitch]) { atomicState[virtualSwitch] = "off"}
    atomicState[virtualSwitch] = cmd
    def DNI = virtualSwitch
    def virtual = getChildDevice(DNI)
    virtual.update(cmd)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    //log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    //log.debug hexport
    return hexport
}
