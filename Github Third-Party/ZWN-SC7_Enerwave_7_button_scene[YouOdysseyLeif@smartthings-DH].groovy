/**
 *	Button Controller App for ZWN-SC7
 *
 *	Author: Matt Frank based on VRCS Button Controller by Brian Dahlem, based on SmartThings Button Controller
 *	Date Created: 2014-12-18
 *  	Last Updated: 2015-11-14
 *      Modified by Scott Barton to add Dimmer Level, Light Color, FanSpeed, DimmerUp, DimmerDown, and Shade control 2016-10-11
 *      SB Mods based on Button Controller+ and Garden Hue code  
 *
 * 	Contributions from erocm1231 @ SmartThings Community
 *
 */
definition(
    name: "ZWN-SC7	 Button Controller",
    namespace: "mattjfrank",
    author: "Matt Frank, using code from Brian Dahlem",
    description: "ZWN-SC7	 7-Button	 Scene	 Controller	Button Assignment App",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/MyApps/Cat-MyApps@2x.png"
)

preferences {
  page(name: "selectButton")
  page(name: "configureButton1")
  page(name: "configureButton2")
  page(name: "configureButton3")
  page(name: "configureButton4")
  page(name: "configureButton5")
  page(name: "configureButton6")
  page(name: "configureButton7")
}

def selectButton() {
  dynamicPage(name: "selectButton", title: "First, select which ZWN-SC7", nextPage: "configureButton1", uninstall: configured()) {
    section {
      input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
    }
    section(title: "Advanced", hideable: true, hidden: true) {
      input "debounce", "number", title: "Debounce time in milliseconds", required: true, value: 3000
    }


  }
}

def configureButton1() {
  dynamicPage(name: "configureButton1", title: "1st button, what do you want it to do?",
    nextPage: "configureButton2", uninstall: configured(), getButtonSections(1))

}

def configureButton2() {
  dynamicPage(name: "configureButton2", title: "2nd button, what do you want it to do?",
    nextPage: "configureButton3", uninstall: configured(), getButtonSections(2))
}

def configureButton3() {
  dynamicPage(name: "configureButton3", title: "3rd button, what do you want it to do?",
    nextPage: "configureButton4", uninstall: configured(), getButtonSections(3))
}
def configureButton4() {
  dynamicPage(name: "configureButton4", title: "4th button, what do you want it to do?",
    nextPage: "configureButton5", uninstall: configured(), getButtonSections(4))
}
def configureButton5() {
  dynamicPage(name: "configureButton5", title: "5th button, what do you want it to do?",
    nextPage: "configureButton6", uninstall: configured(), getButtonSections(5))
}
def configureButton6() {
  dynamicPage(name: "configureButton6", title: "6th button, what do you want it to do?",
    nextPage: "configureButton7", uninstall: configured(), getButtonSections(6))
}
def configureButton7() {
  dynamicPage(name: "configureButton7", title: "7th  button, what do you want it to do?",
    install: true, uninstall: true, getButtonSections(7))
}

