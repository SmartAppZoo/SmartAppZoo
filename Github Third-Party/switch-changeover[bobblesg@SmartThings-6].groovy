 
/**
 * ****************  Switch Changeover  ****************
 *
 *  Design Usage:
 *	This was designed to switch 'Virtual' season switches (Spring, Summer, Autumn, Winter)
 *
 *
 *  Copyright 2017 Andrew Parker
 *  
 *  This SmartApp is free!
 *  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://www.paypal.me/smartcobra
 *  
 *
 *  I'm very happy for you to use this app without a donation, but if you find it useful then it would be nice to get a 'shout out' on the forum! -  @Cobra
 *  Have an idea to make this app better?  - Please let me know :)
 *
 *  Website: http://securendpoint.com/smartthings
 *
 *-------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @Cobra
 *
 *-------------------------------------------------------------------------------------------------------------------
 *
 *  Last Update:
 *
 *  Changes:
 *
 * 
 *
 *  V1.3.0 - Added more switch slots - now 6
 *  V1.2.1 - Debug
 *  V1.2.0 - Added more switch slots (Originally just Summer & Winter)
 *  V1.0.0 - POC - Summer & Winter switches
 */
 
 
 
 
definition(
    name: "Switch Changeover",
    namespace: "Cobra",
    author: "Andrew Parker",
    description: "If you turn on a switch - All others turn off. \r\n Two switches cannot be on at the same time.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX2Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    iconX3Url: "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
    )


preferences {

section ("") {
 paragraph " V1.3.0 "
  paragraph image:  "https://raw.githubusercontent.com/cobravmax/SmartThings/master/icons/cobra3.png",
       	title: "Switch Changeover",
        required: false, 
    	 "If you turn on a switch - All others turn off. \r\n Two switches cannot be on at the same time."
 
 }

	 section("Switches"){
		input "switch1",  "capability.switch", multiple: false, required: false
		input "switch2",  "capability.switch", multiple: false, required: false
        input "switch3",  "capability.switch", multiple: false, required: false
        input "switch4",  "capability.switch", multiple: false, required: false
        input "switch5",  "capability.switch", multiple: false, required: false
        input "switch6",  "capability.switch", multiple: false, required: false
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
	subscribe(switch1, "switch", switchHandler1)
    subscribe(switch2, "switch", switchHandler2)
    subscribe(switch3, "switch", switchHandler3)
    subscribe(switch4, "switch", switchHandler4)
    subscribe(switch5, "switch", switchHandler5)
    subscribe(switch6, "switch", switchHandler6)
}



def switchHandler1 (evt) {
state.currS1 = evt.value 
if (state.currS1 == "on") { 
log.info "Turning on $switch1"

if (switch2 != null){ switch2.off()}
if (switch3 != null){ switch3.off()}
if (switch4 != null){ switch4.off()}
if (switch5 != null){ switch5.off()}
if (switch6 != null){ switch6.off()}
}
}

def switchHandler2 (evt) {
state.currS2 = evt.value 
if (state.currS2 == "on") { 
log.info "Turning on $switch2"
if (switch1 != null){ switch1.off()}
if (switch3 != null){ switch3.off()}
if (switch4 != null){ switch4.off()}
if (switch5 != null){ switch5.off()}
if (switch6 != null){ switch6.off()}
}
}

def switchHandler3 (evt) {
state.currS3 = evt.value 
if (state.currS3 == "on") { 
log.info "Turning on $switch3"
if (switch2 != null){ switch2.off()}
if (switch1 != null){ switch1.off()}
if (switch4 != null){ switch4.off()}
if (switch5 != null){ switch5.off()}
if (switch6 != null){ switch6.off()}
}
}
def switchHandler4 (evt) {
state.currS4 = evt.value 
if (state.currS4 == "on") { 
log.info "Turning on $switch4"
if (switch2 != null){ switch2.off()}
if (switch3 != null){ switch3.off()}
if (switch1 != null){ switch1.off()}
if (switch5 != null){ switch5.off()}
if (switch6 != null){ switch6.off()}
}
}
def switchHandler5 (evt) {
state.currS5 = evt.value 
if (state.currS5 == "on") { 
log.info "Turning on $switch5"
if (switch1 != null){ switch1.off()}
if (switch2 != null){ switch2.off()}
if (switch3 != null){ switch3.off()}
if (switch4 != null){ switch4.off()}
if (switch6 != null){ switch6.off()}
}
}
def switchHandler6 (evt) {
state.currS6 = evt.value 
if (state.currS6 == "on") { 
log.info "Turning on $switch6"
if (switch1 != null){ switch1.off()}
if (switch2 != null){ switch2.off()}
if (switch3 != null){ switch3.off()}
if (switch4 != null){ switch4.off()}
if (switch5 != null){ switch5.off()}

}
}

def setAppVersion(){
    state.appversion = "1.3.0"
}