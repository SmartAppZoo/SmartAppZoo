/*
 *  DSC-Envisalink Integration SmartApp
 *
 *  Author: Kent Holloway <drizit@gmail.com>
 *  Modified by: Matt Martz <matt.martz@gmail.com>
 *  Modified by: Jordan <jordan@xeron.cc>
 *  Modified by: Ralph Torchia
 *  Modified by: Dieter Rothhardt
 *  Date: 2020-12-18
 */

definition(
  name: 'DSC Envisalink',
  namespace: 'acrosscable12814',
  author: 'Dieter Rothhardt',
  description: 'DSC-Envisalink Integration SmartApp',
  category: 'My Apps',
  iconUrl: 'https://raw.githubusercontent.com/LXXero/DSCAlarm/master/resources/images/dscpanel_small.jpg',
  iconX2Url: 'https://raw.githubusercontent.com/LXXero/DSCAlarm/master/resources/images/dscpanel_large.jpg',
  oauth: true,
  singleInstance: true
)

import groovy.json.JsonBuilder

preferences {
  page(name: "MainSetup")
}

def MainSetup() {
  if (!state.accessToken) {
    createAccessToken()
  }
	dynamicPage(name: "MainSetup", title: "DSC-Envisalink Setup", install:true, uninstall:true) {
	  section('AlarmServer Setup:') {
  		input('ip', 'text', title: 'IP Address', description: 'The IP address of your AlarmServer (required)', required: false)
	  	input('port', 'text', title: 'Port Number', description: 'The port number (required)', required: false)
		  input 'sthmSync', 'enum', title: 'SmartThings Home Monitor Sync', required: false,
		  metadata: [
		    values: ['Yes','No']
		  ]
		  input 'sthmBypass', 'enum', title: 'STHM Stay/Away Bypass', required: false,
		    metadata: [
		      values: ['Yes','No']
		    ]
		  input 'sthmLocalStay', 'enum', title: 'STHM Stay if DSC has no interior zones', required: false,
		    metadata: [
		      values: ['Yes','No']
		    ]
		  input 'sthmVirtualSwitches', 'enum', title: 'STHM Create Virtual Switches', required: false,
		    metadata: [
		      values: ['Yes','No']
		    ]
	  }
	  
    section('Notifications (optional)') {
		  input 'sendPush', 'enum', title: 'Push Notification', required: false,
		    metadata: [
		      values: ['Yes','No']
		    ]
		  input 'phone1', 'phone', title: 'Phone Number', required: false
	  }
	  
    section('Notification events (optional):') {
		  input 'notifyEvents', 'enum', title: 'Which Events?', description: 'Events to notify on', required: false, multiple: true,
		    options: [
			    'all', 'partition alarm', 'partition armed', 'partition away', 'partition chime', 'partition disarm',
			    'partition duress', 'partition entrydelay', 'partition exitdelay', 'partition forceready',
			    'partition instantaway', 'partition instantstay', 'partition nochime', 'partition notready', 'partition ready',
			    'partition restore', 'partition stay', 'partition trouble', 'partition keyfirealarm', 'partition keyfirerestore',
			    'partition keyauxalarm', 'partition keyauxrestore', 'partition keypanicalarm', 'partition keypanicrestore',
			    'led backlight on', 'led backlight off', 'led fire on', 'led fire off', 'led program on', 'led program off',
			    'led trouble on', 'led trouble off', 'led bypass on', 'led bypass off', 'led memory on', 'led memory off',
			    'led armed on', 'led armed off', 'led ready on', 'led ready off', 'zone alarm', 'zone clear', 'zone closed',
			    'zone fault', 'zone open', 'zone restore', 'zone smoke', 'zone tamper'
		    ]
	  }
	  
	section('SmartApp Name:') {
		label title:"SmartApp Label (optional)", required: false 
	}
    section('Token Info:') {
		  paragraph "View this SmartApp's AppID, URL and Token Configuration to use it in the Alarmserver config."
		  href url:"${apiServerUrl("/api/smartapps/installations/${app.id}/config?access_token=${state.accessToken}")}", style:"embedded", required:false, title:"Show Smartapp Token Info", description:"Tap, select, copy, then click \"Done\""
	}
  }
}
mappings {
  path('/update')            { action: [POST: 'update'] }
  path('/installzones')      { action: [POST: 'installzones'] }
  path('/installpartitions') { action: [POST: 'installpartitions'] }
  if (!params.access_token || (params.access_token && params.access_token != state.accessToken)) {
    path("/config") { action: [GET: "authError"] }
	} else {
    path("/config") { action: [GET: "renderConfig"]  }
  }
}

