/**
 *  Copyright 2015 dburman 
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
 *  Author: dburman 
 *  Version: 2
 *  Date: 2015-11-09
 */
definition(
    name: "Speaker Notify with RapBoard",
    namespace: "smartthings",
    author: "dburman",
    description: "Play therapboard.com sounds or custom message through your speakers when the mode changes or other events occur.",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/sonos@2x.png"
    )

preferences {
  page(name: "mainPage", title: "Play a message on your Sonos when something happens", install: true, uninstall: true)
    page(name: "chooseTrack", title: "Select a song or station")
    page(name: "timeIntervalInput", title: "Only during a certain time") {
      section {
        input "starting", "time", title: "Starting", required: false
          input "ending", "time", title: "Ending", required: false
      }
    }
}

def mainPage() {
  dynamicPage(name: "mainPage") {
    def anythingSet = anythingSet()
      if (anythingSet) {
        section("Play message when"){
          ifSet "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
            ifSet "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
            ifSet "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
            ifSet "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
            ifSet "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
            ifSet "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
            ifSet "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
            ifSet "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
            ifSet "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
            ifSet "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
            ifSet "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
            ifSet "triggerModes", "mode", title: "System Changes Mode", required: false, multiple: true
            ifSet "timeOfDay", "time", title: "At a Scheduled Time", required: false
        }
      }
    def hideable = anythingSet || app.installationState == "COMPLETE"
      def sectionTitle = anythingSet ? "Select additional triggers" : "Play message when..."

      section(sectionTitle, hideable: hideable, hidden: true){
        ifUnset "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
          ifUnset "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
          ifUnset "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
          ifUnset "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
          ifUnset "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
          ifUnset "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
          ifUnset "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
          ifUnset "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
          ifUnset "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
          ifUnset "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
          ifUnset "button1", "capability.button", title: "Button Press", required:false, multiple:true //remove from production
          ifUnset "triggerModes", "mode", title: "System Changes Mode", description: "Select mode(s)", required: false, multiple: true
          ifUnset "timeOfDay", "time", title: "At a Scheduled Time", required: false
      }
    section{
      input "actionType", "enum", title: "Action?", required: true, defaultValue: "Custom Message", options: [
        "Custom Message",
        "Random",
        "2 Chainz - 2 Chains",
        "2 Chainz - Yeah",
        "2Pac - Baby",
        "2pac - Yeah N*gga",
        "2pac - Thug Life",
        "50 Cent - G G G G G G-unit",
        "50 Cent - Its Fifty",
        "50 Cent - (Laugh)",
        "50 Cent - Yeah", 
        "Action Bronson - Bronsolino",
        "Action Bronson - Yeah",
        "Action Bronson - Yo",
        "Akon - Convict Music",
        "Beanie Sigel - Mac",
        "Big Boi - Warsta",
        "Big L - Hey Yo",
        "Big L - Yeh Yeh",
        "Big L - Harlems Finest",
        "Big Sean - Boi",
        "Big Sean - Do It",
        "Big Sean - Hold Up",
        "Big Sean - Oh God",
        "Big Sean - Okay",
        "Big Sean - Stop",
        "Big Sean - Whoa",
        "Big Sean - Whoa There",
        "Birdman - Brrrrrrrrrrr",
        "Birdman - Cash Money",
        "Birdman - Believe Dat",
        "Birdman - Aruuu Aru Aru",
        "Birdman - One Hundred",
        "Bow Wow - Yeah",
        "Bun B - UGK 4 Life",
        "Busta Rhymes - Yaw Yaw ya Yaw",
        "Busta Rhymes - Whoooo Ha",
        "Busta Rhymes - Yeah",
        "Busta Rhymes - (Laugh)",
        "Camron - Killa",
        "Camron - Dip Say",
        "Chief Keef - Bang Meh",
        "Chief Keef - Catchup",
        "Chingy - Right There",
        "CurrenSy - Chill",
        "DaBrat - Comeon",
        "DaBrat - Lookout",
        "DaBrat - Oh",
        "Danny Brown - Stop",
        "Danny Brown - Yeah",
        "Danny Brown - (Laugh)",
        "David Banner - Yeah",
        "P Diddy - Talk To Em",
        "P Diddy - Ha!",
        "P Diddy - I like this right here", 
        "P Diddy - Come On", 
        "P Diddy - Uh Huh", 
        "P Diddy - Bad Boi", 
        "Dizzee Rascal - Hoy",
        "DJ Khaled - DJ Khaled",
        "DJ Khaled - This For The Hood",
        "DJ Paul - Hypnotize Mine",
        "DJ Paul - 3 6 Mafia",
        "DJ Paul - Goin Down",
        "DMX - (Bark)",
        "DMX - Come On", 
        "DMX - Yeah", 
        "DMX - Uh", 
        "Drake - Uh",
        "Drake - Young Money",
        "Drake - Yeah",
        "Drake - Alright",
        "Drumma Boy - Eh Yeah Boi",
        "E40 - Uhhh",
        "E40 - Ghost Ride the Whip",
        "Easy E - Easy Motha F*ckin E",
        "Eminem - My Name Is",
        "Eminem - Slim Shady",
        "Fat Joe - Calca",
        "Fat Joe - Owwwww",
        "Fat Joe - Crack",
        "Flavor Flav - Yeah Boi",
        "Freeway - You Know",
        "French Montana - Montana",
        "Ghostface Killah - Yo",
        "Grandmaster Flash - Huh Huh ha Hah",
        "Gucci Mane - Its Gucci",
        "Gucci Mane - Yoow",
        "Gucci Mane - Buh",
        "Gucci Mane - Damn",
        "Gucci Mane - Huh",
        "Hurricane Chris - Hey Baby",
        "Ice Cube - Yay Yaaay",
        "Inspectah Deck - Killa Hill",
        "Jadakiss - UhHui",
        "Ja Rule - Holla Holla",
        "Ja Rule - Its Murdah",
        "Jay-Z - Uh uh Uh",
        "Jay-Z - Hov",
        "Jay-Z - Geya",
        "Jay-Z - Im Focused Man",
        "Jay-Z - Young Hova, Yah Heard",
        "Jay-Z - Yo",
        "Jay-Z - Its da Roc",
        "Jay-Z - Its yo Boy",
        "Jay-Z - Jigga Man",
        "Jay-Z - Wheew",
        "Jay-Z - Yessir",
        "Jay-Z - Young",
        "Jazzy Phae - Ladies and Gentlemen",
        "Jermaine Dupri - Uh",
        "Jim Jones - Fallen",
        "Jim Jones - Dipset",
        "Juelz Santana - Ey",
        "Juicy J - Play me some pimpin Man",
        "Juicy J - Shut the f*ck uuuuhhhp",
        "Juicy J - Yessir",
        "Juicy J - Trippy Mane",
        "Juicy J - Motha F*cker",
        "Kanye West - Uh uhh",
        "Killer Mike - Killer",
        "Killer Mike - Prime Time Rap Game",
        "KRS One - Thats the sound of the Police",
        "Lil B - Swag",
        "Lil B - Whooop",
        "Lil Jon - Yeeaaaah",
        "Lil Jon - What",
        "Lil Jon - Whats Happenin",
        "Lil Jon - Truck Tay Ho",
        "Lil Scrappy - Oh K K Kay",
        "Lil Wayne - Hah",
        "Lil Wayne - Young Money",
        "Lil Wayne - Yessir",
        "Lil Wayne - Eee Vee",
        "Lil Wayne - Damn",
        "Lil Wayne - Yeah Right",
        "Lil Wayne - Hit Me",
        "Lil Wayne - What it it",
        "Lil Wayne - Young Mulla Faithey",
        "Lloyd Banks - Unh",
        "Lloyd Banks - Yeah",
        "Ludacris - Luda",
        "Ludacris - Wheew",
        "Mannie Fresh - Ladys & Gentlemen",
        "Mannie Fresh - Fre Fre Fresh",
        "MC Eiht - Geya",
        "MC Hammer - Cant touch This",
        "Method Man - M E T H O D Man",
        "Method Man - Tical",
        "Method Man - Yo",
        "Mike Jones - Mike Jones",
        "Mike Jones - Ice Age", 
        "Mike Jones - shyea", 
        "MOP - Brrrddddd", 
        "MOP - (laugh)",
        "Nas - Yeah Yeah",
        "Nas - Uh",
        "Nas - Nastradamas",
        "Nate Dogg - Hold up",
        "NORE - What",
        "Notorious BIG - Uh",
        "Notorious BIG - Baby Babay",
        "OJ Da Juiceman - Eyyhh",
        "ODB - Shimmy Shimmy",
        "ODB - Yoooo",
        "Pharrell - Yessir",
        "Pharrell - Eww Ew",
        "Pill - Ok Den",
        "Pimp C - Smoke sum B*tch",
        "Pimp C - Holdup",
        "Pimp C - Sweet Jones",
        "Pitbull - Its 305",
        "Pitbull - Ehhhh oow", 
        "Pitbull - Ehhhh oow", 
        "Pitbull - Hah", 
        "Pitbull - Dolla", 
        "Project Pat - Good Googly Moogly",
        "Pusha T - Ehhhk",
        "Pusha T - (laugh)",
        "Pusha T - Uhhh", 
        "Raekwon - Yo",
        "Redman - Heyo",
        "Rick Ross - Huuuh",
        "Rick Ross - Boss",
        "Rick Ross - Ricky Ross",
        "Rick Ross - Wheew",
        "Sean Price - Be",
        "Slim Thug - Slim Thugga",
        "Slim Thug - Boss Hog O Los",
        "Snoop Dog - Snoop D O Dub",
        "Snoop Dog - Blah Down",
        "Snoop Dog - Yum Ta Buh",
        "Soulja Boy - Soldier",
        "Soulja Boy - Soldier Boy Telldem",
        "Soulja Boy - You",
        "Swizz Beatz - Its yo Time",
        "Swizz Beatz - God Damnit",
        "T.I. - Ehhh",
        "T.I. - Haaaah",
        "T.I. - Hustle Partna",
        "T.I. - Ready for Dis",
        "T.I. - T I P",
        "Too $hort - Biatch",
        "T-Pain - Nappy",
        "T-Pain - Hey",
        "Trey Songz - Uhyeah",
        "Trick Daddy - uh Hah",
        "Trick Daddy - Uh Huh Ok",
        "Trick Daddy - Tricka Lota Kids",
        "Tyga - Uhh",
        "Vado - Huh",
        "Waka Floka Flame - Pop Pop Pop",
        "Waka Floka Flame - Floka",
        "Will Smith - Wheew",
        "Will Smith - Uun",
        "Will Smith - Uhh",
        "Wiz Khalifa - (laugh)",
        "Wiz Khalifa - Uhhn",
        "Young Jeezy - Yeah",
        "Young Jeezy - Ehhhh"
          ]
          input "message","text",title:"Play this message", required:false, multiple: false
    }
    section {
      input "sonos", "capability.musicPlayer", title: "On this Sonos player", required: true
    }
    section("More options", hideable: true, hidden: true) {
      input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
        href "chooseTrack", title: "Or play this music or radio station", description: song ? state.selectedSong?.station : "Tap to set", state: song ? "complete" : "incomplete"

        input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
        input "frequency", "decimal", title: "Minimum time between actions (defaults to every event)", description: "Minutes", required: false
        href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : "incomplete"
        input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
              options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
                if (settings.modes) {
                  input "modes", "mode", title: "Only when mode is", multiple: true, required: false
                }
      input "oncePerDay", "bool", title: "Only once per day", required: false, defaultValue: false
    }
    section([mobileOnly:true]) {
      label title: "Assign a name", required: false
        mode title: "Set for specific mode(s)", required: false
    }
  }
}

