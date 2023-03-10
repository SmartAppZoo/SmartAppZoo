/**
 *  TheaterExperience
 *
 *  Copyright 2017 Ken Cote
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
    name: "TheaterExperience",
    namespace: "KenCote",
    author: "Ken Cote",
    description: "Give me dat movie experience",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment6-icn@3x.png")


preferences {
    mappings {
        path("/lights/dim") {
            action: [
                GET: "dimTheLights"
            ]
        } 
        path("/lights/undim") {
            action: [
                GET: "unDimTheLights"
            ]
        }
        path("/lights/pause") {
            action: [
                GET: "pause"
            ]
        } 
        path("/lights/unpause") {
            action: [
                GET: "unPause"
            ]
        }    
    }
    section("Fronts...") {
        input "dimmersFront", "capability.switchLevel", title: "Select", required: true, multiple: true
    }
    section("Rears...") {
        input "dimmersRear", "capability.switchLevel", title: "Select", required: true, multiple: true
    }
}

void dimTheLights() {
    state.lightsDown = 1
    state.pause = 0
    dimLights()
}

void unDimTheLights() {
    state.lightsDown = 0
    state.pause = 0
    dimLights()
}

void pause() {
    state.lightsDown = 0
    state.pause = 1
    dimLights()
}

void unPause() {
    state.lightsDown = 1
    state.pause = 1
    dimLights()
}

private void dimLights() {
  	state.currentLevel = state.lightsDown == 1 ? 51 : 0 
    dimProcess()
}

def dimProcess() {
    def goalLevel = state.lightsDown == 1 ? 0 : 51
    
    if (state.pause == 0)
    {
        if (state.lightsDown == 1)
        {
        	log.debug "dim"
        	def keepDimming = dimmersFront[0].currentLevel >= goalLevel
            log.debug "dimmer:  ${dimmersFront[0].currentLevel} goal:  ${goalLevel}  keepDimming ${keepDimming}"
            def dimStep = 3
            state.currentLevel = state.currentLevel - dimStep 
            log.debug "${state.currentLevel}"
           
            if (keepDimming)
            {
                dimmersRear.setLevel(state.currentLevel)
                dimmersFront.setLevel(state.currentLevel)
                runIn(1, dimProcess)   
            }
        }
        else
        {
        	log.debug "unDim"
            dimmersFront.setLevel(goalLevel) 
            dimmersRear.setLevel(goalLevel) 
        }
    }
    else
    {        
        if (state.lightsDown == 0)
        {
            dimmersRear.setLevel(51) 
        }
        else
        {
            dimmersRear.setLevel(0) 
        }
    }
}