/**
 *  vivint.SmartHome Manager
 *
 *  Copyright 2020 Nathan Spencer
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
 *  CHANGE HISTORY
 *  VERSION     DATE            NOTES
 *  1.0.0       2020-07-21      Initial release
 *  1.0.1       2020-08-31      Report as dead battery if device has gone offline
 *
 */

definition(
    name: "vivint.SmartHome Manager",
    namespace: "natekspencer",
    author: "Nathan Spencer",
    description: "Access and control your vivint.SmartHome devices.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/natekspencer/VivintSmartHomeManager/master/images/vivint-smarthome.png",
    iconX2Url: "https://raw.githubusercontent.com/natekspencer/VivintSmartHomeManager/master/images/vivint-smarthome@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/natekspencer/VivintSmartHomeManager/master/images/vivint-smarthome@3x.png",
    singleInstance: true
) {
}

preferences {
    page(name: "mainPage")
    page(name: "authPage")
    page(name: "authResultPage")
    page(name: "mfaPage")
    page(name: "mfaResultPage")
    page(name: "aboutPage")
}

def mainPage() {
    reloadState()

    def systems = getSystemsMap()
        
    dynamicPage(name: "mainPage", install: true, uninstall: true) {
        if (systems) {
            section("Select which systems to use:") {
                input(name: "systems", type: "enum", title: "Systems", required: false, multiple: true, metadata: [values: systems], submitOnChange: true)
            }

            if(settings.systems) {
                section("Select which panels to use:") {
                    input(name: "panels", type: "enum", title: "Panels", required: false, multiple: true, metadata: [values: getDevicesByType("primary_touch_link_device")])
                }

                section("Select which locks to use:") {
                    input(name: "locks", type: "enum", title: "Locks", required: false, multiple: true, metadata: [values: getDevicesByType("door_lock_device")])
                }

                section("Select which garage doors to use:") {
                    input(name: "garages", type: "enum", title: "Garage Doors", required: false, multiple: true, metadata: [values: getDevicesByType("garage_door_device")])
                }

                section("Select which sensors to use:") {
                    input(name: "sensors", type: "enum", title: "Sensors", required: false, multiple: true, metadata: [values: getDevicesByType("wireless_sensor")])
                }
            }

            section("How frequently do you want to poll the vivint.SmartHome cloud for changes? (Use a lower number if you care about trying to capture and respond to events as they happen)") {
                input(name: "pollingInterval", title: "Polling Interval (in Minutes)", type: "enum", required: false, multiple: false, defaultValue: 5, description: "5", options: ["1", "5", "10", "15", "30"])
            }
        }
        section("vivint.SmartHome Authentication") {
            href("authPage", title: "vivint.SmartHome API Authorization", description: "${state.loggedIn == null ? "" : "${state.loggedIn ? "[Connected]" : "[Disconnected] - ${state.loginResponse}"}\n"}Click to enter vivint.SmartHome credentials")
        }
        section ("Name this instance of ${app.name}") {
            label name: "name", title: "Assign a name", required: false, defaultValue: app.name, description: app.name, submitOnChange: true
        }
        section("About")
		{
			href("aboutPage", title: "About vivint.SmartHome Manager", description: "Want to support this app? Consider making a small contribution today!")
		}
    }
}

def authPage() {
    dynamicPage(name: "authPage", nextPage: "authResultPage", uninstall: false, install: false) {
        section("vivint.SmartHome Credentials") {
            input("email", "email", title: "Email", description: "vivint.SmartHome email", required: true)
            input("password", "password", title: "Password", description: "vivint.SmartHome password", required: true)
        }
    }
}

