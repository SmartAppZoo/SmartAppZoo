/**
 *  Minimote Multiclick Mode
 *
 *  Copyright 2016 Pedro Toledo
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
    name: "Minimote MultiClick Mode",
    namespace: "ptoledo",
    author: "Pedro Toledo",
    description: "To use the minimote with multiple clicking",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true
) {
  appSetting "accessToken"
}

preferences {
  page(name: "buttonSelect", title: "Select your action button", nextPage: "switchesOnSelect") {
    section() {
      input "thebutton", "capability.button", title: "Pick your button", required: true
    }
  }
  page(name: "switchesOnSelect", title: "Select your switches to turn On", nextPage: "switchesOffSelect") {
    section() {
      input "switchOn_1_0", "capability.switch", required: false, title: "Pick a Switch on button 1 Hold", multiple: true
      input "switchOn_1_1", "capability.switch", required: false, title: "Pick a Switch on button 1, 1 click", multiple: true
      input "switchOn_1_2", "capability.switch", required: false, title: "Pick a Switch on button 1, 2 click", multiple: true
      input "switchOn_1_3", "capability.switch", required: false, title: "Pick a Switch on button 1, 3 click", multiple: true
      input "switchOn_1_4", "capability.switch", required: false, title: "Pick a Switch on button 1, 4 click", multiple: true
      input "switchOn_2_0", "capability.switch", required: false, title: "Pick a Switch on button 2 Hold", multiple: true
      input "switchOn_2_1", "capability.switch", required: false, title: "Pick a Switch on button 2, 1 click", multiple: true
      input "switchOn_2_2", "capability.switch", required: false, title: "Pick a Switch on button 2, 2 click", multiple: true
      input "switchOn_2_3", "capability.switch", required: false, title: "Pick a Switch on button 2, 3 click", multiple: true
      input "switchOn_2_4", "capability.switch", required: false, title: "Pick a Switch on button 2, 4 click", multiple: true
      input "switchOn_3_0", "capability.switch", required: false, title: "Pick a Switch on button 3 Hold", multiple: true
      input "switchOn_3_1", "capability.switch", required: false, title: "Pick a Switch on button 3, 1 click", multiple: true
      input "switchOn_3_2", "capability.switch", required: false, title: "Pick a Switch on button 3, 2 click", multiple: true
      input "switchOn_3_3", "capability.switch", required: false, title: "Pick a Switch on button 3, 3 click", multiple: true
      input "switchOn_3_4", "capability.switch", required: false, title: "Pick a Switch on button 3, 4 click", multiple: true
      input "switchOn_4_0", "capability.switch", required: false, title: "Pick a Switch on button 4 Hold", multiple: true
      input "switchOn_4_1", "capability.switch", required: false, title: "Pick a Switch on button 4, 1 click", multiple: true
      input "switchOn_4_2", "capability.switch", required: false, title: "Pick a Switch on button 4, 2 click", multiple: true
      input "switchOn_4_3", "capability.switch", required: false, title: "Pick a Switch on button 4, 3 click", multiple: true
      input "switchOn_4_4", "capability.switch", required: false, title: "Pick a Switch on button 4, 4 click", multiple: true
    }
  }
  page(name: "switchesOffSelect", title: "Select your switches to turn Off", nextPage: "sceneAssignment") {
    section() {
      input "switchOff_1_0", "capability.switch", required: false, title: "Pick a Switch off button 1 Hold", multiple: true
      input "switchOff_1_1", "capability.switch", required: false, title: "Pick a Switch off button 1, 1 click", multiple: true
      input "switchOff_1_2", "capability.switch", required: false, title: "Pick a Switch off button 1, 2 click", multiple: true
      input "switchOff_1_3", "capability.switch", required: false, title: "Pick a Switch off button 1, 3 click", multiple: true
      input "switchOff_1_4", "capability.switch", required: false, title: "Pick a Switch off button 1, 4 click", multiple: true
      input "switchOff_2_0", "capability.switch", required: false, title: "Pick a Switch off button 2 Hold", multiple: true
      input "switchOff_2_1", "capability.switch", required: false, title: "Pick a Switch off button 2, 1 click", multiple: true
      input "switchOff_2_2", "capability.switch", required: false, title: "Pick a Switch off button 2, 2 click", multiple: true
      input "switchOff_2_3", "capability.switch", required: false, title: "Pick a Switch off button 2, 3 click", multiple: true
      input "switchOff_2_4", "capability.switch", required: false, title: "Pick a Switch off button 2, 4 click", multiple: true
      input "switchOff_3_0", "capability.switch", required: false, title: "Pick a Switch off button 3 Hold", multiple: true
      input "switchOff_3_1", "capability.switch", required: false, title: "Pick a Switch off button 3, 1 click", multiple: true
      input "switchOff_3_2", "capability.switch", required: false, title: "Pick a Switch off button 3, 2 click", multiple: true
      input "switchOff_3_3", "capability.switch", required: false, title: "Pick a Switch off button 3, 3 click", multiple: true
      input "switchOff_3_4", "capability.switch", required: false, title: "Pick a Switch off button 3, 4 click", multiple: true
      input "switchOff_4_0", "capability.switch", required: false, title: "Pick a Switch off button 4 Hold", multiple: true
      input "switchOff_4_1", "capability.switch", required: false, title: "Pick a Switch off button 4, 1 click", multiple: true
      input "switchOff_4_2", "capability.switch", required: false, title: "Pick a Switch off button 4, 2 click", multiple: true
      input "switchOff_4_3", "capability.switch", required: false, title: "Pick a Switch off button 4, 3 click", multiple: true
      input "switchOff_4_4", "capability.switch", required: false, title: "Pick a Switch off button 4, 4 click", multiple: true
    }
  }
  page(name:"sceneAssignment", title:"Assigning scenes to buttons", install: true, uninstall: true)
}

def sceneAssignment() {
  discoverScenes()
  def scenesDiscovered = scenesDiscovered()
  scenesDiscovered.sort()
  dynamicPage(name:"sceneAssignment", title:"LIFX Scene Assignment Started!") {
    section("Select a scene to add...") {
      input "scene_1_0", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 1 Hold"
      input "scene_1_1", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 1, 1 click"
      input "scene_1_2", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 1, 2 click"
      input "scene_1_3", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 1, 3 click"
      input "scene_1_4", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 1, 4 click"
      input "scene_2_0", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 2 Hold"
      input "scene_2_1", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 2, 1 click"
      input "scene_2_2", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 2, 2 click"
      input "scene_2_3", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 2, 3 click"
      input "scene_2_4", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 2, 4 click"      
      input "scene_3_0", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 3 Hold"
      input "scene_3_1", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 3, 1 click"
      input "scene_3_2", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 3, 2 click"
      input "scene_3_3", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 3, 3 click"
      input "scene_3_4", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 3, 4 click"
      input "scene_4_0", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 4 Hold"
      input "scene_4_1", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 4, 1 click"
      input "scene_4_2", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 4, 2 click"
      input "scene_4_3", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 4, 3 click"
      input "scene_4_4", "enum", required: false, options:scenesDiscovered, title: "Pick a Scene button 4, 4 click"      
    }
  }
}

def discoverScenes() {
  //log.debug("Start Discovery of LIFX scenes with access_token="+appSettings.accessToken)
  def pollParams = [
    uri: "https://api.lifx.com",
    path: "/v1/scenes",
    headers: ["Content-Type": "text/json", "Authorization": "Bearer "+appSettings.accessToken],
    query: [format:"json", body: jsonRequestBody]
  ]
  try{
    httpGet(pollParams) { resp ->
      if(resp.data){
        def scenes = getScenes()
        resp.data.each() { scene ->
          //log.debug("+scene")
          scenes << ["${scene.uuid.toString()}":scene]
        }
      }
    }
  } catch(Exception e){
    log.debug("___exception: " + e)
  }
}

def scenesDiscovered() {
    def scenes = getScenes()
    def map = [:]
    scenes.each {
        map["${it.value.uuid}"] = it.value.name
    }
    map
}

private getScenes() {
    if (!state.scenes) { state.scenes = [:] }
    state.scenes
}

def activateScene(scene) {
  if (scene == null) {
    log.debug("scene not configured")
    return
  }
  log.debug("scene: "+scene)
  def pollParams = [
    uri: "https://api.lifx.com", 
    path: "/v1/scenes/scene_id:"+scene+"/activate", 
    requestContentType: "application/json",
    headers: ["Authorization": "Bearer "+appSettings.accessToken]
  ]
  try{
    httpPut(pollParams) { resp ->
      log.debug "scene: "+scene+": ${resp.data}"
      //log.debug "response contentType: ${resp.contentType}"
    }
  } catch(Exception e){
    log.debug("___exception: " + e)
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubscribe()
  initialize()
}

def initialize() {
  subscribe(thebutton, "button", buttonHandler)
  state.last_event = 0
  state.last_button = 0
  state.click_counter = 0
  atomicState.on_action = 0
  discoverScenes()
}

def buttonHandler(evt) {
  int current_button
  try {
    def data = evt.jsonData
    current_button = data.buttonNumber
  } catch (e) {
    log.warn "caught exception getting event data as json: $e"
  }  
  if (evt.value == "pushed") {
    state.current_event = now()
    //log.debug "button eventtttttttttttttttttttttttttttttttttttttttttttt"
    //log.debug "-1 event at: ${state.last_event}"
    //log.debug "+0 event at:  ${state.current_event}"
    if (state.current_event-state.last_event<1000 && state.click_counter < 4) {
      state.click_counter = state.click_counter + 1
      log.debug "Click ++"
    } else {
      state.click_counter = 1
      state.last_button = current_button
      log.debug "Click start pushed"
      runIn(2,buttonAction)
    }
    state.last_event = state.current_event
  } else {
    state.click_counter = 5
    state.last_button = current_button
    log.debug "Click start hold"
    buttonAction()
  }
}

def buttonAction(){
  if (state.click_counter != 0) {
    if (state.click_counter < 5) {
      log.debug "${state.click_counter} clicks from ${state.last_button} where detected"
    } else {
      log.debug "0 clicks from ${state.last_button} where detected"
    }
    switch(state.last_button){
      case 1:
        if (state.click_counter==1) {
          activateScene(scene_1_1)
          switchOn_1_1?.on()
          switchOff_1_1?.off()
        }
        if (state.click_counter==2) {
          activateScene(scene_1_2)
          switchOn_1_2?.on()
          switchOff_1_3?.off()
        }        
        if (state.click_counter==3) {
          activateScene(scene_1_3)
          switchOn_1_3?.on()
          switchOff_1_3?.off()
        }
        if (state.click_counter==4) {
          activateScene(scene_1_4)
          switchOn_1_4?.on()
          switchOff_1_4?.off()
        }        
        if (state.click_counter==5) {
          activateScene(scene_1_0)
          switchOn_1_0?.on()
          switchOff_1_0?.off()
        }
        break
      case 2:
        if (state.click_counter==1) {
          activateScene(scene_2_1)
          switchOn_2_1?.on()
          switchOff_2_1?.off()
        }
        if (state.click_counter==2) {
          activateScene(scene_2_2)
          switchOn_2_2?.on()
          switchOff_2_2?.off()
        }        
        if (state.click_counter==3) {
          activateScene(scene_2_3)
          switchOn_2_3?.on()
          switchOff_2_3?.off()
        }
        if (state.click_counter==4) {
          activateScene(scene_2_4)
          switchOn_2_4?.on()
          switchOff_2_4?.off()
        }        
        if (state.click_counter==5) {
          activateScene(scene_2_0)
          switchOn_2_0?.on()
          switchOff_2_0?.off()
        }
        break
      case 3:
        if (state.click_counter==1) {
          activateScene(scene_3_1)
          switchOn_3_1?.on()
          switchOff_3_1?.off()
        }
        if (state.click_counter==2) {
          activateScene(scene_3_2)
          switchOn_3_2?.on()
          switchOff_3_2?.off()
        }        
        if (state.click_counter==3) {
          activateScene(scene_3_3)
          switchOn_3_3?.on()
          switchOff_3_3?.off()
        }
        if (state.click_counter==4) {
          activateScene(scene_3_4)
          switchOn_3_4?.on()
          switchOff_3_4?.off()
        }        
        if (state.click_counter==5) {
          activateScene(scene_3_0)
          switchOn_3_0?.on()
          switchOff_3_0?.off()
        }
        break
      case 4:
        if (state.click_counter==1) {
          activateScene(scene_4_1)
          switchOn_4_1?.on()
          switchOff_4_1?.off()
        }
        if (state.click_counter==2) {
          activateScene(scene_4_2)
          switchOn_4_2?.on()
          switchOff_4_2?.off()
        }        
        if (state.click_counter==3) {
          activateScene(scene_4_3)
          switchOn_4_3?.on()
          switchOff_4_3?.off()
        }
        if (state.click_counter==4) {
          activateScene(scene_4_4)
          switchOn_4_4?.on()
          switchOff_4_4?.off()
        }        
        if (state.click_counter==5) {
          activateScene(scene_4_0)
          switchOn_4_0?.on()
          switchOff_4_0?.off()
        }
        break
      default:
        break
    }
    state.click_counter = 0
  } else {
    log.debug "De-synchronized button event"
  }
}
