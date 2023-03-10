/**
 *  Media Center
 *
 *  Copyright 2016 Michael Chang
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
    name: "Media Center",
    namespace: "miccrun",
    author: "Michael Chang",
    description: "Media Center",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "Media Center", displayLink: "https://github.com/miccrun/smartthings"])


preferences {
    page(name: "configurations", install: true, uninstall: true)
}

def configurations() {
    dynamicPage(name: "configurations", title: "Configurations") {
        section(title: "Select TV") {
            input "tv", "capability.switch", title: "TV", multiple: false, required: true
        }

        section(title: "Select Netflix Virtual Switch") {
            input "netflix", "capability.switch", title: "Netflix", multiple: false, required: true
        }

        section(title: "Select News Virtual Switch") {
            input "news", "capability.switch", title: "News", multiple: false, required: true
        }

        section(title: "Select PlayStation Virtual Switch") {
            input "playstation", "capability.switch", title: "PlayStation", multiple: false, required: true
        }

        section(title: "Select Media Center Virtual Switch") {
            input "mediacenter", "capability.switch", title: "Media Center", multiple: false, required: true
        }

        section ("Assign a name") {
            label title: "Assign a name", required: false
        }
    }
}

mappings {
  path("/tv/:command") {
    action: [
      PUT: "tvCommands"
    ]
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
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
    subscribe(netflix, "switch.on", switchOnHandler)
    subscribe(news, "switch.on", switchOnHandler)
    subscribe(playstation, "switch.on", switchOnHandler)
    subscribe(mediacenter, "switch.on", switchOnHandler)
}

def switchOnHandler(evt) {
    switch (evt.deviceId) {
        case netflix.id:
            runCommand("netflix")
            break
        case news.id:
            runCommand("news")
            break
        case playstation.id:
            runCommand("ps")
            break
        case mediacenter.id:
            runCommand("bilibili")
            break
    }
}

def lanResponseHandler(evt) {
    def parsedEvent = parseEventMessage(evt.description)
    if (parsedEvent.body) {
        def body = new String(parsedEvent.body.decodeBase64())
        if (body.find('uuid:02780011-440b-107f-8073-3052cbc59611')) {
            log.debug 'roku tv is on'
            state.rokuOn = true
            return
        }
    }
}

def tvOn() {
    log.debug 'turning on tv'
    //state.rokuOn = false
    //checkRokuOnline()
    if (tv.currentSwitch == 'on') {
        log.debug 'tv already on'
        launchRokuApp()
    } else {
        log.debug 'turn on tv, wait 30s'
        tv.on()
        runIn(30, launchRokuApp)
    }
}

def tvOff() {
    tv.off()
}

def checkRokuOnline() {
    log.debug "check if roku is online"
    def hubAction = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/",
        headers: [
            HOST: "192.168.29.101:8060"
        ]
    )
    sendHubCommand(hubAction)
    log.debug 'command send'
}

def launchRokuApp() {
    log.debug "launch app: ${state.appId}"
    def hubAction = new physicalgraph.device.HubAction(
        method: "POST",
        path: "/launch/${state.appId}",
        headers: [
            HOST: "192.168.29.101:8060"
        ]
    )
    sendHubCommand(hubAction)
}

def tvCommands() {
    runCommand(params.command)
}

def runCommand(command) {
    if (command == "on") {
        tvOn()
    }
    else if (command == "off") {
        tvOff()
    }
    else if (command == "netflix") {
        state.appId = "12"
        tvOn()
    }
    else if (command == "ps") {
        state.appId = "tvinput.hdmi2"
        tvOn()
    }
    else if (command == "bilibili") {
        state.appId = "tvinput.hdmi3"
        tvOn()
    }
    else if (command == "news") {
        state.appId = "27536"
        tvOn()
    }
}

private def parseEventMessage(String description) {
    def event = [:]
    def parts = description.split(',')
    parts.each { part ->
    part = part.trim()
        if (part.startsWith('devicetype:')) {
            def valueString = part.split(":")[1].trim()
            event.devicetype = valueString
        }
        else if (part.startsWith('mac:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.mac = valueString
            }
        }
        else if (part.startsWith('networkAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.ip = valueString
            }
        }
        else if (part.startsWith('deviceAddress:')) {
            def valueString = part.split(":")[1].trim()
            if (valueString) {
                event.port = valueString
            }
        }
        else if (part.startsWith('headers:')) {
            part -= "headers:"
            def valueString = part.trim()
            if (valueString) {
                event.headers = valueString
            }
        }
        else if (part.startsWith('body:')) {
            part -= "body:"
            def valueString = new String(part.trim())
            //def valueString = new String(part)
            //log.debug "Raw Body: " + valueString.decodeBase64()
            if (valueString) {
                event.body = valueString
            }
        }
    }
    event
}
