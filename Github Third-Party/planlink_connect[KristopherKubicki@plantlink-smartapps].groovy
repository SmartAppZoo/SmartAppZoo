/**
 *	PlantLink Service Manager
 *
 *	Author: Dan Widing
 *
 * available functions at https://graph.api.smartthings.com/ide/doc/smartApp
 * http should accept data like http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/HTTPBuilder.RequestConfigDelegate.html#setPropertiesFromMap(java.util.Map)
 */


import groovy.json.JsonBuilder

import java.util.regex.Matcher
import java.util.regex.Pattern

definition(
        name: "PlantLink Connect",
        namespace: "OsoTech",
        author: "Oso Technologies",
        description: "Connect PlantLink to your SmartThings Account",
        category: "",
        iconUrl: "https://oso-tech.appspot.com/images/apple-touch-icon-76x76-precomposed.png",
        iconX2Url: "https://oso-tech.appspot.com/images/apple-touch-icon-120x120-precomposed.png"
)

preferences {
    page(name: "auth", title: "PlantLink", nextPage:"deviceList", content:"authPage", uninstall: true)
    page(name: "deviceList", title: "PlantLink", install:true, uninstall:true){
        section {
            input "plantlinksensors", "capability.sensor", title: "Select PlantLink sensors to connect", multiple: true, required: true
        }
    }
}

mappings {
    path("/swapToken") {
        action: [
                GET: "swapToken"
        ]
    }
}

def authPage()
{
    log.debug "authPage()"

    if(!atomicState.accessToken)
    {
        log.debug "about to create access token"
        createAccessToken() // creates an access token which is needed to be able to hit a url
        atomicState.accessToken = state.accessToken
    }


    def description = "Required"
    def uninstallAllowed = false
    def oauthTokenProvided = false

    if(atomicState.authToken)
    {
        // TODO: Check if it's valid
        if(true)
        {
            description = "You are connected."
            uninstallAllowed = true
            oauthTokenProvided = true
        }
        else
        {
            description = "Required" // Worth differentiating here vs. not having atomicState.authToken?
            oauthTokenProvided = false
        }
    }

    def redirectUrl = oauthInitUrl()

    log.debug "RedirectUrl = ${redirectUrl}"

    if (!oauthTokenProvided) {

        return dynamicPage(name: "auth", title: "Login", nextPage:null, uninstall:uninstallAllowed) {
            section(){
                paragraph "Tap below to log in to PlantLink service and authorize SmartThings access."
                href url:redirectUrl, style:"embedded", required:true, title:"PlantLink", description:description
            }
        }
    } else{
        return dynamicPage(name: "auth", title: "LogIn", nextPage:"deviceList", uninstall:uninstallAllowed) {
            section(){
                paragraph "Tap Next to continue to setup your Sensors."
                href url:redirectUrl, style:"embedded", state:"complete", title:"PlantLink", description:description
            }
        }
    }

    //previously there was an else here for alternate log in that I didn't understand so I removed it

}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    // TODO: subscribe to attributes, devices, locations, etc.
    // subscribe to battery and moisture events from links
    atomicState.attached_sensors = [:]
    if (plantlinksensors){
        log.debug "initialize starting"
        subscribe(plantlinksensors, "moisture_status", moistureHandler)
        subscribe(plantlinksensors, "battery_status", batteryHandler)
        plantlinksensors.each{ sensor_device ->
            sensor_device.setStatusIcon("Waiting on First Measurement")
        }
    }
}

def dock_sensor(device_serial, expected_plant_name) {
    def docking_body_json_builder = new JsonBuilder([version: '1c', smartthings_device_id: device_serial])
    def docking_params = [
            uri        : "https://oso-tech.appspot.com",
            path       : "/api/v1/smartthings/links",
            headers    : ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
            contentType: "application/json",
            body: docking_body_json_builder.toString()
    ]
    def plant_post_body_map = [
            plant_type_key: 999999,
            soil_type_key : 1000004
    ]
    def plant_post_params = [
            uri        : "https://oso-tech.appspot.com",
            path       : "/api/v1/plants",
            headers    : ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
            contentType: "application/json",
    ]
    log.debug "docking - ${expected_plant_name}"
    httpPost(docking_params) { docking_response ->
        if (parse_api_response(docking_response, "Docking a link")) {

            if (docking_response.data.plants.size() == 0) {
                log.debug "creating plant for - ${expected_plant_name}"
                plant_post_body_map["name"] = expected_plant_name
                plant_post_body_map['links_key'] = [docking_response.data.key]
                def plant_post_body_json_builder = new JsonBuilder(plant_post_body_map)
                plant_post_params["body"] = plant_post_body_json_builder.toString()
                httpPost(plant_post_params) { plant_post_response ->
                    if(parse_api_response(plant_post_response, 'creating plant')){
                        def attached_map = atomicState.attached_sensors
                        attached_map[device_serial] = plant_post_response.data
                        atomicState.attached_sensors = attached_map
                    }
                }
            } else {
//                log.debug "checking plant for - ${expected_plant_name}"
                def plant = docking_response.data.plants[0]
                def attached_map = atomicState.attached_sensors
                attached_map[device_serial] = plant
                atomicState.attached_sensors = attached_map
                checkAndUpdatePlantIfNeeded(plant, expected_plant_name)
            }
        }
    }
    return True

}

