/**
 *  App JGLS
 *
 *  Copyright 2016 FI UNAM
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
    name: "App RobotJGLS",
    namespace: "Bio-RoboticsUNAM",
    author: "FI UNAM",
    description: "Example App. Objetivo: encender algun switch cuando se detecta movimiento",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Select your Robot") {
        input "therobot", "capability.actuator", required: true, title: "Select the robot you want to use!"
    }
    section("Select Doors and/or Windows") {
        input "thedoorwindow", "capability.contactSensor", required: true, title: "Which door(s) and/or window(s) do you want?", multiple: true
    }
	section("Select Motion Sensors") {
        input "themotion", "capability.motionSensor", required: false, title: "Which motion sensor(s) do you want?", multiple: true
    }
    section("Select Switch") {
        input "theswitch", "capability.switch", required: false, title: "Which switch do you want?", multiple: true
    }
    section("Select Water Sensor(s)") {
        input "thewater", "capability.waterSensor", required: false, title: "Which water sensor(s) do you want?", multiple: true
    }
    /*section("Select Sound Sensor(s)") {
        input "thesound", "capability.soundSensor", required: false, title: "Which sound sensor(s) do you want?", multiple: true
    }*/
    /*section("Select Smoke Detector(s)") {
        input "thesmoke", "capability.smokeDetector", required: false, title: "Which smoke sensor(s) do you want?"
    }*/
    /*section("Select Carbon Dioxide Detector(s)") {
        input "thedioxide", "capability.carbonDioxideMeasurement", required: true, title: "Which carbon dioxide detector(s) do you want?", multiple: true
    }*/
    /*section("Select Carbon Monoxide Detector(s)") {
        input "themonoxide", "capability.carbonMonoxideDetector", required: false, title: "Which carbon monoxide measurer do you want?", multiple: true
    }*/
    
    
    /*section("Turn off when there's been no movement for") {
        input "minutes", "number", required: true, title: "Minutes?"
    }*/

    
	
    //section("Title") {
	//	// TODO: put inputs here
	//}
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
    log.debug "Los settings son: ${settings}"
    if(settings["thedoorwindow"])
    {
    	log.debug "Existe sensor thedoorwindow"
        subscribe(thedoorwindow, "contact", contactHandler)
    }
    if(settings["themotion"])
    {
    	log.debug "Existe sensor themotion"
        subscribe(themotion, "motion.active", motionDetectedHandler)
        /*Imprimir el contenido de la llave themotion
        log.debug settings["themotion"]*/
    }
    if(settings["theswitch"])
    {
    	log.debug "Existe sensor theswitch"
        subscribe(theswitch, "switch", switchHandler)
        //theswitch.each {
        //    log.debug "${it.displayName}, attribute: ${it.supportedAttributes.name}, values: ${it.supportedAttributes.values[0][0]}, dataType: ${it.supportedAttributes.dataType}"
        //}
        /*Imprimir el contenido de la llave theswitch
        log.debug settings["theswitch"]*/
    }
    if(settings["thewater"])
    {
    	log.debug "Existe sensor thewater"
        subscribe(thewater, "water.wet", wetDetectedHandler)
    }
    if(settings["therobot"])
    {
    	log.debug "Existe sensor therobot"
        subscribe(therobot, "cmdReceived.cmdOk", receivedCmdHandler)
    }
    /*if(settings["themonoxide"])
    {
    	log.debug "Existe sensor themonoxide"
        subscribe(themonoxide, "carbonDioxide", dioxideMeasurementHandler)
    }*/
    
    
    
    /*log.debug "Name : ${theswitch.switch.name}"
    log.debug "Name: ${theswitch.displayName}"*/
    
    
    
    
    
    
    
    /*log.debug "${theswitch.displayName}, attribute: ${theswitch.supportedAttributes.name}, values: ${theswitch.supportedAttributes.values}"
    
    def attrs = thetemp.supportedAttributes
    attrs.each {
        log.debug "${thetemp.displayName}, attribute ${it.name}, values: ${it.values}"
        log.debug "${thetemp.displayName}, attribute ${it.name}, dataType: ${it.dataType}"
    }*/
    
}

// TODO: implement event handlers

def motionDetectedHandler(evt) {
    //log.debug "motionDetectedHandler called: $evt"
    def nameSensor = evt.displayName
    log.debug "Comando al Robot"
    sendCommandToRobot(nameSensor, "active")
    //therobot.commandRobot(nameSensor)
    //def message = "Unexpected motion has been detected with a Motion Sensor named: \"${nameSensor}\""
    //log.debug message
    //log.debug "Nombre del sensor: ${nameSensor}"
    //theswitch.on()
    
    
    //theswitch[0].on()
    //theswitch[1].on()
    //theswitch[2].on()
    //sendPush("Motion Detected!")
}

def contactHandler(evt) {
	//log.debug "contactHandler called: $evt"
    def nameSensor = evt.displayName
    log.debug "Comando al Robot"
    //therobot.commandRobot(nameSensor)
    def valueSensor = evt.value
    sendCommandToRobot(nameSensor, valueSensor)
    //def message = "Unexpected activity has been detected with a Contact Sensor named: \"${nameSensor}\", because is \"${valueSensor}\""
    //log.debug message
    /*
    Event detected by sensor "Door", whose current state is "on".
	Command has been send to "ROS".
	Command has been recevied by "ROS".
    */
}

def switchHandler(evt) {
	//log.debug "switchHandler called: $evt"
    def nameSensor = evt.displayName
    log.debug "Comando al Robot"
    //therobot.commandRobot(nameSensor)
    def valueSensor = evt.value
    sendCommandToRobot(nameSensor, valueSensor)
    //def message = "Unexpected activity has been detected with a Switch named: \"${nameSensor}\", because is \"${valueSensor}\""
    //log.debug message
}

def wetDetectedHandler(evt) {
    def nameSensor = evt.displayName
    log.debug "Comando al Robot"
    sendCommandToRobot(nameSensor, "wet")
    //therobot.commandRobot(nameSensor)
    //def message = "Unexpected activity has been detected with a Water Sensor named: \"${nameSensor}\", because it has detected: \"wet\""
    //log.debug message
}

def sendCommandToRobot(def sensorName, def sensorValue){
	def pushMessage = "Event detected by sensor \"${sensorName}\", whose current state is \"${sensorValue}\""
    log.debug pushMessage
    therobot.commandRobot(sensorName)
    sendPush(pushMessage)
    sendPush("Command has been sent to the Robot!")
}

def receivedCmdHandler(def nothing){
	log.debug "Confirmando al Device Handler recepcion de comando"
	therobot.confirmCommand("ok")
    sendPush("Command has been received by the Robot!")
}
/*
def motionStoppedHandler(evt) {
    log.debug "motionStoppedHandler called: $evt"
    theswitch.off()
}
*/
