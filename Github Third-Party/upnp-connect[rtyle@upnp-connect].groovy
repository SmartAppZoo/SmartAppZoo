// vim: ts=4:sw=4
/**
 *	UPnP (Connect)
 *
 *	Copyright 2019 Ross Tyler
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
**/

private static String namespace() {
	'rtyle'
}
private static String name() {
	'UPnP (Connect)'
}

definition(
	namespace	: namespace(),
	name		: name(),
	author		: 'Ross Tyler',
	description	: 'UPnP connect to hub-local devices',
	category	: 'Convenience',
	iconUrl		: "https://raw.githubusercontent.com/${namespace()}/upnp-connect/master/smartapps/${namespace()}/upnp-connect.src/app.png",
	iconX2Url	: "https://raw.githubusercontent.com/${namespace()}/upnp-connect/master/smartapps/${namespace()}/upnp-connect.src/app@2x.png",
	iconX3Url	: "https://raw.githubusercontent.com/${namespace()}/upnp-connect/master/smartapps/${namespace()}/upnp-connect.src/app@3x.png",
)

private int getTrace() {0}
private int getDebug() {1}
private int getInfo () {2}
private int getWarn () {3}
private int getError() {4}
private void log(int level, String message) {
	if (level > logLevel) {
		log."${['trace', 'debug', 'info', 'warn', 'error'][level]}" message
	}
}

private String getNamespace() {
	namespace()
}
private String getName() {
	name()
}
private String getUpnpNamespace() {
	'schemas-upnp-org:device'
}

private Map getSupportedCandidateTypeMap() {
	Map map = [:]
    // make sure urn values are of type String (not GString) because we will be comparing them to objects of type String
    // while String and GString objects may compare (compareTo, ==) the same, they will not be equal
    // Apparently the contains method on a collection evaluates using equal, not == or compareTo!
	['BinaryLight', 'DimmableLight'].each {name ->
		map."$namespace\t$name" = [urn: "$upnpNamespace:$name:1".toString(), deviceHandler: {notUsed -> [namespace: namespace, name: "UPnP $name"]}]
	}
	String name = 'Denon AVR'; map."$namespace\t$name" = [urn: "$upnpNamespace:MediaRenderer:1".toString(), deviceHandler: {device ->
		if ('Denon' == device.manufacturer.text())
			[namespace: namespace, name: "UPnP $name"]
		else
			null
	}]
	map
}

private Map getRequestedCandidateTypeMap() {
	supportedCandidateTypeMap.findAll{requestedCandidateTypes?.contains(it.key)}
}

private HashSet getRequestedCandidateUrns() {
	requestedCandidateTypeMap.collect([] as HashSet){it.value.urn}
}

private Map resolveDeviceHandler(String urn, device) {
	Map deviceHandler = null
	supportedCandidateTypeMap.find {
		urn == it.value.urn && (deviceHandler = it.value.deviceHandler(device))
	}
	deviceHandler
}

private Integer decodeHexadecimal(String hexadecimal) {
	Integer.parseInt hexadecimal, 16
}
private String decodeNetworkAddress(String networkAddress) {
	[
		decodeHexadecimal(networkAddress[0..1]),
		decodeHexadecimal(networkAddress[2..3]),
		decodeHexadecimal(networkAddress[4..5]),
		decodeHexadecimal(networkAddress[6..7]),
	].join('.')
}
private Integer decodeDeviceAddress(String deviceAddress) {
	decodeHexadecimal deviceAddress
}

private Map getRememberedDevice() {
	if (!state.rememberedDevice) {
		state.rememberedDevice = [:]
	}
	state.rememberedDevice
}

private Map decodeMap(String serialization, String major = '\\s*,\\s*', String minor = '\\s*:\\s*') {
	Map map = [:]
	serialization.split(major).each {
		def a = it.split(minor, 2)
		map << [(a[0]) : a[1]]
	}
	map;
}

