/**
*  KuKu Meter
*
*  Copyright 2017 KuKu
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
def version() {	return "v1.3.0" }
/*
 *	09/10/2017 >>> v1.0.0 - Release first KuKuMeter
 *  09/10/2017 >>> v1.1.0 - Modified Tile view and added a 'Power Meter', 'Energy Meter' capability
 *  09/11/2017 >>> v1.1.5 - added 'uninstall' confirm and fixed 'refresh token' issue
 *  09/11/2017 >>> v1.2.0 - added Voltage, Current, Charge tile
 *  09/12/2017 >>> v1.2.1 - fixed issue that there is no 'charge' value when it is installed
 *  09/14/2017 >>> v1.3.0 - added 11 graph cards
 *  10/02/2017 >>> v1.3.1 - changed to singleInstance
 *  11/08/2017 >>> v1.3.2 - add 'Sensor' capability for adding to 'ActionTile'
 *  11/10/2018 >>> v1.3.3 - add reinstall function
*/


definition(
    name: "KuKu Meter",    
    namespace: "turlvo",
    author: "KuKu",
    description: "With visible realtime energy usage status, have good energy habits and enrich your life",
    category: "Convenience",
    iconUrl: "https://cdn.rawgit.com/turlvo/KuKuMeter/master/images/icon/KuKu_Meter_Icon_1x.png",
    iconX2Url: "https://cdn.rawgit.com/turlvo/KuKuMeter/master/images/icon/KuKu_Meter_Icon_2x.png",
    iconX3Url: "https://cdn.rawgit.com/turlvo/KuKuMeter/master/images/icon/KuKu_Meter_Icon_3x.png",
    singleInstance: true,
    oauth: true)
{
    appSetting "clientId"
    appSetting "clientSecret"
    appSetting "callback"
}


preferences {
    page(name: "installPage")
    page(name: "mainPage")  
    page(name: "reinstall")
}

cards {
    card(name: "Encored Energy Service", type: "html", action: "getHtml", whitelist: whiteList()) {}
}

/* This list contains, that url need to be allowed in Smart Energy Service.*/
def whiteList() {
    [
        "code.jquery.com", 
        "ajax.googleapis.com", 
        "fonts.googleapis.com",
        "code.highcharts.com", 
        "enertalk-card.encoredtech.com", 
        "s3-ap-northeast-1.amazonaws.com",
        "s3.amazonaws.com", 
        "ui-hub.encoredtech.com",
        "auth.encoredtech.com",
        "api.encoredtech.com",
        "cdnjs.cloudflare.com",
        "encoredtech.com",
        "itunes.apple.com"
    ]
}

/* url endpoints */
mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

// First page will be shown
def mainPage() {
    if (!atomicState.isInstalled) {
        loadInitValue()
    }

    return dynamicPage(name: "mainPage", title: "", install: true, uninstall: true,refreshInterval: interval) {
        section("User Info. :") {
            paragraph "EMAIL : ${atomicState.userInfo.email}"
        }

        section("Site Info :") {                    
            paragraph "Desc : ${atomicState.installedSiteInfo.description}"
        }

        section("Device Info :") {
            if (atomicState.installedDeviceInfo.name) {
                paragraph "NAME : ${atomicState.installedDeviceInfo.name}"
            } else {
                paragraph "SERIAL : ${atomicState.installedDeviceInfo.serialNumber}"
            }
        }
        section() {
            href "reinstall", title: "Reinstall", description: "If you have a problem, just reinstall instead of uninstall"
        }
        /*
            section("Select Info. Card :") {
                def cardType = [ "Energy Clock",
                                  "Real Time v3",
                                  "Notification",
                                  "Home",
                                  "Neighbor Comparison",
                                  "Usage Plan",
                                  "Real Time v4",
                                  "Progress Tier",
                                  "Month Usage",
                                  "Month Usage Chart",
                                  "Energy Book"]
                input name: "selectedCards", type: "enum", title: "asfa", multiple: true, options: cardType, submitOnChange: true, required: false   
            }*/

    }
}

