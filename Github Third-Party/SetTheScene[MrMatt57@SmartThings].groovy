/**
 *  Set the Scene
 *
 *  Author: MrMatt57 (mwwalker@gmail.com)
 *  Date: 2014-02-14
 *	Description: Saves the state of the selected switches during install and update.  Running 
 *  	the app restores the switches to the state captured during install/update.  Base don 
 *  	code from the Make it so example application.
 */

preferences {
	section("Switches to remember...") {
		input "switches", "capability.switch", multiple: true
    }
}

def installed()
{
	initialize()
}

def updated()
{
	unsubscribe()
	initialize()	
}

def initialize() {
	subscribe(app, appTouch)
    saveState()
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	restoreState()
}

private restoreState()
{
	def mode = "SwitchState"
	log.info "restoring state for mode '$mode'"
	def map = state[mode] ?: [:]
	switches?.each {
		def value = map[it.id]
		if (value?.switch == "on") {
			def level = value.level
			if (level) {
				log.debug "setting $it.label level to $level"
				it.setLevel(level)
			}
			else {
				log.debug "turning $it.label on"
				it.on()
			}
		}
		else if (value?.switch == "off") {
			log.debug "turning $it.label off"
			it.off()
		}
	}
}


private saveState()
{
	def mode = "SwitchState"
	def map = state[mode] ?: [:]

	switches?.each {
		map[it.id] = [switch: it.currentSwitch, level: it.currentLevel]
	}

	state[mode] = map
	log.debug "saved state for mode ${mode}: ${state[mode]}"
	log.debug "state: $state"
}