void ssdpPathResponse(physicalgraph.device.HubResponse hubResponse) {
	def message = parseLanMessage(hubResponse.description)
	// log debug, "ssdpPathResponse: $message.body"
	def xml = parseXml(message.body)
	def device = xml.device
	String udn = decodeMap(device.UDN.text()).uuid
	log debug, "ssdpPathResponse: udn=$udn"

	if (rememberedDevice."$udn") {
		// complete what is remembered about this UDN identified device with the serializable ssdpPath response
		rememberedDevice."$udn".ssdpPathResponse = hubResponse.description
	}
}

private void ssdpDiscovered(physicalgraph.app.EventWrapper e) {
	def discovered = parseLanMessage e.description
	discovered.ssdpUSN = decodeMap discovered.ssdpUSN, '\\s*::\\s*'
	discovered.ssdpTerm = decodeMap discovered.ssdpTerm
	discovered.hubId = e.hubId
	log debug, "ssdpDiscovered: $discovered"

	String udn = discovered.ssdpUSN.uuid
	physicalgraph.app.DeviceWrapper udnChild = getChildDevice udn
	if (udnChild) {
		// SmartThings delivers events from a UPnP device
		// to the SmartThings device identified by the MAC address of the UPnP device
		// that the hub received the event from.
		// UPnP identifies its devices with a UDN and there may be many UPnP devices supported at a MAC address.
		// We create a child for each MAC address and one for each UDN.
		// The UDN identified devices handle all UPnP communication directly except they cannot handle event reception.
		// Instead, they encode their UDN and notify method in the SUBSCRIBE CALLBACK header.
		// The MAC identified devices' parse method will decode this from the HTTP request,
		// and notify the UDN identified device.

		// create a different MAC identified child, if needed
		String mac = discovered.mac
		physicalgraph.app.DeviceWrapper macChild = getChildDevice mac
		if (!macChild) {
			String hubId = discovered.hubId
			String label = name + ' ' + mac
			log info, "ssdpDiscovered: addChildDevice $namespace, $name, $mac, $hubId, [label: $label, completedSetup: true]"
			addChildDevice namespace, name, mac, hubId, [label: label, completedSetup: true]
		}

		// update UDN identified child with discovered networkAddress and deviceAddress
		log debug, "ssdpDiscovered: (getChildDevice $udn).update $discovered.networkAddress $discovered.deviceAddress"
		udnChild.update discovered.networkAddress, discovered.deviceAddress
	} else {
		String urn = discovered.ssdpUSN.urn
		if (!search) {
			log debug, "ssdpDiscovered: $urn ignored, not searching"
        } else if (!(requestedCandidateUrns.contains(urn))) {
			log debug, "ssdpDiscovered: $urn ignored, not one of $requestedCandidateUrns"
		} else {
			// remember what we have discovered so far and try get an ssdpPath response
			rememberedDevice."$udn" = discovered;
			String target = decodeNetworkAddress(discovered.networkAddress) + ':' + decodeDeviceAddress(discovered.deviceAddress)
			log debug, "ssdpDiscovered: GET http://$target${discovered.ssdpPath}"
			sendHubCommand new physicalgraph.device.HubAction(
				"GET ${discovered.ssdpPath} HTTP/1.1\r\nHOST: $target\r\n\r\n",
				physicalgraph.device.Protocol.LAN,
				target,
				[callback: ssdpPathResponse],
			)
		}
	}
}

void ssdpDiscover() {
	List hubActions = [];
	requestedCandidateUrns.each {urn ->
		log debug, "ssdpDiscover: hubAction lan discover urn:${urn}"
		hubActions.add new physicalgraph.device.HubAction('lan discovery urn:' + urn, physicalgraph.device.Protocol.LAN)
	}
	sendHubCommand hubActions, 4000 // perform hubActions with a delay between them
}

private void ssdpSubscribe() {
	requestedCandidateUrns.each {urn ->
		// subscribe to event by name (ssdpTerm) and (dot) value (urn:$urn)
		log debug, "ssdpSubscribe: subscribe ssdpTerm.urn:$urn"
		subscribe location, 'ssdpTerm' + '.urn:' + urn, 'ssdpDiscovered'
	}
}