def authResultPage() {
    doLogin()
    
    // Check if login was successful
    if (state.cookie == null) {
        dynamicPage(name: "authResultPage", nextPage: "authPage", uninstall: false, install: false) {
            section("${state.loginResponse}") {
                paragraph ("Please check your credentials and try again.")
            }
        }
    } else if (state.mfaEnabled) {
        dynamicPage(name: "authResultPage", nextPage: "mfaPage", uninstall: false, install: false) {
            section("${state.loginResponse}") {
                paragraph ("Your account is protected by MFA. Please click next to enter the six digit code generated by your authentication app.")
            }
        }
    } else {
        dynamicPage(name: "authResultPage", nextPage: "mainPage", uninstall: false, install: false) {
            section("${state.loginResponse}") {
                paragraph ("Please click next to continue setting up your vivint.SmartHome system.")
            }
        }
    }
}

def mfaPage() {
    dynamicPage(name: "mfaPage", nextPage: "mfaResultPage", uninstall: false, install: false) {
        section("vivint.SmartHome MFA") {
            input("code", "number", title: "Code", description: "Enter the six digit code generated by your authentication app.", required: true)
        }
    }
}

def mfaResultPage() {
    def resp = doCallout("POST", "v0/platformusers/2fa/validate", [code: code.toString()], null, true)
    if (resp?.code) {
        dynamicPage(name: "mfaResultPage", nextPage: "mfaPage", uninstall: false, install: false) {
            section("${resp.message}") {
                paragraph ("Please click next to try again.")
            }
        }
    } else {
        dynamicPage(name: "mfaResultPage", nextPage: "mainPage", uninstall: false, install: false) {
            section("vivint.SmartHome MFA") {
                    paragraph ("Please click next to continue setting up your vivint.SmartHome system.")
            }
        }
    }
}

def aboutPage()
{
	dynamicPage(name: "aboutPage", title: "About vivint.SmartHome Manager", uninstall: false, install: false)
	{
		section()
		{
			paragraph image: "https://raw.githubusercontent.com/natekspencer/VivintSmartHomeManager/master/images/vivint-smarthome@3x.png", "vivint.SmartHome Manager"
		}
        section("Support locations")
		{
			href (name: "scReleaseThd", style:"embedded", title: "SmartThings Community Support Thread", url: "https://community.smartthings.com/t/release-vivintsmarthomemanager-smartthings-integration-for-vivint-smarthome-security-systems/199995")
			href (name: "github", style:"embedded", title: "GitHub Repository Issues", url: "https://github.com/natekspencer/VivintSmartHomeManager/issues")
		}
        section("Support the Project")
		{
			paragraph "vivint.SmartHome Manager is provided free for personal and non-commercial use. I have worked on this app in my free time to fill my own personal needs and will continue to make improvements where I can. If you have found this app helpful and would like to donate to continue to help with development, please use the link below."
			href (name: "donate", style:"embedded", title: "Consider making a \$5 or \$10 donation today!", image: "https://raw.githubusercontent.com/natekspencer/VivintSmartHomeManager/master/images/vivint-smarthome@3x.png", url: "https://www.paypal.me/natekspencer")
		}
        section ("Return to vivint.SmartHome Manager main menu"){
            href "mainPage", title: "vivint.SmartHome Manager Main Menu", description: "Return to vivint.SmartHome Manager main menu"            
		}
	}
}

def reloadState() {
    def lastLoad = state.lastLoad ?: 0
    if (state.cookie?.trim() && (!lastLoad || now()-lastLoad > 1000 * 60 * 10)) {
        state.lastLoad = now()
        getSystems()
        getDevices()
    }
}

boolean doLogin(){
    state.remove("lastLoad")
    def resp = doCallout("POST", "login", [username: email, password: password, persist_session: true])
    if (resp) {
        state.mfaEnabled = resp.u.mfa_enabled
    }
    return resp != null
}

// Get the list of systems
def getSystems() {
    def systems = []
    doCallout("GET", "authuser", null)?.u?.system?.each {
        systems.add([id: it.panid.toString(), name: it.sn])
    }
    state.systems = systems
    systems
}

def getSystemsMap() {
    def systems = [:]
    state.systems?.each {
        systems[it.id] = it.name
    }
    systems
}

