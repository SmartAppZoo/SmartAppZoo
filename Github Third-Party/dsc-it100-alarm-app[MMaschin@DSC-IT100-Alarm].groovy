/**
 *  DSC-IT100-Alarm App
 *
 *  Author: Matt Maschinot
 *  
 *  Date: 04/10/2019
 */

// for the UI
definition( 
    name: "DSC-IT100-Alarm-App",
    namespace: "mmaschin/DSCIT100Alarm",
    author: "mmaschin@gmail.com",
    description: "SmartApp DSCAlarmApp",
    category: "Safety & Security",
    iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=2x",
    iconX3Url: "https://graph.api.smartthings.com/api/devices/icons/st.Home.home3-icn?displaySize=3x",
    singleInstance: true,
    oauth: true
)

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

preferences {
    page(name: "prefs", title: "DSC Alarm")
    page(name: "zones", title: "DSC Alarm")
}

def prefs() {

    dynamicPage(name: "prefs", uninstall: true, nextPage: "zones") {

        section("Select the DSC Alarm Panel Device") {
            input "panel", "capability.Alarm", title: "Select DSC Alarm Panel if available", multiple: false, required: false
        }
        section("SmartThings Hub") {
            input "hostHub", "hub", title: "Select Smartthings Hub", multiple: false, required: true, submitOnChange: true
        }
        if (!state.accessToken) {
            // the createAccessToken() method will store the access token in state.accessToken
            state.accessToken = createAccessToken()
        }
        section("SmartThings Raspberry Pi") {
            input "proxyAddress", "text", title: "Raspberry Pi Address", description: "(ie. 192.168.1.10)", required: true
            input "proxyPort", "text", title: "Raspberry Pi Port", description: "(ie. 3000)", required: true, defaultValue: "3000"
            input "sharedLocation", "text", title: "Shared Location", description: "", required: true, defaultValue: apiServerUrl.substring(8, apiServerUrl.lastIndexOf(":"))
            input "appid", "text", title: "Application Id", description: "", required: true, defaultValue: app.id
            input "accessToken", "text", title: "Access Token", description: "", required: true, defaultValue: state.accessToken
        }
        section("DSC Alarm Arm/Disarm Code") {
            input "securityCode", "text", title: "Security Code", description: "User code to arm/disarm the security panel", required: true
        }
        section("Smart Home Monitor") {
            input "enableSHM", "bool", title: "Integrate with Smart Home Monitor", required: true, defaultValue: true
        }
        section("Enable Debug Log at SmartThing IDE") {
            input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
        }
    }
}

def zones() {
    dynamicPage(name: "zones", install: true) {

        section("Zones") {
            app(name: "zoneDefinitions", appName: "DSC-IT100-Alarm-Zone-App", namespace: "mmaschin/DSCIT100Alarm", title: "Define New Zone", multiple: true)
        }
    }
}


// When installed - initialize
def installed() {

    initialize()
}


// When updated - initialize
def updated() {

    initialize()
}


// removed existing subscriptions and add subscritions to REST API (IT-100/Raspberry Pi) and Smartthings SHM
// after 10 seconds send request to IT-100 for Alarm status
def initialize() {

	unsubscribe()

    sendCommand('/subscribe/' + settings.hostHub.localIP + ":" + settings.hostHub.localSrvPortTCP) //send Hub address to Raspberry pi
    sendCommand('/config/' + settings.securityCode + ":" + settings.sharedLocation + ":" + settings.appid + ":" + settings.accessToken) //send configuration info to Raspberry pi

    subscribe(location, "alarmSystemStatus", alarmHandler)

	//wait 10 secoinds and then send requests to Raspberry Pi for alarm status 
    runIn(10, alarmStatus)
}


// Uninstalled - make sure to delete all the children
def uninstalled() {
    
    getAllChildDevices().each {
        deleteChildDevice(it.deviceNetworkId)
    }
}


//Receive data from REST API (Raspberry Pi - IT-100)
mappings {

    path("/dscalarm/:command") {
        action: [
            GET: "responseFromRaspberry"
        ]
    }
}


