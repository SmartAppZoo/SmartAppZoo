/**
 *  Knockerz
 *
 *  Author: paul.knight@delmarvacomputer.com
 *  Date: 6/17/17
 *
 *  Based on the work of brian@bevey.org in 2013.
 *
 *  Notifies when someone knocks on a door, but does not open it.
 *  Alerts are by push, SMS, PushBullet, audio, and/or by
 *  turning on a switch and/or dimming the device.
 *
 *  v2.0.1 - Major workflow/UI revisions 4/22/20
 *  v1.2.0 - Added Echo Speaks support 4/13/20
 *  v1.1.0 - Added notification restrictions 2/12/20
 *  v1.0.0 - Initial release 6/17/17
 */

definition(
    name: "Knockerz",
    namespace: "dca",
    author: "paul.knight@delmarvacomputer.com",
    description: "Alerts when there is a knock at a door.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
  page name:"pageSetup"
  page name:"pageDoors"
  page name:"pageNotifications"
  page name:"pageVoice"
  page name:"pageRestrictions"
  page name:"pageLicense"
}

/**
 * PAGE METHODS
 **/

/**
 * Main page
 * The "Doors" and "Notification Options" configuration pages, along
 * with the "About" page are accessed from this page. The SmartApp
 * can also be renamed or uninstalled from here.
 *
 * @return a dynamically created main page
 */
def pageSetup() {
  LOG("pageSetup()")

  def hrefGithub = [
    url:            "https://github.com/n3pjk/Knockerz/",
    style:          "embedded",
    title:          "More information...",
    description:    "https://github.com/n3pjk/Knockerz/blob/master/README.md",
    required:       false
  ]

  def hrefPaypal = [
    url:            "https://www.paypal.me/n3pjk",
    title:          "If you like this smartapp, please support the developer via PayPal using the link below"
  ]

  def hrefDoors = [
    page:           "pageDoors",
    title:          "Doors",
    description:    "Tap to open"
  ]

  def hrefLicense = [
    page:           "pageLicense",
    title:          "License",
    description:    "Tap to view license"
  ]

  def hrefNotifications = [
    page:           "pageNotifications",
    title:          "Notification Options",
    description:    "Tap to open"
  ]

  def hrefVoice = [
    page:           "pageVoice",
    title:          "Voice Notification Options",
    description:    "Tap to open"
  ]

  def hrefRestrictions = [
    page:           "pageRestrictions",
    title:          "Notification Restrictions",
    description:    "Tap to open"
  ]

  def inputLabel = [
    title:          "Assign a name",
    required:       false
  ]

  def pageProperties = [
    name:           "pageSetup",
    nextPage:       "pageDoors",
    install:        false,
    uninstall:      state.installed
  ]

  return dynamicPage(pageProperties) {
    section("About") {
      paragraph "${app.getName()}, the SmartApp that notifies when someone " +
        "knocks on a door, but does not open it. Alerts are by push, SMS, " +
        "PushBullet, audio, Alexa, setting a switch or dimming a light."
      paragraph "Version ${getVersion()}"
      paragraph "${textCopyright()}"
      href hrefLicense
      href hrefGithub
    }
    section("Paypal Donation") {
      href hrefPaypal
    }
    if (state.installed) {
      section("Setup Menu") {
        href hrefDoors
        href hrefNotifications
        href hrefVoice
        href hrefRestrictions
      }
    }
    section([mobileOnly:true],"Rename SmartApp") {
      label  inputLabel
    }
  }
}

/**
 * "Doors" page
 * Configure acceleration, contact sensors, and delay to wait after
 * a knock is detected to see if the door opens.
 *
 * @return a dynamically created "Doors" page
 */
def pageDoors() {
  LOG("pageDoors()")

  def helpAbout =
    "Select acceleration and contact sensors, then " +
    "set delay after knock to see if door opens."

  def inputAccelerationSensors = [
    name:           "accelerationSensors",
    title:          "Listen For Knocks At",
    type:           "capability.accelerationSensor",
    multiple:       true,
    required:       true
  ]

  def inputContactSensors = [
    name:           "contactSensors",
    title:          "See If These Doors Open",
    type:           "capability.contactSensor",
    multiple:       true,
    required:       true
  ]

  def inputKnockDelay = [
    name:           "knockDelay",
    title:          "Knock Delay in seconds",
    type:           "number",
    defaultValue:   3,
    required:       true
  ]

  def pageProperties = [
    name:           "pageDoors",
    nextPage:       "pageNotifications",
    title:          "Doors",
    uninstall:      state.installed
  ]

  return dynamicPage(pageProperties) {
    section("Instructions") {
      paragraph helpAbout
    }
    section("Select Doors") {
      input inputAccelerationSensors
      input inputContactSensors
      input inputKnockDelay
    }
  }
}

