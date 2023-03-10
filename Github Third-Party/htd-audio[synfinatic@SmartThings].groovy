/**
 *  HTD Audio using the HTD (W)GW-SL1
 *
 *  Copyright 2018 Aaron Turner
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
        name: "HTD Audio",
        namespace: "synfinatic",
        author: "Aaron Turner",
        description: "HTD MC-66/MCA-66 via (W)GW-SL1 Smart Gateway",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
       	singleInstance: true,
)

preferences {
    page(name: "config", title: "HTD (W)GW-SL1 Config", uninstall: true, nextPage: "active_page") {
        section() {
            input("ipAddress", "text", multiple: false, required: true, title: "IP Address:", defaultValue: "172.16.1.133")
            input("tcpPort", "integer", multiple: false, required: true, title: "TCP Port:", defaultValue: 10006)
            // Only MC-66 and MCA-66 are supported for now
            input("HTDtype", "enum", multiple: false, required: true, title: "HTD Controller:", options: ['MC-66', 'MCA-66'])
            input("theHub", "hub", multiple: false, required: true, title: "Pair with SmartThings Hub:")
        }
    }
    page(name: "active_page", title: "Select Active Zones and Sources", nextPage: "naming_page")
    page(name: "naming_page", title: "Name Zones and Sources", install: true)
}

def active_page() {
	dynamicPage(name: "active_page") {
        section("Which zones are available?") {
            input("active_zones", "enum", multiple: true, title: "Active Zones", options: controllerZones(HTDtype))
        }
        section("Which input sources are available?") {
            input("active_sources", "enum", multiple: true, title: "Active Sources:", options: controllerSources(HTDtype))
        }
    }
}

def naming_page() {
    dynamicPage(name: "naming_page") {
        section("Name your zones:") {
        	def zones = convertStringList(active_zones)
        	log.debug("My active zones: ${zones.join(',')}")
            for (int i = 1; i <= controllerZones(HTDtype); i++) {
            	if (i in zones) {
                    log.debug("creating active zone: ${i}")
                    input("zone_name.${i}", "text", multiple: false, required: true, title: "Zone ${i}:", defaultValue: "Zone ${i}")
                } else {
                	log.debug("Skipping zone ${i}")
                }
            }
        }
        section("Name your input sources:") {
        	def sources = convertStringList(active_sources)
        	log.debug("My active sources: ${sources.join(',')}")
            for (int i = 1; i <= controllerSources(HTDtype); i++) {
            	if (i in sources) {
                    input "source_name.${i}", "text", multiple: false, required: true, title: "Source ${i}:", defaultValue: "Source ${i}"
                }
            }
        }
    }
}

// Takes a list of Strings, and converts the list to Integers
private convertStringList(list) {
	return list.collect { it.toInteger() }
}

private int controllerZones(controller) {
	switch(controller) {
   		case ["MC-66", "MCA-66", "Lync6"]:
        	return 6
        case "Lync12":
        	return 12
        default:
        	return 0
    }
}

private int controllerSources(controller) {
	switch(controller) {
   		case ["MC-66", "MCA-66"]:
        	return 6
        case "Lync6":
        	return 12
        case "Lync12":
        	return 18
        default:
        	return 0
    }

}

// How many sources does our controller support?
private controllerSources(controller) {
    def ret = []
    def list = 1..controllerSources(controller)
    list.each { n ->
   		ret.add(n)
    }
    return ret
}

// How many zones does our controller support?
private controllerZones(controller) {
    def ret = []
    def list = 1..controllerZones(controller)
    list.each { n ->
   		ret.add(n)
    }
    return ret
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
    // nothing is muted by default
    state.zone_mute = [:]
    for (int i = 0; i <= controllerZones(HTDtype); i++) {
    	state.zone_mute[i] = false
    }
    // all zones default to source = 1.  Hopefully the user enabled it :)
    state.zone_source = [:]
   	for (int i = 0; i <= controllerSources(HTDtype); i++) {
    	state.zone_source[i] = 1
    }
    // remember our active available zones & sources
    state.available_zones = convertStringList(active_zones)
    state.available_sources = convertStringList(active_sources)
   	state.zone_names = []
    state.source_names = []
    state.htd_controller = HTDtype

    def porthex = convertPortToHex(tcpPort)
    def iphex = convertIPtoHex(ipAddress)
    def dni_base = "${iphex}:${porthex}"

    // remember the zone & source names
    for (entry in settings) {
    	log.debug("setting entry: ${entry.getKey()} = ${entry.getValue()}")
    	if (entry.getKey().startsWith("zone_name.")) {
       		def kvpair = entry.getKey().split(/\./)
            log.debug("${HTDtype} zone ${kvpair[1]}: ${entry.getValue()}")
            state.zone_names.putAt(kvpair[1].toInteger(), entry.getValue())
        } else if (entry.getKey().startsWith("source_name.")) {
       		def kvpair = entry.getKey().split(/\./)
            log.debug("${HTDtype} source ${kvpair[1]}: ${entry.getValue()}")
            state.source_names.putAt(kvpair[1].toInteger(), entry.getValue())
        }
    }

    // create the zones
    switch (HTDtype) {
    	case ['MC-66', 'MCA-66']:
            for (int i = 1; i <= 6; i ++) {
                if (i in state.available_zones) {
                    def dni = "${dni_base}:${i}"
                    def existingDevice = getChildDevice(dni)
                    if (!existingDevice) {
                        // Add any enabled zones
                        def dev = addChildDevice("synfinatic", "HTD MC-66 Zone", dni, theHub.id,
                                                [label: "HTD ${state.zone_names[i]}", name: "MC-66Zone.${dni}"])
                        log.info "created ${dev.displayName} with ${dev.deviceNetworkId}"
                    } else {
                        // delete zone if disabled
                        log.info "Deleting disabled zone: ${dni}"
                        deleteChildDevice(dni)
                    }
                }
            }
            break
        default:
        	// Lync isn't supported yet
            log.error("Unable to add any child devices for ${HTDtype}")
    }
	// remove zones which were disabled
    def delete = getChildDevices().findAll { !settings.devices.contains(it.deviceNetworkId) }
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def unsubscribe() {
    getAllChildDevices().each {
        log.debug "deleting child device: ${it.displayName} = ${it.id}"
        deleteChildDevice(it.deviceNetworkId)
    }
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address ${ipAddress} is converted to ${hex}"
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    log.debug "Port ${port} is converted to $hexport"
    return hexport
}


// get the zone_id for a child device
private int get_zone_id(child) {
    def values = child.id.split(':')
    return values[2]
}

/*
 * sends a command to the gateway
 * Sadly, due to a bug in HubAction, you can't read any reply :(
 */
