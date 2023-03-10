/**
 *  STEight (Connect)
 *
 *  Copyright 2017 Alex Lee Yuk Cheung
 *  Copyright 2019 Jason Franz
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
 *	VERSION HISTORY
 *  11.02.2019: v1.0.3 - Device type reference, remove category
 *  05.02.2019: v1.0.2 - Name change, helper functions
 *  21.10.2018: v1.0.1 - Forked
 *	26.01.2017: v1.0b - Token renew error fix.
 *	26.01.2017: v1.0 - Remove BETA label.
 *
 *	19.01.2017: 1.0 BETA Release 6 - Added notification framework with option screen.
 *	12.01.2017: 1.0 BETA Release 5 - Stop partner credentials being mandatory. Change device creation based on whether partner credentials are present.
 *	12.01.2017: 1.0 BETA Release 4 - Enable changing of SmartApp name.
 *	12.01.2017: 1.0 BETA Release 3b - Remove single instance lock for users with multiple mattresses.
 *	12.01.2017: 1.0 BETA Release 3 - Better messaging within smart app on login errors.
 *	11.01.2017: 1.0 BETA Release 2 - Support partner account authentication and session management.
 *	11.01.2017: 1.0 BETA Release 1 - Initial Release
 */
definition(
  name: "STEight (Connect)",
  namespace: "jfrazx",
  author: "Jason Franz",
  category: 'Health & Wellness',
  description: "Connect your Eight Sleep device to SmartThings",
  iconUrl: "https://raw.githubusercontent.com/jfrazx/STEight/master/assets/8slp-icon.png",
  iconX2Url: "https://raw.githubusercontent.com/jfrazx/STEight/master/assets/8slp-icon.png",
  iconX3Url: "https://raw.githubusercontent.com/jfrazx/STEight/master/assets/8slp-icon.png"
)

preferences {
  page(name:"firstPage", title:"Eight Sleep Device Setup", content:"firstPage", install: true)
  page(name: "loginPAGE")
  page(name: "partnerLoginPAGE")
  page(name: "selectDevicePAGE")
  page(name: "notificationsPAGE")
}

def apiURL(path = '/') { return "https://client-api.8slp.net/v1${path}" }

def firstPage() {
  if (isNullOrEmptyString(username) || isNullOrEmptyString(password)) {
    return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
      section {
        headerSECTION()
        href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Eight Sleep account credentials", state: authenticated())
      }
    }
  }
  else
  {
    return dynamicPage(name: "firstPage", title: "", install: true, uninstall: true) {
      section {
        headerSECTION()
        href("loginPAGE", title: null, description: authenticated() ? "Authenticated as " +username : "Tap to enter Eight Sleep account credentials", state: authenticated())
      }
      if (stateTokenPresent()) {
        section ("Add your partner's credentials (optional):") {
          href("partnerLoginPAGE", title: null, description: partnerAuthenticated() ? "Authenticated as " + partnerUsername : "Tap to enter Eight Sleep partner account credentials", state: partnerAuthenticated())
        }
        section ("Choose your Eight Sleep devices:") {
          href("selectDevicePAGE", title: null, description: devicesSelected() ? getDevicesSelectedString() : "Tap to select Eight Sleep devices", state: devicesSelected())
        }
        section ("Notifications:") {
          href("notificationsPAGE", title: null, description: notificationsSelected() ? getNotificationsString() : "Tap to configure notifications", state: notificationsSelected())
        }
        section ("App Name") {
          label name: "name", title: "Assign a Name", defaultValue: app.name
        }
      } else {
        section {
          paragraph "There was a problem connecting to Eight Sleep. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
        }
      }
    }
  }
}

def isNullOrEmptyString(value) {
  return isNull(value) || isEmptyString(value)
}

def isNull(value) {
  return value == null
}

def isEmptyString(value) {
  return value == ''
}

