/**
 *  Living Legrand
 *
 *  Copyright 2016 Alain Mena Galindo
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
	name: "Living Legrand",
	namespace: "wimzel",
	author: "Alain Mena Galindo",
	description: "Connect and sync your Legrand LC6001 Hub Zones into SmartThings. You'll no longer have to be at home to take control!",
	category: "My Apps",
	iconUrl: "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=76&y=76&a=true&file=76x76.png&t=BIIe0l9rpkFoT2s&scalingup=0",
	iconX2Url: "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=120&y=120&a=true&file=120x120.png&t=mIpelIItFvjnD9j&scalingup=0",
	iconX3Url: "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=152&y=152&a=true&file=152x152.png&t=3d7Rouj24XVkHOC&scalingup=0")

// ========================================================
// PAGES - PREFERENCES
// ========================================================

preferences {
	page(name: "firstPage")
	page(name: "secondPage")
	page(name: "thirdPage")
}

// ========================================================
// PAGES - FIRST - PAGE
// ========================================================

def firstPage() {
	
	// SUBSCRIBE TO LOCATION HANDLER TO COMMUNICATE WITH THE LEGRAND HUB	
	startSubscriptions()
	
	dynamicPage(name: "firstPage", title: "EASY PEASY!", nextPage: "secondPage", uninstall: true) {
		
		section("Step #1") {
			image "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=1920&y=549&a=true&file=search.png&t=GOdoEvtg56haq3N&scalingup=0"
			paragraph "We're going to scout your Legrand Hub for details."
		}
		
		section("Hub's IP Address") {
			input "ip", "text", required: true, title: "What is your Hub's IP address? Check the LCD panel towards the front of your Legrand LC6001."
		}
		
		section("Learn More") {
			href(name: "Blog", title: "FAQs", required: false, url: "https://blog.wimzel.com/developments/living-legrand/faq/", description: "Just incase you need help.")
		}
		
	}

}

// ========================================================
// PAGES - SECOND - PAGE
// ========================================================

def secondPage() {
	
	// SEARCH FOR HUB AND ZONES
	syncZones()
	
	dynamicPage(name: "secondPage", title: "ALMOST DONE!", nextPage: "thirdPage") {
		
		section("Step #2") {
			image "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=1913&y=543&a=true&file=which.png&t=rfdicsYtGtRwy0h&scalingup=0"
			paragraph "By the time you're done reading this message, your Legrand Hub should have been scanned. Go ahead and click next."
			paragraph "If you don't see any available Zones on the next page, come back, wait a few seconds and click next again."
			href(name: "Setup", title: "Next", required: false, page: "thirdPage", description: "Complete setup!")
		}
		
	}

}

// ========================================================
// PAGES - THIRD - PAGE
// ========================================================

def thirdPage() {
	
	def l = "Zones"
	
	if (!state.zones) state.zones = []
	
	def options = []
    
	if (state.zones) {
		for (int i = 0; i < state.zones.size(); i++) {
			if (state.zones[i].ip == ip) {
			
				def map = [:]
				def A = state.zones[i].id
				
				map[A] = state.zones[i].name
				options << map
				
			}
		}
	}
	
	if (options.size() == 0) {
		state.remove("zones")
		updateDevices();
	}
	
	if (options.size() == 1) l = "Zone"

	dynamicPage(name: "thirdPage", title: "${options.size()} ${l}", install: true) {
	
		// NO ZONES FOUND
		if (options.size() == 0) {
			
			section("YIKES!") {
				image "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=1913&y=543&a=true&file=error.png&t=X2myVKHBZTcjgNg&scalingup=0"
				paragraph "No Zones found! Visit the Web Control while on the same network and make sure you can see your Zones from there.";
				href(name: "Restart", title: "Start Over", required: false, page: "firstPage", description: "Rescan and scout for zones.")
				href(name: "Rescan", title: "Rescan", required: false, page: "secondPage", description: "Rescan and scout for zones.")
			}
			
			return
			
		}
		
		section("Sweet! ${state.zones.size()} ${l} synced into your things.\nYou're all done!") {
			image "https://cloud.wimzel.com/index.php/apps/files_sharing/ajax/publicpreview.php?x=1913&y=543&a=true&file=sweet.png&t=txdOW5Ep3SgoPVx&scalingup=0&cache=123"
		}
		
		section("Learn More") {
			href(name: "Blog", title: "FAQs", required: false, url: "https://blog.wimzel.com/developments/living-legrand/faq/", description: "Just incase you need help.")
		}
	
	}
    
}

// ========================================================
// INSTALLED
// ========================================================

def installed() {
	
	log.debug("Installed with settings: ${settings}")
	
	initialize()
	
	runEvery3Hours("updateDevices")
	
}

// ========================================================
// UNINSTALLED
// ========================================================

def uninstalled() {
	
	log.info("Uninstalling, removing child devices...")
	
	unschedule("updateDevices")
	
	removeChildDevices(getChildDevices())
	
}


// ========================================================
// UPDATED
// ========================================================

def updated() {
	
	log.debug "Updated with settings: ${settings}"
	
	unsubscribe()
	initialize()
	
}

// ========================================================
// INITIALIZE
// ========================================================

def initialize() {
	
	// TODO: subscribe to attributes, devices, locations, etc.
	
	startSubscriptions()
	updateDevices()
	
}

// ========================================================
// START SUBSCRIPTIONS
// ========================================================

def startSubscriptions() {

	unsubscribe()
	
	log.info("***** SUBSRIBED TO LOCATION HANDLER")
	
	subscribe(location, null, locationHandler, [filterEvents:false])

}

// ========================================================
// STORE
// ========================================================

def store(data) {
	
	// SETUP ZONES
	state.zones = [
		[
			name: "Legrand Master",
			id: "9999",
			ip: ip,
			product: "zone"
		]
	]
	
	def zones = data.split(":")
	
	if (!zones.length) return;
    
	// PARSE AND STORE ZONES
	for (int i = 0; i < zones.length; i++) {
		if (zones[i].toInteger() > 0 && zones[i].toInteger() < 4096) {
			state.zones << [
				name: "Legrand Zone ${zones[i]}",
				id: zones[i],
				ip: ip,
				product: "zone"
			]
		} 
	}
	
	log.debug("Storing Zones: ${state.zones}")
	
}

// ========================================================
// LOG - RESPONSE
// ========================================================

def logResponse(response) {
	log.info("Status: ${response.status}")
	log.info("Body: ${response.data}")
}

// ========================================================
// LOG - ERRORS
// ========================================================

// API Requests
// logObject is because log doesn't work if this method is being called from a Device

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		
		options.logObject.error("got error: ${e}, body: ${e.getResponse().getData()}")
		
		if (e.statusCode == 401) { // token is expired
			options.logObject.warn "Access token is not valid"
		}
		
		return options.errorReturn
		
	} catch (java.net.SocketTimeoutException e) {
		options.logObject.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}

// ========================================================
// LOCATION HANDLER
// ========================================================

def locationHandler(evt) {
	
	def result = []
	def msg = parseLanMessage(evt.description)
	def hub = evt?.hubId
	
	def headersAsString = msg.header // => headers as a string
	def headerMap = msg.headers      // => headers as a Map
	def body = msg.body              // => request body as a string
	def status = msg.status          // => http status code of the response
	def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
	def xml = msg.xml                // => any XML included in response body, as a document tree structure
	def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)
	
	if (!headersAsString || !headersAsString?.contains("savannah.nongnu.org")) return;
	
	log.warn("\n${msg}")
	
	store(body)
	
}

// ========================================================
// API - URI
// ========================================================

def apiURL(path = "/") {
	return "http://${ip}${path}"
}

// ========================================================
// API - HEADERS
// ========================================================

Map apiRequestHeaders() {
	
	log.warn("API Request: ${ip}")
	
	def nid = makeNetworkId(ip, 80)
	
	return [
		"HOST": nid,
		"Accept": "*/*",
		"User-Agent": "SmartThings Integration"
	]
    
}

