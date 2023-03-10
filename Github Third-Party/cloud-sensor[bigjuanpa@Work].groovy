/**
 *  SmartUY Cloud Sensor App
 *
 */

definition(
  name: "Cloud Sensor",
  namespace: "SmartUY",
  author: "SmartUY",
  description: "SmartUY - A webhook handler for internet connected contact or motion sensors",
  category: "Safety & Security",
  iconUrl: "https://smartuy.com/SmartUY-Logo/SmartUY_55x80.png",
  iconX2Url: "https://smartuy.com/SmartUY-Logo/SmartUY_110x160.png"
)

preferences {
	section("Select devices to monitor") {
  	input "contactSensors", "capability.contactSensor", title: "Contact sensors", multiple:true, required:false
    input "motionSensors", "capability.motionSensor", title: "Motion sensors", multiple:true, required:false
    input "smokeDetectors", "capability.smokeDetector", title: "Smoke detectors", multiple:true, required:false
    input "alarms", "capability.alarm", title: "Alarms", multiple: true, required:false
  }
}

mappings {
  path("/event") {
    action: [
      POST: "handle_event"
    ]
  }

  path("/sync") {
    action: [
      POST: "sync"
    ]
  }
}

def handle_event() {
  def event = request.JSON
  def allSensors = (contactSensors?:[]) + (motionSensors?:[]) + (smokeDetectors?:[]) - null
  def device = allSensors.find { 
    event.sensor_id == it.id
  }
  
  if (device == null) {
    httpError(501, "Unknown device " + event.sensor_id)
  }
  
  switch (event.state) {
    case 0: device.close(); break;
    case 1: device.open(); break;
    default: httpError(500, "Unknown device state " + event.state);
  }

  log.debug "Updated " + device + " to " + event.state

  return [ "success": true ]
}

def sync() {
  def sync_data = request.JSON
  def alarm = alarms.find {
    sync_data.device_id == it.id
  }
  if (alarm) {
    alarm.sync(sync_data.ip, sync_data.port as String, sync_data.mac)
  }
  return [ "success": true ]
}