def loginPAGE() {
  if (isNullOrEmptyString(username) || isNullOrEmptyString(password)) {
    return dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
      section { headerSECTION() }
      section { paragraph "Enter your Eight Sleep account credentials below to enable SmartThings and Eight Sleep integration." }
      section {
        input("username", "text", title: "Username", description: "Your Eight Sleep username (usually an email address)", required: true)
        input("password", "password", title: "Password", description: "Your Eight Sleep password", required: true, submitOnChange: true)
  		}
    }
  }
  else {
    getEightSleepAccessToken()
    dynamicPage(name: "loginPAGE", title: "Login", uninstall: false, install: false) {
      section { headerSECTION() }
      section { paragraph "Enter your Eight Sleep account credentials below to enable SmartThings and Eight Sleep integration." }
      section("Eight Sleep Credentials:") {
        input("username", "text", title: "Username", description: "Your Eight Sleep username (usually an email address)", required: true)
        input("password", "password", title: "Password", description: "Your Eight Sleep password", required: true, submitOnChange: true)
      }

      if (stateTokenPresent()) {
        section {
          paragraph "You have successfully connected to Eight Sleep. Click 'Done' to select your Eight Sleep devices."
        }
      }
      else {
        section {
          paragraph "There was a problem connecting to Eight Sleep. Check your user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
        }
      }
    }
  }
}

def partnerLoginPAGE() {
  if (isNullOrEmptyString(partnerUsername) || isNullOrEmptyString(partnerPassword)) {
    return dynamicPage(name: "partnerLoginPAGE", title: "Partner Login", uninstall: false, install: false) {
      section { headerSECTION() }
      section { paragraph "Enter your Eight Sleep partner account credentials below." }
      section {
        input("partnerUsername", "text", title: "Username", description: "Your Eight Sleep partner username (usually an email address)", required: false)
        input("partnerPassword", "password", title: "Password", description: "Your Eight Sleep partner password", required: true, submitOnChange: false)
      }
    }
  }
  else {
    getEightSleepPartnerAccessToken()
    dynamicPage(name: "partnerLoginPAGE", title: "Login", uninstall: false, install: false) {
      section { headerSECTION() }
      section { paragraph "Enter your Eight Sleep partner account credentials below." }
      section("Eight Sleep Partner Credentials:") {
        input("partnerUsername", "text", title: "Username", description: "Your Eight Sleep partner username (usually an email address)", required: false)
        input("partnerPassword", "password", title: "Password", description: "Your Eight Sleep partner password", required: true, submitOnChange: false)
      }

      if (statePartnerTokenPresent()) {
        section {
          paragraph "You have successfully added your partner's credentials.."
        }
      }
      else {
        section {
          paragraph "There was a problem adding your partner's Eight Sleep credentials. Check your partner user credentials and error logs in SmartThings web console.\n\n${state.loginerrors}"
        }
      }
    }
  }
}

def selectDevicePAGE() {
  updateDevices()
  dynamicPage(name: "selectDevicePAGE", title: "Devices", uninstall: false, install: false) {
    section { headerSECTION() }
    section("Select your devices:") {
      input "selectedEightSleep", "enum", image: "https://raw.githubusercontent.com/jfrazx/STEight/master/assets/eightsleep-device.png", required:false, title:"Select Eight Sleep Device \n(${state.eightSleepDevices.size() ?: 0} found)", multiple:true, options:state.eightSleepDevices
    }
  }
}

def notificationsPAGE() {
  dynamicPage(name: "notificationsPAGE", title: "Preferences", uninstall: false, install: false) {
    section {
      input("recipients", "contact", title: "Send notifications to", required: false, submitOnChange: true) {
        input "sendPush", "bool", title: "Send notifications via Push?", required: false, defaultValue: false, submitOnChange: true
      }
      input "sendSMS", "phone", title: "Send notifications via SMS?", required: false, defaultValue: null, submitOnChange: true
      if ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) {
        input "onNotification", "bool", title: "Notify when Eight Sleep heat is on ", required: false, defaultValue: false
        input "offNotification", "bool", title: "Notify when Eight Sleep heat is off ", required: false, defaultValue: false
        input "inBedNotification", "bool", title: "Notify when 'In Bed' event occurs", required: false, defaultValue: false
        input "outOfBedNotification", "bool", title: "Notify when 'Out Of Bed' event occurs", required: false, defaultValue: false
        input "heatLevelReachedNotification", "bool", title: "Notify when desired heat level reached", required: false, defaultValue: false
        input "sleepScoreNotification", "bool", title: "Notify when latest sleep score is updated", required: false, defaultValue: false
      }
    }
  }
}

