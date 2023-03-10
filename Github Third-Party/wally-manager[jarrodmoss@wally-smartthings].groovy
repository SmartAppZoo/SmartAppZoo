import java.text.SimpleDateFormat

definition(
    name: "WallyHome",
    namespace: "jarrodmoss",
    author: "Jarrod Moss",
    description: "Connect your WallyHome to SmartThings.",
    category: "Convenience",
  	iconUrl: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager.png",
  	iconX2Url: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager%402x.png",
  	iconX3Url: "https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/nest_manager%403x.png",
    singleInstance: true)

mappings {
    path("/oauth/initialize") {action: [GET: "oauthInitUrl"]}
    path("/oauth/callback") {action: [GET: "callback"]}
}

preferences {
    page(name: "auth", title: "Authentication", content: "authPage", install: false)
    //page(name: "deviceList", title: "WallyHome", content: "wallyDeviceList", install:true)
}

def authPage() {
  log.debug "auth page"
    // Check to see if our SmartApp has it's own access token and create one if not.
    if(!state.accessToken) {
        // the createAccessToken() method will store the access token in state.accessToken
        createAccessToken()
    }

    // def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    // Check to see if we already have an access token from the 3rd party service.
    if(!state.authToken) {
      state.authToken = "3ee370e1-d496-4e79-bdc3-346c23689605"
    } else {
        // We have the token, so we can just call the 3rd party service to list our devices and select one to install.
        initialize()
    }

    return dynamicPage(name: "auth", title: "Log In", nextpage: "", install: true, uninstall: true) {
      section(""){
        paragraph "Tap Next to continue to setup your thermostats."
      }
    }
}

def updated() {
  log.debug "updated"
    initialize()
}

def initialize() {
    log.debug "initalize"
    def property_id = "555e70ade4b0eafa380f8545"
    state.property_id = "555e70ade4b0eafa380f8545"
    def sensorListParams = [
      uri: "https://api.snsr.net/",
      path: "/v2/places/${property_id}/sensors",
      headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.authToken}"],
    ]
    httpGet(sensorListParams) { resp ->
  		if(resp?.status == 200) {
        state.devices = resp?.data
  		} else {
  			"get sensor data initialize - Received a diffent Response than expected: Resp (${resp?.status})"
  		}
  	}

    state.devices.each {device ->
        def deviceId = device?.snid
        def deviceLabel = "${device?.location?.appliance}-${device?.location?.floor}-${device?.location?.room}"
        try {
            def existingDevice = getChildDevice(deviceId)
            if(!existingDevice) {
                def childDevice = addChildDevice("jarrodmoss", "WallyHome Sensor", deviceId, null, [name: "Device.${deviceId}", label: deviceLabel, completedSetup: true])
            }
        } catch (e) {
            log.error "Error creating device: ${e}"
        }
    }

    pollChildren()
}

def pollChildren() {
  log.debug("poll children")
  //state.each {key, val ->
  //  log.debug "state key: $key, value: $val"
  //}
  def sensorListParams = [
    uri: "https://api.snsr.net/",
    path: "/v2/places/${state.property_id}/sensors",
    headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.authToken}"],
  ]
  def df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  httpGet(sensorListParams) { resp ->
    if(resp?.status == 200) {
      state.devices = resp?.data.inject([:]) { collector, stat ->
        def dni = [ app.id, stat.snid ].join('.')
        def data = [
          humidity: stat.state.RH.value,
          temperature: stat.state.TEMP.value
        ]
        collector[dni] = [data: data]
        def d = getChildDevice(stat.snid)
        if(d) {
          def timeNow = new Date()
          def timeLastUpdated = df1.parse(stat.state.TEMP.at)
          def lastUpdate = (timeNow.getTime() - timeLastUpdated.getTime()) / 60000
          log.debug "${d} last updated ${lastUpdate} minutes ago"
          d.sendEvent(name: "humidity", value: stat.state.RH.value)
          if(lastUpdated <= 30) {
            d.sendEvent(name: "temperature", value: stat.state.TEMP.value * 1.8 + 32)
            d.sendEvent(name: "lastUpdate", value: lastUpdate)
            //sendPush("test message wally")
          } else {
            d.sendEvent(name: "temperature", value: null)
            sendPush("Wally device ${d} hasn't updated in more than 30 minutes")
          }
        }
        collector
      }
    } else {
      log.debug "get sensor data poll - Received a diffent Response than expected: Resp (${resp?.status})"
    }
  }
}

def installed() {
  initialize()
}

def uninstalled() {
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def wallyDeviceList()
{
	log.debug "wallyDeviceList()"

	def stats = [] //getEcobeeThermostats()

	log.debug "device list: $stats"

	def p = dynamicPage(name: "deviceList", title: "Select Your Thermostats", uninstall: true) {
		section(""){
			paragraph "Tap below to see the list of ecobee thermostats available in your ecobee account and select the ones you want to connect to SmartThings."
			input(name: "thermostats", title:"", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:stats])
		}
	}

	log.debug "list p: $p"
	return p
}
