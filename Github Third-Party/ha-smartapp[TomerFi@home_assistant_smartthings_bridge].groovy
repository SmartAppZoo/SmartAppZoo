/************************************************************************************************
 * Home Assistant Bridge (Current Version: 20180512)
 * *************************************************
 * Created by Tomer Figenblat
 * GitHub: https://github.com/TomerFi
 * YouTube: https://www.youtube.com/channel/UCH9z4dabjTo-pRqM3_i5RTg
 * Linkedin: https://www.linkedin.com/in/tomer-figenblat-18aabb76/
 *
 * This smartapp allows bridging devices from Smartthings to Home Assistant.
 * This is not a "standalone" app, A designated Home Assistant custom component is required.
 * You can find all the required files and documentation in the project's github repository:
 * https://github.com/TomerFi/home_assistant_smartthings_bridge
 *
 * This project is a work in progress, so please follow the github repository for future updates.
 * Tested contributions to this project will be gladly accepted via the repository.
 * If any error occurs, please open an issue with the appropriate information and log details.
 *
 * Supported capabilites as HA sensor entities:
 * - capability.accelerationSensor
 * - capability.airQualitySensor
 * - capability.carbonDioxideMeasurement
 * - capability.contactSensor
 * - capability.dustSensor
 * - capability.illuminanceMeasurement
 * - capability.motionSensor
 * - capability.odorSensor
 * - capability.presenceSensor
 * - capability.sleepSensor
 * - capability.smokeDetector
 * - capability.soundSensor
 * - capability.stepSensor
 * - capability.temperatureMeasurement
 * - capability.touchSensor
 * - capability.waterSensor
 *
 * There's no need to change anything in this smartapp
 *
 ***********************************************************************************************/

/*##############################
#### Smartapp configuration ####
##############################*/
definition(
    name: "HomeAssistant.bridge",
    namespace: "tomerfi",
    author: "Tomer Figenblat",
    description: "Bridge devices to Home Assistant",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)
    
preferences {
	page(name: "ha_def_pg", title: "Home Assistant Stuff", nextPage: "st_def_pg", install: false, uninstall: true) {
    	section("Home Assistant") {
        	input(name: "ha_url", title: "HA Domain Name", description: "The FQDN for accessing your Home Assistant environment remotly",
            	required: true, type: "text", multiple: false)
            input(name: "ha_port", title: "HA Port", description: "The port for accessing your Home Assistant environment remotly",
            	required: true, type: "number", multiple: false)
            input(name: "ha_pass", title: "HA Password", description: "The API Password set for your Home Assistant environment",
            	required: true, type: "password", multiple: false)
        }
    }
    
    page(name: "st_def_pg", title: "SmartThings Stuff", install: true, uninstall: false) {
    	section(name: "Devices To Bridge", hideWhenEmpty: true) {
        	input(name: "acceleration_sensors", title: "Acceleration Sensors", required: false, multiple: true, type: "capability.accelerationSensor")
            input(name: "air_quality_sensors", title: "Air Quality Sensors", required: false, multiple: true, type: "capability.airQualitySensor")
            input(name: "carbon_dioxide_sensors", title: "Carbon Dioxide Sensors", required: false, multiple: true, type: "capability.carbonDioxideMeasurement")
            input(name: "contact_sensors", title: "Contact Sensors", required: false, multiple: true, type: "capability.contactSensor")
            input(name: "dust_sensors", title: "Dust Sensors", required: false, multiple: true, type: "capability.dustSensor")
            input(name: "illuminance_sensors", title: "Illuminance Sensors", required: false, multiple: true, type: "capability.illuminanceMeasurement")
            input(name: "motion_sensors", title: "Motion Sensors", required: false, multiple: true, type: "capability.motionSensor")
            input(name: "odor_sensors", title: "Odor Sensors", required: false, multiple: true, type: "capability.odorSensor")
            input(name: "presence_sensors", title: "Presence Sensors", required: false, multiple: true, type: "capability.presenceSensor")
            input(name: "sleep_sensors", title: "Sleep Sensors", required: false, multiple: true, type: "capability.sleepSensor")
            input(name: "smoke_sensors", title: "Smoke Sensors", required: false, multiple: true, type: "capability.smokeDetector")
			input(name: "sound_sensors", title: "Sound Sensors", required: false, multiple: true, type: "capability.soundSensor")
            input(name: "step_sensors", title: "Step Sensors", required: false, multiple: true, type: "capability.stepSensor")
            input(name: "temperature_sensors", title: "Temperature Sensors", required: false, multiple: true, type: "capability.temperatureMeasurement")
			input(name: "touch_sensors", title: "Touch Sensors", required: false, multiple: true, type: "capability.touchSensor")
            input(name: "water_sensors", title: "Water Sensors", required: false, multiple: true, type: "capability.waterSensor")
        }
    }
}

