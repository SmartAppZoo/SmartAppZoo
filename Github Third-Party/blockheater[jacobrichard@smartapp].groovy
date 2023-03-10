definition(
    name: "Truck Outlet",
    namespace: "jrichard",
    author: "Jacob Richard",
    description: "Switch Truck Outlet Based on Weather",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Turn on Which Switch"){
        input "truck", "capability.switch", multiple: false
    }     
    
    section("Turn on At...") {
        input "timeon", "time", title:"When?"
    }
    
    section("Turn off At...") {
        input "timeoff", "time", title:"When?"
    }
    
    section("Set Temperature...") {
        input "temperature", "number", title:"Temp?"
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    schedule(timeon, "handleOutletOn")
    schedule(timeoff, "handleOutletOff")
}

def handleOutletOn() {
    def weather = getWeatherFeature("conditions")
    def current_temp = weather.current_observation.temp_f
    if (current_temp < temperature) {
        sendNotificationEvent("Turning on Truck Block Heater")    
        truck.on()
    }
}

def handleOutletOff() {
    sendNotificationEvent("Turning off Truck Block Heater")                
    truck.off()
}
