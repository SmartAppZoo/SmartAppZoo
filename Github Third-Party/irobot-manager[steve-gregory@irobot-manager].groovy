/**
 *  Copyright 2016 steve-gregory
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

definition(
    name: "iRobot Manager (Connect)",
    namespace: "steve-gregory",
    author: "Steve Gregory",
    description: "Integrate your iRobot with SmartThings.",
    category: "SmartThings Labs",
    iconUrl: "https://raw.githubusercontent.com/steve-gregory/irobot-manager/master/Images/roomba-manager-small.jpg",
    iconX2Url: "https://raw.githubusercontent.com/steve-gregory/irobot-manager/master/Images/roomba-manager-small.jpg",
    iconX3Url: "https://raw.githubusercontent.com/steve-gregory/irobot-manager/master/Images/roomba-manager-small.jpg"
)

preferences {
    page(name: "initializeRobots", title: "iRobot")
    page(name: "selectRobot", title: "iRobot")
}

def initializeRobots() {
    log.info "Enter initializeRobots"
    def showUninstall = username != null && password != null
    return dynamicPage(name: "initializeRobots", title: "Connect your iRobot", install:true, uninstall:showUninstall) {
        section("What is your Robots IP address _or_ username/password:") {
            input "ipaddress", "text", title: "IP Address", required:false, autoCorrect:false
            input "username", "text", title: "Username/blid", autoCorrect:false
            input "password", "password", title: "password", autoCorrect:false
        }
        section("To use iRobot, SmartThings encrypts and securely stores your iRobot credentials.") {}
    }
}

private ipAddressToPasswd() {
    def jsonStr = '{"do":"get","args":["passwd"],"id":1}'
    def loginParams = [
        uri: "https://${ipaddress}",
        path: "/umi",
        headers: [
            'User-Agent': 'aspen%20production/2618 CFNetwork/758.3.15 Darwin/15.4.0',
            Accept: "*/*",
            contentType: "application/json",
            "Content-Encoding": "identity",
            Connection: "close",
            Host: "${ipaddress}",
            'Accept-Language': 'en-us',
            'ASSET-ID': state.AssetID,
        ],
        body: "${jsonStr}"
    ]

    def result = [success:false, data: [], reason: ""]

    try {
      httpPost(loginParams) { resp ->
          if (resp.status == 200) {
              log.debug "response contentType: ${resp.contentType}"
              log.debug "response data: ${resp.data}"
              log.debug "response Headers:" + resp.headers.collect { "${it.name}:${it.value}" }
              result.success = true
              result.data = resp.data
          } else {
              // ERROR: any more information we can give?
              result.reason = "Bad login"
          }
      }
    } catch (groovyx.net.http.HttpResponseException e) {
        result.reason = "Error on login"
    }
    return result
}

private ipAddressToUsername() {
    def encoded_authorization = "user:${password}".bytes.encodeBase64()
    def jsonStr = '{"do":"get","args":["sys"],"id":2}'
    def loginParams = [
        uri: "https://${ipaddress}",
        path: "/umi",
        headers: [
            'User-Agent': 'aspen%20production/2618 CFNetwork/758.3.15 Darwin/15.4.0',
            Authorization: "${encoded_authorization}",
            Accept: '*/*',
            contentType: "application/json",
            "Content-Encoding": "identity",
            Connection: "close",
            Host: "${ipaddress}",
            'Accept-Language': 'en-us',
            'ASSET-ID': state.AssetID,
        ],
        body: "${jsonStr}"
    ]

    def result = [success:false, data: [], reason: ""]

    try {
      httpPost(loginParams) { resp ->
          if (resp.status == 200) {
              log.debug "response contentType: ${resp.contentType}"
              log.debug "response data: ${resp.data}"
              /*Example:
              {"ok":{"umi":2,"pid":2,"blid":[43,6,75,31,32,127,12,132],"sw":"v1.2.9","cfg":0,"boot":4042,"main":4313,"wifi":517,"nav":"01.08.04","ui":2996,"audio":32,"bat":"lith"},"id":2}
              */
              log.debug "response Headers:" + resp.headers.collect { "${it.name}:${it.value}" }
              result.success = true
              result.data = resp.data
              log.debug "Username collected: ${username}"
              username = resp.data.blid.collect( it.toHexString()).join("")
          } else {
              // ERROR: any more information we can give?
              result.reason = "Bad login"
          }
      }
    } catch (groovyx.net.http.HttpResponseException e) {
        result.reason = "Error on login"
    }
    return result
}
private doLogin() {
    log.info "Enter doLogin()"
    // Path (No changes required)
    def request_host = "https://irobot.axeda.com"
    def request_path = "/services/v1/rest/Scripto/execute/AspenApiRequest"
    def request_query = "?blid=${username}&robotpwd=${password}&method=getStatus"
    def encoded_str = "${username}:${password}".bytes.encodeBase64()
    def AssetID = "ElPaso@irobot!${username}"
    def Authorization = "${encoded_str}"
    // Query manipulation

    def requestURI = "${request_host}${request_path}${request_query}"
    def httpRequest = [
        method:"GET",
        uri: "${requestURI}",
        headers: [
            'User-Agent': 'aspen%20production/2618 CFNetwork/758.3.15 Darwin/15.4.0',
            Accept: '*/*',
            Authorization: "${Authorization}",
            'Accept-Language': 'en-us',
            'ASSET-ID': "${AssetID}",
        ]
    ]
    def result = [success:false, data: [], reason: ""]
    try {
        httpGet(httpRequest) { resp ->
            log.debug "Login response Headers:" + resp.headers.collect { "${it.name}:${it.value}" }
            log.debug "Login response contentType: ${resp.contentType}"
            log.debug "Login response data: ${resp.data}"
            def robotName = resp.data.robotName
            log.debug "response data.robotName: ${robotName}"
            result.data = resp.data
            result.success = true
            log.info "Login response result success."
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
    return result
}

def installed() {
    log.info "Enter Installed()"
    initialize()
}

def updated() {
    log.info "Enter Updated()"
    unsubscribe()
    initialize()
}

def initialize() {
    log.info "Enter initialize()"
    addDevice()

}

//CHILD DEVICE METHODS
def addDevice() {
    log.info "Enter addDevice"
    def item = getRobotDevice()
    log.info "Adding childs $item"
    try {
        def d = getChildDevice(item.dni)
        def props = [name:"iRobot Roomba", label: item.name, preferences:[pollInterval:4, roomba_username:username, roomba_password:password], completedSetup: true]
        if(!d) {
            d = addChildDevice(app.namespace, "iRobot Roomba", item.dni, null, props)
            log.warn "created ${d.name} with id ${item.dni} and ${d.properties} that should be == ${props}"
        } else {
            log.info "found ${d.name} with id ${item.dni} already exists with ${d.properties}"
            
        }
        log.info "attempting to update device"
        d.update()
        d.updated()
        
    } catch(e) {
        log.error "Error creating device: ${e}"
    }
}

def getRobotDevice() {
    log.info "Enter getRobotDevice"
    def result = doLogin()
    log.info "getRobotDevice result ${result}"
    def robotName = result.data.robotName
    log.info "getRobotDevice robotName ${robotName}"
    def device = ["name" : "${robotName}", "dni" : "irobot-roomba-9xx-${robotName}"]
    return device
}