def installPage() {
    /* Select API server version */    
    atomicState.env_mode ="prod"

    def lang = clientLocale?.language

    /* getting language settings of user's device.  */
    atomicState.language = "ko"

    /* create tanslation for descriptive and informative strings that can be seen by users. */
    if (!state.languageString) {
        createLocaleStrings() 
    }

    if (!state.accessToken) {
        log.debug "checkAccessToken>> SmartThings Access Token does not exist."
        createAccessToken()
    }
    def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    
    if (!atomicState.encoredAccessToken) {
        /* These lines will start the OAuth process.\n*/
        log.debug "checkAccessToken>> Start OAuth request: " + redirectUrl
        return dynamicPage(name: "installPage", install: true,  uninstall: true) {
            section{
                paragraph state.languageString."${atomicState.language}".desc1
                href(title: state.languageString."${atomicState.language}".main,
                     description: state.languageString."${atomicState.language}".desc2,
                     required: false,
                     style:"embedded",
                     url: redirectUrl)
            }
        }
    } else {
    	return mainPage()
    }
}

def reinstall() {
	atomicState.encoredAccessToken = ""
    return installPage()
}
//////////////
// ST method
def installed() {
    log.debug "Installed()"
	atomicState.isInstalled = true
    initialize()
}

def updated() {
    log.debug "updated()"    
}

def initialize() {
    log.debug "Initializing Application"

    /* add device Type Handler */        
    def d = getChildDevice(atomicState.installedDeviceInfo.id)
    if(!d) {
        log.debug "Creating Device Type Handler."
        d = addChildDevice("turlvo", "KuKu Meter Energy Meter", atomicState.installedDeviceInfo.id, null, [name:"KuKu Meter", label:name])

        pullData()
    } else {
        log.debug "Device already created"
    }    
}
//////////////

//////////////
// load method
// load first data from server(user, site, device)
def loadInitValue() {
    def EATValidation = checkEncoreAccessTokenValidation()

    /* if token exist get user's device id, uuid */
    if (EATValidation) {
    	loadUserInfo()
        loadSiteList()
        if(atomicState.sitesList[0] != null) {
        	atomicState.installedSiteInfo = atomicState.sitesList[0]
        	loadDeviceListFromSite(atomicState.sitesList[0].id)
            if (atomicState.deviceList[0] != null) {
            	atomicState.installedDeviceInfo = atomicState.deviceList[0]
            }
		}
        log.debug "initialize>> site: ${atomicState.installedSiteInfo}, device: ${atomicState.installedDeviceInfo}"
    } else {
        log.warning "Ecored Access Token did not get refreshed!"
        return
    }
}

// Called by child and load data from server(usage, real)
def pullData() {
	log.debug "pullData()"
    checkEncoreAccessTokenValidation()

    def d = getChildDevice(atomicState.installedDeviceInfo.id)
    
    if (null != location) {
        def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
        d?.sendEvent(name: "lastCheckin", value: now)
    }
    
    def TotalUsage = getBillingUsage(atomicState.installedDeviceInfo.id)
    log.debug "pullData>> TotalUsage : $TotalUsage"
    if (TotalUsage) {    	
    	if (TotalUsage.usage) {
       		def fixedValue = changedTo2Point(TotalUsage.usage / 1000000)
            log.debug "pullData>> TotalUsage : ${fixedValue}"
            d?.sendEvent(name: "energy", value : "${fixedValue}")
        }
        
        if (TotalUsage.bill && TotalUsage.bill.charge) {
        	d?.sendEvent(name: "charge", value : "${TotalUsage.bill.charge}")
        }
    }
    
    def RealUsage = getRealtimeUsage(atomicState.installedDeviceInfo.id)
    log.debug "pullData>> RealUsage : $RealUsage"
    if (RealUsage) {    	
        if (RealUsage.activePower) {
            def fixedPowerValue = changedTo2Point(RealUsage.activePower / 1000)
            log.debug "pullData>> RealUsage power: ${fixedPowerValue}"
            d?.sendEvent(name: "power", value : "${fixedPowerValue}")
            d?.sendEvent(name: "view", value : "${fixedPowerValue}", displayed: false) 
        }

        if (RealUsage.current) {
            def fixedCurrentValue = changedTo2Point(RealUsage.current / 1000)
            log.debug "pullData>> RealUsage power: current: ${fixedCurrentValue}"
            d?.sendEvent(name: "current", value : "${fixedCurrentValue}")
        }

        if (RealUsage.voltage) {
            def fixedVoltageValue = changedTo2Point(RealUsage.voltage / 1000)   
            log.debug "pullData>> RealUsage power: voltage: ${fixedVoltageValue}"
            d?.sendEvent(name: "voltage", value : "${fixedVoltageValue}")        
        }   	

     
    }
}

