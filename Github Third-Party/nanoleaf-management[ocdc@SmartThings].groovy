/**
 *
 *  Nanoleaf Management SmartApp v1.1
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
 
 /* Todo List
    1. Rename scene favorites (day, evening, night, relax, music relax, music party)
    2. Refactor and optimise DTH
    3. Rename SmartApp
 */

definition(
    name: "Nanoleaf Management",
    namespace: "ocdc",
    author: "OC",
    description: "Nanoleaf Management SmartApp",
    category: "Convenience",
    iconUrl: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/icon.png",
    iconX2Url: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/icon-2x.png",
    iconX3Url: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/icon-3x.png",
    pausable: false
)

preferences {
    page(name: "pageMain")
    page(name: "pageSelectScene")
    page(name: "pageSelectSceneConfirm")
    page(name: "pageSetIpAndPort")
    page(name: "pageSetIpAndPortConfirm")
    page(name: "pageGetApi")
    page(name: "pageClearApi")
    page(name: "pageSelectDevice")
    page(name: "pageInformation")
    page(name: "pageIdentifyPanels")
    page(name: "pageMissingData")
    page(name: "pageAcknowledgements")
    page(name: "pageSetupHelp")
    page(name: "pageRefresh")
    page(name: "pageClearScenesAndPanelIds")
    page(name: "pageSetRefreshPeriod")
    page(name: "pageSetRefreshPeriodConfirm")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
}

