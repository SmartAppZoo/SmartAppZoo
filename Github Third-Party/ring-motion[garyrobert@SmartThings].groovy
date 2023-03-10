/**
 *  Ring Motion Zones
 *
 *  Copyright 2019 Gary Robert
 *  Version 1.0 3/4/19
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
definition(name: "Ring Motion Zones", namespace: "", author: "Gary Robert",
    description: "Enable/Disable Ring Motion Zones", category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences 
{
    page(name: "getPrefrences")
}

def getPrefrences()
{
    dynamicPage(name: "getPrefrences", title: "Enter your Ring.com Credentials", install:true, uninstall: true)
    {
        section("Provide your Ring.com credentials.)")
        {
            input name: "refreshToken", type: "password", title: "Ring Refresh Token", description: "Enter token", required: true
            input name: "doorBellId", type: "text", title: "Doorbot ID", description: "Enter doorbot id", required: true
        }

        section("Zone 1 Switch")
        {
            input "zone1", "capability.switch"
        }
        
        section( "Notifications" ) 
        {
            input("recipients", "contact", title: "Send notifications to") 
            {
                input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
                input "phoneNumber", "phone", title: "Send a text message?", required: false
            }
        }
    }
}

page(name: "pageAbout", title: "About ${textAppName()}") 
{
    section
    {
        paragraph "${textVersion()}\n${textCopyright()}\n\n${textLicense()}\n"
    }
    
    section("Instructions")
    {
        paragraph textHelp()
    }
}

def installed() 
{
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated()
{
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() 
{
    subscribe(zone1, "switch.on", enableMotion)
    subscribe(zone1, "switch.off", disableMotion)
}

def enableMotion(evt)
{
    getAccessToken()
    enableZone()
    sendNotifications "Motion Zone 1 Enabled"
}

def disableMotion(evt)
{
    getAccessToken()
    disableZone()
    sendNotifications "Motion Zone 1 Disabled"
}

def getAccessToken() 
{
    def params = [
        uri: "https://oauth.ring.com/oauth/token",
        contentType: "application/json",
        body: [
            client_id: "ring_official_android",
            grant_type: "refresh_token",
            scope: "client",
            refresh_token: refreshToken
        ]
    ]

    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            state.accessToken = "${resp.data.access_token}"
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

def enableZone() 
{
    log.debug "Enabling zone!"
    def params = [
        uri: "https://api.ring.com/clients_api/doorbots/",
        path: doorBellId,
        query: ['doorbot[description]':'Front Door', 'doorbot[settings][motion_zones][zone1][state]': '2'],
        contentType: "application/json",
        headers: [ Authorization : "Bearer $state.accessToken" ]
    ]

    try {
        httpPutJson(params) { resp ->
            resp.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

def disableZone() 
{
    log.debug "Enabling zone!"
    def params = [
        uri: "https://api.ring.com/clients_api/doorbots/",
        path: doorBellId,
        query: ['doorbot[description]':'Front Door', 'doorbot[settings][motion_zones][zone1][state]': '0'],
        contentType: "application/json",
        headers: [ Authorization : "Bearer $state.accessToken" ]
    ]

    try {
        httpPutJson(params) { resp ->
            resp.headers.each {
                log.debug "${it.name} : ${it.value}"
            }
            log.debug "Enabled Motion Zone 1"
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

def sendNotifications(message)
{
    if (settings.sendPushMessage == "Yes")
    {
        log.debug("sending push message")
        sendPush(message)
    }

    if (settings.phoneNumber)
    {
        log.debug("sending text message")
        sendSms(phoneNumber, message)
    }
}

//Version/Copyright/Information/Help

private def textAppName() 
{
    def text = "Ring Motion Zones"
}    

private def textVersion() 
{
    def text = "Version 1.1 (3/10/20)"
}

private def textCopyright() 
{
    def text = "Copyright © 2019 Gary Robert"
}

private def textLicense() 
{
    def text =
        "Licensed under the Apache License, Version 2.0 (the 'License'); "+
        "you may not use this file except in compliance with the License. "+
        "You may obtain a copy of the License at"+
        "\n\n"+
        "    http://www.apache.org/licenses/LICENSE-2.0"+
        "\n\n"+
        "Unless required by applicable law or agreed to in writing, software "+
        "distributed under the License is distributed on an 'AS IS' BASIS, "+
        "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
        "See the License for the specific language governing permissions and "+
        "limitations under the License."
}

private def textHelp() 
{
    def text =
        "Enable/Disable Ring Motion Zones" +
        "v1 only supports a single zone" 

}