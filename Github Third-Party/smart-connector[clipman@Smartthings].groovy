/**
 *	Smart Connector v2021-08-15
 *	clipman@naver.com
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 *  스마트 앱 추가 가능
 */

definition(
	name: "Smart Connector",
	namespace: "clipman",
	author: "clipman",
	description: "A Connector between RealDevice and SmartDevice",
	category: "My Apps",
	iconUrl: "https://cdn2.iconfinder.com/data/icons/ballicons-2-free/100/smart_watch-512.png",
	iconX2Url: "https://cdn2.iconfinder.com/data/icons/ballicons-2-free/100/smart_watch-512.png",
	iconX3Url: "https://cdn2.iconfinder.com/data/icons/ballicons-2-free/100/smart_watch-512.png",
	singleInstance: false,
	pausable: false
)

preferences {
   page(name: "mainPage")
   page(name: "airConditionerPage")
   page(name: "airConditionerWallPage")
   page(name: "airPurifierPage")
   page(name: "airMonitorPage")
   page(name: "deleteDevicePage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "", nextPage: null, uninstall: true, install: true) {
		section("Smart Connector 설정하기"){
			input "stToken", "text", title: "SmartThings Token", required: true
		}
		section("Smart 가상장치 만들기"){
			href "airConditionerPage", title: "에어컨(스탠드) 장치", description:""
			href "airConditionerWallPage", title: "에어컨(벽걸이) 장치", description:""
			href "airPurifierPage", title: "공기청정기 장치", description:""
			href "airMonitorPage", title: "에어모니터 장치", description:""
		}
		section("Smart 가상장치 삭제하기"){
			href "deleteDevicePage", title: "추가된 Smart 가상장치 삭제", description:""
			label title: "App Label (optional)", description: "Rename this App", defaultValue: app?.name, required: false
		}
		section("만든이"){
			paragraph "김민수 clipman@naver.com [날자]\n네이버카페: Smartthings & IoT home Community\nSmart Connector v2021-08-15"
		}
	}
}

def airConditionerPage() {
	dynamicPage(name: "airConditionerPage", title: "")
	{
		section("에어컨(스탠드) 장치") {
			input(name: "airConditioner", type: "capability.airConditionerMode", title: "에어컨 선택", multiple: false, required: false)
		}
	}
}

def airConditionerWallPage() {
	dynamicPage(name: "airConditionerWallPage", title: "")
	{
		section("에어컨(벽걸이) 장치") {
			input(name: "airConditionerWall", type: "capability.airConditionerMode", title: "에어컨 선택", multiple: false, required: false)
		}
	}
}

def airPurifierPage() {
	dynamicPage(name: "airPurifierPage", title: "")
	{
		section("공기청정기 장치") {
			input(name: "airPurifier", type: "capability.dustSensor", title: "공기청정기 선택", multiple: false, required: false)
		}
	}
}

def airMonitorPage() {
	dynamicPage(name: "airMonitorPage", title: "")
	{
		section("에어모니터 장치") {
			input(name: "airMonitor", type: "capability.carbonDioxideMeasurement", title: "에어모니터 선택", multiple: false, required: false)
		}
	}
}

def deleteDevicePage(){
	def list = []
	def childDevices = getAllChildDevices()
	list.push("None")
	childDevices.each {childDevice->
		list.push(childDevice.label + " : " + childDevice.deviceNetworkId)
	}
	dynamicPage(name: "deleteDevicePage", nextPage: "mainPage") {
		section ("Delete Smart Device") {
			input(name: "selectedDeleteSmartDevice", title:"Select" , type: "enum", required: true, options: list, defaultValue: "None")
		}
	}
}

def installed() {
	//log.debug "Installed with settings: ${settings}"
	state.appID = Math.abs(new Random().nextInt() % 9999) + 1
}

def uninstalled() {
	if(getAirConditioner()) deleteChildDevice("smart_airconditioner_" + state.appID)
	if(getAirConditionerWall()) deleteChildDevice("smart_airconditionerwall_" + state.appID)
	if(getAirPurifier()) deleteChildDevice("smart_airpurifier_" + state.appID)
	if(getAirMonitor()) deleteChildDevice("smart_airmonitor_" + state.appID)
}

