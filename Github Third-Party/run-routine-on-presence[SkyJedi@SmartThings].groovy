/**
 *  Mode Change on Presence
 *
 *  Author: skyjedi@gmail.com
 *  Date: 11/2/16
 */

definition(
  name: "Run Routine on Presence",
  namespace: "SkyJedi",
  author: "skyjedi@gmail.com",
  description: "When everyone leaves, change mode.",
  category: "My Apps",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
    page(name: "configure")
}
def configure() {
    dynamicPage(name: "configure", title: "Configure Presence and Phrase", install: true, uninstall: true) {
  
  section("When all of these people leave home") {
    input "people", "capability.presenceSensor", multiple: true
  		}

	def actions = location.helloHome?.getPhrases()*.label
     if (actions) {
            actions.sort()
                    section("Hello Home Actions") {
                            log.trace actions
                	input "awayAction", "enum", title: "Action to execute when everyone is away", options: actions, required: true
                	input "arrivalAction", "enum", title: "Action to execute when someone arrives", options: actions, required: true
                    }
                    }

  section("Away threshold (defaults to 10 min)") {
    input "awayThreshold", "decimal", title: "Number of minutes", required: false
  		}
  section("Change Name of App (optional)") {
  label title: "Assign a name", required: false
        }
  	}
}

def installed() {
  init()
}

def updated() {
  unsubscribe()
  init()
}

def init() {
  subscribe(people,"presence",allAway)
  subscribe(people,"presence",arrivals)
  
}

def allAway(evt) {
  if(evt.value == "not present") {
    log.debug("Checking if everyone is away")

    if(everyoneIsAway()) {
      log.info("Starting ${awayAction} sequence")
      def delay = (awayThreshold != null && awayThreshold != "") ? awayThreshold * 60 : 10 * 60
      runIn(delay, "setAway")
    }
  }

    else {
      log.debug("Someone is home, no routine run")
    }
}

def arrivals(evt) {
  if(evt.value == "present") {
  def message = "${app.label} ran '${arrivalAction}' routine because someone arrived"
      log.info(message)
      location.helloHome?.execute(settings.arrivalAction)
  }
}
  
def setAway() {
  if(everyoneIsAway()) {
      def message = "${app.label} ran '${awayAction}' routine because everyone left home"
      log.info(message)
      location.helloHome?.execute(settings.awayAction)
  }

  else {
    log.info("Somebody returned home before we ran '${awayAction}'")
  }
}

private everyoneIsAway() {
  def result = true

  if(people.findAll { it?.currentPresence == "present" }) {
    result = false
  }

  log.debug("everyoneIsAway: ${result}")

  return result
}
