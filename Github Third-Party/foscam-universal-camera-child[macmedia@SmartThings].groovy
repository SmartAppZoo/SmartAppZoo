/**
 *  Foscam Universal Camera child
 *
 *  Copyright 2016 Mike Elser
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
    name: "Foscam Universal Camera child",
    namespace: "macmedia",
    author: "Mike Elser",
    parent: "macmedia:Foscam Universal Camera (Connect)",
    description: "Foscam Universal Camera App to control the camera and view live video",
    category: "Safety & Security",
    iconUrl: "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon.png",
    iconX2Url: "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon@2x.png",
    iconX3Url: "https://s3-us-west-2.amazonaws.com/smartthings-icons/foscam-icon@2x.png")


preferences {
    page(name:"mainPage", title:"Install Video Camera", install: true, uninstall: true){
        section("Camera Name"){
            label(name:"label", title: "Name This camera", required: true, multiple: false, defaultValue:"FS")
        }

        section("on this hub...") {
            input "theHub", "hub", multiple: false, required: true
        }

        section("Add a Camera"){
            input("cameraStreamIP", "text", title: "Camera's IP Address", description: "Please enter your camera's streaming IP", required:true)
            input("cameraStreamPort", "text", title: "Camera's Port", description: "Please enter your camera's port", required:false, defaultValue:"88")
            input("cameraStreamProtocol", "enum", title: "Camera Protocol", description: "Please enter your camera's streaming Protocol", required:true, submitOnChange: true,
                options: [
                    ["http://" : "HTTP"],
                    ["rtsp://" : "RTSP"]

                ],displayDuringSetup: true
            )
            input("cameraStreamRTSPPort", "text", title: "Camera's RTSP Port", description: "Please enter your camera's port", required:false, defaultValue:"554")
            input("cameraStreamUser", "text", title: "Camera's Username", description: "Please enter your camera's username", required:false)
            input("cameraStreamPwd", "text", title: "Camera's Password", description: "Please enter your camera's password", required:false)
            input("cameraStreamHD", "bool", title: "Camera is HD", description: "Please select if your camera is a HD model", required:false, defaultValue:true)
            input("cameraStreamDebug", "bool", title: "Debug Mode", description: "Turn on debug mode for mroe logging", required:false, defaultValue:true)

        }


    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.info("2Updated with settings: ${settings}")

    log.info("Update Call - unsubscribe")
    unsubscribe()

    log.info("Update Call - initialize")
    initialize()
}



def initialize() {
    log.info("Start Initialize - Child")
    if(CameraStreamIP) {state.CameraStreamIP = CameraStreamIP}

    try{

        log.info("HUB ID: ${theHub.id}")

        //Get a current list of cameras
        def cameras = getChildDevices()

        if (cameras) {
            removeChildDevices(cameras)
        }

        //Create random number for each camera
        def DNI = makeDNI() //(Math.abs(new Random().nextInt()) % 99999 + 1).toString()
        def childDevice = addChildDevice("macmedia", "Foscam Universal Camera Device", DNI, theHub.id, [name: app.label, label: app.label, completedSetup: true])

    } catch (e) {
        log.error("Error triggered in init: $e")
    }
}


private String makeDNI(){
    def iphex = convertIPtoHex(cameraStreamIP)
    def porthex = convertPortToHex(cameraStreamPort)

    return "${iphex}:${porthex}"
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    return hex.toUpperCase()
}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    return hexport.toUpperCase()
}

//Remove all previous cameras
private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

private String getStreamURI(){
    log.info("Get StreamURI")

    def cameraPort = ""

    if(cameraStreamPort)
        cameraPort = ":" + cameraStreamRTSPPort

    return cameraStreamProtocol + cameraStreamUser + ":" + cameraStreamPwd + "@" + cameraStreamIP + cameraPort + "/videoMain"
}

private doDebug(Object... dbgStr) {
    //if (camDebug) {
        log.debug dbgStr
    //}
}
