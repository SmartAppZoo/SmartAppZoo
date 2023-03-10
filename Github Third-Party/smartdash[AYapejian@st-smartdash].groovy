/**
 *  SmartDash
 *
 *  Copyright 2016 Ara Yapejian
 */

definition(
    name:        "SmartDash",
    namespace:   "AYapejian",
    author:      "Ara Yapejian",
    description: "API Endpoints",
    category:    "",
    iconUrl:     "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url:   "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url:   "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth:       true
)

preferences {
    section("Allow SmartDash to control these things...") {
        input "switches",            "capability.switch",                 multiple: true, required: false, title: "Which Switches?"
        input "motionSensors",       "capability.motionSensor",           multiple: true, required: false, title: "Which Motion Sensors?"
        input "contactSensors",      "capability.contactSensor",          multiple: true, required: false, title: "Which Contact Sensors?"
        input "presenceSensors",     "capability.presenceSensor",         multiple: true, required: false, title: "Which Presence Sensors?"
        input "temperatureSensors",  "capability.temperatureMeasurement", multiple: true, required: false, title: "Which Temperature Sensors?"
        input "accelerationSensors", "capability.accelerationSensor",     multiple: true, required: false, title: "Which Vibration Sensors?"
        input "locks",               "capability.lock",                   multiple: true, required: false, title: "Which Locks?"
        input "threeAxis",           "capability.threeAxis",              multiple: true, required: false, title: "Which 3 Axis Sensors?"
        input "thermostats",         "capability.thermostat",             multiple: true, required: false, title: "Which Thermostats?"

        input "energyMeters",        "capability.energyMeter",            multiple: true, required: false, title: "Which Energy Meters?"
        input "powerMeters",         "capability.powerMeter",             multiple: true, required: false, title: "Which Power Meters?"
    }
}

/* ************************************************************************* */
/* Begin Route Definitions                                                   */
/* ************************************************************************* */
mappings {
    path("/all") {
        action: [GET: "getAll"]
    }
    path("/:deviceType") {
        action: [GET: "getAllByType"]
    }
    path("/locations") {
        action: [GET: "getCurrentLocation"]
    }
    path("/:deviceType/:id") {
        action: [GET: "getByTypeAndId", PUT: "update"]
    }
    path("/modes") {
    	action: [GET: "getModes", PUT: "setModes"]
    }
}

/* ************************************************************************* */
/* Install / Updated Event Handlers                                          */
/* ************************************************************************* */
def installed() {
	LOG("Installed with settings: ${settings}");
	initialize()
}

def updated() {
	LOG("Updated with settings: ${settings}");

	unsubscribe()
	initialize()
}

def initialize() {
    LOG("initialize()");
    // TODO: Make map to support multiple devices by [ deviceId: orientation ]
	state.lastOrientationSent = '';     // Tracks last normalized orientation sent
    state.smartdashEventPostUrl = "https://smartdash.io/api/events";	// Where HTTP Post on Subscribed Events

    subscribe(switches,            'switch',       switchEventHandler);
    subscribe(motionSensors,       'motion',       motionEventHandler);
    subscribe(contactSensors,      'contact',      contactEventHandler);
    subscribe(presenceSensors,     'presence',     presenceEventHandler);
    subscribe(temperatureSensors,  'temperature',  temperatureEventHandler);
    subscribe(accelerationSensors, 'acceleration', accelerationEventHandler);
    subscribe(locks,               'lock',         lockEventHandler);
    subscribe(threeAxis,           'threeAxis',    threeAxisEventHandler);
    subscribe(thermostats,         'thermostat',   thermostatEventHandler);

    subscribe(energyMeters,        'energy',       energyEventHandler);
    subscribe(powerMeters,         'power',        powerEventHandler);
}

def switchEventHandler(evt)       { LOG('switchEventHandler()');        sendPostEvent('switches',             'switch',       evt); }
def motionEventHandler(evt)       { LOG('motionEventHandler()');        sendPostEvent('motionSensors',        'motion',       evt); }
def contactEventHandler(evt)      { LOG('contactEventHandler()');       sendPostEvent('contactSensors',       'contact',      evt); }
def presenceEventHandler(evt)     { LOG('presenceEventHandler()');      sendPostEvent('presenceSensors',      'presence',     evt); }
def temperatureEventHandler(evt)  { LOG('temperatureEventHandler()');   sendPostEvent('temperatureSensors',   'temperature',  evt); }
def accelerationEventHandler(evt) { LOG('accelerationEventHandler()');  sendPostEvent('accelerationSensors',  'acceleration', evt); }
def lockEventHandler(evt)         { LOG('lockEventHandler()');          sendPostEvent('locks',                'lock',         evt); }
def thermostatEventHandler(evt)   { LOG('thermostatEventHandler()');    sendPostEvent('thermostats',          'thermostat',   evt); }

def threeAxisEventHandler(evt)    { LOG('threeAxisEventHandler()');     sendPostEventOrientation('threeAxis', 'threeAxis',    evt); }

