/**
 *  Copyright 2016 Mark Grimes
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
 *  Based on Keep Me Cozy II by SmartThings
 *
 */

definition(
    name: "Multi-Sensor Thermostat",
    namespace: "mvgrimes",
    author: "mgrimes@cpan.org",
    description: "Use multiple sensors to run thermostat. Use the average, minimum or maximum of multiple sensors.",
    category: "Green Living",
    version: "2.6",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo@2x.png"
)

preferences() {
    page(name: "page1", title: "Select thermostat and heat/cool settings", nextPage: "page2", uninstall: true){
        section("Choose thermostat... ") {
            input "thermostat", "capability.thermostat"
            input "threshold", "decimal", title: "Theshold (default: 1)", defaultValue: 1
        }
        section("Target Temps..." ) {
            input "targetControl", "capability.thermostat"
        }
        section("Heat setting..." ) {
            input "heatingSetpoint", "decimal", title: "Degrees"
            input "heatingFunction", "enum", title: "Combine via (default: min)",
            required: true, options: [ "ave", "min", "max" ], defaultValue: "min"
        }
        section("Air conditioning setting...") {
            input "coolingSetpoint", "decimal", title: "Degrees"
            input "coolingFunction", "enum", title: "Combine via (default: max)",
            required: true, options: [ "ave", "min", "max" ], defaultValue: "max"
        }
        section("Optionally choose temperature sensors to use instead of the thermostat's... ") {
            input "sensors", "capability.temperatureMeasurement", title: "Temp Sensors", multiple: true, required: false
        }
    }
    page(name: "page2", title: "Presence sensors", nextPage: "page3", install: false, uninstall: false )
    page(name: "page3", title: "Name app and configure modes", install: true, uninstall: true){
        section {
            label title: "Assign a name", required: false
            input "modes", "mode", title: "Set for specific mode(s)", multiple: true, required: false
        }
    }
}

def page2(){
    dynamicPage(name: "page2", install: false, uninstall: false ){
        section("Optionally choose presence sensors to toggle temperature sensors...") {
            sensors.each{
                input "presenceSensorFor$it", "capability.switch",
                    title: "Presence Sensor for $it", multiple: false, required: false
            }
        }
    }
}

def installed() {
    log.debug "enter installed, state: $state"
    updateSetpoints()
    subscribeToEvents()

    // check on a regular interval (5 minutes), events just don't seem to be consistent enough
    runEvery5Minutes( publicEvaluate )
}

def updated() {
    log.debug "enter updated, state: $state"
    unsubscribe()
    unschedule()
    installed()
}

def subscribeToEvents() {
    subscribe(location, changedLocationMode)
    sensors.each{ subscribe(it, "temperature", temperatureHandler) }
    subscribe(thermostat, "temperature", temperatureHandler)
    subscribe(thermostat, "thermostatMode", temperatureHandler)
    sensors.each{
        if( settings["presenceSensorFor$it"] ){
            log.debug( "Subscribing to presenceSensorFor${it}" )
            subscribe(settings["presenceSensorFor$it"], "switch", temperatureHandler)
        }
    }
    // Subscribe to these just so we can re-evaluate
    if( targetControl ){
      subscribe( targetControl, "coolingSetpoint", coolingSetpointHandler )
      subscribe( targetControl, "heatingSetpoint", heatingSetpointHandler )
    }
    evaluate()
}

def changedLocationMode(evt) {
    log.debug "changedLocationMode: mode change to $evt.value"
    evaluate()
}

def temperatureHandler(evt) {
    evaluate()
}

def coolingSetpointHandler(evt){
    // log.debug "coolingSetpointHandler: $evt.value"
    // state.coolingSetpoint = evt.value.toFloat()
    // sendPush( "Set cooling target to $evt.value" )
    evaluate()
}

def heatingSetpointHandler(evt){
    // log.debug "heatingSetpointHandler: $evt.value"
    // state.heatingSetpoint = evt.value.toFloat()
    // sendPush( "Set heat target to $evt.value" )
    evaluate()
}

private updateSetpoints(){
    if( targetControl ){
      state.heatingSetpoint = targetControl.currentValue("heatingSetpoint")
      state.coolingSetpoint = targetControl.currentValue("coolingSetpoint")
    }
    if( state.heatingSetpoint == null ) state.heatingSetpoint = settings.heatingSetpoint
    if( state.coolingSetpoint == null ) state.coolingSetpoint = settings.coolingSetpoint
}

def publicEvaluate(){
  sensors.each{
    if( it.hasCapability("Polling") ){
      log.debug("polling ${it}")
      it.poll()
    } else if( it.hasCapability("Refresh") ){
      log.debug("refreshing ${it}")
      it.refresh()
    }
  }
  evaluate()
}

