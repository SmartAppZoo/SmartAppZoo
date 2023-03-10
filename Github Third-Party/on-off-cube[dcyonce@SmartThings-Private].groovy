/**
 *  On/Off Cube
 *
 *  8/10/2017  Initial Coding       dcyonce
 */

definition(
	name: "On/Off Cube",
	namespace: "dcyonce",
	author: "Don Yonce",
	description: "Turn On/Off lights by rotating a MultiSensor",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

preferences {
	page(name: "mainPage", title: "", nextPage: "scenesPage", uninstall: true) {
		section("On/Off Cube - dcyonce") {}
		section("Select a Sensor to control lights  ...") {
			input "cube", "capability.threeAxis", required: false, title: "SmartSense Multi sensor"
		}
		section("To control these lights") {
			input "lights", "capability.switch", multiple: true, required: false, title: "Lights, switches & dimmers"
		}
		section([title: " ", mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
	page(name: "scenesPage", title: "Scenes", install: true, uninstall: true)
	page(name: "scenePage", title: "Scene", install: false, uninstall: false, previousPage: "scenesPage")
	page(name: "devicePage", install: false, uninstall: false, previousPage: "scenePage")
	page(name: "saveStatesPage", install: false, uninstall: false, previousPage: "scenePage")
}


def scenesPage() {
	log.debug "scenesPage()"
	def sceneId = getOrientation()
	dynamicPage(name:"scenesPage") {
		section {
			href "scenePage", title: "1:Top Edge - ${sceneName(1)}${sceneId==1 ? ' (current)' : ''}", params: [sceneId:1], description: "", state: sceneIsDefined(1) ? "complete" : "incomplete"
			href "scenePage", title: "2:Bottom Edge - ${sceneName(2)}${sceneId==2 ? ' (current)' : ''}", params: [sceneId:2], description: "", state: sceneIsDefined(2) ? "complete" : "incomplete"
			href "scenePage", title: "3:Face Down - ${sceneName(3)}${sceneId==3 ? ' (current)' : ''}", params: [sceneId:3], description: "", state: sceneIsDefined(3) ? "complete" : "incomplete"
			href "scenePage", title: "4:Face Up - ${sceneName(4)}${sceneId==4 ? ' (current)' : ''}", params: [sceneId:4], description: "", state: sceneIsDefined(4) ? "complete" : "incomplete"
			href "scenePage", title: "5:Left Edge - ${sceneName(5)}${sceneId==5 ? ' (current)' : ''}", params: [sceneId:5], description: "", state: sceneIsDefined(5) ? "complete" : "incomplete"
			href "scenePage", title: "6:Right Edge - ${sceneName(6)}${sceneId==6 ? ' (current)' : ''}", params: [sceneId:6], description: "", state: sceneIsDefined(6) ? "complete" : "incomplete"
		}
		section {
			href "scenesPage", title: "Refresh", description: ""
		}
	}
}

def scenePage(params=[:]) {
	log.debug "scenePage($params)"
	def currentSceneId = getOrientation()
	def sceneId = params.sceneId as Integer ?: state.lastDisplayedSceneId
	state.lastDisplayedSceneId = sceneId
	dynamicPage(name:"scenePage", title: "${sceneId}. ${sceneName(sceneId)}") {
		section {
			input "sceneName${sceneId}", "text", title: "Scene Name", required: false
		}

		section {
			href "devicePage", title: "Show Device States", params: [sceneId:sceneId], description: "", state: sceneIsDefined(sceneId) ? "complete" : "incomplete"
		}

        section {
            href "saveStatesPage", title: "Record Current Device States", params: [sceneId:sceneId], description: ""
        }
	}
}

def devicePage(params) {
	log.debug "devicePage($params)"

	getDeviceCapabilities()

	def sceneId = params.sceneId as Integer ?: state.lastDisplayedSceneId

	dynamicPage(name:"devicePage", title: "${sceneId}. ${sceneName(sceneId)} Device States") {
		section("Lights") {
			lights.each {light ->
				input "onoff_${sceneId}_${light.id}", "boolean", title: light.displayName
			}
		}

		section("Dimmers") {
			lights.each {light ->
				if (state.lightCapabilities[light.id] in ["level", "color"]) {
					input "level_${sceneId}_${light.id}", "enum", title: light.displayName, options: levels, description: "", required: false
				}
			}
		}

		section("Colors (hue/saturation)") {
			lights.each {light ->
				if (state.lightCapabilities[light.id] == "color") {
					input "color_${sceneId}_${light.id}", "text", title: light.displayName, description: "", required: false
				}
			}
		}
	}
}

def saveStatesPage(params) {
	saveStates(params)
	devicePage(params)
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
	subscribe cube, "threeAxis", positionHandler
}


def positionHandler(evt) {

	def sceneId = getOrientation(evt.xyzValue)
	log.trace "orientation: $sceneId"

	if (sceneId != state.lastActiveSceneId) {
		restoreStates(sceneId)
	}
	else {
		log.trace "No status change"
	}
	state.lastActiveSceneId = sceneId
}


/******************
 * Helper methods *
 ******************/
private Boolean sceneIsDefined(sceneId) {
	def tgt = "onoff_${sceneId}".toString()
	settings.find{it.key.startsWith(tgt)} != null
}

private updateSetting(name, value) {
	app.updateSetting(name, value)
	settings[name] = value
}

private closestLevel(level) {
	level ? "${Math.round(level/5) * 5}%" : "0%"
}

private saveStates(params) {
	log.trace "saveStates($params)"
	def sceneId = params.sceneId as Integer
	getDeviceCapabilities()

	lights.each {light ->
		def type = state.lightCapabilities[light.id]

		updateSetting("onoff_${sceneId}_${light.id}", light.currentValue("switch") == "on")

		if (type == "level") {
			updateSetting("level_${sceneId}_${light.id}", closestLevel(light.currentValue('level')))
		}
		else if (type == "color") {
			updateSetting("level_${sceneId}_${light.id}", closestLevel(light.currentValue('level')))
			updateSetting("color_${sceneId}_${light.id}", "${light.currentValue("hue")}/${light.currentValue("saturation")}")
		}
	}
}


private restoreStates(sceneId) {
	log.trace "restoreStates($sceneId)"
	getDeviceCapabilities()

	lights.each {light ->
		def type = state.lightCapabilities[light.id]

		def isOn = settings."onoff_${sceneId}_${light.id}" == "true" ? true : false
		log.debug "${light.displayName} is '$isOn'"
		if (isOn) {
			light.on()
		}
		else {
			light.off()
		}

		if (type != "switch") {
			def level = switchLevel(sceneId, light)

			if (type == "level") {
				log.debug "${light.displayName} level is '$level'"
				if (level != null) {
					light.setLevel(level)
				}
			}
			else if (type == "color") {
				def segs = settings."color_${sceneId}_${light.id}"?.split("/")
				if (segs?.size() == 2) {
					def hue = segs[0].toInteger()
					def saturation = segs[1].toInteger()
					log.debug "${light.displayName} color is level: $level, hue: $hue, sat: $saturation"
					if (level != null) {
						light.setColor(level: level, hue: hue, saturation: saturation)
					}
					else {
						light.setColor(hue: hue, saturation: saturation)
					}
				}
				else {
					log.debug "${light.displayName} level is '$level'"
					if (level != null) {
						light.setLevel(level)
					}
				}
			}
			else {
				log.error "Unknown type '$type'"
			}
		}


	}
}

private switchLevel(sceneId, light) {
	def percent = settings."level_${sceneId}_${light.id}"
	if (percent) {
		percent[0..-2].toInteger()
	}
	else {
		null
	}
}

private getDeviceCapabilities() {
	def caps = [:]
	lights.each {
		if (it.hasCapability("Color Control")) {
			caps[it.id] = "color"
		}
		else if (it.hasCapability("Switch Level")) {
			caps[it.id] = "level"
		}
		else {
			caps[it.id] = "switch"
		}
	}
	state.lightCapabilities = caps
}

private getLevels() {
	def levels = []
	for (int i = 0; i <= 100; i += 5) {
		levels << "$i%"
	}
	levels
}

private getOrientation(xyz=null) {
	final threshold = 250

	def value = xyz ?: cube.currentValue("threeAxis")

	def x = Math.abs(value.x) > threshold ? (value.x > 0 ? 1 : -1) : 0
	def y = Math.abs(value.y) > threshold ? (value.y > 0 ? 1 : -1) : 0
	def z = Math.abs(value.z) > threshold ? (value.z > 0 ? 1 : -1) : 0

	def orientation = 0
	if (z > 0) {
		if (x == 0 && y == 0) {
        	// Standing on top edge
			orientation = 1
		}
	}
	else if (z < 0) {
		if (x == 0 && y == 0) {
        	// Standing on bottom edge
			orientation = 2
		}
	}
	else {
		if (x > 0) {
			if (y == 0) {
            	// upside down
				orientation = 3
			}
		}
		else if (x < 0) {
			if (y == 0) {
            	// Normal face-up orientation
				orientation = 4
			}
		}
		else {
			if (y > 0) {
            	// standing on left edge
				orientation = 5
			}
			else if (y < 0) {
            	// standing on right edge
				orientation = 6
			}
		}
	}

	log.debug("orientation=${orientation}")
	return orientation
}

private sceneName(num) {
	final names = ["UNDEFINED","One","Two","Three","Four","Five","Six"]
    log.debug("Scene num ${num}")
    if (num != null) 
    {
		settings."sceneName${num}" ?: "Scene ${names[num]}"
    }
}