//Process data received from Raspberry Pi - IT-100
void responseFromRaspberry() {

	writeLog("*** params.command=${params.command}")

	if (params.command.length() < 4) {
    	return
    }
    
    def msg = params.command

    if (msg.length() >= 4) {

        if (msg.substring(0, 2) == "RD") {
            if (msg[3] != "0") {
                updateAlarmSystemStatus("ready")
            }
            // Process arm update
        } else if (msg.substring(0, 2) == "AR") {
            if (msg[3] == "0") {
                updateAlarmSystemStatus("ready")
            } else if (msg[3] == "1") {
                if (msg[5] == "0") {
                    updateAlarmSystemStatus("armedaway")
                } else if (msg[5] == "2") {
                    updateAlarmSystemStatus("armedstay")
                }
            } else if (msg[3] == "2") {
                updateAlarmSystemStatus("arming")
            }
        } else if (msg.substring(0, 2) == "SY") {
            // Process alarm update
        } else if (msg.substring(0, 2) == "AL") {
            // Process chime update
        } else if (msg.substring(0, 2) == "CH") {
            // Process zone update
        } else if (msg.substring(0, 2) == "ZN") {

            def zoneNumber = msg.substring(6, 9)
            zoneNumber = zoneNumber.replaceAll("^0+", "");

			//the below code will locate an existing child app for the
            //zone number of the 'ZN' command.  If one is found then the 
            //updateAlarmZoneDevice method will be called on the zone.
            def zonedeviceNetworkID = "Alarm_Zone${zoneNumber}"
            def zonedevice
            def zoneDeviceFound = false

            def childApps = getAllChildApps()
            childApps.each {
                childApp->
                    if (!zoneDeviceFound) {

                        def childDevices = childApp.getChildDevices()
                        childDevices.each {
                            childDev->

                                if (!zoneDeviceFound) {
                                    zoneDeviceFound = childDev.deviceNetworkId == zonedeviceNetworkID
                                    if (zoneDeviceFound) {

                                        childApp.updateAlarmZoneDevice(msg)
                                        zonedevice = childDev
                                    }
                                }
                        }
                    }
            }
        }
        
        //If a DSC Alarm Panel has been created and added, then pass command to it
        if (panel) {
            panel.dscalarmparse("${msg}")

        }
    }
}


//When receiving event from SHM send command to IT-100
def alarmHandler(evt) {
    if (!settings.enableSHM) {
        return
    }

    if (state.alarmSystemStatus == evt.value) {
        return
    }

    state.alarmSystemStatus = evt.value
    if (evt.value == "stay") {
        sendCommand('/api/alarmArmStay');
    }
    if (evt.value == "away") {
        sendCommand('/api/alarmArmAway');
    }
    if (evt.value == "off") {
        sendCommand('/api/alarmDisarm');
    }
}


//When alarm status changes send event to SHM
def updateAlarmSystemStatus(partitionstatus) {
    if (!settings.enableSHM || partitionstatus == "arming") {
        return
    }

    def lastAlarmSystemStatus = state.alarmSystemStatus
    if (partitionstatus == "armedstay") {
        state.alarmSystemStatus = "stay"
    }
    if (partitionstatus == "armedaway") {
        state.alarmSystemStatus = "away"
    }
    if (partitionstatus == "ready") {
        state.alarmSystemStatus = "off"
    }

    if (lastAlarmSystemStatus != state.alarmSystemStatus) {
        sendLocationEvent(name: "alarmSystemStatus", value: state.alarmSystemStatus)
    }
}


//Send command to IT-100 to receive alarm status
def alarmStatus() {
    sendCommand('/api/alarmUpdate')
}


//Send command to Smartthings hub - REST API/IT-100
def sendCommand(path) {

    if (settings.proxyAddress.length() == 0 ||
        settings.proxyPort.length() == 0) {
        log.error "SmartThings Node Proxy configuration not set!"
        return
    }

    def host = settings.proxyAddress + ":" + settings.proxyPort
    def headers = [: ]
    headers.put("HOST", host)
    headers.put("Content-Type", "application/json")
    headers.put("stnp-auth", settings.authCode)

    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: headers
    )

    sendHubCommand(hubAction)
}


// Write debug messages when turned on 
def writeLog(message) {
    if (idelog) {
        log.debug "${message}"
    }
}