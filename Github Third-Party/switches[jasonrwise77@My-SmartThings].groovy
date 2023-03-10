/**
* Switches
*/
definition(
name: "Switches",
namespace: "Lightnjac",
author: "Jason Craig",
description: "Link one switch to another or one switch to many.",
parent: "Lightnjac:Switches link",
category: "Convenience",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
section("Main switch,...") {
input "mainswitch", "capability.switch", multiple: false
}
section("additional switches...") {
input "switches", "capability.switch", multiple: true
}
section("Take action when On, Off or Both:") {
input (name: "type", type: "enum", title: "Select when to link action, for:", required: true, multiple: false,
defaultValue: 'Off', options: ['On', 'Off', 'Both']) 
}
section("Control Direction:") {
input (name: "direction", type: "enum", title: "ontomany (main switch controls others)\nmanytoone (main switch is on if any others on)\nsynced (main is both controlled and controls)\nonetomanyoff main on turns only one switch on",
required: true, multiple: false,
defaultValue: 'true', options: ['onetomany', 'manytoone', 'linked', 'onetomanyoff'])
}
section("one on switch,...") {
input "oneonswitch", "capability.switch", multiple: false, required: false
}
}

def installed()
{
state.main = 0
state.switchesPosition = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
setup()
}

def updated()
{
unsubscribe()
setup()
}

def setup()
{
subscribe(mainswitch, "switch.off", mainswitchOff)
if(settings["type"] == "Off" || settings["type"] == "Both") {
if(settings["direction"] == "manytoone" || settings["direction"] == "linked") {
subscribe(switches, "switch.off", oneswitchOff)
}
}
subscribe(mainswitch, "switch.on", mainswitchOn)
if(settings["direction"] == "manytoone" || settings["direction"] == "linked") {
subscribe(switches, "switch.on", oneswitchOn)
}
}

def mainswitchOff(evt) {
log.debug "main switch: $evt.displayName $evt.value"
if((settings["type"] == "Off" || settings["type"] == "Both") && (settings["direction"] == "onetomany" || settings["direction"] == "linked" || settings["direction"] == "onetomanyoff") && state.main == 1) {
switches?.off()
}
state.main = 0
}

def mainswitchOn(evt) {
log.debug "main switch: $evt.displayName $evt.value"
if((settings["type"] == "On" || settings["type"] == "Both") && (settings["direction"] == "onetomany" || settings["direction"] == "linked") && state.main == 0) {
switches?.on()
}
if((settings["type"] == "On" || settings["type"] == "Both") && (settings["direction"] == "onetomanyoff") && state.main == 0) {
oneonswitch.on()
}
state.main = 1
}

def oneswitchOff(evt) {
log.debug "switch: $evt.displayName $evt.value"
def i = 0
def keepOn = 0
switches.each {
//log.debug "$it"
if("$it" == "$evt.displayName") {
//log.debug i
state.switchesPosition.putAt(i, 0)
}
if(state.switchesPosition.get(i) == 1) {
keepOn = 1
}
i++
}
if((settings["type"] == "Off" || settings["type"] == "Both") && state.main == 1 && keepOn == 0) {
state.main = 0
mainswitch.off()
}
}
def oneswitchOn(evt) {
log.debug "switch: $evt.displayName $evt.value"
def i = 0
switches.each {
//log.debug "$it"
if("$it" == "$evt.displayName") {
//log.debug i
state.switchesPosition.putAt(i, 1)
}
i++
}
if((settings["type"] == "On" || settings["type"] == "Both") && state.main == 0) {
state.main = 1
mainswitch.on()
}
}