/**
 * "Notification Options" page
 * Define what happens if someone is actually knocking on a door.
 * Switches can be turned on, and, optionally dimmed. A message
 * can be sent via push, text, or PushBullet.
 *
 * @return a dynamically created "Notifications Options" page
 */
def pageNotifications() {
  LOG("pageNotifications()")

  def helpAbout =
    "How do you want to be notified of a knock at a " +
    "door? Turn on a switch, a chime, or dim a light. " +
    "Send an SMS text message, push or PushBullet " +
    "announcement."

  def inputNotifySwitches = [
    name:           "notifySwitches",
    type:           "capability.switch",
    title:          "Set these switches",
    multiple:       true,
    required:       false,
    submitOnChange: true
  ]

  def inputNotifyDimmerLevel = [
    name:           "notifyDimmerLevel",
    type:           "enum",
    metadata:       [values:["10%","20%","30%","40%","50%","60%","70%","80%","90%","100%"]],
    title:          "Dimmer Level",
    defaultValue:   "40%",
    required:       false
  ]

  def inputMessageText = [
    name:           "messageText",
    type:           "text",
    title:          "Message Phrase",
    defaultValue:   "%door detected a knock.",
    required:       false
  ]

  def inputSendPush = [
    name:           "sendPush",
    type:           "bool",
    title:          "Send Push on Knock",
    defaultValue:   true
  ]

  def inputContacts = [
    name:           "contacts",
    type:           "contact",
    title:          "Send notifications to",
    multiple:       true,
    required:       false
  ]

  def inputPhone = [
    name:           "phone",
    type:           "phone",
    title:          "Send to this number",
    required:       false
  ]

  def inputPushbulletDevice = [
    name:           "pushbullet",
    type:           "device.pushbullet",
    title:          "Which Pushbullet devices?",
    multiple:       true,
    required:       false
  ]

  def pageProperties = [
    name:           "pageNotifications",
    nextPage:       "pageVoice",
    title:          "Notification Options",
    uninstall:      state.installed
  ]

  return dynamicPage(pageProperties) {
    section("Instructions") {
      paragraph helpAbout
    }
    section("Turn On Switches") {
      input inputNotifySwitches
      if (settings.notifySwitches) {
        input inputNotifyDimmerLevel
      }
    }
    section("Push & SMS Notifications") {
      input inputMessageText
      input("contacts", "contact", title: "Send notification to") {
        input inputSendPush
        input inputPhone
      }
    }
    section("Pushbullet Notifications") {
      input inputPushbulletDevice
    }
  }
}

/**
 * "Voice Notification Options" page
 * Define speech, TTS, audio, and Alexa device options.
 *
 * @return a dynamically created "Voice Notification Options" page
 */
def pageVoice() {

  def helpAbout =
    "Do you prefer an audio or voice announcement? Enter the phrase you want " +
    "spoken, '%door' will be replaced by the name of the sensor. Amazon Echo " +
    "requires Echo Speaks. Non-TTS audio devices are not yet supported."

  def inputSpeechText = [
    name:           "speechText",
    type:           "text",
    title:          "Knock Phrase",
    defaultValue:   "There is a knock at the %door",
    required:       false
  ]

  def inputAudioPlayers = [
    name:           "audioPlayers",
    type:           "capability.musicPlayer",
    title:          "Which audio players?",
    multiple:       true,
    required:       false
  ]

  def inputUseTTS = [
    name:           "useTTS",
    type:           "bool",
    title:          "Supports TTS?",
    defaultValue:   true,
    required:       true
  ]

  def inputSpeechDevices = [
    name:           "speechDevices",
    type:           "capability.speechSynthesis",
    title:          "Which speech devices?",
    multiple:       true,
    required:       false
  ]

  def inputEchoDevice = [
    name:           "echoSpeaks",
    type:           "capability.musicPlayer",
    title:          "Select an Amazon Echo Device",
    multiple:       false,
    required:       false,
    submitOnChange: true
  ]

  def inputEchoAll = [
    name:           "echoAll",
    type:           "bool",
    title:          "Announce on all Echo devices?",
    defaultValue:   true,
    required:       false
  ]

  def pageProperties = [
    name:           "pageVoice",
    nextPage:       "pageRestrictions",
    title:          "Voice Notification Options",
    uninstall:      state.installed,
    hideWhenEmpty:  true
  ]

  return dynamicPage(pageProperties) {
    section("Instructions") {
      paragraph helpAbout
    }
    section("Content") {
      input inputSpeechText
    }
    section("Speech") {
      input inputSpeechDevices
    }
    section("Audio") {
      input inputAudioPlayers
      input inputUseTTS
    }
    section("Echo Speaks") {
      input inputEchoDevice
      if (settings.echoSpeaks) {
        input inputEchoAll
      }
    }
  }
}