/*####################
#### API Mappings ####
####################*/
mappings {
    path("/get_devices") {
        action: [
            GET: "_get_devices"
        ]
    }
    path("/start_bridge") {
        action: [
            GET: "_start_bridge"
        ]
    }
    path("/stop_bridge") {
        action: [
            GET: "_stop_bridge"
        ]
    }
    path("/check_bridge") {
        action: [
            GET: "_check_bridge"
        ]
    }
}

/*########################
#### Smartapp Methods ####
########################*/
def installed() {
	log.info "smartapp installed"
    state.running = false
    try {
    	_send_api_info_to_ha()
    	log.info "bridge is not running, please start"
    } catch (ex) {
    	log.error("failed to send api info to ha, caught exception", ex)
    }
    
}

def updated() {
    log.info "smart app updated"
    if (state.running) {
    	log.info "restarting bridge"
        try {
        	_unsubscribe_handlers()
        	_subscribe_handlers()
        } catch (ex) {
        	log.error("failed restart the bridge, caught exception", ex)
        }
    } else {
    	log.info "bridge is not running, please start"
    }
}

def uninstalled() {
	log.info "smartapp uninstalled"
    try {
    	_fire_homeassistant_event("st_smartapp_uninstalled")
    } catch (ex) {
    	log.error("failed to send st_smartapp_uninstalled event to ha, caught exception", ex)
    }
}

/*#############################
#### API Requests Handlers ####
#############################*/
def _get_devices() {
    log.info "get devices request received"
    try {
    	_get_selected_devices().collect{ _create_device_object(it) }
    } catch (ex) {
    	log.error("get devices failed, caught exception", ex)
        [error: true, message: ex.message]
    }
}

def _start_bridge() {
	log.info "start request received"
    if (state.running) {
    	log.info "bridge is already running"
    	return [status: false, message: "bridge is already running"]
    }
	try {
    	_subscribe_handlers()
    	return [status: true, message: "bridge started"]
    } catch (ex) {
    	log.error("start bridge failed, caught exception", ex)
        [error: true, message: ex.message]
    }
}

def _stop_bridge() {
	log.info "stop request received"
    if (!state.running) {
    	log.info "bridge is not running"
        return [status: false, message: "bridge is not running"]
    }
	try {
    	_unsubscribe_handlers()
    	return [status: true, message: "bridge stopped"]
    } catch (ex) {
    	log.error("stop bridge failed, caught exception", ex)
        [error: true, message: ex.message]
    }
}

def _check_bridge() {
	log.info "check request received"
    if (state.running) {
    	log.info "bridge is running"
    	[status: true, message: "bridge is running"]
    } else {
    	log.info "bridge is not running"
    	[status: false, message: "bridge is not running"]
    }
}

/*######################
#### Event Handlers ####
######################*/
def _event_recieved(evt) {
	log.debug "state changed event received"
    try {
    	_fire_homeassistant_event("st_bridge_device_update", _create_device_object([ "device": evt.device, "attributes": [evt.name] ]))
    } catch (ex) {
    	log.error("failed to handle state change event, caught exception", ex)
    }
}

