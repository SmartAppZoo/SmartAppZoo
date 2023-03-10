/**
 *  Monitor
 *
 *  Copyright 2016 Charlie Gorichanaz
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
import groovy.time.TimeCategory

definition(
    name: "Monitor",
    namespace: "CNG",
    author: "Charlie Gorichanaz",
    description: "Allow external service to access thing data.",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true,
)

preferences {
  section ("Sensor access") {
    input "sensorsTemperature",
        "capability.temperatureMeasurement",
        title: "Temperature",
        multiple: true,
        required: false
    input "sensorsHumidity",
        "capability.relativeHumidityMeasurement",
        title: "Humidity",
        multiple: true,
        required: false
    input "sensorsEnergy",
        "capability.energyMeter",
        title: "Energy",
        multiple: true,
        required: false
    input "sensorsPower",
        "capability.powerMeter",
        title: "Power",
        multiple: true,
        required: false
    input "sensorsIlluminance",
        "capability.illuminanceMeasurement",
        title: "Illuminance",
        multiple: true,
        required: false
    input "sensorsMotion",
        "capability.motionSensor",
        title: "Motion",
        multiple: true,
        required: false
  }
  section ("Thermostat access") {
    input "thermostats",
        "capability.thermostat",
        title: "Thermostat",
        multiple: true,
        required: false
  }
  section ("Switch access") {
    input "switches",
        "capability.switch",
        title: "Switch",
        multiple: true,
        required: false
    input "switchesLevel",
        "capability.switchLevel",
        title: "Switch level",
        multiple: true,
        required: false
  }
}

mappings {
    path("/endpoint") {
        action: [
            GET: "handlerURL"
        ]
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
}

def updated() {
    log.debug "Updated with settings: ${settings}"
}

/**
 * Return $type things
 *
 * TODO: Is it valid to combine things like I'm doing when "all" is passed in? Check 
 * if the attributes, etc., are dependent on the capability used in the selector.
 */
def getThings(kind){
    switch(kind) {
        case "all":
            // Add all things
            // Must test each since selecting no things causes variable to be null,
            // and adding null to a list causes an error.
            def things = []
            things += sensorsTemperature?: []
            things += sensorsHumidity   ?: []
            things += sensorsEnergy     ?: []
            things += sensorsPower      ?: []
            things += sensorsIlluminance?: []
            things += sensorsMotion     ?: []
            things += thermostats       ?: []
            things += switches          ?: []
            things += switchesLevel     ?: []
            // remove things that were selected in multiple sections
            def thingComparator = [
                equals: { delegate.equals(it) },
                compare: { first, second ->
                    first.id <=> second.id
                }
            ] as Comparator
            return things.unique(thingComparator)
        case "temperature":
            return sensorsTemperature?: []
        case "humidity":
            return sensorsHumidity   ?: []
        case "energy":
            return sensorsEnergy     ?: []
        case "power":
            return sensorsPower      ?: []
        case "illuminance":
            return sensorsIlluminance?: []
        case "motion":
            return sensorsMotion     ?: []
        case "thermostat":
            return thermostats       ?: []
        case "switch":
            return switches          ?: []
        case "switchLevel":
            return switchesLevel     ?: []
    }
    log.debug( /getThings() called with invalid kind "$kind"/ )
}

/**
 * Return thing with .id $id
 */
def getThing(id){
    getThings("all").findResult {
        it.id == id ? it : null
    }
}

/**
 * Direct request based on $params.function
 */
def handlerURL() {
    switch(params.function) {
        case "things":
            return handlerThings()
        case "events":
            return handlerEvents()
        case "states":
            return handlerStates()
        default:
            log.debug( $/handlerURL() received invalid $$params.function "$params.function"/$ )
            return []
    }
}

/**
 * List .id and .displayName for each $params.type things
 */
def handlerThings() {
    def things = getThings(params.kind)
    def resp = []
    things.each {
        def capabilities = []
        it.capabilities.each {
            def attributes = it.attributes.collect([]) {
                name: it.name
            }
            def commands = it.commands.collect([]) {
                name: it.name
            }
            capabilities << [
                name:       it.name,
                attributes: attributes,
                commands:   commands,
            ]
        }
        resp << [
            id:           it.id,
            name:         it.name,
            label:        it.label,
            capabilities: capabilities,
        ]
    }
    return resp
}


/**
 * Retrieve last $params.max events from sensor with ID $params.id
 */
def handlerEvents() {
    getThing(params.id).events(max: params.max).collect([]) {[
        id:          it.id.toString(),
        date:        it.date,
        deviceId:    it.deviceId,
        name:        it.name,
        unit:        it.unit,
        stringValue: it.stringValue,
    ]}
}


/**
 * Retrieve up to $params.max $params.state states since $params.since from sensor with ID $params.thingID
 * 
 * since is seconds since epoch as float or int, defaults to 7 days ago
 * max defaults to 1000
 */
def handlerStates(){
    if(!params.state || !params.thing_id){
        // exit early since required params not set
        log.debug "handlerStates() exiting early"
        return []
    }
    def since
    log.debug "handlerStates().params.since: ${params.since}"
    if(params.since){
        since = new Date(Math.round(params.since.toFloat() * 1000))
    } else {
        since = new Date() -7
    }
    log.debug "handlerStates().since: ${since}"
    def max = params.max ? params.max.toInteger() : 1000
    max = 0 < max && max < 1000 ? max : 1000
    log.debug "handlerStates().max: ${max}"
    getThing(params.thing_id).statesSince(params.state, since, [max: max]).collect([]) {[
        state: params.state,
        date:  it.date,
        value: it.value,
    ]}
}