/**
 * "Notification Restrictions" page
 * Define when to allow notifications to be sent. Based on time
 * of day, day of week, modes or switches in a defined state.
 *
 * @return a dynamically created "Notification Restrictions" page
 */
def pageRestrictions() {
  LOG("pageRestrictions()")

  def helpAbout =
    "Restrict when you will receive door knock notifications " +
    "by time of day, day of week, house mode, or when one or " +
    "more switches are on or off."

  def inputStartTime = [
    name:           "startTime",
    type:           "time",
    title:          "Starting time",
    required:       false
  ]

  def inputStopTime = [
    name:           "stopTime",
    type:           "time",
    title:          "Ending time",
    required:       false,
    submitOnChange: true
  ]

  def inputWeekDays = [
    name:           "weekDays",
    type:           "enum",
    options:        ["Sunday": "Sunday","Monday": "Monday","Tuesday": "Tuesday","Wednesday": "Wednesday","Thursday": "Thursday","Friday": "Friday","Saturday": "Saturday"],
    title:          "These days of the week",
    multiple:       true,
    required:       false
  ]

  def inputNotifyModes = [
    name:           "notifyModes",
    type:           "mode",
    title:          "These modes",
    multiple:       true,
    required:       false
  ]

  def inputControlSwitches = [
    name:           "controlSwitches",
    type:           "capability.switch",
    title:          "These switches",
    multiple:       true,
    required:       false,
    submitOnChange: true
  ]

  def inputControlSwitchState = [
    name:           "controlSwitchState",
    type:           "enum",
    metadata:       [values:["on","off"]],
    title:          "Are",
    defaultValue:   "on",
    multiple:       false,
    required:       true,
    submitOnChange: true
  ]

  def inputSetSwitchState = [
    name:           "setSwitchState",
    type:           "bool",
    title:          "Turn switches ${settings.notifySwitchState} afterward?",
    defaultValue:   false,
    required:       false
  ]

  def pageProperties = [
    name:           "pageRestrictions",
    nextPage:       "pageSetup",
    title:          "Notification Restrictions",
    install:        true,
    uninstall:      state.installed
  ]

  return dynamicPage(pageProperties) {
    section("Instructions") {
      paragraph helpAbout
    }
    section("Notify between") {
      input inputStartTime
      if (settings.startTime) {
        input inputStopTime
      }
    }
    section("Notify on") {
      input inputWeekDays
    }
    section("Notify when the house is in") {
      input inputNotifyModes
    }
    section("Notify when") {
      input inputControlSwitches
      if (settings.controlSwitches) {
        input inputControlSwitchState
        input inputSetSwitchState
      }
    }
  }
}

/**
 * "License" page
 * Display the license for this SmartApp.
 *
 * @return a dynamically created "License" page
 */
def pageLicense() {
  LOG("pageLicense()")

  def pageProperties = [
    name:           "pageLicense",
    nextPage:       null,
    title:          "License",
    install:        false,
    uninstall:      false
  ]

  dynamicPage(pageProperties) {
    section("GNU General Public License v3") {
      paragraph textLicense()
    }
  }
}

/**
 * APP INIT
 **/

def installed() {
    LOG("installed()")

    initialize()
    state.installed = true
}

def updated() {
    LOG("updated()")

    unsubscribe()
    initialize()
}

def initialize() {
  log.info "Knockerz. Version ${getVersion()}. ${textCopyright()}"
  LOG("settings: ${settings}")

  state.lastClosed = 0
  subscribe(settings.accelerationSensors, "acceleration.active", onMovement)
  subscribe(settings.contactSensors, "contact.closed", onContact)

  STATE()
}

