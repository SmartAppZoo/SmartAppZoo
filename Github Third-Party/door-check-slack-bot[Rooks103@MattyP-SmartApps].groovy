/**
 *  SmartApp for being able to monitor any contact sensors and if open, send a normal, @here, and 
 *  @channel message once per day to remind people to close them. Intended use casewould to be have them 
 *  be setup in an escalating manner to ensure doors are closed at the end of a day. 
 *  Additionally, the app will check every 2 minutes to determine if the doors have been closed, and if so, 
 *  send a normal message back to Slack to notify people that the doors were closed.
 *
 *  Author: Matt Peterson
 */
 
include 'asynchttp_v1'
 
definition (
    name: "Door Check Slack Bot",
    namespace: "rooks103",
    author: "Matt Peterson",
    description: "SmartApp for sending Slack Notifications when a door is open beyond a certain time",
    category: "SmartThings Labs",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics13-icn@2x.png"
)

preferences {
    page(name: "App Configuration", title: "Setup", install: true, uninstall: true) {
        section("Sensor Configuration") {
            input "officeDoors", "capability.contactSensor", title: "Doors that will be monitored.", required: true, multiple: true
        }
        section("Time Configuration") {  
            input "normalTime", "time", title: "Normal Time", required: true, description: "When a standard slack message will be sent"
            input "hereTime", "time", title: "Here Time", required: true, description: "When a @here slack message will be sent"
            input "channelTime", "time", title: "Channel Time", required: true, description: "When a @channel slack message will be sent"
            input "endTime", "time", title: "Stop Time", required: true, description: "When to stop checking if doors are open"
        }
        section("Slack Configuration") {
            input "slackURI", "text", title: "Slack Instance", required: true, description: "URI for Slack Instacne e.g. smartthings.slack.com"
            input "slackChannel", "text", title: "Slack Channel", required: true, description: "Channel to get message e.g. #general"
            input "slackToken", "password", title: "Slack API Token", required: true, description: "API Token for Slackbot"
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: true
        }
    }
}

def installed() {   
    log.debug "It's installed."
    initialize()
}

def updated() {
    log.debug "It's updated."
    initialize()
}

def initialize() {
    state.wereDoorsOpenPreviously = false
    state.hasCloseMessageBeenSent = true
    schedule("0 0/2 * ? * MON-FRI", openDoorCheckMethod)
    schedule(normalTime, normalSenderMethod)
    schedule(hereTime, hereSenderMethod)
    schedule(channelTime, channelSenderMethod)
}

private Map slackMsgBuilder(message) {
    log.debug "Entering slack message builder."
    Map slackParams = [
        uri: "https://$slackURI/api/chat.postMessage",
        headers: [
            "Authorization": "Bearer $slackToken"
        ],
        body: [
            channel: "$slackChannel",
            text: "$message",
            icon_emoji: ":bender2:",
            as_user: true
        ]
    ]
    return slackParams
}

private Map messagePicker(msgType, openDoors) {
    log.debug "Entering message picker."
    Random rand = new Random()
    String[] openQuotes = ["Hey! My sensors indicate open doors. One of you meatbags go close them. I'm busy doing nothing.",
              "For real. Someone go close the doors. The draft from them is messing with my cigars.",
              "I know you silly humans aren't as cool as me, and it makes you sad, but seriously. Close the doors!",
              "I don't remember closing the door, because I didn't. Someone else go close them.",
              "You are going to be so sorry when I envoke the wrath of Hann on you meatbags. Go forth and close... or else.",
              "If you think I'm going to close these, you can bite my shiny metal... well you know.",
              "Might be time for a Bender bender with the kitchen booze. Better close the doors or it's mine.",
              "You think I'm going to close the doors? Hahahahaha... oh wait, you're serious? Let me laugh even harder."]
    String[] closeQuotes = ["Looks like you closed the doors. Good work. What? You thought there was a prize? Hahaha...",
              "Hey, I'm impressed. You folks actually followed directions. Good humans.",
              "Wow! The doors are now closed! Beer for everyone, you can buy."]
    String quote = ""
    if (msgType == 'channel') {
        quote = "<!channel|channel> " + openQuotes[rand.nextInt(openQuotes.length)] + "\n*Open Doors:* ${openDoors}"
    } else if (msgType == 'here') {
        quote = "<!here|here> " + openQuotes[rand.nextInt(openQuotes.length)] + "\n*Open Doors:* ${openDoors}"
    } else if (msgType == "normal") {
        quote = openQuotes[rand.nextInt(openQuotes.length)] + "\n*Open Doors:* ${openDoors}"
    } else if (msgType == "closed") {
        quote = closeQuotes[rand.nextInt(closeQuotes.length)]
    }
    return slackMsgBuilder(quote)
}

private String[] findOpenDoors() {
    String[] openDoors = officeDoors.findAll { doorVal ->
        doorVal.currentContact == "open" ? true : false
    }
    return openDoors
}

def normalSenderMethod() {
    log.debug "Normal message"
    if (findOpenDoors().size() != 0) {
    	state.hasCloseMessageBeenSent = false
        asynchttp_v1.post(processResponse, messagePicker("normal", "${findOpenDoors()}"))
    }
}

def hereSenderMethod() {
    log.debug "Here message"
    if (findOpenDoors().size() != 0) {
        asynchttp_v1.post(processResponse, messagePicker("here", "${findOpenDoors()}"))
    }
}

def channelSenderMethod() {
    log.debug "Channel message"
    if (findOpenDoors().size() != 0) {
        asynchttp_v1.post(processResponse, messagePicker("channel", "${findOpenDoors()}"))
    }
}

def openDoorCheckMethod() {
    String[] openDoors = findOpenDoors()
    if (timeOfDayIsBetween(normalTime, endTime, new Date(), location.timeZone)) {
        log.debug "Time is right"
        if (openDoors.size() != 0) {
            log.debug "Doors are open"
            state.wereDoorsOpenPreviously = true
        } else {
            log.debug "No open doors."
            log.debug "${state.wereDoorsOpenPreviously} || ${state.hasCloseMessageBeenSent}"
            if (state.wereDoorsOpenPreviously && !state.hasCloseMessageBeenSent) {
                log.debug "Sending door closure."
                asynchttp_v1.post(processResponse, messagePicker("closed", "${findOpenDoors()}"))
                state.hasCloseMessageBeenSent = true
            }
        }
    }
    else {
        log.debug "Outside the specified timeframe for sending messages."
        state.wereDoorsOpenPreviously = false
    }
}

private void processResponse(response, data) {
    log.debug "Response Handler"
}