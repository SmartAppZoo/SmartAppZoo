/**
 *  HTTP Requests
 *
 *  Copyright 2016 Patrick Joyce (Credit goes to: Brian Freund (@brianfreund), Patrick Stuart (@pstuart) and Jason E (jasone) for their code examples I copied plus of course the SmartThings Documentation which helped immensely)
 *
*This is free and unencumbered software released into the public domain.
*
* Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <http://unlicense.org/>
 *
 * Changes made by Patrick Joyce:
 1. Replace all instances of "Android" with "Server"
 2. Changed name, namespace, and author to prevent confusion
 3. Updated description to match planned changes
 4. Changed License from Apache License, version 2.0 to "the Unlicensed"
 5. Change icon URLs 
 */
definition(
name: "RGBW lights",
namespace: "pjoyce42",
author: "Patrick Joyce",
description: "A SmartApp that when paired with  running Automagic and UDP Sender, can control your Milights naively through UDP commands.",
category: "Convenience",
iconUrl: "http://apk-dl.com/detail/image/com.lierda.wifi-w250.png",
iconX2Url: "http://apk-dl.com/detail/image/com.lierda.wifi-w250.png",
iconX3Url: "http://apk-dl.com/detail/image/com.lierda.wifi-w250.png")

preferences {
section("Execute HTTP Request attached to switch") {
    input "theswitch", "capability.switch", required: true, title: "Which lights?"
}
section("Network Information"){
	input("ServerIP", "string", title:"Server IP Address", description: "Please enter your Server's IP Address", defaultValue: "192.168.1.195" , required: true, displayDuringSetup: true)
			input("ServerPort", "string", title:"Server Port", description: "Please enter your Server's Port", defaultValue: 8080 , required: true, displayDuringSetup: true)
    input("PathOn", "string", title:"Path", description: "Enter a path for when the switch turns on", required: false, displayDuringSetup: true)
			input("PathOff", "string", title:"Path", description: "Enter a path for when the switch turns off", required: false, displayDuringSetup: true)
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
	subscribe(theswitch, "switch", switchHandler)
}

def switchHandler(evt) {
	if (evt.value == "on") {
		onSwitches()
	} else if (evt.value == "off") {
		offSwitches()
	}
}

def onSwitches() {
def host = ServerIP
def port = ServerPort
def hosthex = convertIPtoHex(ServerIP)
def porthex = convertPortToHex(ServerPort)
def deviceNetworkId = "$hosthex:$porthex"
def ip = "$ServerIP:$ServerPort"
sendHubCommand(new physicalgraph.device.HubAction("""POST $PathOn HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
log.debug "$ip was sent $PathOn via $deviceNetworkId"
log.debug "host is $host, port is $port, hosthex is $hosthex, porthex is $porthex, deviceNetworkId is     $deviceNetworkId, ip is $ip"
}

def offSwitches() {
def host = ServerIP
def port = ServerPort
def hosthex = convertIPtoHex(ServerIP)
def porthex = convertPortToHex(ServerPort)
def deviceNetworkId = "$hosthex:$porthex"
def ip = "$ServerIP:$ServerPort"
sendHubCommand(new physicalgraph.device.HubAction("""POST $PathOff HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
log.debug "$ip was sent $PathOff via $deviceNetworkId"
log.debug "host is $host, port is $port, hosthex is $hosthex, porthex is $porthex, deviceNetworkId is     $deviceNetworkId, ip is $ip"
}

private String convertIPtoHex(host) { 
String hosthex = host.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
log.debug "the returned host is $hosthex"
return hosthex
}

private String convertPortToHex(port) {
	String porthex = port.toString().format( '%04x', port.toInteger() )
log.debug "the returned port is $porthex"
return porthex
}