/**
 *  RebootRoku
 *
 *  Version 1.0.2 - 08/26/16
 *   -- Added app touch to reboot all of your rokus at once.
 *  Version 1.0.1 - 08/13/16
 *   -- Added the ability to select a day of the week or multiple. 
 *  Version 1.0.0 - 08/12/16
 *   -- Initial Build
 *
 *  Tips
 *  1. You need to use the ip address of your roku(s), so you might want to make them static ip assignments in your router.
 *  2. You can customize keypresses to match your roku, make sure you start with home,home to ensure you are starting from the correct place.
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
 *  You can find this smart app @ https://github.com/ericvitale/ST-RebootRoku
 *  You can find my other device handlers & SmartApps @ https://github.com/ericvitale
 *
 */
 
definition(
    name: "${appName()}",
    namespace: "ericvitale",
    author: "Eric Vitale",
    description: "Reboots your roku(s).",
    category: "",
    singleInstance: true,
    iconUrl: "https://s3.amazonaws.com/ev-public/st-images/reboot-roku-icon.png", 
    iconX2Url: "https://s3.amazonaws.com/ev-public/st-images/reboot-roku-icon-2x.png", 
    iconX3Url: "https://s3.amazonaws.com/ev-public/st-images/reboot-roku-icon-3x.png")

preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
}

def startPage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	return dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
        section("Create a new child app.") {
            app(name: "childApps", appName: appName(), namespace: "ericvitale", title: "New Roku Automation", multiple: true)
        }
        
        section("Settings") {
        	input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
            input "appTouch", "bool", title: "Enable App Touch?", required: true, defaultValue: true
        }
    }
}
 
def childStartPage() {
	return dynamicPage(name: "childStartPage", title: "", install: true, uninstall: true) {
    
    	section("Roku") {
            input "roku", "text", title: "Roku IP:", required: true
            input "rokuKeys", "text", title: "Reset Keystrokes", required: true, defaultValue: "home,left,home,up,right,up,right,up,up,up,up,right,select"
            input "keyWait", "number", title: "Time Between Keypress (ms)", required: true, defaultValue: 500, range: "250..5000",  description: "Can't be too short as the commands could get out of sync, too long and you will timeout the SmartApp."
    	}
        
        section("Trigger") {
        	input "switches", "capability.switch", title: "Switches", multiple: true, required: false, description: "Only needed for testing."
        }
        
        section("Setting") {
        	label(title: "Assign a name", required: false)
            input "hour", "text", title: "Hour of the Day", required: true, defaultValue: "2", range: "0..23"
            input "minute", "text", title: "Minute of the Hour", required: true, defaultValue: "0", range: "0..59"
            input "daysOfWeek", "enum", title: "Specific Day(s) of the Week", required: false, multiple: true, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "active", "bool", title: "Rules Active?", required: true, defaultValue: true
            input "logging", "enum", title: "Log Level", required: true, defaultValue: "INFO", options: ["TRACE", "DEBUG", "INFO", "WARN", "ERROR"]
        }
	}
}

private def appName() { return "${parent ? "Roku Automation" : "RebootRoku"}" }

private determineLogLevel(data) {
    switch (data?.toUpperCase()) {
        case "TRACE":
            return 0
            break
        case "DEBUG":
            return 1
            break
        case "INFO":
            return 2
            break
        case "WARN":
            return 3
            break
        case "ERROR":
        	return 4
            break
        default:
            return 1
    }
}

def log(data, type) {
    data = "RR -- ${data ?: ''}"
        
    if (determineLogLevel(type) >= determineLogLevel(settings?.logging ?: "INFO")) {
        switch (type?.toUpperCase()) {
            case "TRACE":
                log.trace "${data}"
                break
            case "DEBUG":
                log.debug "${data}"
                break
            case "INFO":
                log.info "${data}"
                break
            case "WARN":
                log.warn "${data}"
                break
            case "ERROR":
                log.error "${data}"
                break
            default:
                log.error "RR -- Invalid Log Setting"
        }
    }
}