private byte[] send_command(child, command) {
    def values = child.id.split(':')
    def dest = "${values[0]}:${values[1]}"
    /*
     * convert the bytes to ISO-8859-1 which should protect signed values > 7F
     * https://community.smartthings.com/t/sending-raw-tcp-packets/41519/14
     */
    String str = new String(command, "ISO-8859-1")
    new physicalgraph.device.HubAction(str, physicalgraph.device.Protocol.LAN, dest)
}

/*
 * handle commands from the device
 */
def setMute(child, value) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'setMute' for zone ${zone_id}"
    if (value != state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def mute(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'mute' for zone ${zone_id}"
    if (! state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def unmute(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'unmute' for zone ${zone_id}"
    if (state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def setVolume(child, value) {
    zone_id = get_zone_id(child.device.id)
    log.debug "setVolume' does nothing at this time..."
    // we need some way of figuring out what the current volume is (0-60) and then
    // calling volumeUp/Down as appropriate to get to the new value. Maybe is returned
    // by queryZoneState?
}

def volumeUp(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'volumeUp' for zone ${zone_id}"
    send_command(child, _volume_up(zone_id))
}

def volumeDown(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'volumeDown' for zone ${zone_id}"
    send_command(child, _volume_down(zone_id))
}

def setInputSource(child, source_id) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'setInputSource' for zone ${zone_id} => ${source_id}"
    send_command(child, _set_input_channel(zone_id, source_id))
}

def on(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'on' for zone ${zone_id}"
    send_command(child, _power_on(zone_id))
}

def off(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'off' for zone ${zone_id}"
    send_command(child, _power_off(zone_id))
}

def setTrebel(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'up') {
        log.debug "Executing trebel up for zone ${zone_id}"
        send_command(child, _trebel_up(zone_id))
    } else {
        log.debug "Executing trebel down for zone ${zone_id}"
        send_command(child, _trebel_down(zone_id))
    }
}

def setBass(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'up') {
        log.debug "Executing bass up for zone ${zone_id}"
        send_command(child, _bass_up(zone_id))
    } else {
        log.debug "Executing bass down for zone ${zone_id}"
        send_command(child, _bass_down(zone_id))
    }
}

def setBalance(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'left') {
        log.debug "Executing balance left for zone ${zone_id}"
        send_command(child, _balance_left(zone_id))
    } else {
        log.debug "Executing balance right for zone ${zone_id}"
        send_command(child, _balance_rigth(zone_id))
    }
}

def partyMode(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing party mode for zone ${zone_id}"
    send_command(child, _party_mode(zone_id))
}

def queryZoneState(child) {
	// Not really useful right now since we can't do a read :(
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing query zone state for zone ${zone_id}"
    send_command(child, _query_zone_state(zone_id))
}

/*
 * Helper methods to generate the actual 6 byte message sent on the wire
 */

// generates the actual 6 byte command as a string.  x is _almost_ always 0x04
private byte[] command(zone_id, x, y) {
    def cmd = [0x02, 0x0, zone_id, x, y, 0] as byte[]
    // last byte is a "checksum" which is just all the bytes added up
    cmd[5] = cmd.sum()
    return cmd
}

// input channel source (channel)
private byte[] _set_input_channel(zone_id, source_id) {
    def cmd = 0x03 + source_id - 1
    state.zone_source.put(zone_id, source_id)
    return command(zone_id, 0x04, cmd)
}

private byte[] _volume_up(zone_id) {
    return command(zone_id, 0x04, 0x09)
}

private byte[] _volume_down(zone_id) {
    return command(zone_id, 0x04, 0x10)
}

private byte[] _all_power_on() {
    return command(0x01, 0x04, 0x38)
}

private byte[] _all_power_off() {
    return command(0x01, 0x04, 0x39)
}

private byte[] _power_on(zone_id) {
    return command(zone_id, 0x04, 0x20)
}

private byte[] _power_off(zone_id) {
    return command(zone_id, 0x04, 0x21)
}

private byte[] _toggle_mute(zone_id) {
    state.zone_mute.put(zone_id, ! state.zone_mute.get(zone_id))
    return command(zone_id, 0x04, 0x22)
}

private byte[] _bass_up(zone_id) {
    return command(zone_id, 0x04, 0x26)
}

private byte[] _bass_down(zone_id) {
    return command(zone_id, 0x04, 0x27)
}

private byte[] _trebel_up(zone_id) {
    return command(zone_id, 0x04, 0x28)
}

private byte[] _trebel_down(zone_id) {
    return command(zone_id, 0x04, 0x29)
}

private byte[] _balance_right(zone_id) {
    return command(zone_id, 0x04, 0x31)
}

private byte[] _balance_left(zone_id) {
    return command(zone_id, 0x04, 0x32)
}

/*
 * I assume I should read something when this is sent?
 * trying to get a secret decoder ring for the reply from HTD
 */
private byte[] _query_zone_state(zone_id) {
    return command(zone_id, 0x06, 0x00)
}

/*
 * Party mode!  Retrieve the current source for our given
 * zone
 */
private byte[] _party_mode(zone_id) {
    def source_id = state.zone_source.get(zone_id)
    return command(zone_id, 0x04, 0x39 + zone_id + source_id)
}
