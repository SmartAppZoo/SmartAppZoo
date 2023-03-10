//import grails.converters.JSON

/**
*  http://docs.smartthings.com/en/latest/smartapp-web-services-developers-guide/tutorial-part1.html
*/

definition(
  name: "Device Endpoints",
  namespace: "Operations",
  category: "Convenience",
  author: "justinlhudson",
  description: "Exposed Devices",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
  oauth: [displayName: "Device Endpoints", displayLink: ""]
)

preferences {
  section("Exposed Devices...") {
    input "switches", "capability.switch", title: "Switches", multiple: true, required: false
  }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches" //switches
    ]
  }
  path("/switches/:command") {
    action: [
      PUT: "updateSwitches" // /switches/on, /switches/off
    ]
  }
}

def installed() {}

def updated() {}

def listSwitches() {
    def resp = []
    switches.each {
      resp << [name: it.displayName, value: it.currentValue("switch")]
    }
    return resp
}

void updateSwitches() {
  // Get the JSON body from the request.
  // Safe de-reference using the "?." operator
  // to avoid NullPointerException if no JSON is passed.

  //def command = request.JSON?.command

    // use the built-in request object to get the command parameter
    def command = params.command

      log.debug("updateSwitches: "+ command)

    if (command) {
        // check that the switch supports the specified command
        // If not, return an error using httpError, providing a HTTP status code.
        switches.each {
            if (!it.hasCommand(command)) {
                httpError(501, "$command is not a valid command for all switches specified")
            }
        }

        // all switches have the command
        // execute the command on all switches
        // (note we can do this on the array - the command will be invoked on every element
        switches."$command"()
    }
}