def loadSiteList() {
	def list = []
    
    def result = getSiteList()
    if (result) {
    	result.each {
        	log.debug "loadSiteList>> item : $it"
            list.add(it)
        }
        atomicState.sitesList = list
    } else {
    	atomicState.sitesList = null
    }
    log.debug "loadSiteList>> sitesList : ${atomicState.sitesList}"
}

def loadDeviceListFromSite(siteId) {
	def list = []
    
    def result = getDeviceList(siteId)
    if (result) {
    	result.each {
        	log.debug "loadDeviceListFromSite>> item : $it"
            list.add(it)
        }
        atomicState.deviceList = list
    } else {
    	atomicState.deviceList = null
    }
    log.debug "loadDeviceListFromSite>> deviceList : ${atomicState.deviceList}"
}

def loadUserInfo() {
	def result = getUserInfo()
    atomicState.userInfo = result
    log.debug "loadUserInfo>> userInfo : $result"
}

def changedTo2Point(value) {
	return (Math.round(value*100) / 100) as Float
}
//////////////


//////////////
// HTTP method
private getHttpPutJson(param) {

    log.debug "getHttpPutJson>>  params : ${param}"
    try {
        httpPut(param) { resp ->
            log.debug "getHttpPutJson>> resp: ${resp.data}"
            jsonMap = resp.data
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        log.warn "getHttpPutJson>> HTTP Put Error : ${e}"
    }
}

private getHttpPostJson(param) {
    log.debug "getHttpPostJson>> params :${param}"
    def jsonMap = null
    try {
        httpPost(param) { resp ->
            log.debug "getHttpPostJson>> resp: ${resp.data}"
            jsonMap = resp.data
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        log.warn "getHttpPostJson>> HTTP Post Error : ${e}"
    }

    return jsonMap
}

private getHttpGetJson(param, testLog) {
    log.debug "getHttpGetJson>> params : ${param}"
    def jsonMap = null
    try {
        httpGet(param) { resp ->
            log.debug "getHttpGetJson>> resp: ${resp.data}"
            jsonMap = resp.data
        }
    } catch(groovyx.net.http.HttpResponseException e) {
        log.warn "getHttpGetJson>> HTTP Get Error : ${e}"
    }

    return jsonMap

}

private makePostParams(uri, header, body=[]) {
    return [
        uri : uri,
        headers : header,
        body : body
    ]
}

private makeGetParams(uri, headers, path="") {
    return [
        uri : uri,
        path : path,
        headers : headers
    ]
}

String toQueryString(Map m) {
    return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}
//////////////

//////////////
// Auth method
// Checking whether authToken is valid and if not, refresh
private checkEncoreAccessTokenValidation() {
    /* make a parameter to check the validation of Encored access token */    
    log.debug "checkEncoreAccessTokenValidation>> before access: ${atomicState.encoredAccessToken}"
    log.debug "checkEncoreAccessTokenValidation>> before refresh: ${atomicState.encoredRefreshToken}"
    log.debug "checkEncoreAccessTokenValidation>> before authorization: " + "${appSettings.clientId}:${appSettings.clientSecret}".bytes.encodeBase64()
    
    def verifyParam = makeGetParams("https://auth.enertalk.com/verify", 
                                    [Authorization: "Bearer ${atomicState.encoredAccessToken}", ContentType: "application/json"])
    /* check the validation */
    def verified = getHttpGetJson(verifyParam, 'verifyToken')

    log.debug "checkEncoreAccessTokenValidation>> verified : ${verified}"

    /* if Encored Access Token need to be renewed. */
    if (!verified) {
        try {
            refreshAuthToken()

            /* Recheck the renewed Encored access token. */
            verifyParam.headers = [Authorization: "Bearer ${atomicState.encoredAccessToken}"]
            verified = getHttpGetJson(verifyParam, 'CheckRefresh')

        } catch (groovyx.net.http.HttpResponseException e) {
            /* If refreshing token raises an error  */
            log.warn "checkEncoreAccessTokenValidation>> Refresh Token Error :  ${e}"
        }
    }


    return verified
}

def oauthInitUrl() {
    log.debug "oauthInitUrl>> In state of sending a request to Encored for OAuth code.\n"
    // Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    state.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
        response_type: "code",
        scope: "smartRead,smartWrite",
        client_id: appSettings.clientId,
        state: state.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback"
    ]
    log.debug "oauthInitUrl>> " + "https://auth.enertalk.com/authorization?${toQueryString(oauthParams)}"
    redirect(location: "https://auth.enertalk.com/authorization?${toQueryString(oauthParams)}")
}

// refresh auth token
private refreshAuthToken() {
    /*Refreshing Encored Access Token*/

    log.debug "refreshAuthToken>> Refreshing Encored Access Token"
    if(!atomicState.encoredRefreshToken) {
        log.error "refreshAuthToken>> Encored Refresh Token does not exist!"
    } else {
        /* Making a parameter to swap code with a token */
        def authorization = "Basic " + "${appSettings.clientId}:${appSettings.clientSecret}".bytes.encodeBase64()
        def uri = "https://auth.enertalk.com/token"
        def header = ["Authorization": "${authorization}", "contentType": "application/json"]
        def body = ["grant_type": "refresh_token", "refresh_token": "${atomicState.encoredRefreshToken}"]

        log.debug "refreshAuthToken>> Swap code with a token"
        def refreshTokenParams = makePostParams(uri, header, body)

        log.debug "refreshAuthToken>> API call to Encored to get a refresh a token"
        def newAccessToken = getHttpPostJson(refreshTokenParams)

        if (newAccessToken) {
            atomicState.encoredAccessToken = newAccessToken.access_token
            atomicState.encoredRefreshToken = newAccessToken.refresh_token
            log.debug "refreshAuthToken>> after access: ${atomicState.encoredAccessToken}"
            log.debug "refreshAuthToken>> after refresh :${atomicState.encoredRefreshToken}"            
            log.debug "refreshAuthToken>> after authorization: " + "${appSettings.clientId}:${appSettings.clientSecret}".bytes.encodeBase64()
            log.debug "refreshAuthToken>> Successfully got new Encored Access Token.\n"
        } else {
            log.error "refreshAuthToken>> Was unable to renew Encored Access Token.\n"
        }
    }
}

// Oauth calbak method
def callback() {
    log.debug "callback()>> params: $params, params.code ${params.code}, params.state ${params.state}, atomicState.oauthInitState ${atomicState.oauthInitState}"

    def code = params.code
    def oauthState = params.state

    // Validate the response from the third party by making sure oauthState == state.oauthInitState as expected
    if (oauthState == state.oauthInitState){
        log.debug "callback()>> Request Encored to swap code with Encored Aceess Token"

        /* Making a parameter to swap code with a token */
        def uri = "https://auth.enertalk.com/token"
        def header = ["contentType": "application/json"]
        def body = ["client_id": "${appSettings.clientId}", "client_secret": "${appSettings.clientSecret}","grant_type": "authorization_code", 'code': "${params.code}"]

        log.debug "callback()>> Swap code with a token"
        def encoredTokenParams = makePostParams(uri, header, body)

        log.debug "callback()>> API call to Encored to swap code with a token"
        def encoredTokens = getHttpPostJson(encoredTokenParams)

        /* make a page to show people if the REST was successful or not. */
        if (encoredTokens) {
            log.debug "callback()>> Got Encored OAuth token\n"
            atomicState.encoredRefreshToken = encoredTokens.refresh_token
            atomicState.encoredAccessToken = encoredTokens.access_token

            success()          
        } else {
            log.debug "callback()>> Could not get Encored OAuth token\n"
            fail()     
        }
    } else {
        log.error "callback() failed. Validation of state did not match. oauthState != state.oauthInitState"
    }
}

/* make a success message. */
private success() {
	def lang = clientLocale?.language
   
    if ("${lang}" == "ko") {
   		log.debug "I was here at first."
    	atomicState.language = "ko"
    } else {
  
    	atomicState.language = "en"
    }
	log.debug atomicState.language
	def message = atomicState.languageString."${atomicState.language}".message1
	connectionStatus(message)
}

/* make a failure message. */
private fail() {
	def lang = clientLocale?.language
   
    if ("${lang}" == "ko") {
   		log.debug "I was here at first."
    	atomicState.language = "ko"
    } else {
  
    	atomicState.language = "en"
    }
    def message = atomicState.languageString."${atomicState.language}".message2
    connectionStatus(message)
}

private connectionStatus(message) {
    def html = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta name="viewport" content="initial-scale=1, maximum-scale=1, user-scalable=no, width=device-width height=device-height">

                <link href='https://fonts.googleapis.com/css?family=Roboto' rel='stylesheet' type='text/css'>
                <title>SmartThings Connection</title>
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
                body {
                margin: 0;
                width : 100%;
                }
                .container {
                width: 100%;

                /*background: #eee;*/
                text-align: center;
                }
                img {
                vertical-align: middle;
                margin-top:20.3125vw;

                }

                .encored{
                width: 25vw;
                height: 25vw;
                margin-right : 8.75vw;
                }
                .chain {
                width:6.25vw;
                height: 6.25vw;
                }
                .smartt {
                width: 25vw;
                height: 25vw;
                margin-left: 8.75vw
                }

                p {
                font-size: 21px;
                font-weight: 300;
                font-family: Roboto;
                text-align: center;
                color: #4c4c4e;

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
                <img class="encored" src="https://cdn.rawgit.com/turlvo/KuKuMeter/master/images/icon/KuKu_Meter_Icon_3x.png" alt="Encored icon" />
                <img class="chain" src="https://s3-ap-northeast-1.amazonaws.com/smartthings-images/icon_link.svg" alt="connected device icon" />
                <img class="smartt" src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
                <p>${message}</p>

                </div>

                </body>
                </html>
                """
    render contentType: 'text/html', data: html
}

def getAccessToken() {
	return atomicState.encoredAccessToken
}
//////////////


//////////////
// API method
// User
// get User Info
def getUserInfo() {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/users/me",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}", "accept-version": "2.0.0"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getBillInfo>> result : $result"

    return result
}


// Bill
// Get a site’s billing information.
def getBillInfo(siteId) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/sites/${siteId}/bills",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}", "accept-version": "2.0.0"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getBillInfo>> sitedId : $siteId, result : $result"

    return result
}

// Update bill information
def updateBillInfo(siteId, meterDate, supplierId, ratePlanId, billSettings) {
    // To Do
}

// List suppliers
def getSuppliersList(siteId, countryCode) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/suppliers/${siteId}/?countryCode=${countryCode}",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}", "accept-version": "2.0.0"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getSuppliersList>> sitedId : $siteId, country : $countryCode, result : $result"

    return result
}

// Get rate plan schema
def getRatePlan(siteId, supplierId, ratePlanId) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/suppliers/${supplierId}/rateplans/${ratePlanid}/schema",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}", "accept-version": "2.0.0"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getRatePlan>> sitedId : $siteId, supplierId : $supplierId, ratePlanId : $ratePlanId, result : $result"

    return result
}

// Device 
// Create Device
def createDevice(siteId) {
    // To Do
}

def updateDevice() {
    // To Do
}

def replaceDevice() {
    // To Do
}

def deleteDevice() {
    // To Do
}

// List devices of a site
def getDeviceList(siteId) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/sites/${siteId}/devices",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getDeviceList>> sitedId : $siteId, result : $result"

    return result
}

// Get device
def getDeviceInfo(deviceId) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/devices/${deviceId}",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getDeviceInfo>> deviceId : $deviceId, result : $result"

    return result
}

// Push Notification
//def regPush(

// Sites
// Get information of all sites belonging to a user
def getSiteList() {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/sites",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}", "accept-version": "2.0.0"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getSiteList>> result : $result"

    return result
}

def getSiteInfo(siteId) {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/sites/${siteId}",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getSiteInfo>> siteId : $siteId, result : $result"

    return result
}

// Update a site Information for a user.
def updateSiteInfo() {
    // ToDo
}

// Delete a site and its devices.
def deleteSite() {
    // To Do
}

// USAGE
// Get usage from start to end time for a site, tag, or a device.
def getBillingUsage(deviceId, subParam="") {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/devices/${deviceId}/usages/billing",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getBillingUsage>> deviceId : $deviceId, result : $result"

    return result
}

// Get realtime usage information for a site, tag, or a device
def getRealtimeUsage(deviceId, subParam="") {
    def getParams = makeGetParams("${state.domains."${atomicState.env_mode}"}/devices/${deviceId}/usages/realtime",
                                  ["Authorization": "Bearer ${atomicState.encoredAccessToken}"])
    def result = getHttpGetJson(getParams, "")
    log.debug "getRealtimeUsage>> deviceId : $deviceId, result : $result"

    return result
}
//////////////

private createLocaleStrings() {
    state.domains = [
        test : "http://api.enertalk.com/1.2",
        prod : "https://api2.enertalk.com"
    ]

    state.languageString = 
        [
            energyPlan : 30000,
            en : [
                desc1 : "Tab below to sign in or sign up to Encored EnerTalk smart energy service and authorize SmartThings access.",
                desc2 : "Click to proceed authorization.",
                main : "EnerTalk",
                defaultValues : [
                    default1 : "kWh",
                    default2 : "1st day of the month",
                    default3 : "Low voltage"
                ],
                meteringDays : [
                    "1st day of the month", 
                    "2nd day of the month", 
                    "3rd day of the month",
                    "4th day of the month",
                    "5th day of the month",
                    "6th day of the month",
                    "7th day of the month",
                    "8th day of the month",
                    "9th day of the month",
                    "10th day of the month",
                    "11th day of the month",
                    "12th day of the month",
                    "13th day of the month",
                    "14th day of the month",
                    "15th day of the month",
                    "16th day of the month",
                    "17th day of the month",
                    "18th day of the month",
                    "19th day of the month",
                    "20st day of the month",
                    "21st day of the month",
                    "22nd day of the month",
                    "23rd day of the month",
                    "24th day of the month",
                    "25th day of the month",
                    "26th day of the month",
                    "Rest of the month"
                ],
                displayUnits : ["WON(₩)", "kWh"],
                contractTypes : ["Low voltage", "High voltage"],
                title1 : "Send push notification",
                title2 : "Energy Plan",
                subTitle1 : "Setup your energy plan by won",
                title3 : "Display Unit",
                title4 : "Metering Date",
                title5 : "Contract Type",
                title6 : "User & Notifications",
                message1 : """ <p>Your Encored Account is now connected to SmartThings!</p> <p>Click 'Done' to finish setup.</p> """,
                message2 : """ <p>The connection could not be established!</p> <p>Click 'Done' to return to the menu.</p> """,
                message3 : [
                    header : "Device is not installed",
                    body1 : "You need to install EnerTalk device at first,",
                    body2 : "and proceed setup and register device.",
                    button1 : "Setup device",
                    button2 : "Not Installed"
                ],
                message4 : [
                    header : "Device is not connected.",
                    body1 : "Please check the Wi-Fi network connection",
                    body2 : "and EnerTalk device status.",
                    body3 : "Select ‘Setup Device’ to reset the device."
                ]
            ],
            ko :[
                desc1 : "스마트 에너지 서비스를 이용하시려면 EnerTalk 서비스 가입과 SmartThings 접근 권한이 필요합니다.",
                desc2 : "아래 버튼을 누르면 인증을 시작합니다",
                main : "EnerTalk 인증",
                defaultValues : [
                    default1 : "kWh",
                    default2 : "매월 1일",
                    default3 : "주택용 저압"
                ],
                meteringDays : [
                    "매월 1일", 
                    "매월 2일", 
                    "매월 3일",
                    "매월 4일",
                    "매월 5일",
                    "매월 6일",
                    "매월 7일",
                    "매월 8일",
                    "매월 9일",
                    "매월 10일",
                    "매월 11일",
                    "매월 12일",
                    "매월 13일",
                    "매월 14일",
                    "매월 15일",
                    "매월 16일",
                    "매월 17일",
                    "매월 18일",
                    "매월 19일",
                    "매월 20일",
                    "매월 21일",
                    "매월 22일",
                    "매월 23일",
                    "매월 24일",
                    "매월 25일",
                    "매월 26일",
                    "말일"
                ],
                displayUnits : ["원(₩)", "kWh"],
                contractTypes : ["주택용 저압", "주택용 고압"],
                title1 : "알람 설정",
                title2 : "사용 계획 (원)",
                subTitle1 : "월간 계획을 금액으로 입력하세요",
                title3 : "표시 단위",
                title4 : "정기검침일",
                title5 : "계약종별",
                title6 : "사용자 & 알람 설정",
                message1 : """ <p>EnerTalk 계정이 SmartThings와 연결 되었습니다!</p> <p>Done을 눌러 계속 진행해 주세요.</p> """,
                message2 : """ <p>계정 연결이 실패했습니다.</p> <p>Done 버튼을 눌러 다시 시도해주세요.</p> """,
                message3 : [
                    header : "기기 설치가 필요합니다.",
                    body1 : "가정 내 분전반에 EnerTalk 기기를 먼저 설치하고,",
                    body2 : "아래 버튼을 눌러 기기등록 및 연결을 진행하세요.",
                    button1 : "기기 설정",
                    button2 : "설치필요"
                ],
                message4 : [
                    header : "Device is not connected.",
                    body1 : "Please check the Wi-Fi network connection",
                    body2 : "and EnerTalk device status.",
                    body3 : "Select ‘Setup Device’ to reset the device."
                ],
                rankingShow : "데이터 수집 중",
                meteringPeriodBillFalse : "데이터 수집 중",
                lastMonthFalse : "정보가 없습니다", 
                standbyFalse : "데이터 수집 중",
                planFalse : "계획을 입력하세요",
                thisMonthTitle : "이번 달", 
                tierTitle : "누진단계", 
                planTitle : "사용 계획", 
                lastMonthTitle : "지난달", 
                rankingTitle : "랭킹", 
                standbyTitle : "대기전력", 
                energyMonitorDeviceTitle : "스마트미터 상태",
                realtimeTitle : "실시간",

            ]
        ]

}