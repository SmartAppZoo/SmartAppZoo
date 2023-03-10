definition(
    name: "TEST - Momentary To Toggle Lights",
    namespace: "philippegravel",
    author: "philippegravel",
    description: "Toggle a light(s) using a virtual momentary switch. With Log Child App",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Select momentary switch to monitor"){
		input "theSwitch", "capability.switch", multiple: false, required: true
	}
    section("Toggle these lights..."){
		input "switches1", "capability.switch", multiple: true, required: false
	}
    
    section {
        app(name: "logApps", appName: "Logs App", namespace: "philippegravel", title: "Logs", multiple: false)
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


def toggleHandler(evt) {

	theLog.AddMessage($app.label, "Toggle switch [$evt.value]", 1)
//	log.info evt.value
    def cnt = 0
    def curSwitch = switches1.currentValue("switch")
//    log.info curSwitch
    
    for(switchVal in curSwitch) {
    
    	if(curSwitch[cnt] == "on") {
    
    		switches1[cnt].off()
    
    	}     
    	if(curSwitch[cnt] == "off") {
    
    		switches1[cnt].on()
    
    	}
   		cnt++
    }
    
    theLog.closeMessage()
}

def initialize() {
	subscribe(theSwitch, "momentary.pushed", toggleHandler)
    
    def theLog = findChildAppByName("Logs App")
}