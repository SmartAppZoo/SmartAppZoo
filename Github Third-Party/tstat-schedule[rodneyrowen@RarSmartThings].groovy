/**
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
    name: "Tstat Schedule",
    namespace: "rodneyrowen",
    author: "rrowen",
    description: "Tstat Schedule Child",
    category: "My Apps",
    parent: "rodneyrowen:Tstat Master",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences {
    page(name: "Schedule", title: "Set Schedule", install: true, uninstall: true) {
        section("Schedule Name") {
            input(name: "scheduleName", type: "text", title: "Name of this Schedule", required: true)
        }
        section("Set points") {
            input "coolingSetpoint", "number", title: "Cooling Setpoint", required: true
            input "heatingSetpoint", "number", title: "Heating Setpoint", required: true
        }
        section("Priority") {
            input(name: "priority", type: "enum", title: "Priority", required: true, options: ["Inactive","Low","Medium","High"])
        }
        section("Apply Settings When...") {
	        input "sModes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        section("On Which Days") {
            input "days", "enum", title: "Select Days of the Week", required: false, multiple: true, options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday"]
        }
        section("Between what times?") {
            input "fromTime", "time", title: "From", required: false
            input "toTime", "time", title: "To", required: false
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
    def name = settings.scheduleName 
    app.updateLabel("Schedule-${name}") 
    log.debug "Installed with settings: ${settings}"
}

def Integer getHeatingSetpoint() {
    def temp = settings.heatingSetpoint
    temp = temp as Integer
    return temp ? temp : 60
}

def Integer getCoolingSetpoint() {
    def temp = settings.coolingSetpoint
    temp = temp as Integer
    return temp ? temp : 85
}

def Integer convertPriority(strText) {
    def curPriority = settings.priority
    def curValue = 0
    if (curPriority == "High") {
        curValue = 3
    } else if (curPriority == "Medium") {
        curValue = 2
    } else if (curPriority == "Low") {
        curValue = 1
    }

    return curValue
}

def Integer isActive() {

    def state = convertPriority(settings.priority)
    if ( (state > 0) && (settings.sModes) ) {
        // first check in the appropriate mode
        if (!settings.sModes.contains(location.mode)) {
            state = 0
        }
    }

    if ( (state > 0) && (settings.days) ) {
        // Now check days
        def df = new java.text.SimpleDateFormat("EEEE")
        // Ensure the new date object is set to local time zone
        df.setTimeZone(location.timeZone)
        def day = df.format(new Date())
        //Does the preference input Days, i.e., days-of-week, contain today?
        if (!settings.days.contains(day)) {
            state = 0
        }
    }

    if ( (state > 0) && (settings.fromTime) && (settings.toTime) ) {
        // Now check time range
        def between = timeOfDayIsBetween(fromTime, toTime, new Date(), location.timeZone)
        if (!between) {
            state = 0
        }
    }

   log.debug "IsActive(Returns: ${state}) Modes: ${settings.sModes} Days: ${settings.days}"
   return state
}
