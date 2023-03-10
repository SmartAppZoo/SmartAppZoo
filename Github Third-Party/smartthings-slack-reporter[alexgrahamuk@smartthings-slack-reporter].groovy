definition(
        name: "Smartthings Slack Reporter",
        author: "alex@a-graham.com",
        namespace: "alexgrahamuk",
        description: "Allows you to log various Smartthings events to slack.",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png",
        singleInstance: true
)

preferences {

	page(name: "page1", title: "Select contact sensors", uninstall: true)
    //page(name: "page2", title: "User Setup", uninstall: false, install: false)
    //page(name: "page3", title: "Edit User", uninstall: false, install: false)
    //page(name: "removePage", title: "Remove User", uninstall: false, install: false)
    //page(name: "doRemovePage", title: "User Removed", uninstall: false, install: false)
}

def page1() {

	dynamicPage(name: "page1", install: true, uninstall: true) {
    
        section {
            input(name: "keypads", type: "“capability.contactSensor", title: "Open Close Sensors", required: true, multiple: true)
        }
    
    }

}

/*
def page2() {

	dynamicPage(name: "page2") {
    
        section("Users") {

            for (int x=0; x<maxUsers; x++) {
                def p = [userID: x as String, smecker: "ih2"]
                href(name: "linkToPage3${x}", title: "Edit User $x (Bob)", required: false, page: "page3", params: p)
            }
            
        }
        
    }
    
}	

def page3(params) {

	log.debug("Params Page 3: $params")

	if (params.smecker) {
    	atomicState.params = params
    } else {
    	params = atomicState.params
    }

   	def uid = "userCode${params.userID}"
        
	log.debug("UID is: $uid")
    log.debug("Current Settings: $settings")

    dynamicPage(name: "page3", title: "Editing User $params.userID X (Bob)") {
    
        section {
        	input(name: "userName${params.userID}", type: "text", title: "User Name", defaultValue: settings."userName${params.userID}", required: true)
            input(name: "userCode${params.userID}", type: "text", title: "User Code", defaultValue: settings."userCode${params.userID}", required: true)
            input(name: "userIsTag${params.userID}", type: "bool", title: "RFID Tag?", defaultValue: settings."userIsTag${params.userID}", required: true)
            input(name: "userDisabled${params.userID}", type: "bool", title: "User Disabled", defaultValue: settings."userDisabled${params.userID}", required: true)
        }
        
        section {
        	href(name: "linkToRemovePage${params.userID}", title: "Remove User $x (Bob)", required: false, page: "removePage", params: params)
        }
        
    }
}


def removePage(params) {

	if (params.smecker) {
    	atomicState.params = params
    } else {
    	params = atomicState.params
    }
    
    
    dynamicPage(name: "removePage", title: "Remove User") {
    
        section {
        	href(name: "linkToDoRemovePage${params.userID}", title: "Remove User $x (Bob)", required: false, page: "doRemovePage", params: params)
            href(name: "linkBackToPage3${params.userID}", title: "Cancel", required: false, page: "page3", params: params)
        }
        
    }    

}
*/

/*
def doRemovePage(params) {

	log.debug("AAA: $params")

	if (params.smecker) {
    	atomicState.params = params
    } else {
    	params = atomicState.params
    }
    
    log.debug("BBB: $params")

    def x = params.userID
    
	if (!state.users)
    	state.users = [:]
   
    if (!state.users."user${x}")
    	state.users."user${x}" = [:]
    
	state.users."user${x}".requiresUpdate = true
    state.users."user${x}".deleted = true
    
    dynamicPage(name: "doRemovePage", title: "User Removed") {
    
        section {
        	paragraph "User has been removed."
            href(name: "linkBackToPage2${params.userID}", title: "Continue", required: false, page: "page2")
        }
        
    }    
}
*/

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}


def updated() {

    /*
	if (!state.users)
    	state.users = [:]

    for (int x=0; x<maxUsers; x++) {
    
    	if (!state.users."user${x}")
        	state.users."user${x}" = [:]
    
    	if (settings."userCode${x}" != state.users."user${x}".code)
        	state.users."user${x}".requiresUpdate = true
            
    	if (settings."userIsTag${x}" != state.users."user${x}".isTag)
        	state.users."user${x}".requiresUpdate = true
     
    }

	log.debug(state.users)
    log.debug "Updated with settings: ${settings}"
     */
    unsubscribe()
    initialize()
}

def initialize() {

	log.debug("init called")
    subscribe(keypads, "contactsensor.open", itOpened)
    //subscribe(keypads, "alarm.armed", armedHandler)
    //subscribe(keypads, "alarm.reallyArmed", reallyHandler)
}


/*
def updateCodes() {

    for (int x=0; x<maxUsers; x++) {
    
    	if (state.users."user${x}".requiresUpdate) {
        	keypads.setCode(x, settings."userCode${x}")
            state.users."user${x}".requiresUpdate = false
            state.users."user${x}".code = settings."userCode${x}"
        }
        
    }

}
*/

/*
def reallyHandler(evt) {
	log.debug("It really is armed")
    location.setMode("Away")
    location.setMode("Home")
}

def armedHandler(evt) {
	log.debug("Alarm Handler")
    runIn(Integer.valueOf(String.valueOf(evt.value)), armAlarm)
}

def armAlarm() {
	keypads.armAlarm()
}

 */

/*
def switchOnHandler(evt)
{
	log.debug(state.users)
    updateCodes()
    log.debug(state.users)
    
	//log.debug("Users Number Call")
	//keypads.reloadAllCodes()
}
*/


def itOpened(evt)
{
    log.debug(state.users)
    updateCodes()
    log.debug(state.users)

    //log.debug("Users Number Call")
    //keypads.reloadAllCodes()
}
