/**
 *  mqtt bridge app
 *
 *  Copyright 2020 Jerzy Mikucki
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
 
import groovy.json.JsonSlurper

definition(
    name: "mqtt bridge app",
    namespace: "mikuslaw",
    author: "Jerzy Mikucki",
    description: "Mqtt Bridge App",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
	}
    section ("Bridge") {
        input "bridge", "capability.notification", title: "Bridge Device", required: true, multiple: false
        input "devices", "capability.notification", title: "Mqtt Devices", required: false, multiple: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    if(state.containsKey("topics")) {
    	state.topics = [:]
    }
	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(bridge, "mqttpublish", publishHandler)
    subscribe(devices, "mqttrefreshsubscriptions", refreshSubscriptionsHandler)
    subscribe(devices, "mqttnotifysubscriptions", notifySubscriptionsHandler)
    subscribe(devices, "mqttpoll", pollHandler)
    
    refreshSubscriptions()
}

def refreshSubscriptionsHandler(evt) {
	// Delay this action a bit, in case more 
    // devices are refreshing at the same time
	runIn(1, refreshSubscriptions)
}

def refreshSubscriptions() {
	bridge.unsubscribeAllTopics()
    
    if(state.containsKey("subscriptions")) {
    	state.subscriptions = [:]
    }
    
    devices.each {
    	log.debug "Ask for device subscriptions for ${it.name}"
    	it.notifySubscriptions()
    }
}

def pollHandler(evt) {
	bridge.poll()
}

def notifySubscriptionsHandler(evt) {	
    def json = new JsonSlurper().parseText(evt.data)
    
    //if(!state.containsKey("subscriptions")) {
    //	state.subscriptions = [:]
    //}
	def subscriptions_map = state.subscriptions
    subscriptions_map.put(json.id, json.topics)
    state.subscriptions = subscriptions_map

	json.topics.each {
    	bridge.subscribeToTopic(json.id, it)
    }
    log.debug "State topic: ${state.subscriptions}"
}

def publishHandler(evt) {
	def id = evt.id
    if (evt.name == "mqttpublish") {
        notificationHandlerMqtt(evt)
    } else {
    	log.debug "Received device event other"
    }
}

def notificationHandlerMqtt(evt)
{
	def json = new JsonSlurper().parseText(evt.data)
    log.debug "Mqtt data parsed: ${json}"
    
    devices.each { dev ->
    	if(state.subscriptions.containsKey(dev.id)) {
        	def device_subscriptions = state.subscriptions[dev.id]
            
            def split_topic = json.topic.split('/')
            // log.debug "Topic split: ${split_topic}"
            device_subscriptions.each { sub ->
            	def split_filter = sub.split('/')
                // log.debug "Filter split: ${split_filter}"
                
                if(split_filter.size() > split_topic.size()) {
                	return
                }
                
                def matched = false
                for(int i = 0;i<split_filter.size();i++) {
                	if(split_filter[i] == "#") {
                    	// Match anything in this and next levels
                        matched = true
                    }
                    else if(split_filter[i] == "+") {
                    	// Match anything in this level
                        if(split_filter.size() == i+1) {
                        	// This was last level, match was found
                            matched = true
                        } else {
                        	// Let's check next level
                        	continue
                        }
                    } else if(split_filter[i] == split_topic[i]) {
                    	// Exact match, check next level
                        if(split_filter.size() == i+1) {
                        	// This was last level, match was found
                            matched = true
                        } else {
                        	// Let's check next level
                        	continue
                        }
                    }  
                }
                
            	// Multilevel wildcard must be at the end and used once
				if(matched) {
                	log.debug "Matched to device"
                	dev.processMqttMessage(json)
                }
            }
    	}    
	
    }
}