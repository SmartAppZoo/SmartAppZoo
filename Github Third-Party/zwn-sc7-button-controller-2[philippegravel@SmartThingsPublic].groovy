/**
 *	Button Controller App for ZWN-SC7
 *
 *	Author: Matt Frank based on VRCS Button Controller by Brian Dahlem, based on SmartThings Button Controller
 *	Date Created: 2014-12-18
 *  	Last Updated: 2015-11-14
 * 
 * 	Contributions from erocm1231 @ SmartThings Community
 *
 */
definition(
    name: "ZWN-SC7	 Button Controller 2",
    namespace: "philippegravel",
    author: "Philippe Gravel, using code from Matt Frank, using code from Brian Dahlem",
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
    
    section(title: "Other", mobileOnly:true, hideable: true, hidden: true) {
		label title: "Assign a name", required: false
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
      input "lights_${buttonNumber}_toggle", "capability.switch", title: "switches:", multiple: true, required: false
      input "resetLevel_${buttonNumber}_toggle", "bool", title: "Reset Lights to 100% at On"
    }
    section(title: "Turn on these...", hidden: hideSection(buttonNumber, "on"), hideable: true) {
      input "lights_${buttonNumber}_on", "capability.switch", title: "switches:", multiple: true, required: false
    }
    section(title: "Turn on with Percent...", hidden: ((find("lights", buttonNumber, "dim")) ? false : true), hideable: true) {
      input "lights_${buttonNumber}_dim", "capability.switch", title: "switches:", multiple: true, required: false
	  input "percent_${buttonNumber}_dim", "number", title:"Light Percent?", required: false
    }        
    section(title: "Turn on with Color...", hidden: ((find("lights", buttonNumber, "color")) ? false : true), hideable: true) {
      input "lights_${buttonNumber}_color", "capability.switch", title: "switches:", multiple: true, required: false
	  input "color_${buttonNumber}_color", "enum", title:"Light Color?", required: false,
		options: ["White", "Daylight", "Warm White", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink", "Cyan", "Magenta", "Nath relax"]      
    }
    section(title: "Turn off these...", hidden: hideSection(buttonNumber, "off"), hideable: true) {
      input "lights_${buttonNumber}_off", "capability.switch", title: "switches:", multiple: true, required: false
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
    settings["mode_$idx_on"] ||
    settings["lights_$idx_on"] ||
    settings["lights_$idx_off"]
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
  if (lights != null) 
  {
  	def resetLevel = find('resetLevel', buttonNumber, "toggle")
    toggle(lights, resetLevel)
  }
  
  lights = find('lights', buttonNumber, "on")
  if (lights != null) flip(lights, "on", 0)
  
  lights = find('lights', buttonNumber, "dim")
  if (lights != null) {
  	def percentLights = find('percent', buttonNumber, "dim")
    flip(lights, "dim", percentLights)
  }
  
  lights = find('lights', buttonNumber, "color")
  if (lights != null) {
    def colorLights = find('color', buttonNumber, "color")
    setColor(lights, colorLights)
  }
  
  lights = find('lights', buttonNumber, "off")
  if (lights != null) flip(lights, "off", 0)

  def mode = find('mode', buttonNumber, "on")
  if (mode != null) changeMode(mode)

  def phrase = find('phrase', buttonNumber, "on")
  if (phrase != null) location.helloHome.execute(phrase)
}

def find(type, buttonNumber, value) {
  def preferenceName = type + "_" + buttonNumber + "_" + value
  log.debug "preferenceName: " + preferenceName
  def pref = settings[preferenceName]
  if(pref != null) {
    log.debug "Found: $pref for $preferenceName"
  }

  return pref
}

def flip(devices, newState, percentLight) {
  log.debug "flip: $devices = ${devices*.currentValue('switch')}"

  if (newState == "off") {
    devices*.off()
  }
  else if (newState == "on") {
    devices*.on()
  }
  else if (newState == "dim") {
    devices*.setLevel(percentLight)
  }
}

def setColor(devices, colorToSet) {
	log.debug "Color: $devices = ${devices*.currentValue('switch')} ${devices*.currentValue('color')} New Color: E"

	switch (colorToSet) {
    	case "White":
        	devices*.white()
        	break;
        case "Daylight":
        	devices*.coldWhite()
        	break;
        case "Warm White":
        	devices*.warmWhite()
        	break;
        case "Red":
        	devices*.red()
        	break;
        case "Green":
        	devices*.green()
        	break;
        case "Blue":
        	devices*.blue()
        	break;
        case "Yellow":
        	devices*.yellow()
        	break;
        case "Orange":
        	devices*.orange()
        	break;
        case "Purple":
        	devices*.purple()
        	break;
        case "Pink":
        	devices*.pink()
        	break;
        case "Cyan":
        	devices*.cyan()
        	break;
        case "Magenta":
        	devices*.magenta()
        	break;        
            
        case "Nath relax":
        	devices*.setColor(hex: "#C696FF")
			devices*.setLevel(30)
            break;
    }
}

def toggle(devices, resetLevel) {
  log.debug "toggle: $devices = ${devices*.currentValue('switch')}"

  if (devices*.currentValue('switch').contains('on')) {
    devices*.off()
  }
  else if (devices*.currentValue('switch').contains('off')) {

    devices*.on()
    if (resetLevel) {
      devices*.setLevel(100)
    }
  }
  else {
    devices*.on()
  }
}

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

private hideSection(buttonNumber, actionType) {
//  (find("lights", buttonNumber, action) || find("locks", buttonNumber, action) || find("sonos", buttonNumber, action)) ? false : true
  (find("lights", buttonNumber, actionType)) ? false : true
}

//private hideLocksSection(buttonNumber) {
//  (find("lights", buttonNumber, "lock") || find("locks", buttonNumber, "unlock")) ? false : true
//}

private timeIntervalLabel() {
  (starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

private integer(String s) {
  return Integer.parseInt(s)
}