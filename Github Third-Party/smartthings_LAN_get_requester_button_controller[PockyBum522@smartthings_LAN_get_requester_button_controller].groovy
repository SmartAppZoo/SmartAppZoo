/**
 *  
 *	Modified Button Controller
 *
 *  Modified by David so a HTTP get is sent when buttons are pressed instead of controlling lighting.
 *
 *  smartthings_LAN_get_requester_button_controller Allows a smartthings button device, such as the Aeon minimote to 
 *  make GET requests to LAN devices that are on the same network as the hub.
 *
 *  Modified by: David Sikes
 *  email: pockybum522 at gmail
 *
 *	Original Author: Eric Maycock (erocm123) email: erocmail@gmail.com (Originally used for controlling Hue lights.)
 *   
 *	Note: The app treats the Aeon Minimote buttons as 1,2,3,4 for a single press and
 *	5,6,7,8 when the buttons are pressed and held. This should be pretty self explanatory but
 *	holding 1 is equivalent to button 5, holding 2 is equivalent to button 6, and so on.
 *
 */
 


definition(
    name: "Aeon Minimote Button Configure for HTTP Get",
    namespace: "pockybum522",
    author: "PockyBum522",
    description: "Fire a HTTP get with button controllers like the Aeon Labs Minimote",
    category: "Convenience",
    iconUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Applications-internet.svg/2000px-Applications-internet.svg.png",
    iconX2Url: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/70/Applications-internet.svg/2000px-Applications-internet.svg.png"
)

preferences {
	page(name: "selectButton")
	page(name: "configureButton") 
}

def selectButton() {
	dynamicPage(name: "selectButton", title: "First, select your button device", nextPage: null, uninstall: configured(), install: true) {
		section {
			input "buttonDevice", "capability.button", title: "Button", multiple: false, required: true, submitOnChange: true
		}
        section("Menu") {
                input "numberOfButtons", "enum", title: "Number of Buttons?", required: true, value: 8, defaultValue: 8, submitOnChange: true, options: [
                1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24]
                def configDescription = ""
                for (int i = 1; i <= (numberOfButtons as Integer); i++){
                   configDescription = ""
                   if (settings["lights_${i}"] != null) { 
                       settings["lights_${i}"].each {
                         configDescription += "${it.displayName}, "
                      }
                      configDescription = configDescription.substring(0, configDescription.length() - 2)
                   } else {
                      configDescription = "Click to configure"
                   }
                   href "configureButton", title:"Configure Button $i", description:"$configDescription", params: [pbutton: i]
                }
        }
        
		section(title: "Options", hidden: hideOptionsSection(), hideable: false) {
            input "debounce", "number", title: "Debounce time in milliseconds (set to 0 to disable)", required: true, value: 3000, defaultValue: 3000
            def timeLabel = timeIntervalLabel()
            href "timeIntervalInput", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
		}
        section([title:"Available Options", mobileOnly:true]) {
			label title:"Assign a name for your app (optional)", required:false
		}
	}
}

def configureButton(params) {
    if (params.pbutton != null) state.currentButton = params.pbutton.toInteger() //log.debug "$params.pbutton"
    dynamicPage(name: "configureButton", title: "Button ${state.currentButton} request URL.", uninstall: configured()){
    section (URLget){
      input "URLLocation${state.currentButton}", "text", title: "Button ${state.currentButton} URL to GET:", required:false
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
}

def configured() {
	return buttonDevice || buttonConfigured(1) || buttonConfigured(2) || buttonConfigured(3) || buttonConfigured(4) || buttonConfigured(5) || buttonConfigured(6) || buttonConfigured(7) || buttonConfigured(8)
}

def buttonConfigured(idx) {
	return settings["lights_$idx"]
}

def buttonEvent(evt){
	if(allOk) {
        def buttonNumber = evt.jsonData.buttonNumber
        def firstEventId = 0
		def value = evt.value
		//log.debug "buttonEvent: $evt.name = $evt.value ($evt.data)"
		log.debug "button: $buttonNumber, value: $value"
        if (value == "held")
        	buttonNumber = buttonNumber + numberOfButtons.toInteger()/2
        if (value == "double")
        	buttonNumber = buttonNumber + numberOfButtons.toInteger() 
        
        if(debounce != null && debounce != "" && debounce > 0) {
            def recentEvents = buttonDevice.eventsSince(new Date(now() - debounce)).findAll{it.value == evt.value && it.data == evt.data}
            log.debug "Found ${recentEvents.size()?:0} events in past ${debounce/1000} seconds"
            if (recentEvents.size() != 0){
                log.debug "First Event ID: ${recentEvents[0].id}"
                firstEventId = recentEvents[0].id
            }
            else {
                firstEventId = 0
            }
        } else {
            firstEventId = evt.id
        }
        
        log.debug "This Event ID: ${evt.id}"

		if(firstEventId == evt.id){
			switch(buttonNumber) {
				case ~/.*1.*/:
					executeHandlers(1)
                    sendHttpLAN(URLLocation1)               
					break
	 			case ~/.*2.*/:
					executeHandlers(2)
					sendHttpLAN(URLLocation2)
					break
				case ~/.*3.*/:
					executeHandlers(3)
					sendHttpLAN(URLLocation3)
					break
				case ~/.*4.*/:
					executeHandlers(4)
					sendHttpLAN(URLLocation4)
					break
                case ~/.*5.*/:
					executeHandlers(5)
					sendHttpLAN(URLLocation5)
					break
                case ~/.*6.*/:
					executeHandlers(6)
					sendHttpLAN(URLLocation6)
					break
                case ~/.*7.*/:
					executeHandlers(7)
					sendHttpLAN(URLLocation7)
					break
                case ~/.*8.*/:
					executeHandlers(8)
					sendHttpLAN(URLLocation8)
					break
			}
		} else if (firstEventId == 0) {
      		log.debug "No events found. Possible SmartThings latency"
    	} else {
      		log.debug "Duplicate button press found. Not executing handlers"
    	}
	}
}

def executeHandlers(buttonNumber) {
    log.debug "executeHandlers: $buttonNumber"
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

private timeIntervalLabel() {
	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
}

def sendHttpLAN(def httpToGet, def port = "80") {
  def hostToGet = httpToGet.toLowerCase()
  hostToGet = hostToGet - "http://"
  hostToGet = hostToGet - "www."
  
  hostToGet = hostToGet.split("/", 2)
  def path = hostToGet[1]
  hostToGet = hostToGet[0]
  
  sendHubCommand(new physicalgraph.device.HubAction("""GET /$path HTTP/1.1\r\nHOST: $hostToGet:$port\r\n\r\n""", physicalgraph.device.Protocol.LAN))
  log.debug "Request to GET /$path HTTP/1.1 (newline) HOST: $hostToGet:$port sent"
}