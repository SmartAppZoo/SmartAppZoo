/**
 *  SmartThingsSleepIQ
 *
 *  Copyright 2021 Jonathan Wentz
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
    name: "SmartThingsSleepIQ",
    namespace: "jrwentz",
    author: "Jonathan Wentz",
    description: "Provides control and presence for a Sleep Number bed",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    //appSetting "username"
    //appSetting "password"
}


preferences {
	//section("Title") {
		// TODO: put inputs here
	//}
    page(name: "mainPage", title: "Sleep Number SleepIQ")
    page(name: "authPage", title: "SleepIQ Login")
    page(name: "authResultsPage", title: "SleepIQ Login Status")
    page(name: "bedSelectPage", title: "SleepIQ Bed Selection")
    page(name: "newBedPage", title: "SleepIQ Bed Configuration")
    page(name: "createDevicesPage", title: "SleepIQ Device Creation")
    page(name: "configureBedPage", title: "SleepIQ Bed Configuration")
}

//////////////////////////////////
// Preference Pages
//////////////////////////////////

def mainPage() {
	//TODO: Check if logged in
    //TODO: Route to Login Page
    
    def loggedIn = isLoggedIn()
    def statusMessage = ""
    if(!loggedIn){
        statusMessage = "Status: Not Authenticated"
        state.lastPage = "mainPage"
        return authPage()
    }
    else {
        statusMessage = "Status: Authenticated"
    }
    
    state.lastPage = "mainPage"
    
    dynamicPage(name: "mainPage", nextPage: "", uninstall: true, install: true) {
        def beds = refreshBedStatus()

        section("App Info") {
            paragraph "Sleep Number SmartIQ Integration\nAuthor: Jonathan Wentz"
        }

        section("SmartIQ Account Information") {

            paragraph "Username: ${settings.username}\n" + statusMessage
            //TODO: Allow Log Out

        }

		/*
        section("Beds") {
        	if(beds) {
            	paragraph title: "Connected Beds", "Found $beds.size bed(s)"
            } else {
            	paragraph title: "Connected Beds", "No Beds Connected"
            }
        }
        */
    }
}

def authPage() {
	log.trace "[SmartThingsSleepIQ:authPage] "
	state.installMessage = ""
    state.beds = []
    state.rightSleeperName = null
    state.leftSleeperName = null
	return dynamicPage(name: "authPage", title: "Connect to SleepIQ", nextPage:"authResultsPage", uninstall:false, install: false, submitOnChange: true) {
		section("Login Credentials"){
			input("username", "email", title: "Username", description: "SmartIQ Username (email address)")
			input("password", "password", title: "Password", description: "SmartIQ password")
		}
	}
}

def authResultsPage() {
	log.trace "[SmartThingsSleepIQ:authResultsPage] Login result next page: ${state.lastPage}"
    if(login()) {
    	return bedSelectPage()
        //return configureBedPage()
    } else {
    	return dynamicPage(name: "authResultsPage", title: "Login Error", install:false, uninstall:false) {
			section(""){
				paragraph "The username or password you entered is incorrect. Go back and try again. "
			}
		}
	}
}

//TODO: Get Bed Data
//TODO: Get Sleepers
//TODO: Swap to Params

def bedSelectPage() {
	log.trace "[SmartThingsSleepIQ:bedSelectPage]"
    state.beds = getBeds()
    
    log.debug "[SmartThingsSleepIQ:bedSelectPage] Found $state.beds.size bed(s)"
    
    dynamicPage(name: "bedSelectPage") {
    	if(state.beds.size > 0) {
        	state.beds.each { bed ->
            	section("Bed: $bed.name") {
                	state.bedId = bed.bedId //HACK: Temporary
                    paragraph "Size: $bed.size\nModel: $bed.model"
                	href page: "newBedPage", title: "Select This Bed", params: [bedId: bed.bedId]
                }
            }
        } else {
        	section {
            	paragraph "No beds found"
            }
        }
    }
}