def deviceHandler(evt) {
	log.debug "tomerfi: need to check this event ${evt.name}"
}

def _subscribe_handlers() {
    log.info "starting bridge"
    
    if (acceleration_sensors) {
    	_device_type_map().acceleration_device.attributes.each { subscribe(acceleration_sensors, it, "_event_recieved") }
    }
    if (air_quality_sensors) {
    	_device_type_map().air_quality_device.attributes.each { subscribe(air_quality_sensors, it, "_event_recieved") }
    }
    if (carbon_dioxide_sensors) {
    	_device_type_map().carbon_dioxide_device.attributes.each { subscribe(carbon_dioxide_sensors, it, "_event_recieved") }
    }
    if (contact_sensors) {
    	_device_type_map().contact_device.attributes.each { subscribe(contact_sensors, it, "_event_recieved") }
    }
    if (dust_sensors) {
    	_device_type_map().dust_device.attributes.each { subscribe(dust_sensors, it, "_event_recieved") }
    }
    if (illuminance_sensors) {
    	_device_type_map().illuminance_device.attributes.each { subscribe(illuminance_sensors, it, "_event_recieved") }
    }
    if (motion_sensors) {
    	_device_type_map().motion_device.attributes.each { subscribe(motion_sensors, it, "_event_recieved") }
    }
    if (odor_sensors) {
    	_device_type_map().odor_device.attributes.each { subscribe(odor_sensors, it, "_event_recieved") }
    }
    if (presence_sensors) {
    	_device_type_map().presence_device.attributes.each { subscribe(presence_sensors, it, "_event_recieved") }
    }
    if (sleep_sensors) {
    	_device_type_map().sleep_device.attributes.each { subscribe(sleep_sensors, it, "_event_recieved") }
    }
    if (smoke_sensors) {
    	_device_type_map().smoke_device.attributes.each { subscribe(smoke_sensors, it, "_event_recieved") }
    }
    if (sound_sensors) {
    	_device_type_map().sound_device.attributes.each { subscribe(sound_sensors, it, "_event_recieved") }
    }
    if (step_sensors) {
    	_device_type_map().step_device.attributes.each { subscribe(step_sensors, it, "_event_recieved") }
    }
    if (temperature_sensors) {
    	_device_type_map().temperature_device.attributes.each { subscribe(temperature_sensors, it, "_event_recieved") }
    }
    if (touch_sensors) {
    	_device_type_map().touch_device.attributes.each { subscribe(touch_sensors, it, "_event_recieved") }
    }
    if (water_sensors) {
    	_device_type_map().water_device.attributes.each { subscribe(water_sensors, it, "_event_recieved") }
    }

    state.running = true
    log.info "bridge started"
}

def _unsubscribe_handlers() {
    log.info "stopping bridge"
    unsubscribe()
    state.running = false
    log.info "bridge stopped"
}

