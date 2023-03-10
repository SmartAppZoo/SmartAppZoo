/**
 *	SmartLib
 *
 *	Copyright 2014 Oliver Ratzesberger
 *
 *	Common functions Library for SMartThings Apps and device drivers
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
definition(
		name: "SmartLib",
		namespace: "fxstein",
		author: "Oliver Ratzesberger",
		description: "SmartLib extension library for SmartThings",
		category: "SmartThings Labs",
		iconUrl: "https://raw.githubusercontent.com/fxstein/smartthings-smartlib/master/smartlib.png",
		iconX2Url: "https://raw.githubusercontent.com/fxstein/smartthings-smartlib/master/smartlib@2x.png")


preferences {
	section("Title") {
		// TODO: put inputs here
	}
}

def installed() {
	slLog("info", "Installed with settings: ${settings}")

	initialize()
}

def updated() {
	slLog("info", "Updated with settings: ${settings}")

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(app, slEventHandler)
	subscribe(location, slEventHandler)
}

// Default event handler to help debug event behavior
def slEventHandler(event) {
	slLog("debug", "event=$event&value=${event.value}&properties=" + event.getProperties())
}


//
// IP Address conversion helpers
//

// Unfortunately needed helpers to work with HubAction - Thanks to @pstuart
public String slConvertIPtoHex(ipAddress) {
	// Must do 2 digit hex padding or 10.x.x.x addresses will not work
	String hexIp = ipAddress.tokenize( '.' ).collect {	String.format( '%02x', it.toInteger() ) }.join()
	log.debug "IP address entered is $ipAddress and the converted hexIp code is $hexIp"
	return hexIp
}

public String slConvertPortToHex(port) {
	// Must do 4 digit hex padding or 2 digit ports like 80 will not work
	String hexPort = port.toString().format( '%04x', port.toInteger() )
	log.debug "Port entered is $port and the converted hex code is $hexPort"
	return hexPort
}

// Take IP:Port format and convert to correspondig HEX format for the ST Hub
public String slConvertAddrToHex(deviceAddress) {

	def parts = deviceAddress.split(":")
	def iphex = convertIPtoHex(parts[0])
		// TODO: Should add handling for addresses without port specified and default to e.g. 80
	def porthex = convertPortToHex(parts[1])

	def hexAddr = "$iphex:$porthex"
	log.debug "Device address entered is $deviceAddress and the converted hexAddr code is $hexAddr"
	return hexAddr
}

//
// Advanced application level logging
//

public slLog(level, message) {
	def location = getLocation()
	def timeNow = now();
	String tags = "&level=$level&settings=$settings&time=$timeNow"
	String logMessage = "$message$tags"

	log.trace "app=$app&properties=" + app.getProperties()
	log.trace "appparent=${app.parent}&properties=" + app.parent.getProperties()
	log.trace "location=$location&properties=" + location.getProperties()
	log.trace "state=$state&properties=" + state.getProperties()
	log.trace "settings=$settings&properties=" + settings.getProperties()

  // TODO: Add external realtime logging capability

  switch (level) {
    case "info":
      log.info logMessage
      break
    case "trace":
      log.trace logMessage
      break
    case "warn":
      log.warn logMessage
      break
    case "error":
      log.error logMessage
      break
    default:
      log.debug logMessage
  }
}