/**
 * EVENT HANDLERS
 **/

/**
 * Check the specific contact sensor to see if the door is open or
 * was openned in the last 60 seconds.
 *
 * @param a map containing the name of the detecting acceleration sensor.
 */
def checkMultiSensor(data) {
  LOG("checkMultiSensor(${data.name})")

  def contactSensor = settings.contactSensors.find{ it.label == "${data.name}" || it.name == "${data.name}" }
  LOG("Using ${contactSensor?.label ?: contactSensor?.name} contact sensor")
  if ((contactSensor?.latestValue("contact") == "closed") && (now() - (60 * 1000) > state.lastClosed)) {
    LOG("${data.name} detected a knock.")
    notify("${data.name}")
  } else {
    LOG("${data.name} detected acceleration, but appears to be just someone opening the door.")
  }
}

/**
 * Check if any door is open or was openned in the last 60 seconds.
 *
 * @param a map containing the name of the detecting acceleration sensor.
 */
def checkAnySensor(data) {
  LOG("checkAnySensor(${data.name})")

  if (settings.contactSensors.any { it.latestValue("contact") == "open" }) {
    LOG("${data.name} knocked, but a door is open.")
  } else {
    if (now() - (60 * 1000) > state.lastClosed) {
      LOG("${data.name} detected a knock.")
      notify("${data.name}")
    } else {
      LOG("${data.name} detected acceleration, but appears to be just someone opening the door.")
    }
  }
}

/**
 * Acceleration Event Handler
 * Use one of the check handlers depending on whether we can
 * specifically identify the contact sensor or not.
 *
 * @param an acceleration event object
 */
def onMovement(evt) {
  LOG("onMovement(${evt.displayName})")

  def contactSensor = settings.contactSensors.find{ it.label == "${evt.displayName}" || it.name == "${evt.displayName}" }
  if (contactSensor) {
    runIn(settings.knockDelay, "checkMultiSensor", [data: [name: "${evt.displayName}"]])
  } else {
    LOG("${evt.displayName} is a ${accelerationSensor.name}")
    runIn(settings.knockDelay, "checkAnySensor", [data: [name: "${evt.displayName}"]])
  }
}

/**
 * Contact Event Handler
 * Saves the last time a contact was closed.
 *
 * @param a contact event object
 */
def onContact(evt) {
  LOG("onContact(${evt.displayName})")
  state.lastClosed = now()
}

/**
 * NOTIFICATION HANDLERS
 **/

/**
 * Main notification processor
 * Turns on and dims switches, calls additional notification methods.
 *
 * @param the name of the acceleration sensor that detected the knock.
 */
private notify(name) {
  LOG("notify(${name})")

  // Determine if conditions permit notification
  def restricted = notifyRestrictions()
  if (!restricted) {
    def msg = textMessage(name)

    // Only turn on those switches that are currently off
    def switchesOn = settings.notifySwitches?.findAll { it?.currentSwitch == "off" }
    LOG("switchesOn: ${switchesOn}")
    if (switchesOn) {
      switchesOn*.on()
    }

    // TODO: Add camera support?
    //settings.cameras*.take()

    // Standard SMS, push and PushBullet
    if (contacts) {
      notifyContacts(msg)
    } else {
      notifyPush(msg)
      notifyText(msg)
    }
    notifyPushBullet(msg)

    // Voice notification options
    notifyEcho(name)
    notifyAudio(name)
    notifySpeech(name)
  } else {
    LOG("notification restricted")
  }
}

/**
 * Check restrictions to notification
 */