def initialize() {

  if(settings.sthmVirtualSwitches == "Yes") {
  	installVirtualSwitches()
  }

 if (settings.sthmSync == 'Yes') {
    //STHM
    log.debug("Subscribing to Security System Status")
    subscribe(location, 'securitySystemStatus', sthmHandler)
    subscribe(location, 'alarmSystemStatus', sthmHandler)
  }

  if(!state.accessToken) {
	  createAccessToken()
  }
}

def sthmHandler(evt) {
  log.debug (evt)
  if (settings.sthmSync == 'Yes') {
    log.debug "sthmHandler: STHM changed status to: ${evt.value}"

	//update virtual switches
	if(settings.sthmVirtualSwitches == "Yes") {
    	if(evt.value == "disarmed") {
              log.debug "Switching off both VSwitches"
              updateVirtualSwitches("stay", "off")
              updateVirtualSwitches("away", "off")
        }
    	if(evt.value == "armedAway") {
	           log.debug "Switching on VSwitch Away"
    	       updateVirtualSwitches("away", "on")
        }
    	if(evt.value == "armedStay") {
	           log.debug "Switching on VSwitch Stay"
    	       updateVirtualSwitches("stay", "on")
       }
    }

    def children = getChildDevices()
    def child = children.find { item -> item.device.deviceNetworkId in ['dscstay1', 'dscaway1'] }
    if (child != null) {
      def panelStatus = child.currentPartitionStatus
      log.debug "sthmHandler: using panel: ${child.device.deviceNetworkId} status: ${panelStatus}"

      //map DSC Panel status to simplified values for comparison
	  def dscMap = [
        'Armed Away':'armedAway',
        'Entry Dealy': 'on',
        'Exit Delay': 'on',
        'Force Ready':'disarmed',
        'Ready':'disarmed',
        'Armed Stay':'armedStay',
      ]

      if (dscMap[panelStatus] && evt.value != dscMap[panelStatus]) {
        if (evt.value == 'disarmed' && dscMap[panelStatus] in ['armedStay', 'armedAway', 'on'] ) {
          sendUrl('disarm')
          log.debug "sthmHandler: ${evt.value} is valid action for ${panelStatus}, disarmed sent"
        } else if (evt.value == 'armedAway' && dscMap[panelStatus] in ['armedStay', 'disarmed'] ) {
          sendUrl('arm')
          log.debug "sthmHandler: ${evt.value} is valid action for ${panelStatus}, armedAway sent"
        } else if (evt.value == 'armedStay' && dscMap[panelStatus] in ['armedAway', 'disarmed'] ) {
          sendUrl('stayarm')
          log.debug "sthmHandler: ${evt.value} is valid action for ${panelStatus}, armedStay sent"
        }
      }
    }
  }
}

