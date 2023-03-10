/**
 *  Web Dushboard
 *
 *  Copyright 2014 Alex Malikov
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
    name: "Web Dushboard",
    namespace: "625alex",
    author: "Alex Malikov",
    description: "Web Dushboard",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section("Allow control of these things...") {
        input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
        input "locks", "capability.lock", title: "Which Locks?", multiple: true, required: false
    }
    
    section("View state of these things...") {
        input "contacts", "capability.contactSensor", title: "Which Contact?", multiple: true, required: false
        input "presence", "capability.presenceSensor", title: "Which Presence?", multiple: true, required: false
    }
}

mappings {
    path("/data") {
		action: [
			GET: "list",
		]
	}
    path("/ui") {
		action: [
			GET: "html",
		]
	}
    path("/command") {
    	action: [
			GET: "command",
		]
    }
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
	if (!state.accessToken) {
    	createAccessToken()
    }
    subscribe(app, getURL)
    getURL(null)
}

def getURL(e) {
	def url = "https://graph.api.smartthings.com/api/smartapps/installations/${app.id}/ui?access_token=${state.accessToken}"
    log.debug "app url: $url"
}

def index() {
	["index", "list", "html"]
}

def list() {
	render contentType: "application/javascript", data: "${params.callback}(${data().encodeAsJSON()}})"
}

def data() {
    [
        switch: switches.collect{[type: "switch", id: it.id, name: it.displayName, status: it.currentValue('switch')]},
        lock: locks?.collect{[type: "lock", id: it.id, name: it.displayName, status: it.currentValue('lock') == "locked" ? "lock" : "unlock"]},
        contact: contacts.collect{[type: "contact", id: it.id, name: it.displayName, status: it.currentValue('contact')]},
        presence: presence.collect{[type: "presence", id: it.id, name: it.displayName, status: it.currentValue('presence')]},
    ]
}

def html() {
    render contentType: "text/html", data: "<!DOCTYPE html><html><head>${head()}</head><body>${body()}</body></html>"
}

def head() {
	"""
    <link rel="stylesheet" href="//code.jquery.com/ui/1.11.1/themes/smoothness/jquery-ui.css" />
    <link href="//maxcdn.bootstrapcdn.com/font-awesome/4.2.0/css/font-awesome.min.css" rel="stylesheet">
    <link href="//code.jquery.com/ui/1.11.1/themes/redmond/jquery-ui.css" rel="stylesheet">
	<script src="//code.jquery.com/jquery-2.1.1.min.js"></script>
	<script src="//code.jquery.com/ui/1.11.1/jquery-ui.min.js"></script>
	<style>
    	* {
        	font-family: Trebuchet MS,Helvetica,Arial,sans-serif;
            font-size:11px;
            text-align:center;
        }
		li {
            width: 150px;
            height: 150px;
            border: 1px solid lightgrey;
            display: inline-block;
            margin: 5px;
            padding: 5px;
            text-align: center;
            vertical-align: top;
    	}
        ul {
            margin: 0 auto;
            text-align: left;
            padding: 0;
        }
        .icon {
        	text-shadow: 0px 0px 5px grey;
            margin: 10px;
            color: #669999;
            position:relative;
            bottom: -5px;
            cursor: default;
        }
        .pointer {
        	cursor: pointer;
        }
        .active .icon {
            text-shadow: 0px 0px 10px orange; 
            color: #339933;
        }
        .name {
        	height: 2.2em;
            line-height: 1.1em;
            overflow: hidden;
        }
        .state {
            bottom: -25px;
        	position: relative;
        }
        .status {
        	border: none;
            line-height: 25px;
            background: none;
            font-weight: bold;
        }
	</style>
    <script>
        \$(function() {
        \$( ".state" ).buttonset();
        });
        var commandInProgress = false;
        function refresh() {
        	if (commandInProgress) return;
            
            commandInProgress = true;
            location.reload();
        }
        
        function sendCommand(id, command) {
        	if (commandInProgress) return;
            
            commandInProgress = true;
        	spin(id);
        	var url = "command/?" + window.location.href.slice(window.location.href.indexOf('?') + 1) + "&id=" + id + "&command=" + command;
            \$.getJSON(url + "&callback=?")
            .done(function( data ) {
            	if (data.status == "ok") {
                	commandInProgress = false;
                	setTimeout(function() { refresh(); }, 5000);
                } else {
                	alert("error");
                }
            })
            .fail(function( jqxhr, textStatus, error ) {
                var err = textStatus + ", " + error;
                alert( "Request Failed: " + err );
            });
        }
        
        function spin(id) {
        	\$("#" + id + "_icon").addClass("fa-spin");
        }
        
        function refreshMyself() {
        	setTimeout(function() {spin("refresh"); refresh(); }, 1000 * 60 * 2);
        }
        
        refreshMyself();
     </script>
    """
}


def command() {
	def response = [status: "ok"]
    def id = params.id
    def command = params.command
    log.debug "command received. id: $id, command: $command"
    switches?.each {
    	if (it.id == id) {
            if(command == "toggle") {
                if(it.currentValue('switch') == "on") {
                	it.off()
                } else {
                	it.on()
                }
            } else {
                it."$command"()
            }
        }
    }
    locks?.each {
    	if (it.id == id) {
            if(command == "toggle") {
                if(it.currentValue('lock') == "locked") {
                	it.unlock()
                } else {
                	it.lock()
                }
            } else {
                it."$command"()
            }
        }
    }
    
    render contentType: "application/javascript", data: "${params.callback}(${response.encodeAsJSON()})"
}

def body() {
	def content = """<form><ul>"""
    content = content + renderDevice([type: "refresh", id: "refresh", name: "Refresh", status: ""])
    data().each(){
    	it.value?.each() {
        	content = content + renderDevice(it)
        }
    }
    content + "</ul></form>"
}

def icons() {
	[
    	switch: [active: "on", classActive: "fa-power-off", classInactive: "fa-power-off", states:["off", "on"], toggle: true],
        dimmer: [active: "on", classActive: "fa-lightbulb-o", classInactive: "fa-lightbulb-o", states:["off", "10", "50", "75", "100"], toggle: true], 
        lock: [active: "lock", classActive: "fa-lock", classInactive: "fa-unlock-alt", states:["lock", "unlock"], toggle: true],
        contact: [active: "closed", classActive: "fa-square", classInactive: "fa-square-o"],
        presence: [active: "present", classActive: "fa-map-marker", classInactive: "fa-map-marker"],
        refresh: [active: "refresh", classActive: "fa-refresh", classInactive: "fa-refresh", toggle: true]
    ]
}

def renderDevice(device) {
	def isActive = icons()[device.type].active == device.status
	"""
    <li id="device_$device.id" class="device $device.type ${isActive ? "active" : "inactive"}">
    	<div class="name">$device.name</div>
        ${getIcon(device)}
        ${getStates(device)}
    </li>
    """
}

def getIcon(device) {
	def isActive = icons()[device.type].active == device.status
    """
    <span
    	class="icon fa ${icons()[device.type][isActive ? "classActive" : "classInactive"]} fa-5x ${icons()[device.type].toggle ? "pointer" : ""}" 
    	onclick="${onClick(device)} return false;" 
    	deviceid="$device.id"
        id="${device.id}_icon"
        ></span>
    """
    
}

def onClick(device) {
	if (device.type == "refresh") {
    	"spin('${device.id}'); refresh();"
    } else if (icons()[device.type].toggle) {
       "sendCommand('$device.id', 'toggle');"
    } else {
    	return ""
    }
}

def getStates(device) {
	def result = """<div class="state">"""
	if (icons()[device.type].states) {
    	icons()[device.type].states.each {
    		def checked = it == device.status ? """ checked="checked" """ : "unchecked"
        	result = result + """\n<input type="radio" id="${device.id + it}" name="$device.id" value="$it" $checked command="$it" onclick="sendCommand('$device.id', '$it');"/><label for="${device.id + it}">$it</label>\n"""
        }
    	
    } else {
    	def isActive = icons()[device.type].active == device.status
    	result = result + """<span class="status ui-state-${isActive ? "active" : "default"}">$device.status</span>"""
    }
    
    result + "</div>"
}