def pageMain() {
	if (!selectedDevice) {
		return dynamicPage(name: "pageMain", title: "Select Device", install: true, uninstall: true) {
			section("") {
				input "selectedDevice", "device.NanoleafAuroraSmarterAPI", multiple: false, required: true, title: "Tap to select", submitOnChange: true
			}
		}
	}
	else {
        def presetsMap = null
    
        if (selectedDevice.currentValue("presets")?.trim()) {
            presetsMap = new groovy.json.JsonSlurper().parseText(selectedDevice.currentValue("presets"))
        }
    
        return dynamicPage(name: "pageMain", uninstall: true, install: true) {
            section (){
                paragraph "Currently selected: ${selectedDevice.name}"
                href(name: "pageSelectDevice", title: "Select a Device", required: false, page: "pageSelectDevice", description: "Tap to change selected device", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/select.png")
            }
            section ("Actions"){
                if (presetsMap && presetsMap.name.size() > 0) {
                    href(name: "pageSelectScene", title: "Activate a Scene", required: false, page: "pageSelectScene", description: "Tap to select a scene", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/activate.png")
                    href(name: "pageIdentifyPanels", title: "Identify Panels", required: false, page: "pageIdentifyPanels", description: "Tap to identify panels", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/identify.png")
                } else {
                    href(name: "pageMissingData", title: "Missing Data", required: false, page: "pageMissingData", description: "Tap for more information", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/warning.png")
                }
                href(name: "pageInformation", title: "View Information", required: false, page: "pageInformation", description: "Tap to view panel information", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/information.png")
            }
            section ("Connection Settings"){
                href(name: "pageSetIpAndPort", title: "Set IP and Port", required: false, page: "pageSetIpAndPort", description: "Tap to set IP and Port", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/ip.png")
                href(name: "pageSetApi", title: "Get an API Key", required: false, page: "pageGetApi", description: "Tap to get an API Key", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/get-key.png")
                href(name: "pageClearApi", title: "Clear the API Key", required: false, page: "pageClearApi", description: "Tap to clear the API Key", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/clear.png")
            }
            section ("Other Settings"){
                href(name: "pageSetRefreshPeriod", title: "Set Refresh Period", required: false, page: "pageSetRefreshPeriod", description: "Tap to set refresh period", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/timer.png")
                href(name: "pageRefresh", title: "Refresh Data", required: false, page: "pageRefresh", description: "Tap to refresh data", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/refresh.png")
                href(name: "pageClearScenesAndPanelIds", title: "Clear Scenes and IDs", required: false, page: "pageClearScenesAndPanelIds", description: "Tap to clear Scenes and IDs", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/clear.png")
            }
            section ("Help"){
                href(name: "pageAcknowledgements", title: "Acknowledgements", required: false, page: "pageAcknowledgements", description: "", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/acknowledgements.png")
                href(name: "pageSetupHelp", title: "Setup Help", required: false, page: "pageSetupHelp", description: "", image: "https://github.com/ocdc/SmartThings/raw/master/smartapps/ocdc/nanoleaf-management.src/menu-icons/help.png")
            }
        }
    } 
}

def pageSelectScene() {
    log.debug "Select Scene"

    def presetsMap = new groovy.json.JsonSlurper().parseText(selectedDevice.currentValue("presets"))
    return dynamicPage(name: "pageSelectScene", title: "Select Scene", nextPage: "pageSelectSceneConfirm") {
        section (){
            input "selectedScene", "enum", options: presetsMap.name, description: "Tap to select", defaultValue: "", required: no
        } 
    }
}

def pageSelectSceneConfirm() {
	log.debug "Setting Scene to ${selectedScene}"

    selectedDevice.changeScene(selectedScene)
    return dynamicPage(name: "pageSelectSceneConfirm", title: "Scene Set", nextPage: "pageMain") {
        section("Scene set to ${selectedScene}"){
        }
    }
}

def pageClearScenesAndPanelIds() {
	log.debug "Clearing Scenes and Panel IDs"

    selectedDevice.clearScenesAndPanelIds()
    return dynamicPage(name: "pageClearScenesAndPanelIds", nextPage: "pageMain") {
        section("All Scenes and Panel IDs have been cleared, a data refresh will be needed sync current data"){
        }
    }
}

def pageRefresh() {
	log.debug "Refresh data"

    selectedDevice.refresh()
    return dynamicPage(name: "pageRefresh", title: "Refresh Data", nextPage: "pageMain") {
        section("A request to get current data has been sent"){
        }
    }
}

def pageSetRefreshPeriod() {
    log.debug "Setting Refresh Period"

	def delayList = [1, 5, 10, 15, 30]
    def currentTimerDelay = 5
    
    if (selectedDevice.currentValue("timerDelay")?.trim()) {
		currentTimerDelay = selectedDevice.currentValue("timerDelay")
    }
    
    return dynamicPage(name: "pageSetRefreshPeriod", title: "Select Refresh Period", nextPage: "pageSetRefreshPeriodConfirm") {
        section(){
            input "timerDelay", "enum", options: delayList, title: "Tap to select", defaultValue: currentTimerDelay, required: no
        }
    }
}

def pageSetRefreshPeriodConfirm() {
    log.debug "Setting Refresh Period to ${timerDelay}"

	selectedDevice.setTimerDelay(timerDelay)
    return dynamicPage(name: "pageSetRefreshPeriodConfirm", title: "Refresh Period Set", nextPage: "pageMain") {
        section("Refresh period has been set to ${timerDelay}"){
        }
    }
}

def pageSetIpAndPort() {
    log.debug "Set IP and Port Device"

    def currentIpAddress = selectedDevice.currentValue("ipAddress")
    def currentPort = selectedDevice.currentValue("port")
    def defaultIpAddress = null
    def defaultPort = null

    if (currentIpAddress?.trim()) {
        defaultIpAddress = selectedDevice.currentValue("ipAddress")
    }

    if (currentPort?.trim()) {
        defaultPort = selectedDevice.currentValue("port")
    }

    return dynamicPage(name: "pageSetIpAndPort", title: "Set IP Address and Port", nextPage: "pageSetIpAndPortConfirm") {
        section() {
            paragraph "If IP and port have been changed directly in the device they may not be correct below. Current values directly from the device are:\nIP: ${currentIp}\nPort: ${currentPort}"
            input "ipAddress", "text", required: true, title: "Enter IP address", defaultValue: defaultIpAddress
            input "port", "text", required: true, title: "Enter port (Default is 16021)", defaultValue: defaultPort
        }
    }
}

def pageSetIpAndPortConfirm() {
    log.debug "Set IP and Port Device"

    selectedDevice.setIpAddressAndPort(ipAddress, port)
    return dynamicPage(name: "pageSetIpAndPortConfirm", title: "IP Address and Port Set", nextPage: "pageMain") {
        section("IP address has been set to ${ipAddress} and port has been set to ${port}") {
        }
    }
}

def pageGetApi() {
    log.debug "Getting API Key"

    selectedDevice.requestAPIkey()
    return dynamicPage(name: "pageGetApi", title: "Request API Key", nextPage: "pageMain") {
        section ("An API Key for ${selectedDevice.name} has been requested"){
        }
    }
}

def pageClearApi() {
    log.debug "Clearing API Key"

    selectedDevice.clearApiKey()
    return dynamicPage(name: "pageClearApi", title: "Clear API Key", nextPage: "pageMain") {
        section ("The API Key for ${selectedDevice.name} has been removed"){
        } 
    }
}

def pageSelectDevice() {
    log.debug "Select Device"

    return dynamicPage(name: "pageSelectDevice", title: "Select Device", nextPage: "pageMain") {
        section() {
            input "selectedDevice", "device.NanoleafAuroraSmarterAPI", multiple: false, required: true, title: "Tap to select"
        }
    }
}

def pageInformation() {
    log.debug "View Information"

    def scene1 = selectedDevice.currentValue("scene1")
    def scene2 = selectedDevice.currentValue("scene2")
    def scene3 = selectedDevice.currentValue("scene3")
    def currentScene = selectedDevice.currentValue("scene")
    def deviceInfo = selectedDevice.currentValue("IPinfo")
    def apiKey = selectedDevice.currentValue("retrievedAPIkey")
    def apiStatus = selectedDevice.currentValue("apiKeyStatus")
    def timerDelay = selectedDevice.currentValue("timerDelay")
    def sceneList = selectedDevice.currentValue("scenesList").replaceAll("[^a-zA-Z0-9, ]+","")
    def panelIds = selectedDevice.currentValue("panelIds").replaceAll(",", ", ")

    return dynamicPage(name: "pageInformation", title: "Device Information", nextPage: "pageMain") {
        section () {
            paragraph "Main Information: ${deviceInfo}\nAPI Status: ${apiStatus}\nAPI Key: ${apiKey}\nRefresh period: ${timerDelay}"
            paragraph "Curent scene: ${currentScene}"
            paragraph "Scene 1: ${scene1}\nScene 2: ${scene2}\nScene 3: ${scene3}"
            paragraph "Panel IDs: ${panelIds}"
            paragraph "Scene List: ${sceneList}"
        }
    }
}

def pageIdentifyPanels() {
    log.debug "Identify Panels"

    def panels = selectedDevice.currentValue("panelIds").split(",")
    def colors = ["Red", "Blue", "Green", "Yellow", "Orange", "Pink", "Purple", "Black", "White"]
    def setColor
    def colorIndex = 0
    def setNumber = 0
    def pageText = "${selectedDevice.name} panels:"

    for (int i = 0; i < panels.size(); i++) {
        if (colorIndex == 8) {setNumber++}
        colorIndex = i - (9 * setNumber)	
        setColor = colors[colorIndex]
        pageText = "${pageText}\nPanel Id ${panels[i]} is ${colors[colorIndex]}"
        selectedDevice.setPanelColor(panels[i], colors[colorIndex], false)
    }

    return dynamicPage(name: "pageIdentifyPanels", title: "Identify Panels", nextPage: "pageMain") {
        section () {
            paragraph pageText
        }
    }
}

private pageMissingData() {
    log.debug "Missing data"

	return dynamicPage(name: "pageMissingData", title: "Missing Data", nextPage: "pageMain") {
        section() {
            paragraph "Data hasn't been fully loaded yet, assuming the details are correct you can use the refresh option to force a data refresh"
        }
    }
}

private pageSetupHelp() {
	return dynamicPage(name: "pageSetupHelp", title: "Setup Help", nextPage: "pageMain") {
        section() {
            paragraph "To get setup there are a few steps:"
            paragraph "1. Install SmartApp and Device Handler"
            paragraph "2. Create a new device using the Device Handler via https://graph-eu01-euwest1.api.smartthings.com/ (or your local shard)"
            paragraph "3. Select the device you have created using the select option"
            paragraph "4. Get IP of your Nanoleaf and enter using the set IP and port option (don't worry about port unless you have changed the default)"
            paragraph "5. Hold the power button on your device for 5 seconds untill the control lights start flashing"
            paragraph "6. Select the get API key option, you will have about 30 seconds to do this"
            paragraph "7. Refresh the data via the refresh option"
        }
    }
}

private pageAcknowledgements() {
	return dynamicPage(name: "pageAcknowledgements", title: "Acknowledgements", nextPage: "pageMain") {
        section() {
            paragraph "Original code: https://github.com/Mellit7/NanoleafAuroraHandler"
            paragraph "Icons: https://iconapp.io"
        }
    }
}