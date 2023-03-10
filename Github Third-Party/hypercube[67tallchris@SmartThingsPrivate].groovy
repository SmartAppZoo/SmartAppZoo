/**
 *  HyperCube
 *  Based on skp19 code
 *  Copyright 2015 skp19
 *
 *
 */

/************
 * Metadata *
 ************/
definition(
	name: "HyperCube",
	namespace: "primalmotion",
	author: "primalmotion",
	description: "Run a Hello Home action by rotating a cube containing a SmartSense Multi",
	category: "Convenience",
	iconUrl: "http://i.imgur.com/bLZGbTH.png",
	iconX2Url: "http://i.imgur.com/bLZGbTH.png"
)

/**********
 * Setup  *
 **********/
preferences {
	page(name: "mainPage", title: "", nextPage: "scenesPage", uninstall: true) {
		section("Use the orientation of this cube") {
			input "cube", "capability.threeAxis", required: false, title: "SmartSense Multi sensor"
		}
		section([title: " ", mobileOnly:true]) {
			label title: "Assign a name", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
	page(name: "scenesPage", title: "Scenes", install: true, uninstall: true)
}


def scenesPage() {
	log.debug "scenesPage()"
	def sceneId = getOrientation()
	dynamicPage(name:"scenesPage") {
    	def phrases = location.helloHome?.getPhrases()*.label
        phrases.sort()
        section("Face 1 ${sceneId==1 ? ' (current)' : ''}") {
            input name: "homeAction1", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn1", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff1", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        section("Face 2 ${sceneId==2 ? ' (current)' : ''}") {
            input name: "homeAction2", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn2", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff2", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        section("Face 3 ${sceneId==3 ? ' (current)' : ''}") {
            input name: "homeAction3", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn3", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff3", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        section("Face 4 ${sceneId==4 ? ' (current)' : ''}") {
            input name: "homeAction4", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn4", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff4", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        section("Face 5 ${sceneId==5 ? ' (current)' : ''}") {
            input name: "homeAction5", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn5", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff5", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
        section("Face 6 ${sceneId==6 ? ' (current)' : ''}") {
            input name: "homeAction6", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn6", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff6", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
		section("At Default Position") {
            input name: "homeAction7", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn7", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff7", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }
		section("At Different Position") {
            input name: "homeAction8", type: "enum", title: "Home Action", required: false, options: phrases
            input "switchOn8", "capability.switch", title: "Turn these switches on", multiple: true, required: false
            input "switchOff8", "capability.switch", title: "Turn these switches off", multiple: true, required: false
        }

		section {
			href "scenesPage", title: "Refresh", description: ""
		}
	}
}


/*************************
 * Installation & update *
 *************************/
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
    // subscribe cube, "acceleration", positionHandler
    subscribe cube, "contact", contactHandler
}


/******************
 * Event handlers *
 ******************/
def positionHandler(evt) {

	final sceneId = getOrientation(cube.currentXyzValue)
	log.trace "orientation: $sceneId"
	
    if (sceneId != state.lastActiveSceneId) {
	    runSwitchOff(sceneId)
    	runSwitchOn(sceneId)
        runHomeAction(sceneId)
    }
	else {
		log.trace "No status change"
	}
	state.lastActiveSceneId = sceneId
}

def contactHandler(evt) {

	def sceneId = $evt.value == "open" ? 8 : 9
    log.trace "contact ${evt.value} : $sceneId"
    
	if (sceneId != state.lastActiveSceneId) {
        runSwitchOff(sceneId)
        runSwitchOn(sceneId)
        runHomeAction(sceneId)
	}
	else {
		log.trace "No status change"
	}
	state.lastActiveSceneId = sceneId
}


/******************
 * Helper methods *
 ******************/
private runHomeAction(sceneId) {
	log.trace "runHomeAction($sceneId)"
    
	if (sceneId == 1)
	    run_home_action(homeAction1)
    else if (sceneId == 2)
    	run_home_action(homeAction2)
    else if (sceneId == 3)
    	run_home_action(homeAction3)
    else if (sceneId == 4)
    	run_home_action(homeAction4)
    else if (sceneId == 5)
    	run_home_action(homeAction5)
    else if (sceneId == 6)
    	run_home_action(homeAction6)
    else if (sceneId == 7)
    	run_home_action(homeAction7)
    else if (sceneId == 8)
    	run_home_action(homeAction8)
}

private runSwitchOff(sceneId) {
	log.trace "runSwitchOff($sceneId)"
  
	if (sceneId == 1)
	    set_switch_state(settings.switchOff1, false)
    else if (sceneId == 2)
    	set_switch_state(settings.switchOff2, false)
    else if (sceneId == 3)
    	set_switch_state(settings.switchOff3, false)
    else if (sceneId == 4)
    	set_switch_state(settings.switchOff4, false)
    else if (sceneId == 5)
    	set_switch_state(settings.switchOff5, false)
    else if (sceneId == 6)
    	set_switch_state(settings.switchOff6, false)
    else if (sceneId == 7)
    	set_switch_state(settings.switchOff7, false)
    else if (sceneId == 8)
    	set_switch_state(settings.switchOff8, false)
}

private runSwitchOn(sceneId) {
	log.trace "runSwitchOn($sceneId)"
  
	if (sceneId == 1)
	    set_switch_state(settings.switchOn1, true)
    else if (sceneId == 2)
    	set_switch_state(settings.switchOn2, true)
    else if (sceneId == 3)
    	set_switch_state(settings.switchOn3, true)
    else if (sceneId == 4)
    	set_switch_state(settings.switchOn4, true)
    else if (sceneId == 5)
    	set_switch_state(settings.switchOn5, true)
    else if (sceneId == 6)
    	set_switch_state(settings.switchOn6, true)
    else if (sceneId == 7)
    	set_switch_state(settings.switchOn7, true)
    else if (sceneId == 8)
    	set_switch_state(settings.switchOn8, true)
}


def set_switch_state(switches, state) {
	switches.collect { s -> state ? s.on() : s.off() }
}

def run_home_action(homeAction) {
	if (homeAction)
		location.helloHome.execute(homeAction)
}

private getOrientation(xyz=null) {

	if (!cube)
    	return 0
    
	final threshold = 250

	def value = xyz ?: cube.currentValue("threeAxis")

	def x = Math.abs(value.x) > threshold ? (value.x > 0 ? 1 : -1) : 0
	def y = Math.abs(value.y) > threshold ? (value.y > 0 ? 1 : -1) : 0
	def z = Math.abs(value.z) > threshold ? (value.z > 0 ? 1 : -1) : 0

	def orientation = 0
	if (z > 0) {
		if (x == 0 && y == 0) {
			orientation = 1
		}
	}
	else if (z < 0) {
		if (x == 0 && y == 0) {
			orientation = 2
		}
	}
	else {
		if (x > 0) {
			if (y == 0) {
				orientation = 3
			}
		}
		else if (x < 0) {
			if (y == 0) {
				orientation = 4
			}
		}
		else {
			if (y > 0) {
				orientation = 5
			}
			else if (y < 0) {
				orientation = 6
			}
		}
	}

	orientation
}