def headerSECTION() {
  return paragraph (image: "https://raw.githubusercontent.com/jfrazx/STEight/master/assets/8slp-icon.png", "${textVersion()}")
}

def isNotNullOrEmptyString(value) {
  return isNullOrEmptyString(value) == false
}

def stateTokenPresent() {
  return isNotNullOrEmptyString(state.eightSleepAccessToken)
}

def statePartnerTokenPresent() {
  return isNotNullOrEmptyString(state.eightSleepPartnerAccessToken)
}

def authenticated() {
  return (state.eightSleepAccessToken != null && state.eightSleepAccessToken != '') ? "complete" : null
}

def partnerAuthenticated() {
  return (state.eightSleepPartnerAccessToken != null && state.eightSleepPartnerAccessToken != '') ? "complete" : null
}

def devicesSelected() {
  return (selectedEightSleep) ? "complete" : null
}

def getDevicesSelectedString() {
  if (state.eightSleepDevices == null) {
    updateDevices()
  }

  def listString = ""
  selectedEightSleep.each { childDevice ->
    if (state.eightSleepDevices[childDevice] != null) listString += state.eightSleepDevices[childDevice] + "\n"
  }
  return listString
}

def notificationsSelected() {
  return ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) && (settings.onNotification || settings.offNotification || settings.inBedNotification || settings.outOfBedNotification || settings.heatLevelReachedNotification || settings.sleepScoreNotification) ? "complete" : null
}

def getNotificationsString() {
  def listString = ""
  if (location.contactBookEnabled && settings.recipients) {
    listString += "Send the following notifications to " + settings.recipients
  }
  else if (settings.sendPush) {
    listString += "Send the following notifications"
  }

  if (!settings.recipients && !settings.sendPush && settings.sendSMS != null) {
    listString += "Send the following SMS to ${settings.sendSMS}"
  }
  else if (settings.sendSMS != null) {
    listString += " and SMS to ${settings.sendSMS}"
  }

  if ((location.contactBookEnabled && settings.recipients) || settings.sendPush || settings.sendSMS != null) {
    listString += ":\n"
    if (settings.onNotification) listString += "??? Eight Sleep On\n"
    if (settings.offNotification) listString += "??? Eight Sleep Off\n"
    if (settings.inBedNotification) listString += "??? In Bed\n"
    if (settings.outOfBedNotification) listString += "??? Out Of Bed\n"
    if (settings.heatLevelReachedNotification) listString += "??? Desired Heat Level Reached\n"
    if (settings.sleepScoreNotification) listString += "??? Sleep Score\n"
  }
  if (listString != "") listString = listString.substring(0, listString.length() - 1)

  return listString
}

// App lifecycle hooks

def installed() {
  log.debug "installed"
  initialize()
  // Check for new devices and remove old ones every 3 hours
  runEvery3Hours('updateDevices')
  // execute refresh method every minute
  runEvery5Minutes('refreshDevices')
}

// called after settings are changed
def updated() {
  log.debug "updated"
  initialize()
  unschedule('refreshDevices')
  runEvery5Minutes('refreshDevices')
}

def uninstalled() {
  log.info("Uninstalling, removing child devices...")
  unschedule()
  removeChildDevices(getChildDevices())
}

private removeChildDevices(devices) {
  devices.each {
    deleteChildDevice(it.deviceNetworkId) // 'it' is default
  }
}

