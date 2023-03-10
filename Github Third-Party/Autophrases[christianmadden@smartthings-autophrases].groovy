/**
 *  Autophrases
 *  Author: Christian Madden
 *
 *
 *  Copyright 2015 Christian Madden
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
  name: "Autophrases",
  namespace: "christianmadden",
  author: "Christian Madden",
  description: "Automate Hello Home phrases based on time of day and presence",
  category: "Convenience",
  iconUrl: "https://s3.amazonaws.com/christianmadden.com/i/autophrases/autophrases-icon.png?v=2",
  iconX2Url: "https://s3.amazonaws.com/christianmadden.com/i/autophrases/autophrases-icon@2x.png?v=2"
)

preferences
{
  page(name: "prefsPage", title: "Automate Hello Home phrases based on time of day and presence.", uninstall: true, install: true)
}

def prefsPage()
{
  state.phrases = (location.helloHome?.getPhrases()*.label).sort()
  def days = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

  dynamicPage(name: "prefsPage")
  {
    section("When any of these people are home or away")
    {
      input "people", "capability.presenceSensor", title: "Which?", multiple: true, required: true
    }
    section("Run these phrases at sunrise and sunset")
    {
      input "sunrisePhrase", "enum", options: state.phrases, title: "At sunrise when home", required: true
      input "sunrisePhraseAway", "enum", options: state.phrases, title: "At sunrise when away", required: true
      input "sunsetPhrase", "enum", options: state.phrases, title: "At sunset when home", required: true
      input "sunsetPhraseAway", "enum", options: state.phrases, title: "At sunset when away", required: true
    }
    section("Run these phrases at a custom time (optional)")
    {
      input "customOneTime", "time", title: "At this time every day", required: false
      input "customOnePhrase", "enum", options: state.phrases, title: "When home", required: false
      input "customOnePhraseAway", "enum", options: state.phrases, title: "When away", required: false
    }
    section("Run these phrases at a custom time (optional)")
    {
      input "customTwoTime", "time", title: "At this time every day", required: false
      input "customTwoPhrase", "enum", options: state.phrases, title: "When home", required: false
      input "customTwoPhraseAway", "enum", options: state.phrases, title: "When away", required: false
    }
    section("Notifications")
    {
      input "sendPushMessage", "enum", title: "Send a push notification?", metadata: [values: ["Yes","No"]], required: false
      input "phoneToText", "phone", title: "Send a text message to this phone (optional)", required: false
    }
  }
}

def installed()
{
  initialize()
}

def updated()
{
  // When app is updated, clear out subscriptions and scheduled events and re-initialize
  unsubscribe()
  unschedule()
  initialize()
}

def initialize()
{
  log.debug "Initializing..."
  log.debug "Settings:"
  log.debug settings

  subscribe(people, "presence", onPresence)
  subscribe(location, "sunrise", onSunrise)
  subscribe(location, "sunset", onSunset)

  // Custom dayparts are optional, make sure we have settings for them
  if(settings.customOneTime && settings.customOnePhrase && settings.customOnePhraseAway)
  {
    log.debug "Scheduling Custom One..."
    schedule(customOneTime, onCustomOne)
  }

  if(settings.customTwoTime && settings.customTwoPhrase && settings.customTwoPhraseAway)
  {
    log.debug "Scheduling Custom Two..."
    schedule(customTwoTime, onCustomTwo)
  }

  // Get initial values for presence and daypart, then run the proper phrase
  initializePresence()
  initializeDaypart()

  // Don't notify during install process, it fails
  // TODO: Named argument here, how to do this with a default value also?
  updatePhrase(false)
}

private initializePresence()
{
  log.debug "Determining initial presence state..."
  if(anyoneIsHome())
  {
    log.debug "Initial presence: home"
    state.presence = "home"
  }
  else
  {
    log.debug "Initial presence: away"
    state.presence = "away"
  }
}

private initializeDaypart()
{
  log.debug "Determining initial daypart state..."

  def tz = location.timeZone
  def sun = getSunriseAndSunset()

  // Put the dayparts and now into a map
  def dayparts = [:]
  dayparts["now"] = new Date()
  dayparts["sunrise"] = sun.sunrise
  dayparts["sunset"] = sun.sunset

  // Custom dayparts are optional, make sure we have settings for them
  if(settings.customOneTime && settings.customOnePhrase && settings.customOnePhraseAway)
  {
    dayparts["customOne"] = timeToday(settings.customOneTime, tz)
  }
  if(settings.customTwoTime && settings.customTwoPhrase && settings.customTwoPhraseAway)
  {
    dayparts["customTwo"] = timeToday(settings.customTwoTime, tz)
  }

  // Sort the map in order of the dates
  dayparts = dayparts.sort { it.value }

  // Where is now in the sorted list of dayparts?
  def nowPosition = (dayparts.findIndexOf { it.key == "now" })

  def currentDaypart

  // If now is the first item in the list,
  // the last daypart (from the previous day) is still active/current
  if(nowPosition == 0)
  {
    currentDaypart = dayparts.keySet().last()
  }
  else
  {
    // Otherwise, the active/current daypart is the one that started previous to now
    currentDaypart = dayparts.keySet()[nowPosition - 1]
  }

  state.daypart = currentDaypart
}

def onPresence(evt)
{
  log.debug "Presence event..."
  log.debug evt.name + " | " + evt.value

  def newPresence

  if(evt.value == "not present")
  {
    if(everyoneIsAway())
    {
      newPresence = "away"
    }
    else
    {
      newPresence = "home"
    }
  }
  else if(evt.value == "present")
  {
    newPresence = "home"
  }

  // Only update if the presence has changed
  if(newPresence != state.presence)
  {
    log.debug "Presence changed from ${state.presence} to ${newPresence}"
    state.presence = newPresence
    updatePhrase()
  }
}

// Event handlers for daypart events
def onSunrise(evt){ onDaypartChange("sunrise") }
def onSunset(evt){ onDaypartChange("sunset") }
def onCustomOne(evt){ onDaypartChange("customOne") }
def onCustomTwo(evt){ onDaypartChange("customTwo") }

private onDaypartChange(daypart)
{
  if(daypart != state.daypart)
  {
    state.daypart = daypart
    log.debug "Daypart changed from ${state.daypart} to: ${daypart}"
    updatePhrase()
  }
}

private updatePhrase(notificationsEnabled=true)
{
  log.debug "Updating phrase..."
  def phrase = getPhrase()
  executePhrase(phrase)
  if(notificationsEnabled)
  {
    notify(phrase)
  }
}

private getPhrase()
{
  log.debug "Presence for phrase: ${state.presence}"
  log.debug "Daypart for phrase: ${state.daypart}"

  def phrase

  if(state.presence == "home")
  {
    phrase = settings["${state.daypart}Phrase"]
  }
  else
  {
    phrase = settings["${state.daypart}PhraseAway"]
  }

  return phrase
}

private executePhrase(phrase)
{
  log.debug ">>> Executing phrase: ${phrase}"
  location.helloHome.execute(phrase)
}

private notify(phrase)
{
  // Phrases with spaces are put into a list of some sort
  phrase = phrase.toString()

  def message = "Autophrases ran the Hello Home phrase '${phrase}' for you."
  log.debug message

  // Send push message and HH message
  if(settings.sendPushMessage == "Yes")
  {
    log.debug "Sending push message..."
    sendPush(message)
  }
  else
  {
    // Just sent HH message
    log.debug "Sending Hello Home message..."
    sendNotificationEvent(message)
  }

  // Send text message (don't re-send HH message)
  if(settings.phoneToText)
  {
    log.debug "Sending text message..."
    sendSmsMessage(settings.phoneToText, message)
  }
}

private everyoneIsAway()
{
  return people.every{ it.currentPresence == "not present" }
}

private anyoneIsHome()
{
  return people.any{ it.currentPresence == "present" }
}
