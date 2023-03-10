definition(
    name: "Panel",
    namespace: "kmcoulson",
    author: "Kevin Coulson",
    description: "First Smart App",
    iconUrl: "http://cdn.device-icons.smartthings.com/Electronics/electronics18-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Electronics/electronics18-icn@2x.png",
    oath: false
)

preferences {
	page(name:"configurationHomePage", title:"Panel Configuration", content:"configurationHomePage", install: true)
    page(name: "configurationLoginPage")
    page(name: "configurationDevicesPage")
}

include 'asynchttp_v1'

def apiUri() { return "http://panel.eu-west-2.elasticbeanstalk.com" }

// Configuration

def configurationHomePage() {
    return dynamicPage(name: "configurationHomePage", title: "", install: true, uninstall: true) {
        section ("Your account") {
            href("configurationLoginPage", title: null, description: hasAuthToken() ? "Authenticated as $username, tap to change credentials" : "Tap to enter your account credentials", state: hasAuthToken() ? "complete" : null)
        }
        if (hasAuthToken()) {           	
           section ("Choose your devices:") {
               href("configurationDevicesPage", title: null, description: "Tap to select devices to use with Panel", state: null)
           }
           section () {
               label name: "name", title: "Assign a Name", required: true, state: (name ? "complete" : null), defaultValue: app.name
           }
        }
    }
}

def configurationLoginPage() {
    if (username != null && password != null && !hasAuthToken()) apiAuth();

    dynamicPage(name: "configurationLoginPage", title: "Login", uninstall: false, install: false) {
        section { paragraph "Enter your account credentials below to enable Panel integration." }
        section {
            input("username", "text", title: "Username", description: "Your Panel username", required: true)
            input("password", "password", title: "Password", description: "Your Panel password", required: true, submitOnChange: true)
        }
        
        if (hasAuthToken()) {
            section {
                paragraph "You have successfully connected to Panel. Click 'Done' and select your devices."
            }
        }
    }
}

def configurationDevicesPage() {
	dynamicPage(name: "configurationDevicesPage", title: "Devices", uninstall: false, install: false) {
        section { paragraph "Select the devices you want to show in Panel." }
        section {
            input "selectedSwitches", "capability.switch", title: "Select switches", multiple: true
        }
  }
}

def hasAuthToken() {
	return state.apiToken != null && state.apiToken != ''
}

// App lifecycle

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialized()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialized()
}

def initialized() {
	log.debug "Initializing"
    sendConfiguredDevices()
    subscribe selectedSwitches, "switch", eventHandler
}

def sendConfiguredDevices() {
    List devices = []
    selectedSwitches.each { device ->
        devices << [ id: "${device.id}", name: "${device.name}", type: "Switch" ]
    }

    def body = [
        token: state.apiToken,
        devices: devices
    ]
    apiPost("/Devices", body, sendConfiguredDevicesResponseProcessor)
}

def sendConfiguredDevicesResponseProcessor(response, data) {
    log.debug "Status: $response.status"
}

def eventHandler(evt) {
    log.debug "Switch event for $evt.displayName: $evt.value"

    def body = [
        token: state.apiToken,
        deviceId: evt.deviceId,
        state: evt.value
    ]
    apiPost("/Event", body, eventResponseProcessor)
}

def eventResponseProcessor(response, data) {
	if (response.status != 200) {
    	log.debug response
    }
}

def apiAuth() {
    def body = [
        username: username,
        password: password
    ]
    apiPost("/Auth", body, apiAuthResponseProcessor)
}

def apiAuthResponseProcessor(response, data) {
    if (response.status == 200) {
        def jsonResponse = response.json;
        log.debug "API token received for $jsonResponse.Username: $jsonResponse.Token"
        state.apiToken = jsonResponse.Token
    }
}

def apiGet(path, callback) {
    def params = [
        uri: apiUri(),
        path: "/api${path}",
        contentType: 'application/json'
    ]
    log.debug "Api GET: $params"
    asynchttp_v1.get(callback, params)
}

def apiPost(path, body = [:], callback) {
    def params = [
        uri: apiUri(),
        path: "/api${path}",
        body: body,
        contentType: 'application/json'
    ]
    log.debug "Api POST: $params"
    asynchttp_v1.post(callback, params)
}