def newBedPage(params) {
	log.trace "[SmartThingsSleepIQ:newBedPage] params: ${params}"
    
    settings.bedName = null
    settings.rightSleeperName = null
    settings.leftSleeperName = null
    settings.bedId = params.bedId
    if(!settings.bedId){
    	settings.bedId = state.bedId
    }
    log.trace "[SmartThingsSleepIQ:newBedPage] Looking up bed: $settings.bedId"
    
    //state.beds = getBeds()
    def bed = state.beds.find { it.bedId == settings.bedId }
    if(bed) {
        log.trace "[SmartThingsSleepIQ:newBedPage] Found bed: $bed"

        def sleepers = getSleepers()
        def rightSleeperName = sleepers[bed.sleeperRightId].firstName
        def leftSleeperName = sleepers[bed.sleeperLeftId].firstName
        log.trace "[SmartThingsSleepIQ:newBedPage] Right: $rightSleeperName, Left: $leftSleeperName"

        dynamicPage(name: "newBedPage", nextPage: "createDevicesPage") {
            section ("Bed: $bed.name") {
                paragraph "Bed ID: $bed.bedId\nSize: $bed.size\nModel: $bed.model"
                input "bedName", "text", title: "Bed Name", description: "What is the name of this bed?", defaultValue: bed.name, required: true
                input "rightSleeperName", "text", title: "Right Side", description: "Who sleeps on the right side of the bed?", defaultValue: rightSleeperName, required: true
                input "leftSleeperName", "text", title: "Left Side", description: "Who sleeps on the left side of the bed?", defaultValue: leftSleeperName, required: true
            }
        }
    } else {
    	log.error "[SmartThingsSleepIQ:newBedPage] Could not find bed: $settings.bedId"
    }
}

def createDevicesPage() {
	log.trace "[SmartThingsSleepIQ:createDevicePage]"
    
    def bedId = state.bedId
    log.debug "Using BedId: $bedId"
	def rightDeviceId = "sleepiq." + bedId + ".right"
	def leftDeviceId = "sleepiq." + bedId + ".left"
    
    //TODO: Create Child Devices
    log.debug "[SmartThingsSleepIQ:createDevicePage] Creating SleepIQ on $settings.bedName Right Side: $rightDeviceId for $settings.rightSleeperName"
    //def rightDevice = addChildDevice(
    
    log.debug "[SmartThingsSleepIQ:createDevicePage] Creating SleepIQ on $settings.bedName Left Side: $leftDeviceId for $settings.leftSleeperName"
    
    
    
    
    settings.rightSleeperName = null
    settings.leftSleeperName = null
    settings.bedName = null
    settings.bedId = null
    
    mainPage()
}


//////////////////////////////////
// Installation
//////////////////////////////////

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
	// TODO: subscribe to attributes, devices, locations, etc.
}

// TODO: implement event handlers



//////////////////////////////////
// API Calls
//////////////////////////////////
private def ApiHost() { "prod-api.sleepiq.sleepnumber.com" }
private def ApiUriBase() { "https://prod-api.sleepiq.sleepnumber.com/rest/" }
private def ApiUserAgent() { "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.95 Safari/537.36" }


private def login() {
  log.debug "[SmartThingsSleepIQ:login] Logging in..."
  state.session = null
  
  try {
    def loginParams = [
      uri: ApiUriBase() + 'login',
      headers: [
        'Content-Type': 'application/json;charset=UTF-8',
        'Host': ApiHost(),
        'User-Agent': ApiUserAgent(),
        'DNT': '1',
      ],
      body: '{"login":"' + settings.username + '","password":"' + settings.password + '"}='
    ]
    httpPut(loginParams) { response ->
      if (response.status == 200) {
        log.trace "[SmartThingsSleepIQ:login] Login was successful"
        state.session = [:]
        state.session.key = response.data.key
        state.session.cookies = ''
        response.getHeaders('Set-Cookie').each {
          state.session.cookies = state.session.cookies + it.value.split(';')[0] + ';'
        }
        log.info "[SmartThingsSleepIQ:login] Login successful"
        return true
      } else {
        log.error "[SmartThingsSleepIQ:login] Login failed: ($response.status) $response.data"
        state.session = null
        return false
      }
    }
  } catch(Exception e) {
    log.error "[SmartThingsSleepIQ:login] Login failed: Error ($e)"
    state.session = null
    return false
  }
}


