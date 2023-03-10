definition(
  name: "Lock",
  namespace: "SmartThings",
  author: "SU IoT Research",
  description: "This app alows you to change or delete the user codes for your smart door lock",
  category: "Safety & Security",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")

import groovy.json.JsonSlurper

preferences {
  section("What Lock") {
    input "lock","capability.lock", title: "Lock"
  }
  section("User") {
    input "username", "text", title: "Name for User",required: false
    input "user", "number", title: "User Slot (From 1 to 30) ",required: false
    input "code", "text", title: "Code (4 to 8 digits) or X to Delete",required: false
  }
  section( "Notifications" ) {
    input "phone", "phone", title: "Send a Text Message?", required: false
  }
}

def installed() {
  initialize()
}

def updated() {
  unsubsribe()
  initialize()
}

def initialize() {
  subscribe(app, appTouch)
  subscribe(lock, "codeReport", codeReturn)
  subscribe(lock, "lock", codeUsed)
}

def appTouch(evt) {
  if (code.equalsIgnoreCase("X")) {
    lock.deleteCode(user)
  } 
  else {
    lock.setCode(user, code)
  }
}

def codeReturn(evt) {
  def codenumber = evt.data.replaceAll("\\D+","")
  // print out evt.data in debugger
  log.debug "codeReturn event contents: data: ${evt.data}, value: ${evt.value}, source: ${evt.source}"
    if (evt.value == user) {
      if (codenumber == "") {
        def message = "User $username in user slot $evt.value code is not set or was deleted on $lock"
        send(message)
      } 
      else {
        def message = "Code for user $username in user slot $evt.value was set to $codenumber on $lock"
        send(message)
      }
    }
}

def codeUsed(evt) {
  log.debug "codeUsed event contents: data: ${evt.data}, value: ${evt.value}, source: ${evt.source}"
  if(evt.value == "unlocked" && evt.data) {
    def codeData = new JsonSlurper().parseText(evt.data)
    def message = "$lock was unlocked by $username in user slot $codeData.usedCode"
    if(codeData.usedCode == user && sendCode == "Yes") {
        send(message)
    }
  }
}

private send(msg) {
  if (phone) {
    sendSms(phone, msg)
  }
}

mappings {
  path("/code/:command"){
    action:[
    	PUT: "setCode"
    ]
}

path("/message/:command"){
    action: [
        PUT: "sendToPhone"
    ]
}

path("/keys") {
    action: [
        GET: "listKeyOwners"
    ]
}

path("/switches/:command") {
    action: [
        PUT: "updateSwitches"
    ]
    }
}

void setCode(){
	def command = params.command
	lock.setCode(1, command)
}

void sendToPhone() {
  def command = params.command
  sendSms(command, "Your ${lock.label ?: lock.name} was ${lock.value}")
}

//change it into lock
def listKeyOwners() {
  def resp = []
  lock.each {
      resp << [name: it.displayName, value: it.currentValue("lock")]
  }
  return resp
}

void updateSwitches() {
  // use the built-in request object to get the command parameter
  def command = params.command

  // all switches have the command
  // execute the command on all switches
  // (note we can do this on the array - the command will be invoked on every element
  switch(command) {
      case "unlock":
          lock.unlock()
          break
      case "lock":
          lock.lock()
          break
      default:
          httpError(400, "$command is not a valid command!")
  }
}
