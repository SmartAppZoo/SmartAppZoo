/*  
 * LogicRulz - from the creators of EchoSistant    
 *
 *	05/30/2018		Version:2.0 R.0.0.3		Added integration with RemindR
 *	05/14/2018		Version:2.0 R.0.0.2		Added Running/Block count to main page
 *	04/27/2018		Version:2.0 R.0.0.1c	UI Changes
 *	04/26/2018		Version:2.0 R.0.0.1b	Code cleanup
 *	04/24/2018		Version:2.0	R.0.0.1a	Application name change
 *	04/23/2018		Version:2.0 R.0.0.1		Total rewrite
 *	04/20/2018		Version:1.0 R.0.0.3		UI Changes
 *	04/18/2018		Version:1.0 R.0.0.2		added ability to pause all routines from the parent
 *	04/13/2018		Version:1.0 R.0.0.1		Initital Release
 *
 *
 *  Copyright 2018 Jason Headley, Bobby Dobrescu, Jason Wise
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
/**********************************************************************************************************************************************/
definition(
    name		: "LogicRulz",
    namespace	: "Echo",
    author		: "JH/BD",
    description	: "A complicated rule engine with a simple interface",
    category	: "My Apps",
	iconUrl			: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/LogicRulz.png",
	iconX2Url		: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/LogicRulz2x.png",
	iconX3Url		: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/LogicRulz2x.png")

/**********************************************************************************************************************************************/
private def textVersion() {
	def text = "2.0"
}
private release() {
    def text = "Ver R.0.0.3 30-May-2018"
}
/**********************************************************************************************************************************************/
preferences {
    page(name: "main")
    page(name: "statusPage")
    page(name: "settingsPage")
    page(name: "weatherPage")
}
page name: "main"
def main() {
    dynamicPage (name: "main", title: "", install: true, uninstall: uninstalled) {  
        section("Create and Manage Logic Blockz") {
            href "blockzPage", title: "Manage Logic Blockz", description: mRoomsD(), state: mRoomsS(), image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/Blok.png" 
            }
        section("Logic Block Details",  uninstall: false){
            href "statusPage", title: "View the details of your Logic Blockz", description: mBlocksD(), state: mBlocksS(), image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/logic%20Blok.png"
            }
        section("Pause Logic Blockz") {
        	input "aPause", "bool", title: "Turn off this toggle to pause ALL Logic Blockz", defaultValue: true, submitOnChange: true, image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/pause.png"
        	}
        section("Settings") {
        	href "settingsPage", title: "Configure Settings", description: mSettingsD(), state: mSettingsS(), image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/settings.png"
            }
        section("Uninstall") {
        	href "uninstallPage", title: "Click here to remove $app.label", image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/uninstall.png"
            }
    }
}

page name: "settingsPage"
	def settingsPage() {
    	dynamicPage (name: "settingsPage", title: "Tap here to configure settings") {
        section("") {
            input "debug", "bool", title: "Enable Debug Logging", default: true, submitOnChange: true, image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/DeBug.jpg"
        	input "trace", "bool", title: "Enable Trace Logging", default: false, submitOnChange: true, image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/DeBug.jpg"
            paragraph "Debug logging is for normal use, Trace logging is for when we have a problem"
			}
        section("App Details") {
            paragraph "Parent App Version: ${textVersion()} | Release: ${release()}", image:"https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/LogicRulz%20Icons/LogicRulz.png"
        	}
        }    
	}
        
page name: "statusPage"
    def statusPage() {
    	dynamicPage (name: "statusPage", title: "You have created (${childApps?.size()}) Logic Blockz", install: true, uninstall: installed) {
    		section("Paused Logic Blockz"){
            paragraph runningAppsFalse()  
            }
            section("Running Logic Blockz") {
            paragraph runningAppsTrue()
            }
        }
    }    
page name: "blockzPage"
    def blockzPage() {
    	dynamicPage (name: "blockzPage", title: "You have created (${childApps?.size()}) Blockz", install: true, uninstall: installed) {
    		section(""){
            app(name: "Logic Blockz", appName: "LogicBlockz", title: "Create a new Logic Block", namespace: "Echo", displayChildApps: true, multiple: true,  uninstall: false)
            }
		}
    }    
page name: "uninstallPage"
    def uninstallPage() {
    	dynamicPage (name: "uninstallPage", title: "Clicking on the BIG RED BUTTON below will completely remove $app.label and all Routines!", install: true, uninstall: true) {
    		section("Please ensure you are ready to take this step, there is no coming back from the brink!"){
            }
		}
    }    

// METHOD FOR LOGIC BLOCK DETAILS - RUNNING APPS SECTION
private runningAppsTrue() { 
    def runBlock = []
    def children = getChildApps()
    children?.each { child ->
        if (child.rPause == true) {
            def running = (child?.appStatusTrue())
            state.theRunning = "$running "
            paragraph "${state.theRunning}" 
            String run  = (String) child
            runBlock += run
        }
    }
    	state.runBlock = runBlock.size()
        if (state.theRunning == null) {
        paragraph "All Logic Blocks are Currently Paused" 
        }
    return 
}

// METHOD FOR LOGIC BLOCK DETAILS - PAUSED APPS SECTION
private runningAppsFalse() {
	def pauseBlock = []
    def children = getChildApps()
    children?.each { child ->
        if (child.rPause == false) {
            def paused = (child?.appStatusFalse())
            state.thePaused = "$paused "
            paragraph "${state.thePaused}"
            String pause  = (String) child
            pauseBlock += pause
            }
        }    
        state.pauseBlock = pauseBlock.size()
        if (state.thePaused == null) {
        paragraph "All Logic Blocks are Currently Running" 
        }
    return 
}

// METHOD FOR LOGIC BLOCK DETAILS - RUNNING APPS COUNT
private runningAppsTrueUpdate() {
	def result
    def runBlock = []
    def children = getChildApps()
    children?.each { child ->
        if (child.rPause == true) {
            def running = (child?.appStatusTrue())
            state.theRunning = "$running "
            result = "${state.theRunning}" 
            String run  = (String) child
            runBlock += run
        }
    }
    	state.runBlock = runBlock.size()
        if (state.theRunning == null) {
        result = "All Logic Blocks are Currently Paused" 
        }
    return result
}

// METHOD FOR LOGIC BLOCK DETAILS - RUNNING APPS COUNT
private runningAppsFalseUpdate() {
	def result
	def pauseBlock = []
    def children = getChildApps()
    children?.each { child ->
        if (child.rPause == false) {
            def paused = (child?.appStatusFalse())
            state.thePaused = "$paused "
            result = "${state.thePaused}"
            String pause  = (String) child
            pauseBlock += pause
            }
        }    
        state.pauseBlock = pauseBlock.size()
        if (state.thePaused == null) {
        result = "All Logic Blocks are Currently Running" 
        }
    return result
}

/************************************************************************************************************
		Base Process
************************************************************************************************************/
def installed() {
	if (debug) log.debug "Installed with settings: ${settings}"
    log.debug "Parent App Version: ${textVersion()} | Release: ${release()}"
    initialize()
}
def updated() { 
	if (debug) log.debug "Updated with settings: ${settings}"
    log.debug "Parent App Version: ${textVersion()} | Release: ${release()}"
	unsubscribe()
    initialize()
}
def initialize() {
        //Other Apps Events	
        subscribe(location, "RemindRevent", remindRHandler)
        subscribe(location, "remindR", remindRProfiles)
        state.thePaused = null
        state.theRunning = null
        state.esEvent = [:]
        runningAppsFalseUpdate()
        runningAppsTrueUpdate()
        sendLocationEvent(name: "LogicRulz", value: "refresh", data: [blocks: getProfileList()] , isStateChange: true, descriptionText: "LogicRulz list refresh")		
        state.profiles = state.profiles ? state.profiles : []
        state.zones = state.zones ? state.zones : []
}

def OAuthToken(){
	try {
		createAccessToken()
		log.debug "Creating new Access Token"
	} catch (e) {
		log.error "Access Token not defined. OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth."
	}
}

/** Configure Profiles Pages **/
def mRoomsS(){
    def result = ""
    if (childApps?.size()) {
    	result = "complete"	
    }
    result
}
def mRoomsD() {
    def text = "No Profiles have been configured. Tap here to begin"
    def ch = childApps?.size()     
    if (ch == 1) {
        text = "One profile has been configured. Tap here to view and change"
    }
    else {
    	if (ch > 1) {
        text = "${ch} Profiles have been configured. Tap here to view and change"
     	}
    }
    text
}                     
def getProfileList(){
		return getChildApps()*.label
}
/** Configure Settings Pages **/
def mSettingsS(){
    def result = ""
    if (debug == true && trace == true) {
    	result = "complete"	
    }
    result
}
def mSettingsD() {
    def text = "Settings have not been configured. Tap here to begin"
    if ("$debug" == true) { debug = "Debug Logging is Active" }
    if ("$debug" == false) { debug = "Dubug Logging is not Active" }
    if ("$trace" == true) { trace = "Trace Logging is Active" }
    if ("$trace" == false) { trace = "Trace Logging is not Active" }
    	text = "Debug Logging is $debug \n" +
        	"Trace Logging is $trace "
}                     


def mBlocksS(){
    def result = ""
    if (state.runBlock > 0) {
    	result = "complete"	
    }
    result
}
def mBlocksD() {
    def text = "Logic Blockz have not been created. Tap here to begin"
    	text = "There are $state.runBlock running Logic Blockz \n" +
        	"and $state.pauseBlock paused Logic Blockz "
}                     

def listRemindRProfiles() {
log.warn "child requesting RemindR Profiles"
log.info "RemindR Profile List = $state.rProfiles"
	return state.profiles = state.profiles ? state.profiles : []
}

// INTEGRATION WITH REMINDR
def remindRHandler(evt) {
	log.info "event received from RemindR ==> $evt.descriptionText"
    	def children = getChildApps()
        def data = evt.descriptionText
        children.each {child ->
            if (child.label == data) {
            log.info "executing logic block $child.label"
                child.processActions(evt)
            	}
            }
        }
    
def remindRProfiles(evt) {
	def result
    log.warn "received Profiles List from RemindR: $evt.data"
	switch (evt.value) {
		case "refresh":
		state.profiles = evt.jsonData && evt.jsonData?.profiles ? evt.jsonData.profiles : []
			break
		case "runReport":
			def profiles = evt.jsonData
            	result = runReport(profiles)
            break	
    }
    return result
}