def getDevices(children) {
    def systems = children ? settings.systems : state.systems.collect { it.id }
    def devices = []

    systems.each { systemId ->
        def systemData = doCallout("GET", "${systemId}/0/armedstates", null)?.par
        systemData?.d.findAll { 
            ["primary_touch_link_device", "door_lock_device", "garage_door_device", "wireless_sensor"].contains(it.t) && (!children || children.collect{ it.deviceNetworkId }.contains([systemId, it._id].join(".")))
        }.each { device ->
            if (children) {
                def child = children.find { 
                    it.deviceNetworkId == [systemId, device._id].join(".")
                }
                if (device.t == "primary_touch_link_device") {
                    device.s = systemData.s
                    device.messages = getSystemHealthData(systemId)
                }
                child.parseEventData(standardize(device))
            } else {
                devices.add([systemId: systemId, id: device._id, type: device.t, name: device.n])
            }
        }
    }
    if (!children)
        state.devices = devices
    devices
}

def getDevicesByType(type) {
    def devices = [:]

    def displaySystemName = settings.systems?.size() > 1
    def systems = getSystemsMap()

    state.devices.findAll { 
        settings.systems?.contains(it.systemId.toString()) && (type == "ALL" || type?.contains(it.type))
    }.each {
        devices[[it.systemId, it.id].join('.')] = "${it.name}${displaySystemName ? " (${systems[it.systemId]})" : "" }"
    }

    devices.sort { it.value }
}

def doCallout(calloutMethod, urlPath, calloutBody) {
    doCallout(calloutMethod, urlPath, calloutBody, null)
}

def doCallout(calloutMethod, urlPath, calloutBody, queryParams) {
    doCallout(calloutMethod, urlPath, calloutBody, null, false)
}

def doCallout(calloutMethod, urlPath, calloutBody, queryParams, isMfa){
    def isLoginRequest = urlPath == "login"
   
    if (state.loggedIn || isLoginRequest) { // prevent unauthorized calls
        log.debug "\"${calloutMethod}\"-ing to \"${urlPath}\""
    
        def params = [
            uri: "https://vivintsky.com",
            path: "/${isMfa ? "platform-user-" : ""}api/${urlPath}",
            query: queryParams,
            headers: [
                "Content-Type": "application/json",
                "Cookie": state.cookie
            ],
            body: calloutBody
        ]
        
        try {
            switch (calloutMethod) {
                case "GET":
                    httpGet(params) {resp->
                        return handleResponse(resp)
                    }
                    break
                case "PATCH":
                    params.headers["x-http-method-override"] = "PATCH"
                    // NOTE: break is purposefully missing so that it falls into the next case and "POST"s
                case "POST":
                    httpPostJson(params) {resp->
                        return handleResponse(resp)
                    }
                    break
                case "PUT":
                    httpPutJson(params) {resp->
                        return handleResponse(resp)
                    }
                    break
                default:
                    log.error "unhandled method"
                    return [error: "unhandled method"]
                    break
            }
        } catch (groovyx.net.http.HttpResponseException e) {
            log.error e
            return handleResponse(e.response)
        } catch (e) {
            log.error "Something went wrong: ${e}"
            return [error: e.message]
        }
    } else {
        log.info "skipping request since the user is not currently logged in"
        return []
    }
}

def handleResponse(response) {
    switch (response.status) {
        case 200:
            state.loggedIn = true
            state.loginResponse = "Login successful"
            if (response.headers["Set-Cookie"]) {
                state.cookie = response.headers["Set-Cookie"].value
            }
            break
        case 401:
            state.loggedIn = false
            state.loginResponse = response.data.msg
            state.remove("cookie")
            state.remove("systems")
            break
        case 400:
        case 403:
            return response.data
            break
        default:
            log.debug response.data
            break
    }

    response.success ? response.data : null
}

