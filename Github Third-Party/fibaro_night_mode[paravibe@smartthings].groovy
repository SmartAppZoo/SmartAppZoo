/**
 *  Copyright 2018 paravibe
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
 *  Fibaro Night Mode
 *
 *  Author: Max Petyurenko
 *
 *  Works only with https://github.com/codersaur/SmartThings/blob/master/devices/fibaro-dimmer-2/fibaro-dimmer-2.groovy
 */

definition(
    name: "Fibaro Night Mode",
    namespace: "Fibaro",
    author: "Max Petyurenko",
    description: "Enables/disables night mode for Fibaro Dimmer 2 during specified period.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Lighting/light14-icn@2x.png",
)

preferences {
  section('Control this Fibaro Dimmer 2') {
    input 'fibaroDimmer2Devices', 'capability.switch', multiple: true, required: true
  }

  section('Starting') {
    input 'sunsetOffset', 'number', title: 'Minutes after sunset', range: '-1140..1140', defaultValue: 0, required: true
    input 'startTime', 'time', title: 'or from this time', required: false
    input 'startMode', 'mode', title: 'or when mode is activated', multiple: false, required: false
  }

  section('Ending') {
    input 'sunriseOffset', 'number', title: 'Minutes after sunrise', range: '-1140..1140', defaultValue: 0, required: true
    input 'endTime', 'time', title: 'or till this time', required: false
    input 'endMode', 'mode', title: 'or when mode is activated', multiple: false, required: false
  }

  section('Set night mode brightness level') {
    input 'reducedBrightness', 'number', title: 'Brightness level', description: '1-100%', range: '1..100', required: true
  }
}

/**
 * {@inheritdoc}
 */
def installed() {
  initialize()
}

/**
 * {@inheritdoc}
 */
def updated() {
  initialize()
}

/**
 * Changes Night Mode on mode change.
 *
 * @param evt
 *   Event object.
 */
def modeChangeNightMode(evt) {
  if (location.mode == startMode) {
    enableNightMode()
  }
  else if (location.mode == endMode) {
    disableNightMode()
  }
}

/**
 * Sunset event handler.
 */
def sunsetHandler(evt) {
  scheduleEnableNightMode(evt.value)
}

/**
 * Sunrise event handler.
 */
def sunriseHandler(evt) {
  scheduleDisableNightMode(evt.value)
}

/**
 * Initializes app. Sets all needed schedules or subscriptions.
 */
private initialize() {
  unschedule()
  unsubscribe()

  if (fibaroDimmer2Devices) {
    // Enable night mode.
    if (startMode) {
      log.debug "Scheduling to set ${reducedBrightness}% of brightness when ${startMode} activated"
      subscribe(location, "mode", modeChangeNightMode)
    }
    else if (startTime) {
      log.debug "Scheduling to set ${reducedBrightness}% of brightness at ${startTime}"
      schedule(startTime, 'enableNightMode')
    }
    else {
      log.debug "Scheduling to set ${reducedBrightness}% of brightness at ${sunsetOffset} mins after sunset"
      subscribe(location, 'sunsetTime', sunsetHandler)
      scheduleEnableNightMode(location.currentValue('sunsetTime'))
    }

    // Disable night mode.
    if (endMode) {
      log.debug "Scheduling to disable night mode when ${endMode} activated"
      subscribe(location, "mode", modeChangeNightMode)
    }
    else if (endTime) {
      log.debug "Scheduling to disable night mode at ${endTime}"
      schedule(endTime, 'disableNightMode')
    }
    else {
      log.debug "Scheduling to disable night mode at ${sunriseOffset} mins after sunrise"
      subscribe(location, 'sunriseTime', sunriseHandler)
      scheduleDisableNightMode(location.currentValue('sunriseTime'))
    }
  }
}

/**
 * Schedules Night Mode to be on at sunset.
 *
 * @param sunsetString
 *   Sunset date string.
 */
private scheduleEnableNightMode(sunsetString) {
  def timeAfterSunset = calculateDateOffset(sunsetString)

  log.debug "Scheduling for: ${timeAfterSunset} (sunset is ${sunsetString})"

  runOnce(timeAfterSunset, enableNightMode)
}

/**
 * Schedules Night Mode to be off at sunrise.
 *
 * @param sunriseString
 *   Sunrise date string.
 */
private scheduleDisableNightMode(sunriseString) {
  def timeAfterSunrise = calculateDateOffset(sunriseString)

  log.debug "Scheduling for: ${timeAfterSunrise} (sunrise is ${sunriseString})"

  runOnce(timeAfterSunrise, disableNightMode)
}

/**
 * Helper function that parses date string to date object.
 *
 * @param dateString
 *   Date string.
 *
 * @return object
 *   Date object.
 */
private calculateDateOffset(dateString) {
  def dateObject = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", dateString)

  return new Date(dateObject.time + (sunsetOffset * 60 * 1000))
}

/**
 * Sets dimmer Night Mode to on.
 */
private enableNightMode() {
  if (fibaroDimmer2Devices) {
    log.debug 'Enable night mode for Fibaro Dimmer 2 devices'
    fibaroDimmer2Devices*.enableNightmode(reducedBrightness)
  }
}

/**
 * Sets Dimmer Night Mode to off.
 */
private disableNightMode() {
  if (fibaroDimmer2Devices) {
    log.debug 'Disable night mode for Fibaro Dimmer 2 devices'
    fibaroDimmer2Devices*.disableNightmode()
  }
}
