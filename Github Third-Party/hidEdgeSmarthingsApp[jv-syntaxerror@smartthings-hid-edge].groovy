
definition(
    name: "HID Edge Integration",
    namespace: "",
    author: "Jeff Vacaro <product@vaccaros.net>",
    description: "Integration to operate with HID Edge",
    category: "My Apps",
    iconUrl: "http://www.radenco.com/images/icon-card-reader.jpg",
    iconX2Url: "http://www.radenco.com/images/icon-card-reader.jpg",
    oauth: true
)

import groovy.json.JsonBuilder

preferences {
  section("Notifications (optional) - NOT WORKING:") {
    input "sendPush", "enum", title: "Push Notifiation", required: false,
      metadata: [
       values: ["Yes","No"]
      ]
    input "phone1", "phone", title: "Phone Number", required: false
  }
  section("Notification events (optional):") {
    input "notifyEvents", "enum", title: "Which Events?", description: "default (none)", required: false, multiple: false,
     options:
      ['all','alarm','closed','open','closed','partitionready',
       'partitionnotready','partitionarmed','partitionalarm',
       'partitionexitdelay','partitionentrydelay'
      ]
  }
}

mappings {
    path("/receiveToken") {
        action: [
            POST: "receiveToken",
            GET: "receiveToken"
        ]
    }
}

def receiveToken() {
    state.sampleAccessToken = params.access_token
    render contentType: 'text/html', data: "<html><body>Saved. Now click 'Done' to finish setup.</body></html>"
}

def installed() {
  log.debug "Installed!"
}

def updated() {
  log.debug "Updated!"
}

private update() {
}

private sendMessage(msg) {
    def newMsg = "Alarm Notification: $msg"
    if (phone1) {
        sendSms(phone1, newMsg)
    }
    if (sendPush == "Yes") {
        sendPush(newMsg)
    }
}