// Implement event handlers
def eventHandler(evt) {
  log.debug "Executing 'eventHandler' for ${evt.displayName}"
  def msg
  if (evt.value == "open") {
    msg = "${evt.displayName} is out of bed."
    if (settings.outOfBedNotification) {
      messageHandler(msg, false)
    }
  }
  else if (evt.value == "closed") {
    msg = "${evt.displayName} is in bed."
    if (settings.inBedNotification) {
      messageHandler(msg, false)
    }
  }
  else if (evt.value == "on") {
    msg = "${evt.displayName} is on."
    if (settings.onNotification) {
      messageHandler(msg, false)
    }
  }
  else if (evt.value == "off") {
    msg = "${evt.displayName} is off."
    if (settings.offNotification) {
      messageHandler(msg, false)
    }
  }
  else if (evt.value == "true") {
    msg = "${evt.displayName} has reached desired temperature."
    if (settings.heatLevelReachedNotification) {
      messageHandler(msg, false)
    }
  }
  else if (evt.name == "battery") {
    msg = "${evt.displayName} sleep score is ${evt.value}."
    if (settings.sleepScoreNotification) {
      messageHandler(msg, false)
    }
  }
}

// called after Done is hit after selecting a Location
def initialize() {
  log.debug "initialize"
  if (selectedEightSleep) {
    addEightSleep()
  }

  def devices = getChildDevices()
  devices.each {
    if (notificationsSelected() == "complete") {
      subscribe(it, "switch", eventHandler, [filterEvents: false])
      subscribe(it, "contact", eventHandler, [filterEvents: false])
      subscribe(it, "desiredHeatLevelReached", eventHandler, [filterEvents: false])
      subscribe(it, "battery", eventHandler, [filterEvents: false])
    }
    log.debug "Refreshing device $it.name"
    it.refresh()
  }
}

def updateDevices() {
  if (!state.devices) {
    state.devices = [:]
  }
  def devices = devicesList()
  state.eightSleepDevices = [:]

  def selectors = []

  devices.each { device ->
    log.debug "Identified: device ${device}"
    def value = "Eight Sleep ${device.reverse().take(4).reverse()}"
    def key = device
    state.eightSleepDevices["${key}"] = value
    def resp = apiGET("/devices/${device}?filter=ownerId,leftUserId,rightUserId")

    if (resp.status == 200) {
      def leftUserId = resp.data.result.leftUserId
      def rightUserId = resp.data.result.rightUserId
      selectors.add("${device}/${leftUserId}")
      selectors.add("${device}/${rightUserId}")
    } else {
      log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
      return []
    }
  }
  log.debug selectors

  //Remove devices if does not exist on the Eight Sleep platform
  getChildDevices().findAll { !selectors.contains("${it.deviceNetworkId}") }.each {
    log.info("Deleting ${it.deviceNetworkId}")
    try {
      deleteChildDevice(it.deviceNetworkId)
    } catch (physicalgraph.exception.NotFoundException e) {
      log.info("Could not find ${it.deviceNetworkId}. Assuming manually deleted.")
    } catch (physicalgraph.exception.ConflictException ce) {
      log.info("Device ${it.deviceNetworkId} in use. Please manually delete.")
    }
  }
}

def addEightSleep() {
  updateDevices()

  selectedEightSleep.each { device ->
    def resp = apiGET("/devices/${device}?filter=ownerId,leftUserId,rightUserId")
    if (resp.status == 200) {
      //Add left side of mattress as device
      def ownerId = resp.data.result.ownerId
      def leftUserId = resp.data.result.leftUserId
      def rightUserId = resp.data.result.rightUserId
      def childDevice
      if ((leftUserId == ownerId) || (partnerAuthenticated())) {
        childDevice = getChildDevice("${device}/${leftUserId}")
        if (!childDevice && state.eightSleepDevices[device] != null) {
          log.info("Adding device ${device}/${leftUserId}: ${state.eightSleepDevices[device]} [Left]")
          def data = [
            name: "${state.eightSleepDevices[device]} [Left]",
            label: "${state.eightSleepDevices[device]} [Left]"
          ]
          childDevice = addChildDevice(app.namespace, "STEight Mattress", "${device}/${leftUserId}", null, data)
          log.debug "Created ${state.eightSleepDevices[device]} [Left] with id: ${device}/${leftUserId}"
        } else {
          log.debug "found ${state.eightSleepDevices[device]} [Left] with id ${device}/${leftUserId} already exists"
        }
      }

        //Add right side of mattress as device
      if ((rightUserId == ownerId) || (partnerAuthenticated())) {
        childDevice = getChildDevice("${device}/${rightUserId}")
        if (!childDevice && state.eightSleepDevices[device] != null) {
          log.info("Adding device ${device}/${rightUserId}: ${state.eightSleepDevices[device]} [Right]")
          def data = [
            name: "${state.eightSleepDevices[device]} [Right]",
            label: "${state.eightSleepDevices[device]} [Right]"
           ]
          childDevice = addChildDevice(app.namespace, "STEight Mattress", "${device}/${rightUserId}", null, data)
          log.debug "Created ${state.eightSleepDevices[device]} [Right] with id: ${device}/${rightUserId}"
        } else {
          log.debug "found ${state.eightSleepDevices[device]} [Right] with id ${device}/${rightUserId} already exists"
        }
      }
    } else {
      log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
      return []
    }
  }
}