/*########################
#### Helper Functions ####
########################*/
private _get_selected_devices() {
	log.info "gathering selected devices"
    def selected_devices = []
    
    if (acceleration_sensors) {
    	acceleration_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().acceleration_device.attributes ] }
    }
    if (air_quality_sensors) {
    	air_quality_sensors.each { selected_devices <<  [ "device": it, "attributes":  _device_type_map().air_quality_device.attributes ] }
    }
    if (carbon_dioxide_sensors) {
    	carbon_dioxide_sensors.each { selected_devices <<  [ "dev": it, "attributes": _device_type_map().carbon_dioxide_device.attributes ] }
    }
    if (contact_sensors) {
    	contact_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().contact_device.attributes ] }
    }
    if (dust_sensors) {
    	dust_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().dust_device.attributes ] }
    }
    if (illuminance_sensors) {
    	illuminance_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().illuminance_device.attributes ] }
    }
    if (motion_sensors) {
    	motion_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().motion_device.attributes ] }
    }
    if (odor_sensors) {
    	odor_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().odor_device.attributes ] }
    }
    if (presence_sensors) {
    	presence_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().presence_device.attributes ] }
    }
    if (sleep_sensors) {
    	sleep_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().sleep_device.attributes ] }
    }
    if (smoke_sensors) {
    	smoke_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().smoke_device.attributes ] }
    }
    if (sound_sensors) {
    	sound_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().sound_device.attributes ] }
    }
    if (step_sensors) {
    	step_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().step_device.attributes ] }
    }
    if (temperature_sensors) {
    	temperature_sensors.each { selected_devices <<  [ "dev": it, "attributes": _device_type_map().temperature_device.attributes ] }
    }
    if (touch_sensors) {
    	touch_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().touch_device.attributes ] }
    }
    if (water_sensors) {
    	water_sensors.each { selected_devices <<  [ "device": it, "attributes": _device_type_map().water_device.attributes ] }
    }
    
    log.debug "found ${selected_devices.size()} devices"
    
    selected_devices
}

private _create_device_object(device_map) {
    def device_json = [
    	id: device_map.device.id,
        label: device_map.device.label,
        hub_name: device_map.device.hub?.name,
        display_name: device_map.device.displayName,
        manufacturer_name: device_map.device?.manufacturerName,
        model_name: device_map.device?.modelName,
        status: device_map.device.status,
        type_name: device_map.device.typeName,
        ha_type: _get_ha_type_by_attribute(device_map.attributes[0]),
        watched_attributes: [:]        
    ]
    
    device_map.attributes.each { device_json.watched_attributes[it] = _get_attribute_data(device_map.device, it) }
    
    device_json
}

private _get_attribute_data(device, attribute) {
    [
        value: _fix_value(attribute, device.currentState(attribute)),
        last_changed: device.currentState(attribute)?.isoDate
    ]
}

private _fix_value(attribute, state_obj) {
    if (attribute in _device_type_map().acceleration_device.attributes) {
    	return state_obj?.value //schema: ActivityState type:ENUM[active, inactive]
    } 
    else if (attribute in _device_type_map().air_quality_device.attributes) {
    	return state_obj?.value //schema: PositiveInteger type: NUMBER
    }
    else if (attribute in _device_type_map().carbon_dioxide_device.attributes) {
    	return state_obj?.value //schema: PositiveInteger type: NUMBER
    }
    else if (attribute in _device_type_map().contact_device.attributes) {
    	return  state_obj?.value //schema: ContactState type: ENUM[closed, open]
    }
    else if (attribute in _device_type_map().dust_device.attributes) {
    	return state_obj?.value //type: NUMBER
    }
    else if (attribute in _device_type_map().illuminance_device.attributes) {
    	return state_obj?.value //type: NUMBER unit: lux
    }
    else if (attribute in _device_type_map().motion_device.attributes) {
    	return state_obj?.value //schema: ActivityState type:ENUM[active, inactive]
    }
    else if (attribute in _device_type_map().odor_device.attributes) {
    	return state_obj?.value //type: NUMBER
    }
    else if (attribute in _device_type_map().presence_device.attributes) {
    	return state_obj?.value //schema: PresenceState type: ENUM[not present, present]
    }
    else if (attribute in _device_type_map().sleep_device.attributes) {
    	return state_obj?.value //type: ENUM[not sleeping, sleeping]
    }
    else if (attribute in _device_type_map().smoke_device.attributes) {
    	return state_obj?.value //type: ENUM[clear, detected, tested]
    }
    else if (attribute in _device_type_map().sound_device.attributes) {
    	return state_obj?.value //type: NUMBER
    }
    else if (attribute in _device_type_map().step_device.attributes) {
    	return state_obj?.value //schema: PositiveInteger type: NUMBER
    }
    else if (attribute in _device_type_map().temperature_device.attributes) {
    	return state_obj?.value //type: NUMBER
    }
    else if (attribute in _device_type_map().touch_device.attributes) {
    	if (state_obj?.value == "touched" ) {
        	return state_obj?.value
        } else {
        	return "clear"
        } //type: ENUM[touched]
    }
    else if (attribute in _device_type_map().water_device.attributes) {
    	return state_obj?.value //schema: MoistureState type: ENUM[dry, wet]
    }
}

