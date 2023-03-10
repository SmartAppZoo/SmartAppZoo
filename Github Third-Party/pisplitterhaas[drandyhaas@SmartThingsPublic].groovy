definition(
    name: "PiSplitterHaas",
    namespace: "drandyhaas",
    author: "Andrew Haas",
    description: "Get info from pi and send it out to esp8266's",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
	section("Choose switch to monitor for raspberry pi info... ") {
		input "myswitch",  "capability.switch"
	}
	section("Delay") {
		input "myothers", "capability.switch", multiple: true
	}
}

def installed(){
    log.debug("installed")
    subscribe(myswitch, "esp8266haas", handler)
}

def updated(){
    unsubscribe()
    installed()
}

import groovy.json.JsonSlurper
def handler(evt){
	log.debug "handler: $evt.name, $evt.value, $settings "
    def slurper = new JsonSlurper()
    def result = slurper.parseText(evt.value)
    log.debug "result: ${result}"
    if (result.containsKey("value")) {
    	def val = result["value"]
    	log.debug "got value $val "
        def esp1 = myothers[0]
        def esp1name = esp1.name
        log.debug "$esp1 $esp1name "
        if (esp1name == "ESP8266 test1"){
        	log.debug "sending to $esp1 "
            esp1.get("temperature", val)
        }
    }
}