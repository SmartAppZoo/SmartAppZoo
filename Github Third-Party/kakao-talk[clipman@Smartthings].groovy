/**
 *  Kakao Talk v2022-04-28
 *  fison67@nate.com/clipman@naver.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

include 'asynchttp_v1'

definition(
	name: "Kakao Talk",
	namespace: "clipman",
	author: "clipman",
	description: "SmartThings에서 카카오톡 메시지를 발송합니다.",
	category: "My Apps",
	iconUrl: "https://www.e-mailit.com/social-buttons/build/images/solutions/kakao-talk-icon.png",
	iconX2Url: "https://www.e-mailit.com/social-buttons/build/images/solutions/kakao-talk-icon.png",
	iconX3Url: "https://www.e-mailit.com/social-buttons/build/images/solutions/kakao-talk-icon.png",
	oauth: true
)

preferences {
   page(name: "mainPage")
   page(name: "loginPage")
   page(name: "authPage")
   page(name: "donePage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Kakao Talk", nextPage: null, uninstall: true, install: true) {
   		section("Request New Devices"){
			input "client_id", "text", title: "Client ID", required: true, description:"Cient ID"
		}
		
		section() {
			paragraph "View this SmartApp's configuration to use it in other places."
			href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Config", description:"Config"
	   	}
		
		section() {
		  	href "loginPage", title: "Login", description:""
	   	}
		
	}
}

def loginPage(){
	dynamicPage(name: "loginPage", title: "Login", nextPage: "authPage") {
		section() {
			href url: "https://kauth.kakao.com/oauth/authorize?client_id=" + settings.client_id + "&redirect_uri=" + getRedirectURI() + "&response_type=code", style:"embedded", title:"Click Here"
			paragraph "Access Token: ${state._access_token}"
			paragraph "Refresh Token: ${state._refresh_token}"
	   	}
	}
}

def authPage(){
	dynamicPage(name: "authPage", title:"Auth", nextPage: "donePage") {
		section() {
			href url: "https://kauth.kakao.com/oauth/authorize?client_id=" + settings.client_id + "&redirect_uri=" + getRedirectURI() + "&response_type=code&scope=friends,talk_message", style:"embedded", title:"Click Here"
	   	}
	}
}

def donePage(){
	_getFriends()
	dynamicPage(name: "donePage", title: "Kakao Talk", nextPage: "mainPage") {
   		section("Done"){
			paragraph "Go Back to Main"
		}
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	initialize()
	takeTokenAuto()
}

def initialize() {
	log.debug "initialize"
	
	if (!state.accessToken) {
		createAccessToken()
	}
	
	def dni = "KakaoTalk"
	def exist = existChild(dni)
	def dth = "Kakao Talk"

	if(!exist){
		log.debug "Add dth"
		try{
			//def childDevice = addChildDevice("clipman", dth, dni, location.hubs[0].id, ["label": dth])	
			def childDevice = addChildDevice("clipman", dth, dni, null, ["name": dni, "label": dth])	
		}catch(err){
			log.error err
		}
	}
	
	state["messageIndex"] = 0;
	
	unschedule()
	schedule("0 0/30 * * * ?", takeTokenAuto)
}

def takeTokenAuto(){
	log.debug "Try to a new Access Token by Refresh Token per 1 hours."
	
	try {
		httpPost("https://kauth.kakao.com/oauth/token", 'grant_type=refresh_token&client_id=' + settings.client_id + '&refresh_token=' + state._refresh_token) { resp ->
			processToken(resp)
		}
	} catch (e) {
		log.debug "getAccessToken >> something went wrong: $e"
	}
}

def authError() {
	[error: "Permission denied"]
}

def renderConfig() {
	def configJson = new groovy.json.JsonOutput().toJson([
		description: "Kakao Talk API",
		platforms: [
			[
				platform: "SmartThings Kakao Talk",
				name: "Kakao Talk",
				app_url: apiServerUrl("/api/smartapps/installations/"),
				app_id: app.id,
				access_token:  state.accessToken,
				redirect_uri: getRedirectURI()
			]
		],
	])

	def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
	render contentType: "text/plain", data: configString
}

def getRedirectURI(){
	return apiServerUrl("/api/smartapps/installations/") + app.id + "/oauth?access_token=${state.accessToken}"
}

def oauthPage(){
	if(params.code){
		log.debug "Code >>"  + params.code
		getToken(params.code)
	}
	
	render contentType: "application/javascript", data: new groovy.json.JsonOutput().toJson("result":true)
}

def messagePage(){
	render contentType: "application/javascript", data: state["msg_" + params.index]
}

def getToken(code){
	try {
		httpPost("https://kauth.kakao.com/oauth/token", 'grant_type=authorization_code&client_id=' + settings.client_id + '&redirect_uri=' + getRedirectURI() + '&code=' + code) { resp ->
			processToken(resp)
		}
	} catch (e) {
		log.debug "getAccessToken >> something went wrong: $e"
	}
}

def getAccessToken(){
	return state._access_token
}

def saveData(data){
	def index = state.messageIndex as int
	index++
	if(index > 100){
		index = 0
	}
	state.messageIndex = index
	state["msg_" + state.messageIndex] = data
}

def getMessageCheckURL(){
	def url = "${apiServerUrl("/api/smartapps/installations/${app.id}/message?&access_token=${state.accessToken}&index=${state.messageIndex}")}"
	return url
}

def processToken(resp){
	log.debug resp.data
	if(resp.data.access_token){
		state._access_token = resp.data.access_token
	}
	if(resp.data.refresh_token){
		state._refresh_token = resp.data.refresh_token
	}
}

def friendsCallBack(response, data){
	log.debug "friendsCallBack"
	if (response.hasError()) {
		log.debug "error response data: $response.errorData"
		try {
			// exception thrown if json cannot be parsed from response
			log.debug "error response json: $response.errorJson"
		} catch (e) {
			log.warn "error parsing json: $e"
		}
		try {
			// exception thrown if xml cannot be parsed from response
			log.debug "error response xml: $response.errorXml"
		} catch (e) {
			log.warn "error parsing xml: $e"
		}
		state["friendsUUID"] = ""
	}else{
		def uuids = ""
		def list = response.json.elements
		list.each { person ->
			if(uuids != ""){
				uuids += ","
			}
			uuids += person.uuid
		}
		state["friendsUUID"] = uuids
	}
}

def _getFriends(){
	def param = [
		uri: 'https://kapi.kakao.com/v1/api/talk/friends',
		headers: ['Authorization': 'Bearer ' + state._access_token]
	]
	asynchttp_v1.get(friendsCallBack, param)
}

def getFriendsUUID(){
	def list = state["friendsUUID"].split(",")
	List<String> resultList = []
	list.each { uuid ->
		resultList << uuid
	}
	return resultList
}

def existChild(dni){
	def result = false
	def list = getChildDevices()
	list.each { child ->
		if(child.getDeviceNetworkId() == dni){
			result = true
		}
	}
	return result
}

/*
def getLocationID(){
	def locationID = null
	try{ locationID = location.hubs[0].id }catch(err){}
	return locationID
}
*/

mappings {
	if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
		path("/config")		{ action: [GET: "authError"] }
		path("/oauth")		{ action: [GET: "authError"] }
		path("/message")	{ action: [GET: "authError"] }
	} else {
		path("/config")		{ action: [GET: "renderConfig"] }
		path("/oauth")		{ action: [GET: "oauthPage"] }
		path("/message")	{ action: [GET: "messagePage"] }
	}
}