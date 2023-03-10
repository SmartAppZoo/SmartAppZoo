/*
 *  Honeywell alarm panel callbacks via honeyalarm server python script by MattTW
 *	uses restful api code by Kent Holloway <drizit@gmail.com>
 *  got the idea from phiz118 in the ST Forum
 *
 *  Versioning
 *  2.0 - Added debugging to SHM sync
 *  
 */

definition(
    name: "Honeywell Panel Integration V2",
    namespace: "Webstas",
    author: "BigWebstas (JWebstas@gmail.com)",
    description: "Honeywell Panel Integration V2",
    category: "My Apps",
    iconUrl: "http://d1unzhqf5a606m.cloudfront.net/images/large/honeywell-6150-fixed-english-alarm-keypad-with-function-buttons.jpg?1340151882",
    iconX2Url: "http://d1unzhqf5a606m.cloudfront.net/images/large/honeywell-6150-fixed-english-alarm-keypad-with-function-buttons.jpg?1340151882",
    oauth: true
)
import groovy.json.JsonBuilder

preferences {

  section("Alarm Panel:") {
    input "paneldevices", "capability.polling", title: "Partition Devies", multiple: false, required: false
  }
  section("Zone Devices:") {
    input "zonedevices", "capability.polling", title: "Honeywell Zone Devices", multiple: true, required: false
  }
  section("Alarm Server Settings") {
    input("ip", "text", title: "IP", description: "The IP of your AlarmServer", required: true)
    input("port", "text", title: "Port", description: "The Port AlarmServer is running on", required: true)
  }
  section("SHM sync ") {
  	input "syncshm", "enum", title: "SHM<->Partiton", options: ["Yes", "No"], required: true
  }
}

mappings {
  path("/panel/:eventcode/:zoneorpart") {
    action: [
      GET: "updateZoneOrPartition"
    ]
  }
}
def installed() {
initialize()
}

def updated() {
log.debug "Updated!"
initialize()
}

def initialize() {
	log.debug "Initalizing ${settings}"
	 if(syncshm.value[0] != "N") {
    	subscribe(location, "alarmSystemStatus", shmtopartition)
        subscribe(paneldevices, "dscpartition", partitiontoshm)
    }
    log.debug "APP_ID: $app.id"
    log.debug "ACCESS_TOKEN: $state.accessToken"
}

void updateZoneOrPartition() {
  update()
}

private update() {
    def zoneorpartition = params.zoneorpart

    //map event codes to zones status and/or partition status's
    def eventMap = [
      '601':"zone alarm",
      '602':"zone closed",
      '609':"zone open",
      '610':"zone closed",
      '631':"zone smoke",
      '632':"zone clear",
      '650':"partition ready",
      '651':"partition notready",
      '652':"partition armed-stay",
      '654':"partition alarm",
      '703':"partition armed-max",
      '656':"partition exit/entry-delay",
      '701':"partition armed-away",
      '702':"partition ready-bypass"
    ]

    def eventCode = params.eventcode
    if (eventCode)
    {
      //lookup event and send SHM command if needed
      def opts = eventMap."${eventCode}"?.tokenize()
      log.debug eventCode
    if (opts[0])
      {
        if ("${opts[0]}" == 'zone') {
           updateZoneDevices(zonedevices,"$zoneorpartition","${opts[1]}")
        }
        if ("${opts[0]}" == 'partition') {
           updatePartitions(paneldevices, "$zoneorpartition","${opts[1]}")
        }
      }
    }
}
//update zone child devices
private updateZoneDevices(zonedevices,zonenum,zonestatus) {
  log.debug "zonedevices: $zonedevices - ${zonenum} is ${zonestatus}"
  def zonedevice = zonedevices.find { it.deviceNetworkId == "zone${zonenum}" }
  if (zonedevice) {
      zonedevice.zone("${zonestatus}")
    }
}
//update partition child devices
private updatePartitions(paneldevices, partitionnum, partitionstatus) {
  log.debug "paneldevices: $paneldevices - ${partitionnum} is ${partitionstatus}"
  def paneldevice = paneldevices.find { it.deviceNetworkId == "partition${partitionnum}" }
  if (paneldevice) {
    paneldevice.partition("${partitionstatus}", "${partitionnum}")
  }
}
//Honeywell panel to SHM
def partitiontoshm(evt) {
    def securityMonitorMap = [
        'armed-stay':"stay",
        'exit/entry-delay':"off",
        'armed-away':"away",
        'ready':"off",
        'notready':"off",
        'ready-bypass':"off"
    ]
    SetSHM(securityMonitorMap."${evt.value}")
}
//SHM to Honeywell panel
def shmtopartition(evt) {
	def eventMap = [
        'stay':"/api/alarm/stayarm",
        'off':"/api/alarm/disarm",
        'away':"/api/alarm/arm"
    ]
    def securityMonitorMap = [
        'stay':"armed-stay",
        'off':"ready",
        'away':"armed-away"
    ]
    def path = eventMap."${evt.value}"
    def panelstate = securityMonitorMap."${evt.value}"
    def currstate = paneldevices.currentState("dscpartition").value
    
    log.debug "${panelstate}":"${currstate}"
    if (currstate != panelstate && path != null){
    	log.debug "States dont match!"
    	callAlarmServer(path)
    } else { log.debug "States Match" }
    
}
//Send commands to EVL3/4 
private callAlarmServer(path) {
		log.debug "Sending command to EVL"
		log.debug "Set Panel with ${ip}:${port}${path}"
        sendHubCommand(new physicalgraph.device.HubAction(
            method: "GET",
            path: path,
            headers: [
                HOST: "${ip}:${port}"
            ],
        ))
}
//Set the SHM
private SetSHM(status)
{
	if(location.currentState("alarmSystemStatus").value != status && status != null && syncshm.value[0] != "N") {
    	log.debug "Set Smart Home Monitor to $status"
    	sendLocationEvent(name: "alarmSystemStatus", value: status)
        }
}