/**
 *  Falcon Player Pro
 */
definition(
    name: "Falcon Player Pro",
    namespace: "kmorey",
    author: "Kevin Morey",
    description: "Basic control of FPP playlists",
    category: "My Apps",
    iconUrl: "https://falconchristmas.com/images/Site%20LogoTransparent%204%20forum.png",
    iconX2Url: "https://falconchristmas.com/images/Site%20LogoTransparent%204%20forum.png",
    iconX3Url: "https://falconchristmas.com/images/Site%20LogoTransparent%204%20forum.png",
    singleInstance: true) {
}

preferences {
	page(name: "MainSetup")
}

def MainSetup() {
	dynamicPage(name: "MainSetup", title: "Falcon Player Pro", install: true, uninstall: true) {
	    section('FPP Server') {
		  input('ip', 'string', title: 'IP Address', description: 'The IP of your FPP server', required: true)
        }
    }
}

mappings {
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    def children = getChildDevices()
    if (children.size() == 0) {
        addChildDevice("kmorey", "Falcon Player Pro Controller", "fpp_controller", null, [name: "Christmas Lights", label:"Christmas Lights", completedSetup: true])
    }
}

def fetchPlaylists() {
    try
    {
        fppXmlRequest("getPlayLists", [], getPlaylistsHandler)
    }
    catch (errorException)
    {
        log.error "Caught exception [${errorException}] while attempting to retreive the playlists."
    }
}

def getStatus() {
    try
    {
        fppXmlRequest("getFPPstatus", [], getStatusHandler)
    }
    catch (errorException)
    {
        log.error "Caught exception [${errorException}] while attempting to retreive the status."
    }
}

void getPlaylistsHandler(physicalgraph.device.HubResponse hubResponse) {
    def children = getChildDevices()
    
    def body = hubResponse.xml
    def playlists = []
    body.children().each {
        playlists.add(it.text())
    }
    
    children[0].getPlaylistsHandler(playlists)
}

void getStatusHandler(physicalgraph.device.HubResponse hubResponse) {
    def children = getChildDevices()
    
    def body = hubResponse.xml
    def status = [:]
    status["currentPlaylist"] = body.CurrentPlaylist.text()
    
    children[0].getStatusHandler(status)
}

def fppXmlRequest(command, args = [], handler = null) {
    def params = [
        method: 'GET',
        path: "/fppxml.php",
        query: ["command": command] + args,
        headers: [HOST: "${settings.ip}:80"]
    ]
    def result = new physicalgraph.device.HubAction(params, null, [callback: handler])
    sendHubCommand(result)
}

def fppJsonRequest(command, handler) {
    def params = [
        method: 'GET',
        path: "/fppjson.php",
        query: ["command": command],
        headers: [HOST: "${settings.ip}:80"]
    ]
    def result = new physicalgraph.device.HubAction(params, null, [callback: handler])
    sendHubCommand(result)
}

def startPlaylist(playlist) {
	fppXmlRequest('stopNow')
	fppXmlRequest('startPlaylist', ["playList": playlist, "repeat": "checked"])
}

def stopEverything() {
	fppXmlRequest('stopNow')
}