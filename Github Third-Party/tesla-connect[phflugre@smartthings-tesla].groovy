/**
 *  Tesla Service Manager
 *
 *  Author: Schwark Satyavolu
 *  includes code for haversine formula from https://github.com/acmeism/RosettaCodeData
 *
 */

import groovy.json.JsonSlurper

definition(
    name: "Tesla",
    namespace: "schwark",
    author: "Schwark Satyavolu",
    description: "Allows you to connect your Tesla cars with SmartThings and control them from your Things area or Dashboard in the SmartThings Mobile app.",
    category: "SmartThings Labs",
    iconUrl: "http://brandchannel.com/wp-content/uploads/2016/10/tesla-logo-500.jpg",
    iconX2Url: "http://brandchannel.com/wp-content/uploads/2016/10/tesla-logo-500.jpg",
    singleInstance: true
)

preferences {
	input("username", "string", title:"Username", description: "Please enter your Tesla username", required: true, displayDuringSetup: true)
	input("password", "password", title:"Password", description: "Please enter your Tesla password", required: true, displayDuringSetup: true)
}

/////////////////////////////////////
def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def uninstalled() {
	log.debug("Uninstalling with settings: ${settings}")
	unschedule()

	removeChildDevices(getChildDevices())
}

/////////////////////////////////////
def updated() {
	//log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

/////////////////////////////////////
def initialize() {
	// remove location subscription aftwards
	unsubscribe()
	state.subscribe = false

	if(!state.token) {
		navigateUrl(getRecipe(id, 'token'))
	}
	runIn(60*5, doDeviceSync)
}

def getHubId() {
	return state.hubId ? state.hubId : location.hubs[0].id
}

/////////////////////////////////////
def locationHandler(evt) {
	log.debug "$locationHandler(evt.description)"
	def description = evt.description
	def hub = evt?.hubId
	state.hubId = hub
	log.debug("location handler: event description is ${description}")
}

/////////////////////////////////////
private def parseEventMessage(Map event) {
	//handles gateway attribute events
	return event
}

private def parseEventMessage(String description) {
}


private def evalJsonPath(obj, String path) {
	def json = obj
	//log.debug("json source obj is : ${json}")
	path.split("\\.").each { 
		//log.debug("json obj part is : ${json} and path part is : ${it} and result is ${json[it]}")
		json = json ? json[it] : null
	}
	return json
}

private def toQueryString(Map m)
{
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

private def haversine(lat1, lon1, lat2, lon2) {
  def R = 6372.8
  // In kilometers
  def dLat = Math.toRadians(lat2 - lat1)
  def dLon = Math.toRadians(lon2 - lon1)
  lat1 = Math.toRadians(lat1)
  lat2 = Math.toRadians(lat2)

  def a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
  def c = 2 * Math.asin(Math.sqrt(a))
  R * c
}

def setState(response, browserSession) {
	log.debug ("setting token to ${browserSession.state.token}")
	state.token = browserSession.state.token
}

private def getRecipe(id, command) {
	def domain = "https://owner-api.teslamotors.com"
	def STEPS = [
			'secret': [name: 'secret', uri: 'http://pastebin.com/raw/YiLPDggh', state: [client_id: 'json:OWNERAPI_CLIENT_ID', client_secret: 'json:OWNERAPI_CLIENT_SECRET'], force_type: 'json', prefix: "{", suffix:'"dummy":"dummy"}'],
			'auth': [name: 'auth', uri: "${domain}/oauth/token", variables: [grant_type: 'password', client_id: null, client_secret: null, email: "${settings.username}", password: "${settings.password}"], state: ['token': 'json:access_token'], processor: this.&setState,  method: "post"],	
			'vehicles': [name: 'vehicles', uri: "${domain}/api/1/vehicles", headers: ['Authorization': "Bearer ${token}"]],	
			]
	def RECIPES = [
				'token': ['secret', 'auth']
			]
	if(command) {
		if(RECIPES[command]) {
			def recipe = []
			RECIPES[command].each { recipe.add(0, STEPS[it]) }
			return recipe
		}
		if(STEPS[command]) return STEPS[command]
	} 
}

def runCommand(command, id=null, browserSession=[:]) {
	if(!browserSession.state || !browserSession.state.token) {
		if(!browserSession.state) browserSession.state = [:]
		browserSession.state.token = state.token
	}
	navigateUrl(getRecipe(id, command), browserSession)
}

private def getPatternValue(html, browserSession, kind, variable, pattern=null) {
	if(!pattern) {
        pattern = /(?ms)name="${variable}".*?value="([^"]*)"/
    }
	log.debug("looking for values with pattern ${pattern} for ${variable}")
	def value = null
	if(html) {
		if(!browserSession[kind]) browserSession[kind] = [:]
		def group = (html =~ pattern)
		if(group) {
			log.debug "found variable value ${group[0][1]} for ${variable}"
			value = group[0][1]
			browserSession[kind][variable] = value
		}
	}
	return value
}

