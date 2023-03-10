/**
 *  Quirky (Connect)
 *
 *  Author: todd@wackford.net
 *  Date: 2014-02-15
 *
 *  Update: 2014-02-22
 *			Added eggtray
 *			Added device specific methods called from poll (versus in poll)
 *
 *  Update2:2014-02-22
 *			Added nimbus
 *
 *  Update3:2014-02-26
 *			Improved eggtray integration
 *			Added notifications to hello home
 *			Introduced Quirky Eggtray specific icons (Thanks to Dane)
 *			Added an Egg Report that outputs to hello home.
 *			Switched to Dan Lieberman's client and secret
 *			Still not browser flow (next update?)
 *
 *  Update4:2014-03-08
 *			Added Browser Flow OAuth
 *
 *
 *  Update5:2014-03-14
 *			Added dynamic icon/tile updating to the nimbus. Changes the device icon from app.
 *
 *  Update6:2014-03-31
 *			Stubbed out creation and choice of nimbus, eggtray and porkfolio per request.
 *
 *  Update7:2014-04-01
 *          Renamed to 'Quirky (Connect)' and updated device names
 *
 *
 *  Update8:2014-04-08 (dlieberman)
 * 	    Stubbed out Spotter
 *
 *  Update9:2014-04-08 (twackford)
 * 	    resubscribe to events on each poll
 *
 *  Update10:2014-04-24 (twackford)
 * 	    fixed null battery and temperature errors in spotter
 *
 *  Update11:2014-04-26 (twackford)
 * 	    fixed multiple instances of same device getting installed
 *
 *  Update12:2014-05-20 (twackford)
 * 	    Added Aros
 *
 *  Update13:2014-06-05 (twackford)
 *		Multiple Aros bug fixes after initial QA
 *
 *  Update14:2014-09-22 (twackford)
 * 	    Added Refuel
 *
 *  Update14:2014-10-04 (twackford)
 * 	    Added enum of sub-child devices
 *		added renew subscription every 12 hours
 *		Added ability to delete from child device and reflect in parent
 *		Added delete of child if unselected in device list
 *		un-stubbed spotter, nimbus, eggtray and porkfolio
 *		Fixed page display of quirky and ST icons on connection
 *		Fixed deleting the last device from parent
 *		Fixed bad login handling
 *
 *  Update15:2014-11-29 (twackford)
 * 	    Added check for token expiration (wink just set to 6 weeks)
 */

import java.text.DecimalFormat
import groovy.json.JsonSlurper

// Wink API stuff
private apiUrl() 			{ "https://winkapi.quirky.com/" }
private getVendorName() 	{ "Quirky Wink" }
private getVendorAuthPath()	{ "https://winkapi.quirky.com/oauth2/authorize?" }
private getVendorTokenPath(){ "https://winkapi.quirky.com/oauth2/token?" }
private getVendorIcon()		{ "https://s3.amazonaws.com/smartapp-icons/Partner/quirky@2x.png" }
private getClientId() 		{ "c22d82a7fc3d6faf06dcff1bcf0feb52" } // Dan Lieberman's
private getClientSecret() 	{ "bd44c524a1df9dce134235d174350603" }

private getServerUrl() 		{ "https://graph.api.smartthings.com" }


// Automatically generated. Make future change here.
definition(
    name: "Quirky (Connect)",
    namespace: "wackford",
    author: "Todd Wackford",
    description: "Connect your Quirky to SmartThings.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/quirky.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/quirky@2x.png",
    oauth: true
)

mappings {
	path("/receivedToken") 			{ action:[ POST: "receivedToken", 				GET: "receivedToken"] }
	path("/receiveToken") 			{ action:[ POST: "receiveToken", 				GET: "receiveToken"] }
    path("/propaneTankCallback")	{ action:[ POST: "propaneTankEventHandler",		GET: "subscriberIdentifyVerification"]}
    path("/airConditionerCallback")	{ action:[ POST: "airConditionerEventHandler",	GET: "subscriberIdentifyVerification"]}
	path("/powerstripCallback")		{ action:[ POST: "powerstripEventHandler",		GET: "subscriberIdentifyVerification"]}
	path("/sensor_podCallback") 	{ action:[ POST: "sensor_podEventHandler",		GET: "subscriberIdentifyVerification"]}
	path("/piggy_bankCallback") 	{ action:[ POST: "piggy_bankEventHandler",		GET: "subscriberIdentifyVerification"]}
	path("/eggtrayCallback") 		{ action:[ POST: "eggtrayEventHandler",			GET: "subscriberIdentifyVerification"]}
	path("/cloud_clockCallback")	{ action:[ POST: "cloud_clockEventHandler", 	GET: "subscriberIdentifyVerification"]}
}

preferences {
    page(name: "Credentials", title: "Fetch OAuth2 Credentials", content: "authPage", install: false)
	page(name: "listDevices", title: "Quirky Devices", content: "listDevices", install: false)    
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule("5 0,12 * * * ?", updateWinkSubscriptions) //renew subscriptions every 12 hours
	listDevices()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    
    unschedule()
	schedule("5 0,12 * * * ?", updateWinkSubscriptions) //renew subscriptions every 12 hours
	initialize()

	listDevices()
}

def listDevices()
{
	log.debug "In listDevices"

	def devices = getDeviceList()
	log.debug "Device List = ${devices}"
    log.debug "Settings List = ${settings}"

	dynamicPage(name: "listDevices", title: "Choose devices", install: true) {
		section("Devices") {
			input "devices", "enum", title: "Select Device(s)", required: false, multiple: true, options: devices
		}
	}
}