def updated() {
	log.info "Updated with settings: ${settings}"

	unsubscribe()

	if(settings.airConditioner) {
		subscribe(settings.airConditioner, "switch", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "airConditionerMode", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "fanMode", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "temperature", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "coolingSetpoint", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "humidity", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "dustLevel", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "fineDustLevel", airConditionerChangeHandler)
		subscribe(settings.airConditioner, "odorLevel", airConditionerChangeHandler)
		addAirConditioner()
	}

	if(settings.airConditionerWall) {
		subscribe(settings.airConditionerWall, "switch", airConditionerWallChangeHandler)
		subscribe(settings.airConditionerWall, "airConditionerMode", airConditionerWallChangeHandler)
		subscribe(settings.airConditionerWall, "fanMode", airConditionerWallChangeHandler)
		subscribe(settings.airConditionerWall, "temperature", airConditionerWallChangeHandler)
		subscribe(settings.airConditionerWall, "coolingSetpoint", airConditionerWallChangeHandler)
		addAirConditionerWall()
	}

	if(settings.airPurifier) {
		subscribe(settings.airPurifier, "switch", airPurifierChangeHandler)
		subscribe(settings.airPurifier, "airPurifierFanMode", airPurifierChangeHandler)
		subscribe(settings.airPurifier, "dustLevel", airPurifierChangeHandler)
		subscribe(settings.airPurifier, "fineDustLevel", airPurifierChangeHandler)
		subscribe(settings.airPurifier, "odorLevel", airPurifierChangeHandler)
		addAirPurifier()
	}

	if(settings.airMonitor) {
		subscribe(settings.airMonitor, "dustLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "fineDustLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "veryFineDustLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "airQuality", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "odorLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "carbonDioxide", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "tvocLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "radonLevel", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "temperature", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "humidity", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "dustHealthConcern", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "fineDustHealthConcern", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "veryFineDustHealthConcern", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "carbonDioxideHealthConcern", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "tvocHealthConcern", airMonitorChangeHandler)
		subscribe(settings.airMonitor, "radonHealthConcern", airMonitorChangeHandler)
		addAirMonitor()
	}

	deleteChildDevice()
}

def airConditionerChangeHandler(evt) {
	def smartAirConditioner = getAirConditioner()
	if(smartAirConditioner) {
		smartAirConditioner.refresh()
	}
	log.debug "airConditionerChangeHandler: " + evt.displayName + ", " + evt.name
}

def airConditionerWallChangeHandler(evt) {
	def smartAirConditionerWall = getAirConditionerWall()
	if(smartAirConditionerWall) {
		smartAirConditionerWall.refresh()
	}
	log.debug "airConditionerWallChangeHandler: " + evt.displayName + ", " + evt.name
}

def airPurifierChangeHandler(evt) {
	def smartAirPurifier = getAirPurifier()
	if(smartAirPurifier) {
		smartAirPurifier.refresh()
	}
	log.debug "airPurifierChangeHandler: " + evt.displayName + ", " + evt.name
}

def airMonitorChangeHandler(evt) {
	def smartAirMonitor = getAirMonitor()
	if(smartAirMonitor) {
		smartAirMonitor.refresh()
	}
	log.debug "airMonitorChangeHandler: " + evt.displayName + ", " + evt.name
}

def addAirConditioner() {
	if(getAirConditioner() == null) {
		def dth = "SmartAirConditioner"
		def dni = "smart_airconditioner_" + state.appID
		def name = "Smart에어컨"
		try {
			def childDevice = addChildDevice("clipman", dth, dni, null, ["label": name])
			childDevice.setToken(settings.stToken, settings.airConditioner.id)
			childDevice.refresh()
		} catch(err) {
			log.error "Add SmartAirConditioner Device ERROR >> ${err}"
		}
	}
}

def addAirConditionerWall() {
	if(getAirConditionerWall() == null) {
		def dth = "SmartAirConditionerWall"
		def dni = "smart_airconditionerwall_" + state.appID
		def name = "Smart벽걸이"
		try {
			def childDevice = addChildDevice("clipman", dth, dni, null, ["label": name])
			childDevice.setToken(settings.stToken, settings.airConditionerWall.id)
			childDevice.refresh()
		} catch(err) {
			log.error "Add SmartAirConditionerWall Device ERROR >> ${err}"
		}
	}
}

def addAirPurifier() {
	if(getAirPurifier() == null) {
		def dth = "SmartAirPurifier"
		def dni = "smart_airpurifier_" + state.appID
		def name = "Smart청정기"
		try {
			def childDevice = addChildDevice("clipman", dth, dni, null, ["label": name])
			childDevice.setToken(settings.stToken, settings.airPurifier.id)
			childDevice.refresh()
		} catch(err) {
			log.error "Add SmartAirPurifier Device ERROR >> ${err}"
		}
	}
}

def addAirMonitor() {
	if(getAirMonitor() == null) {
		def dth = "SmartAirMonitor"
		def dni = "smart_airmonitor_" + state.appID
		def name = "Smart모니터"
		try{
			def childDevice = addChildDevice("clipman", dth, dni, null, ["label": name])
			childDevice.setToken(settings.stToken, settings.airMonitor.id)
			childDevice.refresh()
		} catch(err) {
			log.error "Add SmartAirMonitor Device ERROR >> ${err}"
		}
	}
}

def getAirConditioner() {
	def child = getChildDevices().find {
		d -> d.deviceNetworkId.startsWith("smart_airconditioner_" + state.appID)
	}
	return child
}

def getAirConditionerWall() {
	def child = getChildDevices().find {
		d -> d.deviceNetworkId.startsWith("smart_airconditionerwall_" + state.appID)
	}
	return child
}

def getAirPurifier() {
	def child = getChildDevices().find {
		d -> d.deviceNetworkId.startsWith("smart_airpurifier_" + state.appID)
	}
	return child
}

def getAirMonitor() {
	def child = getChildDevices().find {
		d -> d.deviceNetworkId.startsWith("smart_airmonitor_" + state.appID)
	}
	return child
}

def deleteChildDevice() {
	if(settings.selectedDeleteSmartDevice){
		if(settings.selectedDeleteSmartDevice != "None"){
			def nameAndDni = settings.selectedDeleteSmartDevice.split(" : ")
			try{
				deleteChildDevice(nameAndDni[1])
			}catch(err){

			}
		 }
	}
	app.updateSetting("selectedDeleteSmartDevice", "None")
}