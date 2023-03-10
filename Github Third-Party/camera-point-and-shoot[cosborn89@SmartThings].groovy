/**
 *  Camera Point and Shoot
 *
 *  Copyright 2015 Kevin Tierney
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
    name: "Camera Point and Shoot",
    namespace: "tierneykev",
    author: "Kevin Tierney",
    description: "Move camera to preset, and take a picture based on contact sensor trigger",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("When this contact sensor is opened"){
    input "contactSensor","capability.contactSensor",multiple: false,required:true
  }
  section("Move to this preset"){
    input(name:"preset",type:"enum",options:['1','2','3'],required:true)
  }
  section("Travel Time to wait before taking photo"){
    input(name:"pauseSeconds",type:"number",required:true)
  }
  section("Take a picture with this camera"){
    input "camera","capability.imageCapture",multiple: false,required:true
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
 subscribe(contactSensor,"contact.open",contactHandler)
 log.debug "pause - ${pauseSeconds.toInteger()}"
}
def contactHandler(evt){
	log.debug "Contact Opened - Moving into position"
    if(preset =='1'){
    	camera.preset1()
    }
    
	if(preset =='2') {
    	camera.preset2()
    }
    if(preset =='3'){
    	camera.preset3()
    }
	runIn(pauseSeconds,takePicture)
    
	
}

def takePicture(){
	camera.take()
    log.debug "Photo Taken"
  
    
}