def getDeviceList()
{
	log.debug "In getDeviceList"

	def deviceList = [:]
	state.deviceDataArr = []

	apiGet("/users/me/wink_devices") { response ->
		response.data.data.each() {

			if ( it.propane_tank_id ) {
				deviceList["${it.propane_tank_id}"] = it.name
				state.deviceDataArr.push(['name'    : it.name,
										  'id'      : it.propane_tank_id,
										  'type'    : "propane_tank",
										  'serial'  : it.serial,
										  'data'    : it,
										  'subsSuff': "/propaneTankCallback",
										  'subsPath': "/propane_tanks/${it.propane_tank_id}/subscriptions"
				])
			}
            
			if ( it.air_conditioner_id ) {
				deviceList["${it.air_conditioner_id}"] = it.name
				state.deviceDataArr.push(['name'       : it.name,
										     'id'      : it.air_conditioner_id,
										     'type'    : "air_conditioner",
										     'serial'  : it.serial,
										     'data'    : it,
										     'subsSuff': "/airConditionerCallback",
										     'subsPath': "/air_conditioners/${it.air_conditioner_id}/subscriptions"
				])
			}
                                
			if ( it.cloud_clock_id ) {
            	//log.debug "${it.dials[0]}"
                def dial1Data = it.dials[0]
                deviceList[dial1Data.dial_id] =     it.name + " Dial 1"
                state.deviceDataArr.push(['name'  : it.name + " Dial 1",
                						'id'      : dial1Data.dial_id,
										'type'    : "nimbusDial",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/cloud_clockCallback",
										'subsPath': "/cloud_clocks/${it.cloud_clock_id}/subscriptions"
				])
                def dial2Data = it.dials[1]
                deviceList[dial2Data.dial_id] =     it.name + " Dial 2"
                state.deviceDataArr.push(['name'  : it.name + " Dial 2",
                						'id'      : dial2Data.dial_id,
										'type'    : "nimbusDial",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/cloud_clockCallback",
										'subsPath': "/cloud_clocks/${it.cloud_clock_id}/subscriptions"
				])
                def dial3Data = it.dials[2]
                deviceList[dial3Data.dial_id] =     it.name + " Dial 3"
                state.deviceDataArr.push(['name'  : it.name + " Dial 3",
                						'id'      : dial3Data.dial_id,
										'type'    : "nimbusDial",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/cloud_clockCallback",
										'subsPath': "/cloud_clocks/${it.cloud_clock_id}/subscriptions"
				])
                def dial4Data = it.dials[3]
                deviceList[dial4Data.dial_id] =     it.name + " Dial 4"
                state.deviceDataArr.push(['name'  : it.name + " Dial 4",
                						'id'      : dial4Data.dial_id,
										'type'    : "nimbusDial",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/cloud_clockCallback",
										'subsPath': "/cloud_clocks/${it.cloud_clock_id}/subscriptions"
				])
                
                //dials are handled individually now, commented out
				/*deviceList["${it.cloud_clock_id}"] = it.name
				state.deviceDataArr.push(['name'   : it.name,
										  'id'     : it.cloud_clock_id,
										  'type'   : "cloud_clock",
										  'serial' : it.serial,
										  'data'   : it,
										  'subsSuff': "/cloud_clockCallback",
										  'subsPath': "/cloud_clocks/${it.cloud_clock_id}/subscriptions"
				])*/
			} 
            
			if ( it.powerstrip_id ) {
            	def outlet1Data = it.outlets[0]
                deviceList[outlet1Data.outlet_id] = it.name + " " + outlet1Data.name
                state.deviceDataArr.push(['name'  : it.name + " " + outlet1Data.name,
										'id'      : outlet1Data.outlet_id,
										'type'    : "powerstripOutlet",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/powerstripCallback",
										'subsPath': "/powerstrips/${it.powerstrip_id}/subscriptions"
				])
                
                def outlet2Data = it.outlets[1]
                deviceList[outlet2Data.outlet_id] = it.name + " " + outlet2Data.name
                state.deviceDataArr.push(['name'  : it.name + " " + outlet2Data.name,
										'id'      : outlet2Data.outlet_id,
										'type'    : "powerstripOutlet",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/powerstripCallback",
										'subsPath': "/powerstrips/${it.powerstrip_id}/subscriptions"
				])
                
                //this is commented out cuz we install and handle outlets individualy
                /* 
				deviceList["${it.powerstrip_id}"] = it.name
				state.deviceDataArr.push(['name'  : it.name,
										'id'      : it.powerstrip_id,
										'type'    : "powerstrip",
										'serial'  : it.serial,
										'data'    : it,
										'subsSuff': "/powerstripCallback",
										'subsPath': "/powerstrips/${it.powerstrip_id}/subscriptions"
				])*/
			}
			
			if ( it.sensor_pod_id ) {
				deviceList["${it.sensor_pod_id}"] = it.name
				state.deviceDataArr.push(['name'  : it.name,
										 'id'     : it.sensor_pod_id,
										 'type'   : "sensor_pod",
										 'serial' : it.serial,
										 'data'   : it,
										 'subsSuff': "/sensor_podCallback",
										 'subsPath': "/sensor_pods/${it.sensor_pod_id}/subscriptions"
				])
			} 
                    
			
			if ( it.piggy_bank_id ) {
				deviceList["${it.piggy_bank_id}"]   = it.name
				state.deviceDataArr.push(['name'    : it.name,
										  'id'      : it.piggy_bank_id,
										  'type'    : "piggy_bank",
										  'serial'  : it.serial,
										  'data'    : it,
										  'subsSuff': "/piggy_bankCallback",
										  'subsPath': "/piggy_banks/${it.piggy_bank_id}/subscriptions"
				])
			} 
                
			if ( it.eggtray_id ) {
				deviceList["${it.eggtray_id}"]      = it.name
				state.deviceDataArr.push(['name'    : it.name,
										  'id'      : it.eggtray_id,
										  'type'    : "eggtray",
										  'serial'  : it.serial,
										  'data'    : it,
										  'subsSuff': "/eggtrayCallback",
										  'subsPath': "/eggtrays/${it.eggtray_id}/subscriptions"
				])
			}
		}
	}

  	return deviceList
}