def energyEventHandler(evt)       { LOG('energyEventHandler()');        sendPostEvent('energyMeters', 'energy', evt); }
def powerEventHandler(evt)        { LOG('powerEventHandler()');         sendPostEvent('powerMeters',  'power',  evt); }

/* ************************************************************************* */
/* Begin Route Handlers                                                      */
/* ************************************************************************* */
// GET: /all
def getAll() {
    LOG("getAll()");

	def supportedTypes = [
        "switches", "motionSensors", "contactSensors", "presenceSensors", "locks",
        "temperatureSensors", "accelerationSensors", "threeAxis", "thermostats",
        "energyMeters", "powerMeters"
    ];

    def returnDevices = [:]
    supportedTypes.each { type ->
		returnDevices[type] = settings[type]?.collect{ getDeviceDetails(it) } ?: []
    }

    return returnDevices
}
// GET: /:deviceType
def getAllByType() {
    LOG("getAllByType()");

    def type = params.deviceType
    settings[type]?.collect{ getDeviceDetails(it) } ?: []
}

// GET: /:deviceType/:id
def getByTypeAndId() {
    LOG("GET: /:deviceType/:id -- getByTypeAndId()");

    def type = params.deviceType
    def devices = settings[type]
    def device = devices.find { it.id == params.id }

    if (!device) {
        httpError(404, "Device not found")
    }
    else {
        getDeviceDetails(device)
    }
}

// GET: /locations
def getCurrentLocation() {
    LOG("GET: /locations -- getCurrentLocation()");

    def currentLocation              = [:]
    currentLocation.id               = location.id;
    currentLocation.name             = location.name;
    currentLocation.longitude        = location.longitude;
    currentLocation.latitude         = location.latitude;
    currentLocation.temperatureScale = location.temperatureScale;
    currentLocation.timeZone         = location.timeZone;
    currentLocation.zipCode          = location.zipCode;

    return [currentLocation: currentLocation] + [modes: getModes()]
}

// GET: /modes
def getModes() {
    LOG("getModes()");

	def returnModes = [:];
    location.modes.each { mode ->
        returnModes[mode.name] = [id: mode.id, name: mode.name];
    }

	return [current: location.mode] + [available: returnModes];
}

// PUT: /modes
def setMode(modeName) {
    LOG("setMode()");

    if (location.modes?.find {it.name == modeName}) {
        location.setMode(modeName)
    } else {
        httpError(404, "Mode not found");
    }
}

// PUT: /:deviceType/:id
def update() {
	LOG("update(): Received Update Command");

    def type = params.deviceType
    def data = request.JSON
    def devices = settings[type]
    def command = data.command
	LOG("Received Update Command: Type: ${type}, Data: ${data}, Command: ${command}");

	if (command) {
        def device = devices?.find { it.id == params.id }
        if (!device) {
            LOG("Device not found");
            httpError(404, "Device not found")
        } else {
            LOG("Running: ${device}.${command}()");
            device."$command"()
        }
    }
}

/*
FaceUp:    "-1008,22,-24"   -1, 0, 0
FaceDown:  "1033, -25, 32"  1, 0, 0
Top Up:    "8,40,-1016"     0, 0, -1
Letft Up:  "13,-1042,-43"   0, -1, 0
Right Up:  "19,1048,49"     0,  1, 0
Bottom Up: "0,-59,1012"     0,  0, 1
*/
def sendPostEventOrientation(theDeviceType, eventAttribute, evt) {
    LOG("Enter: sendPostEventOrientation($theDeviceType, $eventAttribute, $evt)");
    try {
    	final threshold = 250;
		def value = evt.xyzValue;
	    LOG("Parsing Orientation: $value");

    	// -250 -> 250 === 0, 250 -> 1000 === 1, -250 -> -1000 = -1
		def x = Math.abs(value.x) > threshold ? (value.x > 0 ? 1 : -1) : 0
		def y = Math.abs(value.y) > threshold ? (value.y > 0 ? 1 : -1) : 0
		def z = Math.abs(value.z) > threshold ? (value.z > 0 ? 1 : -1) : 0
        def normalizedXYZ = [x,y,z];

        def ORIENTATIONS = [
        	FACE_UP:   [-1, 0, 0],
            FACE_DOWN: [1, 0, 0],
            LEFT_UP:   [0, -1, 0],
            RIGHT_UP:  [0, 1, 0],
            TOP_UP:    [0, 0, -1],      // Not Working
            BOTTOM_UP: [0, 0, 1]        // Not Working
        ];

        def parsedEventValue = [ orientation: '', xyz: [:], xyzNormalized: [:] ];

		ORIENTATIONS.each { k,v -> if (normalizedXYZ == v) { parsedEventValue.orientation = k } }
        parsedEventValue.xyzNormalized = normalizedXYZ;
        parsedEventValue.xyz = [value.x, value.y, value.z]

        if (parsedEventValue.orientation && state.lastOrientationSent != parsedEventValue.orientation) {
	        state.lastOrientationSent = parsedEventValue.orientation;
        	LOG("***********SENDING POST EVENT: ${parsedEventValue.orientation}");

            sendPostParsedEvent(theDeviceType, eventAttribute, evt, parsedEventValue.orientation);
        } else {
        	LOG("Skipped sending orientation, either hasn't changed or doesnt match normalized values ${parsedEventValue}");
        }
    } catch (e) {
    	LOG("ERROR: sendPostEventOrientation(): Could not get orientation: $e");
    }
}