def installed() {
	log("Begin installed.", "DEBUG")
	initialization() 
    log("End installed.", "DEBUG")
}

def updated() {
	log("Begin updated().", "DEBUG")
	unsubscribe()
    unschedule()
	initialization()
    log("End updated().", "DEBUG")
}

def initialization() {
	log("Begin initialization().", "DEBUG")
    
    if(parent) { 
    	initChild() 
    } else {
    	initParent() 
    }
    
    log("End initialization().", "DEBUG")
}

def initParent() {
	log("initParent()", "DEBUG")
    
    unsubscribe()
    
    if(appTouch) {
	    subscribe(app, appHandler)
        log("App touch enabled.", "INFO")
    }
}

def initChild() {
	log("Begin intialization().", "DEBUG")
    
    log("active = ${active}.", "INFO")
    
    unschedule()
    unsubscribe()
    
    def days = buildDayOfWeekString()
    
    log("Schedule will be active on days: ${days}.", "INFO")
    log("Schedule will be active on hour ${hour} and minute ${minute}.", "INFO")
    
    if(active) {
        log("CRON = <<<44 ${minute} ${hour} ? * ${days}>>>", "DEBUG")
        schedule("44 ${minute} ${hour} ? * ${days}", rebootRokus)
        subscribe(switches, "switch", switchHandler)
        log("Subscriptions to devices made.", "INFO")   
    } else {
    	log("App is set to inactive in settings.", "INFO")
    }
    
    log("Roku = ${roku}", "INFO")
    log("Roku Reboot Keys = ${rokuKeys}.", "INFO")
    log("Time between keypresses: ${keyWait}.", "INFO")

    log("End initialization().", "DEBUG")
}

def appHandler(evt) {
    log("App Touch Event Triggered.", "INFO")
    childApps.each {child ->
        if(child.isActive()) {
        	log("child app: ${child.label} is active, sending reboot request.", "INFO")
            child.rebootRokus()
        } else {
			log("child app: ${child.label} is not active.", "INFO")
		}
    }
}

def switchHandler(evt) {
	log("Manual Trigger!", "INFO")
    rebootRokus()
}

def rebootRokus() {
	runIn(1, scheduleReboot)
    log("Scheduled reboot for roku ip: ${roku}.", "INFO")
}

def sendRebootToRoku() {
	log("sendRebootToRoku(${roku}, ${rokuKeys}.", "INFO")
    
	if(roku != null) {        

		def keyMap = [:]        
        log("Keys = ${rokuKeys}.", "DEBUG")        
        keyMap = rokuKeys.split(",")
        
        log("Key map to be sent = ${keyMap}.", "DEBUG")
        log("Preparing to reboot Roku ${roku}.", "INFO")
        
        keyMap.each { key->
        	sendKeyPressToRoku(roku, key)
            pause(500)
        }
    } else {
    	log("No roku selected", "ERROR")
    }
}

def scheduleReboot() {
    log("Running Reboot for roku ip: ${roku}.", "DEBUG")
    sendRebootToRoku()
}

def sendKeyPressToRoku(ip, key) {
	log("ip = ${ip} & key = ${key}.", "DEBUG")
    def httpRequest = [
    	method:		"POST",
        path: 		"/keypress/${key}",
        headers: [
                 	HOST:		"${ip}:8060",
                    Accept: 	"*/*",
                 ]
    ]

    def hubAction = new physicalgraph.device.HubAction(httpRequest)
    sendHubCommand(hubAction)
}

def buildDayOfWeekString() {
	def days = ""
    
    daysOfWeek.each { it->
    	log("Day: ${it}", "DEBUG")
    	days += it.substring(0, 3).toUpperCase().trim()
        days += ","
        log("Days = ${days}.", "DEBUG")
    }
    
    if(days == "") {
    	days = "*"
    } else {
    	days = days?.substring(0, days?.length() - 1)
    }
    
    return days
}

def isActive() {
	return active
}