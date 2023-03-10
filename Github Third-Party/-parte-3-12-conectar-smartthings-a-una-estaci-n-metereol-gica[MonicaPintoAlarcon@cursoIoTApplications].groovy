/**
 *  Weather Underground PWS Connect
 *
 *  Copyright 2015 Andrew Mager
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
 
import java.text.DecimalFormat

// ID = IMLAGA24
// KEY = zyjjqshr
// C = (F-32)/1.8
// F = C*1.8 + 32
definition(
    name: "(Parte 3) 12. Conectar SmartThings a una Estación Metereológica",
    namespace: "cursoIoTApplications",
    author: "Variación de Andrew Mager por Monica Pinto",
    description: "Connect your SmartSense Temp/Humidity sensor to your Weather Underground Personal Weather Station.",
    category: "Green Living",
    iconUrl: "http://i.imgur.com/HU0ANBp.png",
    iconX2Url: "http://i.imgur.com/HU0ANBp.png",
    iconX3Url: "http://i.imgur.com/HU0ANBp.png",
    oauth: true)


preferences {
    section("Select a sensor") {
        input "temp", "capability.temperatureMeasurement", title: "Temperature", required: true
        input "humidity", "capability.relativeHumidityMeasurement", title: "Humidity", required: false
    }
    section("Configure your Weather Underground credentials") {
        input "weatherID", "text", title: "Weather Station ID", defaultValue: "IANDALUC230", required: false
        input "password", "password", title: "Weather Underground password", defaultValue: "csnkm3d9", required: false

    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}


def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}


def initialize() {
    
    runEvery10Minutes(updateCurrentWeather)
}


def updateCurrentWeather() {
    
    log.info "$location.temperatureScale"
    log.trace "Temp: " + temp?.currentTemperature
    log.trace "Humidity: " + humidity?.currentHumidity
    //Dew Point : Punto de rocío, calculado en función de la temperatura y humedad actual
    //El punto de rocío o temperatura de rocío es la temperatura a la que empieza a condensarse 
    //el vapor de agua contenido en el aire, produciendo rocío, neblina, cualquier tipo de nube o, 
    //en caso de que la temperatura sea lo suficientemente baja, escarcha.
    log.trace "Dew Point: " + calculateDewPoint(temp.currentTemperature, humidity.currentHumidity)

def params = [
        uri: "http://wunderground.com",
        path: "/weatherstation/updateweatherstation.php",
        query: [
            "ID": weatherID,
            "PASSWORD": password,
            "dateutc": "now",
            "tempf": temp.currentTemperature,
            "humidity": humidity?.currentHumidity,
            "dewptf": calculateDewPoint(temp.currentTemperature, humidity.currentHumidity),
            "action": "updateraw",
            "softwaretype": "SmartThings"
        ]
    ]
    
    if (temp.currentTemperature) {
        try {
        	//sendPush("temp = ${temp.currentTemperature}")
            httpGet(params) { resp ->   
                log.debug "response data: ${resp.data}"
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }

}

//Función para el cálculo del punto de rocío
def calculateDewPoint(t, rh) {
    def dp = 243.04 * ( Math.log(rh / 100) + ( (17.625 * t) / (243.04 + t) ) ) / (17.625 - Math.log(rh / 100) - ( (17.625 * t) / (243.04 + t) ) ) 
    return new DecimalFormat("##.##").format(dp)
}