private def isLoggedIn() {
	log.debug "[SmartThingsSleepIQ:isLoggedIn] Determining if the user needs to log in"
	if(!state.session || !state.session?.key){
    	log.trace "[SmartThingsSleepIQ:isLoggedIn] Session missing"
		return false
	}
    
    if(getBedStatus()) {
    	log.trace "[SmartThingsSleepIQ:isLoggedIn] Successfully refreshed Bed Status, so login is working"
    	return true
    } else {
    	log.trace "[SmartThingsSleepIQ:isLoggedIn] Could not refresh Bed Status, so we need to login again"
    	return false
    }
}


def refreshBedStatus() {
	if(!isLoggedIn()) {
    	login()
    }
    
    log.debug "[SmartThingsSleepIQ:refreshBedStatus] Refreshing Beds Status..."
    def beds = getBedStatus()
    
    if(beds.size > 0) {
    	return beds
    } else {
    	return []
    }
}


def getBedStatus() {
	log.trace "[SmartThingsSleepIQ:getBedStatus]"

	def bedStatus = []
    def results = getSleepIqRestApi("bed/familyStatus")
    
    if(results?.beds) {
    	log.trace "[SmartThingsSleepIQ:getBedStatus] Status for $results.beds.size bed(s)"
    	bedStatus = results.beds
    }
    
    return bedStatus
}


def getBeds() {
	log.trace "[SmartThingsSleepIQ:getBeds]"
    
    def beds = []
    def results = getSleepIqRestApi("bed")
    
    if(results?.beds) {
    	log.debug "[SmartThingsSleepIQ:getBeds] Found $results.beds.size bed(s)"
        beds = results.beds
    }
    
    return beds
}


def getSleepers() {
	log.trace "[SmartThingsSleepIQ:getSleepers]"
    
    def sleepers = [:]
    def results = getSleepIqRestApi("sleeper")
    
    if(results?.sleepers) {
    	log.debug "[SmartThingsSleepIQ:getSleepers] Found $results.sleepers.size sleepers(s)"
        results.sleepers.each { sleeper ->
        	log.trace "[SmartThingsSleepIQ:getSleepers] Loading sleeper data for $sleeper.firstName ($sleeper.sleeperId)"
        	sleepers[sleeper.sleeperId] = sleeper
        }
    }
    
    return sleepers
}


def getSleepIqRestApi(String uri) {
	//log.trace "[SmartThingsSleepIQ:getSleepIqRestApi]"
    def apiResults = [:]
    
    try {
    	def apiParams = [
        	uri: ApiUriBase() + uri + '?_k=' + state.session?.key,
			headers: [
				'Content-Type': 'application/json;charset=UTF-8',
                'Host': ApiHost(),
                'User-Agent': ApiUserAgent(),
                'Cookie': state.session?.cookies,
                'DNT': '1'
            ]
        ]
        
        httpGet(apiParams) { response -> 
			if (response.status == 200) {
            	//log.trace "[SmartThingsSleepIQ:getSleepIqRestApi] GET $uri successful: $response.data"
				apiResults = response.data
			} else {
            	log.error "[SmartThingsSleepIQ:getSleepIqRestApi] Error GET $uri, REST request unsuccessful: ($response.status): $response.data"
			}
        }
    } catch (Exception ex) {
    	log.error "[SmartThingsSleepIQ:getSleepIqRestApi] Error GET $uri: $ex"
    }
    
    return apiResults
}
