definition(
    name: "Baur Lighting",
    namespace: "baurandr",
    author: "Andrew Baur",
    description: "Container for all lighting automations",
    category: "My Apps",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light1-icn@2x.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Lighting/light1-icn@2x.png")


preferences {
    // The parent app preferences are pretty simple: just use the app input for the child app.
    page(name: "mainPage", title: "My Lighting Routines", install: true, uninstall: true, submitOnChange: true) {
        section {
            app(name: "motionActivatedLighting", appName: "Motion & Contact Activated Lighting", namespace: "baurandr", title: "Create New Motion/Contact Activated Lighting Routine", multiple: true)
        }
                section {
            app(name: "motionActivatedLightingV2", appName: "Motion & Contact Activated Lighting v2", namespace: "baurandr", title: "Create New Motion/Contact Activated Lighting Routine v2", multiple: true)
        }
		section {
            app(name: "doubleTap", appName: "Double Tap", namespace: "baurandr", title: "Create Double Tap Lighting Routine", multiple: true)
        }
        section{
            app(name: "flashLighting", appName: "Flash Lighting", namespace: "baurandr", title: "Create New Flash Lighting Routine", multiple: true)
        }
        section ("Global Preferences"){
            	input "offset", "number", title: "Daylight offset before/after sunset/sunrise - in minutes"
        }
        section("Sun State Device"){
				input "sunStateDevice", "capability.sensor", title: "?"
		}
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {

    subscribe(location, "sunsetTime", sunsetTimeHandler)
    subscribe(location, "sunriseTime", sunriseTimeHandler)
    
    //schedule it to run today too
    scheduleSunsetChange(location.currentValue("sunsetTime"))
    scheduleSunriseChange(location.currentValue("sunriseTime"))

    log.debug "there are ${childApps.size()} child smartapps"
    childApps.each {child ->
        log.debug "child app: ${child.label}"
    }
}

def sunsetTimeHandler(evt) {
    //when I find out the sunset time, schedule the lights to turn on with an offset
    scheduleSunsetChange(evt.value)
}

def sunriseTimeHandler(evt) {
    //when I find out the sunrise time, schedule the lights to turn off with an offset
    scheduleSunriseChange(evt.value)
}

def scheduleSunsetChange(sunsetString) {
    //get the Date value for the string
    def sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunsetString)

    //calculate the offset
    def timeBeforeSunset = new Date(sunsetTime.time - (offset * 60 * 1000))
    def timeAfterSunset = new Date(sunsetTime.time + (offset * 60 * 1000))

    log.debug "Scheduling dusk for: $timeBeforeSunset (sunset is $sunsetTime)"
    log.debug "Scheduling night for: $timeAfterSunset (sunset is $sunsetTime)"
    
    //schedule this to run one time
    runOnce(timeBeforeSunset, changeToDusk, [overwrite: false])
	runOnce(timeAfterSunset, changeToNight, [overwrite: false])
}

def scheduleSunriseChange(sunriseString) {
    //get the Date value for the string
    def sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", sunriseString)

    //calculate the offset
    def timeBeforeSunrise = new Date(sunriseTime.time - (offset * 60 * 1000))
	def timeAfterSunrise = new Date(sunriseTime.time + (offset * 60 * 1000))

    log.debug "Scheduling dawn for: $timeBeforeSunrise (sunrise is $sunriseTime)"
    log.debug "Scheduling day for: $timeAfterSunrise (sunrise is $sunriseTime)"

    //schedule this to run one time
    runOnce(timeBeforeSunrise, changeToDawn, [overwrite: false])
    runOnce(timeAfterSunrise, changeToDay, [overwrite: false])
}

def changeToDusk() {
    log.debug "Changing to dusk mode"
	sunStateDevice.setSunState("Dusk")
}
def changeToDawn() {
    log.debug "Change to dawn mode"
    sunStateDevice.setSunState("Dawn")
}
def changeToDay() {
    log.debug "Changing to day mode"
    sunStateDevice.setSunState("Day")
}
def changeToNight() {
    log.debug "Change to night mode"
    sunStateDevice.setSunState("Night")
}
