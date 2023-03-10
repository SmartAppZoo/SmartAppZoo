/**
 *  HTTP Presence Detector
 *    Increases your presence sensor by monitoring your ASUS router
 */
definition(
    name: "HTTP Presence Detector",
    namespace: "KristopherKubicki",
    author: "Kristopher Kubicki",
    description: "Monitors an HTTP page for a string, marking device present",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")



preferences {
	section("Router information") { 
		input("destIp", "text", title: "IP", description: "The device IP", required: true)
    	input("destPort", "number", title: "Port", description: "The port you wish to connect", required: true)
        input("path", "text", title: "Path", description: "HTTP path you wish to query (/update_clients.asp)", required: true)
    }
	section("Which presence sensor..."){
		input "presence", "capability.presenceSensor", multiple: false, required: true
	}
    section("String to detect (MAC Address, IP, etc)..."){
		input(name: "detectString", type: "text", title: "String", required: true)
	}
    
    section("Poll when this event is activated..."){
		input "switches", "capability.switch", multiple: true, required: false
		input "motions", "capability.motionSensor", multiple: true, required: false
		input "contacts", "capability.contactSensor", multiple: true, required: false
	}
}


def installed() {
   initialized()
}

def updated() {
	unsubscribe()
    initialized()
}

def initialized() {
    subscribe(switches, "switch",takeHandler)
    subscribe(motions, "motion",takeHandler)
    subscribe(contacts, "contact",takeHandler)
    subscribe(location, null, lanResponseHandler, [filterEvents:false])
}

def lanResponseHandler(evt) { 


    if(presence.currentValue("presence") != "present") { 
		def map = stringToMap(evt.description)
		def body = new String(map.body.decodeBase64())

    	if(body.contains(detectString)) { 
			log.debug "Marking present via HTTP presence detection"
            presence.present()
		}
    }
}

// handle commands
def takeHandler(evt) {

    
	// only run this is my device is not already present 
	if(presence.currentValue("presence") == "present") { 
    	return
    }

    def hosthex = convertIPtoHex(destIp)
    def porthex = convertPortToHex(destPort)

	// This router doesn't even seem to need authentication
    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'GET',
    		'path': path,
        	'body': '',
        	'headers': [ HOST: "$destIp:$destPort" ]
		) 
          

	sendHubCommand(hubAction)
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}
