/**
 *  Copyright 2015 SmartThings
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
    name: "Mode Change Notify",
    namespace: "SkyJedi",
    author: "skyjedi@gmail.com",
    description: "Sends a notification when a mode change occurs",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/alarm/beep/beep@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/alarm/beep/beep@2x.png"
) 

preferences {
    section( "Notifications" ) 
    {
        def pageProperties = [
			name:	"pageSetup",
			title:	"Configuration",
			install:	true
		]
        
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "bool", title: "Send a push notification?", required: false
            input "phoneNumber", "phone", title: "Send a Text Message?", required: false
            }
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
  subscribe(location, onLocation)
  state.lastmode = location.mode
}

def onLocation(evt) {
	def msg = "${location.name} mode changed from ${state.lastmode} to ${evt.value}"
    
		if (sendPushMessage) {
         	log.debug("sending push message")
         	sendPush("${msg}")
        	}
            
		if (phoneNumber) {
            log.debug("sending text message")
            sendSms(phoneNumber,"${msg}")
        	}
            
  	state.lastmode = location.mode
}
