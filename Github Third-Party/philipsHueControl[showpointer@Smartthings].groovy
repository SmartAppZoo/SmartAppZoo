/**
 *  AlarmVentilation
 *
 *  Copyright 2017 Wanho
 *
 */
definition(
        name: "AlarmVentilation",
        namespace: "showpointer",
        author: "wanho92@gmail.com",
        description: "HueBulbControl",
        category: "SmartThings Labs",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

import groovy.json.JsonBuilder

preferences {
    section("Title") {
        // TODO: put inputs here
    }
}

def installed(){
    log.debug "Installed with settings: ${settings}"
    initialize()

}

def updated(){
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
}

def initialize(){
    // First executed custom function
    turnOn()
}

def turnOn(){

    //hue hub ip setting
    def ip = "192.168.0.2:80"

    //saturation, brighness, hue initialization
    def sat = 0
    def bri = 0
    def hue = 0


    def alert = false

    //color setting
    if(alert){
        log.debug "green"
        //set green
        sat = 150
        bri = 10
        hue = 25000

    } else{
        log.debug "red"
        //set red
        sat = 180
        bri = 30
        hue = 65536
    }

    def json = "{\"on\":true,\"sat\":${sat},\"bri\":${bri},\"hue\":${hue}}"
    log.debug json
    def headers = [:]
    headers.put("HOST",ip)
    headers.put("Content-Type","application/json")

    //philips Hue Bulb Control
    def result = sendHubCommand(new physicalgraph.device.HubAction(
            method: "PUT",
            path: "/api/newdeveloper/lights/4/state",
            headers: headers,
            body: json
    ))
    result
}