private def notifyRestrictions() {
  LOG("notifyRestrictions()")

  // Create and ensure the data object is set to local time zone
  def df = new java.text.SimpleDateFormat("EEEE")
  df.setTimeZone(location.timeZone)

  // Is today a selected day of the week?
  if (settings.weekDays) {
    def day = df.format(new Date())
    def dayCheck = settings.weekDays.contains(day)
    if (!dayCheck) {
      LOG("Not an allowed weekday")
      return true
    }
  }

  // Is the time within the specified interval?
  if (settings.startTime && settings.stopTime) {
    def timeCheck = timeOfDayIsBetween(settings.startTime, settings.stopTime, new Date(), location.timeZone)
    if (!timeCheck) {
      LOG("Outside time of day")
      return true
    }
  }

  // Is the house in a selected mode?
  if (settings.notifyModes) {
    def modeCheck = settings.notifyModes.contains(location.currentMode)
    if (!modeCheck) {
      LOG("Not allowed in ${location.currentMode} mode")
      return true
    }
  }

  // Are any switches set to disable notifications?
  def switchCheck = settings.controlSwitches?.findAll { it?.currentSwitch != settings.controlSwitchState }
  if (switchCheck) {
    LOG("Switches not ${settings.controlSwitchState}: ${switchCheck}")
    if (settings.setSwitchState) {
      LOG("Setting to ${settings.controlSwitchState}: ${switchCheck}")
      if (settings.controlSwitchState == "on") {
        switchCheck*.on()
      } else {
        switchCheck*.off()
      }
    }
    return true
  }

  return false
}

/**
 * Process message to Contact Book
 *
 * @param the message to send
 */
private def notifyContacts(msg) {
  LOG("notifyContacts(${msg})")

  sendNotificationToContacts(msg, contacts)
}

/**
 * Process a push message
 *
 * @param the message to send
 */
private def notifyPush(msg) {
  LOG("notifyPush(${msg})")

  if (settings.sendPush) {
    // sendPush can throw an exception
    try {
      sendPush(msg)
    } catch (e) {
      log.error e
    }
  } else {
    sendNotificationEvent(msg)
  }
}

/**
 * Process a text message
 *
 * @param the message to send
 */
private def notifyText(msg) {
  LOG("notifyText(${msg})")

  if (settings.phone) {
    sendSms(phone, msg)
  }
}

/**
 * Process a PushBullet message
 *
 * @param the message to send
 */
private def notifyPushBullet(msg) {
  if (settings.pushbullet) {
    settings.pushbullet*.push(location.name, msg)
  }
}

/**
 * Process a text-to-speech message using audio. Note that the
 * string '%door' in the message text will be replaced with the
 * name of the acceleration sensor that detected the knock.
 *
 * @param the name of the acceleration sensor that detected the knock.
 */
private def notifyAudio(name) {
  LOG("notifyAudio(${name})")

  if (!settings.audioPlayers) {
    return
  }

  if (settings.useTTS) {
    // Replace %door with name
    def phrase = textSpeech(name)

    if (phrase) {
      settings.audioPlayers*.playText(phrase)
    }
  }
}

/**
 * Process a text-to-speech message using speech synthesis. Note
 * that the string '%door' in the message text will be replaced
 * with the name of the acceleration sensor that detected the
 * knock.
 *
 * @param the name of the acceleration sensor that detected the knock.
 */
private def notifySpeech(name) {
  LOG("notifySpeech(${name})")

  if (!settings.speechDevices) {
    return
  }

  // Replace %door with name
  def phrase = textSpeech(name)

  if (phrase) {
    settings.speechDevices*.speak(phrase)
  }
}

private def notifyEcho(name) {
  LOG("notifyEcho(${name})")

  if (!settings.echoSpeaks) {
    return
  }

  // Replace %door with name
  def phrase = textSpeech(name)

  if (phrase) {
    if (settings.echoAll) {
      settings.echoSpeaks*.playAnnouncementAll(phrase,app.getName())
    } else {
      settings.echoSpeaks*.playAnnouncement(phrase,app.getName())
    }
  }
}

private def textMessage(name) {
  def text = settings.messageText.replaceAll('%door', name)
}

private def textSpeech(name) {
  def text = settings.speechText.replaceAll('%door', name)
}

private def getVersion() {
  return "2.0.1"
}

private def textCopyright() {
  def text = "Copyright © 2017-20 Delmarva Computer Associates LLC"
}

private def textLicense() {
  def text =
    "This program is free software: you can redistribute it and/or " +
    "modify it under the terms of the GNU General Public License as " +
    "published by the Free Software Foundation, either version 3 of " +
    "the License, or (at your option) any later version.\n\n" +
    "This program is distributed in the hope that it will be useful, " +
    "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
    "General Public License for more details.\n\n" +
    "You should have received a copy of the GNU General Public License " +
    "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def LOG(message) {
  def appID = app.getLabel()
  if (appID == null) {
    appID = app.getName()
  }
  log.trace "${appID}> " + message
}

private def STATE() {
  log.trace "state: ${state}"
}