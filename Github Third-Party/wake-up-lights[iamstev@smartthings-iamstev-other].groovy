/**
 *  Smart Sunrise
 *
 *  Copyright 2015 Steven Smith
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
    name:		"Wake Up Lights",
    namespace:	"iamstev",
    author:		"Steven Smith",
    description:"Utilize up to 4 lights to wake up naturally.",
    category:	"My Apps",
    iconUrl:	"https://s3.amazonaws.com/a.stev.link/smartthings/sunrise.png",
    iconX2Url:	"https://s3.amazonaws.com/a.stev.link/smartthings/sunrise@2x.png",
    iconX3Url:	"https://s3.amazonaws.com/a.stev.link/smartthings/sunrise@2x.png")


preferences {
	page(name: "pageSettings", title: "Settings", install: true, uninstall: false){
    	section("Time"){
        	input(name: "onoff", type: "bool", title: "Off / On", defaultValue: true, require: true)
            input(name: "waketime", type: "time", title: "What time do you want to get up?", defaultValue: "2015-01-01T07:00:00.000-0500", required: true)
        }
    	section("Setup"){
        	href(name: "href", title: "Device Setup", required: false, page: "pageSetup")
        }
        section("Help"){
        	paragraph "Set the time you want to get up, and flip the switch to \"on\"."
            paragraph "If the wake up light sequence is currently running, opening this settings page will stop the sequence. When you hit done the lights will turn back off. If you set a time greater than 10 minutes from now and have the switch flipped to \"on\", the sequence will run again today."
        }
    }
    page(name: "pageSetup", title: "Setup", install: true, uninstall: true){
    	section("Lights"){
        	input(name:"light1", type: "capability.switchLevel", title: "Light 1", multiple: false, required: true)
            input(name:"light2", type: "capability.switchLevel", title: "Light 2", multiple: false, required: false)
            input(name:"light3", type: "capability.switchLevel", title: "Light 3", multiple: false, required: false)
            input(name:"light4", type: "capability.switchLevel", title: "Light 4", multiple: false, required: false)

        }
        section(){
        	label(title: "Name this app", defaultValue: "Wake Up Lights", required: true)
            mode(title: "Set for specific mode(s)", required: false)
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
    if(state.running){
        light1.setLevel(0)
        light2.setLevel(0)
        light3.setLevel(0)
        light4.setLevel(0)
        state.running = false
    }
    
    if(onoff){
        long waketime_minus_10min = new Date().parse("yyy-MM-dd'T'HH:mm:ss.SSSZ", waketime).getTime() - 600000
        schedule(waketime_minus_10min, step1)
    }
}

def step1(){
	state.running = true
	light1.setLevel(1)
    
    runIn(180, step2)
}

def step2(){
    light1.setLevel(10)
    light2.setLevel(1)
    
    runIn(180, step3)
}

def step3(){
	light1.setLevel(20)
    light2.setLevel(10)
    light3.setLevel(1)
    
    runIn(120, step4)
}

def step4(){
    light1.setLevel(50)
    light2.setLevel(40)
    light3.setLevel(5)
    light4.setLevel(1)
    
    runIn(120, step5)
}

def step5(){
    light1.setLevel(99)
    light2.setLevel(99)
    light3.setLevel(99)
    light4.setLevel(99)
    
    state.running = false
}