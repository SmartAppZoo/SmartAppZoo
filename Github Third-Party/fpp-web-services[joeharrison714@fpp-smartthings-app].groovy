/**
 *  FPP Web Services
 *
 */
 
 import groovy.json.JsonBuilder
 
definition(
    name: "FPP Web Services",
    namespace: "joeharrison714",
    author: "Joe Harrison",
    description: "FPP web services",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "FPP Web Services", displayLink: "http://fpp"])


preferences {
  section ("Allow FPP to control these things...") {
    input "switches", "capability.switch", multiple: true, required: true
  }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }
  path("/switches/:command/:sname") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/locations") 							{	action: [	GET: "listLocation"        														]}
  path("/routines") 							{   action: [   GET: "listRoutines"        														]}
  path("/routines/:id") 						{   action: [   GET: "listRoutines",            	POST: "executeRoutine"        				]}
}

/****************************
* Routine API Commands
****************************/

/**
* Gets Routines for location, if params.id is provided, get details for that Routine
*
* @param params.id is the routine id
* @return renders json
*/
def listRoutines() {
	debug("listRoutines called")
    def id = params?.id
    def results = []
    // if there is an id parameter, list only that routine. Otherwise list all routines in location
    if(id) {
        def routine = location.helloHome?.getPhrases().find{it.id == id}
        def myRoutine = [:]
        if(!routine) {
            httpError(404, "Routine not found")
        } else {
            render contentType: "text/json", data: new JsonBuilder(getRoutine(routine)).toPrettyString()            
        }
    } else {
        location.helloHome?.getPhrases().each { routine ->
            results << getRoutine(routine)
        }
        debug("Returning ROUTINES: $results")
        render contentType: "text/json", data: new JsonBuilder(results).toPrettyString()
    }
}

/**
* Executes Routine for location
*
* @param params.id is the routine id
* @return renders json
*/
def executeRoutine() {
	debug("executeRoutine called")
    def id = params?.id
    def routine = location.helloHome?.getPhrases().find{it.id == id}
    if(!routine) {
        httpError(404, "Routine not found")
    } else {
        debug("Executing Routine: $routine.label in location: $location.name")
        location.helloHome?.execute(routine.label)
        render contentType: "text/json", data: new JsonBuilder(routine).toPrettyString()
    }
}

/****************************
* Location Methods
****************************/

/**
* Gets the location object
*
* @return renders json
*/
def listLocation() {
	debug("listLocation called")
    def result = [:]
    ["contactBookEnabled", "name", "temperatureScale", "zipCode"].each {
        result << [(it) : location."$it"]
    }
    result << ["latitude" : location.latitude as String]
    result << ["longitude" : location.longitude as String]
    result << ["timeZone" : location.timeZone?.getDisplayName()]
    result << ["currentMode" : getMode(location.currentMode)]

    // add hubs for this location to the result
    def hubs = []
    location.hubs?.each {
        hubs << getHub(it)
    }
    result << ["hubs" : hubs]
    debug("Returning LOCATION: $result")
    //result
    render contentType: "text/json", data: new JsonBuilder(result).toPrettyString()
}

// returns a list like
// [[name: "kitchen lamp", value: "off"], [name: "bathroom", value: "on"]]
def listSwitches() {

    def resp = []
    switches.each {
        resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}

void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command
    def sname = params.sname

    log.info "Issued command $command for switch $sname"

    def theSwitch = switches.find { it.displayName == "${sname}" }
    log.debug "Selected switch ${theSwitch}"
    def switchCurrent = theSwitch.currentValue("switch")
    log.debug "Current value: ${switchCurrent}"

    switch(command) {
       	case "on":
			theSwitch.on()
	        break
	    case "off":
	        theSwitch.off()
	        break
	    case "toggle":
	     	if(switchCurrent == "on")
	            {theSwitch.off()}
	        else
	            {theSwitch.on()}
    	    break
	    default:
	        httpError(400, "$command is not a valid command for the specified switch")
	}

}
def installed() {}

def updated() {}

/****************************
* Private Methods
****************************/

/**
* Builds a map of hub details
*
* @param hub id (optional), explodedView to show details
* @return a map of hub
*/
private getHub(hub, explodedView = false) {
	debug("getHub called")
    def result = [:]
    //put the id and name into the result
    ["id", "name"].each {
        result << [(it) : hub."$it"]
    }

    // if we want detailed information about this hub
    if(explodedView) {
        ["firmwareVersionString", "localIP", "localSrvPortTCP", "zigbeeEui", "zigbeeId"].each {
            result << [(it) : hub."$it"]
        }
        result << ["type" : hub.type as String]
    }
    debug("Returning HUB: $result")
    result
}

/**
* Gets the hub detail
*
* @param params.id is the hub id
* @return renders json
*/
def getHubDetail() {
	debug("getHubDetail called")
    def id = params?.id
    debug("getting hub detail for id: " + id)
    if(id) {
        def hub = location.hubs?.find{it.id == id}
        def result = [:]
        //put the id and name into the result
        ["id", "name"].each {
            result << [(it) : hub."$it"]
        }
        ["firmwareVersionString", "localIP", "localSrvPortTCP", "zigbeeEui", "zigbeeId", "type"].each {
            result << [(it) : hub."$it"]
        }
        result << ["type" : hub.type as String]

        debug("Returning HUB: $result")
        render contentType: "text/json", data: new JsonBuilder(result).toPrettyString()
    }
}

/**
* gets mode information
*
* @param mode object
* @return a map of mode information
*/
private getMode(mode, explodedView = false) {
	debug("getMode called")
    def result = [:]
    ["id", "name"].each {
        result << [(it) : mode."$it"]
    }

    if(explodedView) {
        ["locationId"].each {
            result << [(it) : mode."$it"]
        }
    }
    result
}

/**
* gets Routine information
*
* @param routine object
* @return a map of routine information
*/
private getRoutine(routine) {
	debug("getRoutine called")
    def result = [:]
    ["id", "label"].each {
        result << [(it) : routine."$it"]
    }
    result
}

//Debug Router to log events if logging is turned on
def debug(evt) {
	if (logging) {
    	log.debug evt
    }
}