private def visitNodes(node, processor) {
	def stack = [node]
	
    def current = null
	while(stack && (current = stack.pop())) {
		if(processor) {
			if(processor(current)) return
		}
        if(current instanceof groovy.util.slurpersupport.Node) {
			current.children().each {
				stack.add(0,it)
			}
        }
	}
}

private def getIdAttr(html, id, attr) {
	def result = null
	if(html && id && attr) {
	   visitNodes(html[0]) {
	    	if(it instanceof groovy.util.slurpersupport.Node) {
		        def attributes = it.attributes()
		        if(attributes && attributes['id'] == id) {
		        	if('#text' == attr) 
		        		result = it.text()
		        	else {
		        		result = attributes ? attributes[attr] : null
		        	}
	        	}
	        }
	        return result
	    }
	}
    return result
}

private def getParsedResponse(params, response, browserSession) {
	def html = response.data
	if(response.contentType == 'text/plain' && params.force_type) {
		html = "${params.prefix}${html}${params.suffix}"
		if(params.force_type == 'json') {
			html = new JsonSlurper().parseText(html)
		}
	}
	return html
} 

private def extractSession(params, response, browserSession) {
	//log.debug("extracting session variables..")
	def count = 1
	def html = getParsedResponse(params, response, browserSession)
    
    if(params.state) {
    	params.state.each { name, pattern ->
    		if(pattern instanceof java.util.regex.Pattern) {
		    	getPatternValue(html, browserSession, 'state', name, pattern)
    		} else if(pattern instanceof String) {
    			if(pattern.startsWith('json:')) {
    				browserSession.state[name] = evalJsonPath(html, (pattern - 'json:'))
    			} else {
		    		def parts = pattern.tokenize('.')
		    		browserSession.state[name] = parts.size() == 2 ? getIdAttr(html, parts[0], parts[1]) : null
		    	}
		    } 
    	}
	}

    browserSession.vars.each() { n, v ->
    	browserSession.vars[n] = ''
    }
        
    visitNodes(html[0]) {
    	if(it instanceof groovy.util.slurpersupport.Node && it.name == 'INPUT') {
        	def attr = it.attributes()
            if(attr) {
            	def name = attr['name']
                if(name) {
					def value = attr['value'] ? attr['value'] : ''
					if(browserSession.vars.containsKey(name)) {
						browserSession.vars[name] = value
						//log.debug "found form value ${value} for ${name}"
					}
				}
            }
        }
        return false
    }
    
	return browserSession
}

private def fillTemplate(template, map) {
	if(!map) return template
	def result = template.replaceAll(/\$\{(\w+)\}/) { k -> map[k[1]] ?: k[0] }
	return result
}