def refreshDevices() {
  log.info("Executing refreshDevices...")
  atomicState.renewAttempt = 0
  atomicState.renewAttemptPartner = 0
  getChildDevices().each { device ->
    log.info("Refreshing device ${device.name} ...")
    device.refresh()
  }
}

def devicesList() {
  logErrors([]) {
    def resp = apiGET("/users/me")
    if (resp.status == 200) {
      return resp.data.user.devices
    } else {
      log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
      return []
   }
  }
}

def getEightSleepAccessToken() {
	def body = [
        	"email": "${username}",
        	"password" : "${password}"
        ]
	def resp = apiPOST("/login", body)
    state.eightSleepAccessToken = null
    if (resp.status == 200) {
		state.eightSleepAccessToken = resp.data.session.token
        state.userId = resp.data.session.userId
        atomicState.expirationDate = resp.data.session.expirationDate
        log.debug "eightSleepAccessToken: $resp.data.session.token"
        log.debug "eightSleepUserId: $resp.data.session.userId"
        log.debug "eightSleepTokenExpirationDate: $resp.data.session.expirationDate"
        state.loginerrors = null
	} else {
		log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
        state.loginerrors = "Error: ${resp.status}: ${resp.data}"
		return []
    }
}

def getEightSleepPartnerAccessToken() {
	def body = [
        	"email": "${partnerUsername}",
        	"password" : "${partnerPassword}"
        ]
	def resp = apiPOST("/login", body)
    state.eightSleepPartnerAccessToken = null
    if (resp.status == 200) {
		state.eightSleepPartnerAccessToken = resp.data.session.token
        state.partnerUserId = resp.data.session.userId
        atomicState.partnerExpirationDate = resp.data.session.expirationDate
        log.debug "eightSleepPartnerAccessToken: $resp.data.session.token"
        log.debug "partnerUserId: $resp.data.session.userId"
        log.debug "partnerExpirationDate: $resp.data.session.expirationDate"
        state.loginerrors = null
	} else {
		log.error("Non-200 from device list call. ${resp.status} ${resp.data}")
        state.loginerrors = "Error: ${resp.status}: ${resp.data}"
		return []
    }
}

