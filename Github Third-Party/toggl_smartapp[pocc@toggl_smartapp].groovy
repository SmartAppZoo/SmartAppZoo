/**
 *  Code is being run on Samsung Smartthings online IDE
 *  Copyright 2020 Ross Jacobs
 *  License: Apache 2.0
 */
definition(
  name: "The Button",
  namespace: "smartthings",
  author: "SmartThings",
  description: "Call webhook when button is pressed.",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png 3",
  iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png 1"
)

preferences {
  section("When this Button is Pressed") {
    input "buttonDevice", "capability.button",
    title: "Choose Button"
  }
  section("Push Project") {
    input "push_project", "text", 
    required: true, 
    title: "Project Name (for push action)"
  }
  section("Push Task") {
    input "push_description", "text", 
    required: true, 
    title: "What you're doing (for push action)"
  }
   section("Double Project") {
    input "double_project", "text", 
    required: false, 
    title: "Project Name (for double action), blank for noop"
  }
  section("Double Task") {
    input "double_description", "text", 
    required: false, 
    title: "What you're doing (for double action), blank for noop"
  }
  section("Toggl API key") {
    input "apikey", "text", 
    required: true, 
    title: "API key (find me @ https://track.toggl.com/profile)"
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  subscribe(buttonDevice, "button", buttonEventHandler)
  initialize()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  subscribe(buttonDevice, "button", buttonEventHandler)
}

def initialize() {
  log.debug "Installed with settings: ${settings}"
}

// Start a timer if a button is pressed. Stop the timer on double press.
def buttonEventHandler(evt) {
  def buttonNumber = evt.jsonData.buttonNumber
  log.debug buttonNumber
  log.debug "${evt.value}"
  def action, project, description
  if ("${evt.value}" == "pushed") {
    action = "start"
    project = push_project
    description = push_description
  } else if ("${evt.value}" == "double") {
    if (double_project.length() == 0 || double_description.length() == 0) {
      return // If double hasn't been setup, noophttps://graph-na04-useast2.api.smartthings.com/ide/app/editor/9360bcdb-0189-400d-b9ce-19e7e209c33c#
    } else {
      action = "start"
      project = double_project
      description = double_description
    }
  } else if ("${evt.value}" == "held") {
    description = ""
    project = ""
    action = "stop"
  } else {
    sendPush("Problem parsing value: ${evt.value}")
    return // With an unknown value, do nothing
  }
  def params = [
    "uri": "http://157.245.238.3:5432",
    "path": "/toggl_smartapp",
    "query": [
      "action": action,
      "desc": description.trim(),
      "project": project.trim(),
      "toggl_api_token": apikey.trim(),
    ],
    "Content-Type": "text/html"
  ]
  def content = ""
  try {
    httpGet(params) { resp ->
      // Returning a non 200 resp.status will cause this to crash
      // So we're skipping that on the proxy server and providing error data with a 200.
      log.debug "DATA: ${resp.data}\nSTATUS:${resp.status}"
      if (action == "start") {
        content = "Timer for ${description} in ${project} started."
      } else {
        content = "Timer stopped."
      }
      if (resp.data.toString().startsWith("ERR")) { // If there's an error, send it to the user
        content = content + resp.data
      }
    }
  } catch (e) {
    content = "something went wrong: $e"
  }
  sendPush("EVENT: Button ${evt.value}\nMESSAGE: ${content}")
}