private def navigateUrl(recipe, browserSession=[:]) {
    def params = recipe.pop()

	def success = { response ->
    	log.trace("response status is ${response.status}")

    	browserSession.cookies = !browserSession.get('cookies') ? [] : browserSession.cookies
    	response.headerIterator('Set-Cookie').each {
    		log.debug "adding cookie to request: ${it}"
      		browserSession.cookies.add(it.value.split(';')[0])
    	}

    	if(response.status == 200) {
			extractSession(params, response, browserSession)
	    	if(params.processor) params.processor(response, browserSession)
	    	if(params.expect) {
	    		log.debug((response.data =~ params.expect) ? "${params.name} is successful" : "${params.name} has failed")
	    	}
	    } else if(response.status == 302) {
	    	response.headerIterator('Location').each {
    			def location = params.uri.toURI().resolve(it.value).toString()
    			log.debug "redirecting on 302 to: ${location}"
    			recipe.push(['name': "${params.name} redirect", 'uri': location, 'expect': params.expect, 'processor': params.processor, 'state': params.state])
    		}
	    }

		if(recipe) {
			if(!recipe[-1].referer) recipe[-1].referer = params.uri
			navigateUrl(recipe, browserSession)
		}

	    return browserSession
    }

	if(params.uri) {
		if(!browserSession.state) browserSession.state = [:]
		if(!browserSession.vars) browserSession.vars = [:]
		params.uri = fillTemplate(params.uri, browserSession.vars + browserSession.state)
        if(!params.headers) params.headers = [:]
		if(!params.headers['Origin']) params.headers['Host'] = params.uri.toURI().host
		params.headers['User-Agent'] = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36'
		params.headers['Accept'] = 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8'
		params.headers['Accept-Language'] = 'en-US,en;q=0.5'

		if(params.referer == 'self') params.referer = params.uri
		//if(params.referer) params.headers['Referer'] = params.referer
		if(browserSession.cookies) {
			params.headers['Cookie'] = browserSession.cookies.join(";")
		}
		if(browserSession.vars || browserSession.state) {
			params.variables = (params.variables ? params.variables : [:])
			params.variables.each {name, value ->
				if(browserSession && browserSession.vars && !value && browserSession.vars[name]) params.variables[name] = browserSession.vars[name]
				if(browserSession && browserSession.state && !value && browserSession.state[name]) params.variables[name] = browserSession.state[name]
			}
		}
		log.debug("navigating to ${params.uri} and method: ${params.method} and headers ${params.headers} and using params: ${params.variables} and state of ${browserSession.state}")
		try {
			if(params.method == 'post' && params.variables) {
				params.body = toQueryString(params.variables)
				httpPost(params, success)
			} else {
				if(params.variables) params.query = params.variables
	    		httpGet(params,success)
   			}
		} catch (e) {
    			log.error "something went wrong: $e"
		}
	}

	return browserSession
}

/////////////////////////////////////
def doDeviceSync(){
	log.debug "Doing Tesla Device Sync!"

	if(!state.subscribe) {
		subscribe(location, null, locationHandler, [filterEvents:false])
		state.subscribe = true
	}

	createSwitches()
}


////////////////////////////////////////////
//CHILD DEVICE METHODS
/////////////////////////////////////
def parse(childDevice, description) {
	def parsedEvent = parseEventMessage(description)

	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = new String(parsedEvent.headers.decodeBase64())
		def bodyString = new String(parsedEvent.body.decodeBase64())
		log.debug "parse() - ${bodyString}"
	} else {
		log.debug "parse - got something other than headers,body..."
		return []
	}
}

def getPrefix() {
	return "TESLA"
}

def createSwitches() {
	log.debug("Creating Tesla Switches...")

	def PREFIX = getPrefix()
	def CARS = state.cars
	if(!CARS) return

	// add missing devices
	CARS.each() { id, map ->
		def name = map['name']
		log.debug("processing switch ${id} with name ${name}")
		def hubId = getHubId()
		def device = getChildDevice("${PREFIX}${id}")
		if(map.button && !device) {
			def createSwitches = addChildDevice("schwark", "Tesla Car", "${PREFIX}${id}", hubId, ["name": "Tesla.${id}", "label": "${name}", "completedSetup": true])
			log.debug("created child device ${PREFIX}${id} with name ${name} and hub ${hubId}")
			carSwitch.setId(id)
		}
	}

	// remove disabled devices
	def children = getChildDevices()
	children.each {
		if(it && it.deviceNetworkId) {
			def id = it.deviceNetworkId
			if(id.startsWith(PREFIX)) {
				id = id - PREFIX
				def button = CARS[id]
				if(!button) deleteChildDevice(it.deviceNetworkId)
			}
		}
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private Boolean canInstallLabs()
{
	return hasAllHubsOver("000.011.00603")
}

private Boolean hasAllHubsOver(String desiredFirmware)
{
	return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

private List getRealHubFirmwareVersions()
{
	return location.hubs*.firmwareVersionString.findAll { it }
}

private removeChildDevices(data) {
    data.delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