def chooseTrack() {
  dynamicPage(name: "chooseTrack") {
    section{
      input "song","enum",title:"Play this track", required:true, multiple: false, options: songOptions()
    }
  }
}

private songOptions() {

  // Make sure current selection is in the set

  def options = new LinkedHashSet()
    if (state.selectedSong?.station) {
      options << state.selectedSong.station
    }
    else if (state.selectedSong?.description) {
      // TODO - Remove eventually? 'description' for backward compatibility
      options << state.selectedSong.description
    }

  // Query for recent tracks
  def states = sonos.statesSince("trackData", new Date(0), [max:30])
    def dataMaps = states.collect{it.jsonValue}
  options.addAll(dataMaps.collect{it.station})

    log.trace "${options.size()} songs in list"
    options.take(20) as List
}

private saveSelectedSong() {
  try {
    def thisSong = song
      log.info "Looking for $thisSong"
      def songs = sonos.statesSince("trackData", new Date(0), [max:30]).collect{it.jsonValue}
    log.info "Searching ${songs.size()} records"

      def data = songs.find {s -> s.station == thisSong}
    log.info "Found ${data?.station}"
      if (data) {
        state.selectedSong = data
          log.debug "Selected song = $state.selectedSong"
      }
      else if (song == state.selectedSong?.station) {
        log.debug "Selected existing entry '$song', which is no longer in the last 20 list"
      }
      else {
        log.warn "Selected song '$song' not found"
      }
  }
  catch (Throwable t) {
    log.error t
  }
}

