/**
 *
 * handle blink motion switch
 *
 *  Author: Greg Frank
 */
definition(
  name: "Handle Blink Camera Motion via simulated switch",
  namespace: "greglarious",
  author: "greglarious",
  description: "When a switch is turned on (from motion), turn on another switch for some minutes and also trigger a motion simulated sensor",
  category: "Safety & Security",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion-cam.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/intruder_motion-cam.png@2x.png"
)

preferences {
  section("This switch turns on (indicating motion)..."){
    input "triggerSwitch", "capability.switch", title: "Switch" 
  }
  section("Turn on another switch..."){
    input "targetSwitch", "capability.switch", title: "Switch"
  }
  section ("And turn it back off after..."){
    input "offMinutes", "number", title: "Minutes"
  }
  section("And trigger motion sensor..."){
    input "targetSensor", "capability.motionSensor", title: "Simulated Motion sensor"
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  subscribe(switch1, "switch.on", motionTrigger)
}

def updated(settings) {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  subscribe(triggerSwitch, "switch.on", motionTrigger)
}

def motionTrigger(evt) {
  targetSwitch.on()
  targetSensor.active()
  
  def delaySeconds = 60 * offMinutes
  runIn(delaySeconds, resetMotion)
}

def resetMotion() {
  triggerSwitch.off()
  targetSwitch.off()
  targetSensor.inactive()
}