def installzones() {
  def children = getChildDevices()
  def zones = request.JSON

  def zoneMap = [
    'contact':'DSC Zone Contact',
    'motion':'DSC Zone Motion',
    'smoke':'DSC Zone Smoke',
    'co':'DSC Zone CO',
    'water':'DSC Zone Water',
  ]

  log.debug "children are ${children}"
  for (zone in zones) {
    def id = zone.key
    def type = zone.value.'type'
    def device = zoneMap."${type}"
    def name = zone.value.'name'
    def networkId = "dsczone${id}"
    def zoneDevice = children.find { item -> item.device.deviceNetworkId == networkId }

    if (zoneDevice == null) {
      log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
      zoneDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
    } else {
      log.debug "zone device was ${zoneDevice}"
      try {
        log.debug "trying name update for ${networkId}"
        zoneDevice.name = "${name}"
        log.debug "trying label update for ${networkId}"
        zoneDevice.label = "${name}"
      } catch(IllegalArgumentException e) {
        log.debug "excepted for ${networkId}"
         if ("${e}".contains('identifier required')) {
           log.debug "Attempted update but device didn't exist. Creating ${networkId}"
           zoneDevice = addChildDevice("acrosscable12814", "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
         } else {
           log.error "${e}"
         }
      }
    }
  }

  for (child in children) {
    if (child.device.deviceNetworkId.contains('dsczone')) {
      def zone = child.device.deviceNetworkId.minus('dsczone')
      def jsonZone = zones.find { x -> "${x.key}" == "${zone}"}
      if (jsonZone == null) {
        try {
          log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
          deleteChildDevice(child.device.deviceNetworkId)
        } catch(MissingMethodException e) {
          if ("${e}".contains('types: (null) values: [null]')) {
            log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
          } else {
            log.error e
          }
        }
      }
    }
  }
}

def installpartitions() {
  def children = getChildDevices()
  def partitions = request.JSON

  def partMap = [
    //'stay':'DSC Stay Panel',
    'away':'DSC Panel',
    //'simplestay':'DSC Simple Stay Panel',
    //'simpleaway':'DSC Simple Away Panel',
  ]

  log.debug "children are ${children}"
  for (part in partitions) {
    def id = part.key

    for (p in part.value) {
      def type = p.key
      def name = p.value
      def networkId = "dsc${type}${id}"
      def partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
      def device = partMap."${type}"

      if (partDevice == null) {
        log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
        partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
      } else {
        log.debug "part device was ${partDevice}"
        try {
          log.debug "trying name update for ${networkId}"
          partDevice.name = "${name}"
          log.debug "trying label update for ${networkId}"
          partDevice.label = "${name}"
        } catch(IllegalArgumentException e) {
          log.debug "excepted for ${networkId}"
          if ("${e}".contains('identifier required')) {
            log.debug "Attempted update but device didn't exist. Creating ${networkId}"
            partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
          } else {
            log.error "${e}"
          }
        }
      }
    }
  }

  for (child in children) {
    for (p in ['stay', 'away']) {
        if (child.device.deviceNetworkId.contains("dsc${p}")) {
        def part = child.device.deviceNetworkId.minus("dsc${p}")
        def jsonPart = partitions.find { x -> x.value."${p}" }
        if (jsonPart== null) {
          try {
            log.debug "Deleting device ${child.device.deviceNetworkId} ${child.device.name} as it was not in the config"
            deleteChildDevice(child.device.deviceNetworkId)
          } catch(MissingMethodException e) {
            if ("${e}".contains('types: (null) values: [null]')) {
              log.debug "Device ${child.device.deviceNetworkId} was empty, likely deleted already."
            } else {
              log.error e
            }
          }
        }
      }
    }
  }
}

def installVirtualSwitches() {
  def children = getChildDevices()

  log.debug "installVirtualSwitches. Children are ${children}"
  
  //Away Switch
  def networkId = "dscvswitchaway"
  def partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
  def device = "DSC Virtual Switch"
  def name = "DSC STHM Away VSwitch"

  if (partDevice == null) {
	log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
    partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
  } else {
    log.debug "part device was ${partDevice}"
    try {
        log.debug "trying name update for ${networkId}"
        partDevice.name = "${name}"
        log.debug "trying label update for ${networkId}"
        partDevice.label = "${name}"
    } catch(IllegalArgumentException e) {
        log.debug "excepted for ${networkId}"
        if ("${e}".contains('identifier required')) {
            log.debug "Attempted update but device didn't exist. Creating ${networkId}"
            partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
        } else {
            log.error "${e}"
        }
    }
  }

  //Stay Switch
  networkId = "dscvswitchstay"
  partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
  device = "DSC Virtual Switch"
  name = "DSC STHM Stay VSwitch"

  if (partDevice == null) {
	log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
    partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
  } else {
    log.debug "part device was ${partDevice}"
    try {
        log.debug "trying name update for ${networkId}"
        partDevice.name = "${name}"
        log.debug "trying label update for ${networkId}"
        partDevice.label = "${name}"
    } catch(IllegalArgumentException e) {
        log.debug "excepted for ${networkId}"
        if ("${e}".contains('identifier required')) {
            log.debug "Attempted update but device didn't exist. Creating ${networkId}"
            partDevice = addChildDevice('acrosscable12814', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
        } else {
            log.error "${e}"
        }
    }
  }
}

def autoBypass() {
  def closedList = ['clear','closed','dry','inactive']
  def deviceList = ['co','contact','motion','smoke','water']
  def children = getChildDevices()
  def zones = children.findAll { it.device.deviceNetworkId.startsWith("dsczone") }
  def bypassList = []

  for (zone in zones) {
  	log.debug("bypass item: ${zone}")
    for (device in deviceList) {
      if (zone.currentValue(device)) {
      	log.debug("bypass item: ${zone.currentValue(device)}")
        if (!closedList.contains(zone.currentValue(device))) {
          def bypass = zone.deviceNetworkId.minus('dsczone')
          log.debug("bypass item: ${bypass}")
          bypassList.add(bypass)
        }
      }
    }
  }
  if (bypassList) {
    sendUrl("bypass?zone=${bypassList.sort().unique().join(',')}")
  }
}

def sendUrl(url) {

  //eeddir add a hack to allows STHM to got ArmedStay if DSC has no interior zones defined and hence will return ArmedAway
  log.debug ("sendUrl: ${url}")
  if (settings.sthmLocalStay == 'Yes') {
  	if (url.contains('stayarm')) {
          log.debug "sending SmartThings Home Monitor: 'armedStay'"
          sendLocationEvent(name: "securitySystemStatus", value: "armedStay")
	}          
  }

/*  if(settings.sthmVirtualSwitches == "Yes") {
  	if (url.contains('stayarm')) {
          log.debug "Switching on VSwitch Stay"
          updateVirtualSwitches("stay", "on")
	}          
  	if (url.contains('stayarm')) {
          log.debug "Switching on VSwitch Away"
          updateVirtualSwitches("away", "on")
	}          
  	if (url.contains('stayarm')) {
          log.debug "Switching off both VSwitches"
          updateVirtualSwitches("stay", "off")
          updateVirtualSwitches("away", "off")
	}          
  }
*/
  def result = new physicalgraph.device.HubAction(
    method: 'GET',
    path: "/api/alarm/${url}",
    headers: [
      HOST: "${settings.ip}:${settings.port}"
    ]
  )
	log.debug("result: ${result}")
	sendHubCommand(result)
	log.debug 'response' : "Request to send url: ${url} received"
  return result
}


def installed() {
  log.debug 'Installed!'
  initialize()
}

def updated() {
  unsubscribe()
  unschedule()
  initialize()
  log.debug 'Updated!'
}

def authError() {
  [error: "Permission denied"]
}

def renderConfig() {
  def configJson = new groovy.json.JsonOutput().toJson([
    description: "SmartApp Token/AppID",
    platforms: [
      [
        app_url: apiServerUrl("/api/smartapps/installations"),
        app_id: app.id,
        access_token:  state.accessToken
      ]
    ],
  ])

  def configString = new groovy.json.JsonOutput().prettyPrint(configJson)
  render contentType: "text/plain", data: configString
}

private update() {
  def update = request.JSON

  log.debug ("update: ${update}")
  
  // Map DSC states to STHM modes, only using absolute states for now, no exit/entry delay
  def sthmMap = [
        'away':'armedAway',
        'forceready':'disarmed',
        'instantaway':'armedAway',
        'instantstay':'armedStay',
        'notready':'disarmed',
        'ready':'disarmed',
        'stay':'armedStay',
  ]

  def messageMap = [
        'alarm':'ALARMING!',
        'armed':'armed',
        'away':'armed away',
        'bypass':'zone bypass refresh',
        'clear':'sensor cleared',
        'closed':'closed',
        'chime':'chime enabled',
        'disarm':'disarmed',
        'duress':'DURESS ALARM!',
        'entrydelay':'entry delay in progress',
        'exitdelay':'exit delay in progress',
        'fault': 'faulted!',
        'forceready':'forced ready',
        'instantaway':'armed instant away',
        'instantstay':'armed instant stay',
        'nochime':'chime disabled',
        'notready':'not ready',
        'open': 'opened',
        'ready':'ready',
        'restore':'restored',
        'smoke': 'SMOKE DETECTED!',
        'stay':'armed stay',
        'tamper': 'tampered!',
        'trouble':'in trouble state!',
        'keyfirealarm':'Fire key ALARMING!',
        'keyfirerestore':'Fire key restored',
        'keyauxalarm':'Auxiliary key ALARMING!',
        'keyauxrestore':'Auxiliary key restored',
        'keypanicalarm':'Panic key ALARMING!',
        'keypanicrestore':'Panic key restored',
  ]

  if (update.'parameters' && "${update.'type'}" == 'partition') {
    for (p in update.'parameters') {
      if (notifyEvents && (notifyEvents.contains('all') || notifyEvents.contains("led ${p.key} ${p.value}".toString()))) {
        def flash = (update.'status'.startsWith('ledflash')) ? 'flashing ' : ''
        sendMessage("Keypad LED ${p.key.capitalize()}: ${flash}${p.value}")
      }
    }
  } else {
    if (notifyEvents && (notifyEvents.contains('all') || notifyEvents.contains("${update.'type'} ${update.'status'}".toString()))) {
      def messageBody = messageMap[update.'status']

      if (update.'name') {
        sendMessage("${update.'type'.capitalize()} ${update.'name'} ${messageBody}")
      } else {
        sendMessage(messageBody)
      }
    }
  }
  if ("${update.'type'}" == 'zone') {
    updateZoneDevices(update.'value', update.'status')
    //eeddir added to show zone in Mode
    updatePartitionsAddtl(update.'type', update.'value', update.'status', update.'parameters')
  } else if ("${update.'type'}" == 'partition') {
    if (settings.sthmSync == 'Yes') {
      log.debug("STHM: ${location.currentState("securitySystemStatus")?.value} - sthmMap: ${sthmMap[update.'status']} ")
      
      if (sthmMap[update.'status']) {
        if (settings.sthmBypass != 'Yes' || !(location.currentState("securitySystemStatus")?.value in ['armedAway','armedStay'] && sthmMap[update.'status'] in ['armedAway','armedStay'])) {
          log.debug "sending SmartThings Home Monitor: ${sthmMap[update.'status']} for status: ${update.'status'}"
          sendLocationEvent(name: "securitySystemStatus", value: sthmMap[update.'status'])
        }
      }
      
    }

    
    updatePartitions(update.'value', update.'status', update.'parameters')
  } else if ("${update.'type'}" == 'bypass') {
  	//eeddir
    updatePartitionsAddtl(update.'type', update.'value', update.'status', update.'parameters')
    for (p in update.'parameters') {
      updateZoneDevices(p.key, p.value)
    }
  }
}

private updateVirtualSwitches(type, action) {
  def children = getChildDevices()
  log.debug "type: ${type} action: ${action}"
  def vswitch = children.find { item -> item.device.deviceNetworkId == "dscvswitch${type}"}
  if (vswitch) {
    log.debug "vswitch: device $vswitch.displayName at $vswitch.deviceNetworkId is ${action}"
    vswitch.toggle("${action}")
  }
}

private updateZoneDevices(zonenum,zonestatus) {
  def children = getChildDevices()
  log.debug "zone: ${zonenum} is ${zonestatus}"
  def zonedevice = children.find { item -> item.device.deviceNetworkId == "dsczone${zonenum}"}
  if (zonedevice) {
    log.debug "zone: device $zonedevice.displayName at $zonedevice.deviceNetworkId is ${zonestatus}"
    //Was True... Zone Device: Front Door Sensor at zone1 is closed
    zonedevice.zone("${zonestatus}")
  }
}

private updatePartitions(partitionnum, partitionstatus, partitionparams) {
  def children = getChildDevices()
  log.debug "partition: ${partitionnum} is ${partitionstatus}"

  def panelList = ['stay', 'away', 'simplestay', 'simpleaway']
  
  for (paneltype in panelList) {
    def panel = children.find { item -> item.device.deviceNetworkId == "dsc${paneltype}${partitionnum}"}
    if (panel) {
      log.debug "partition: ${paneltype.capitalize()} Panel device: $panel.displayName at $panel.deviceNetworkId is ${partitionstatus}"
      panel.partition("${partitionstatus}", "${partitionnum}", partitionparams)
    }
  }
}

private updatePartitionsAddtl(type, zonenum, zonestatus, zoneparams) {
  def children = getChildDevices()
  log.debug "partitionAddtl: ${zonenum} is ${zonestatus}"

  def panelList = ['stay', 'away', 'simplestay', 'simpleaway']
  
  for (paneltype in panelList) {
    def panel = children.find { item -> item.device.deviceNetworkId == "dsc${paneltype}1"}
    if (panel) {
      log.debug "partition: ${paneltype.capitalize()} Panel device: $panel.displayName at $panel.deviceNetworkId is ${partitionstatus}"
      panel.partitionAddtl("${type}", "${zonenum}", zoneparams, "${zonestatus}")
    }
  }
}

private sendMessage(msg) {
  def newMsg = "Alarm Notification: $msg"
  if (phone1) {
    sendSms(phone1, newMsg)
  }
  if (sendPush == 'Yes') {
    sendPush(newMsg)
  }
}