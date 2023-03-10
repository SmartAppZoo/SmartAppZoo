/**
 *  ZL 7432US Adapter
 *
 *  Copyright 2014 Joel Tamkin, updated 2017 physhster
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
    name: "ZL 7432US Switch Adapter",
    namespace: "",
    author: "NA",
    description: "Creates an adapter for ON/OFF Switches to the ZL 7432US Channels",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
  section("ZL 7432US Module:") {
    input "rsm", "capability.switch", title: "Which ZL 7432US Dual Relay Module?", multiple: false, required: true
    input "switch1", "capability.switch", title: "Switch to assign 1st Channel", multiple: false, required: true
    input "switch2", "capability.switch", title: "Switch to assign 2nd Channel", multiple: false, required: true
  }
}

def installed() {
  log.debug "Installed!"
  subscribe(switch1, "switch.on", switchOnOneHandler)
  subscribe(switch2, "switch.on", switchOnTwoHandler)
  subscribe(switch1, "switch.off", switchOffOneHandler)
  subscribe(switch2, "switch.off", switchOffTwoHandler)


}

def updated() {
  log.debug "Updated!"
  unsubscribe()
  subscribe(rsm,     "switch1", rsmHandler)
  subscribe(rsm,     "switch2", rsmHandler)
  subscribe(switch1, "switch.on", switchOnOneHandler)
  subscribe(switch2, "switch.on", switchOnTwoHandler)
  subscribe(switch1, "switch.off", switchOffOneHandler)
  subscribe(switch2, "switch.off", switchOffTwoHandler)
  
}




def switchOnOneHandler(evt) {
  log.debug "switch on"
  rsm.on()
  rsm.refresh()
}

def switchOnTwoHandler(evt) {
  log.debug "switch on2"
  rsm.on2()
  rsm.refresh()
}


def switchOffOneHandler(evt) {
  log.debug "switch off"
  rsm.off()
  rsm.refresh()
}

def switchOffTwoHandler(evt) {
  log.debug "switch off2"
  rsm.off2()
  rsm.refresh()
}


/*


def switch1Handler(evt) {
  log.debug "switchHandler: ${evt.value}, ${evt.deviceId}, ${evt.source}, ${evt.id}"

    	switch (evt.value) {
        	case 'on':
        		log.debug "switch 1 on"
	       rsm.on()
	       break
        	case 'off':
        		log.debug "switch 1 off"
	       rsm.off()
	       break
	   }
     

}
def switch2Handler(evt) {
  log.debug "switchHandler: ${evt.value}, ${evt.deviceId}, ${evt.source}, ${evt.id}"

    	switch (evt.value) {
        	case 'on':
        		log.debug "switch 2 on"
	       rsm.on2()
	       break
        	case 'off':
        		log.debug "switch 2 off"
	       rsm.off2()
	       break
	   }
     

}

def rsmHandler(evt) {
	log.debug "$evt.name $evt.value"
    if (evt.name == "switch1") {
    	switch (evt.value) {
        	case 'on':
	   	switch1.on()
	       break
	   case 'off':
	   	switch1.off()
	       break
        }
    }
    else if (evt.name == "switch2") {
    	switch (evt.value) {
        	case 'on':
	   	switch2.on()
	       break
	   case 'off':
	   	switch2.off()
	       break
        }
    }
      	
}
*/ 

def rsmHandler(evt) {
  log.debug "rsmHandler called with event:  name:${evt.name} source:${evt.source} value:${evt.value} isStateChange: ${evt.isStateChange()} is: ${evt.is()} isDigital: ${evt.isDigital()} data: ${evt.data} device: ${evt.device}"
  if (evt.name == "switch1") {
    switch (evt.value) {
      case 'on':
        switch1.on()
        break
      case 'off':
        switch1.off()
        break
    }
  }
  else if (evt.name == "switch2") {
    switch (evt.value) {
      case 'on':
        switch2.on()
        break
      case 'off':
        switch2.off()
        break
    }
  }
}
def rsmRefresh() {
	rsm.refresh()
}
