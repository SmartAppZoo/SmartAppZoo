/**
 *  Smart Bathroom Fan
 *
 *  Copyright 2018 Chris Roberts
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
    name: "Smart Bathroom Fan III",
    namespace: "m0ntecarloss",
    author: "Chris Roberts",
    description: "Work in progress.  Based on AirCycler switch with enhancements...",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

import java.text.SimpleDateFormat
import groovy.time.TimeCategory

preferences {

    // TODO: cleanup sections, descriptions, pages and stuff...
    // TODO: outdoor temp sensor / dew point type stuff
    
    page(name:"mainPage")
    page(name:"hardwarePage")
    page(name:"controlPage")
    page(name:"schedulePage")

}

def mainPage() {
    dynamicPage(name:"mainPage", uninstall: true, install: true) {

        if(state.installed_and_configured) {
        
            section(hideable: true, "Stats and Junk") {

                String stats_string = ""
                
                stats_string += "  In the last 24 hours\n"
                stats_string += "      Last refreshed:   ${state.last_24_refresh_time}\n"
                stats_string += "      Start time:       ${state.last_24_start_time}\n"
                stats_string += "      # On Events:      ${state.last_24_on_events}\n"
                stats_string += "      # Off Events:     ${state.last_24_off_events}\n"
                stats_string += "      Longest on min:   ${state.last_24_longest_on_min}\n"
                stats_string += "      Longest on start: ${state.last_24_longest_on_start}\n"
                stats_string += "      Longest on end:   ${state.last_24_longest_on_end}\n"
                stats_string += "      Total runtime:    ${state.last_24_runtime_minutes}\n"
                stats_string += "      Average min/hr:   ${state.last_24_average}\n"
                stats_string += "\n"
                stats_string += "  In the last 3 days\n"
                stats_string += "      Last refreshed:   ${state.last_3_days_refresh_time}\n"
                stats_string += "      Start time:       ${state.last_3_days_start_time}\n"
                stats_string += "      # On Events:      ${state.last_3_days_on_events}\n"
                stats_string += "      # Off Events:     ${state.last_3_days_off_events}\n"
                stats_string += "      Longest on min:   ${state.last_3_days_longest_on_min}\n"
                stats_string += "      Longest on start: ${state.last_3_days_longest_on_start}\n"
                stats_string += "      Longest on end:   ${state.last_3_days_longest_on_end}\n"
                stats_string += "      Total runtime:    ${state.last_3_days_runtime_minutes}\n"
                stats_string += "      Average min/hr:   ${state.last_3_days_average}\n"

                paragraph "${stats_string}"
            }
        }
    
        section(hideable: true, "Debug") {
            input "debugEnabled", "bool", title: "Debug Enabled", required: true
        }
        
        section(hideable: true, "Hardware") {
            
            // bath fan switch to control smartly...
            input "fanSwitch",   "capability.switch", title: "Fan Switch", required: true
            
            // bath light switch to tie fan operation to
            input "lightSwitch", "capability.switch", title: "Light Switch", required: true
            
            // humidity sensor to override normal operation
            input "humiditySensor", "capability.relativeHumidityMeasurement", title: "Humidity Sensor", required: false
        
            // motion sensor to turn on fan and/or light
            input "motionSensor", "capability.motionSensor", title: "Motion Sensor", required: false
        
            // contact sensors to turn on the fan
            input "contactSensors", "capability.contactSensor", title: "Door/Window Sensors", required: false, multiple: true
        }
        
        section(hideable: true, "Controls or something") {
    
            input "fanMinPerHour", "number", title: "Hourly Budget", description: "How many minutes per hour to run fan for", required: true
        
            input "minAfterLightOn", "number", title: "Minutes After Light On", description: "How many minutes after light turned on before fan turns on", required: false
     
            input "minAfterLightOff", "number", title: "Minutes After Light Off", description: "How many minutes after light turned off before fan turns off", required: false
        }
        
        section(hideable: true, "Scheduling and stuff") {
            paragraph "This section will be implemented when I feel like it"
        }
    
    }
}


//------------------------------------------------------------------------------

def installed() {
	DEBUG("installed")
    state.installed_and_configured = true
	initialize()
}

//------------------------------------------------------------------------------

def updated() {
    DEBUG("updated")
	initialize()
}

//------------------------------------------------------------------------------

def initialize() {

    String debug_string = "initialize:\n"
    
    if(settings.fanMinPerHour <= 0) {
        settings.fanMinPerHour = 0
        debug_string += "Invalid fanMinPerHour.  Using ${settings.fanMinPerHour}\n"
    }
    if(maxFanRuntime <= 0) {
        settings.maxFanRuntime = 60
        debug_string += "Invalid maxFanRuntime.  Using ${settings.maxFanRuntime}\n"
    }
    if(minFanRuntime <= 0) {
        settings.minFanRuntime = 0
        debug_string += "Invalid minFanRuntime.  Using ${settings.minFanRuntime}\n"
    }
    debug_string += "settings = ${settings}\n"
 

    unschedule()
    schedule("58 58 * * * ?", hourlyHandler)

    unsubscribe()
    subscribe (lightSwitch,    "switch.on",      lightHandler)
    subscribe (lightSwitch,    "switch.off",     lightHandler)
    subscribe (fanSwitch,      "switch.on",      fanOnHandler)
    subscribe (fanSwitch,      "switch.off",     fanOffHandler)
    subscribe (contactSensors, "contact.open",   contactOpenHandler)
	subscribe (contactSensors, "contact.closed", contactCloseHandler)
  
    def fanSwitchCommands = fanSwitch.supportedCommands
    log.debug "fanSwitchCommands: ${fanSwitchCommands}"
   
    DEBUG(debug_string)
    
    dumps()
    runEvery5Minutes(dumps)
}

//------------------------------------------------------------------------------

def lightHandler(evt) {
    String debug_string = "lightHandler:\n"
    
    try {
    } catch (e) {
    }

    //DEBUG(debug_string)
}

//------------------------------------------------------------------------------

def fanOnHandler(evt) {
    String debug_string = "fanOnHandler:\n"
    
    try {
    } catch (e) {
    }

    //DEBUG(debug_string)
}

//------------------------------------------------------------------------------

def fanOffHandler(evt) {
    String debug_string = "fanOffHandler:\n"
    
    try {
    } catch (e) {
    }

    //DEBUG(debug_string)
}

//------------------------------------------------------------------------------

def contactOpenHandler(evt) {
    String debug_string = "contactOpenHandler:\n"
    
    try {
    } catch (e) {
    }

    //DEBUG(debug_string)
}

//------------------------------------------------------------------------------

def contactCloseHandler(evt) {
    String debug_string = "contactCloseHandler:\n"
    try {
    } catch (e) {
    }

    //DEBUG(debug_string)
}

//------------------------------------------------------------------------------

def hourlyHandler() {
    
    String debug_string = "hourlyHandler:\n"
   
    try {
    } catch(e) {
    }

    //DEBUG(debug_string)
}
           
//------------------------------------------------------------------------------

def dumps() {
    oldSchoolDump_7days()
    oldSchoolDump24()
}

def oldSchoolDump24() {
    
    String  debug_string     = ""
    Integer total_total_secs = 0
    Integer total_secs       = 0
    Integer total_mins       = 0
    Integer counter          = 0
    
    debug_string += "\n"
    debug_string += "-------------------------------------\n"
    debug_string += "---- Old School Stuff - 24 hours ----\n"
    debug_string += "-------------------------------------\n"
    
    state.last_24_refresh_time     = "never"
    state.last_24_on_events        = 0
    state.last_24_off_events       = 0
    state.last_24_longest_on_min   = 0.0
    state.last_24_longest_on_start = ""
    state.last_24_longest_on_end   = ""
    state.last_24_runtime_minutes  = 0.0
    state.last_24_average          = 0.0

    try {
        def     cur          = new Date()
        def     day_ago      = new Date()
        use(TimeCategory) {
            day_ago = day_ago - 24.hour - 1.hour
        }
        def last_event       = day_ago
        def last_event_value = "" // 

        //def df = new java.text.SimpleDateFormat("EEE MMM dd 'at' hh:mm:ss a")
        def df = new java.text.SimpleDateFormat("MMM dd 'at' hh:mm:ss a")
        df.setTimeZone(location.timeZone)
        
        state.last_24_refresh_time = "${df.format(cur)}"
        state.last_24_start_time   = "${df.format(day_ago)}"
        
        //debug_string += "  current time = ${cur}\n"
        debug_string += "  current time = ${df.format(cur)}\n"
        //debug_string += "  last hour    = ${day_ago}\n"
        debug_string += "  last day     = ${df.format(day_ago)}\n"

        for(zzz in fanSwitch.statesSince("switch", day_ago, [max: 1000]).reverse()) {
            counter += 1
           
            debug_string += "   STATE: ${counter} - ${zzz.value} @ ${df.format(zzz.date)}\n"
            //debug_string += "     date            = ${zzz.date}\n"
            //debug_string += "     date            = ${df.format(zzz.date)}\n"
            //debug_string += "     name            = ${zzz.name}\n"
            //debug_string += "     device          = ${zzz.device.displayName}\n"
            //debug_string += "     description     = ${zzz.description}\n"
            //debug_string += "     descriptionText = ${zzz.descriptionText}\n"
            //debug_string += "     state_change    = ${zzz.isStateChange()}\n"
            //debug_string += "     physical        = ${zzz.isPhysical()}\n"
            //debug_string += "     value           = ${zzz.value}\n"
            //debug_string += "     last value      = ${last_event_value}\n"
            //debug_string += "     source          = ${zzz.source}\n"
            
            if(zzz.value == "off" && last_event_value == "on") {
                state.last_24_off_events = state.last_24_off_events + 1
                if(last_event_value == zzz.value) {
                    null
                    //debug_string += "     Last event was off so not counting...\n"
                } else {
                    def seconds_since_last_mark = (zzz.date.getTime() - last_event.getTime()) / 1000
                    total_total_secs += seconds_since_last_mark
                    
                    if( (seconds_since_last_mark / 60.0) > state.last_24_longest_on_min ) {
                        state.last_24_longest_on_min   = seconds_since_last_mark / 60.0
                        state.last_24_longest_on_start = "${counter} ${df.format(last_event)}"
                        state.last_24_longest_on_end   = "${counter} ${df.format(zzz.date)}"
                    }
                    
                    debug_string += "     seconds since last = ${seconds_since_last_mark}\n"
                    //debug_string += "     Total secs         = ${total_total_secs}\n"
                }
            } else if(zzz.value == "on") {
                state.last_24_on_events = state.last_24_on_events + 1
                last_event              = zzz.date
                last_event_value        = zzz.value
            }
        }
        
        total_mins = total_total_secs / 60
        total_secs = total_total_secs - (total_mins * 60)
   
        state.last_24_runtime_minutes = total_total_secs / 60.0
        state.last_24_average         = state.last_24_runtime_minutes / (1 * 24.0)
    
        debug_string += "Total fan runtime last 24 hours: ${counter} events for total of ${total_mins} mins and ${total_secs} secs\n"
        debug_string += "  --> average is ${state.last_24_average}\n"
    
    } catch(e) {
        debug_string += "DARN.  Unhandled exception in oldSchool24:\n${e}\n"
    }
  
    lightSwitch.refresh()
    fanSwitch.refresh()
   
    DEBUG(debug_string)
}

def oldSchoolDump_7days() {
    
    String  debug_string     = ""
    Integer total_total_secs = 0
    Integer total_secs       = 0
    Integer total_mins       = 0
    Integer counter          = 0
    
    debug_string += "\n"
    debug_string += "-------------------------------------\n"
    debug_string += "---- Old School Stuff - 3 Days ----\n"
    debug_string += "-------------------------------------\n"
    
    state.last_3_days_refresh_time     = "never"
    state.last_3_days_on_events        = 0
    state.last_3_days_off_events       = 0
    state.last_3_days_longest_on_min   = 0.0
    state.last_3_days_longest_on_start = ""
    state.last_3_days_longest_on_end   = ""
    state.last_3_days_runtime_minutes  = 0.0
    state.last_3_days_average          = 0.0

    try {
        def     cur            = new Date()
        def     three_days_ago = new Date()
        use(TimeCategory) {
            three_days_ago = cur - 3.day
        }
        def last_event       = three_days_ago
        def last_event_value = ""

        //def df = new java.text.SimpleDateFormat("EEE MMM dd 'at' hh:mm:ss a")
        def df = new java.text.SimpleDateFormat("MMM dd 'at' hh:mm:ss a")
        df.setTimeZone(location.timeZone)
        
        state.last_3_days_refresh_time = "${df.format(cur)}"
        state.last_3_days_start_time   = "${df.format(three_days_ago)}"
        
        debug_string += "  current time     = ${df.format(cur)}\n"
        debug_string += "  three days ago   = ${df.format(three_days_ago)}\n"

        for(zzz in fanSwitch.statesSince("switch", three_days_ago, [max: 1000]).reverse()) {
            counter += 1
          
            if(zzz.value == "off") {
           
                if(counter == 1) {
                    debug_string += "  first event(off) = ${df.format(zzz.date)}\n"
                }
            
                state.last_3_days_off_events = state.last_3_days_off_events + 1
                if(last_event_value == zzz.value) {
                    null
                } else {
                    def seconds_since_last_mark = (zzz.date.getTime() - last_event.getTime()) / 1000
                    total_total_secs += seconds_since_last_mark
                    
                    if( (seconds_since_last_mark / 60.0) > state.last_3_days_longest_on_min ) {
                        state.last_3_days_longest_on_min  = seconds_since_last_mark / 60.0
                        state.last_3_days_longest_on_date  = "${df.format(zzz.date)}"
                        state.last_3_days_longest_on_start = "${counter} ${df.format(last_event)}"
                        state.last_3_days_longest_on_end   = "${counter} ${df.format(zzz.date)}"
                    }
                }
            } else if(zzz.value == "on") {
                if(counter == 1) {
                    debug_string += "  first event(on)  = ${df.format(zzz.date)}\n"
                }
                state.last_3_days_on_events = state.last_3_days_on_events + 1
                last_event                  = zzz.date
                last_event_value            = zzz.value
            }
        }
        
        total_mins = total_total_secs / 60
        total_secs = total_total_secs - (total_mins * 60)
   
        state.last_3_days_runtime_minutes = total_total_secs / 60.0
        state.last_3_days_average         = state.last_3_days_runtime_minutes / (3 * 24.0)
    
        debug_string += "Total fan runtime last 3 days: ${counter} events for total of ${total_mins} mins and ${total_secs} secs\n"
        debug_string += "  --> average is ${state.last_3_days_average}\n"
    
    } catch(e) {
        debug_string += "DARN.  Unhandled exception in oldSchoolDump_7days:\n${e}\n"
    }
  
    DEBUG(debug_string)
}

           
//------------------------------------------------------------------------------

private def DEBUG(txt) {
    //log.debug ${app.label}
    log.debug(txt)
    if( debugEnabled ) {
        sendNotificationEvent("SMF3: " + txt)
    }
}

//------------------------------------------------------------------------------

/*
def OLDhourlyHandler(evt) {
    
    // TODO: This whole thing is no good.  We don't want to check after an hour is over to see
    //       how much time the fan SHOULD have been on in that hour.
    //
    //       What really needs to happen is we need to schedule the check
    //       at the beginning of the hour at the latest possible time
    //       we should turn the fan on in that hour.  Then each time
    //       the fan turns off we should unschedule/reschedule accordingly
    
    def debug_string = new String()
    def counter      = 0
    def cur          = new Date()
    def hour_ago     = new Date()
    def total_secs   = 0
    use(TimeCategory) {
        hour_ago = hour_ago - 1.hour
    }
    def last_event  = hour_ago
    
    debug_string += "--------------------------\n"
    debug_string += "current time = ${cur}\n"
    debug_string += "last hour    = ${hour_ago}\n"
    
    // TODO: need to check for and handle repeated events in the
    //       logic (i.e. two off events in a row for whatever reason)
    for(zzz in fanSwitch.eventsSince(hour_ago).reverse()) {
        if(zzz.value == "on" || zzz.value == "off") {
            counter += 1
            debug_string += "--------------------------\n"
            debug_string += "EVENT: ${counter}\n"
            debug_string += "       date            = ${zzz.date}\n"
            //debug_string += "       name            = ${zzz.name}\n"
            debug_string += "       device          = ${zzz.device.displayName}\n"
            debug_string += "       description     = ${zzz.description}\n"
            //debug_string += "       descriptionText = ${zzz.descriptionText}\n"
            debug_string += "       state_change    = ${zzz.isStateChange()}\n"
            //debug_string += "       physical        = ${zzz.isPhysical()}\n"
            debug_string += "       value           = ${zzz.value}\n"
            debug_string += "       source          = ${zzz.source}\n"
          
            if(zzz.value == "off") {
                def seconds_since_last_mark = (zzz.date.getTime() - last_event.getTime()) / 1000
                total_secs += seconds_since_last_mark
                debug_string += "       seconds since l = ${seconds_since_last_mark}\n"
            }
            
            last_event = zzz.date
        }
    }
    // If fan switch is still on, then reset the last on time to now since we've already
    // addressed this past hours runtime
    if(fanSwitch.currentSwitch == "on") {
        def fanRuntimeSeconds = (now() - state.last_fan_on_time) / 1000.0
        state.total_fan_runtime_this_hour_in_seconds = state.total_fan_runtime_this_hour_in_seconds + fanRuntimeSeconds
        
        debug_string += "Fan switch is still on so resetting last on time to now..."
        state.last_fan_on_time = now()
    } else {
        debug_string += "Fan switch is off.  No need to mess with last fan on time..."
    }
    
    debug_string += "TOTAL ON TIME: ${total_secs}\n"
    
    DEBUG("hourlyHandler:\n ${debug_string}")
}
*/