def apiGET(path) {
	try {
        httpGet(uri: apiURL(path), headers: apiRequestHeaders()) {response ->
			logResponse(response)
			return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def apiGETWithPartner(path) {
	def result
	if (partnerUsername != null && partnerUsername != '' && partnerPassword != null && partnerPassword != '') {
		try {
        	httpGet(uri: apiURL(path), headers: apiPartnerRequestHeaders()) {response ->
				logResponse(response)
				result = response
			}
		} catch (groovyx.net.http.HttpResponseException e) {
			logResponse(e.response)
			result = e.response
		}
    } else {
    	result = ""
    }
    result
}

def apiPOST(path, body = [:]) {
	try {
		log.debug("Beginning API POST: ${path}, ${body}")
		httpPost(uri: apiURL(path), body: new groovy.json.JsonBuilder(body).toString(), headers: apiRequestHeaders() ) {response ->
			logResponse(response)
			return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

def apiPUT(path, body = [:]) {
	try {
		log.debug("Beginning API POST: ${path}, ${body}")
		httpPut(uri: apiURL(path), body: new groovy.json.JsonBuilder(body).toString(), headers: apiRequestHeaders() ) {response ->
			logResponse(response)
			return response
		}
	} catch (groovyx.net.http.HttpResponseException e) {
		logResponse(e.response)
		return e.response
	}
}

Map apiRequestHeaders() {
   //Check token expiry
   if (state.eightSleepAccessToken) {
   		def now = new Date().getTime()
   		def sessionExpiryTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", atomicState.expirationDate).getTime()
   		if (now > sessionExpiryTime) {
        	if (!atomicState.renewAttempt) {atomicState.renewAttempt = 0}
            log.debug "Renewing Access Token Attempt ${atomicState.renewAttempt}"
            if (atomicState.renewAttempt < 5) {
            	atomicState.renewAttempt = atomicState.renewAttempt +1
        		getEightSleepAccessToken()
            } else {
            	log.error "Renew attempt limit reached"
            }
   		} else {
        	atomicState.renewAttempt = 0
        }
   }

   return [ "Host": "client-api.8slp.net",
   			"Content-Type": "application/json",
            "API-Key": "api-key",
            "Application-Id": "morphy-app-id",
    		"Connection": "keep-alive",
            "User-Agent" : "Eight%20AppStore/11 CFNetwork/808.2.16 Darwin/16.3.0",
            "Accept-Language": "en-gb",
			"Accept-Encoding": "gzip, deflate",
			"Accept": "*/*",
			"app-Version": "1.10.0",
            "Session-Token": "${state.eightSleepAccessToken}"

	]
}

Map apiPartnerRequestHeaders() {
   //Check token expiry
   if (state.eightSleepPartnerAccessToken) {
   		def now = new Date().getTime()
   		def sessionExpiryTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", atomicState.partnerExpirationDate).getTime()
   		if (now > sessionExpiryTime) {
        	if (!atomicState.renewAttemptPartner) {atomicState.renewAttemptPartner = 0}
            log.debug "Renewing Partner Access Token Attempt ${atomicState.renewAttempt}"
            if (atomicState.renewAttemptPartner < 5) {
            	atomicState.renewAttemptPartner = atomicState.renewAttemptPartner +1
        		getEightSleepPartnerAccessToken()
            } else {
            	log.error "Renew attempt limit reached"
            }
   		} else {
        	atomicState.renewAttemptPartner = 0
        }
   }

   return [ "Host": "client-api.8slp.net",
   			"Content-Type": "application/json",
            "API-Key": "api-key",
            "Application-Id": "morphy-app-id",
    		"Connection": "keep-alive",
            "User-Agent" : "Eight%20AppStore/11 CFNetwork/808.2.16 Darwin/16.3.0",
            "Accept-Language": "en-gb",
			"Accept-Encoding": "gzip, deflate",
			"Accept": "*/*",
			"app-Version": "1.10.0",
            "Session-Token": "${state.eightSleepPartnerAccessToken}"

	]
}

def logResponse(response) {
	log.info("Status: ${response.status}")
	log.info("Body: ${response.data}")
}

def logErrors(options = [errorReturn: null, logObject: log], Closure c) {
	try {
		return c()
	} catch (groovyx.net.http.HttpResponseException e) {
		options.logObject.error("got error: ${e}, body: ${e.getResponse().getData()}")
		if (e.statusCode == 401) { // token is expired
			state.remove("eightSleepAccessToken")
			options.logObject.warn "Access token is not valid"
		}
		return options.errorReturn
	} catch (java.net.SocketTimeoutException e) {
		options.logObject.warn "Connection timed out, not much we can do here"
		return options.errorReturn
	}
}

def messageHandler(msg, forceFlag) {
	log.debug "Executing 'messageHandler for $msg. Forcing is $forceFlag'"
	if (settings.sendSMS != null && !forceFlag) {
		sendSms(settings.sendSMS, msg)
	}
    if (location.contactBookEnabled && settings.recipients) {
    	sendNotificationToContacts(msg, settings.recipients)
    } else if (settings.sendPush || forceFlag) {
		sendPush(msg)
	}
}

private def textVersion() {
  def text = "STEight (Connect)\nVersion: 1.0.3\nDate: 11022019(2019)"
}

private def textCopyright() {
  def text = "Copyright ?? 2019 Jason Franz"
}