private evaluate() {
    log.trace("executing evaluate()")

    log.debug("location mode: ${location.mode}");
    log.debug("settings: $settings");
    if( modes && ! modes.contains(location.mode) ) return;

    // Let's get the target setpoints each time we evaluate
    updateSetpoints()

    // If there are no sensors, then just adjust the thermostat's setpoints
    if(! sensors){
        log.info( "setPoints( ${state.coolingSetpoint} - ${state.heatingSetpoint} ), no sensors" )
        thermostat.setHeatingSetpoint(state.heatingSetpoint)
        thermostat.setCoolingSetpoint(state.coolingSetpoint)
        thermostat.poll()
        return
    }

    def tstatMode = thermostat.currentThermostatMode
    def tstatTemp = thermostat.currentTemperature

    // Debugging current status
    log.debug("therm [${thermostat}]: mode=$tstatMode temp=$tstatTemp heat=$thermostat.currentHeatingSetpoint cool=$thermostat.currentCoolingSetpoint")
    sensors.each{
        if( settings["presenceSensorFor$it"] ){
          log.debug( "sensor [${it}]: temp=${it.currentTemperature} presenceSensor=${settings["presenceSensorFor$it"]} presence=${settings["presenceSensorFor$it"].currentSwitch}")
        } else {
          log.debug( "sensor [${it}]: temp=${it.currentTemperature} presenceSensor=${settings["presenceSensorFor$it"]}")
        }
    }

    def temps = sensors.findResults {
        def presenceSensor = settings["presenceSensorFor$it"]
        ( !presenceSensor  || presenceSensor.currentSwitch == "on" ) ? it.currentTemperature : null
    }
    log.debug("temps= $temps");

    if (tstatMode in ["cool","auto"]) {     // air conditioner
        def virtualTemp = evaluateCooling( tstatTemp, temps )
        if( targetControl ){
          targetControl.setTemperature( virtualTemp )
          targetControl.setCombiningFunc( coolingFunction )
          targetControl.setThermostatMode( tstatMode )
          targetControl.setOperatingState( "cooling" )
        }
    }

    if (tstatMode in ["heat","emergency heat","auto"]) {  // heater
        def virtualTemp = evaluateHeating( tstatTemp, temps )
        if( targetControl ){
          targetControl.setTemperature( virtualTemp )
          targetControl.setCombiningFunc( heatingFunction )
          targetControl.setThermostatMode( tstatMode )
          targetControl.setOperatingState( "heating" )
        }
    }
}

private evaluateCooling( Float tstatTemp, List temps ){
    def virtTemp = calcTemperature( coolingFunction, temps )
    log.debug( "target=${state.coolingSetpoint} combine=${coolingFunction} virtTemp=${virtTemp}" )

    if (virtTemp - state.coolingSetpoint >= threshold) {        // virtTemp > target
        // Consider:
        // Set it at the target, or 2 degrees lower than current, but never less than 5 degrees lower than target
        // thermostat.setCoolingSetpoint( max( min( state.coolingSetpoint, tstatTemp - 2 ), state.coolingSetpoint - 5 ) )
        thermostat.setCoolingSetpoint(tstatTemp - 2)
        log.debug( "thermostat.setCoolingSetpoint(${tstatTemp - 2}), ON" )
    }
    else if (state.coolingSetpoint - virtTemp >= threshold      // virtTemp < taget, and
        && tstatTemp - thermostat.currentCoolingSetpoint >= 0 ) // thermTemp > thermSetPoint (ie, running)
    {
        thermostat.setCoolingSetpoint(tstatTemp + 2)
        log.debug( "thermostat.setCoolingSetpoint(${tstatTemp + 2}), OFF" )
    }

    return virtTemp
}

private evaluateHeating( Float tstatTemp, List temps ){
    def virtTemp = calcTemperature( heatingFunction, temps )
    log.debug( "target=${state.heatingSetpoint} combine=${heatingFunction} virtTemp=${virtTemp}" )

    if (state.heatingSetpoint - virtTemp >= threshold) {        // virtTemp < target
        // Consider:
        // Set it at the target, or 2 degrees higher than current, but never more than 5 degrees higher than target
        // thermostat.setHeatingSetpoint( min( max( state.heatingSetpoint, tstatTemp + 2 ), state.heatingSetpoint + 5 ) )
        thermostat.setHeatingSetpoint(tstatTemp + 2)
        log.debug( "thermostat.setHeatingSetpoint(${tstatTemp + 2}), ON" )
    }
    else if (virtTemp - state.heatingSetpoint >= threshold      // virtTemp > target, and
        && thermostat.currentHeatingSetpoint - tstatTemp >= 0)  // termTemp < termSetPoint (ie, running)
    {
        thermostat.setHeatingSetpoint(tstatTemp - 2)
        log.debug( "thermostat.setHeatingSetpoint(${tstatTemp - 2}), OFF" )
    }

    return virtTemp
}

private calcTemperature( String func, List temps ){
    def calcTemp
    switch( func ){
        case "ave":
            calcTemp = temps.sum() / temps.size()
            break
        case "max":
            calcTemp = temps.max()
            break
        case "min":
            calcTemp = temps.min()
            break
        default:
            log.error( "bad function: ${func}" )
    }

    return calcTemp
}
