/**
* Alexa Home Phrase Executor
*
* Copyright 2015 Ron S
* Version 1.0.0 09/01/15
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Ties a Hello, Home phrase to a switch's (virtual or real) on/off state. Perfect for use with IFTTT.
* Simple define a switch to be used, then tie the on/off state of the switch to a specific Hello, Home phrases.
* Connect the switch to an IFTTT action, and the Hello, Home phrase will fire with the switch state change.
*
*
*/
definition(
    name: "Alexa Home Phrase Executor",
    namespace: "rsarkar",
    author: "Ron S",
    description: "Ties a routine to a momentary button tile to be used by Alexa.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "getPref")
}

def getPref() { 
    dynamicPage(name: "getPref", install:true, uninstall: true) {
        section("Choose a Momentary Button to use...") {
            input "momentarySwitch", "capability.momentary", title: "Switch", multiple: false, required: true
        }
        def routines = location.helloHome?.getPhrases()*.label
        if (routines) {
            routines.sort()
            section("Perform which routine when button is pushed...") {
                input "routine_on", "enum", title: "Switch is pushed", options: routines, required: false
            }	
        }

        section([mobileOnly:true], "Options") {
            label(title: "Assign a name", required: false)
            mode title: "Set for specific mode(s)", required: false
            href "pageAbout", title: "About ${textAppName()}", description: "Tap to get application version, license and instructions"
        }
	}
}

page(name: "pageAbout", title: "About ${textAppName()}") {
    section {
    	paragraph "${textVersion()}\n${textCopyright()}\n\n${textLicense()}\n"
    }
    section("Instructions") {
    	paragraph textHelp()
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    subscribe(momentarySwitch, "switch", "pushHandler")
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(momentarySwitch, "switch", "pushHandler")
}

def pushHandler(evt) {
    if (evt.value == "on" && routine_on) {
        if (routine_on){
        	location.helloHome.execute(settings.routine_on)
        }
    } 
}

private def textAppName() {
	def text = "Alexa Home Phrase Executor"
}

private def textVersion() {
	def text = "Version 1.0.0 (09/01/2015)"
}

private def textCopyright() {
	def text = "Copyright © 2015 Ron S"
}

private def textLicense() {
    def text =
        "Licensed under the Apache License, Version 2.0 (the 'License'); "+
        "you may not use this file except in compliance with the License. "+
        "You may obtain a copy of the License at"+
        "\n\n"+
        " http://www.apache.org/licenses/LICENSE-2.0"+
        "\n\n"+
        "Unless required by applicable law or agreed to in writing, software "+
        "distributed under the License is distributed on an 'AS IS' BASIS, "+
        "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
        "See the License for the specific language governing permissions and "+
        "limitations under the License."
}

private def textHelp() {
    def text =
        "Create a momentary button tile from IDE. Expose the switch to Alexa. Tie a routine through this app. " +
        "Use command like Alexa, Turn on Night Mode (where Night Mode is the group name which has your device. "
}