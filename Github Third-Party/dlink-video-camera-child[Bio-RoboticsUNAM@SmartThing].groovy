/**
*  Dlink Video Camera Child
*
*  Copyright 2016 
*  Parent/Child SmartApp based on Patrick Stuart's Generic Video Camera SmartApp
*
*  Dr. Savage Carmona Jesus
*  Rocha Reséndiz Irving
*  Posgrado FI UNAM 
*  Bio-Robotics
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
    name: "Dlink Video Camera Child",
    namespace: "biorobotics",
    author: "bioRobotics",
    description: "Dlink Child Video Camera SmartApp",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Camera/dlink.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Camera/dlink@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Camera/dlink@3x.png")


preferences {
    page(name: "mainPage", title: "Instalación de Videocamára", install: true, uninstall:true) {
        section("Camera Name") {
            label(name: "label", title: "Nombre de la Camára", required: true, multiple: false, submitOnChange: true)
        }
        section("Add a Camera") {
        	input("CameraType","enum", title: "Modelo de Cámara", description: "Seleccione su camára", required:false, submitOnChange: true,
            options: ["DCS-930L", "DCS-931L", "DCS-932L", "DCS-933L", "DCS-934L", "DCS-935L", "DCS-942L", "DCS-960L", "DCS-2132L", "DCS-2210L", "DCS-2310L", "DCS-2330L", "DCS-2630L"], displayDuringSetup: true)
            }
        section("Camera Settings:"){
        	input("CameraIP", "string", title:"IP Camára", description: "Indique la dirección IP de la camára (Debera ser una IP estática)", required: true, displayDuringSetup: true)
    		input("CameraPort", "string", title:"Puerto de Camára", description: "Indique el puerto de la camára (Se sugiere el puerto 80 )", defaultValue: 80 , required: true, displayDuringSetup: true)
    		input("VideoIP", "string", title:"IP Video", description: "Introduzca la dirección IP de video (utilize una IP externa, si esta utilizando reenvío de de puertos)", required: true, displayDuringSetup: true)
    		input("VideoPort", "string", title:"Puerto de Video", description: "Introduzca la dirección IP de video (utilize un pueto externa, si esta utilizando reenvío de de puertos, Se suguiere 5086)", required: true, displayDuringSetup: true)
    		input("CameraUser", "string", title:"Usuario Dlink camára", description: "Introduzca el usuario de la camára", required: false, displayDuringSetup: true)
    		input("CameraPassword", "password", title:"Password Dlink camára", description: "Introduzca el password de la camára", required: false, displayDuringSetup: true)
            }
        section("Hub Settings"){
        	input("hubName", "hub", title:"HUB", description: "Selecciona tu HUB", required: true, displayDuringSetup: true)
        }
    }
    
}

def installed() {
    log.debug "Installed"

    initialize()
}

def updated() {
    log.debug "Updated"

    unsubscribe()
    initialize()
}

def initialize() {

        state.CameraIP = CameraIP
        state.CameraPort = CameraPort
        state.VideoIP = VideoIP
        state.VideoPort = VideoPort
        state.CameraUser = CameraUser
        state.CameraPassword = CameraPassword
        
        /*
        log.debug "Camera IP: ${state.CameraIP}"
        log.debug "Camera Port: ${state.CameraPort}"
        log.debug "Video IP: ${state.VideoIP}"
        log.debug "Video Port: ${state.VideoPort}"
        log.debug "Camera User: ${state.CameraUser}"
        log.debug "Camera Password: ${state.CameraPassword}"
        */
        
	try {
        def DNI = (Math.abs(new Random().nextInt()) % 99999 + 1).toString()
        def cameras = getChildDevices()
        if (cameras) {
            cameras[0].configure()
        }
        else {
        	def childDevice = addChildDevice("biorobotics", CameraType, DNI, hubName.id, [name: app.label, label: app.label, completedSetup: true])
        }
    } catch (e) {
    	log.error "Error creating device: ${e}"
    }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}