def initialize()
{
	log.debug "Initialized with settings: ${settings}"

	settings.devices.each {
		def deviceId = it

		state.deviceDataArr.each {
			if ( it.id == deviceId ) {
				switch(it.type) {
                
                	case "propane_tank":
						log.debug "we have a Refuel"
						createChildDevice("Quirky Refuel", deviceId, it.name, it.label)
						createWinkSubscription( it.subsPath, it.subsSuff )
                        pollPropaneTank(getChildDevice(deviceId))
						break
                        
                	case "air_conditioner":
						log.debug "we have an Aros"
						createChildDevice("Quirky Aros", deviceId, it.name, it.label)
						createWinkSubscription( it.subsPath, it.subsSuff )
                        pollAros(getChildDevice(deviceId))
						break

					case "powerstrip":
						log.debug "we have a Pivot Power Genius"
						createPowerstripChildren(it.data) //has sub-devices, so we call out to create kids
						createWinkSubscription( it.subsPath, it.subsSuff )
						break
                        
					case "powerstripOutlet":
						log.debug "we have a Pivot Power Genius Outlet"
						createChildDevice( "Quirky Pivot Power Genius", deviceId, it.name, it.name )
						createWinkSubscription( it.subsPath, it.subsSuff )
                        pollOutlet(getChildDevice(deviceId))
						break

					case "nimbusDial":
						log.debug "we have a Nimbus Dial"
						//createNimbusChildren(it.data) //has sub-devices, so we call out to create kids
                        createChildDevice( "Quirky Nimbus", deviceId, it.name, it.name )
						createWinkSubscription( it.subsPath, it.subsSuff )
                        pollNimbus(getChildDevice(deviceId))
						break
         
					case "cloud_clock":
						log.debug "we have a Nimbus"
						createNimbusChildren(it.data) //has sub-devices, so we call out to create kids
						createWinkSubscription( it.subsPath, it.subsSuff )
                        pollNimbus(getChildDevice(deviceId))
						break
                        
					case "sensor_pod":
						log.debug "we have a Spotter"
						createChildDevice("Quirky Spotter", deviceId, it.name, it.label)
                        createWinkSubscription( it.subsPath, it.subsSuff )
                        getSensorPodUpdate(getChildDevice(deviceId))
						break
                        
					case "piggy_bank":
						log.debug "we have a Piggy Bank"
						createChildDevice("Quirky Porkfolio", deviceId, it.name, it.label)
                        createWinkSubscription( it.subsPath, it.subsSuff )
                        getPiggyBankUpdate(getChildDevice(deviceId))
						break

					case "eggtray":
						log.debug "we have an Egg Minder"
						createChildDevice("Quirky Eggtray", deviceId, it.name, it.label)
                        createWinkSubscription( it.subsPath, it.subsSuff )
                        getEggtrayUpdate(getChildDevice(deviceId))
						break                     
				}
			}                       
		}
	}
        
    // Delete any that are no longer in settings
	def delete = getChildDevices().findAll { !settings.devices?.contains(it.deviceNetworkId) }
    log.debug "deleting ${delete}"
    delete.each() {
        uninstallChildDevice(it)
		deleteChildDevice(it.deviceNetworkId)
    }
}

def createChildDevice(deviceFile, dni, name, label)
{
	log.debug "In createChildDevice"
    
	try {
		def existingDevice = getChildDevice(dni)
        log.debug "existingDevice = ${existingDevice}"
        
		if(!existingDevice) {
			log.debug "Creating child"
			def childDevice = addChildDevice(app.namespace, deviceFile, dni, null, [name: name, label: label, completedSetup: true])
		} else {
			log.debug "Device $dni already exists"
		}
	} 
    catch (e) {
		log.error "Error creating device: ${e}"
	}
}

def createWinkSubscription(path, suffix)
{
	log.debug "In createWinkSubscription"

	def callbackUrl = buildCallbackUrl(suffix)

	httpPostJson([
		uri : apiUrl(),
		path: path,
		body: ['callback': callbackUrl],
		headers : ['Authorization' : 'Bearer ' + state.vendorAccessToken]
	],)
	{ 	response ->
		log.debug "Created subscription ID ${response.data.data.subscription_id}"
	}
}

def subscriberIdentifyVerification()
{
	log.debug "In subscriberIdentifyVerification"

	def challengeToken = params.hub.challenge

	render contentType: 'text/plain', data: challengeToken
}

def uninstalled()
{
	log.debug "In uninstalled"

	removeWinkSubscriptions()

	removeChildDevices(getChildDevices())
    
    unschedule()
}

def removeWinkSubscriptions()
{
	log.debug "In removeWinkSubscriptions"

	state.deviceDataArr.each() {
		if (it.subsPath) {
			def path = it.subsPath
			apiGet(it.subsPath) { response ->
				response.data.data.each {
					if ( it.subscription_id ) {
                    	log.debug "Deleting Subscription: ${path}" + "/" + "${it.subscription_id}"
						deleteWinkSubscription(path + "/", it.subscription_id)
					}
				}
			}
		}
	}
}

private removeChildDevices(delete)
{
	log.debug "In removeChildDevices"
	log.debug "deleting ${delete.size()} devices"

	delete.each {
		deleteChildDevice(it.deviceNetworkId)
	}
}