def checkAndUpdatePlantIfNeeded(plant, expected_plant_name){
    def plant_put_params = [
            uri        : "https://oso-tech.appspot.com",
            headers    : ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
            contentType: "application/json",
    ]
    if (plant.name != expected_plant_name) {
        log.debug "updating plant for - ${expected_plant_name}"
        plant_put_params["path"] = "/api/v1/plants/${plant.key}"
        plant_put_params["body"] = "{name: ${expected_plant_name}}"
        httpPut(plant_put_params) { plant_put_response ->
            parse_api_response(plant_put_response, 'updating plant name')
        }
    }
}

def moistureHandler(event){
//    event documentation found at https://graph.api.smartthings.com/ide/doc/event
//    debugEvent("Received event value on moisture handler ${event.value} attached ${atomicState.attached_sensors}", True)
    log.debug "Received event value on moisture handler ${event.value}"
    def expected_plant_name = "SmartThings - ${event.displayName}"
    def device_serial = getDeviceSerialFromEvent(event)
    if (!atomicState.attached_sensors.containsKey(device_serial)){
        dock_sensor(device_serial, expected_plant_name)
    }
    else{
        def measurement_post_params = [
                uri: "https://oso-tech.appspot.com",
                path: "/api/v1/smartthings/links/${device_serial}/measurements",
                headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
                contentType: "application/json",
                body: event.value
        ]
        httpPost(measurement_post_params) { measurement_post_response ->
            if (parse_api_response(measurement_post_response, 'creating moisture measurement') &&
                    measurement_post_response.data.size() >0){
                def measurement = measurement_post_response.data[0]
                def plant =  measurement.plant
                checkAndUpdatePlantIfNeeded(plant, expected_plant_name)
                plantlinksensors.each{ sensor_device ->
                    if (sensor_device.id == event.deviceId){
                        sensor_device.setStatusIcon(plant.status)
                        if (plant.last_measurements && plant.last_measurements[0].plant_fuel_level){
                            sensor_device.setPlantFuelLevel(plant.last_measurements[0].plant_fuel_level * 100)
                        }
                        if (plant.last_measurements && plant.last_measurements[0].battery){
                            sensor_device.setBatteryLevel(plant.last_measurements[0].battery * 100)
                        }
                    }
                }
            }
        }
    }
}

def batteryHandler(event){
    // send to api
//    log.debug "Received event value on batteryHandler ${event.value} attached ${atomicState.attached_sensors}"
    def expected_plant_name = "SmartThings - ${event.displayName}"
    def device_serial = getDeviceSerialFromEvent(event)
    if (!atomicState.attached_sensors.containsKey(device_serial)){
        dock_sensor(device_serial, expected_plant_name)
    }
    else{
        def measurement_post_params = [
                uri: "https://oso-tech.appspot.com",
                path: "/api/v1/smartthings/links/${device_serial}/measurements",
                headers: ["Content-Type": "application/json", "Authorization": "Bearer ${atomicState.authToken}"],
                contentType: "application/json",
                body: event.value
        ]
        httpPost(measurement_post_params) { measurement_post_response ->
            parse_api_response(measurement_post_response, 'creating battery measurement')
        }
    }
}

def getDeviceSerialFromEvent(event){
    def pattern = /.*"zigbeedeviceid"\s*:\s*"(\w+)".*/
    def match_result = (event.value =~ pattern)
    return match_result[0][1]
}

def oauthInitUrl()
{
    //done
    log.debug "oauthInitUrl"
    // def oauth_url = "https://api.PlantLink.com/authorize?response_type=code&client_id=qqwy6qo0c2lhTZGytelkQ5o8vlHgRsrO&redirect_uri=http://localhost/&scope=smartRead,smartWrite&state=abc123"

    atomicState.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
            response_type: "code",
            client_id: getSmartThingsClientId(),
            state: atomicState.oauthInitState,
            redirect_uri: buildRedirectUrl()
    ]
    return "https://oso-tech.appspot.com/oauth/oauth2/authorize?" + toQueryString(oauthParams)
}

def buildRedirectUrl()
{
    log.debug "buildRedirectUrl"
    // return serverUrl + "/api/smartapps/installations/${app.id}/token/${atomicState.accessToken}"
    return getServerUrl() + "/api/token/${atomicState.accessToken}/smartapps/installations/${app.id}/swapToken"
}

