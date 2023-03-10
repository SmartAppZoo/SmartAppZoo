/**
 *  smartthings.groovy
 *
 *  David Janes
 *  IOTDB.org
 *  2014-02-01
 *
 *  Allow control of your SmartThings via an API; 
 *  Allow monitoring of your SmartThings using
 *  MQTT through the IOTDB MQTT Bridge.
 *
 *  Follow us on Twitter:
 *  - @iotdb
 *  - @dpjanes
 *
 *  A work in progress!
 */

/* --- IOTDB section --- */

/*
 *  IOTDB MQTT Bridge
 *  - this works for now
 *  - your life is private
 *  - just set to empty values to turn off MQTT
 */
def iotdb_api_username = "nobody"
def iotdb_api_key = "4B453B43-F1FC-4327-85FA-4DF93E0F5B01"
    

/* --- setup section --- */
/*
 *  The user 
 *  Make sure that if you change anything related to this in the code
 *  that you update the 
// Automatically generated. Make future change here.
definition(
    name: "iotdb",
    namespace: "",
    author: "",
    description: "test",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


// Automatically generated. Make future change here.
definition(
    name: "iotdb",
    namespace: "",
    author: "",
    description: "test",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


// Automatically generated. Make future change here.
definition(
    name: "iotdb",
    namespace: "",
    author: "",
    description: "test",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


// Automatically generated. Make future change here.
definition(
    name: "iotdb",
    namespace: "",
    author: "",
    description: "test",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


// Automatically generated. Make future change here.
definition(
    name: "iotdb",
    namespace: "",
    author: "",
    description: "test",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)

preferences in your installed apps.
 *
 *  Note that there's a SmartThings magic that's _required_ here,
 *  in that you cannot access a device unless you have it listed
 *  in the preferences. Access to those devices is given through
 *  the name used here (i.e. d_*)
 */
preferences {
    section("Allow IOTDB to Control & Access These Things...") {
        input "d_switch", "capability.switch", title: "Switch", multiple: true
        input "d_motion", "capability.motionSensor", title: "Motion", required: false, multiple: true
        input "d_temperature", "capability.temperatureMeasurement", title: "Temperature", multiple: true
        input "d_contact", "capability.contactSensor", title: "Contact", required: false, multiple: true
        input "d_acceleration", "capability.accelerationSensor", title: "Acceleration", required: false, multiple: true
        input "d_presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
        input "d_battery", "capability.battery", title: "Battery", multiple: true
        input "d_threeAxis", "capability.threeAxis", title: "3 Axis", multiple: true
    }
}

/*
input "d_alarm", "capability.alarm", title: "alarm", multiple: true
input "d_configuration", "capability.configuration", title: "configuration", multiple: true
input "d_illuminanceMeasurement", "capability.illuminanceMeasurement", title: "illuminanceMeasurement", multiple: true
input "d_polling", "capability.polling", title: "polling", multiple: true
input "d_relativeHumidityMeasurement", "capability.relativeHumidityMeasurement", title: "relativeHumidityMeasurement", multiple: true
input "d_thermostatCoolingSetpoint", "capability.thermostatCoolingSetpoint", title: "thermostatCoolingSetpoint", multiple: true
input "d_thermostatFanMode", "capability.thermostatFanMode", title: "thermostatFanMode", multiple: true
input "d_thermostatHeatingSetpoint", "capability.thermostatHeatingSetpoint", title: "thermostatHeatingSetpoint", multiple: true
input "d_thermostatMode", "capability.thermostatMode", title: "thermostatMode", multiple: true
input "d_thermostatSetpoint", "capability.thermostatSetpoint", title: "thermostatSetpoint", multiple: true
input "d_threeAxisMeasurement", "capability.threeAxisMeasurement", title: "threeAxisMeasurement", multiple: true
input "d_waterSensor", "capability.waterSensor", title: "waterSensor", multiple: true
lqi: 100 %
acceleration: inactive
threeAxis: -38,55,1021
battery: 88 %
temperature: 65 F
*/

/*
 *  The API
 *  - ideally the command/update bit would actually
 *    be a PUT call on the ID to make this restful
 */
