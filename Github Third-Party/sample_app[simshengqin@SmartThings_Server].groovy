definition(
    name: "SmartApp_Heroku",
    namespace: "simshengqin",
    author: "Sim Sheng Qin",
    description: "A SmartApp to test connection with Heroku server",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Add which new event?") {
        input "event", "text", required: false, title: "Event name?"
    }
    section("Test which action is allowed?") {
        input "action", "text", required: false, title: "Action name?"
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
    log.debug event == null  ?"No event is indicated" : "Status of adding " + event + ": " + newEvent(event)
	log.debug action == null ?"No action is indicated" : "Is " + action + " allowed?: " + isAllowed(action)
}

def isAllowed(action_name) {
    def params = [
        uri: "https://smartthings-security.herokuapp.com/requests.php/",
        contentType: 'application/json',
        query: [action: action_name, mode: 'json']
    ]
    try {
        httpGet(params) { resp ->
            // iterate all the headers
            // each header has a name and a value
            resp.headers.each {
               //log.debug "${it.name} : ${it.value}"
            }

            // get an array of all headers with the specified key
            //def theHeaders = resp.getHeaders("Content-Length")

            // get the contentType of the response
            //log.debug "response contentType: ${resp.contentType}"

            // get the status code of the response
            //log.debug "response status code: ${resp.status}"

            // get the data from the response body
            //log.debug "response data: ${resp.data}"
            
            // get specific data from the JSON array
            //log.debug "requested action: ${resp.data.action}"
            //log.debug "Is allowed?: ${resp.data.is_allowed}"
            return resp.data.is_allowed
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
	
}

def newEvent(event) {
    def params = [
        uri: "https://smartthings-security.herokuapp.com/requests.php/",
        contentType: 'application/json',
        query: [newEvent: event, mode: 'json']
    ]
    try {
        httpGet(params) { resp ->
            // iterate all the headers
            // each header has a name and a value
            resp.headers.each {
               //log.debug "${it.name} : ${it.value}"
            }

            // get an array of all headers with the specified key
            //def theHeaders = resp.getHeaders("Content-Length")

            // get the contentType of the response
            //log.debug "response contentType: ${resp.contentType}"

            // get the status code of the response
            //log.debug "response status code: ${resp.status}"

            // get the data from the response body
            //log.debug "response data: ${resp.data}"
            
            // get specific data from the JSON array
            //log.debug "requested action: ${resp.data.action}"
            //log.debug "Is allowed?: ${resp.data.is_allowed}"
            return resp.data.status
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
	
}