def installed() {
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    def systems = settings.systems?.intersect(getSystemsMap()?.keySet())
    def validDevices = getDevicesByType("ALL")
    def validDeviceIds = validDevices.keySet()

    def panels = settings.panels?.findAll {
        systems?.contains(it.tokenize(".")[0])
    }?.intersect(validDeviceIds)

    def locks = settings.locks?.findAll { 
        systems?.contains(it.tokenize(".")[0])
    }?.intersect(validDeviceIds)

    def garages = settings.garages?.findAll { 
        systems?.contains(it.tokenize(".")[0])
    }?.intersect(validDeviceIds)

    def sensors = settings.sensors?.findAll { 
        systems?.contains(it.tokenize(".")[0])
    }?.intersect(validDeviceIds)

    def selectedDevices = [panels,locks,garages,sensors].flatten()
    selectedDevices.removeAll([null])

    getChildDevices().findAll {
        !selectedDevices.contains(it.deviceNetworkId)
    }.each {
        log.info "Removing ${it}"
        deleteChildDevice(it.deviceNetworkId)
    }
    
    if(selectedDevices.size() > 0) {
        // add child devices after a delay since this can take a while and may cause a timeout if a lot of devices are selected
        runIn(1, delayAddChildren, [overwrite: true, data: [devices: selectedDevices]])
        
        // set up polling only if we have devices selected
        "runEvery${pollingInterval}Minute${pollingInterval != "1" ? 's' : ''}"("pollChildren")
    } else unschedule(pollChildren)
}

def delayAddChildren(data) {
    try {
        def validDevices = getDevicesByType("ALL")
        data.devices.each { deviceNetworkId ->
            try {
                if(!getChildDevice(deviceNetworkId)) {
                    def device = state.devices.find {
                        deviceNetworkId == [it.systemId, it.id].join(".")
                    }
                    def name = validDevices[deviceNetworkId]
                    log.info "Adding device: ${name} [${deviceNetworkId}]"
                    addChildDevice(app.namespace, typeName(device.type), deviceNetworkId, location.hubs[0]?.id, [label: name, completedSetup: true])
                }
            } catch (e) {
                log.error "Error creating device: ${e}"
            }
        }
        runIn(1, pollChildren, [overwrite: true])
    } catch (java.util.concurrent.TimeoutException e) {
        // hacky workaround for timeout during child device creation
        // this happens when a lot of devices are being created, so just call it again since it will only create devices if they don't exist
        runIn(1, delayAddChildren, [data: data])
    } catch (e) {
        log.error "Error creating children: ${e}"
    }
}

def pollChildren() {
    def start = now()
    log.debug "polling..."
    def children = getChildDevices()
    if (children.size() == 0) {
        log.info "no children to update: skipping polling"
    } else {
        getDevices(children)
    }
    log.debug "polling took ${(now()-start)/1000} seconds"
}

def getDeviceData(systemId, deviceId) {
    standardize(doCallout("GET", "system/${systemId}/device/${deviceId}", null).system.par[0].d[0])
}

def dispatchCommand(systemId, deviceId, type, deviceData) {
    doCallout("PUT", "${systemId}/0/${type}${deviceId ? "/${deviceId}" : ""}", deviceData).status == 200
}

def getSystemHealthData(systemId) {
    doCallout("GET", "system_health/${systemId}", null).system_health.collect {
        it.banner
    }.join("\n")
}

def isoFormat() {
    "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
}

def toStDateString(date) {
    date.format(isoFormat())
}

def parseDate(dateStr) {
    dateStr?.trim() ? Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS", dateStr?.substring(0,23)) : null
}

def typeName(type) {
    [
        "primary_touch_link_device":"vivint.SmartHome Panel",
        "door_lock_device":"vivint.SmartHome Lock",
        "garage_door_device":"vivint.SmartHome Garage Door",
        "wireless_sensor":"vivint.SmartHome Sensor"
    ][type]
}

def standardize(device) {
    if (device.ol != null && !device.ol && device.bl > 0) { // if device is offline, it is likely the battery is dead
        device.bl = 0
    }
    device
}