/* ************************************************************************* */
/* Subscribed Event Handlers                                                 */
/* ************************************************************************* */
def sendPostParsedEvent(theDeviceType, eventAttribute, evt, parsedEventValue) {
    LOG("ENTER: sendPostParsedEvent(${theDeviceType}, ${eventAttribute}, evt)");

    try {
        def params = [
            uri: state.smartdashEventPostUrl,
            body: [
                deviceType:       theDeviceType,
                deviceLabel:      evt.device,
                deviceId:         evt.deviceId,
                eventDate:        evt.date,
                eventDescription: evt.descriptionText,
                eventAttribute:   eventAttribute,
                eventValue:       evt.value,
                parsedEventValue: parsedEventValue
            ]
        ];

        httpPost(params) { resp ->
			LOG("sendPostEvent(): Post response recieved, statusCode ${resp.status}");
		}
    } catch (e) {
        LOG("ERROR: sendPostParsedEvent(): Error during httpPost $e");
    }
}

def sendPostEvent(theDeviceType, eventAttribute, evt) {
    LOG("ENTER: sendPostOnEvent(${theDeviceType}, ${eventAttribute}, evt)");

    try {
        def params = [
            uri: state.smartdashEventPostUrl,
            body: [
                deviceType:       theDeviceType,
                deviceLabel:      evt.device,
                deviceId:         evt.deviceId,
                eventDate:        evt.date,
                eventDescription: evt.descriptionText,
                eventAttribute:   eventAttribute,
                eventValue:       evt.value
            ]
        ];
        if (evt.hasProperty('parsedEventValue')) {
        	LOG("PARSED EVENT VALUE FOUND ${evt.parsedEventValue}");
        	params.body.parsedEventValue = evt.parsedEventValue
        }

        httpPost(params) { resp ->
			LOG("sendPostEvent(): Post response recieved, statusCode ${resp.status}");
		}
    } catch (e) {
        LOG("ERROR: sendPostEvent(): Error during httpPost $e");
    }
}

/* ************************************************************************* */
/* Helper Methods                                                            */
/* ************************************************************************* */

/**
 *  getDeviceDetails()
 *
 *  Assembles Device, Attribute and Supported Command details for a device
 **/
private def getDeviceDetails(device) {
    LOG("getDeviceDetails(${device})");

    if (!device) {
        LOG("Device Not Found");
        httpError(404, "Device not found")
    } else {
        def attributeMap = getSupportedAttributes(device)
        def supportedCommands = getSupportedCommands(device);

        return [device: device] + [attributes: attributeMap] + [supportedCommands: supportedCommands]
    }
}

/**
 *  getSupportedAttributes()
 *
 *  Returns map of all attributes for a given device with state
 **/
private def getSupportedAttributes(device) {
    LOG("getSupportedAttributes(${device})");

    def supportedAttributes = device.supportedAttributes
    def attributeMap = [:]

    supportedAttributes.each {attribute ->
        attributeMap[attribute.name] = device.currentState(attribute.name)
    }

    return attributeMap
}

/**
 *  getSupportedCommands()
 *
 *  Returns map of all commands supported for a given device
 **/
private def getSupportedCommands(device) {
    LOG("getSupportedCommands(${device})");

    def returnSupportedCommands = [:]
    def supportedCommands = device.supportedCommands

    supportedCommands.each {
        returnSupportedCommands[it.name] = it.arguments;
    }
    return returnSupportedCommands;
}

/**
 *  getGroupName()
 *
 *  Get the name of a 'Group' (i.e. Room) from its ID.
 **/
private def getGroupName(id) {
    LOG("getGroupName(${id})");

	if (id == null) {return 'Home'}

    else if (id == '98367de6-19de-4798-8813-451e7d31ec48') { return 'Kitchen'           }
    else if (id == '327e79ee-5bc3-4f2b-b583-2cda0bb2a0fa') { return 'Livingroom'        }
    else if (id == '2e903ac0-0074-495d-b548-1d0b1f49055f') { return 'Garage'            }

    else if (id == '6fb05dfb-1cc3-469c-b744-a5961144e011') { return 'Upstairs Hallway'  }
    else if (id == '45dd13cb-ad9b-4128-bd4e-709c472173db') { return 'Guest Room'        }
    else if (id == '6efd2cf0-0dd8-4de2-84e4-3576d8dbe372') { return 'Office'            }
    else if (id == '04ea989e-dc75-45c8-80f5-c4c3ba36817d') { return 'Bedroom'           }
    else if (id == '650e3e72-7ca0-4510-8732-cd7cec3565f3') { return 'Bedroom Bathroom'  }

    else if (id == 'f3f21cb7-ea34-4a1e-b656-3ac354091fb2') { return 'Upstairs Bathroom' }

	else { return 'Unknown' }
}

private def LOG(message) {
    log.trace message
}