def getButtonSections(buttonNumber) {
  return {
    section(title: "Toggle these...", hidden: hideSection(buttonNumber, "toggle"), hideable: true) {
      input "lights_${buttonNumber}_toggle", "capability.switch", title: "Switches:", multiple: true, required: false
      input "locks_${buttonNumber}_toggle", "capability.lock", title: "Locks:", multiple: true, required: false
      input "sonos_${buttonNumber}_toggle", "capability.musicPlayer", title: "Music Players:", multiple: true, required: false
    }
    section(title: "Turn on these...", hidden: hideSection(buttonNumber, "on"), hideable: true) {
      input "lights_${buttonNumber}_on", "capability.switch", title: "Switches:", multiple: true, required: false
      input "sonos_${buttonNumber}_on", "capability.musicPlayer", title: "Music Players:", multiple: true, required: false
    }
    section(title: "Turn off these...", hidden: hideSection(buttonNumber, "off"), hideable: true) {
      input "lights_${buttonNumber}_off", "capability.switch", title: "Switches:", multiple: true, required: false
      input "sonos_${buttonNumber}_off", "capability.musicPlayer", title: "Music Players:", multiple: true, required: false
    }
    
    //SB
    section("Toggle Lights to Level", hidden: hideSection(buttonNumber, "setLevel"), hideable: true) {
			input "dimmers_${buttonNumber}_setLevel", "capability.switchLevel", title: "Lights", multiple: true, required: false
			input "dimmerLevel_${buttonNumber}_level", "number", title: "Dim Level", required: false, description: "0 to 99"
	}
    section("Set Light to Color", hidden: hideSection(buttonNumber, "setColor"), hideable: true) {
			input "lights_${buttonNumber}_setColor", "capability.colorControl", title: "Lights", multiple: true, required: false
            input "lightColor_${buttonNumber}_color", "enum", title: "Color" , options: ["Warm White","Soft White","Daylight","White","Red","Orange","Amber","Yellow","Green","Turquoise","Aqua","Navy Blue","Blue","Indigo","Purple","Pink"], required: false
			input "lightLevel_${buttonNumber}_level", "number", title: "Level", required: false, description: "0 to 99"
	}
    section(title: "Adjust Fan Speed - Low, Medium, High, Off", hidden: hideSection(buttonNumber, "fanSpeed"), hideable: true) {
      input "fans_${buttonNumber}_fanSpeed", "capability.switchLevel", title: "Fans:", multiple: true, required: false
    }
    section(title: "Adjust Dimmer Up 10%", hidden: hideSection(buttonNumber, "dimmerUp"), hideable: true) {
      input "dimmers_${buttonNumber}_dimmerUp", "capability.switchLevel", title: "Dimmers:", multiple: true, required: false
    }
    section(title: "Adjust Dimmer Down 10%", hidden: hideSection(buttonNumber, "dimmerDown"), hideable: true) {
      input "dimmers_${buttonNumber}_dimmerDown", "capability.switchLevel", title: "Dimmers:", multiple: true, required: false
    }
   section("Adjust Shade - Up, Down, or Stop", hidden: hideSection(buttonNumber, "adjustShade"), hideable: true) {
	   input "shades_${buttonNumber}_adjustShade", "capability.doorControl", title: "Shades:", multiple: false, required: false
   }
   //END SB
   
    section(title: "Locks:", hidden: hideLocksSection(buttonNumber), hideable: true) {
      input "locks_${buttonNumber}_unlock", "capability.lock", title: "Unlock these locks:", multiple: true, required: false
      input "locks_${buttonNumber}_lock", "capability.lock", title: "Lock these locks:", multiple: true, required: false
    }
    section("Modes") {
      input "mode_${buttonNumber}_on", "mode", title: "Activate these modes:", required: false
    }
    def phrases = location.helloHome?.getPhrases()*.label
    if (phrases) {
      section("Hello Home Actions") {
        log.trace phrases
        input "phrase_${buttonNumber}_on", "enum", title: "Activate these phrases:", required: false, options: phrases
      }
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

  subscribe(buttonDevice, "button", buttonEvent)

    if (relayDevice) {
        log.debug "Associating ${relayDevice.deviceNetworkId}"
        if (relayAssociate == true) {
            buttonDevice.associateLoad(relayDevice.deviceNetworkId)
        }
        else {
            buttonDevice.associateLoad(0)
        }
    }
}

def configured() {
  return  buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4) || buttonConfigured(5) || buttonConfigured(6) || buttonConfigured(7)
}

def buttonConfigured(idx) {
  return settings["lights_$idx_toggle"] ||
    settings["locks_$idx_toggle"] ||
    settings["sonos_$idx_toggle"] ||
    settings["mode_$idx_on"] ||
    settings["lights_$idx_on"] ||
    settings["locks_$idx_on"] ||
    settings["sonos_$idx_on"] |
    settings["lights_$idx_off"] ||
    //SB
    settings["dimmers_$idx_setLevel"] ||
    settings["dimmerLevel_$idx_level"] ||
    settings["lights_$idx_setColor"] ||
    settings["lightColor_$idx_color"] ||
    settings["lightLevel_$idx_level"] ||
    settings["fans_$idx_fanSpeed"] ||
    settings["dimmers_$idx_dimmerUp"] ||
    settings["dimmers_$idx_dimmerDown"] ||
    settings["shades_$idx_adjustShade"] ||
    //END SB
    settings["locks_$idx_off"] ||
    settings["sonos_$idx_off"]
}

def buttonEvent(evt){
  log.debug "buttonEvent"
  if(allOk) {
      def buttonNumber = evt.jsonData.buttonNumber
      def firstEventId = 0
	  def value = evt.value
	  //log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
	  log.debug "button: $buttonNumber, value: $value"
	  def recentEvents = buttonDevice.eventsSince(new Date(now() - debounce)).findAll{it.value == evt.value && it.data == evt.data}
	  log.debug "Found ${recentEvents.size()?:0} events in past ${debounce/1000} seconds"
      if (recentEvents.size() != 0){
          log.debug "First Event ID: ${recentEvents[0].id}"
          firstEventId = recentEvents[0].id
      }
      else {
          firstEventId = 0
      }
        
      log.debug "This Event ID: ${evt.id}"

      if(firstEventId == evt.id){
      switch(buttonNumber) {
        case ~/.*1.*/:
          executeHandlers(1)
          break
        case ~/.*2.*/:
          executeHandlers(2)
          break
        case ~/.*3.*/:
          executeHandlers(3)
          break
        case ~/.*4.*/:
          executeHandlers(4)
          break
        case ~/.*5.*/:
          executeHandlers(5)
          break
        case ~/.*6.*/:
          executeHandlers(6)
          break
        case ~/.*7.*/:
          executeHandlers(7)
          break
      }
    } else if (firstEventId == 0) {
      log.debug "No events found. Possible SmartThings latency"
    } else {
      log.debug "Duplicate button press found. Not executing handlers"
    }
    
  }
    else {
      log.debug "NotOK"
    }
}

def executeHandlers(buttonNumber) {
  log.debug "executeHandlers: $buttonNumber"

  def lights = find('lights', buttonNumber, "toggle")
  if (lights != null) toggle(lights)

  def locks = find('locks', buttonNumber, "toggle")
  if (locks != null) toggle(locks)

  def sonos = find('sonos', buttonNumber, "toggle")
  if (sonos != null) toggle(sonos)

  lights = find('lights', buttonNumber, "on")
  if (lights != null) flip(lights, "on")

  locks = find('locks', buttonNumber, "unlock")
  if (locks != null) flip(locks, "unlock")

  sonos = find('sonos', buttonNumber, "on")
  if (sonos != null) flip(sonos, "on")

  lights = find('lights', buttonNumber, "off")
  if (lights != null) flip(lights, "off")
  
//SB
  def level = find('dimmerLevel', buttonNumber, "level")
  if (level == null) level = 99
  def dimmers = find('dimmers', buttonNumber, "setLevel")
  if (dimmers != null) setLightLevel(dimmers, level)
  
  def color = find('lightColor', buttonNumber, "color")
  if (color == null) color = "White"
  level = find('lightLevel', buttonNumber, "level")
  if (level == null) level = 99
  lights = find('lights', buttonNumber, "setColor")
  if (lights != null) setMyColor(lights, color, level)
  
  def fans = find('fans', buttonNumber, "fanSpeed")
  if (fans != null) adjustFan(fans)

  dimmers = find('dimmers', buttonNumber, "dimmerUp")
  if (dimmers != null) dimmerUp(dimmers)

  dimmers = find('dimmers', buttonNumber, "dimmerDown")
  if (dimmers != null) dimmerDown(dimmers)

  def shades = find('shades', buttonNumber, "adjustShade")
  if (shades) adjustShade(shades)
//END SB

  locks = find('locks', buttonNumber, "lock")
  if (locks != null) flip(locks, "lock")

  sonos = find('sonos', buttonNumber, "off")
  if (sonos != null) flip(sonos, "off")

  def mode = find('mode', buttonNumber, "on")
  if (mode != null) changeMode(mode)

  def phrase = find('phrase', buttonNumber, "on")
  if (phrase != null) location.helloHome.execute(phrase)
}

def find(type, buttonNumber, value) {
  def preferenceName = type + "_" + buttonNumber + "_" + value
  def pref = settings[preferenceName]
  if(pref != null) {
    log.debug "Found: $pref for $preferenceName"
  }

  return pref
}

def flip(devices, newState) {
  log.debug "flip: $devices = ${devices*.currentValue('switch')}"


  if (newState == "off") {
    devices.off()
  }
  else if (newState == "on") {
    devices.on()
  }
  else if (newState == "unlock") {
    devices.unlock()
  }
  else if (newState == "lock") {
    devices.lock()
  }
}

def toggle(devices) {
  log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

  if (devices*.currentValue('switch').contains('on')) {
    devices.off()
  }
  else if (devices*.currentValue('switch').contains('off')) {
    devices.on()
  }
  else if (devices*.currentValue('lock').contains('locked')) {
    devices.unlock()
  }
  else if (devices*.currentValue('lock').contains('unlocked')) {
    devices.lock()
  }
  else {
    devices.on()
  }
}

//SB
def setLightLevel(devices, level) {
  log.debug "setLevel: $devices = ${devices*.currentSwitch}"
  if (level < 1) level = 1
  if (level > 99) level = 99
  if (devices*.currentValue('switch').contains('on')) {
    devices.off()
  }
  else if (devices*.currentValue('switch').contains('off')) {
    devices.setLevel(level)
  }
  else {
    devices.on()
  }
}


def setMyColor(devices, color, level) {
  log.debug "setLevel: $devices = ${devices*.currentSwitch}"
  if (devices*.currentValue('switch').contains('on')) {
    devices.off()
  }
  else if (devices*.currentValue('switch').contains('off')) {
    setLightColor(devices, color, level)
  }
  else {
    devices.on()
  }
}

def setLightColor(devices, color, brightnessLevel)
{
 log.debug "setColor: $devices = ${devices*.currentValue('switch')}"
	//Initialize the hue and saturation
	def hueColor = 0
	def saturation = 100

	//Use the user specified brightness level. If they exceeded the min or max values, overwrite the brightness with the actual min/max
	if (brightnessLevel<1) {
		brightnessLevel=1
	}
    else if (brightnessLevel>100) {
		brightnessLevel=100
	}
    //Set the hue and saturation for the specified color.
	switch(color) {
		case "White":
			hueColor = 0
			saturation = 0
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80 
			break;
        case "Navy Blue":
            hueColor = 61
            break;
		case "Blue":
			hueColor = 65
			break;
		case "Green":
			hueColor = 33
			break;
        case "Turquoise":
        	hueColor = 47
            break;
        case "Aqua":
            hueColor = 50
            break;
        case "Amber":
            hueColor = 13
            break;
		case "Yellow":
			//hueColor = 25
            hueColor = 17
			break; 
        case "Safety Orange":
            hueColor = 7
            break;
		case "Orange":
			hueColor = 10
			break;
        case "Indigo":
            hueColor = 73
            break;
		case "Purple":
			hueColor = 82
			saturation = 100
			break;
		case "Pink":
			hueColor = 90.78
			saturation = 67.84
			break;
        case "Rasberry":
            hueColor = 94
            break;
		case "Red":
			hueColor = 0
			break;
         case "Brick Red":
            hueColor = 4
            break;
         default:
           hueColor = 0
		   saturation = 0
           break;
	}

	//Change the color of the light
	def newValue = [hue: hueColor, saturation: saturation, level: brightnessLevel]  
	devices.setColor(newValue)
    state.currentColor = color
    //mysend("$app.label: Setting Color = $color")
    log.debug "$app.label: Setting Color = $color"
}

def adjustFan(devices) {
	log.debug "adjustFan: $devices = ${devices*.currentLevel}"
    
    def levelsList = devices*.currentLevel
    levelsList.sort()
    levelsList = levelsList.reverse() 
	def currentLevel = levelsList[0]
	log.debug "currentLevel =$currentLevel"

	if (!devices*.currentValue('switch').contains('on')) devices.setLevel(20)
	else if (currentLevel < 34) devices.setLevel(50)
  	else if (currentLevel < 67) devices.setLevel(99)
	else devices.off()
}

def dimmerUp(devices) {
    //Must take your time (~5s) between button presses or it may get messed up. 
	log.debug "dimmerUp: $devices = ${devices*.currentLevel}"
    
    def levelsList = devices*.currentLevel
    levelsList.sort()
    levelsList = levelsList.reverse()
	def currentLevel = levelsList[0]
   	log.debug "currentLevel =$currentLevel"


	if (!devices*.currentValue('switch').contains('on')) devices.setLevel(10)
	else if (currentLevel < 20) devices.setLevel(20)
	else if (currentLevel < 30) devices.setLevel(30)
	else if (currentLevel < 40) devices.setLevel(40)
	else if (currentLevel < 50) devices.setLevel(50)
	else if (currentLevel < 60) devices.setLevel(60)
	else if (currentLevel < 70) devices.setLevel(70)
 	else if (currentLevel < 80) devices.setLevel(80)
 	else if (currentLevel < 90) devices.setLevel(90)
 	else if (currentLevel < 99) devices.setLevel(99)
 	else devices.on()
}

def dimmerDown(devices) {
    //Must take your time (~5s) between button presses or it may get messed up. 
    log.debug "dimmerDown: $devices = ${devices*.currentLevel}"
   
    def levelsList = devices*.currentLevel
    log.debug "listSize= $levelsList.size"
    levelsList.sort()
    log.debug "levelsListSorted= $levelsList"
    //levelsList = levelsList.reverse()
    //log.debug "levelsListSortedDesc= $levelsList"

    log.debug "1st Item = ${levelsList[0]}"
	def currentLevel = levelsList[0]
	log.debug "currentLevel =$currentLevel"

	if (devices*.currentValue('switch').contains('off')) devices.off()
	else if (currentLevel > 90) devices.setLevel(90)
	else if (currentLevel > 80) devices.setLevel(80)
	else if (currentLevel > 70) devices.setLevel(70)
	else if (currentLevel > 60) devices.setLevel(60)
	else if (currentLevel > 50) devices.setLevel(50)
	else if (currentLevel > 40) devices.setLevel(40)
	else if (currentLevel > 30) devices.setLevel(30)
	else if (currentLevel > 20) devices.setLevel(20)
	else if (currentLevel > 10) devices.setLevel(10)
	else devices.off()
}

def adjustShade(device) {
	log.debug "adjustShade: $device = ${device.currentMotor} state.lastUP = $state.lastshadesUp"

	if(device.currentMotor in ["up","down"]) {
    	state.lastshadesUp = device.currentMotor == "up"
    	device.stop()
    } else {
    	state.lastshadesUp ? device.down() : device.up()
//    	if(state.lastshadesUp) device.down()
//        else device.up()
        state.lastshadesUp = !state.lastshadesUp
    }
}
//END SB

def changeMode(mode) {
  log.debug "changeMode: $mode, location.mode = $location.mode, location.modes = $location.modes"

  if (location.mode != mode && location.modes?.find { it.name == mode }) {
    setLocationMode(mode)
  }
}

// execution filter methods
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
    def start = timeToday(starting).time
    def stop = timeToday(ending).time
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

private hideOptionsSection() {
  (starting || ending || days || modes) ? false : true
}

private hideSection(buttonNumber, action) {
  (find("lights", buttonNumber, action) || find("locks", buttonNumber, action) || find("sonos", buttonNumber, action)) ? false : true
}

private hideLocksSection(buttonNumber) {
  (find("lights", buttonNumber, "lock") || find("locks", buttonNumber, "unlock")) ? false : true
}

private timeIntervalLabel() {
  (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private integer(String s) {
  return Integer.parseInt(s)
}

