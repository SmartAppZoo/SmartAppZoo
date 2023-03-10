/**
 *  Web Event Logger
 *
 *  Copyright 2018 Andrew Baur
 *
 */
definition(
    name: "Web Event Logger",
    namespace: "baurandr",
    author: "Andrew Baur",
    description: "Log events",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Select contacts...") {
		input("contact1", "capability.contactSensor", title: "Which contact sensor(s)?", multiple: true, required: false)
    }
	section("Select motion sensors..."){
		input "motion1", "capability.motionSensor", title: "Which motion sensor(s)?", multiple: true, required: false
	}
	section("Select buttons..."){
		input "button1", "capability.button", title: "Which button(s)?", multiple: true, required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
	runEvery5Minutes(eventLogging)
    def currentLog = new Date(now())
    state.previousLog = currentLog
}

include 'asynchttp_v1'

def eventLogging(){
	log.debug "<<<<<<<<<<<<<<<<<<<< Event Logging >>>>>>>>>>>>>>>>>>>>"
    //def limitedEvents = []
	def queue = []
	def nowLock = now()
    
    def currentLog = new Date(nowLock)
    def currentLogOffset = new Date(nowLock+1000) //statesBetween appears to be INCLUSIVE so we need to offset on one side
    
    def previousLog = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.previousLog);
    
    log.debug "previousLog: ${previousLog}, currentLog: ${currentLog}"

    contact1.each {
        it.statesBetween("contact", previousLog, currentLog, [max: 300]).each{
            queue << processEvent(it)
        }
    }
    motion1.each {
        it.statesBetween("motion",previousLog, currentLog, [max: 300]).each{
            queue << processEvent(it)
        }
    }
    button1.each {
        it.statesBetween("button",previousLog, currentLog, [max: 300]).each{
            queue << processEvent(it)
        }
    }

	def url = "http://www.baurfam.com/addEvent.php"
	
	if (queue != []) {
    	queue.sort{it.eventDateTime}
		log.debug "Events to be sent to baurfam.com: ${queue}"
        try {
            asynchttp_v1.put(processResponse, [uri: url, body: queue])
            state.previousLog = currentLogOffset
        }
        catch (e) {
            log.debug "something went wrong: $e"
        }
        catch (groovyx.net.http.ResponseParseException e) {
            // ignore error 200, bogus exception
            if (e.statusCode != 200) {
                log.error "Baurfam: ${e}"
            } else {
                log.debug "Baurfam accepted event(s)"
            }                  
        }
        
/* OLD JSON LOGIC
		try {
			httpPutJson([uri: url, body: queue]) {response ->
				if (response.status != 200) {
					log.debug "Baurfam logging failed, status = ${response.status}"
                    state.queue = queue
				} else {
					log.debug "Baurfam accepted event(s)"
                    //log.debug "Response: ${response.data}"
                    state.queue = []
				}
			}
		} catch (groovyx.net.http.ResponseParseException e) {
			// ignore error 200, bogus exception
			if (e.statusCode != 200) {
				log.error "Baurfam: ${e}"
			} else {
				log.debug "Baurfam accepted event(s)"
			}                   
			state.queue = []                      
		} catch (e) {
			def errorInfo = "Error sending value: ${e}"
			log.error errorInfo
			state.queue = []                   
		}
        */
	} else {
    	state.previousLog = currentLogOffset
        log.debug "Queue Empty"
    }
}

def processResponse(response, data) {
//log.debug "Process Async response"
    try {
        if (response.status != 200) {
            log.debug "Logging failed, status = ${response.status}"
            log.debug "raw response: ${response.errorData}"
            def headers = response.headers
            headers.each {header, value ->
                log.debug "$header: $value"
            }
        } else {
            log.debug "Baurfam Accepted event(s)"
            //log.debug "Response: ${response.data}"
        }
    }
    catch (e) {
        log.debug "something went wrong: $e"
    }
    catch (groovyx.net.http.ResponseParseException e) {
        // ignore error 200, bogus exception
        if (e.statusCode != 200) {
            log.error "Baurfam: ${e}"
        } else {
            log.debug "Baurfam accepted event(s)"
            //log.debug "Response: ${response.data}"
        }
    }
}

def processEvent(evt){
    def eventDate = evt.date
    String eventDateString = eventDate.format("yyyy-MM-dd HH:mm:ss", location.timeZone)
    String eventNameStr = evt.name
    String eventValueStr = evt.value
    String eventDeviceStr = evt.device
    
    return [eventType: eventNameStr, eventValue: eventValueStr, eventDateTime: eventDateString, eventDeviceName: eventDeviceStr]
}