private _get_ha_type_by_attribute(attribute) {
	for (def device_type: _device_type_map()) {
    	if (attribute in device_type.value.attributes) {
        	return device_type.value.ha_type
        }
    }
}

/*#####################
#### Data Mappings ####
#####################*/
private _device_type_map() {
	[
		acceleration_device: [
        	attributes: ["acceleration"],
            ha_type: "sensor"
        ],
        air_quality_device: [
        	attributes: ["airQuality"],
            ha_type: "sensor"
        ],
        carbon_dioxide_device: [
        	attributes: ["carbonDioxide"],
            ha_type: "sensor"
        ],
        contact_device: [
        	attributes: ["contact"],
            ha_type: "sensor"
        ],
        dust_device: [
        	attributes: ["fineDustLevel", "dustLevel"],
            ha_type: "sensor"
        ],
        illuminance_device: [
        	attributes: ["illuminance"],
            ha_type: "sensor"
        ],
        motion_device: [
        	attributes: ["motion"],
            ha_type: "sensor"
        ],
        odor_device: [
        	attributes: ["odorLevel"],
            ha_type: "sensor"
        ],
        presence_device: [
        	attributes: ["presence"],
            ha_type: "sensor"
        ],
        sleep_device: [
        	attributes: ["sleeping"],
            ha_type: "sensor"
        ],
        smoke_device: [
        	attributes: ["smoke"],
            ha_type: "sensor"
        ],
        sound_device: [
        	attributes: ["sound"],
            ha_type: "sensor"
        ],
        step_device: [
        	attributes: ["goal", "steps"],
            ha_type: "sensor"
        ],
        temperature_device: [
        	attributes: ["temperature"],
            ha_type: "sensor"
        ],
        touch_device: [
        	attributes: ["touch"],
            ha_type: "sensor"
        ],
        water_device: [
        	attributes: ["water"],
            ha_type: "sensor"
        ]
    ]
}

/*##################################
#### Home Assistant Integration ####
##################################*/
private _send_api_info_to_ha() {
	def st_api_url = "${getApiServerUrl()}/api/smartapps/installations/${app.id}"
    if (!state.accessToken) {
    	createAccessToken()
    }
    def uri = "https://${ha_url.replace("http://", "").replace("https://", "")}:${ha_port}/api/services/persistent_notification/create"
    
    def headers = [
    	"x-ha-access": ha_pass,
        "Content-Type": "application/json"
    ]
    
	def params_for_url = [
        uri: uri,
        headers: headers,
        body: [
    		title: "ST API Url",
        	message: "${st_api_url}".toString()
    	]
    ]
    
	def params_for_token = [
        uri: uri,
        headers: headers,
        body: [
    		title: "ST API Token",
        	message: state.accessToken
    	]
    ]

    log.trace "sending the api url to home assistant: ${st_api_url}"
    httpPostJson(params_for_url)
    
    log.trace "sending the api token to home assistant: ${state.accessToken}"
    httpPostJson(params_for_token)
}

private _fire_homeassistant_event(event_name, data = [:]) {
	def uri = "https://${ha_url.replace("http://", "").replace("https://", "")}:${ha_port}/api/events/${event_name}"
    
    def headers = [
    	"x-ha-access": ha_pass,
        "Content-Type": "application/json"
    ]

	def params = []
    if (data) {
        params = [
            uri: uri,
            headers: headers,
            body: data
        ]    
    } else {
        params = [
            uri: uri,
            headers: headers
        ]    
    }
    
    log.debug "firing home assistant event: ${event_name}"
    httpPostJson(params)
}