// ========================================================
// LEGRAND - GET - ZONES
// ========================================================

def syncZones() {
		
	def httpRequest = [
		method: "GET",
		path: "/get_zones_list.cgi",
		headers: apiRequestHeaders()
	]
	
	def hubAction = new physicalgraph.device.HubAction(httpRequest)
	
	sendHubCommand(hubAction)
	
	log.warn("\n${hubAction}")
    
}

// ========================================================
// API - GET
// ========================================================

def apiGET(path) {
	try {
		
		def httpRequest = [
			method: "GET",
			path: path,
			headers: apiRequestHeaders()
		]
		
		def hubAction = new physicalgraph.device.HubAction(httpRequest)
		
		sendHubCommand(hubAction)
	
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

// ========================================================
// DEVICES LIST
// ========================================================

def devicesList(selector = "") {

	return state.zones	
	
}

// ========================================================
// LOCATION OPTIONS
// ========================================================

Map locationOptions() {

	def options = [:]
	def devices = devicesList()
	
	devices.each { device ->
		options[device.location.id] = device.location.name
	}
	
	log.debug("Locations: ${options}")
	
	return options
    
}

// ========================================================
// DEVICES IN LOCATION
// ========================================================

def devicesInLocation() {
	return devicesList("location_id:${settings.selectedLocationId}")
}

// ========================================================
// REMOVE - CHILD - DEVICES
// ========================================================

private removeChildDevices(devices) {
	devices.each {
		deleteChildDevice(it.deviceNetworkId) // 'it' is default
	}
}

// ========================================================
// UPDATE - DEVICES
// ========================================================

// ensures the devices list is up to date

def updateDevices() {

	if (!state.devices) state.devices = [:]
	
	def selectors = []
	def devices = devicesInLocation()
	
	log.warn("Active Legran Zones: ${devices}")
	
	devices.each { device ->
	
		def childDevice = getChildDevice(device.id)
		
		log.debug("Child Devices: ${childDevice}")
		
		selectors.add("${device.id}")
	
		if (!childDevice) {
			
			log.info("Adding Device: ID: ${device.id} Product: ${device.product}")
			
			def data = [
				label: device.name,
				level: Math.round((device.brightness ?: 1) * 100),
				switch: device.connected ? device.power : "unreachable"
			]
			
			childDevice = addChildDevice(app.namespace, "Living Legrand Zones", device.id, null, data)
		
		}
	
	}
	
	getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
		log.info("Deleting ${it.deviceNetworkId}")
		deleteChildDevice(it.deviceNetworkId)
	}
	
	// Asynchronously refresh devices so we don't block    
	runIn(1, "refreshDevices")
    
}

// ========================================================
// REFRESH DEVICES
// ========================================================

def refreshDevices() {

	log.info("Refreshing all devices...")
	
	getChildDevices().each { device ->
		device.refresh()
	}
    
}

// ========================================================
// MAKE NETWORK ID
// ========================================================

private String makeNetworkId(ipaddr, port) {
	
	try {
	
		String hexIp = ipaddr.tokenize(".").collect { 
			String.format("%02X", it.toInteger()) 
		}.join()
		
		String hexPort = String.format("%04X", port)
		
		log.debug("${hexIp}:${hexPort}")
		
		return "${hexIp}:${hexPort}"
	
	} catch(err) {
	
		return "${ipaddr}:${port}"
	
	}
     
}

// ========================================================
// TO QUERY STRING
// ========================================================

String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}