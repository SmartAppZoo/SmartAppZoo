/**
 *  Title: Withings Service Manager
 * 	Description: Connect Your Withings Devices
 *
 *  Author: steve
 *  Date: 1/9/15
 *
 *
 *  Copyright 2015 steve
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
	name: "Withings Manager",
	namespace: "thebrent",
	author: "SmartThings",
	description: "Connect With Withings",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/withings.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/withings%402x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Partner/withings%402x.png",
	oauth: true,
	usesThirdPartyAuthentication: true,
	pausable: false
) {
	appSetting "clientId"
	appSetting "clientSecret"
}

// ========================================================
// PAGES
// ========================================================

preferences {
	page(name: "authPage")
}

mappings {
  path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
  path("/oauth/callback") {action: [GET: "callback"]}
}
def baseApiUrl() { "https://wbsapi.withings.net/v2" }
def userid() { state.userid }

def authPage() {
  if(!state.accessToken) {
    log.debug "Creating access token"
    createAccessToken()
  }
   
  def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
  log.debug redirectUrl
  if(!state.authToken) {
    log.debug "Loading authPage"
    return dynamicPage(name: "authPage", title: "Login", nextPage: "", uninstall: false) {
      section() {
        paragraph "tap below to log in to the third-party service and authorize SmartThings access"
        href url: redirectUrl, style: "embedded", required: true, title: "Authenticate with Withings", description: "Click to enter credentials"
      }
    }
  } else {
  }
  remove("Remove")
}

// ========================================================
// MAPPINGS
// ========================================================

def test() {
	"${params.action}"()
}

def authenticate() {
	// do not hit userAuthorizationUrl when the page is executed. It will replace oauth_tokens
	// instead, redirect through here so we know for sure that the user wants to authenticate
	// plus, the short-lived tokens that are used during authentication are only valid for 2 minutes
	// so make sure we give the user as much of that 2 minutes as possible to enter their credentials and deal with network latency
	log.trace "starting Withings authentication flow"
	redirect location: userAuthorizationUrl()
}

def exchangeTokenFromWithings() {
	// Withings hits us here during the oAuth flow
//	log.trace "exchangeTokenFromWithings ${params}"
	state.userid = params.userid // TODO: restructure this for multi-user access
	exchangeToken()
}

def notificationReceived() {
//	log.trace "notificationReceived params: ${params}"

	def notificationParams = [
		startdate: params.startdate,
		userid   : params.userid,
		enddate  : params.enddate,
	]

	def measures = GetMeasures(notificationParams)
	sendMeasureEvents(measures)
	return [status: 0]
}

// ========================================================
// HANDLERS
// ========================================================


def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

//	wRevokeAllNotifications()

	unsubscribe()
	initialize()
}

def initialize() {
  log.debug "Initialize"
	if (!getChild()) { createChild() }
	app.updateLabel(withingsLabel)
	wCreateNotification()
	backfillMeasures()
}

// ========================================================
// CHILD DEVICE
// ========================================================

private getChild() {
	def children = childDevices
	children.size() ? children.first() : null
}

private void createChild() {
	def child = addChildDevice("thebrent", "Withings User", userid(), null, [name: app.label, label: withingsLabel])
	state.child = [dni: child.deviceNetworkId]
}

// ========================================================
// WITHINGS MEASURES
// ========================================================

def unixTime(date = new Date()) {
	def unixTime = date.time / 1000 as int
//	log.debug "converting ${date.time} to ${unixTime}"
	unixTime
}

def backfillMeasures() {
//	log.trace "backfillMeasures"
	def measureParams = [startdate: unixTime(new Date() - 10)]
	def measures = wGetMeasures(measureParams)
	sendMeasureEvents(measures)
}

// this is body measures. // TODO: get activity and others too
def getMeasures(measureParams = [:]) {
	def baseUrl = "${baseApiUrl()}/measure"
	def urlParams = [
    access_token: state.authToken,
		category     : userid(),
		startdate  : unixTime(new Date() - 5),
		enddate    : unixTime(),
	] + measureParams
	def measureData = get(baseUrl, urlParams)
//	log.debug "measureData: ${measureData}"
	measureData.body.measuregrps.collect { parseMeasureGroup(it) }.flatten()
}

def sendMeasureEvents(measures) {
//	log.debug "measures: ${measures}"
	measures.each {
		if (it.name && it.value) {
			sendEvent(userid(), it)
		}
	}
}

def parseMeasureGroup(measureGroup) {
	long time = measureGroup.date // must be long. INT_MAX is too small
	time *= 1000
	measureGroup.measures.collect { parseMeasure(it) + [date: new Date(time)] }
}

def parseMeasure(measure) {
//	log.debug "parseMeasure($measure)"
	[
		name : measureAttribute(measure),
		value: measureValue(measure)
	]
}

def measureValue(measure) {
	def value = measure.value * 10.power(measure.unit)
	if (measure.type == 1) { // Weight (kg)
		value *= 2.20462262 // kg to lbs
	}
	value
}

String measureAttribute(measure) {
	def attribute = ""
	switch (measure.type) {
		case 1: attribute = "weight"; break;
		case 4: attribute = "height"; break;
		case 5: attribute = "leanMass"; break;
		case 6: attribute = "fatRatio"; break;
		case 8: attribute = "fatMass"; break;
		case 9: attribute = "diastolicPressure"; break;
		case 10: attribute = "systolicPressure"; break;
		case 11: attribute = "heartPulse"; break;
		case 54: attribute = "SP02"; break;
	}
	return attribute
}

String measureDescription(measure) {
	def description = ""
	switch (measure.type) {
		case 1: description = "Weight (kg)"; break;
		case 4: description = "Height (meter)"; break;
		case 5: description = "Fat Free Mass (kg)"; break;
		case 6: description = "Fat Ratio (%)"; break;
		case 8: description = "Fat Mass Weight (kg)"; break;
		case 9: description = "Diastolic Blood Pressure (mmHg)"; break;
		case 10: description = "Systolic Blood Pressure (mmHg)"; break;
		case 11: description = "Heart Pulse (bpm)"; break;
		case 54: description = "SP02(%)"; break;
	}
	return description
}

// ========================================================
// WITHINGS NOTIFICATIONS
// ========================================================

def wNotificationBaseUrl() { "https://wbsapi.withings.net/notify" }

def wNotificationCallbackUrl() { shortUrl("n") }

def wGetNotification() {
	def userId = userid()
	def url = wNotificationBaseUrl()
	def params = [
		action: "subscribe"
	]

}

// TODO: keep track of notification expiration
def wCreateNotification() {
	def baseUrl = wNotificationBaseUrl()
	def urlParams = [
		action     : "subscribe",
		userid     : userid(),
		callbackurl: wNotificationCallbackUrl(),
		oauth_token: oauth_token(),
		comment    : "hmm" // TODO: figure out what to do here. spaces seem to break the request
	]

	get(baseUrl, urlParams)
}

def wRevokeAllNotifications() {
	def notifications = wListNotifications()
	notifications.each {
		wRevokeNotification([callbackurl: it.callbackurl]) // use the callbackurl Withings has on file
	}
}

def wRevokeNotification(notificationParams = [:]) {
	def baseUrl = wNotificationBaseUrl()
	def urlParams = [
		action     : "revoke",
		userid     : userid(),
		callbackurl: wNotificationCallbackUrl(),
		oauth_token: oauth_token()
	] + notificationParams

	get(baseUrl, urlParams)
}

def wListNotifications() {
	def baseUrl = wNotificationBaseUrl()
	def urlParams = [
		action     : "list",
		userid     : userid(),
		callbackurl: wNotificationCallbackUrl(),
		oauth_token: oauth_token()
	]

	def notificationData = get(baseUrl, urlParams)
	notificationData.body.profiles
}

// ========================================================
// WITHINGS DATA FETCHING
// ========================================================

def get(url, params) {
	log.debug "get(${url}, ${urlParams})"
	def builtUrl = buildUrl(url, paramStrings)
	def json
//	log.debug "about to make request to ${url}"
  def currentAction = [url: builtUrl, params: params]
	httpGet(uri: builtUrl, headers: ["Content-Type": "application/json"]) { response ->
		json = new groovy.json.JsonSlurper().parse(response.data)
	}
	return json
}

private httpGetAndVerifyToken(url, params) {
  log.debug "get(${url}, ${urlParams})"
	def builtUrl = buildUrl(url, paramStrings)
	def json
//	log.debug "about to make request to ${url}"
  def currentAction = [url: builtUrl, params: params]
	httpGet(uri: builtUrl, headers: ["Content-Type": "application/json"]) { response ->
    if(resp.status == 401 && resp.data.status.code == 14) {
      log.debug "Storing the failed action to try later"
      def action = "actionCurrentlyExecuting"
      log.debug "Refreshing your auth_token!"
      refreshAuthToken()
      httpGet(uri: builtUrl, headers: ["Content-Type": "application/json"]) { response2 ->
        json = new groovy.json.JsonSlurper().parse(response2.data);
      }
    }
    else{
		  json = new groovy.json.JsonSlurper().parse(response.data)
    }
	}
	return json
}

def buildUrl(url, params) {
  url + "?" + params.sort().join("&")
}

// ========================================================
// WITHINGS OAUTH LOGGING
// ========================================================

def wLogEnabled() { false } // For troubleshooting Oauth flow

void wLog(message = "") {
	if (!wLogEnabled()) { return }
	def wLogMessage = state.wLogMessage
	if (wLogMessage.length()) {
		wLogMessage += "\n|"
	}
	wLogMessage += message
	state.wLogMessage = wLogMessage
}

void wLogNew(seedMessage = "") {
	if (!wLogEnabled()) { return }
	def olMessage = state.wLogMessage
	if (oldMessage) {
		log.debug "purging old wLogMessage: ${olMessage}"
	}
	state.wLogMessage = seedMessage
}

String wLogMessage() {
	if (!wLogEnabled()) { return }
	def wLogMessage = state.wLogMessage
	state.wLogMessage = ""
	wLogMessage
}


// ========================================================
// WITHINGS OAUTH
// ========================================================
def oauthEndpoint() { "https://account.withings.com/oauth2" }

def oauthInitUrl() {
  state.oauthInitState = UUID.randomUUID().toString()

  def oauthParams = [
    response_type: "code",
    client_id: appSettings.clientId,
    scope: "user.info,user.metrics,user.activity",
    state: state.oauthInitState,
    redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
  ]

  def url = "${oauthEndpoint()}/authorize?${toQueryString(oauthParams)}"
  log.debug url
  redirect(location: url)
}

def callback() {
  log.debug "callback()>> params: $params, params.code ${params.code}"

  def code = params.code
  def oauthState = params.state

  // Validate the response from the third party by making sure oauthState == state.oauthInitState as expected
  if (oauthState == state.oauthInitState) {
    def tokenParams = [
      grant_type: "authorization_code",
      client_id : appSettings.clientId,
      client_secret: appSettings.clientSecret,
      code      : code,
      redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]

      // This URL will be defined by the third party in their API documentation
    def tokenUrl = "${oauthEndpoint()}/token"
    def tokenBody = toQueryString(tokenParams)

    httpPost(uri: tokenUrl, body: tokenBody) { resp ->
      state.refreshToken = resp.data.refresh_token
      state.authToken = resp.data.access_token
    }

    if (state.authToken) {
      success()
    } else {
      fail()
    }

  } else {
    log.error "callback() failed. Validation of state did not match. oauthState != state.oauthInitState"
  }
}

// Example success method
def success() {
  renderAction("authorized")
}

// Example fail method
def fail() {
  renderAction("notAuthorized")
}

private refreshAuthToken() {
  def refreshParams = [
      method: 'POST',
      uri: oauthEndpoint(),
      path: "/token",
      query: [grant_type:'refresh_token', code:"${state.refreshToken}", client_id:"${appSettings.clientId}"],
  ]
    def jsonMap
    httpPost(refreshParams) { resp ->
      if(resp.status == 200) {
        jsonMap = resp.data
        if (resp.data) {
          state.refreshToken = resp?.data?.refresh_token
          state.accessToken = resp?.data?.access_token
        }
      }
  }
}

private String toQueryString(Map m) {
  return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

// ========================================================
// HTML rendering
// ========================================================

def renderAction(action, title = "") {
	log.debug "renderAction: $action"
	renderHTML(title) {
		head { "${action}HtmlHead"() }
		body { "${action}HtmlBody"() }
	}
}

def authorizedHtmlHead() {
	log.trace "authorizedHtmlHead"
	"""
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
				/*width: 560px;
				padding: 40px;*/
				/*background: #eee;*/
				text-align: center;
			}
			img {
				vertical-align: middle;
							max-width:20%;
			}
			img:nth-child(2) {
				margin: 0 30px;
			}
			p {
				/*font-size: 1.2em;*/
				font-family: 'Swiss 721 W01 Thin';
				text-align: center;
				color: #666666;
				padding: 0 10px;
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
		"""
}

def authorizedHtmlBody() {
	"""
		<div class="container">
			<img src="https://s3.amazonaws.com/smartapp-icons/Partner/withings@2x.png" alt="withings icon" />
			<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
			<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
			<p>Your Withings scale is now connected to SmartThings!</p>
			<p>Click 'Done' to finish setup.</p>
		</div>
		"""
}

def notAuthorizedHtmlHead() {
	log.trace "notAuthorizedHtmlHead"
	authorizedHtmlHead()
}

def notAuthorizedHtmlBody() {
	"""
		<div class="container">
			<p>There was an error connecting to SmartThings!</p>
			<p>Click 'Done' to try again.</p>
		</div>
		"""
}
