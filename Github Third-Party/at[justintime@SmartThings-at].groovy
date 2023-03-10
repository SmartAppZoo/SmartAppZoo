/**
* at for SmartThings
*
*
*/
definition(
 name: "At Job Scheduler",
    namespace: "justintime",
    author: "Justin Ellison",
    description: "Schedules one-time jobs akin to 'at' on Linux",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {

  section("Job Definition") {
    input "switches", "capability.switch", 
      multiple: true, 
      title: "Operate on these switches:", 
      required: true

    input "operation", "enum",
      title: "Turn them on or off?",
      options: ["On","Off"]

    input "schedule", "time",
      title: "When should the job run?",
      required: true
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"

  unschedule("handler")
  initialize()
}

def initialize() {
  log.debug "Scheduling turning ${operation} switches"
  runOnce(schedule, handler)
}

def handler() {
  log.debug "Turning ${operation} switches"
  switch(operation) {
    case 'On':
      switches.on()
      break
    case 'Off':
      switches.off()
      break
  }
}