/**
 *  TurnOffKateTV
 *
 *  Copyright 2020 FLEET LARSEN
 *
 */
 
 

//include 'asynchttp_v1'
 
definition(
    name: "TurnOffKateTV",
    namespace: "fl",
    author: "FLEET LARSEN",
    description: "Turn off kates tv",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
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

//def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
//}

// TODO: implement event handlers

def initialize() {
	//curl -k -H "Content-Type: application/json" -H "AUTH: Zbamrgp7xg" -X GET https://192.168.86.50:7345/state/device/power_mode
    //getpowermode()
}

def getpowermode() {
    def params = [
        //content-Type: "application/json",
        AUTH: 'Zbamrgp7xg',
        uri: 'https://192.168.86.50:7345',
        path: '/state/device/power_mode'
    ]
    try {
    	log.debug  "here1"
        httpGet(params) { resp ->
            resp.headers.each {
            log.debug "${it.name} : ${it.value}"
            log.debug  "here2"
        }
        log.debug  "here3"
        log.debug "response contentType: ${resp.contentType}"
        log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

//def initialize() {
	//curl -k -H "Content-Type: application/json" -H "AUTH: Zbamrgp7xg" -X GET https://192.168.86.50:7345/state/device/power_mode
//    def params = [
//        AUTH: 'Zbamrgp7xg',
//        uri: 'https://192.168.86.50:7345/state/device/power_mode',
//        path: '',
//        body: [key1: 'value 1']
//    ]
//    asynchttp_v1.put(processResponse, params)
//}

//def processResponse(response, data) {
//	log.debug response
//}