def uninstallChildDevice(childDevice) 
{
	log.debug "in uninstallChildDevice"
    
    // Remove the childs subscription
    def deviceData = state.deviceDataArr.findAll { it.id == childDevice.device.deviceNetworkId } 	
    deviceData.each() {
        def path = it.subsPath
        apiGet(it.subsPath) { response ->
			response.data.data.each {
				if ( it.subscription_id ) {
					deleteWinkSubscription(path + "/", it.subscription_id)
				}
			}
		}    
    }

    //now remove the child from settings. Unselects from list of devices, not delete
    log.debug "Settings size = ${settings['devices']}"
    
    if (!settings['devices']) //empty list, bail
    	return
    
    def newDeviceList = settings['devices'] - childDevice.device.deviceNetworkId
    app.updateSetting("devices", newDeviceList)
}

def updateWinkSubscriptions()
{	//since we don't know when wink subscription dies, call this from a scheduled job
	log.debug "In updateWinkSubscriptions"

	state.deviceDataArr.each() {
		if (it.subsPath) {
			def path = it.subsPath
            def suffix = it.subsSuff
			apiGet(it.subsPath) { response ->
				response.data.data.each {
					if ( it.subscription_id ) {
						deleteWinkSubscription(path + "/", it.subscription_id)
                        createWinkSubscription(path, suffix)
					}
				}
			}
		}
	}
}

def deleteWinkSubscription(path, subscriptionId)
{
	httpDelete([
		uri : apiUrl(),
		path: path + subscriptionId,
		headers : [ 'Authorization' : 'Bearer ' + state.vendorAccessToken ]
	],)
	{	response ->
		log.debug "Subscription ${subscriptionId} deleted"
	}
}

def buildCallbackUrl(suffix)
{
	log.debug "In buildRedirectUrl"

	def serverUrl = getServerUrl()
	return serverUrl + "/api/token/${state.accessToken}/smartapps/installations/${app.id}" + suffix
}

def checkToken() {
	log.debug "In checkToken"
    
    def tokenStatus = "bad"
    
    //check existing token by calling user device list
    try {
    	httpGet([ uri		: apiUrl(),
    		  	  path 		: "/users/me/wink_devices",
               	  headers 	: [ 'Authorization' : 'Bearer ' + state.vendorAccessToken ]
		])
		{ response ->
        	debugOut "Response is: ${response.status}"
            if ( response.status == 200 ) {
            	debugOut "The current token is good"
                tokenStatus = "good"
            }
		}
	}
    catch(Exception e) {
    	debugOut "Current access token did not work. Trying refresh Token now."
    }

	if ( tokenStatus == "bad" ) {
        //Let's try the refresh token now
    	debugOut "Trying to refresh tokens"
        
    	def tokenParams = [ client_id		: getClientId(),
        					client_secret	: getClientSecret(),
							grant_type		: "refresh_token",
							refresh_token	: state.vendorRefreshToken ]

		def tokenUrl = getVendorTokenPath() + toQueryString(tokenParams)
        
		def params = [
			uri: tokenUrl,
		]
        
    	try {
			httpPost(params) { response ->
            	debugOut "Successfully refreshed tokens with code: ${response.status}"
            	state.vendorRefreshToken = response.data.refresh_token
            	state.vendorAccessToken = response.data.access_token
            	tokenStatus = "good"
        	}
		}
    	catch(Exception e) {
    		debugOut "Unable to refresh token. Error ${e}"
    	}
	}
    
    if ( tokenStatus == "bad" ) {
    	return "Error: Unable to refresh Token"
    } else {
    	return null //no errors
    }
}

def apiGet(String path, Closure callback)
{
	log.debug "In apiGet with path: $path"
    
    //check to see if our token has expired
    def status = checkToken()
    debugOut "Status of checktoken: ${status}"
    
    if ( status ) {
    	debugOut "Error! Status: ${status}"
        return
    } else {
    	debugOut "Token is good. Call the command"
    }
    
	httpGet([
		uri : apiUrl(),
		path : path,
		headers : [ 'Authorization' : 'Bearer ' + state.vendorAccessToken ]
	])
	{
		response ->
			callback.call(response)
	}
}

def apiPut(String path, cmd, Closure callback)
{
	log.debug "In apiPut with path: $path and cmd: $cmd"
    
    //check to see if our token has expired
    def status = checkToken()
    debugOut "Status of checktoken: ${status}"
    
    if ( status ) {
    	debugOut "Error! Status: ${status}"
        return
    } else {
    	debugOut "Token is good. Call the command"
    }
    
	httpPutJson([
		uri : apiUrl(),
		path: path,
		body: cmd,
		headers : [ 'Authorization' : 'Bearer ' + state.vendorAccessToken ]
	])

	{
		response ->
			callback.call(response)
	}
}

def poll(childDevice)
{
	log.debug "In poll" //we should not ever get here, devices have unique polls
}

def cToF(temp) {
	return temp * 1.8 + 32
}

def fToC(temp) {
	return (temp - 32) / 1.8
}

def debugOut(msg) {

	log.debug msg
	//sendNotificationEvent(msg) //Uncomment this for troubleshooting only
}

def dollarize(int money)
{
	def value = money.toString()

	if ( value.length() == 1 )
		value = "00" + value

	if ( value.length() == 2 )
		value = "0" + value

	def newval = value.substring(0, value.length() - 2) + "." + value.substring(value.length()-2, value.length())
	value = newval

	def pattern = "\$0.00"
	def moneyform = new DecimalFormat(pattern)
	String output = moneyform.format(value.toBigDecimal())

	return output
}

def debugEvent(message, displayEvent) {

	def results = [
		name: "appdebug",
		descriptionText: message,
		displayed: displayEvent
	]
	log.debug "Generating AppDebug Event: ${results}"
	sendEvent (results)

}

