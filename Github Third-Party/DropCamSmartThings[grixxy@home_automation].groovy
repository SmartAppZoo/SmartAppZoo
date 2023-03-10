/**
 *  Nest Integration
 *
 *  Copyright 2016 Gregory
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
        name: "Nest Integration",
        namespace: "grixxy",
        author: "Gregory",
        description: "App to switch on or off the cam",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        oauth: true)



preferences {
    section("When all of these people leave home") {
        input "people", "capability.presenceSensor", multiple: true
        input("username", "text", title: "Username", description: "Your Nest username (usually an email address)")
        input("password", "password", title: "Password", description: "Your Nest password")
    }
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
    subscribe(people, "presence", presenceHandler)
}

def presenceHandler(evt) {
    log.debug "evt.name: $evt.value"
    log.debug "checking if everyone is away"

    if (everyoneIsAway()) {
        turnCameraOn('true')
    } else {
        turnCameraOn('false')
    }

}


// returns true if all configured sensors are not present,
// false otherwise.
private everyoneIsAway() {
    def result = true
    // iterate over our people variable that we defined
    // in the preferences method
    for (person in people) {
        if (person.currentPresence == "present") {
            // someone is present, so set our our result
            // variable to false and terminate the loop.
            result = false
            break
        }
    }
    log.debug "everyoneIsAway: $result"
    return result
}


//*******************************************************

def turnCameraOn(turnCameraOn) {
    nestLogin()
    dropCamLogin()
    getCameraInfo()
    turnItOn(turnCameraOn)
}

def nestLogin(){
    def params = [
            uri: 'https://home.nest.com/user/login',
            body: [username: settings.username, password: settings.password]
    ]
    httpPost(params) {response ->
        state.auth_params = response.data
        log.debug "Nest Login status: ${response.statusLine}"
        state.cookies = response.getHeaders('Set-Cookie')[0].getValue()
    }
}


def dropCamLogin(){
    def params = [
            uri: 'https://home.nest.com/dropcam/api/login',
            body: [access_token: state.auth_params.access_token],
            headers: [
                    "Accept":"*/*",
                    "Accept-Encoding":"gzip, deflate",
                    "Accept-Language":"en-US,en;q=0.8,ru;q=0.6",
                    "Connection":"keep-alive",
                    "Content-Type":"application/x-www-form-urlencoded; charset=UTF-8",
                    "Cookie":state.cookies,
                    "Host":"home.nest.com",
                    "Origin":"https://home.nest.com",
                    "Referer":"https://home.nest.com/",
                    "User-Agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36",
                    "X-Requested-With":"XMLHttpRequest"
            ]
    ]

    httpPost(params) {response ->
        log.debug "Drop Cam Login status: ${response.statusLine}"
        state.cookies = state.cookies + "; "+ response.getHeaders('Set-Cookie')[0].getValue()
    }
}


def getCameraInfo(){
    def params = [
            uri: 'https://home.nest.com/dropcam/api/cameras',
            headers: [
                    "Accept":"*/*",
                    "Accept-Encoding":"gzip, deflate, sdch",
                    "Accept-Language":"en-US,en;q=0.8,ru;q=0.6",
                    "Connection":"keep-alive",
                    "Cookie":state.cookies,
                    "Host":"home.nest.com",
                    "Referer":"https://home.nest.com/",
                    "User-Agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36",
                    "X-Requested-With":"XMLHttpRequest"
            ]
    ]

    httpGet(params) {response ->
        log.debug "Get Cam Info status: ${response.statusLine}"
        state.camera_info = response.data
    }
}

def turnItOn(turnCameraOn){
    def uuid = state.camera_info[0].get("uuid")
    def key = 'streaming.enabled'

    def params = [
            uri: "https://home.nest.com/dropcam/api/cameras/${uuid}/properties",
            body: [uuid:uuid, key:key, value:turnCameraOn],
            headers: [
                    "Accept":"*/*",
                    "Accept-Encoding":"gzip, deflate, sdch",
                    "Accept-Language":"en-US,en;q=0.8,ru;q=0.6",
                    "Connection":"keep-alive",
                    "Cookie":state.cookies,
                    "Host":"home.nest.com",
                    "Referer":"https://home.nest.com/",
                    "User-Agent":"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 Safari/537.36",
                    "X-Requested-With":"XMLHttpRequest"
            ]
    ]

    httpPost(params) {response ->
        log.debug 'Turning on/off status: '
        log.debug response.statusLine
    }


}