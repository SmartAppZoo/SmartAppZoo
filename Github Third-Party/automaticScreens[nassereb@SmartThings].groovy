/**
 *  Automatic Screens
 *
 *  Copyright 2018 Nassere Besseghir
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
    name: "Automatic Screens",
    namespace: "NassereB",
    author: "Nassere Besseghir",
    description: "automatic control for fully optimised sun shading",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "WeatherApiKey"
}

////////////////////////////
// Preferences and Settings
////////////////////////////

preferences {
    page(name: "pageMain")
    page(name: "pageConfigureWindowShade")
    page(name: "pageForecastIO")
}

def pageMain() {
    def thescreen = [
        name:       "z_blinds",
        type:       "capability.windowShade",
        title:      "Which blinds/screens/shutters?",
        multiple:   true,
        submitOnChange: true,
        required:   false
    ]
            
    dynamicPage(name: "pageMain", title: "Configure blinds", install: true, uninstall: true) {
        section("Shades Control", hideable:true) {
			paragraph image:"https://raw.githubusercontent.com/verbem/SmartThingsPublic/master/smartapps/verbem/smart-screens.src/Settings.png", "Configure Window Shades"
            if (thescreen) z_blinds.each { windowShade ->
                href "pageConfigureWindowShade", title:"${windowShade.displayName}", description:"", params: windowShade
            }
        }  
    
        section("Screen", hideable:true) {
            input thescreen
            if (thescreen) {
                z_blinds.each { windowShade ->
                    def devId = windowShade.id
                    // Settings
                    paragraph image: "http://cdn.device-icons.smartthings.com/Weather/weather14-icn@2x.png",
                        title: "Configuration for screen ${windowShade.displayName}",
                        required: true,
                        "WindowShadeState: ${windowShade.currentValue("windowShade")}"
                    input "windowShade_${devId}_orientation", "number", 
                        title: "Orientation", 
                        required: true,
                        defaultValue: 0, 
                        range: "0..360", 
                        description: "Orientation of the blinds relative to true (geographic) north (0 = North, 90 = east, 180 = south, 270 = West)"
                    input "windowShade_${devId}_directionalTolerance", "number", 
                        title: "Directional tolerance", 
                        required: true,
                        defaultValue: 80,
                        range: "0..90"
                    input "windowShade_${devId}_directionalToleranceOtherDirection", "number", 
                        title: "Directional tolerance in the other direction", 
                        required: true,
                        defaultValue: 80,
                        range: "0..90"
                        
                    input "windowShade_${devId}_cloudCover", "number", 
                        title: "Protect until what cloudcover% (0=clear sky)", 
                        range: "0..100", 
                        multiple: false, 
                        required: false, 
                        defaultValue: 30
                        
                    paragraph image: "http://cdn.device-icons.smartthings.com/Weather/weather1-icn@2x.png",
                        title: "Wind protection",
                        required: true,
                        "Beaufort"
                    input "windowShade_${devId}_cloudCover", "enum", 
                        title: "Wind speed", 
                        options: [
                            "0 - Calm",
                            "1 - Light air",
                            "2 - Light breeze",
                            "3 - Gentle breeze",
                            "4 - Moderate breeze", 
                            "5 - Fresch breeze", 
                            "6 - Strong breeze",
                            "7 - High wind, moderate gale, near gale",
                            "8 - Gale, fresh gale",
                            "9 - Strong/severe gale",
                            "10 - Storm, whole gale",
                            "11 - Violent storm",
                            "12 - Hurricane force"]
                }
            }
        }
        
        section("Info Page") {
            href "pageForecastIO", title: "Environment Info", description: "Tap to open"
        }
        
        section("Send Push Notification?") {
            input "sendPush", "bool", required: false, title: "Send Push Notification?"
        }
    }
}

def pageConfigureWindowShade(windowShade) {
    // Workaround: Bug in Dynamic pages with parameters and submitOnChange
    // See: https://community.smartthings.com/t/bug-in-dynamic-pages-with-parameters-and-submitonchange/36936/4
    if (windowShade?.id != null) state.dynamicPageParameter = windowShade.id
    windowShade = z_blinds.findResult { it -> it.id == state.dynamicPageParameter ? it : null }

    def pageProperties = [
        name: "pageForecastIO",
        title: "Configure '${windowShade.displayName}'",
        nextPage: "pageMain",
        uninstall: false
    ]
   
    return dynamicPage(pageProperties) {
        section("Hub Location") {
            paragraph "Latitude: ${location.latitude}"
            paragraph "Longitude: ${location.longitude}"
        }
    }
}

def pageForecastIO() {
    def sunPosition = getSunPosition(location.latitude, location.longitude, new Date())

    def pageProperties = [
        name: "pageForecastIO",
        title: "Current Sun Info",
        nextPage: "pageMain",
        refreshInterval: 10,
        uninstall: false
    ]

    return dynamicPage(pageProperties) {
        def temperaturScale = location.getTemperatureScale() // Either “F” for fahrenheit or “C” for celsius

        section("Hub Location") {
            paragraph "Latitude: ${location.latitude}"
            paragraph "Longitude: ${location.longitude}"
        }
        section("Sun Info") {
            paragraph "Sunrise: ${getSunriseAndSunset().sunrise}"
            paragraph "Sunset: ${getSunriseAndSunset().sunset}"
            paragraph "Azimuth: ${sunPosition.azimuth}"
            paragraph "Altitude: ${sunPosition.altitude}"
        }
        section("Current Weather conditions") {
	        def currentObservation = getWeatherFeature("conditions")?.current_observation
            paragraph "Temperature: ${temperaturScale == "F" ? currentObservation.temp_f : currentObservation.temp_c}"
            paragraph "Weather: ${currentObservation.weather ?: "-"}"
            paragraph "Icon: ${currentObservation.icon} - ${currentObservation.icon_url}"
            paragraph "Windspeed: ${temperaturScale == "F" ? currentObservation.wind_mph : currentObservation.wind_kph}"
            paragraph "Wind direction: ${currentObservation.wind_dir} ${currentObservation.wind_degrees}°"
            paragraph "Wind degrees: ${currentObservation.wind_degrees}"
            paragraph "UV: ${currentObservation.UV}"
        }
        section("Weather forecast") {
            def forecast = getWeatherFeature("forecast")?.forecast.simpleforecast.forecastday[0]
            paragraph "High: ${temperaturScale == "F" ? forecast.high.fahrenheit : forecast.high.celsius}"
            paragraph "Weather: ${forecast.conditions ?: "-"}"
        }
    }
}

//////////////////////
// Pre-defined callbacks
//////////////////////

def installed() {
    log.debug "'Automatic Screens' smartapp installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "'Automatic Screens' smartapp updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "'Automatic Screens' smartapp initializing"
    
    resetState()

    subscribe(location, "sunset", sunsetHandler)
    subscribe(location, "sunrise", sunriseHandler)
    
    subscribe(z_blinds, "windowShade", windowShadeHandler)
    
    if(timeOfDayIsBetween(getSunriseAndSunset().sunrise, getSunriseAndSunset().sunset, new Date(), location.timeZone)) {  
        runEvery10Minutes(checkForSunHandler)
    }
    
    log.debug "'Automatic Screens' smartapp initialized"
}

//////////////////////
// Event handlers
//////////////////////

def sunsetHandler(evt) {
    resetState()
    runEvery10Minutes(checkForSunHandler)
}

def sunriseHandler(evt) {
    unschedule(checkForSunHandler)
}

def checkForSunHandler() {
    log.debug "checkForSunHandler called at ${new Date()}"
    def sunPosition = getSunPosition(location.latitude, location.longitude, new Date())
    
    z_blinds.each { windowShade ->
        log.debug "Checking action for windowShade '${windowShade.displayName}'"
        if(isAngleBetweenRange(sunPosition.azimuth, getShadingRangeSetting(windowShade.id))) {
            // Execute 'Close' command
            log.debug "Sun matches direction of windowShade '${windowShade.displayName}'"
            if(state.windowShadeStates[windowShade.id] != "Closed") {
                log.debug "execution 'close' command for windowShade '${windowShade.displayName}'"
                windowShade.close()
                state.windowShadeStates[windowShade.id] = "Closed"
                if (sendPush) { sendPush("Closed ${windowShade.displayName}!") }
            }
        } else {
            log.debug "WindowShade '${windowShade.displayName}' does not need protection. No sun."
        }  
    }
}

def windowShadeHandler(evt) {
    log.debug "WindowShade event occured: value => '${evt.value}'"
}

def getShadingRangeSetting(deviceId) {
    return [
        start: settings."windowShade_${deviceId}_orientation" - settings."windowShade_${deviceId}_directionalToleranceOtherDirection",
        end: settings."windowShade_${deviceId}_orientation" + settings."windowShade_${deviceId}_directionalTolerance"
    ]
}

//////////////////////
// State
//////////////////////
def resetState() {
    state.windowShadeStates = [:]
    settings.z_blinds.each { windowShade ->
        state.windowShadeStates[windowShade.id] = "Empty"
    }
}

def isAngleBetweenRange(value, range) {
    def n = (360 + (value % 360)) % 360
    def a = (3600000 + range.start) % 360
    def b = (3600000 + range.end) % 360

    if (a < b) 
        return a <= n && n <= b
    return a <= n || n <= b
}

/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Azimuth and Alitude for current Time
/*-----------------------------------------------------------------------------------------*/
def getSunPosition(latitude, logitude, currentDateTime) {
    def lw = rad() * -location.longitude
    def phi = rad() * location.latitude
    def d = toDays(currentDateTime)
    def c = sunCoords(d)
    def H = siderealTime(d, lw) - c.ra

    def az = azimuth(H, phi, c.dec)
    az = (az * 180 / Math.PI) + 180
    def al = altitude(H, phi, c.dec)
    al = al * 180 / Math.PI

    return [
        azimuth: az,
        altitude: al
    ]
}
/*-----------------------------------------------------------------------------------------*/
/*	Return the Julian date 
/*-----------------------------------------------------------------------------------------*/
def toJulian(date) {
    return date.getTime() / dayMs() - 0.5 + J1970() // ms time/ms in a day = days - 0.5 + number of days 1970.... 
}
/*-----------------------------------------------------------------------------------------*/
/*	Return the number of days since J2000
/*-----------------------------------------------------------------------------------------*/
def toDays(date) {
    return toJulian(date) - J2000()
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun RA
/*-----------------------------------------------------------------------------------------*/
def rightAscension(l, b) {
    return Math.atan2(Math.sin(l) * Math.cos(e()) - Math.tan(b) * Math.sin(e()), Math.cos(l))
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Declination
/*-----------------------------------------------------------------------------------------*/
def declination(l, b) {
    return Math.asin(Math.sin(b) * Math.cos(e()) + Math.cos(b) * Math.sin(e()) * Math.sin(l))
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Azimuth
/*-----------------------------------------------------------------------------------------*/
def azimuth(H, phi, dec) {
    return Math.atan2(Math.sin(H), Math.cos(H) * Math.sin(phi) - Math.tan(dec) * Math.cos(phi))
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Altitude
/*-----------------------------------------------------------------------------------------*/
def altitude(H, phi, dec) {
    return Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(H))
}
/*-----------------------------------------------------------------------------------------*/
/*	compute sidereal time (One sidereal day corresponds to the time taken for the Earth to rotate once with respect to the stars and lasts approximately 23 h 56 min.
/*-----------------------------------------------------------------------------------------*/
def siderealTime(d, lw) {
    return rad() * (280.16 + 360.9856235 * d) - lw
}
/*-----------------------------------------------------------------------------------------*/
/*	Compute Sun Mean Anomaly
/*-----------------------------------------------------------------------------------------*/
def solarMeanAnomaly(d) {
    return rad() * (357.5291 + 0.98560028 * d)
}
/*-----------------------------------------------------------------------------------------*/
/*	Compute Sun Ecliptic Longitude
/*-----------------------------------------------------------------------------------------*/
def eclipticLongitude(M) {
    def C = rad() * (1.9148 * Math.sin(M) + 0.02 * Math.sin(2 * M) + 0.0003 * Math.sin(3 * M))
    def P = rad() * 102.9372 // perihelion of the Earth
    return M + C + P + Math.PI
}
/*-----------------------------------------------------------------------------------------*/
/*	Return Sun Coordinates
/*-----------------------------------------------------------------------------------------*/
def sunCoords(d) {
    def M = solarMeanAnomaly(d)
    def L = eclipticLongitude(M)
    return [dec: declination(L, 0), ra: rightAscension(L, 0)]
}
/*-----------------------------------------------------------------------------------------*/
/*	Some auxilliary routines for readabulity in the code
/*-----------------------------------------------------------------------------------------*/
def dayMs() {
    return 1000 * 60 * 60 * 24
}
def J1970() {
    return 2440588
}
def J2000() {
    return 2451545
}
def rad() {
    return Math.PI / 180
}
def e() {
    return rad() * 23.4397
}
