/*
 *  Copyright 2021 Roymam
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
 *  Tami 4 Edge Smart App for Smarthings
 *
 *  Author: Lior Tamam
 */

definition(
    name: "Tami 4 Edge",
    namespace: "roymam",
    author: "Lior Tamam",
    description: "Boil the water on your Tami 4 Edge device",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section("Step 1") {
        paragraph title: "Login to Tami 4",
                  required: true,
                  "Go to https://www.tami4.co.il/my-area on your browser, and paste the following text on the address bar:"
        input "code", "text", title: "Code", defaultValue:"javascript:getRecaptchaResponse('otp_submit_form', (token) => { navigator.clipboard.writeText(token) })"
        paragraph "make sure to include the \"javascript:\" prefix after pasting."
        paragraph "after running the script, a token will be copied to your clipboard, paste it here:"
    	input "token1", "text", title:"First Access Token", required: true
        input "phoneNum", "text", title:"Your phone number", required: true, description:"+972", defaultValue:"+972"
        paragraph "Click \"Done\" to complete installation of the SmartApp"
    }
    section("Step 2") {
    	paragraph "Run this step, after clicking \"Done\" on install screen. "
		paragraph title: "Enter OTP Code",
                  "Go again to https://www.tami4.co.il/my-area on your browser and perform the same steps again to get a second token"
        paragraph "after running the script, a token will be copied to your clipboard, paste it here:"
    	input "token2", "text", title:"Second Access Token", required: false
        input "otp", "text", title:"The OTP code your received on SMS", required: false
	}
}

def installed()
{
	log.debug "installed"
  sendOTP()
}

def updated()
{
	log.debug "updated"
  enterOTP()
  getAllChildDevices().each { device ->
    log.info "Removing device: ${device.deviceNetworkId}"
    deleteChildDevice(device.deviceNetworkId)
  }
  addChildDevices()
}

def addChildDevices() {
	// refresh token 
	runRefreshToken(refreshToken)
    
    // get list of devices
    def devices = runRemoteAction("GET", "device", "")
    log.info devices
    devices.each { device -> 
    	def deviceId = device["id"]
        log.info "Adding device: ${device["name"]}"
        addChildDevice("roymam","Tami 4 Edge Device", deviceId, "", ["refreshToken":refreshToken])
    }
}

def sendOTP() {
	def params = [	uri: "https://swelcustomers.strauss-water.com",
    			 	path: "/public/phone/generateOTP",
                    body: "{\"phoneNumber\": \"$phoneNum\", \"reCaptchaToken\": \"$token1\"}",
                    contentType: "application/json",
                 	headers: [ "Accept": "/",],
                    requestContentType: "application/json"]
    try {
		httpPost(params) { resp ->
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def enterOTP() {
	def params = [	uri: "https://swelcustomers.strauss-water.com",
    			 	path: "/public/phone/submitOTP",
                    body: "{\"phoneNumber\": \"$phoneNum\", \"reCaptchaToken\": \"$token2\", \"code\":\"$otp\"}",
                    contentType: "application/json",
                 	headers: [ "Accept": "/",],
                    requestContentType: "application/json"]
    try {
		httpPost(params) { resp ->
            log.debug "response data: ${resp.data}"
            log.debug "refresh token: ${resp.data["refresh_token"]}"
            state.accessToken = resp.data["access_token"]
            state.refreshToken = resp.data["refresh_token"]
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def runRefreshToken(refreshToken) {
	def params = [	uri: "https://swelcustomers.strauss-water.com",
    			 	path: "/public/token/refresh",
                    body: "{\"token\": \"$refreshToken\"}",
                    contentType: "application/json",
                 	headers: [ "Accept": "/",],
                    requestContentType: "application/json"]
    def result = ""
    
    try {
		httpPost(params) { resp ->
            //log.debug "response data: ${resp.data}"
            //log.debug "access token: ${resp.data["access_token"]}"
            log.debug "refresh token: ${resp.data["refresh_token"]}"
            state.accessToken = resp.data["access_token"]
            state.refreshToken = resp.data["refresh_token"]
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
    return result
}

def runRemoteAction(method, action, body) {
	def params = [	uri: "https://swelcustomers.strauss-water.com",
    			 	path: "/api/v1/$action",
                    body: body,
                 	headers: [
                        "Accept": "/",
                        "Authorization": "Bearer ${state.accessToken}"
                     ]]
    def result = ""
    
    try {
		if (method == "GET") {
        	httpGet(params) { resp ->
            	log.debug "response data: ${resp.data}"
            	result = resp.data
            }
        } else {
        	httpPost(params) { resp ->
            log.debug "response data: ${resp.data}"
            	result = resp.data
            }
        }
    } catch (e) {
        log.error "something went wrong: ${e.getStatusCode()}"
    }
    return result
}