void updated() {
	log debug, "updated: settings=$settings"
	unsubscribe()
	if (!search) {
		unschedule()
		rememberedDevice.clear()
	} else {
		// create SmartThings devices for the selectedCandidates
		selectedCandidates.collect{it.split("\t")[0]}.each {udn ->
			if (rememberedDevice.containsKey(udn)) {
				def remembered = rememberedDevice."$udn"
				String hubId = remembered.hubId

				// create the MAC identified child, if needed
				String mac = remembered.mac
				physicalgraph.app.DeviceWrapper macChild = getChildDevice mac
				if (!macChild) {
					String label = name + ' ' + mac
					log info, "updated: addChildDevice $namespace, $name, $mac, $hubId, [label: $label, completedSetup: true]"
					addChildDevice namespace, name, mac, hubId, [label: label, completedSetup: true]
				}

				// create the UDN identified child
				String urn = remembered.ssdpUSN.urn
				def device = parseXml(parseLanMessage(remembered.ssdpPathResponse).body).device
				def deviceHandler = resolveDeviceHandler(urn, device)
				String label = (prefix && !prefix.isEmpty() ? prefix + ' ' : '') + device.friendlyName.text()
				log info, "updated: addChildDevice $deviceHandler.namespace, $deviceHandler.name, $udn, $hubId, [label: $label, data: [networkAddress: $remembered.networkAddress, deviceAddress: $remembered.deviceAddress, ssdpPath: $remembered.ssdpPath, description: $remembered.ssdpPathResponse]]"
				physicalgraph.app.DeviceWrapper udnChild = addChildDevice deviceHandler.namespace, deviceHandler.name, udn, hubId, [
					data : [
						networkAddress	: remembered.networkAddress,
						deviceAddress	: remembered.deviceAddress,
						ssdpPath		: remembered.ssdpPath,
						description		: remembered.ssdpPathResponse,
					],
					label				: label,
					completedSetup		: true,
				]
				udnChild.install()

				// we can forget about this
				rememberedDevice.remove udn
			}
		}
		// filter remaining rememberedDevice map based on current state/settings
		rememberedDevice.findAll{
			String udn = it.key
            def remembered = it.value
			String urn = remembered.ssdpUSN.urn
			!requestedCandidateUrns.contains(urn) || {
				remembered.ssdpPathResponse && !resolveDeviceHandler(urn, parseXml(parseLanMessage(remembered.ssdpPathResponse).body).device)
			}()
		}.collect{
			it.key
		}.each{udn ->
			rememberedDevice.remove udn
		}
		ssdpSubscribe()
		ssdpDiscover()
		runEvery5Minutes ssdpDiscover
	}
}

void installed() {
	log debug, "installed $settings"
}

void uninstalled() {
	getChildDevices().each {child ->
		log debug, "uninstalled: deleteChildDevice $child.deviceNetworkId"
		child.uninstall()
		deleteChildDevice child.deviceNetworkId
	}
}

def settingsPage() {
	List supportedCandidateList = []
	rememberedDevice.each {
		String udn = it.key
		String urn = it.value.ssdpUSN.urn
		if (it.value.ssdpPathResponse) {
			def device = parseXml(parseLanMessage(it.value.ssdpPathResponse).body).device
			if (resolveDeviceHandler(urn, device)) {
				supportedCandidateList.add "$udn\t${device.friendlyName.text()}"
			}
		}
	}
	dynamicPage(name: 'settingsPage', refreshInterval: search ? 10 : 0, install: true, uninstall: true) {
		section {
			input 'search', 'bool', title: 'Search for UPnP devices (and update created/associated SmartThings devices) periodically', defaultValue: false
			input 'requestedCandidateTypes', 'enum', required: false, title: 'Select supported SmartThings device types to search for', multiple: true, options: supportedCandidateTypeMap.collect{key, value -> key}
			input 'selectedCandidates', 'enum', required: false, title: "Create/associate supported SmartThings devices from ${supportedCandidateList.size()} UPnP device candidates", multiple: true, options: supportedCandidateList
			input 'prefix', 'text', defaultValue: 'UPnP', required: false, title: 'Device label prefix'
			input 'logLevel', 'number', defaultValue: '1', title: 'Log level (-1..4: trace, debug, info, warn, error, none)', range: '-1..4'
		 }
	}
}

preferences {
	page(name: 'settingsPage')
}
