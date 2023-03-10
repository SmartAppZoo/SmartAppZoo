/**
 *  TV Times
 *
 *  Copyright 2016 Steven Smith
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
    name: "TV Times",
    namespace: "iamstev",
    author: "Steven Smith",
    description: "Coordinate lights around a TV show.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/a.stev.link/smartthings/tv.png",
    iconX2Url: "https://s3.amazonaws.com/a.stev.link/smartthings/tv@2x.png",
    iconX3Url: "https://s3.amazonaws.com/a.stev.link/smartthings/tv@2x.png")


preferences {
	page(name: "pageSettings", title: "Settings", install: true, uninstall: false){
    	section("Time"){
        	input(name: "onoff", type: "bool", title: "Watching TV tonight?", defaultValue: true, require: true)
            input(name: "starttime", type: "time", title: "When does the show start?", defaultValue: "2015-01-01T21:00:00.000-0500", required: true)
            input(name: "showlength", type: "number", title: "How many minutes is the show?", defaultValue: 60, required: true)
        }
        section("Food"){
        	paragraph "This will leave the TV room lamp on at a dim level until you finish eating."
        	input(name: "food", type: "bool", title: "Are you eating delicious food?", defaultValue: false, required: true)
            input(name: "eattime", type: "number", title: "How many minutes does it take to eat?", defaultValue: 15, required: true)
        }
    	section("Setup"){
        	href(name: "href", title: "Device Setup", required: false, page: "pageSetup")
        }
    	 
    }
    page(name: "pageSetup", title: "Setup", install: true, uninstall: true){
    	section("Lights"){
        	input(name:"light_tv_room", type: "capability.switchLevel", title: "TV Room Lights", multiple: true, required: true)
            input(name:"light_off_at_start", type: "capability.switch", title: "Turn these off when the show starts", multiple: true, required: false)
            input(name:"light_on_at_end", type: "capability.switch", title: "Turn these on when the show is over", multiple: true, required: false)
            input(name:"light_dim_at_end", type: "capability.switchLevel", title: "Dim these when the show is over", multiple: true, required: false)
            input(name:"dim_level", type: "number", title: "Set dimmers to this level (1-99)", defaultValue: 30, required: true, range: "1..99")
        }
        section("Entertainment System"){
        	input(name: "tvbool", type: "bool", title: "Can I turn on your entertainment center?", defaultValue: false, required: true)
        	input(name: "tvswitch", type: "capability.switch", title: "Entertainment Equipment", multiple: true, required: false)
            input(name: "tvoff", type: "bool", title: "Do you want me to turn the TV off a few minutes after the show ends?", defaultValue: false, required: true)
        }
        section("Alexa Support"){
            input(name: "alexa_switch", type: "capability.switch", title: "Choose a switch that starts TV Times with the current settings when turned on. Can be useed to have Alexa start TV Times.", multiple: false, required: false)
        }
        section(){
        	label(title: "Name this app", defaultValue: "TV Times", required: true)
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
    long starttime_minus_10min = new Date().parse("yyy-MM-dd'T'HH:mm:ss.SSSZ", starttime).getTime() - 600000
	schedule(starttime_minus_10min, do_prepare)
    subscribe(alexa_switch, "switch.on", evt_alexa)
}

def evt_alexa(evt){	
	log.debug "alexa called"
    light_tv_room.setLevel(30)
    runIn(10, do_start_show)
}

def do_prepare(){
	log.debug "prepare called"
    if(onoff){
    	log.debug "tv time will run"
    	light_tv_room.setLevel(30)
        runIn(600, do_start_show)
        if(tvbool){
            tvswitch.on()
        }
    }else{
    	log.debug "tv time nothing will happen"
    }
}

def do_start_show(){
	log.debug "start show called"
    light_off_at_start.off()
    if(food){
    	runIn(eattime*60, do_food_over)
    }else{
    	do_food_over
    }
    runIn(showlength*60 + 30, do_show_over)
}

def do_food_over(){
	log.debug "food over called"
	light_tv_room.setLevel(5)
    runIn(300, do_tv_lights_out)
}

def do_tv_lights_out(){
	log.debug "tv lights out called"
    light_tv_room.off()
}

def do_show_over(){
	log.debug "show over called"
	light_on_at_end.on()
    light_dim_at_end.setLevel(dim_level)
    if(tvoff){
    	tvswitch.off()
    }
}