private anythingSet() {
  for (name in ["motion","contact","contactClosed","acceleration","mySwitch","mySwitchOff","arrivalPresence","departurePresence","smoke","water","button1","timeOfDay","triggerModes","timeOfDay"]) {
    if (settings[name]) {
      return true
    }
  }
  return false
}

private ifUnset(Map options, String name, String capability) {
  if (!settings[name]) {
    input(options, name, capability)
  }
}

private ifSet(Map options, String name, String capability) {
  if (settings[name]) {
    input(options, name, capability)
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
    subscribeToEvents()
}

def updated() {
  log.debug "Updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    subscribeToEvents()
}

def subscribeToEvents() {
  subscribe(app, appTouchHandler)
    subscribe(contact, "contact.open", eventHandler)
    subscribe(contactClosed, "contact.closed", eventHandler)
    subscribe(acceleration, "acceleration.active", eventHandler)
    subscribe(motion, "motion.active", eventHandler)
    subscribe(mySwitch, "switch.on", eventHandler)
    subscribe(mySwitchOff, "switch.off", eventHandler)
    subscribe(arrivalPresence, "presence.present", eventHandler)
    subscribe(departurePresence, "presence.not present", eventHandler)
    subscribe(smoke, "smoke.detected", eventHandler)
    subscribe(smoke, "smoke.tested", eventHandler)
    subscribe(smoke, "carbonMonoxide.detected", eventHandler)
    subscribe(water, "water.wet", eventHandler)
    subscribe(button1, "button.pushed", eventHandler)

    if (triggerModes) {
      subscribe(location, modeChangeHandler)
    }

  if (timeOfDay) {
    schedule(timeOfDay, scheduledTimeHandler)
  }

  if (song) {
    saveSelectedSong()
  }
}

def eventHandler(evt) {
  log.trace "eventHandler($evt?.name: $evt?.value)"
    if (allOk) {
      log.trace "allOk"
        loadText()
        def lastTime = state[frequencyKey(evt)]
        if (oncePerDayOk(lastTime)) {
          if (frequency) {
            if (lastTime == null || (now() - lastTime) >= (frequency * 60000)) {
              takeAction(evt)
            }
            else {
              log.debug "Not taking action because $frequency minutes have not elapsed since last action"
            }
          }
          else {
            takeAction(evt)
          }
        }
        else {
          log.debug "Not taking action because it was already taken today"
        }
    }
}
def modeChangeHandler(evt) {
  log.trace "modeChangeHandler $evt.name: $evt.value ($triggerModes)"
    if (evt.value in triggerModes) {
      eventHandler(evt)
    }
}

def scheduledTimeHandler() {
  eventHandler(null)
}

def appTouchHandler(evt) {
  takeAction(evt)
}

private takeAction(evt) {

  log.trace "takeAction()"

    if (song) {
      sonos.playSoundAndTrack(state.sound.uri, state.sound.duration, state.selectedSong, volume)
    }
    else if (resumePlaying){
      sonos.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
    }
    else {
      sonos.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
    }

  if (frequency || oncePerDay) {
    state[frequencyKey(evt)] = now()
  }
  log.trace "Exiting takeAction()"
}

private frequencyKey(evt) {
  "lastActionTimeStamp"
}

private dayString(Date date) {
  def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
    if (location.timeZone) {
      df.setTimeZone(location.timeZone)
    }
    else {
      df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
    }
  df.format(date)
}

private oncePerDayOk(Long lastTime) {
  def result = true
    if (oncePerDay) {
      result = lastTime ? dayString(new Date()) != dayString(new Date(lastTime)) : true
        log.trace "oncePerDayOk = $result"
    }
  result
}

// TODO - centralize somehow
private getAllOk() {
  modeOk && daysOk && timeOk
}

private getModeOk() {
  def result = !modes || modes.contains(location.mode)
    log.trace "modeOk = $result"
    result
}

private getDaysOk() {
  def result = true
    if (days) {
      def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
          df.setTimeZone(location.timeZone)
        }
        else {
          df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
      def day = df.format(new Date())
        result = days.contains(day)
    }
  log.trace "daysOk = $result"
    result
}

private getTimeOk() {
  def result = true
    if (starting && ending) {
      def currTime = now()
        def start = timeToday(starting, location?.timeZone).time
        def stop = timeToday(ending, location?.timeZone).time
        result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    }
  log.trace "timeOk = $result"
    result
}

private hhmm(time, fmt = "h:mm a")
{
  def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private getTimeLabel()
{
  (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}
// TODO - End Centralize

private loadText() {
  def trackuri = [
    "2chainz_4.mp3",
    "2chainz_yeah2.mp3",
    "2pac_4.mp3",
    "2pac_5.mp3",
    "2pac_6.mp3",
    "50_5.mp3",
    "50_8.mp3",
    "50_11.mp3",
    "50cent_2.mp3",
    "action_bronsolino.mp3",
    "action_yeah.mp3",
    "action_yo.mp3",
    "akon_1.mp3",
    "beanie_mac.mp3",
    "bigboi_1.mp3",
    "bigl_3.mp3",
    "bigl_4.mp3",
    "bigl_5.mp3",
    "bigsean_boi2.mp3",
    "bigsean_doit.mp3",
    "bigsean_holdup2.mp3",
    "bigsean_ohgod.mp3",
    "bigsean_okay.mp3",
    "bigsean_stop.mp3",
    "bigsean_whoa.mp3",
    "bigsean_whoathere.mp3",
    "birdman_4.mp3",
    "birdman_10.mp3",
    "birdman_16.mp3",
    "birdman_1.mp3",
    "birdman_6.mp3",
    "bowwow_yeah.mp3",
    "bunb_ugk4life.mp3",
    "busta_6.mp3",
    "busta_1.mp3",
    "busta_2.mp3",
    "busta_5.mp3",
    "camron_1.mp3",
    "camron_2.mp3",
    "keef_bang.mp3",
    "keef_catchup.mp3",
    "chingy_1.mp3",
    "currensy_1.mp3",
    "dabrat_comeon.mp3",
    "dabrat_lookout.mp3",
    "dabrat_oh.mp3",
    "danny_stop.mp3",
    "danny_yeah.mp3",
    "dannybrown_laugh2.mp3",
    "davidbanner_5.mp3",
    "diddy_1.mp3",
    "diddy_3.mp3",
    "diddy_4.mp3",
    "diddy_5.mp3",
    "diddy_6.mp3",
    "diddy_7.mp3",
    "dizzee_1.mp3",
    "djkhaled_2.mp3",
    "djkhaled_3.mp3",
    "djpaul_2.mp3",
    "djpaul_3.mp3",
    "djpaul_9.mp3",
    "dmx_1.mp3",
    "dmx_3.mp3",
    "dmx_6.mp3",
    "dmx_7.mp3",
    "drake_2.mp3",
    "drake_3.mp3",
    "drake_4.mp3",
    "drake_5.mp3",
    "drummaboy_1.mp3",
    "e40_1.mp3",
    "e40_2.mp3",
    "eazye_1.mp3",
    "eminem_3.mp3",
    "eminem_4.mp3",
    "fatjoe_1.mp3",
    "fatjoe_9.mp3",
    "fatjoe_5.mp3",
    "flava_1.mp3",
    "freeway_1.mp3",
    "french_1.mp3",
    "ghostface_yo.mp3",
    "grandmaster_1.mp3",
    "gucci_1.mp3",
    "gucci_4.mp3",
    "gucci_14.mp3",
    "gucci_8.mp3",
    "gucci_9.mp3",
    "hurricanechris_1.mp3",
    "icecube_1.mp3",
    "inspectahdeck_killahill.mp3",
    "jadakiss_3.mp3",
    "jarule_1.mp3",
    "jarule_2.mp3",
    "jayz_7.mp3",
    "jayz_9.mp3",
    "jayz_1.mp3",
    "jayz5.mp3",
    "jayz7.mp3",
    "jayz8.mp3",
    "jayz_itsthero.mp3",
    "jayz_itsyoboy.mp3",
    "jayz_jiggaman.mp3",
    "jayz_woo.mp3",
    "jayz_yessir.mp3",
    "jayz_young.mp3",
    "jazzypha_1.mp3",
    "jermaine_unh.mp3",
    "jones_8.mp3",
    "jones_14.mp3",
    "juelz_2.mp3",
    "juicyj_1.mp3",
    "juicyj_8.mp3",
    "juicyj_9.mp3",
    "juicyj_10.mp3",
    "juicyj_7.mp3",
    "kanye_1.mp3",
    "killermike_2.mp3",
    "killermike_3.mp3",
    "krsone_1.mp3",
    "lilb_1.mp3",
    "lilb_2.mp3",
    "liljon_2.mp3",
    "liljon_3.mp3",
    "liljon_8.mp3",
    "liljon_4.mp3",
    "lilscrappy_1.mp3",
    "weezy_14.mp3",
    "weezy_22.mp3",
    "weezy_29.mp3",
    "weezy_4.mp3",
    "weezy_16.mp3",
    "weezy_17.mp3",
    "weezy_25.mp3",
    "weezy_30.mp3",
    "weezy_31.mp3",
    "banks_unh.mp3",
    "banks_yeah.mp3",
    "ludacris_2.mp3",
    "ludacris_woo.mp3",
    "mannie_1.mp3",
    "mannie_2.mp3",
    "mceiht_1.mp3",
    "mchammer_1.mp3",
    "methodman_1.mp3",
    "methodman_tical.mp3",
    "methodman_yo.mp3",
    "mikejones_2.mp3",
    "mikejones_iceage.mp3",
    "mikejones_jyeah.mp3",
    "mop_1.mp3",
    "mop_2.mp3",
    "nas_1.mp3",
    "nas_3.mp3",
    "nas_6.mp3",
    "natedogg_1.mp3",
    "nore_1.mp3",
    "biggie_1.mp3",
    "biggie_2.mp3",
    "oj_4.mp3",
    "odb_shimmy.mp3",
    "odb_yo.mp3",
    "pharrell_1.mp3",
    "pharrell_2.mp3",
    "pill_1.mp3",
    "pimpc_4.mp3",
    "pimpc_1.mp3",
    "pimpc_sweetjones3.mp3",
    "pitbull_1.mp3",
    "pitbull_2.mp3",
    "pitbull_3.mp3",
    "pitbull_6.mp3",
    "projectpat_1.mp3",
    "pushat_1.mp3",
    "pushat_haha.mp3",
    "pushat_unh.mp3",
    "raekwon_yo.mp3",
    "redman_heyo.mp3",
    "ross_1.mp3",
    "ross_2.mp3",
    "ross_4.mp3",
    "ross_woo.mp3",
    "seanprice_1.mp3",
    "thugga_2.mp3",
    "thugga_3.mp3",
    "snoop_5.mp3",
    "snoop_4.mp3",
    "snoop_1.mp3",
    "soulja_2.mp3",
    "soulja_4.mp3",
    "soulja_5.mp3",
    "swizz_1.mp3",
    "swizz_goddamit.mp3",
    "ti_5.mp3",
    "ti_2.mp3",
    "ti_3.mp3",
    "ti_22.mp3",
    "ti_32.mp3",
    "tooshort_1.mp3",
    "tpain_2.mp3",
    "tpain1.mp3",
    "treysongz_4.mp3",
    "trick_1.mp3",
    "trick_2.mp3",
    "trick_4.mp3",
    "tyga_unh.mp3",
    "vado_1.mp3",
    "waka_1.mp3",
    "waka_8.mp3",
    "willsmith_1.mp3",
    "willsmith_2.mp3",
    "willsmith_3.mp3",
    "wiz_1.mp3",
    "wiz_unh.mp3",
    "jeezy_10.mp3",
    "jeezy_11.mp3",
    "jeezy_1.mp3"
      ]

      def trackname = [ 
      "2 Chainz - 2 Chains",
    "2 Chainz - Yeah",
    "2Pac - Baby",
    "2pac - Yeah N*gga",
    "2pac - Thug Life",
    "50 Cent - G G G G G G-unit",
    "50 Cent - Its Fifty",
    "50 Cent - (Laugh)",
    "50 Cent - Yeah", 
    "Action Bronson - Bronsolino",
    "Action Bronson - Yeah",
    "Action Bronson - Yo",
    "Akon - Convict Music",
    "Beanie Sigel - Mac",
    "Big Boi - Warsta",
    "Big L - Hey Yo",
    "Big L - Yeh Yeh",
    "Big L - Harlems Finest",
    "Big Sean - Boi",
    "Big Sean - Do It",
    "Big Sean - Hold Up",
    "Big Sean - Oh God",
    "Big Sean - Okay",
    "Big Sean - Stop",
    "Big Sean - Whoa",
    "Big Sean - Whoa There",
    "Birdman - Brrrrrrrrrrr",
    "Birdman - Cash Money",
    "Birdman - Believe Dat",
    "Birdman - Aruuu Aru Aru",
    "Birdman - One Hundred",
    "Bow Wow - Yeah",
    "Bun B - UGK 4 Life",
    "Busta Rhymes - Yaw Yaw ya Yaw",
    "Busta Rhymes - Whoooo Ha",
    "Busta Rhymes - Yeah",
    "Busta Rhymes - (Laugh)",
    "Camron - Killa",
    "Camron - Dip Say",
    "Chief Keef - Bang Meh",
    "Chief Keef - Catchup",
    "Chingy - Right There",
    "CurrenSy - Chill",
    "DaBrat - Comeon",
    "DaBrat - Lookout",
    "DaBrat - Oh",
    "Danny Brown - Stop",
    "Danny Brown - Yeah",
    "Danny Brown - (Laugh)",
    "David Banner - Yeah",
    "P Diddy - Talk To Em",
    "P Diddy - Ha!",
    "P Diddy - I like this right here", 
    "P Diddy - Come On", 
    "P Diddy - Uh Huh", 
    "P Diddy - Bad Boi", 
    "Dizzee Rascal - Hoy",
    "DJ Khaled - DJ Khaled",
    "DJ Khaled - This For The Hood",
    "DJ Paul - Hypnotize Mine",
    "DJ Paul - 3 6 Mafia",
    "DJ Paul - Goin Down",
    "DMX - (Bark)",
    "DMX - Come On", 
    "DMX - Yeah", 
    "DMX - Uh", 
    "Drake - Uh",
    "Drake - Young Money",
    "Drake - Yeah",
    "Drake - Alright",
    "Drumma Boy - Eh Yeah Boi",
    "E40 - Uhhh",
    "E40 - Ghost Ride the Whip",
    "Easy E - Easy Motha F*ckin E",
    "Eminem - My Name Is",
    "Eminem - Slim Shady",
    "Fat Joe - Calca",
    "Fat Joe - Owwwww",
    "Fat Joe - Crack",
    "Flavor Flav - Yeah Boi",
    "Freeway - You Know",
    "French Montana - Montana",
    "Ghostface Killah - Yo",
    "Grandmaster Flash - Huh Huh ha Hah",
    "Gucci Mane - Its Gucci",
    "Gucci Mane - Yoow",
    "Gucci Mane - Buh",
    "Gucci Mane - Damn",
    "Gucci Mane - Huh",
    "Hurricane Chris - Hey Baby",
    "Ice Cube - Yay Yaaay",
    "Inspectah Deck - Killa Hill",
    "Jadakiss - UhHui",
    "Ja Rule - Holla Holla",
    "Ja Rule - Its Murdah",
    "Jay-Z - Uh uh Uh",
    "Jay-Z - Hov",
    "Jay-Z - Geya",
    "Jay-Z - Im Focused Man",
    "Jay-Z - Young Hova, Yah Heard",
    "Jay-Z - Yo",
    "Jay-Z - Its da Roc",
    "Jay-Z - Its yo Boy",
    "Jay-Z - Jigga Man",
    "Jay-Z - Wheew",
    "Jay-Z - Yessir",
    "Jay-Z - Young",
    "Jazzy Phae - Ladies and Gentlemen",
    "Jermaine Dupri - Uh",
    "Jim Jones - Fallen",
    "Jim Jones - Dipset",
    "Juelz Santana - Ey",
    "Juicy J - Play me some pimpin Man",
    "Juicy J - Shut the f*ck uuuuhhhp",
    "Juicy J - Yessir",
    "Juicy J - Trippy Mane",
    "Juicy J - Motha F*cker",
    "Kanye West - Uh uhh",
    "Killer Mike - Killer",
    "Killer Mike - Prime Time Rap Game",
    "KRS One - Thats the sound of the Police",
    "Lil B - Swag",
    "Lil B - Whooop",
    "Lil Jon - Yeeaaaah",
    "Lil Jon - What",
    "Lil Jon - Whats Happenin",
    "Lil Jon - Truck Tay Ho",
    "Lil Scrappy - Oh K K Kay",
    "Lil Wayne - Hah",
    "Lil Wayne - Young Money",
    "Lil Wayne - Yessir",
    "Lil Wayne - Eee Vee",
    "Lil Wayne - Damn",
    "Lil Wayne - Yeah Right",
    "Lil Wayne - Hit Me",
    "Lil Wayne - What it it",
    "Lil Wayne - Young Mulla Faithey",
    "Lloyd Banks - Unh",
    "Lloyd Banks - Yeah",
    "Ludacris - Luda",
    "Ludacris - Wheew",
    "Mannie Fresh - Ladys & Gentlemen",
    "Mannie Fresh - Fre Fre Fresh",
    "MC Eiht - Geya",
    "MC Hammer - Cant touch This",
    "Method Man - M E T H O D Man",
    "Method Man - Tical",
    "Method Man - Yo",
    "Mike Jones - Mike Jones",
    "Mike Jones - Ice Age", 
    "Mike Jones - shyea", 
    "MOP - Brrrddddd", 
    "MOP - (laugh)",
    "Nas - Yeah Yeah",
    "Nas - Uh",
    "Nas - Nastradamas",
    "Nate Dogg - Hold up",
    "NORE - What",
    "Notorious BIG - Uh",
    "Notorious BIG - Baby Babay",
    "OJ Da Juiceman - Eyyhh",
    "ODB - Shimmy Shimmy",
    "ODB - Yoooo",
    "Pharrell - Yessir",
    "Pharrell - Eww Ew",
    "Pill - Ok Den",
    "Pimp C - Smoke sum B*tch",
    "Pimp C - Holdup",
    "Pimp C - Sweet Jones",
    "Pitbull - Its 305",
    "Pitbull - Ehhhh oow", 
    "Pitbull - Ehhhh oow", 
    "Pitbull - Hah", 
    "Pitbull - Dolla", 
    "Project Pat - Good Googly Moogly",
    "Pusha T - Ehhhk",
    "Pusha T - (laugh)",
    "Pusha T - Uhhh", 
    "Raekwon - Yo",
    "Redman - Heyo",
    "Rick Ross - Huuuh",
    "Rick Ross - Boss",
    "Rick Ross - Ricky Ross",
    "Rick Ross - Wheew",
    "Sean Price - Be",
    "Slim Thug - Slim Thugga",
    "Slim Thug - Boss Hog O Los",
    "Snoop Dog - Snoop D O Dub",
    "Snoop Dog - Blah Down",
    "Snoop Dog - Yum Ta Buh",
    "Soulja Boy - Soldier",
    "Soulja Boy - Soldier Boy Telldem",
    "Soulja Boy - You",
    "Swizz Beatz - Its yo Time",
    "Swizz Beatz - God Damnit",
    "T.I. - Ehhh",
    "T.I. - Haaaah",
    "T.I. - Hustle Partna",
    "T.I. - Ready for Dis",
    "T.I. - T I P",
    "Too $hort - Biatch",
    "T-Pain - Nappy",
    "T-Pain - Hey",
    "Trey Songz - Uhyeah",
    "Trick Daddy - uh Hah",
    "Trick Daddy - Uh Huh Ok",
    "Trick Daddy - Tricka Lota Kids",
    "Tyga - Uhh",
    "Vado - Huh",
    "Waka Floka Flame - Pop Pop Pop",
    "Waka Floka Flame - Floka",
    "Will Smith - Wheew",
    "Will Smith - Uun",
    "Will Smith - Uhh",
    "Wiz Khalifa - (laugh)",
    "Wiz Khalifa - Uhhn",
    "Young Jeezy - Yeah",
    "Young Jeezy - Ehhhh"
      ]

      def int index
      if (actionType.equals("Custom Message"))
      {
        if (message) {
          state.sound = textToSpeech(message instanceof List ? message[0] : message) // not sure why this is (sometimes) needed)
        }
        else {
          state.sound = textToSpeech("custom message with no message in the $app.label smart app. please")
        }
      }
      else
      {
        if (actionType.equals("Random")) {
          index = Math.abs(new Random().nextInt() % (trackuri.size()))
        }
        else {
          index = trackname.indexOf(actionType);
        }
        if ((index >= 0) && (index < trackuri.size())) {
          state.sound = [uri:"http://therapboard.com/audio/" + trackuri[index], duration:"4"]
        }
        else {
          state.sound = textToSpeech("You done messed up setting up $app.label smart app")
        }
      }
}
