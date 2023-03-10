/**
*
*  Super Sprinkler Controller
*
*  Copyright Mark Grimes
*
*  Base on the excellent work of:
*  Irrigation Scheduler SmartApp Smarter Lawn Controller
*  Compatible with an arbitray number of Zones
*  Author: Stan Dotson (stan@dotson.info) and Matthew Nichols (matt@nichols.name)
*  Date: 2014-06-16
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
**/

definition(
    name: "Super Sprinkler Controller",
    namespace: "mvgrimes",
    author: "mgrimes@cpan.org",
    description: "Schedule sprinklers to run unless there is rain.",
    version: "3",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/water_moisture@2x.png"
)

preferences {
    page(name: "schedulePage", title: "Schedule", nextPage: "sprinklerPage", uninstall: true) {
        section("App configruation...") {
            label name: "label", title: "Choose an title for App", required: true, defaultValue: "Super Sprinkler Controller"
        }
        section {
            input (
            name: "wateringDays",
            type: "enum",
            title: "Water which days?",
            required: false,
            multiple: true, // This must be changed to false for development (known ST IDE bug)
            metadata: [values: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']])
        }

        section("Water every...") {
            input "days", "number", title: "Days?", description: "minimum # days between watering", required: false
        }

        section("Water when...") {
            input name: "waterTimeOne",  type: "time", required: true, title: "Turn them on at..."
            input name: "waterTimeTwo",  type: "time", required: false, title: "and again at..."
            input name: "waterTimeThree",  type: "time", required: false, title: "and again at..."
        }

        section("Water every...") {
            input "days", "number", title: "Days?", description: "minimum # days between watering", required: false
        }

        section("Sprinkler switch...") {
            input "switches", "capability.switch", multiple: false
        }

        section("Sprinkler zones...") {
            input "theZoneCount", "number", title: "Zones?", description: "how many zones on your sprinkler?", defaultValue: 8, required: false
        }
    }

    // Include dyanmic page
    page(name: "sprinklerPage", title: "Sprinkler Controller Setup", nextPage: "virtualRainGuage", uninstall: true)

    page (name: "virtualRainGuage", title: "Virtual Rain Guage Setup", install: true) {

        section("Zip code to check weather...") {
            input "zipcode", "text", title: "Zipcode?", required: false
        }

        section("Select which rain to add to guage...") {
            input "isYesterdaysRainEnabled", "boolean", title: "Yesterday's Rain", description: "Include?", defaultValue: "true", required: false
            input "isTodaysRainEnabled", "boolean", title: "Today's Rain", description: "Include?", defaultValue: "true", required: false
            input "isForecastRainEnabled", "boolean", title: "Today's Forecasted Rain", description: "Include?", defaultValue: "false", required: false
        }

       section("Skip watering if more than... (default 0.5)") {
            input "wetThreshold", "decimal", title: "Inches?", required: false
        }

    }
}

def sprinklerPage(){
  dynamicPage(name: "sprinklerPage", install: false, uninstall: true) {
        section("Zone Times...") {
            for (int i = 1; i <= settings.theZoneCount; i++ ) {
                input "zone${i}", "number", title: "Zone ${i} Time", description: "minutes", multiple: false, required: false
            }
        }

     }
}

def installed() {
    scheduling()
    state.daysSinceLastWatering = [0,0,0]
    settings.theZoneCount = settings.theZoneCount ?: 8;
    log.debug("installed: settings.theZoneCount = ${settings.theZoneCount}")
}

def updated() {
    unschedule()
    scheduling()
    state.daysSinceLastWatering = [0,0,0]
    settings.theZoneCount = settings.theZoneCount ?: 8;
    log.debug("updated: settings.theZoneCount = ${settings.theZoneCount}")
}

// Scheduling
def scheduling() {
    schedule(waterTimeOne, "waterTimeOneStart")
    if (waterTimeTwo) {
        schedule(waterTimeTwo, "waterTimeTwoStart")
    }
    if (waterTimeThree) {
        schedule(waterTimeThree, "waterTimeThreeStart")
    }
}

def waterTimeOneStart() {
    state.currentTimerIx = 0
    scheduleCheck()
}
def waterTimeTwoStart() {
    state.currentTimerIx = 1
    scheduleCheck()
}
def waterTimeThreeStart() {
    state.currentTimerIx = 2
    scheduleCheck()
}

def scheduleCheck() {

    def schedulerState = switches?.latestValue("effect")?.toString() ?:"[noEffect]"


    if (schedulerState == "onHold") {
        log.info("Sprinkler schedule on hold.")
        return
    }

    if (schedulerState == "skip") {
        // delay this watering and reset device.effect to noEffect
        schedulerState = "delay"
        for(s in switches) {
            if("noEffect" in s.supportedCommands.collect { it.name }) {
                s.noEffect()
                log.info ("sent noEffect() to ${s}")
            }
        }
    }

    if (schedulerState != "expedite") {
        // Change to rain delay if wet
        schedulerState = isRainDelay() ? "delay" : schedulerState
    }

    if (schedulerState != "delay") {
        state.daysSinceLastWatering[state.currentTimerIx] = daysSince() + 1
    }

    log.info("Scheduler state: $schedulerState. Days since last watering: ${daysSince()}. Is watering day? ${isWateringDay()}. Enought time? ${enoughTimeElapsed(schedulerState)} ")

    if ((isWateringDay() && enoughTimeElapsed(schedulerState) && schedulerState != "delay") || schedulerState == "expedite") {
        // Should this be ${switches[0]}
        sendPush("$switches Is Watering Now!" ?: "null pointer on app name")
        state.daysSinceLastWatering[state.currentTimerIx] = 0
        water()
        // Assuming that sprinklers will turn themselves off.
    }
}

def isWateringDay() {
    if(!wateringDays) return true

    def today = new Date().format("EEEE", location.timeZone)
    if (wateringDays.contains(today)) {
        return true
    }
    log.info "watering is not scheduled for today"
    return false
}

def enoughTimeElapsed(schedulerState) {
    if(!days) return true
    return (daysSince() >= days)
}

def daysSince() {
    if(!state.daysSinceLastWatering) state.daysSinceLastWatering = [0,0,0]
    state.daysSinceLastWatering[state.currentTimerIx] ?: 0
}

def isRainDelay() {

    def rainGauge = 0

    if (isYesterdaysRainEnabled.equals("true")) {
        rainGauge = rainGauge + wasWetYesterday()
    }

    if (isTodaysRainEnabled.equals("true")) {
        rainGauge = rainGauge + isWet()
    }

    if (isForecastRainEnabled.equals("true")) {
        rainGauge = rainGauge + isStormy()
    }

    log.info ("Virtual rain gauge reads $rainGauge in")
    if (rainGauge > (wetThreshold?.toFloat() ?: 0.5)) {
        sendPush("Skipping watering today due to precipitation.")
        for(s in switches) {
            if("rainDelayed" in s.supportedCommands.collect { it.name }) {
                s.rainDelayed()
                log.trace "Watering is rain delayed for $s"
            }
        }
        return true
    }
    return false
}

def safeToFloat(value) {
    if(value && value.isFloat()) return value.toFloat()
    return 0.0
}

def retrieveWeather( feature, zipcode ){
    def weather = getWeatherFeature(feature, zipcode)
    log.debug( "weather for ${feature}: ${weather}" )
    if( weather.containsKey("response") && weather.response.containsKey("error") ){
      log.warn( "unable to request ${feature} weather: ${weather}" )
      return false
    }
    return weather
}

def wasWetYesterday() {
    if (!zipcode) return 0.0

    def yesterdaysWeather = retrieveWeather("yesterday", zipcode)
    if( ! yesterdaysWeather ) return 0.0
    def yesterdaysPrecip=yesterdaysWeather.history.dailysummary.precipi.toArray()
    def yesterdaysInches=safeToFloat(yesterdaysPrecip[0])
    log.info("Checking yesterday's percipitation for $zipcode: $yesterdaysInches in")
    return yesterdaysInches
}


def isWet() {
    if (!zipcode) return 0.0

    def todaysWeather = retrieveWeather("conditions", zipcode)
    if( ! todaysWeather ) return 0.0
    def todaysInches = safeToFloat(todaysWeather.current_observation.precip_today_in)
    log.info("Checking today's percipitation for $zipcode: $todaysInches in")
    return todaysInches
}

def isStormy() {
    if (!zipcode) return 0.0

    def forecastWeather = retrieveWeather("forecast", zipcode)
    if( ! forecastWeather ) return 0.0
    def forecastPrecip=forecastWeather.forecast.simpleforecast.forecastday.qpf_allday.in.toArray()
    def forecastInches=forecastPrecip[0]
    log.info("Checking forecast percipitation for $zipcode: $forecastInches in")
    return forecastInches
}

def water() {
    log.debug( "water() called" )
    log.debug("settings.theZoneCount is ${settings.theZoneCount}")
    state.triggered = true
    if(anyZoneTimes()) {
        def zoneTimes = []
        for(int z = 1; z <= settings.theZoneCount; z++) {
            def zoneTime = settings["zone${z}"]
            if( zoneTime ) {
                if( zoneTime.isNumber() ) {
                    zoneTimes += "${z}:${zoneTime}"
                    log.info("Zone ${z} on for ${zoneTime} minutes")
                } else {
                    log.warn("Zone ${z} duration is not a number: '${zoneTime}'")
                }
            }
        }
        switches.OnWithZoneTimes(zoneTimes.join(","))
    } else {
        log.debug("Turning all zones on")
        switches.on()
    }
}

def anyZoneTimes() {
    log.debug("settings are ${settings}")
    log.debug("settings.theZoneCount is ${settings.theZoneCount}")
    for(int i = 1; i <= settings.theZoneCount; i++) {
        def duration = settings["zone${i}"]
        log.debug("zone${i} has duration of ${duration}")
        if( duration?.isNumber() && duration > 0 ) return true
    }
    return false;
}
