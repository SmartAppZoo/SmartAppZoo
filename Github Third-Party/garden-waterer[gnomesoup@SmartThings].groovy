/*
 * Garden Waterer
 *
 * Copywright 2018 Michael J Pfammatter
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
definition(
    name: "Garden Waterer",
    namespace: "GnomeSoup",
    author: "Michael J Pfammatter",
    description: "Turn on a hose valve when the soil moisture content drops. v0.0.1",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Pets/App-Does___HaveWater.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Pets/App-Does___HaveWater@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Pets/App-Does___HaveWater@2x.png"
)

preferences {
    section("Garden Waterer") {
        paragraph("Version 0.0.1")
    }
    section("Select a soil moisture sensor") {
        input(name:"soil1", type:"capability.relativeHumidityMeasurement",
              title:"Soil moisture sensor...")
    }
    section("Select a hose valve switch to turn on") {
        input(name:"switch1", type:"capability.switch")
    }
    section("Turn on the hose when the soil moisture drops to:") {
        input(name:"soilMin", type:"number",
              title: "Minium soil moisture...")
    }
    section("Turn off the hose when the soil moisture reaches:") {
        input(name:"soilMax", type:"number",
              title: "Maximum soil moisture...")
    }
    section("Notify me when hose turns on:") {
        input(name:"sendPushMessage", type:"bool",
              title: "Send push notification?", required: false)
        input(name:"phone", type:"phone",
              title:"Send a text message?", required: false)
    }
    section("Do not notify me in the following modes:") {
        input(name:"notModes", type:"mode",
              title:"Choose modes...", multiple:true, required:false)
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
    subscribe(soil1, "humidity", moistureHandler)
    subscribe(switch1, "switch", switchHandler)
    runEvery5Minutes(moistureSpotCheck)
    state.name = "Garden Waterer"
}

def moistureHandler(moistureEvt) {
    def moistureValue = moistureEvt.value.toInteger()
    state.soil1Name = moistureEvt.getDevice()
    log.debug("$state.name: moistureHandler called")
    log.debug("$state.name: ${state.soil1Name} moisture is $moistureValue")
    state.moistureValue = moistureValue
    if(moistureValue <= soilMin) {
        switch1.on()
        state.switchValue = "on"
        log.debug("$state.name: Turning ${state.switch1Name} on")
    }
    else if(moistureValue >= soilMax && state.switchValue == "on") {
        switch1.off()
        state.switchValue = "off"
        log.debug("$state.name: Turning ${state.switch1Name} off")
    }
    else {
        log.debug("$state.name: No Action, ${state.switch1Name} already $state.switchValue")
    }
}

def moistureSpotCheck() {
    def moistureValue = soil1.currentValue("humidity").toInteger()
    state.switch1Name = switch1.getDevice()
    log.debug("$state.name: moistureSpotCheck called")
    log.debug("$state.name: ${soil1.getDevice()} moisture is $moistureValue")
    if(moistureValue <= soilMin) {
        switch1.on()
        state.switchValue = "on"
        log.debug("")
    }
    else if(moistureValue >= soilMax && state.switchValue == "on") {
        switch1.off()
        state.switchValue = "off"
        log.debug("$state.name: Turning ${state.switch1Name} off")
    }
    else {
        log.debug("$state.name: No Action, ${state.switch1Name} already $state.switchValue")
    }
}

def switchHandler(switchEvt) {
    def switchValue = switchEvt.value
    log.debug("$state.name: switchHandler called")
    log.debug("$state.name: ${switchEvt.displayname} is $switchValue")
    state.switchValue = switchValue
}

private send(msg) {
	  log.debug("$state.name: Send message called")
    log.debug("sendPushMessage: $sendPushMessage")
    log.debug("mode: ${location.mode}")
    log.debug("notModes = $notModes")
    def dontSend = notModes.contains(location.mode)
    if ( dontSend ) {
    	log.debug("${state.name}: No message was sent because mode is ${location.mode}")
    }
    else {
    	log.debug("$state.name: Okay to send message because mode is ${location.mode}")
        if (sendPushMessage == true) {
            log.debug("$state.name: sending push message")
            sendPush(msg)
        }
        if (phone) {
            log.debug("$state.name: sending text message")
            sendSms(phone, msg)
        }
    }
}
