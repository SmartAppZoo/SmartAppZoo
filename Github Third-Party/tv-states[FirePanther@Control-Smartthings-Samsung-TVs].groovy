definition(
    name: "suat",
    namespace: "suat",
    author: "suat",
    description: "control suats tvs",
    category: "My Apps",
    iconUrl: "https://su.at/i22",
    iconX2Url: "https://su.at/i22",
    oauth: [displayName: "suats tvs", displayLink: ""],
    usesThirdPartyAuthentication: true,
    pausable: false
)

preferences {
	section("suat wants to control your tvs") {
		input "tvs", "capability.switch", title: "Which TVs?", multiple: true, required: false
	}
}

mappings {
	path("/tv/:tv/:command") {
		action: [
			GET: "commandTv"
		]
	}
    
    path("/tv/:tv") {
		action: [
			GET: "statusTv"
		]
	}
    
    path("/tv") {
		action: [
			GET: "statusTvs"
		]
	}
}

def installed() {}

def updated() {}

def commandTv() {
    def tv = tvs?.find { it.label.toLowerCase() == params.tv.toLowerCase() }
    
    if (!tv) {
	    httpError(404, "TV not found: $tvs")
    }
    tv."$params.command"()
    render contentType: "text/plain", data: "switching $params.command"
}
def statusTv() {
    def tv = tvs?.find { it.label.toLowerCase() == params.tv.toLowerCase() }
    
    if (!tv) {
	    httpError(404, "TV not found: $tvs")
    }
    render contentType: "text/plain", data: tv.currentValue("switch")
}
def statusTvs() {
	def response = ""
    def status
    for (item in tvs) {
	    status = item.currentValue("switch")
    	response += "$item:$status;"
    }
    render contentType: "text/plain", data: response
}
