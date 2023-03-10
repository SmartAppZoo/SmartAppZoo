/**
 *  Zendesk Hackery
 *
 *  Copyright 2015 Andrew Boring and Leo Przybylski
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
    name: "Final Countdown",
    namespace: "smarttucson",
    author: "Andrew Boring",
    description: "Stuff",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "mainPage", title: "Adjust the color of your Hue lights based on zendesk ticket state.", install: true, uninstall: true)
}

def mainPage() {
    dynamicPage(name: 'mainPage') {
        section([mobileOnly:true]) {
			label title: "Assign a name", required: false
		}
    	section("Bulb") {
        	input "hue", "capability.colorControl", title: "Which Department?", required: true, multiple: false
    	}
        section("Zendesk") {
            input "division", "enum", title: "Zendesk Division", required: true, options: fetchZendeskViews()
        }
        section("When tickets are...") {
            input "lessThanOne", "number", title: "Less than", required: true
            //input "greaterThanOne", "number", title: "Greater than", required: true
            input "colorOne", "enum", title: "Hue Color?", required: true, multiple:false, 
                options: [
                     ["Soft White":"Soft White - Default"],
                     ["White":"White - Concentrate"],
                     ["Daylight":"Daylight - Energize"],
                     ["Warm White":"Warm White - Relax"],
                     "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevelOne", "enum", title: "Light Level?", required: true, 
                options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]

        }
        section("When tickets are...") {
            input "lessThanTwo", "number", title: "Less than"
            // input "greaterThanTwo", "number", title: "Greater than"
            input "colorTwo", "enum", title: "Hue Color?", required: false, multiple:false, 
                options: [
                    ["Soft White":"Soft White - Default"],
                    ["White":"White - Concentrate"],
                    ["Daylight":"Daylight - Energize"],
                    ["Warm White":"Warm White - Relax"],
                    "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevelTwo", "enum", title: "Light Level?", required: false, 
                options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        }
        section("When tickets are...") {
            //input "lessThanThree", "number", title: "Less than"
            //input "greaterThanThree", "number", title: "Greater than"
            input "colorThree", "enum", title: "Hue Color?", required: false, multiple:false, 
                options: [
                    ["Soft White":"Soft White - Default"],
                    ["White":"White - Concentrate"],
                    ["Daylight":"Daylight - Energize"],
                    ["Warm White":"Warm White - Relax"],
                    "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
                input "lightLevelThree", "enum", title: "Light Level?", required: false, 
                options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        }
    }
}

def initViewMap() {
        def viewMap = [:]

        def authHeader = 'yournetid@email.arizona.edu/token:token'.bytes.encodeBase64()
        def pollParams = [
            uri: "https://hackathonhelp.zendesk.com/api/v2/",
            path: "views/active.json",
            headers: ["Content-Type": "text/json", "Authorization": "Basic ${authHeader}"],
        ]
      
        try {
            httpGet(pollParams) { resp ->
                if(resp.status == 200) {
                    log.debug "poll results returned"
                }
                else {
                    log.error "polling children & got http status ${resp.status}"
                }
            
                resp.data.views.each { view ->
                    viewMap[view.title] = view.id
                }
            }
        }
        catch(Exception e) {
            log.debug "___exception polling children: " + e
        }

    return viewMap
}

def fetchZendeskViews() {
    def viewMap = initViewMap()
    def retval = []
    viewMap.each() { key, value ->
        retval.add(key)
    }
    return retval
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
    //hue.setColor([hue: 39, saturation: 100, level : 0])
    runOnce(new Date(), setZenDeskEventHandler)
}

def setZenDeskEventHandler() {
    while(1) {
        runIn(15, zenDeskUpdate)
    }
}

def zenDeskUpdate() {
    def viewMap = initViewMap()
    def viewId = viewMap[division]
    def authHeader = 'yournetid@email.arizona.edu/token:token'.bytes.encodeBase64()
    def pollParams = [
        uri: "https://hackathonhelp.zendesk.com/api/v2/",
        path: "views/count_many.json",
        headers: ["Content-Type": "text/json", "Authorization": "Basic ${authHeader}"],
        query: [ids : viewId]
    ]
    
    try {
        httpGet(pollParams) { resp ->
            if(resp.status == 200) {
                log.debug "poll results returned"
            }
            else {
                log.error "polling children & got http status ${resp.status}"
            }
            
            resp.data.view_counts.each { view_count ->
            	log.debug(view_count.value.toInteger());
            	ticketBasedHue(view_count.value.toInteger());
               // hue.setColor([hue: 39, saturation: 100, level: view_count.value.toInteger() > 0 ? 100 : 0 ])
            }
        }
    }
    catch(Exception e) {
        log.debug "___exception polling children: " + e
    }
}

def ticketBasedHue(int numTickets){
	// if (lessThanOne >= lessThanTwo)
    // lessThanTwo = (2 * lessThanOne)

	def lightingLevel = 100;
    def color = "";
    
	if (numTickets < lessThanOne) {
    	color = colorOne
        lightingLevel = lightLevelOne.toInteger()
    } else if (numTickets < lessThanTwo) {
    	color = colorTwo
        lightingLevel =  lightLevelTwo.toInteger()
    } else {
    	color = colorThree
        lightingLevel = lightLevelThree.toInteger()
    }    
    
    def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 //83
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}
	log.debug("${hueColor}, ${saturation}, ${lightingLevel}")
	hue.setColor([hue: hueColor , saturation: saturation, level: lightingLevel]);
}


