definition(
    name: "Door Sensor http",
    namespace: "SmartUY",
    author: "SmartUY",
    description: "Door/Window monitor",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Select devices to monitor") {
		input "sensors", "capability.contactSensor", multiple:true, required: true
	}
}

mappings {
    path("/event") {
        action: [
            POST: "handle_event"
        ]
    }
}

def handle_event() {
    def event = request.JSON
    log.debug event
    
    def sensor_id = event.sensor_id 
    def device = null
    sensors.each {
      if(sensor_id == it.id){
        device = it;
        log.trace it.name
      }
    }
    
    if(device == null)
      httpError(501, "Device not found")
      
    state.last_seen = now()
    
    if(event.state == "closed"){
        device.close();
    } 
    else if(event.state == "open"){
        device.open();
    }
    else
      httpError(500, "Unknown device state")
      

    log.trace "Device update successful"
    
    return [ "success": true ]
}