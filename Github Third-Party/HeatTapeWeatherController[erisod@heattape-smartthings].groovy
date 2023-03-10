/**
 *  Heat Tape Weather Controller v0.4.4
 *
 *  Copyright 2015-2017 Eric Mayers
 *  This is a major re-write of erobertshaw's V2.1 Smart Heat Tape controller.
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
    name: "Heat Tape Weather Control",
    namespace: "erisod",
    author: "Eric Mayers",
    description: "Controls snow melting equipment (eg heat tape) using weather data.  Tested " +
    "with Aeon Labs DSC06106-ZWUS switches.  Calculates snow accumulation and melt and uses " +
    "current temperature.",
    category: "Green Living",
    iconUrl: "http://cdn.device-icons.smartthings.com/Weather/weather6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Weather/weather6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Weather/weather6-icn@3x.png") {
    appSetting "zipcode"
    appSetting "heattape"
}

def setupInitialConditions() {
    // Possibly make this configurable in the future.
    if (state.MAX_HISTORY == null) {
        state.MAX_HISTORY = 24 * 14 // 2 weeks of hourly readings.
    }

    // History data structure.
    if (state.history == null) {
        def historyList = []
        state.history = historyList
    }

    // Initial snow level.
    if (state.start_snow == null) {
        state.start_snow = -1
    }

    // Current control state.
    if (state.controlOn == null) {
        state.controlOn = false
    }
}

preferences {
    page(name: "setupPage")
    page(name: "setupTempPage")
    page(name: "listInfoPage")
}

def historyExists() {
    if (state.history.size() > 0)
    	return true
    else
    	return false
}

def setupPage() {
    setupInitialConditions()

    return dynamicPage(name: "setupPage", title: "Device Setup", nextPage: "setupTempPage",
        uninstall: false, install: false) {
        // If we have configured devices, show the current state of them.
        if (heattape != null) {
            section("Current Status") {
                def status = ""
                status =
                    "Current Temp: ${getTemp()}°c\n" +
                    "Current Condition: ${getWeather()}\n" +
                    "Calculated Snow Depth: ${getSnowDepth()}mm\n" +
                    "App Mode: ${op_mode}\n" +
                    "Heat Tape: ${getHeattapeState()} drawing ${getHeattapePower()} watts\n"
                heattape.each {
                    status += "   " + it.displayName + " : " + it.currentValue("switch") + "\n"
                }
                // Remove last line feed.
                status = status[0..-2]
                paragraph status
            }
        }

        // If we have temperature and snowfall history, display it, and tracked depth.
        if (state.history.size() > 0) {
            section("Hourly History", hidden: false, hideable: true) {
                paragraph getHistoryText()
            }
        }

        // General device configuration.
        section("Device & Location Setup") {
            input "heattape", "capability.switch", title: "Heat tape switches", required: true, multiple: true
            input "zipcode", "text", title: "Zipcode or Wunderground weatherstation code (e.g. pws:code)",
                required: true, defaultValue: "pws:KCASOUTH41"
        }
    }
}

def getHistoryText() {
    def history = ""
    def lastDate = ""
    state.history.each {
        def newDate = new Date(it.ts).format("MM-dd", location.timeZone)
        if (newDate != lastDate && it.ts != 0) {
        	history += "${newDate}     Snowfall Temp Power Condition\n"
            lastDate = newDate
        }
        def time = new Date(it.ts).format("k:mm", location.timeZone)
        def snow_mm = it.snow_mm != null ? it.snow_mm : "?"
        def temp_c = it.temp_c != null ? it.temp_c : "?"
        def weather = it.weather != null ? it.weather : "na"
        def power = it.power != null ? it.power : "?"

        if (it.ts == 0)
            history += "Initial setup with ${snow_mm}mm of ground snow"
		else
            history += "@${time} ${snow_mm}mm ${temp_c}°c ${power}w ${weather}\n"
    }

    return history
}

def setupTempPage() {
    return dynamicPage(name: "setupTempPage", title: "Temperature Setup", uninstall: true, install: true) {
        section("Temp range") {
            paragraph "When the temperature is between the min and max values, and there is recent " +
                "precipitation the heat tape switches will be turned on.  In Temp only mode snowfall is ignored."
            input "minTemp", "float", title: "Min Temp (°c)", required: true, defaultValue: -10.0
            input "maxTemp", "float", title: "Max Temp (°c)", required: true, defaultValue: 2.0
        }

        section("Operation Mode") {
            paragraph "Auto tracks snowfall and melt rates.  Temp only uses only temperature range.  Force " +
                "modes simply turn the switches always on or always off (generally for testing).  " +
                "Monitor-Only does not control switches but monitors weather and switch state."
            input "op_mode", "enum", title: "Mode",
                options: ["Automatic", "TemperatureOnly", "ForceOn", "ForceOff", "Monitor"],
                defaultValue: "Automatic"
        }

        if (state.history.size() == 0) {
            section("Initial snow level") {
                paragraph "Indicate additional snow if present when first installing, in mm.  Only " +
                    " available at SmartApp installation time."
                input "start_snow", "float", title: "Starting Snow level (in mm)", required: true,
                    defaultValue: 5.0
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()

    // Get a data reading on first install so we have accurate temp data for control.
    snowBookkeeper()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "initialize()"

    setupInitialConditions()
	startSnowHack()
	snowBookkeeper()
	runEvery1Hour(snowBookkeeper)
    runEvery15Minutes(heattapeController)
}

// Returns power (in Watts) aggregate over switches that provide this data.
def getHeattapePower() {
    float aggregate_power = 0.0

    if (heattape) {
        heattape.each {
            if (it.currentValue("power")) {
                aggregate_power += it.currentValue("power").toFloat()
            }
        }
    }

    return aggregate_power
}

// Describe heat tape state.  Returns "off", "on", or "mixed".
def getHeattapeState() {
    def units_on = 0
    def units_off = 0

    if (heattape) {
        heattape.each {
            if (it.currentValue("switch") == "on") {
                units_on++
            } else {
                units_off++
            }
        }
    }

    if (units_on > 0 && units_off > 0) {
        return "mixed"
    } else if (units_on == 0 && units_off == 0) {
        return "N/A (no units)"
    } else if (units_on > 0) {
        return "on"
    } else {
        return "off"
    }
}

// When installing the app you may indicate existing snow on the ground.  The app can only learn
// about additional snowfall so this is necessary.  This function injects a "fake" historical record
// an hour ago to represent that snow.
def startSnowHack() {
    if (state.history.size() == 0 && start_snow != 0.0) {
        def newHistory = [ts: 0, snow_mm: start_snow, temp_c: 0.0, power: 0.0, weather: "initial setup"]

        // Push in new history data.
        state.history.add(0, newHistory)
    }
}

// Fetch current sun level 0.0-1.0; 1.0 being full sun. 
def getSunLevel() {
    // Identify sunrise and sunset where the hub is (location implied).  
    def sunData = getSunriseAndSunset()

    def weatherMap = ["Clear": 0.9,
        "Mostly Cloudy": 0.3,
        "Mostly Sunny": 0.8,
        "Partly Cloudy": 0.4,
        "Partly Sunny": 0.6,
        "Sunny": 1.0,
        "Cloudy": 0.1,
        "Light Snow": 0.0,
        "Snow": 0.0,
        "Heavy Snow": 0.0
    ]

    Map currentConditions = getWeatherFeature("conditions").current_observation

    def sunLevel = weatherMap[currentConditions.weather]
    if (sunLevel == null)
        sunLevel = 0.0

    def nowDate = new Date(now())

    // Is now between sunrise and sunset?
    if ((sunData.sunrise < nowDate) && (nowDate < sunData.sunset)) {
        return sunLevel
    } else {
        return 0.0
    }
}

// snowBookkeeper expects to be called hourly.  It probes and records relevant weather conditions.
def snowBookkeeper() {
    setupInitialConditions()

    def now = now()
    
    // Check if we already have a very recent history entry.  At startup snowBookkeeper() is
    // sometimes called a few times resulting in duplicate entries.
    if (state.history.size() > 0) {
        def age_ms = now - state.history[0].ts
        log.info "History data is ${age_ms}ms old"
        if (age_ms < 60 * 1000) {
            // One minute
            log.info "Skipping data collection."
            return
        }
    }
    
    Map currentConditions = getWeatherFeature("conditions", zipcode)
    if (null == currentConditions.current_observation) {
        log.error "Failed to fetch weather condition data."
        log.debug "conditions data: ${currentConditions}"
        return
    }

    // Pull weather data.
    log.debug "condition data : ${currentConditions.current_observation}"
    // log.debug "precip: ${currentConditions.current_observation.precip_1hr_metric}"
    float precip_mm = currentConditions.current_observation.precip_1hr_metric.toFloat()
    float temp_c = currentConditions.current_observation.temp_c.toFloat()
    def solar_radiation = currentConditions.current_observation.solarradiation
    def uv = currentConditions.current_observation.uv
    def wind_kph = currentConditions.current_observation.wind_kph
    def weather = currentConditions.current_observation.weather

    def sunLevel = getSunLevel()

    // Pull heattape data.
    def powerValue = getHeattapePower()
    def stateValue = getHeattapeState()

    // Make a guess at the amount of snow that has fallen, also track rain.
    float snow_mm = 0.0
    float rain_mm = 0.0
    if (temp_c < 2) {
        // TODO: Is snow mm equal to precip mm?  Does it matter?
        snow_mm = precip_mm.toFloat()
    } else {
        rain_mm = precip_mm.toFloat()
    }

    // Sometimes the weather data indicates snow but doesn't report precipitation.
    // Check for that condition and assume some snow anyway.  The values here are
    // based on https://skybrary.aero/bookshelf/books/943.pdf : 
    // "Heavy snowfall intensity is defined as greater than 2.5 mm/hr equivalent 
    // liquid water precipitation, moderate snow as 1 mm/hr to 2.5 mm/hr, and light
    // snow as less than 1 mm/hr"
    if (snow_mm == 0.0 && weather == "Light Snow") {
        snow_mm = 1.0
    } else if (snow_mm == 0.0 && weather == "Snow") {
        snow_mm = 2.2
    } else if (snow_mm == 0.0 && weather == "Heavy Snow") {
        snow_mm = 3.0
    }

    if (weather == null || snow_mm == null || temp_c == null) {
    	log.error "Critical field is null.  Not adding data to history.  Something is wrong with fetch or parsing."
        log.error "weather : ${weather}"
        log.error "snow_mm : ${snow_mm}"
        log.error "temp_c  : ${temp_c}"
        return
    }

    // Construct history data entry.
    def newWeatherHistory = [ts: now, snow_mm: snow_mm, rain_mm: rain_mm,
        temp_c: temp_c, solar_radiation: solar_radiation, uv: uv, wind_kph: wind_kph,
        state: stateValue, power: powerValue, agg_snow_mm: 0.0, sun_level: sunLevel,
        weather: weather
    ]

    // Push in new history data.
    state.history.add(0, newWeatherHistory)

    // Calculate total snow from history then update new record.
    state.history[0].agg_snow_mm = getSnowDepth()

    // Clean up old history data if we have too much.
    while (state.history.size() > state.MAX_HISTORY) {
        state.history.remove(state.MAX_HISTORY)
    }

    // Re-evalutate situation for control.
    heattapeController()
}

// Return current weather temperature based on most recent reading from history data.
def getTemp() {
	if (! historyExists()) {
        log.debug "getTemp called with no history data.  This should never happen.  Returning 100.0c"
    	return 100.0
    } else {
        return state.history[0].temp_c.toFloat()
    }
}

// Return minimum temperature in the last n recorded slots.
def getMinTemp(records) {
    def mintemp = 999
    // log.debug "getMinTemp(${records}) called."
    if (state.history.size() > 0) {
        records = [records, state.history.size()].min()

        // log.debug "getting records : " + records
        // log.debug "fewer history : " + state.history[0, records - 1]
        state.history[0, records - 1].each() {
        	// Ignore ts=0 records (initial snow).
        	if (it.ts != 0) {
                mintemp = [it.temp_c.toFloat(), mintemp.toFloat()].min()
            }
        }
        return mintemp
    } else {
        return null
    }
}

def isItSnowing() {
    def weather = getWeather()
    if (weather.contains("Snow"))
        return true
    else
        return false
}

def getWeather() {
    if (! historyExists())
    	return "no-data"
    else if (state.history[0].weather == null)
        return "unknown (null weather field in history)"
    else
        return state.history[0].weather
}

// Calculate accumulated depth of snow based on data history.
def getSnowDepth() {
	// No history means no snow.
	if (! historyExists())
    	return 0.0

    float snow_depth = 0.0

    state.history.reverseEach {
        // Add snow!
        if (it.snow_mm != null) {
            // Note this represents the amount of liquid water precipitation, so snow_mm will be
            // less than the actual depth of the snow.  TODO: Clarify variable names so this is 
            // more clear.
            snow_depth += it.snow_mm.toFloat()
        }

        // Melt snow!
        if (it.temp_c > 0) {
            // Based on http://directives.sc.egov.usda.gov/OpenNonWebContent.aspx?content=17753.wba
            // Typical values are from 0.035 to 0.13 inches per degree-day
            // 1.6 to 6.0 mm/degree-day C .. 2.74 mm/degree-day C is often used when other information.
            float mm_melt_per_degreeC_day = 2.74

            // Rain melts snow faster.  How fast?  add melt for rain and temp.  This is kind of made up.
            mm_melt_per_degreeC_day += (it.rain_mm * 0.1778) // 0.1778 comes from a conversion in the usda doc.

            // Calculate heat from sun.  Made up approach.
            mm_melt_per_degreeC_day += it.sun_level * 2.0

            // Calculate melt increase from wind.
            mm_melt_per_degreeC_day += it.wind_kph * 0.008

            // Note: Solid/Liquid freezing point for water doesn't change substantially with altitude.

            // This is a daily melt rate but we calculate in hours so convert.  
            float melt_mm = (mm_melt_per_degreeC_day / 24.0) * it.temp_c.toFloat()
            snow_depth -= melt_mm
		
	     // Log melted snow.
	     log.info "Calculated snow melt : ${melt_mm}mm"
        }

        // No negative snow level.
        // snow_depth = maxFloat(snow_depth, 0.0)
        snow_depth = [snow_depth, 0.0].max()
    }

    return snow_depth.round(3)
}

def maxFloat(float a, float b) {
    if (a > b)
        return a
    else
        return b
}

// Dispatch to appropriate control method based on operation mode. 
def heattapeController() {
    log.info "heattapeController operational mode: ${op_mode}"
    log.info "4 hour minTemp: " + getMinTemp(4)
    if (heattape) {
        heattape.each {
            log.info "Unit ${it.displayName} is ${it.currentValue('switch')} and drawing ${it.currentValue('power')}W"
 		}
    }

    // Note : Double case (string and int) is a work-around for a bug in the simulator.
    switch (op_mode) {
        case "Automatic":
        case 1:
            controlAuto()
            break
        case "TemperatureOnly":
        case 2:
            controlTempOnly()
            break
        case "ForceOn":
        case 3:
            sendHeattapeCommand(true)
            break
        case "ForceOff":
        case 4:
            sendHeattapeCommand(false)
            break
        case "Monitor":
        case 5:
            // Do nothing.
            break
    }
}

// Determine if the temperature range is appropriate to turn on.
def inTempRange() {
	if (! historyExists()) {
    	return false
    }
    
    // Adjust temps based on sun level.  If it's otherwise too cold to turn on, 
    // but sunny (causing melt) turn on anyway.
    def sun_level = getSunLevel()

    // Number of degreesC to adjust when sun is full level.
    def sun_adjust = 5.0

    // Reduce the mintemp allowed by up to sun_adjust degrees if it's sunny.  
    def adjustedMinTemp = minTemp.toFloat() - (sun_adjust * sun_level)

    // Is the current temperate in range?
    if ((getTemp() > adjustedMinTemp) &&
        (getTemp() < maxTemp.toFloat())) {
        return true
    } else {
	// NOTE: This might be useful but may appear as a bug.
	    
        // If we are in an On state and the temp in the last 2 hours is below
        // min (meaning there is ice we've been trying to melt) then stay on.
        if (state.controlOn && getMinTemp(2) <= minTemp.toFloat()) {
            return true
        } else {
            return false
        }
    }
}

// Automatic controller.
def controlAuto() {
    // Weather data often indicates snowing but does not indicate
    // precipitation > 0, so use the snowing indication or precipitation.
    if (isItSnowing() && inTempRange()) {
        log.info "Snowing and in temp range --> ON"
        sendHeattapeCommand(true)
    } else if ((getSnowDepth() > 0) && inTempRange()) {
        log.info "Ground snow and in temp range --> ON"
        sendHeattapeCommand(true)
    } else {
        log.info "No ground snow or falling snow or out of temp range --> OFF"
		sendHeattapeCommand(false)
    }
}

// Temperature Only controller.  Ignores snowfall history.
def controlTempOnly() {
    if (inTempRange()) {
        sendHeattapeCommand(true)
    } else {
        sendHeattapeCommand(false)
    }
}

// Control for the heat tape switches.  True = on; False = off. 
def sendHeattapeCommand(on) {
    // We could track current state and only make a change if necessary, however
    // with unreliability of wireless signals I prefer to have it re-command each
    // hour.

    // Record current state.
    state.controlOn = on
    
    log.debug "sendHeattapeCommand called with request  on:${on}"

    if (heattape) {
        heattape.each {
            if (on) {
                log.info "Turning ON unit : ${it.displayName}"
                it.on()
            } else {
                log.info "Turning OFF unit : ${it.displayName}"
                it.off()
            }
        }
    }
}
