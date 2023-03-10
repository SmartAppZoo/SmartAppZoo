/**
 *  Wattvision Manager
 *
 *  Author: steve
 *  Date: 2014-02-13
 */
preferences {
	page(name: "rootPage", title: "Wattvision", install: true, uninstall: true) {
		section {
			input(name: "wattvisionDataType", type: "enum", required: false, multiple: false, defaultValue: "rate", options: ["rate", "consumption"])
			label(title: "Assign a name")
		}
	}
}

mappings {
	path("/access") {
		actions: [
			POST: "setApiAccess",
			DELETE: "revokeApiAccess"
		]
	}
	path("/devices") {
		actions: [
			GET: "listDevices"
		]
	}
	path("/device/:sensorId") {
		actions: [
			GET: "getDevice",
			PUT: "updateDevice",
			POST: "createDevice",
			DELETE: "deleteDevice"
		]
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	unschedule()
	initialize()
}

def initialize() {
	getDataFromWattvision()
	// TODO: only schedule if we receive state.wattvisionApiAccess from Wattvision
	schedule("* /5 * * * ?", getDataFromWattvision) // every 5 minutes
}

def getDataFromWattvision() {
	log.debug "getting data from wattvision"

	def children = getChildDevices()
	if (!children) {
		// currently only support one child
		return
	}

	def endDate = new Date()
	def startDate

	if (!state.lastUpdated) {
		log.debug "no state.lastUpdated"
		startDate = new Date(hours: endDate.hours - 3)
	} else {
		log.debug "parsing state.lastUpdated"
		startDate = new Date().parse(smartThingsDateFormat(), state.lastUpdated)
	}

	state.lastUpdated = endDate.format(smartThingsDateFormat())

	children.each { child ->
		getDataForChild(child, startDate, endDate)
	}

}

def getDataForChild(child, startDate, endDate) {
	if (!child) {
		return
	}

	def wattvisionURL = wattvisionURL(child.deviceNetworkId, startDate, endDate)
	if (wattvisionURL) {
		httpGet(uri: wattvisionURL) { response ->
			def json = new org.codehaus.groovy.grails.web.json.JSONObject(response.data.toString())
			child.addWattvisionData(json)
			return "success"
		}
	}
}

def wattvisionURL(senorId, startDate, endDate) {
	log.debug "getting wattvisionURL"

	def wattvisionApiAccess = state.wattvisionApiAccess
	if (!wattvisionApiAccess.id || !wattvisionApiAccess.key || !wattvisionApiAccess.url) {
		return null
	}

	if (!endDate) {
		endDate = new Date()
	}
	if (!startDate) {
		startDate = new Date(hours: endDate.hours - 3)
	}

	def params = [
		"sensor_id": senorId,
		"api_id": wattvisionApiAccess.id,
		"api_key": wattvisionApiAccess.key,
		"type": wattvisionDataType ?: "rate",
		"start_time": startDate.format(wattvisionDateFormat()),
		"end_time": endDate.format(wattvisionDateFormat())
	]

	def parameterString = params.collect { key, value -> "${key.encodeAsURL()}=${value.encodeAsURL()}" }.join("&")
	def url = "${wattvisionApiAccess.url}?${parameterString}"

	log.debug "wattvisionURL: ${url}"
	return url
}

def getData() {
	state.lastUpdated = new Date().format(smartThingsDateFormat())
}

public smartThingsDateFormat() { "yyyy-MM-dd'T'HH:mm:ss.SSSZ" }

public wattvisionDateFormat() { "yyyy-MM-dd'T'HH:mm:ss" }

def childMarshaller(child) {
	return [
		name: child.name,
		label: child.label,
		sensor_id: child.deviceNetworkId,
		location: child.location.name
	]
}

/*
			ENDPOINTS
*/

def listDevices() {
	getChildDevices().collect { childMarshaller(it) }
}

def getDevice() {
	def child = getChildDevice(params.sensorId)

	if (!child) {
		httpError(404, "Device not found")
	}

	return childMarshaller(child)
}

def updateDevice() {
	def body = request.JSON

	def child = getChildDevice(params.sensorId)

	if (!child) {
		httpError(404, "Device not found")
	}

	child.addWattvisionData(body)

	render([status: 204, data: " "])
}

def createDevice() {
	if (getChildDevice(params.sensorId)) {
		httpError(403, "Device already exists")
	}

	def child = addChildDevice("Wattvision", "Wattvision", params.sensorId, null, [name: "Wattvision", label: request.JSON.label])

	getDataForChild(child, null, null)

	return childMarshaller(child)
}

def deleteDevice() {
	deleteChildDevice(params.sensorId)
	render([status: 204, data: " "])
}

def setApiAccess() {
	def body = request.JSON
	state.wattvisionApiAccess = [
		url: body.url,
		id: body.id,
		key: body.key
	]
	render([status: 204, data: " "])
}

def revokeApiAccess() {
	state.wattvisionApiAccess = [:]
	render([status: 204, data: " "])
}