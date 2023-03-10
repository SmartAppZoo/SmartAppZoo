/*
 *  DSC Alarm Panel integration via REST API callbacks
 *
 */

import groovy.json.JsonSlurper;

definition(
    name: "DSC Integration",
    namespace: "",
    author: "",
    description: "DSC Integration App",
    category: "My Apps",
    iconUrl: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_small.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2760581/dscpanel_large.png",
    oauth: true
)

import groovy.json.JsonBuilder

preferences {
  section("Alarm Server Settings") {
  	input("ip", "text", title: "IP", description: "The IP of your AlarmServer", required: true)
    input("port", "text", title: "Port", description: "The port", required: true)
    //Get Alarm Code
    input("alarmCodePanel", "text", title: "Alarm Code", description: "The code for your alarm panel.", required: true)
    //Allow user to turn off the Smart Monitor Integration if they arn't using it or use it for another purpose
  	input name: "smartMonitorInt", title: "Integrate w/ Smart Monitor?", type: "bool", defaultValue: "true", required: true, submitOnChange: false
  	input name: "stayIsInstant", title: "Make Stay Arm Instant arm?", type: "bool", defaultValue: "false", required: true, submitOnChange: false
  	input name: "pushNotify", title: "Send Push Notification?", type: "bool", defaultValue: "false", required: true, submitOnChange: false
	input name: "enableVoiceNotify", title: "Enable Voice Notifications", type: "bool", defaultValue: "false", required: true, submitOnChange: true

        input "Speakers","capability.musicPlayer", title: "Speaker", multiple: true, requred: false
        input "SpeakerVolume", "number", title: "Set volume to (1-100%):", required: false

  }
}

mappings {
  path('/update')            { action: [POST: 'update'] }
  path('/installzones')      { action: [POST: 'installzones'] }
  path('/installpartitions') { action: [POST: 'installpartitions'] }
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "updateZoneOrPartition"
    ]
  }
}

def installed() {
  log.debug "Installed!"
  initialize()
}

def updated() {
  log.debug "Updated!"
  unsubscribe()
  unschedule()
  initialize()
}
def initialize() {
    if(smartMonitorInt)
    {
        subscribe(location, "alarmSystemStatus", alarmStatusUpdate)
    }
}