def swapToken()
{
    log.debug "swapping token: $params"
    debugEvent ("swapping token: $params", true)

    def code = params.code
    def oauthState = params.state


    // https://www.PlantLink.com/home/token?grant_type=authorization_code&code=aliOpagDm3BqbRplugcs1AwdJE0ohxdB&client_id=qqwy6qo0c2lhTZGytelkQ5o8vlHgRsrO&redirect_uri=https://graph.api.smartthings.com/
    def stcid = getSmartThingsClientId()


    def postParams = [
            method: 'POST',
            uri: "https://oso-tech.appspot.com",
            path: "/api/v1/oauth-token",
            query: [grant_type:'authorization_code', code:params.code, client_id:stcid,
                    client_secret:getSmartThingsSecretKey(), redirect_uri: buildRedirectUrl()],
    ]

    log.debug "SCOTT: swapping token $params"

    def jsonMap
    httpPost(postParams) { resp ->
        jsonMap = resp.data
    }

    log.debug "SCOTT: swapped token for $jsonMap"
    debugEvent ("swapped token for $jsonMap", true)

    atomicState.refreshToken = jsonMap.refresh_token
    atomicState.authToken = jsonMap.access_token

    def html = """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=640">
<title>Withings Connection</title>
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
		width: 560px;
		padding: 40px;
		/*background: #eee;*/
		text-align: center;
	}
	img {
		vertical-align: middle;
	}
	img:nth-child(2) {
		margin: 0 30px;
	}
	p {
		font-size: 2.2em;
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
		<img src="https://oso-tech.appspot.com/images/PLlogo.png" alt="PlantLink icon" height="215" width="215"/>
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png" alt="connected device icon" />
		<img src="https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png" alt="SmartThings logo" />
		<p>Your PlantLink Account is now connected to SmartThings!</p>
		<p>Click 'Done' to finish setup.</p>
	</div>
</body>
</html>
"""

    render contentType: 'text/html', data: html
}

private refreshAuthToken() {
    log.debug "refreshing auth token"
    debugEvent("refreshing OAUTH token", true)

    def stcid = getSmartThingsClientId()

    def refreshParams = [
            method: 'POST',
            uri: "https://oso-tech.appspot.com",
            path: "/api/v1/oauth-token",
            query: [grant_type:'refresh_token', code:"${atomicState.refreshToken}", client_id:stcid,
                    client_secret:getSmartThingsSecretKey()],
    ]

    //changed to httpPostJson
    try{
        def jsonMap
        httpPost(refreshParams) { resp ->
            if(resp.status == 200)
            {
                log.debug "Token refreshed...calling saved RestAction now!"

                debugEvent("Token refreshed ... calling saved RestAction now!", true)

                log.debug resp

                jsonMap = resp.data

                if (resp.data) {

                    log.debug resp.data
                    debugEvent ("Response = ${resp.data}", true)

                    atomicState.refreshToken = resp?.data?.refresh_token
                    atomicState.authToken = resp?.data?.access_token

                    debugEvent ("Refresh Token = ${atomicState.refreshToken}", true)
                    debugEvent ("OAUTH Token = ${atomicState.authToken}", true)

                    if (data?.action && data?.action != "") {
                        log.debug data.action

                        "{data.action}"()

                        //remove saved action
                        data.action = ""
                    }

                }
                data.action = ""
            }
            else
            {
                log.debug "refresh failed ${resp.status} : ${resp.status.code}"
            }
        }

        // atomicState.refreshToken = jsonMap.refresh_token
        // atomicState.authToken = jsonMap.access_token
    }
    catch(Exception e)
    {
        log.debug "caught exception refreshing auth token: " + e
    }
}

def parse_api_response(resp, message) {
    if (resp.status == 200) {
        return true
    } else {
        log.error "sent ${message} Json & got http status ${resp.status} - ${resp.status.code}"
        debugEvent("sent ${message} Json & got http status ${resp.status} - ${resp.status.code}", true)

        //refresh the auth token
        if (resp.status == 401) {
            //log.debug "Storing the failed action to try later"
            log.debug "Refreshing your auth_token!"
            debugEvent("Refreshing OAUTH Token", true)
            refreshAuthToken()
            return false
        } else {
            debugEvent("Authentication error, invalid authentication method, lack of credentials, etc.", true)
            return false
        }
    }
}

def getServerUrl() { return "https://graph.api.smartthings.com" }
def getSmartThingsClientId() { return "6479182133460992" }
def getSmartThingsSecretKey() { return "a0c318b6-042f-4a91-8f56-654a6cc37c9a" }

def debugEvent(message, displayEvent) {

    def results = [
            name: "appdebug",
            descriptionText: message,
            displayed: displayEvent
    ]
    log.debug "Generating AppDebug Event: ${results}"
    sendEvent (results)

}

def toQueryString(Map m)
{
    return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}