mappings {
    path("/:type") {
        action: [
            GET: "_api_list"
        ]
    }
    path("/:type/:id") {
        action: [
            GET: "_api_get",
            PUT: "_api_put"
        ]
    }
}

/*
 *  This function is called once when the app is installed
 */
def installed() {
    _event_subscribe()
}

/*
 *  This function is called every time the user changes
 *  their 
// Automatically generated. Make future change here.
 */
def updated()
{
    log.debug "updated"
    unsubscribe()
    _event_subscribe()
}

/* --- event section --- */

/*
 *  What events are we interested in. This needs
 *  to be in it's own function because both
 *  updated() and installed() are interested in it.
 */
def _event_subscribe()
{
	subscribe(d_switch, "switch", "_on_event")
    subscribe(d_switch,"switchOn", "_on_event")
    subscribe(d_switch,"switchOff", "_on_event")
    
    
    subscribe(d_motion, "motion", "_on_event")
    subscribe(d_temperature, "temperature", "_on_event")
    subscribe(d_contact, "contact", "_on_event")
    subscribe(d_acceleration, "acceleration", "_on_event")
    subscribe(d_presence, "presence", "_on_event")
    subscribe(d_battery, "battery", "_on_event")
    subscribe(d_threeAxis, "threeAxis", "_on_event")
    
    subscribe(d_lock,"lock", "_on_event")
    subscribe(d_lock,"locked", "_on_event")
    subscribe(d_lock,"unlocked", "_on_event")
    
    
}

/*
 *  This function is called whenever something changes.
 *  Right now it just 
 */
def _on_event(evt)
{
    def dt = _device_and_type_for_id(evt.deviceId)
    if (!dt) {
        log.debug "_on_event deviceId=${evt.deviceId} not found?"
        return;
    }
    
    def jd = _device_to_json(dt.device, dt.type)
    log.debug "_on_event deviceId=${jd}"

    _send_mqtt(dt.device, dt.type, jd)

}

/* --- API section --- */
def _api_list()
{
    _devices_for_type(params.type).collect{
        _device_to_json(it, params.type)
    }
}

def _api_put()
{
    def devices = _devices_for_type(params.type)
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {
        _device_command(device, params.type, request.JSON)
    }
}

def _api_get()
{
    def devices = _devices_for_type(params.type)
    def device = devices.find { it.id == params.id }
    if (!device) {
        httpError(404, "Device not found")
    } else {
        _device_to_json(device, type)
    }
}

void _api_update()
{
    _do_update(_devices_for_type(params.type), params.type)
}

/*
 *  I don't know what this does but it seems to be needed
 */
def deviceHandler(evt) {
}

/* --- communication section: not done --- */

/*
 *  An example of how to get PushingBox.com to send a message
 */
def _send_pushingbox() {
    log.debug "_send_pushingbox called";

    def devid = "XXXX"
    def messageText = "Hello_World"

    httpGet("http://api.pushingbox.com/pushingbox?devid=${devid}&message=xxx_xxx")
}

/*
 *  Send information the the IOTDB MQTT Bridge
 *  See https://iotdb.org/playground/mqtt/bridge for documentation
 */
def _send_mqtt(device, device_type, deviced) {
    if (!iotdb_api_username || !iotdb_api_key) {
        return
    }

    log.debug "_send_mqtt called";

    def now = Calendar.instance
    def date = now.time
    def millis = date.time
    def sequence = millis
    def isodatetime = deviced?.value?.timestamp
    
    def digest = "${iotdb_api_key}/${iotdb_api_username}/${isodatetime}/${sequence}".toString();
    def hash = digest.encodeAsMD5();
    
    def topic = "${device_type}/${deviced.id}".toString()
    
    def uri = "https://iotdb.org/playground/mqtt/bridge"
    def headers = [:]
    def body = [
        "topic": topic,
        "payloadd": deviced?.value,
        "timestamp": isodatetime,
        "sequence": sequence,
        "signed": hash,
        "username": iotdb_api_username
    ]

    def params = [
        uri: uri,
        headers: headers,
        body: body
    ]

    log.debug "_send_mqtt: params=${params}"
    httpPutJson(params) { log.debug "_send_mqtt: response=${response}" }
}