String toQueryString(Map m) {
	return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

/////////////////////////////////////////////////////////////////////////
//	                 START REFUEL SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////
def pollPropaneTank(childDevice)
{
	log.debug "Polling Refuel ${childDevice.device.deviceNetworkId}"
    
    
	apiGet("/propane_tanks/" + childDevice.device.deviceNetworkId) { response ->
		def status = response.data.data.last_reading
        
        log.debug "Got tank data!"
        
        childDevice?.sendEvent(name:"battery", value:status.battery * 100, unit:"") 
        
        childDevice?.sendEvent(name:"tankLevel", value:status.remaining * 100, unit:"")
        
        //childDevice?.sendEvent(name:"tankChanged", value:new Date((status.tank_changed_at as long)*1000), unit:"")
	}
}

def propaneTankEventHandler()
{
	log.debug "In  propaneTankEventHandler..."

	def json = request.JSON
	def dni = getChildDevice(json.propane_tank_id)
    
	pollPropaneTank(dni)   //sometimes events are stale, poll for all latest states

	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}

/////////////////////////////////////////////////////////////////////////
//	                 START SENSOR POD SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////
def getSensorPodUpdate(childDevice)
{
	apiGet("/sensor_pods/" + childDevice.device.deviceNetworkId) { response ->
		def status = response.data.data.last_reading

		status.loudness ? childDevice?.sendEvent(name:"sound",value:"active",unit:"") :
			childDevice?.sendEvent(name:"sound",value:"inactive",unit:"")

		status.brightness ? childDevice?.sendEvent(name:"light",value:"active",unit:"") :
			childDevice?.sendEvent(name:"light",value:"inactive",unit:"")

		status.vibration ? childDevice?.sendEvent(name:"acceleration",value:"active",unit:"") :
			childDevice?.sendEvent(name:"acceleration",value:"inactive",unit:"")

		status.external_power ? childDevice?.sendEvent(name:"powerSource",value:"powered",unit:"") :
			childDevice?.sendEvent(name:"powerSource",value:"battery",unit:"")

		childDevice?.sendEvent(name:"humidity",value:status.humidity,unit:"")

		if (status.battery != null)
			childDevice?.sendEvent(name:"battery",value:(status.battery * 100).toInteger(),unit:"")
        else
        	childDevice?.sendEvent(name:"battery",value:0,unit:"")

		// Need to get users pref of temp scale here
        if ( status.temperature != null )
			childDevice?.sendEvent(name:"temperature",value:cToF(status.temperature),unit:"F")
	}
}

def sensor_podEventHandler()
{
	log.debug "In  sensor_podEventHandler..."

	def json = request.JSON

	def dni = getChildDevice(json.sensor_pod_id)

	log.debug "event received from ${dni}"

	getSensorPodUpdate(dni)   //sometimes events are stale, poll for all latest states

	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}

/////////////////////////////////////////////////////////////////////////
//	                 START NIMBUS SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////
def createNimbusChildren(deviceData)
{
	log.debug "In createNimbusChildren"

	def nimbusName = deviceData.name
	def deviceFile = "Quirky Nimbus"
	def index = 1
	deviceData.dials.each {
		log.debug "creating dial device for ${it.dial_id}"
		def dialName = "Dial ${index}"
		def dialLabel = "${nimbusName} ${dialName}"
		createChildDevice( deviceFile, it.dial_id, dialName, dialLabel )
		index++
	}
}

def cloud_clockEventHandler()
{
	log.debug "In Nimbus Event Handler..."

	def json = request.JSON
    
	def dials = json.dials

	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html

	if ( dials ) {
		dials.each() {
			def childDevice = getChildDevice(it.dial_id)
            
            if (!childDevice) // not a smartthings device, user did not pick it
            	return
                
			childDevice?.sendEvent( name : "dial", value : it.label , unit : "" )
			childDevice?.sendEvent( name : "info", value : it.name , unit : "" )
		}
	}
}

def pollNimbus(dni)
{

	log.debug "In pollNimbus using dni # ${dni}"

	def dials = null

	apiGet("/users/me/wink_devices") { response ->

		response.data.data.each() {
			if (it.cloud_clock_id  ) {
				log.debug "Found Nimbus #" + it.cloud_clock_id
				dials   = it.dials
				//log.debug dials
			}
		}
	}

	if ( dials ) {
		dials.each() {
			def childDevice = getChildDevice(it.dial_id)
            
            if (!childDevice) //this is not a SmartThings dial (user did not pick)
            	return
            
            //log.debug "Dial event ${childDevice}"
			childDevice?.sendEvent( name : "dial", value : it.label, unit : "" )
			childDevice?.sendEvent( name : "info", value : it.name , unit : "" )

			//Change the tile/icon to what info is being displayed
			switch(it.name) {
				case "Weather":
					childDevice?.setIcon("dial", "dial" ,  "st.quirky.nimbus.quirky-nimbus-weather")
					break
				case "Traffic":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-traffic")
					break
				case "Time":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-time")
					break
				case "Twitter":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-twitter")
					break
				case "Calendar":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-calendar")
					break
				case "Email":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-mail")
					break
				case "Facebook":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-facebook")
					break
				case "Instagram":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-instagram")
					break
				case "Fitbit":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-fitbit")
					break
				case "Egg Minder":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-egg")
					break
				case "Porkfolio":
					childDevice?.setIcon("dial", "dial",  "st.quirky.nimbus.quirky-nimbus-porkfolio")
					break
			}
			childDevice.save()
		}
	}
	return
}

/////////////////////////////////////////////////////////////////////////
//	                 START EGG TRAY SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////
def getEggtrayUpdate(childDevice)
{
	log.debug "In getEggtrayUpdate"

	apiGet("/eggtrays/" + childDevice.device.deviceNetworkId) { response ->

		def data = response.data.data
		def freshnessPeriod = data.freshness_period
		def trayName = data.name
		log.debug data

		int totalEggs = 0
		int oldEggs = 0

		def now = new Date()
		def nowUnixTime = now.getTime()/1000

		data.eggs.each() { it ->
			if (it != 0)
			{
				totalEggs++

				def eggArriveDate = it
				def eggStaleDate = eggArriveDate + freshnessPeriod
				if ( nowUnixTime > eggStaleDate ){
					oldEggs++
				}
			}
		}

		int freshEggs = totalEggs - oldEggs

		if ( oldEggs > 0 ) {
			childDevice?.sendEvent(name:"inventory",value:"haveBadEgg")
			def msg = "${trayName} says: "
			msg+= "Did you know that all it takes is one bad egg? "
			msg+= "And it looks like I found one.\n\n"
			msg+= "You should probably run an Egg Report before you use any eggs."
			sendNotificationEvent(msg)
		}
		if ( totalEggs == 0 ) {
			childDevice?.sendEvent(name:"inventory",value:"noEggs")
			sendNotificationEvent("${trayName} says:\n'Oh no, I'm out of eggs!'")
			sendNotificationEvent(msg)
		}
		if ( (freshEggs == totalEggs) && (totalEggs != 0) ) {
			childDevice?.sendEvent(name:"inventory",value:"goodEggs")
		}
		childDevice?.sendEvent( name : "totalEggs", value : totalEggs , unit : "" )
		childDevice?.sendEvent( name : "freshEggs", value : freshEggs , unit : "" )
		childDevice?.sendEvent( name : "oldEggs", value : oldEggs , unit : "" )
	}
}

def runEggReport(childDevice)
{
	apiGet("/eggtrays/" + childDevice.device.deviceNetworkId) { response ->

		def data = response.data.data
		def trayName = data.name
		def freshnessPeriod = data.freshness_period
		def now = new Date()
		def nowUnixTime = now.getTime()/1000

		def eggArray = []

		def i = 0

		data.eggs.each()  { it ->
			if (it != 0 ) {
				def eggArriveDate = it
				def eggStaleDate = eggArriveDate + freshnessPeriod
				if ( nowUnixTime > eggStaleDate ){
					eggArray.push("Bad  ")
				} else {
					eggArray.push("Good ")
				}
			} else {
				eggArray.push("Empty")
			}
			i++
		}

		def msg = " Egg Report for ${trayName}\n\n"
		msg+= "#7:${eggArray[6]}    #14:${eggArray[13]}\n"
		msg+= "#6:${eggArray[5]}    #13:${eggArray[12]}\n"
		msg+= "#5:${eggArray[4]}    #12:${eggArray[11]}\n"
		msg+= "#4:${eggArray[3]}    #11:${eggArray[10]}\n"
		msg+= "#3:${eggArray[2]}    #10:${eggArray[9]}\n"
		msg+= "#2:${eggArray[1]}      #9:${eggArray[8]}\n"
		msg+= "#1:${eggArray[0]}      #8:${eggArray[7]}\n"
		msg+= "                 +\n"
		msg+= "              ===\n"
		msg+= "              ==="

		sendNotificationEvent(msg)
	}
}

def eggtrayEventHandler()
{
	log.debug "In  eggtrayEventHandler..."

	def json = request.JSON
	def dni = getChildDevice(json.eggtray_id)

	log.debug "event received from ${dni}"

	poll(dni) //sometimes events are stale, poll for all latest states


	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}

/////////////////////////////////////////////////////////////////////////
//	                 START PIGGY BANK SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////
def getPiggyBankUpdate(childDevice)
{
	apiGet("/piggy_banks/" + childDevice.device.deviceNetworkId) { response ->
		def status = response.data.data
		def alertData = status.triggers

		if (( alertData.enabled ) && ( state.lastCheckTime )) {
			if ( alertData.triggered_at[0].toInteger() > state.lastCheckTime ) {
				childDevice?.sendEvent(name:"acceleration",value:"active",unit:"")
			} else {
				childDevice?.sendEvent(name:"acceleration",value:"inactive",unit:"")
			}
		}

		childDevice?.sendEvent(name:"goal",value:dollarize(status.savings_goal),unit:"")

		childDevice?.sendEvent(name:"balance",value:dollarize(status.balance),unit:"")

		def now = new Date()
		def longTime = now.getTime()/1000
		state.lastCheckTime = longTime.toInteger()
	}
}

def piggy_bankEventHandler()
{
	log.debug "In  piggy_bankEventHandler..."

	def json = request.JSON
	def dni = getChildDevice(json.piggy_bank_id)

	log.debug "event received from ${dni}"

	getPiggyBankUpdate(dni) //sometimes events are stale, poll for all latest states


	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}

/////////////////////////////////////////////////////////////////////////
//	                 START POWERSTRIP SPECIFIC CODE HERE
/////////////////////////////////////////////////////////////////////////

def powerstripEventHandler()
{
	log.debug "In Powerstrip Event Handler..."

	def json = request.JSON
	def outlets = json.outlets
    
    def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html

	outlets.each() {
		def dni = getChildDevice(it.outlet_id)
        
        if(!dni) //this is not a smartthings device cuz user did not pick it
        	return
            
		pollOutlet(dni)   //sometimes events are stale, poll for all latest states
	}
}

def pollOutlet(childDevice)
{
	log.debug "In pollOutlet"

	log.debug "Polling powerstrip"
	apiGet("/outlets/" + childDevice.device.deviceNetworkId) { response ->
		def data = response.data.data
		data.powered ? childDevice?.sendEvent(name:"switch",value:"on") :
			childDevice?.sendEvent(name:"switch",value:"off")
	}
}

def on(childDevice)
{
	apiPut("/outlets/" + childDevice.device.deviceNetworkId, [powered : true]) { response ->
		def data = response.data.data
		log.debug "Sending 'on' to device"
	}
}

def off(childDevice)
{
	apiPut("/outlets/" + childDevice.device.deviceNetworkId, [powered : false]) { response ->
		def data = response.data.data
		log.debug "Sending 'off' to device"
	}
}

def createPowerstripChildren(deviceData)
{
	log.debug "In createPowerstripChildren"

	def powerstripName = deviceData.name
	//def deviceFile = "Quirky Pivot Power Genius"

	deviceData.outlets.each {
		createChildDevice( "Quirky Pivot Power Genius", it.outlet_id, it.name, "$powerstripName ${it.name}" )
        pollOutlet(getChildDevice(it.outlet_id))
	}
}

/////////////////////
// Aros stuff here
////////////////////

def arosCoolingSetpoint(childDevice, setPoint)
{
	apiPut("/air_conditioners/" + childDevice.device.deviceNetworkId, ["desired_state": setPoint]) { response ->
		def data = response.data.data
		log.debug "sending 'setPoint' to device"
	}
}

def arosFanSpeed(childDevice, fanSpeed)
{
	apiPut("/air_conditioners/" + childDevice.device.deviceNetworkId, ["desired_state": fanSpeed]) { response ->
		def data = response.data.data
        log.debug "New Fan Speed: ${fanSpeed}"
	}
}

def arosMode(childDevice, newMode)
{
	apiPut("/air_conditioners/" + childDevice.device.deviceNetworkId, [desired_state: [mode: newMode]]) { response ->
		def data = response.data.data
        log.debug "New arosMode: ${mode}"
	}
}

def arosOff(childDevice)
{
	apiPut("/air_conditioners/" + childDevice.device.deviceNetworkId, [desired_state: [powered : false]]) { response ->
		def data = response.data.data
		log.debug "Sending 'off' to device"
	}
}

def arosOn(childDevice)
{
	apiPut("/air_conditioners/" + childDevice.device.deviceNetworkId, [desired_state: [powered : true]]) { response ->
		def data = response.data.data
		log.debug "Sending 'on' to device"
	}
}

def airConditionerEventHandler()
{
	log.debug "In  airConditionerEventHandler..."

	def json = request.JSON
	def dni = getChildDevice(json.air_conditioner_id)
    
	pollAros(dni)   //sometimes events are stale, poll for all latest states

	def html = """{"code":200,"message":"OK"}"""
	render contentType: 'application/json', data: html
}

def pollAros(childDevice)
{
	log.debug "In pollAros"
	def locationScale = location.getTemperatureScale()

	//log.debug "Polling Aros ${childDevice.device.deviceNetworkId}"
    //log.debug "Location Scale: ${locationScale}"
    
	apiGet("/air_conditioners/" + childDevice.device.deviceNetworkId) { response ->
		def status = response.data.data.last_reading
        
        status.powered ? childDevice?.sendEvent(name:"switch",value:"on") :
			childDevice?.sendEvent(name:"switch",value:"off")
        log.debug "Powered Status: ${status.powered}"
        
		if ( status.temperature != null ) {	
        	if ( locationScale == "F" ) {
            	def displayValue = cToF(status.temperature) as int
                childDevice?.sendEvent(name:"temperature",value:displayValue,unit:"F")
            } else {
            	def displayValue = status.temperature as int
				childDevice?.sendEvent(name:"temperature",value:displayValue,unit:"C")
            }
            log.debug "Temperature Status: ${status.temperature}"
        }
        
        if ( status.desired_max_set_point != null ) {
			if ( locationScale == "F" ) {
            	def displaySetPointValue = cToF(status.desired_max_set_point)
                displaySetPointValue = displaySetPointValue as double
                displaySetPointValue = displaySetPointValue.trunc(2)
        		childDevice?.sendEvent(name:"coolingSetpoint",value:displaySetPointValue)
            } else {
            	def displaySetPointCValue = status.desired_max_set_point as double
                displaySetPointCValue = displaySetPointCValue.trunc(2)
            	childDevice?.sendEvent(name:"coolingSetpoint",value:displaySetPointCValue)
            } 
            log.debug "coolingSetpoint Status: ${status.desired_max_set_point}"
        }
        
        if ( status.desired_fan_speed != null ) {
        	if ( (status.desired_fan_speed == 0.333) || (status.desired_fan_speed == 0.33) )
            	childDevice?.sendEvent(name:"fanMode",value:"fanLow", descriptionText: "$childDevice fan speed is LOW")
            if ( (status.desired_fan_speed == 0.666) || (status.desired_fan_speed == 0.66) )
            	childDevice?.sendEvent(name:"fanMode",value:"fanMed", descriptionText: "$childDevice fan speed is MED")
            if ( (status.desired_fan_speed == 0.999) || (status.desired_fan_speed == 1.0) )
            	childDevice?.sendEvent(name:"fanMode",value:"fanHigh", descriptionText: "$childDevice fan speed is HIGH")    
        }
        log.debug "desired_fan_speed Status: ${status.desired_fan_speed}"
        
        if ( status.desired_mode != null ) {
        	if ( status.desired_mode == "fan_only" )
            	childDevice?.sendEvent(name:"mode",value:"fan_only", descriptionText: "$childDevice mode is FAN ONLY")
            if ( status.desired_mode == "auto_eco" )
            	childDevice?.sendEvent(name:"mode",value:"auto_eco", descriptionText: "$childDevice mode is ECO")
            if ( status.desired_mode == "cool_only" )
            	childDevice?.sendEvent(name:"mode",value:"cool_only", descriptionText: "$childDevice mode is COOL")    
        }
        log.debug "desired_mode Status: ${status.desired_mode}"
	}
}

///////////////////////
// HUB STUFF
///////////////////////

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

///////////////////////
// AUTH STUFF
///////////////////////

def authPage() {
	log.debug "In authPage"
	if(canInstallLabs()) {
		def description = null

		if (state.vendorAccessToken == null) {
			log.debug "About to create access token."

			createAccessToken()
			description = "Tap to enter Credentials."

			def redirectUrl = oauthInitUrl()


			return dynamicPage(name: "Credentials", title: "Authorize Connection", nextPage:"listDevices", uninstall: true, install:false) {
				section { href url:redirectUrl, style:"embedded", required:false, title:"Connect to ${getVendorName()}:", description:description }
			}
		} else {
        
			description = "Tap 'Next' to proceed"

			return dynamicPage(name: "Credentials", title: "Credentials Accepted!", nextPage:"listDevices", uninstall: true, install:false) {
				section { href url: buildRedirectUrl("receivedToken"), style:"embedded", required:false, title:"${getVendorName()} is now connected to SmartThings!", description:description }
			}
		}
	}
	else
	{
		def upgradeNeeded = """Before you can participate in SmartThings Labs we need to update your hub.

Please contact our support team at labs@smartthings.com and tell them you want access to SmartThings Labs!"""


		return dynamicPage(name:"Credentials", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
			section {
				paragraph "$upgradeNeeded"
			}
		}
	}
}

def oauthInitUrl() {
	log.debug "In oauthInitUrl"

	/* OAuth Step 1: Request access code with our client ID */

	state.oauthInitState = UUID.randomUUID().toString()

	def oauthParams = [ response_type: "code",
		client_id: getClientId(),
		state: state.oauthInitState,
		redirect_uri: buildRedirectUrl("receiveToken") ]

	return getVendorAuthPath() + toQueryString(oauthParams)
}

def buildRedirectUrl(endPoint) {
	log.debug "In buildRedirectUrl"

	return getServerUrl() + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/${endPoint}"
}

def receiveToken() {
	log.debug "In receiveToken"
	def html = ""
    def badCredentials = false
    
	def oauthParams = [ client_secret: getClientSecret(),
		grant_type: "authorization_code",
		code: params.code ]

	def tokenUrl = getVendorTokenPath() + toQueryString(oauthParams)
	def params = [
		uri: tokenUrl,
	]

	/* OAuth Step 2: Request access token with our client Secret and OAuth "Code" */
	try
    { 
    	httpPost(params) { response ->

			def data = response.data.data

			state.vendorRefreshToken = data.refresh_token //these may need to be adjusted depending on depth of returned data
			state.vendorAccessToken = data.access_token
        	log.debug "Vendor Access Token: ${state.vendorAccessToken}"
			log.debug "Vendor Refresh Token: ${state.vendorRefreshToken}"
		}
    }
    catch(Exception e)
    {
    	badCredentials = true
        log.debug "Login unsuccessful!"
    }
    
    def displayMessage = ""
    
    if ( badCredentials ) {
    	displayMessage = "Unable to login to your " + getVendorName() + " account using these Credentials"
    } else {
    	displayMessage = "We have located your " + getVendorName() + " account."
    }

	//log.debug "token = ${state.vendorAccessToken}"
    
	/* OAuth Step 3: Use the access token to call into the vendor API throughout your code using state.vendorAccessToken. */
	html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=100%">
        <title>${getVendorName()} Connection</title>
        <style type="text/css">
            @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            .container {
                width: 100%;
                padding: 0px;
                /*background: #eee;*/
                text-align: center;
            }
            img {
                vertical-align: middle;
            }
            img:nth-child(2) {
                margin: 0 10px;
            }
            p {
                font-size: 1.5em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 40px;
                margin-bottom: 0;
            }
        /*
            p:last-child {
                margin-top: 0px;
            }
        */
            span {
                font-family: 'Swiss 721 W01 Light';
            }
        </style>
        </head>
        <body>
            <div class="container">
                <img src=""" + getVendorIcon() + """ alt="Vendor icon" style="width: 30%;max-height: 30%"/>
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" style="width: 10%;max-height: 10%"/>
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" style="width: 30%;max-height: 30%"/>
                <p>$displayMessage</p>
                <p>Tap 'Done' to continue.</p>
			</div>
        </body>
        </html>
        """
	render contentType: 'text/html', data: html
}

def receivedToken() {
	log.debug "In receivedToken"

	def html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="100%" />
        <title>Quirky Connection</title>
        <style type="text/css">
            @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            .container {
                width: 100%;
                padding: 0px;
                /*background: #eee;*/
                text-align: center;
            }
            img {
                vertical-align: middle;
            }
            img:nth-child(2) {
                margin: 0 10px;
            }
            p {
                font-size: 1.5em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 40px;
                margin-bottom: 0;
            }
        /*
            p:last-child {
                margin-top: 0px;
            }
        */
            span {
                font-family: 'Swiss 721 W01 Light';
            }
        </style>
        </head>
        <body>
            <div class="container">
                <img src=""" + getVendorIcon() + """ alt="Vendor icon" style="width: 30%;max-height: 30%"/>
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" style="width: 10%;max-height: 10%"/>
                <img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" style="width: 30%;max-height: 30%"/>
                <p>Your Quirky account is connected to SmartThings. Tap 'Done' to continue to choose devices.</p>
			</div>
        </body>
        </html>
        """
	render contentType: 'text/html', data: html
}
