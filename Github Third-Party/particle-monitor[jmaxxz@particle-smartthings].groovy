/**
*  Particle Monitor
*
*  Copyright 2018 Jmaxxz
*
* MIT License
* Copyright (c) 2018 [Jmaxxz](jmaxxz.com)
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*
*/
definition(
    name: "Particle Monitor",
    namespace: "jmaxxz",
    author: "jmaxxz",
    description: "Interact with particles",
    category: "",
    iconUrl: "https://pbs.twimg.com/profile_images/2669963047/2c84fdfd3e3edbe631a91650094f2267_400x400.png",
    iconX2Url: "https://pbs.twimg.com/profile_images/2669963047/2c84fdfd3e3edbe631a91650094f2267_400x400.png",
    iconX3Url: "https://pbs.twimg.com/profile_images/2669963047/2c84fdfd3e3edbe631a91650094f2267_400x400.png")
{
    appSetting "particleToken"
}

preferences {
    page(name: "authTokenEntry")
    page(name:"selectParticles")
}

def authTokenEntry(){

	dynamicPage(name: "authTokenEntry", title: "Particle Api token", nextPage: "selectParticles", install: false, uninstall: true){
    	section {
            paragraph "In order to communicate with the particle cloud you will need an api token. It is recommend you use a token which will never expire."
            input "particleToken", type:"password", required: true, capitalization: "none", defaultValue: appSettings.particleToken, title: "Particle Api Token?"
        }
    }
}

def selectParticles(){
    def devices = getParticles()
    dynamicPage(name:"selectParticles", title: "", install: true, uninstall: true){
        section("Select the particle devices you would like to use"){
            input(name: "particleDevices", type: "enum", title: "Select Devices", required: false, multiple: true, options: devices)
        }
    }
}


def getParticles(){
    def results = [:]
    httpGet(uri:"https://api.particle.io/v1/devices?access_token=${settings.particleToken}", {
        response -> response.data.each { device ->
            results.put(device.id, device.name)
        }
    })
    return results;
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
    checkWebhook()
    updateChildDevices()
}

void updateChildDevices(){
    particleDevices.each{ particle->
        def myDevice = getChildDevice(particle)
        if(!myDevice){
            addDevice(particle)
        }
    }
}

def addDevice(id){
    def result = []
    httpGet(uri:"https://api.particle.io/v1/devices/${id}?access_token=${settings.particleToken}", {
        deviceDetails->
        httpGet(uri:"https://api.particle.io/v1/devices/${id}/devhandler?access_token=${settings.particleToken}", {
            response ->
            try{
                result = addChildDevice("particle",  response.data.result, "${id}", null, [label: deviceDetails.data.name, name:"Particle.${id}" ])
            } catch(ex) {
                log.debug "Adding generic particle handler for ${deviceDetails.data.name} because ${response.data.result} is not a supported device type"
                result = addChildDevice("particle",  "Generic Particle", "${id}", null, [label: deviceDetails.data.name, name:"Particle.${id}" ])
            }
        });
    });
    return result;
}


// Check to see if we need to make the webhook or if it is there already
void checkWebhook() {
    def devicesMissingHooks = particleDevices.collect()
    httpGet(uri:"https://api.particle.io/v1/webhooks?access_token=${settings.particleToken}", {
        response ->
        response.data.each {
            hook ->
            if (hook.url == "${apiServerUrl("")}api/smartapps/installations/${app.id}/updatestate") {
                if(devicesMissingHooks.contains(hook.deviceID)){
                    devicesMissingHooks -= hook.deviceID
                    log.info "Found existing webhook id: ${hook.id}"
                } else {
                    log.info "Found extra webhook id: ${hook.id}"
                    deleteWebhook(hook.id)
                }
            }
        }
        devicesMissingHooks.each{
            d -> createWebhook(d)
        }
    })
}

mappings {
    path("/updatestate") {
        action: [
            POST: "handleStateUpdate"
        ]
    }
}

void deleteWebhook(hookid) {
    httpDelete(uri: "https://api.particle.io/v1/integrations/${hookid}?access_token=${settings.particleToken}") {response -> log.debug "Delete hook response ${response.data}"}
}

// Create a new particle webhook for this app to use
void createWebhook(device) {
    if(!state.accessToken) {
        // the createAccessToken() method will store the access token in state.accessToken
        createAccessToken()
    }
    httpPostJson(uri: "https://api.particle.io/v1/integrations?access_token=${settings.particleToken}",
                 body: new groovy.json.JsonBuilder([
                     integration_type:"Webhook",
                     event: "state-update", // The name of the event raised by the particle device
                     url: "${apiServerUrl("")}api/smartapps/installations/${app.id}/updatestate",
                     deviceID: device,
                     requestType: "POST",
                     headers: [Authorization :"Bearer ${state.accessToken}"]
                 ]).toPrettyString()
                ) {response -> "Create hook response ${log.debug response.data}"}

}

def invoke(childDevice, String func = "", String msg= "") {
	def result= [];
    log.debug "Calling  ${childDevice}"
    httpPostJson(uri: "https://api.particle.io/v1/devices/${childDevice.device.deviceNetworkId}/${func}?access_token=${settings.particleToken}",
                 body: [
                     arg:msg,
                 ]
                ) {response -> 
                result = response.data.return_value;
                log.debug "Called ${func} for ${childDevice.device.name} response ${result}"   
                }
    return result;
}

def getVariable(childDevice, String variable = "") {
    def result = [];
    log.debug "Calling  ${childDevice}"
    httpGet(uri:"https://api.particle.io/v1/devices/${childDevice.device.deviceNetworkId}/${variable}?access_token=${settings.particleToken}", {
        response ->
        result = response.data.result
    })

    return result;
}


def handleStateUpdate() {
    def slurper = new groovy.json.JsonSlurper()
    def update = [
        published_at: Date.parse("yyyy-MM-dd'T'HH:mm:ss.S'Z'", params.published_at),
        coreid: params.coreid,
        event: params.event,
        data:slurper.parseText(params.data)
    ]
    log.debug "Got update ${update}"

    def device = getChildDevice(update.coreid)
    if(device != null){
        device.handleStateUpdate(update)
    } else {
        addDevice(update.coreid)
    }
    return [Respond: "OK"]
}
