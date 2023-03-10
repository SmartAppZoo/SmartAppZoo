/**
 *  Weather Notifications
 *
 *  Copyright 2020 Matt
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
    name: "Weather Notifications",
    namespace: "smartthings",
    author: "Matt",
    description: "Gets the day's weather from the OpenWeather API and sends a formatted push notification to devices on the network.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/App-SevereWeather@2x.png") {
    appSetting "openweather_api_key"
}

preferences {
    section("Choose the time you'd like the weather report to be sent to devices.") {
        input "weather_check_time", "time", required: true, title: "Choose a time to check the weather each day"
    }
    section("Choose the zip code you'd like the weather report for.") {
        input "weather_zip_code", "number", required: true, title: "Enter the zip code you'd like to check the weather for"
    }
}

def installed() {
	schedule(weather_check_time, weatherCheckHandler)
}

def updated() {
	unsubscribe()
    unschedule(weatherCheckHandler)
    schedule(weather_check_time, weatherCheckHandler)
}

def weatherCheckHandler() {
    def params = [
        uri: "https://api.openweathermap.org/data/2.5/",
        path: "forecast",
        contentType: "application/json",
        query: ["zip": weather_zip_code, "appid": appSettings.openweather_api_key, "units": "imperial"]
    ]

    try {
        httpGet(params) { resp ->
        	def temperature_avg = (resp.data.list[0].main.temp + resp.data.list[1].main.temp + resp.data.list[2].main.temp) / 3
            def morning_string = ""
            if (temperature_avg >= 80) {
            	morning_string = "Morning: Hot! "
            } else if (temperature_avg <= 20) {
            	morning_string = "Morning: Cold! "
            } else {
            	morning_string = "Morning: "
            }
        	morning_string = morning_string + "${resp.data.list[0].weather[0].description.capitalize()}, ${Math.round(resp.data.list[0].main.temp)}°"
            def temperature_diff = resp.data.list[0].main.temp - resp.data.list[2].main.temp
           	if (temperature_diff > 15) {
            	morning_string = morning_string + ", falling quickly."
            } else if (temperature_diff < -15) {
            	morning_string = morning_string + ", rising quickly."
            } else {
            	morning_string = morning_string + "."
            }
            def wind_speed = (resp.data.list[0].wind.speed + resp.data.list[1].wind.speed + resp.data.list[2].wind.speed) / 3
            if (wind_speed > 10) {
            	morning_string = morning_string + " Breezy."
            } else if (wind_speed > 15) {
            	morning_string = morning_string + " Windy!"
            }
            
        	temperature_avg = (resp.data.list[2].main.temp + resp.data.list[3].main.temp + resp.data.list[4].main.temp) / 3
            def afternoon_string = ""
            if (temperature_avg >= 80) {
            	afternoon_string = "Afternoon: Hot! "
            } else if (temperature_avg <= 20) {
            	afternoon_string = "Afternoon: Cold! "
            } else {
            	afternoon_string = "Afternoon: "
            }
            afternoon_string = afternoon_string + "${resp.data.list[2].weather[0].description.capitalize()}"
            def afternoon_temp = Math.round(resp.data.list[2].main.temp)
            for (def i=5; i>=3; i--) {
            	if (resp.data.list[i].weather[0].description != resp.data.list[2].weather[0].description) {
                	afternoon_string = afternoon_string + ", then ${resp.data.list[i].weather[0].description}"
                    break
                }
            }
            for (def i=5; i>=3; i--) {
            	if (resp.data.list[i].main.temp > afternoon_temp) {
                	afternoon_temp = Math.round(resp.data.list[i].main.temp)
                }
            }
        	afternoon_string = afternoon_string + ". ${afternoon_temp}°"
            temperature_diff = resp.data.list[2].main.temp - resp.data.list[4].main.temp
           	if (temperature_diff > 15) {
            	afternoon_string = afternoon_string + ", falling quickly."
            } else if (temperature_diff < -15) {
            	afternoon_string = afternoon_string + ", rising quickly."
            } else {
            	afternoon_string = afternoon_string + "."
            }
            wind_speed = (resp.data.list[2].wind.speed + resp.data.list[3].wind.speed + resp.data.list[4].wind.speed) / 3
            if (wind_speed > 10) {
            	afternoon_string = afternoon_string + " Breezy."
            } else if (wind_speed > 15) {
            	afternoon_string = afternoon_string + " Windy!"
            }
            
      		temperature_avg = (resp.data.list[4].main.temp + resp.data.list[5].main.temp + resp.data.list[6].main.temp) / 3
            def evening_string = ""
            if (temperature_avg >= 80) {
            	evening_string = "Evening: Hot! "
            } else if (temperature_avg <= 20) {
            	evening_string = "Evening: Cold! "
            } else {
            	evening_string = "Evening: "
            }
        	evening_string = evening_string + "${resp.data.list[4].weather[0].description.capitalize()}, ${Math.round(resp.data.list[4].main.temp)}°"
            temperature_diff = resp.data.list[4].main.temp - resp.data.list[6].main.temp
           	if (temperature_diff > 15) {
            	evening_string = evening_string + ", falling quickly."
            } else if (temperature_diff < -15) {
            	evening_string = evening_string + ", rising quickly."
            } else {
            	evening_string = evening_string + "."
            }
            wind_speed = (resp.data.list[4].wind.speed + resp.data.list[5].wind.speed + resp.data.list[6].wind.speed) / 3
            if (wind_speed > 10) {
            	evening_string = evening_string + " Breezy."
            } else if (wind_speed > 15) {
            	evening_string = evening_string + " Windy!"
            }
            def weather_string = morning_string + "\n" + afternoon_string + "\n" + evening_string
            sendPush(weather_string)
        }
    } catch (e) {
        log.error "Something went wrong: $e"
    }
}