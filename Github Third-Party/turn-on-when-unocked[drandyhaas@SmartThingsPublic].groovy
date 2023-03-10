/**
 *  Turn It On When It Opens
 *
 *  Author: SmartThings
 */
definition(
    name: "Turn On When Unocked",
    namespace: "drandyhaas",
    author: "Andy Haas",
    description: "Turn something on or off when a door is unlocked",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("When a door opens..."){
		input "locks", "capability.lock", title: "Which locks?", multiple:true
	}
    section("With this code index..."){
		input "lockindex", "number"
	}
	section("Turn on switches (and then turn off in 900s) ..."){
		input "switcheson", "capability.switch", multiple: true, required: false
	}
    section("Turn off switches..."){
		input "switchesoff", "capability.switch", multiple: true, required: false
	}
}

def installed()
{
	subscribe(locks, "lock", contactOpenHandler)
    subscribe(app, appTouch)
    log.debug "subscribed to lock for $lockindex on locks $locks "
}

def updated()
{
	unsubscribe()
	installed()
}

import groovy.json.JsonSlurper
def appTouch(evt){//test things out...
	def str = '{"usedCode":3,"microDeviceTile":{"type":"standard","icon":"st.locks.lock.unlocked","backgroundColor":"#ffffff"}}'
	log.debug "str: $str "
    def results = new groovy.json.JsonSlurper().parseText(str)
    log.debug "results: $results "
    def usedcode = results.usedCode as Integer
    log.debug "used code $usedcode and lockindex $lockindex "
    if (usedcode == lockindex || lockindex<=0){
    	log.debug "matched!"
    }
}
def contactOpenHandler(evt) {//for real...
    def lat = locks.latestValue("lock")
    def val = evt.value
    log.debug "value: $val, settings: $settings, latest: $lat"
    if (val=="unlocked" && evt.data){
    
       def str = evt.data
	   log.debug "str: $str "
       def results = new groovy.json.JsonSlurper().parseText(str)
       log.debug "results: $results "
       def usedcode = results.usedCode as Integer
       log.debug "used code $usedcode and lockindex $lockindex "
       if (usedcode == lockindex || lockindex<=0){

	     log.trace "Turning on switches: $switcheson"
         switcheson.on()
         log.trace "Turning off switches: $switchesoff"
         switchesoff.off()
         runIn(900, "turnEmOff") //900 sec is 15 min
         sendPush "Unlocked with code $usedcode and disarming!"
         sendLocationEvent(name: "alarmSystemStatus", value: "off")
         setLocationMode("Home")
       }
    }
}
def turnEmOff() {
	log.debug "turnEmOff: $switcheson"
    sendPush "turnEmOff: $switcheson"
	switcheson.off()
}
