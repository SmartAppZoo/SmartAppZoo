/**
 *  Azure Queues
 *
 *  Copyrigth 2020 Jeff Schnurr
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

definition(
    name: 'azlogger',
    namespace: 'jschnurr',
    author: 'Jeff Schnurr',
    description: 'Smartthings Azure Queue Integration',
    category: 'My Apps',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
    iconX3Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png') {
        appSetting 'StorageAccount'
        appSetting 'Queue'
        appSetting 'SASToken'  // allowed services: Queue, Allowed Resource Types: Object, Allowed Permissions: Add
    }

preferences {
    section('Power Meter') {
        input 'power', 'capability.powerMeter', title: 'Power Sensor', multiple: true, required: false, hideWhenEmpty: true
    }
    section('Environment') {
        input 'thermOperatingStates', 'capability.thermostat', title: 'Therm Operating States', multiple: true, required: false, hideWhenEmpty: true
        input 'temperatures', 'capability.temperatureMeasurement', title: 'Temperature Sensors', multiple: true, required: false, hideWhenEmpty: true
    }
    section('Security') {
        input 'contacts', 'capability.contactSensor', title: 'Contact Sensors', multiple: true, required: false, hideWhenEmpty: true
        input 'motions', 'capability.motionSensor', title: 'Motion Sensors', multiple: true, required: false, hideWhenEmpty: true
        input 'locks', 'capability.lock', title: 'Locks', multiple: true, required: false, hideWhenEmpty: true
    }
    section('Switches') {
        input 'switches', 'capability.switch', title: 'Switches', multiple: true, required: false, hideWhenEmpty: true
        input 'dimmerSwitches', 'capability.switchLevel', title: 'Dimmer Switches', required: false, multiple: true, hideWhenEmpty: true
    }
    section('Log Other Devices') {
        input 'acceleration', 'capability.accelerationSensor', title: 'Acceleration Sensors', multiple: true, required: false, hideWhenEmpty: true
        input 'alarm', 'capability.alarm', title: 'Alarm', required: false, multiple: true, hideWhenEmpty: true
        input 'batteries', 'capability.battery', title: 'Batteries', multiple: true, required: false, hideWhenEmpty: true
        input 'beacon', 'capability.beacon', title: 'Beacon', required: false, multiple: true, hideWhenEmpty: true
        input 'button', 'capability.button', title: 'Buttons', multiple: true, required: false, hideWhenEmpty: true
        input 'colorControl', 'capability.colorControl', title: 'Color Control', multiple: true, required: false, hideWhenEmpty: true
        input 'humidities', 'capability.relativeHumidityMeasurement', title: 'Humidity Sensors', required: false, multiple: true, hideWhenEmpty: true
        input 'illuminances', 'capability.illuminanceMeasurement', title: 'Illuminance Sensors', required: false, multiple: true, hideWhenEmpty: true
        input 'presenceSensors', 'capability.presenceSensor', title: 'Presence Sensors', required: false, multiple: true, hideWhenEmpty: true
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    // Power
    if(power) { subscribe(power, 'power', handlePowerEvent) }

    // Environment
    if(temperatures) { subscribe(temperatures, 'temperature', handleEnvironmentEvent) }
    if(humidities) { subscribe(humidities, 'humidity', handleEnvironmentEvent) }
    if(thermOperatingStates) { subscribe(thermOperatingStates, 'thermostatOperatingState', handleEnvironmentEvent) }

    // Security
    if(contacts) { subscribe(contacts, 'contact', handleSecurityEvent) }
    if(locks) { subscribe(locks, 'lock', handleSecurityEvent) }
    if(motions) { subscribe(motions, 'motion', handleSecurityEvent) }
    if(alarm) { subscribe(alarm, 'alarm', handleSecurityEvent) }

    // Switches
    if(switches) { subscribe(switches, 'switch', handleSwitchEvent) }
    if(dimmerSwitches) { subscribe(dimmerSwitches, 'level', handleSwitchEvent) }
    if(dimmerSwitches) { subscribe(dimmerSwitches, 'switch', handleSwitchEvent) }

    // Other
    if(acceleration) { subscribe(acceleration, 'acceleration', handleOtherEvent) }
    if(batteries) { subscribe(batteries, 'battery', handleOtherEvent) }
    if(beacon) { subscribe(beacon, 'beacon', handleOtherEvent) }
    if(button) { subscribe(button, 'button', handleOtherEvent) }
    if(colorControl) { subscribe(colorControl, 'Color Control', handleOtherEvent) }
    if(illuminances) { subscribe(illuminances, 'illuminance', handleOtherEvent) }
    if(presenceSensors) { subscribe(presenceSensors, 'presence', handleOtherEvent) }
}

def sendEvent(evt, sensorType) {
    def now = new Date().format('yyyyMMdd-HH:mm:ss.SSS', TimeZone.getTimeZone('UTC'))
    def payload = buildEventMessage(evt, sensorType)
    log.debug "Sending AzureQ event payload: ${payload}"
    def encoded = payload.bytes.encodeBase64()
    def params = [
        uri: "https://${appSettings.StorageAccount}.queue.core.windows.net/${appSettings.Queue}/messages${appSettings.SASToken}",
        body: "<QueueMessage><MessageText>${encoded}</MessageText></QueueMessage>",
        contentType: 'application/xml; charset=utf-8',
        requestContentType: 'application/atom+xml;type=entry;charset=utf-8',
        headers: ['x-ms-date': now],
    ]

    try {
        httpPost(params) { resp ->
            log.debug "response message ${resp}"
        }
    } catch (e) {
        // successful creates come back as 200, so filter for 'Created' and throw anything else
        if (e.toString() != 'groovyx.net.http.ResponseParseException: Created') {
            log.error "Error sending event: $e"
            throw e
        }
    }
}

private buildEventMessage(evt, sensorType) {
    def payload = [
        date: evt.isoDate,
        hub: evt.hubId,
        deviceId: evt.deviceId,
        deviceType: sensorType,
        eventId: evt.id,
        device: evt.displayName,
        property: evt.name,
        value: evt.value,
        unit: evt.unit,
        isphysical: evt.isPhysical(),
        isstatechange: evt.isStateChange(),
        source: evt.source,
        location: evt.location.name,
    ]
    def data = new groovy.json.JsonBuilder(payload).toString()
    return data
}

def handlePowerEvent (evt) {
    sendEvent(evt, 'power')
}

def handleEnvironmentEvent (evt) {
    sendEvent(evt, 'environment')
}

def handleSecurityEvent (evt) {
    sendEvent(evt, 'security')
}

def handleSwitchEvent (evt) {
    sendEvent(evt, 'switch')
}

def handleOtherEvent (evt) {
    sendEvent(evt, 'other')
}
