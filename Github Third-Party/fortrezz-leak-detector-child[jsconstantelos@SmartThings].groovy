/**
 *  Leak Detector
 *
 *  Copyright 2016 Daniel Kurin
 *
 *  Updated by John Constantelos
 */
definition(
    name: "FortrezZ Leak Detector Child",
    namespace: "jsconstantelos",
    author: "FortrezZ, LLC",
    description: "Child SmartApp for leak detector rules",
    category: "Green Living",
    parent: "jsconstantelos:FortrezZ Leak Detector Parent",
    iconUrl: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-200-1.png",
    iconX2Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square-500.png",
    iconX3Url: "http://swiftlet.technology/wp-content/uploads/2016/05/logo-square.png")


preferences {
	page(name: "prefsPage", title: "Choose the detector behavior", install: true, uninstall: true)
}

def prefsPage() {
	def daysOfTheWeek = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
    dynamicPage(name: "prefsPage") {
        section("Set Leak Threshold by...") {
            input(name: "type", type: "enum", title: "Type of rule...", submitOnChange: true, options: ruleTypes())
        }

        if(type)
        {
            switch (type) {
                case "Mode (GPM and mode based)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
                        input(name: "gpm", type: "decimal", title: "GPM exceeds", required: true, defaultValue: 0.1)
                    }
                    section("Only in these modes") {
                        input(name: "modes", type: "mode", title: "select a mode(s)", multiple: true, required: true)
                    }
                    section ("Action") {
                    	input(name: "dev", type: "capability.actuator", title: "Choose a device to perform the action", required: false, submitOnChange: true)
                        if (dev) {
                        	input(name: "command", type: "enum", title: "Command...", submitOnChange: true, options: deviceCommands(dev))
                    	}
                    }
                    break

                case "Time Period (GPM and time based)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
                        input(name: "gpm", type: "decimal", title: "GPM exceeds", required: true)
                    }
                    section("Between...") {
                    	input(name: "startTime", type: "time", title: "Start Time", required: true)
                    }
                    section("...and...") {
                    	input(name: "endTime", type: "time", title: "End Time", required: true)
                    }
                    section("Only on these days") {
                    	input(name: "days", type: "enum", title: "Days of the week", required: false, options: daysOfTheWeek, multiple: true)
                    }
                    section("Only in these modes") {
                    	input(name: "modes", type: "mode", title: "System Modes", required: false, multiple: true)
                    }
                    section ("Action") {
                    	input(name: "dev", type: "capability.actuator", title: "Choose a device to perform the action", required: false, submitOnChange: true)
                        if (dev) {
                        	input(name: "command", type: "enum", title: "Command...", submitOnChange: true, options: deviceCommands(dev))
                    	}
                    }
                    break

                case "Continuous Flow (Gallons and time based)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
                        input(name: "gallons", type: "number", title: "Total Gallons exceeds", required: true)
                    }
                    section("Between...") {
                    	input(name: "startTime", type: "time", title: "Start Time", required: true)
                    }
                    section("...and...") {
                    	input(name: "endTime", type: "time", title: "End Time", required: true)
                    }
                    section("Only on these days") {
                    	input(name: "days", type: "enum", title: "Days of the week", required: false, options: daysOfTheWeek, multiple: true)
                    }
                    section("Only in these modes") {
                    	input(name: "modes", type: "mode", title: "System Modes", required: false, multiple: true)
                    }
                    section ("Action") {
                    	input(name: "dev", type: "capability.actuator", title: "Choose a device to perform the action", required: false, submitOnChange: true)
                        if (dev) {
                        	input(name: "command", type: "enum", title: "Command...", submitOnChange: true, options: deviceCommands(dev))
                    	}
                    }
                    break

                case "Continuous Flow (GPM over time)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
	                    input(name: "flowMinutes", type: "number", title: "Minutes of constant flow", required: true, defaultValue: 60)
                    }
                    section("Only in these modes") {
                    	input(name: "modes", type: "mode", title: "System Modes", required: false, multiple: true)
                    }
                    section ("Action") {
                    	input(name: "dev", type: "capability.actuator", title: "Choose a device to perform the action", required: false, submitOnChange: true)
                        if (dev) {
                        	input(name: "command", type: "enum", title: "Command...", submitOnChange: true, options: deviceCommands(dev))
                    	}
                    }
                    break

                case "Total Flow (Gallons since last reset)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
                        input(name: "gallons", type: "number", title: "Total Gallons exceeds", required: true)
                    }
                    section("Only in these modes") {
                        input(name: "modes", type: "mode", title: "System Modes", required: false, multiple: true)
                    }
                    section ("Action") {
                    	input(name: "dev", type: "capability.actuator", title: "Choose a device to perform the action", required: false, submitOnChange: true)
                        if (dev) {
                        	input(name: "command", type: "enum", title: "Command...", submitOnChange: true, options: deviceCommands(dev))
                    	}
                    }
                    break

                case "Water Valve Status (GPM and valve state)":
                    section("Threshold settings") {
                        input(name: "ruleName", type: "text", title: "Rule Name", required: true)
                        input(name: "gpm", type: "decimal", title: "GPM exceeds", required: true, defaultValue: 0.1)
                    }
                    section ("While...") {
                        input(name: "valve", type: "capability.valve", title: "Choose a valve", required: true)
                    }
                    section ("...is...") {
                        input(name: "valveStatus", type: "enum", title: "Status", options: ["Open","Closed"], required: true)
                    }
                   break

                default:
                    break
            }
        }
    }
}

def ruleTypes() {
	def types = []
    types << "Mode (GPM and mode based)"
    types << "Time Period (GPM and time based)"
    types << "Continuous Flow (Gallons and time based)"
    types << "Continuous Flow (GPM over time)"
    types << "Total Flow (Gallons since last reset)"
    types << "Water Valve Status (GPM and valve state)"
    
    return types
}

def actionTypes() {
	def types = []
    types << [name: "Switch", capability: "capabilty.switch"]
    types << [name: "Water Valve", capability: "capability.valve"]
    
    return types
}

def deviceCommands(dev) {
	def cmds = []
	dev.supportedCommands.each { command ->
    	cmds << command.name
    }
    return cmds
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	app.updateLabel("${ruleName ? ruleName : ""} - ${type}")
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	app.updateLabel("${ruleName ? ruleName : ""} - ${type}")
	unsubscribe()
	initialize()
}

def settings() {
	def set = settings
    if (set["dev"] != null) {
    	log.debug("dev set: ${set.dev}")
    	set.dev = set.dev.id
    }
    if (set["valve"] != null) {
    	log.debug("valve set: ${set.valve}")
    	set.valve = set.valve.id
    }
    log.debug(set)
	return set
}

def devAction(action) {
	if(dev) {
    	log.debug("device: ${dev}, action: ${action}")
		dev."${action}"()
    }
}

def isValveStatus(status) {
	def result = false
    log.debug("Water Valve ${valve} has status ${valve.currentState("contact").value}, compared to ${status.toLowerCase()}")
	if (valve) {
    	if(valve.currentState("contact").value == status.toLowerCase()) {
        	result = true
        }
    }
    return result
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
}