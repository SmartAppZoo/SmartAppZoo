/**
 *  Numerous.
 *
 *  Update your Numerous data using Smartthings Events (www.numerousapp.com).
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright © 2015 David Hodges
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed WITHOUT ANY WARRANTY; without even the implied
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  --------------------------------------------------------------------------
 *
 *  Version 1.0 (12/24/2015)
 */

import groovy.json.JsonSlurper

definition(
    name: "Numerous",
    namespace: "HodgePodgeDevelopments",
    author: "sandman8301@hotmail.com",
    description: "update your Numerous data using Smartthings events (www.numerousapp.com)",
    category: "My Apps",
    oauth: false
)

preferences {
    def textApiKey =
        "You can find your API key in the Numerous mobile apps under " +
        "Settings > Developer Info. The easiest way to enter your API " +
        "key is to copy and paste it."
    def inputApiKey = [
        name:       "apiKey",
        type:       "string",
        title:      "Your API Key",
        required:   true
    ]
    
    page(name:"NumerousAPI",title:"Numerous API Settings",nextPage:"SelectDeviceType", uninstall:true,install:false)
    {
    	section("Numerous API Key"){
        	paragraph textApiKey
        	input inputApiKey
        }
    }
	page(name:"SelectDeviceType",title:"Select Device Type",nextPage:"actions",uninstall:true,install:false)
    {
    	section("Select Device"){
        	input("sensorType", "enum", options: [
            	"switch":"Switch",
                "contactSensor":"Open/Closed Sensor",
                "accelerationSensor":"Vibration Sensor",
                "temperatureMeasurement":"Temperature Sensor"])
        }
    }
    page(name:"actions",title:"Select Sensor and Actions",nextPage:"NumerousSettings", uninstall:true,install:false)
    page(name:"NumerousSettings",title:"Numerous Settings",install:true)
}
def NumerousSettings(){
	dynamicPage(name: "NumerousSettings"){
    	section {
        	input(name:"NumerousMetric",type:"enum", title:"Metric",options: getNumerousMetrics(apiKey))
            input(name:"increment",type:"enum",title:"Increment?",options: ["yes":"yes, increase by 1","no":"no, display current value"])
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}
def actions() {
    dynamicPage(name: "actions") {
        section {
            input(name: "sensor", type: "capability.$sensorType", title: "If the $sensorType device")
            input(name: "action", type: "enum", title: "is", options: attributeValues(sensorType))
        }
    }
}
private getNumerousMetrics(def apiKey)
{
	LOG("getNumerousMetrics(${apiKey})")
	String auth = "${apiKey}:".bytes.encodeBase64()
    def headers = [
        "Authorization": "Basic ${auth}",
    ]

    def params = [
        uri:        "https://api.numerousapp.com/v2/users/me/metrics",
        headers:    headers
    ]

    //log.debug "params: ${params}"

	def outputs = ["new":"Create New"]
    try {
        httpGet(params) { response ->
        	
            def status = response.getStatus()
            if (status != 200) {
                log.error "Server error: ${response.getStatusLine()}"
                return
            }
            
            def metrics = response.getData().metrics 
            
            metrics.each{item ->
            	outputs = outputs + ["${item.id}":"${item.label}"]
            	log.debug "metric-id: ${item.id}, metric-label:${item.label}"
            }
        }
    } catch (e) {
        log.error "${e}"
    }
	return outputs
}
private attributeValues(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on","off"]
        case "contactSensor":
            return ["open","closed"]
        case "motionSensor":
            return ["active","inactive"]
        case "moistureSensor":
            return ["wet","dry"]
        case "accelerationSensor":
            return ["active","inactive"]
        case "temperatureMeasurement":
            return ["temperature"]
        default:
            return ["UNDEFINED"]
    }
}

def installed() {
    LOG("Installed with settings: ${settings}")
    state.installed = true
    initialize()
}

def updated() {
    LOG("Updated with settings: ${settings}")
    unsubscribe()
    state.metric_id = null
    initialize()
}

def onUpdateEvent(evt) {
	LOG("onUpdateEvent($evt.value)")
    

	try
    {
        def device = evt.device
        def metricId = state.metric_id
        def metricType = state.sensorType
        def increment = state.increment
        LOG(metricId)
        def data = []
        if (increment == "yes")
        {
            data = [
                value: 1,
                action: "ADD"
            ]
        }
        else
        {
        	def multiplier = 1
            switch(metricType)
            {
            	case "humidity":
                	multiplier = 100
                default:
                	multiplier = 1
            }
            data = [
                value: evt.value
            ]
        }
        apiUpdateValue(metricId, data)
    }
    catch (e)
    {
    	log.error ("${e}")
    }
}

private def setupInit() {
    LOG("setupInit()")

    if (state.installed == null) {
        state.installed = false
    }

    state.version = version()
    return true
}

private def initialize() {
    log.info "Numerous. Version ${version()}. ${copyright()}"

    if (!settings.apiKey) {
        log.error "Missing API key!"
        state.apiAuth = null
    } else {
        String auth = "${settings.apiKey}:".bytes.encodeBase64()
        state.apiAuth = auth
        state.action = settings.action
        state.increment = settings.increment

		if (settings.NumerousMetric == "new")
        {
        	if (state.metric_id==null)
            {
        		createMetrics(settings.sensorType,settings.sensor, settings.action)
                
            }
        }
        else
        {
        	LOG("Setting tracked metric id to ${settings.NumerousMetric}.")
        	state.metric_id = settings.NumerousMetric
        }
        
        setSubscriptions(settings.sensor, settings.action, settings.sensorType)
    }

    LOG("state: ${state}")
}

private def setSubscriptions(def sensor, def action, def sensorType)
{
	LOG("setSubscriptions(${sensor.displayName},${action},${sensorType})")
    try
    {
        if (sensorType == "accelerationSensor")
        {
            if (action == "inactive")
            {
                LOG("Subscribing to inactive accelerations for ${sensor.displayName}")
                subscribe(sensor,"acceleration.inactive",onUpdateEvent)
            }
            else 
            {
                LOG("Subscribing to active accelerations for ${sensor.displayName}")
                subscribe(sensor,"acceleration.active",onUpdateEvent)
            }
        }
        if (sensorType == "contactSensor")
        {
            if (action == "open")
            {
                LOG("Subscribing to open action for ${sensor.displayName}")
                subscribe(sensor,"contactSensor.open",onUpdateEvent)
            }
            else 
            {
                LOG("Subscribing to closed action for ${sensor.displayName}")
                subscribe(sensor,"contactSensor.closed",onUpdateEvent)
            }
        }
        if (sensorType == "switch")
        {
            if (action == "on")
            {
                LOG("Subscribing to on action for ${sensor.displayName}")
                subscribe(sensor,"switch.on",onUpdateEvent)
            }
            else 
            {
                LOG("Subscribing to off action for ${sensor.displayName}")
                subscribe(sensor,"switch.off",onUpdateEvent)
            }
        }
        if (sensorType == "temperatureMeasurement")
        {
            LOG("Subscribing to Temperature action for ${sensor.displayName}")
            subscribe(sensor,"temperature",onUpdateEvent)
        }
    }
    catch (e)
    {
    	log.error("${e}")
    }
    
}

private def createMetrics(def sensorType, def sensor, def action) {
    LOG("createMetrics(${sensorType},${sensor.displayName})")
    
    if (sensorType == "accelerationSensor")
    {
    	LOG("creating AccelerationSensorMetric")
    	createVibrateMetric(sensor, action)
    }
    if (sensorType == "contactSensor")
    {
    	LOG("creating ContactSensorMetric")
    	createContactMetric(sensor,action)
    }
    if (sensorType == "switch")
    {
    	LOG("creating switchMetric")
    	createSwitchMetric(sensor,action)
    }
    if (sensorType == "temperatureMeasurement")
    {
    	LOG("creating temperatureMeasurementMetric")
    	createTemperatureMetric(sensor)
    }
}
private def createTemperatureMetric(device) {
    LOG("createTemperatureMetric(${device})")

    def data = [
        label:          device.displayName,
        description:    "Temperature of ${device.displayName} at ${location.name}",
        kind:           "temperature",
        precision:      1,
        unit:           "°F",
        units:          "°F",
        visibility:     "private",
        value:          device.currentTemperature,
    ]

    apiCreateMetric(device.id, "temperature", data)
}
private def createContactMetric(device,action) {
    LOG("createContactMetric(${device})")

    def data = [
        label:          device.displayName +" (${action})",
        description:    "# of times ${device.displayName} at ${location.name} has ${action}",
        kind:           "number",
        precision:		1,
        units: 			"",
        unit: 			"",
        visibility:     "private",
        value:          0,
    ]

    apiCreateMetric(device.id, "contact${action}", data)
}
private def createSwitchMetric(device, action) {
    LOG("createSwitchMetric(${device},${action})")

    def data = [
        label:          device.displayName +" (Switch ${action})",
        description:    "# of times ${device.displayName} at ${location.name} has turned ${action}",
        kind:           "number",
        precision:		1,
        units: 			"",
        unit: 			"",
        visibility:     "private",
        value:          0,
    ]

    apiCreateMetric(device.id, "contact${action}", data)
}
private def createVibrateMetric(device,action) {
    LOG("createVibrateEndMetric(${device},${action})")

    def data = [
        label:          device.displayName +  "(${action})",
        description:    "# of times ${device.displayName} at ${location.name} when vibration is ${action}",
        kind:           "number",
        precision:		1,
        units: 			"",
        unit: 			"",
        visibility:     "private",
        value:          0,
    ]

    apiCreateMetric(device.id, "vibrate${action}", data)
}

private def apiCreateMetric(deviceId, deviceType, Map data) {
    LOG("apiCreateMetric(${deviceId}, ${deviceType}, ${data})")

	LOG("${state.apiAuth}")
    def headers = [
        "Authorization": "Basic ${state.apiAuth}",
    ]

    def params = [
        uri:        "https://api.numerousapp.com/v2/metrics",
        headers:    headers,
        body:       toJson(data)
    ]

    //log.debug "params: ${params}"

    try {
        httpPostJson(params) { response ->
            def status = response.getStatus()
            if (status != 201) {
                log.error "Server error: ${response.getStatusLine()}"
                return
            }

            def metric = response.getData()
            LOG("metric: ${metric}")
            def key = "${deviceType}_${deviceId}"
            state.metric_id = metric.id
            
            return metric.id
        }
    } catch (e) {
        log.error "${e}"
        return null
    }
}


private def apiUpdateMetric(metricId, Map data) {
    LOG("apiUpdateMetric(${metricId}, ${data})")

    def headers = [
        "Authorization": "Basic ${state.apiAuth}",
    ]

    def params = [
        uri:        "https://api.numerousapp.com/v2/metrics/${metricId}",
        headers:    headers,
        body:       toJson(data)
    ]

    //log.debug "params: ${params}"

    try {
        httpPutJson(params) { response ->
            def status = response.getStatus()
            if (status != 200) {
                log.error "Server error: ${response.getStatusLine()}"
                return
            }

            LOG("data: ${response.getData()}")
        }
    } catch (e) {
        log.error "${e}"
    }
}

private def apiDeleteMetric(metricId) {
    LOG("apiDeleteMetric(${metricId})")

    def headers = [
        "Authorization": "Basic ${state.apiAuth}",
    ]

    def params = [
        uri:        "https://api.numerousapp.com/v2/metrics/${metricId}",
        headers:    headers,
    ]

    //log.debug "params: ${params}"

    try {
        httpDelete(params) { response ->
            def status = response.getStatus()
            if (status != 204) {
                log.error "Server error: ${response.getStatusLine()}"
                return false
            }
        }
    } catch (e) {
        log.error "${e}"
        return false
    }

    return true
}

private def apiUpdateValue(metricId, Map data) {
    LOG("apiUpdateValue(${metricId}, ${data})")

    def headers = [
        "Authorization": "Basic ${state.apiAuth}",
    ]

    def params = [
        uri:        "https://api.numerousapp.com/v2/metrics/${metricId}/events",
        headers:    headers,
        body:       toJson(data)
    ]

    //log.debug "params: ${params}"

    try {
        httpPostJson(params) { response ->
            def status = response.getStatus()
            if (status != 200) {
                log.error "Server error: ${response.getStatusLine()}"
                return
            }

            LOG("data: ${response.getData()}")
        }
    } catch (e) {
        log.error "${e}"
    }
}

private def toJson(Map m) {
	return new org.codehaus.groovy.grails.web.json.JSONObject(m).toString()
}

private def version() {
    return "1.1.1"
}

private def copyright() {
    return "Copyright © 2015 HodgePodge"
}

private def about() {
    def text =
        "This SmartApp allows you to update your Numerous data using " +
        "Smartthings events (www.numerousapp.com).\n\n" +
        "Version ${version()}\n${copyright()}\n\n" +
        "You can contribute to the development of this app by making a " +
        "PayPal donation to sandman8301@hotmail.com. We appreciate your support."
}

private def LOG(message) {
    log.trace message
}