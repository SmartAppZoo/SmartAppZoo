/*
 *  Motion detected and everybody is gone
 *
 *  Author: casey@softwareforgood.com
 *  Date: 2013-03-22
 *  Version: 0.0.2
 */

def preferences() {[
  sections: [
    [
      title: "When motion is detected...",
      input: [
        [
          name: "motion",
          title: "Where?",
          type: "capability.motionSensor",
          description: "Tap to set",
          multiple: false
        ]
      ]
    ],
    [
      title: "And everybody is away...",
      input: [
        [
          name: "presences",
          title: "Which sensor(s)?",
          type: "capability.presenceSensor",
          description: "Tap to set",
          multiple: true
        ]
      ]
    ]
  ]
]}


def installed() {
  log.trace "Installed with settings: ${settings}"
  subscribe(motion.motion)
  // handle the list?
  presences.each { sensor ->
    subscribe(sensor.presence)
  }
}

def updated() {
  log.trace "Updated with settings: ${settings}"
  unsubscribe()
  subscribe(motion.motion)
  presences.each { sensor ->
    subscribe(sensor.presence)
  }
}


def shouldNotify(sensor) {
  log.trace "should notify? ... "
  def recentEvents = sensor.eventsSince(new Date(now() - 1000))
  def alreadySent = recentEvents.count { it.value && it.value == "active" } > 1
  !alreadySent
}

def present(sensor){
  sensor.latestValue == 'present'
}
def notPresent(sensor){
  sensor.latestValue == 'not present'
}

def allNotPresent(sensors){
  log.info("All Not Present: ${sensors}")
  sensors.every { notPresent(it) } 
}

def motion(evt) {
  log.trace "${motion.name} evt.value: ${evt.value}"

  if (evt.value == 'active'){
    log.trace("Motion detected")
    
    if (shouldNotify(motion) && allNotPresent(presences)) {
      log.info " *** ${ presences.collect{ it.name }.join(', ') } not pesent! *** "
      notifyMe "Motion detected by ${motion.label ?: motion.name} and nobody is home! ${new Date(now())}" 
    }
  }
}

def notifyMe(message){
  log.info "Notify Me: ${message}"
  sendPush(message)
  //sendSms(phoneNumber, message)
  sendSms('6122076622', message)
  //sendSms('6127309391', message)
}


def presence(evt) {
  log.trace "Presence evt.value: ${evt.value}"
  //noop  
}