/* --- internals --- */
/*
 *  Devices and Types Dictionary
 */
def _dtd()
{
    [ 
        
        switch: d_switch, 
        switchOn: d_switch,
        switchOff: d_switch,
        
        motion: d_motion, 
        temperature: d_temperature, 
        contact: d_contact,
        acceleration: d_acceleration,
        presence: d_presence,
        battery: d_battery,
        threeAxis: d_threeAxis,
        
        lock: d_lock,
        locked: d_lock,
        unlocked: d_lock
        
        
    ]
}

def _devices_for_type(type) 
{
    _dtd()[type]
}

def _device_and_type_for_id(id)
{
    def dtd = _dtd()
    
    for (dt in _dtd()) {
        def type = dt.key
        def devices = dt.value
        for (device in devices) {
            if (device.id == id) {
                return [ device: device, type: type ]
            }
        }
    }
}

/**
 *  Do a device command
 */
private _device_command(device, type, jsond) {
	if (!device) {
        return;
    }
    if (!jsond) {
        return;
    }
    
    if (type == "switch") {
        def n = jsond['switch']
        if (n == -1) {
            def o = device.currentState('switch')?.value
            n = ( o != 'on' )
        }
        if (n) {
            device.on()
        } else {
            device.off()
        }
    } else {
        log.debug "_device_command: device type=${type} doesn't take commands"
    }
}

/*
 *  Convert a single device into a JSONable object
 */
private _device_to_json(device, type) {
    if (!device) {
        return;
    }

    def vd = [:]
    def jd = [id: device.id, label: device.label, type: type, value: vd];
    if (type == "switch") {
        def s = device.currentState('switch')
        vd['timestamp'] = s?.isoDate
        vd['switch'] = s?.value == "on"
    }  else if (type == "switchOn") {
    	device.on()
        def s = device.currentState('switch')
        vd['timestamp'] = s?.isoDate
        vd['switch'] = s?.value == "on"
    } else if (type == "switchOff") {
    	device.off()
        def s = device.currentState('switch')
        vd['timestamp'] = s?.isoDate
        vd['switch'] = s?.value == "on"
    }
    
    else if (type == "motion") {
        def s = device.currentState('motion')
        vd['timestamp'] = s?.isoDate
        vd['motion'] = s?.value == "active"
    } else if (type == "temperature") {
        def s = device.currentState('temperature')
        vd['timestamp'] = s?.isoDate
        vd['temperature'] = s?.value.toFloat()
    } else if (type == "contact") {
        def s = device.currentState('contact')
        vd['timestamp'] = s?.isoDate
        vd['contact'] = s?.value == "closed"
    } else if (type == "acceleration") {
        def s = device.currentState('acceleration')
        vd['timestamp'] = s?.isoDate
        vd['acceleration'] = s?.value == "active"
    } else if (type == "presence") {
        def s = device.currentState('presence')
        vd['timestamp'] = s?.isoDate
        vd['presence'] = s?.value == "present"
    } else if (type == "battery") {
        def s = device.currentState('battery')
        vd['timestamp'] = s?.isoDate
        vd['battery'] = s?.value.toFloat() / 100.0;
    } else if (type == "threeAxis") {
        def s = device.currentState('threeAxis')
        vd['timestamp'] = s?.isoDate
        vd['x'] = s?.xyzValue?.x
        vd['y'] = s?.xyzValue?.y
        vd['z'] = s?.xyzValue?.z
    }
    
    else if (type == "lock"){
    	def s = device.currentState('lock')
        vd['timestamp'] = s?.isoDate
        vd['lock'] = s?.value
    } else if(type == "locked"){
    	device.lock();
    	def s = device.currentState('lock')
        vd['timestamp'] = s?.isoDate
        vd['lock'] = s?.value
        
    } else if(type == "unlocked"){
    	device.unlock();
    	def s = device.currentState('lock')
        vd['timestamp'] = s?.isoDate
        vd['lock'] = s?.value   
    }
    
   
    return jd
}

