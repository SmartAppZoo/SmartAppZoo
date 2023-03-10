definition(
    name: "Tell Them Closed",
    namespace: "drandyhaas",
    author: "Andy Haas",
    description: "Tell some contacts they are closed",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_contact-outlet@2x.png"
)

preferences {
	section("These contacts..."){
		input "contacts", "capability.contactSensor", title: "Which contact sensors?", multiple:true
	}
}

def installed()
{
    //subscribe(contacts, "closed", handler)
    subscribe(app, appTouch)
    log.debug "installed"
}

def updated()
{
	unsubscribe()
	installed()
}

def appTouch(evt){
    log.debug "appTouch $evt "
    for (con in contacts){
       log.debug "con: $con "
       //sendEvent(con, [name: "contact", value: "closed"])
       con.closed()
    }
}
def handler(evt) {
    log.debug "handler $evt "

}

/*
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

*/