def installzones() {
  def children = getChildDevices()
  def zones = request.JSON
  for (zone in zones) {
    def id = zone.key
    def device = 'DSC Zone'
    def name = zone.value
    def networkId = "dsczone${id}"
    def zoneDevice = children.find { item -> item.device.deviceNetworkId == networkId }

    if (zoneDevice == null) {
      log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
      zoneDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
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
           zoneDevice = addChildDevice("dsc", "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
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
  for (part in partitions) {
    def id = part.key
    def name = part.value
    def networkId = "dscpanel${id}"
    def partDevice = children.find { item -> item.device.deviceNetworkId == networkId }
    def device = "DSC Command Center"
    if (partDevice == null) {
      log.debug "add new child: device: ${device} networkId: ${networkId} name: ${name}"
      partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
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
             partDevice = addChildDevice('dsc', "${device}", networkId, null, [name: "${name}", label:"${name}", completedSetup: true])
           } else {
             log.error "${e}"
           }
        }
      }
  }
}


void updateZoneOrPartition() {
  update()
}



private update() {
    def zoneorpartition = params.zoneorpart

    // Add more events here as needed
    // Each event maps to a command in your "DSC Panel" device type
    def eventMap = [
      '601':"zone alarm",
      '602':"zone closed",
      '609':"zone open",
      '610':"zone closed",
      '616':"zone bypass",
      '631':"zone smoke",
      '632':"zone clear",
      '650':"partition ready",
      '651':"partition notready",
      '652':"partition armed",
      '654':"partition alarm",
      '656':"partition exitdelay",
      '657':"partition entrydelay",
      '666':"partition instantaway",
      '667':"partition instantstay",
      '668':"partition stayarm",
      '701':"partition armed",
      '702':"partition armed",
     ]
     

    // get our passed in eventcode
    def eventCode = params.eventcode
    if (eventCode)
    {
      // Lookup our eventCode in our eventMap
      def opts = eventMap."${eventCode}"?.tokenize()
      if (opts && opts[0])
      {
        // We have some stuff to send to the device now
        // this looks something like panel.zone("open", "1")
        if ("${opts[0]}" == 'zone') {
           updateZoneDevices("$zoneorpartition","${opts[1]}")
        }
        if ("${opts[0]}" == 'partition') {

   
           updatePartitions( "$zoneorpartition","${opts[1]}")
        }
      }
    }
    if (pushNotify) {
    def pushMessages = [
                        '654':"Alarm Activated",
                        "800":"Bad battery detected on alarm system",
     					"801":"Alarm battery restored",
     					"802":"Alarm has lost power",
      					"803":"Alarm power restored",
      					'840':"Alarm violation triggered",
      					'841':"Alarm violation cleared",
       	]
        def msg = pushMessages."${eventCode}"
        if(msg)
        {
        	sendPush(msg)
        }
     }
}

private updateZoneDevices(zonenum,zonestatus) {
  def children = getChildDevices()
  def zonedevice = children.find { it.deviceNetworkId == "dsczone${zonenum}" }
  if (zonedevice) {
      if(zonedevice.latestValue("contact") != zonestatus) {
         def paneldevice = children.find { it.deviceNetworkId == "dscpanel1" }
	     if(paneldevice) {           
      	    paneldevice.sendEvent(name: "zonestate", isStateChange: true, value: zonestatus, descriptionText: "is ${zonestatus}" , linkText: zonedevice.name )
         }
      }
      zonedevice.zone("${zonestatus}")

    }
}

private updatePartitions( partitionnum, partitionstatus) {
  def children = getChildDevices()
  log.debug "paneldevices: $paneldevices - ${partitionnum} is ${partitionstatus}"
  def paneldevice = children.find { it.deviceNetworkId == "dscpanel${partitionnum}" }

  if (paneldevice) {
    log.debug "Was True... Panel device: $paneldevice.displayName at $paneldevice.deviceNetworkId is ${partitionstatus}"
    paneldevice.partition("${partitionstatus}", "${partitionnum}")
  }
}

private sendMessage(msg) {
    def newMsg = "Alarm Notification: $msg"
    if (phone1) {
        sendSms(phone1, newMsg)
    }
    if (sendPush == "Yes") {
        sendPush(newMsg)
    }
}
def lanResponseHandler(evt) {
  log.debug evt
  }

def switchUpdate(evt) {
  def securityMonitorMap = [
       'stayarm':"stay",
       'instantstay': 'stay',
       'instantaway': 'stay',
       'disarm':"off",
       'arm':"away",
       'armed':"away"
   ]
   def voiceMap = [
   	'exitdelay' : "Arming Alarm. Exit delay in progress",
    'disarm' : "Alarm Disarmed",
    'armed' : 'Alarm Armed in Away mode',
    'stayarm' : 'Alarm Armed in Stay mode',
    'instantstay': 'Alarm Armed in Instant mode',
  
   ]
   
   def action = securityMonitorMap."${evt}"
   if(action) {
	setSmartHomeMonitor(action)
    }
   def voiceaction = voiceMap."${evt}"
   if(voiceaction) {
   		voiceNotify(voiceaction)
   }
}

//When a button is pressed in Smart Home Monitor, this will capture the event and send that to Alarm Server
//It will also sync the status change over to the DSC Command Switch
def alarmStatusUpdate(evt) {
    def eventMap = [
        'stay':"/api/alarm/stayarm",
        'off':"/api/alarm/disarm",
        'away':"/api/alarm/armwithcode",
        'instant':"/api/alarm/instantarm",

    ]

    def securityMonitorMap = [
        'stay':"stayarm",
        'off':"disarm",
        'away':"arm"
    ]

    def command = securityMonitorMap."${evt.value}";
    setCommandSwitch(command)
    def path = eventMap."${evt.value}"
    if(stayIsInstant && evt.value == 'stay')
    {
     path = eventMap.'instant'
    }
    callAlarmServer(path)
}

private callAlarmServer(path) {
    try {
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                HOST: "${ip}:${port}"
            ],
            query: [alarmcode: "${alarmCodePanel.value}"]
        ))
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private setCommandSwitch(command)
{
getChildDevices().each { 
	if(it.deviceNetworkId.startsWith('dscpanel'))
    {
    	if(it.currentSwitch != command) {
         log.debug "Set Command Switch to $command"
         //it."$command"()       
        }
     }
   }
}

def voiceNotify(phrase) {
	if(enableVoiceNotify) {
		Speakers.each(){
			if(SpeakerVolume)
			{
				it.setVolume(SpeakerVolume);
			}
    		it.speak(phrase)
    	}
	}
}

private setSmartHomeMonitor(status)
{
    if(smartMonitorInt && location.currentState("alarmSystemStatus").value != status)
    {
        log.debug "Set Smart Home Monitor to $status"
        sendLocationEvent(name: "alarmSystemStatus", value: status)
    }
}
