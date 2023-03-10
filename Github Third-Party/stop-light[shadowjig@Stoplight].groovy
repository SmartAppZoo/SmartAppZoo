/**
 *  Stop Light
 *
 *  Copyright 2018 shadowjig
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
    name: "Stop Light",
    namespace: "shadowjig",
    author: "shadowjig",
    description: "Light for young children to indicate if it's time to get up.  Create a stop light using a schedule and an RGB bulb.",
    category: "Family",
    iconUrl: "https://github.com/shadowjig/Stoplight/raw/master/traffic_light.png",
    iconX2Url: "https://github.com/shadowjig/Stoplight/raw/master/traffic_light@2x.png",
    iconX3Url: "https://github.com/shadowjig/Stoplight/raw/master/traffic_light@2x.png")

preferences {
    section() {
        input "thebulb", "capability.colorControl", required: true, title: "Select RGB bulb:"
    }
    section ("Scheduling:") {
    }
    section ("Weekday Schedule (Mon-Fri):") {
        input "greenstartTimeWeekday", "time", title: "Green Start Time (Weekdays)", required: true
        input "redstartTimeWeekday", "time", title: "Red Start Time (Weekdays)", required: true
    }
    section ("Weekend Schedule (Sat-Sun):") {
        input "greenstartTimeWeekend", "time", title: "Green Start Time (Weekends)", required: true
        input "redstartTimeWeekend", "time", title: "Red Start Time (Weekends)", required: true
    }
    section ("Attributes of green:") {
        input "greenLevel", "number", title: "Green Brightness Level (1-100)", required: true, range: "1..100", defaultValue: "30"
        input "greenHue", "number", title: "Green Hue (0-100)", required: true, range: "0..100", defaultValue: "33"
        input "greenSaturation", "number", title: "Green Saturation (0-100)", required: true, range: "0..100", defaultValue: "100"
    }
    section ("Attributes of red:") {
        input "redLevel", "number", title: "Red Brightness Level (1-100)", required: true, range: "1..100", defaultValue: "30"
        input "redHue", "number", title: "Red Hue (0-100)", required: true, range: "0..100", defaultValue: "0"
        input "redSaturation", "number", title: "Red Saturation (0-100)", required: true, range: "0..100", defaultValue: "100"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    unschedule()
    initialize()
}

def initialize() {
    //Setup Weekday Schedules
    schedule("0 ${getMinutes(greenstartTimeWeekday)} ${getHour(greenstartTimeWeekday)} ? * MON,TUE,WED,THU,FRI *", greenWeekdayCallback)
    schedule("0 ${getMinutes(redstartTimeWeekday)} ${getHour(redstartTimeWeekday)} ? * MON,TUE,WED,THU,FRI *", redWeekdayCallback)
    
    //Setup Weekend Schedules
    schedule("0 ${getMinutes(greenstartTimeWeekend)} ${getHour(greenstartTimeWeekend)} ? * SAT,SUN *", greenWeekendCallback)
    schedule("0 ${getMinutes(redstartTimeWeekend)} ${getHour(redstartTimeWeekend)} ? * SAT,SUN *", redWeekendCallback)
}

def getHour(value) {
    def tz = location.timeZone
    def schedTime = timeToday(value,tz)
    def hour = schedTime.format("H",tz)
    return hour
}

def getMinutes(value) {
    def tz = location.timeZone
    def schedTime = timeToday(value,tz)
    def min = schedTime.format("m",tz)
    return min
}

def redWeekdayCallback() {
    red()
}

def greenWeekdayCallback() {
    green()
}

def redWeekendCallback() {
    red()
}

def greenWeekendCallback() {
    green()
}

def red() {
    thebulb.setHue(redHue)  //red color 0
    thebulb.setLevel(redLevel)
    thebulb.setSaturation(redSaturation)
    thebulb.on()
}

def green() {
    thebulb.setHue(greenHue)  //green color 33
    thebulb.setLevel(greenLevel)
    thebulb.setSaturation(greenSaturation)
    thebulb.on()
}