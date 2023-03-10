/**
 *  
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
 *  Control the part time door locks
 * "access_token": "d4940593-e86e-4508-b581-9321009faeb0",
 * d4940593-e86e-4508-b581-9321009faeb0
 * d4940593-e86e-4508-b581-9321009faeb0
 * https://graph.api.smartthings.com/api/smartapps/installations/354c0cf1-13dc-435e-b76b-901cf9e7f4d7
 *
 *curl -H "Authorization: Bearer d4940593-e86e-4508-b581-9321009faeb0" -X PUT "https://graph.api.smartthings.com/api/smartapps/installations/354c0cf1-13dc-435e-b76b-901cf9e7f4d7/program/4:11333"
 * curl -H "Authorization: Bearer d4940593-e86e-4508-b581-9321009faeb0" "https://graph.api.smartthings.com/api/smartapps/installations/354c0cf1-13dc-435e-b76b-901cf9e7f4d7/poll"
 *   Supported commands: [poll, lock, unlock, refresh, lock, unlock, setCode, deleteCode, requestCode, reloadAllCodes, unlockWithTimeout, setCodeLength, nameSlot, updateCodes, unlockwtimeout, setBeeperMode, setVacationMode, setLockLeave, setAlarmMode, setAlarmSensitivity, setLocalControl, setAutoLock, setPinLength
 * Author: Jcpearce
 *  Date: 2017-07-20
 */

definition(
    name: "Control lock 321",
    namespace: "OSIOFFICES",
    author: "jcpearce",
    description: "Controls  part time door lock 321.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

preferences {
	
	section("Control lock..."){
		  input(name: "lock", title: "Which Lock?", type: "capability.lock", multiple: false, required: true)
    
	}
}

def installed()
{
log.debug "in C-lock installed"	
subscribe(lock, "lock", codeUsed)
   subscribe(lock, 'codeReport',codeUsed, [filterEvents:false])
     subscribe(lock, "battery", batteryHandler)
}

def updated()
{
log.debug "in C-Lock updated"
	unsubscribe()
    
    subscribe(lock, "lock", codeUsed)
   
      subscribe(lock, 'codeReport',codeUsed, [filterEvents:false])
	  subscribe(lock, "battery", batteryHandler)
}

def codeUsed(evt) {
log.trace "In code used OSI Control the lock"
log.trace evt.data
  def lockId = lock.id
  def message = ''
  def action = evt.value
  def userApp = false
  def codeUsed = false
  def manualUse = false
  def data = false
  log.debug "Code used on lock"
  if (evt.data) {
    data = new JsonSlurper().parseText(evt.data)
    codeUsed = data.usedCode
    log.debug "Code used on log: "+codeUsed +" action" +action
    def params = [
                uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?lock=${lock.displayName}&slot=${codeUsed}",
  
            ]
     try {
                httpPost(params) { resp ->               
                    resp.headers.each {
                        log.debug "${it.name} : ${it.value}"
                    }                  
                }
                
            } catch (e) {
                log.error "something went wrong: $e"
            }
  }

  if (!data || data?.usedCode == 'manual') {
    manualUse = true
  }

  if (action == 'unlocked') {
    // door was unlocked
    
  }
  if (action == 'locked') {
    // door was locked
    
    if (data && data.usedCode == -1) {
      message = "${lock.label} was locked by keypad"
     
    }
    if (manualUse) {
      // locked manually
     
    }
  }


}
def clearCodes() {
// the first 5 are reserved, delete the last 25
log.debug "Trying to clear all codes"
for (def i = 5; i <30; i++) {
   lock.deleteCode(i)
}




}
def showPrograms() {
log.debug "in empty show programs method"
}
def lockDoor() {
log.debug "in C-lock lock Door"

lock.lock()
}
def unlockDoor() {
log.debug "in C-lock-unlock door"
def command=params.command
log.debug "command passed: "${command}
lock.unlock()
return "test"
}
def doPoll() {
log.debug "Polling lock"
//log.debug lock.poll()
//def res=lock.requestCode(4)

//log.debug "finished polling" ${lock.requestCode(4)}

   // log.trace lock.currentState()
}
def doRefresh() {
log.debug "Refresh lock"
log.debug lock.refresh()
log.debug "finished refresh"

   // log.trace lock.currentState()
}
def straightunlock() {
log.debug "in straightunlock door"
def command=params.command
log.debug "command passed: "${command}
lock.unlock()
return "test"
}
def batteryHandler(evt) {
    log.debug "Battery Lock Event value: ${evt.value}%"
    log.debug "Battery Lock Event device: ${evt.device}"
    
     def params = [
    uri: "https://osiitservices.com/osiportal/pub/rest/monitorcapture.xhtml?room=${lock.displayName}&battery=${evt.value}",    
]
try {
    httpPost(params) { resp ->
        // iterate all the headers
        // each header has a name and a value
        resp.headers.each {
       //    log.debug "${it.name} : ${it.value}"
        }

        // get an array of all headers with the specified key
        def theHeaders = resp.getHeaders("Content-Length")

    
    }
} catch (e) {
    log.error "something went wrong: $e"
}

    
    
}

mappings {
  path("/locks") {
    action: [
      GET: "listSwitches"
    ]
  }
   path("/clear") {
    action: [
      GET: "clearCodes"
    ]
  }
    path("/program") {
    action: [
      GET: "showPrograms"
    ]
  }
   path("/poll") {
    action: [
      GET: "doPoll"
    ]
  }
  path("/locks/:command") {
    action: [
      PUT: "updateSwitches"
    ]
  }
  path("/program/:command") {
    action: [
      PUT: "programLock"
    ]
  }
  
}
void updateSwitches() {
    // use the built-in request object to get the command parameter
    def command = params.command

    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
    log.debug  command
    log.trace  command
  
    switch(command) {
        case "code":
           log.debug "trying to code a lock "
          lock.setCode(12,"7881");
            break
        case "unlock":
           log.debug "unlock "
           lock.unlock()
            break
        case "lock":
            log.debug "lock"
            lock.lock()
            break
        default:
            httpError(400, "$command is not a valid command the lock")
    }
}
void programLock() {
    // use the built-in request object to get the command parameter
    def command = params.command
     log.trace  command
     
  // get the slot and get the code
    def split=command.split(":")
    log.debug split[0]
    log.debug "slot" 
     log.debug split[1]
    log.debug "code" 
    lock.setCode(split[0].toInteger(),split[1]);
    // all switches have the command
    // execute the command on all switches
    // (note we can do this on the array - the command will be invoked on every element
   // log.debug  command
   // log.trace  command
    
    
        //    httpError(200, "Set code")
    
}