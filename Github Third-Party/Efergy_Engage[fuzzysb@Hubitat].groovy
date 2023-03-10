/**
 *  Efergy 2.0 (Connect)
 *
 *  Copyright 2015 Anthony S.
 *   ---------------------------
 */
 
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 
import groovy.time.TimeDuration

definition(
	name: "${textAppName()}",
    namespace: "${textNamespace()}",
    author: "${textAuthor()}",
    description: "${textDesc()}",
	category: "My Apps",
	iconUrl:   "https://dl.dropboxusercontent.com/s/daakzncm7zdzc4w/efergy_128.png",
	iconX2Url: "https://dl.dropboxusercontent.com/s/ysqycalevj2rvtp/efergy_256.png",
	iconX3Url: "https://dl.dropboxusercontent.com/s/56740lxra2qkqix/efergy_512.png",
    singleInstance: true)
    
    
def appName() { "Efergy 2.0 (Connect)" }
def appAuthor() { "Anthony S." }
def appNamespace() { "tonesto7" }
def appVersion() { "2.7.3" }
def appVerDate() { "4-26-2018" }

preferences {
	page(name: "startPage")
    page(name: "loginPage")
    page(name: "mainPage")
    page(name: "prefsPage")
	page(name: "hubInfoPage", content: "hubInfoPage", refreshTimeout:5)
    page(name: "readingInfoPage", content: "readingInfoPage", refreshTimeout:5)
    page(name: "infoPage")
    page(name: "savePage")
}

def startPage() {
	if(!state.appInstalled) { state.appInstalled = false }
	if(!state.showLogging) { state.showLogging = false }
    if (location.timeZone.ID.contains("America/")) { state.currencySym = "\$" }
    if (state.efergyAuthToken) { return mainPage() }
    else { return loginPage() }
}

/* Efergy Login Page */
def loginPage() {
    return dynamicPage(name: "loginPage", nextPage: mainPage, uninstall: false, install: false) {
    	section("Efergy Login Page") {
        	paragraph "Please enter your https://engage.efergy.com login credentials to generate you Authentication Token and install the device automatically for you."
			input("username", "email", title: "Username", description: "Efergy Username (email address)")
			input("password", "password", title: "Password", description: "Efergy Password")
            log.debug "login status: ${state.loginStatus} - ${state.loginDesc}"
            if (state.loginStatus != null && state.loginDesc != null && state.loginStatus != "ok") {
            	paragraph "${state.loginDesc}... Please try again!!!"
            }
		}
	}
}

/* Preferences */
def mainPage() {
	if (!state.efergyAuthToken) { getAuthToken() } 
    if (!state.pushTested) { state.pushTested = false }
    if (!state.currencySym) { state.currencySym = "\$" }
    getCurrency()
    def isDebug = state.showLogging ? true : false
    def notif = recipients ? true : false
    if (state.loginStatus != "ok") { return loginPage() }
    def showUninstall = state.appInstalled

	dynamicPage(name: "mainPage", uninstall: showUninstall, install: true) {
        if (state.efergyAuthToken) {
            section("Efergy Hub:") { 
        		href "hubInfoPage", title:"View Hub Info", description: "Tap to view more...", image: "https://dl.dropboxusercontent.com/s/amhupeknid6osmu/St_hub.png"
                href "readingInfoPage", title:"View Reading Data", description: "Last Reading: \n${state.readingUpdated}\n\nTap to view more...", image: "https://dl.dropboxusercontent.com/s/3wb351466vn4w99/power_meter.png"
        	}
			
            section("Preferences:") {
            	href "prefsPage", title: "App Preferences", description: "Tap to configure.\n\nDebug Logging: ${isDebug.toString().capitalize()}\nNotifications: ${notif.toString().capitalize()}", image: "https://dl.dropboxusercontent.com/s/2s3jvtlfrctdcsc/settings_icon.png" 
            }
			
            section(" ", mobileOnly: true) {
            	//App Details and Licensing Page
            	href "infoPage", title:"App Info and Licensing", description: "Name: ${textAppName()}\nCreated by: Anthony S.\n${textVersion()} (${textModified()})\nTimeZone: ${location.timeZone.ID}\nCurrency: ${getCurrency()}\n\nTap to view more...", 
                image: "https://dl.dropboxusercontent.com/s/daakzncm7zdzc4w/efergy_128.png"
            }
        }
        
        if (!state.efergyAuthToken) {
        	section() { 
            	paragraph "Authentication Token is Missing... Please login again!!!"
        		href "loginPage", title:"Login to Efergy", description: "Tap to loging..." 
        	}
        }
   	}
}

//Defines the Preference Page
def prefsPage () {
	dynamicPage(name: "prefsPage", install: false) {
    	section () {
        	paragraph "App and Locale Preferences", image: "https://dl.dropboxusercontent.com/s/2s3jvtlfrctdcsc/settings_icon.png" 
        }
        section("Currency Selection:"){	
           	input(name: "currencySym", type: "enum", title: "Select your Currency Symbol", options: ["\$", "£", "€"], defaultValue: "\$", submitOnChange: true, 
               	image: "https://dl.dropboxusercontent.com/s/7it48iosv1mzcl1/currency_icon.png")
           	state.currencySym = currencySym
        }
    	/*
        section("Notifications:"){	
            input("recipients", "contact", title: "Send notifications to", required: false, submitOnChange: true, image: "https://dl.dropboxusercontent.com/s/dbpk2ucn2huvj6f/notification_icon.png") {
            	input "phone", "phone", title: "Warn with text message (optional)", description: "Phone Number", required: false, submitOnChange: true
        	}
            if(recipients) { 
            	if((settings.recipients != recipients && recipients) || !state.pushTested) {
            		sendNotify("Push Notification Test Successful... Test is successful") 
            		state.pushTested = true
                }
                else { state.pushTested = true }
            }
            else { state.pushTested = false }
        }
        
		// Set Notification Recipients  
        if (location.contactBookEnabled && recipients) {
        	section("Notify Values...", hidden: true, hideable: true) { 
            	input "notifyAfterMin", "number", title: "Send Notification after (X) minutes of no updates", required: false, defaultValue: "60", submitOnChange: true
               	input "notifyDelayMin", "number", title: "Only Send Notification every (x) minutes...", required: false, defaultValue: "50", submitOnChange: true
               	state.notifyAfterMin = notifyAfterMin
               	state.notifyDelayMin = notifyDelayMin         
            }
        }
		*/
        
        section("Debug Logging:"){
            paragraph "This can help you when you are having issues with data not updating\n** This option generates alot of Log Entries!!! Only enable for troubleshooting **"
            paragraph "FYI... Enabling this also enables logging in the Child Device as well"
        	input "showLogging", "bool", title: "Enable Debug Logging", required: false, displayDuringSetup: false, defaultValue: false, submitOnChange: true, image: "https://dl.dropboxusercontent.com/s/nsxve4ciehlk3op/log_icon.png"
        	if(showLogging && !state.showLogging) { 
           		state.showLogging = true
           		log.info "Debug Logging Enabled!!!"
           	}
        	if(!showLogging && state.showLogging){ 
           		state.showLogging = false 
           		log.info "Debug Logging Disabled!!!"
           	}
        }
        refresh()
    }
}

def readingInfoPage () {
	if (!state.hubName) { refresh() }
	return dynamicPage(name: "readingInfoPage", install: false) {
 		section ("Efergy Reading Information") {
    		paragraph "Current Power Reading: " + state.powerReading
        	paragraph "Current Energy Reading: " + state.powerReading
        	paragraph "Tariff Rate: " + state.currencySym + state.tariffRate
        	paragraph "Today's Usage: " + state.currencySym + state.todayCost + " (${state.todayUsage} kWH"
        	paragraph "${state.monthName} Usage: " + state.currencySym + state.monthCost + " (${state.monthUsage} kWH"
        	paragraph "Month Cost Estimate: " + state.currencySym + state.monthBudget
        }
    }
}

def hubInfoPage () {
	if (!state.hubName) { refresh() }
	return dynamicPage(name: "hubInfoPage", install: false) {
 		section ("Efergy Hub Information") {
    		paragraph "Hub Name: " + state.hubName
        	paragraph "Hub ID: " + state.hubId
        	paragraph "Hub Mac Address: " + state.hubMacAddr
        	paragraph "Hub Status: " + state.hubStatus
        	paragraph "Hub Data TimeStamp: " + state.hubTsHuman
        	paragraph "Hub Type: " + state.hubType
        	paragraph "Hub Firmware: " + state.hubVersion
        }
    }
 }

//Defines the Help Page
def infoPage () {
	dynamicPage(name: "infoPage", install: false) {
		section() { 
        	paragraph "App Details and Licensing", image: "https://dl.dropboxusercontent.com/s/y2lcy6iho0dpsp5/info_icon.png"
        }
        
        section("About This App:") {
        	paragraph "Name: ${textAppName()}\nCreated by: Anthony S.\n${textVersion()}\n${textModified()}\nGithub: @tonesto7\n\n${textDesc()}", 
            	image: "https://dl.dropboxusercontent.com/s/daakzncm7zdzc4w/efergy_128.png"
        }
        
        section("App Revision History:") {
        	paragraph appVerInfo()
        }
        
		section("Licensing Info:") {
    		paragraph "${textCopyright()}\n${textLicense()}"
    	}
	}
}

/* Initialization */
def installed() { 
	state.appInstalled = true
	//sendNotificationEvent("${textAppName()} - ${appVersion()} (${appVerDate()}) installed...")
	log.info "${textAppName()} - ${appVersion()} (${appVerDate()}) installed..."
    initialize() 
}

def updated() { 
	if (!state.appInstalled) { state.appInstalled = true }
	//sendNotificationEvent("${textAppName()} - ${appVersion()} (${appVerDate()}) updated...")
	log.info "${textAppName()} - ${appVersion()} (${appVerDate()}) updated..."
	unsubscribe()
	initialize() 
}

def uninstalled() {
	unschedule()
	removeChildDevices(getChildDevices())
}
    
def initialize() {    
	refresh()
	addDevice()	
   	addSchedule()
    evtSubscribe()
}

def onAppTouch(event) {
	refresh()
}

//subscribes to the various location events and uses them to refresh the data if the scheduler gets stuck
private evtSubscribe() {
	subscribe(app, onAppTouch)
    subscribe(location, "sunrise", refresh)
	subscribe(location, "sunset", refresh)
	subscribe(location, "mode", refresh)
	subscribe(location, "sunriseTime", refresh)
	subscribe(location, "sunsetTime", refresh)
}

//Creates the child device if it not already there
private addDevice() {
	def dni = "Efergy Engage|" + state.hubMacAddr
    state.dni = dni
  	def d = getChildDevice(dni)
  	if(!d) {
    	d = addChildDevice("tonesto7", "Efergy Engage Elite 2.0", dni, null, [name:"Efergy Engage Elite", label: "Efergy Engage Elite", completedSetup: true])
    	d.take()
    	logWriter("Successfully Created Child Device: ${d.displayName} (${dni})")
  	} 
    else {
    	logWriter("Device already created")
  	}
}

private removeChildDevices(delete) {
	try {
    	delete.each {
        	deleteChildDevice(it.deviceNetworkId)
            log.info "Successfully Removed Child Device: ${it.displayName} (${it.deviceNetworkId})"
    		}
   		}
    catch (e) { logWriter("There was an error (${e}) when trying to delete the child device") }
}

//Sends updated reading data to the Child Device
def updateDeviceData() {
	logWriter(" ")
    logWriter("--------------Sending Data to Device--------------")
	getAllChildDevices().each { 
        it.updateStateData(state?.showLogging?.toString(), state?.monthName?.toString(), state?.currencySym?.toString())
    	it.updateReadingData(state?.powerReading?.toString(), state?.readingUpdated)
        it.updateTariffData(state?.tariffRate)
		it.updateUsageData(state?.todayUsage, state?.todayCost, state?.monthUsage, state?.monthCost, state?.monthEst, state?.monthBudget)
		it.updateHubData(state?.hubVersion, state?.hubStatus, state?.hubName)
	}
}

// refresh command
def refresh() {
	GetLastRefrshSec()
	if (state.efergyAuthToken) {
		if (state?.timeSinceRfsh > 30) {
        	logWriter("")	
			log.info "Refreshing Efergy Energy data from engage.efergy.com"
    
    		getDayMonth()
    		getReadingData()
 			getUsageData()
    		getHubData()
        	getTariffData()
    
    		//If any people have been added for notification then it will check to see if it should notify
    		if (recipients) { checkForNotify() }
   
    		updateDeviceData()
    		logWriter("")
            //runIn(30, "refresh")
        }
        else if (state?.timeSinceRfsh > 360 || !state?.timeSinceRfsh) { checkSchedule() }
    }
}

//Create Refresh schedule to refresh device data (Triggers roughly every 30 seconds)
private addSchedule() {
    schedule("0 0/1 * * * ? *", "refresh") //Runs every 1 minute to make sure that data is accurate
    //runIn(30, "refresh")
    //runIn(60, "refresh")
    runIn(130, "GetLastRefrshSec")
    //schedule(""0 0/1 * * * ?", "GetLastRefrshSec") //Runs every 1 minute to make sure that data is accurate
    runEvery5Minutes("checkSchedule")
}

def checkSchedule() {
	logWriter("Check Schedule has ran!")	
    GetLastRefrshSec()
    def timeSince = state.timeSinceRfsh ?: null 
    if (timeSince > 360) {
    	log.warn "It has been more than 5 minutes since last refresh!!!"
        log.debug "Scheduling Issue found... Re-initializing schedule... Data should resume refreshing in 30 seconds" 
        addSchedule()
        return
    }
    else if (!timeSince) {
    	log.warn "Hub TimeStamp Value was null..."
        log.debug "Re-initializing schedule... Data should resume refreshing in 30 seconds" 
        addSchedule()
        return
    }
}

// Get Efergy Authentication Token
private def getAuthToken() {
	def closure = { 
    	resp -> 
        log.debug("Auth Response: " + resp.data)  
        if (resp.data.status == "ok") { 
        	state.loginStatus = "ok"
            state.loginDesc = resp.data.desc
        	state.efergyAuthToken = resp.data.token       
        }
        else { 
        	state.loginStatus = resp.data.status
            state.loginDesc = resp.data.desc
           	return
        }
    }
	def params = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile/get_token",
        query: ["username": settings.username, "password": settings.password, "device": "website"],
        contentType: 'application/json'
    	]
	httpGet(params, closure)
    refresh()
}

//Converts Today's DateTime into Day of Week and Month Name ("September")
def getDayMonth() {
	def sdf = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy")
    def now = new Date()
    def month = new SimpleDateFormat("MMMM").format(now)
    def day = new SimpleDateFormat("EEEE").format(now)
   
    if (month && day) {
    	state.monthName = month
        state.dayOfWeek = day
    } 
}

def getCurrency() {
	def unitName = ""
	switch (state.currencySym) {
    	case '$':
        	unitName = "US Dollar (\$)"
            state.centSym = "¢" 
        break
        case '£':
        	unitName = "British Pound (£)"
            state.centSym = "p"
        break
        case '€':
        	unitName = "Euro Dollar (€)"
            state.centSym = "¢" 
        break
    	default:
        	unitName = "unknown"
            state.centSym = "¢"
        break
    }
    return unitName
}

//Checks for Time passed since last update and sends notification if enabled
def checkForNotify() {
    if(!state.notifyDelayMin) { state.notifyDelayMin = 50 }
    if(!state.notifyAfterMin) { state.notifyAfterMin = 60 }
    //logWriter("Delay X Min: " + state.notifyDelayMin)
    //logWriter("After X Min: " + state.notifyAfterMin)
    def delayVal = state.notifyDelayMin * 60
    def notifyVal = state.notifyAfterMin * 60
    def timeSince = GetLastRefrshSec()
    
    if ((state.lastNotifySeconds == null && state.lastNotified == null) || (state.lastNotifySeconds == null || state.lastNotified == null)) {
    	state.lastNotifySeconds = 0
        state.lastNotified = "Mon Jan 01 00:00:00 2000"
        logWriter("Error getting last Notified: ${state.lastNotified} - (${state.lastNotifySeconds} seconds ago)")
        return
    }
    
    else if (state.lastNotifySeconds && state.lastNotified) {
    	state.lastNotifySeconds = GetTimeDiffSeconds(state.lastNotified)
        logWriter("Last Notified: ${state.lastNotified} - (${state.lastNotifySeconds} seconds ago)")
    }

	if (timeSince > delayVal) {
        if (state.lastNotifySeconds < notifyVal){
        	logWriter("Notification was sent ${state.lastNotifySeconds} seconds ago.  Waiting till after ${notifyVal} seconds before sending Notification again!")
            return
        }
        else {
        	state.lastNotifySeconds = 0
        	NotifyOnNoUpdate(timeSince)
        }
    }
}

//Sends the actual Push Notification
def NotifyOnNoUpdate(Integer timeSince) {
	def now = new Date()
	def notifiedDt = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy")
    state.lastNotified = notifiedDt.format(now)
	    
    def message = "Something is wrong!!! Efergy Device has not updated in the last ${timeSince} seconds..."
	sendNotify(message)
}

private def sendNotify(msg) {
	if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    } else {
        logWriter("contact book not enabled")
        if (phone) {
            sendSms(phone, msg)
        }
    }
}

def GetLastRefrshSec() {
	state.timeSinceRfsh = GetTimeDiffSeconds(state.hubTsHuman)
    logWriter("TimeSinceRefresh: ${state.timeSinceRfsh} seconds")
    runIn(130, "GetLastRefrshSec")
}

//Returns time difference is seconds 
def GetTimeDiffSeconds(String startDate) {
	try {
		def now = new Date()
    	def startDt = new SimpleDateFormat("EE MMM dd HH:mm:ss yyyy").parse(startDate)
    	def diff = now.getTime() - startDt.getTime()  
    	def diffSeconds = (int) (long) diff / 1000
    	//def diffMinutes = (int) (long) diff / 60000
    	return diffSeconds
    }
    catch (e) {
    	log.debug "Exception in GetTimeDiffSeconds: ${e}"
        return 10000
    }
}


//Matches hubType to a full name
def getHubName(String hubType) {
	def hubName = ""
    switch (hubType) {
   		case 'EEEHub':
       		hubName = "Efergy Engage Elite Hub"
       	break
        default:
       		hubName "unknown"
	}
    state.hubName = hubName
}

// Get extended energy metrics
private def getUsageData() {
	try {
	def estUseClosure = { 
        estUseResp -> 
            //Sends extended metrics to tiles
            state.todayUsage = "${estUseResp?.data?.day_kwh.estimate}"
            state.todayCost = "${estUseResp?.data?.day_tariff?.estimate}"
            state.monthUsage = "${estUseResp?.data?.month_kwh?.previousSum}"
            state.monthCost = "${estUseResp?.data?.month_tariff?.previousSum}"
            state.monthEst = "${estUseResp?.data?.month_tariff?.estimate}"
            state.monthBudget = "${estUseResp?.data?.month_budget}"
            
            //Show Debug logging if enabled in preferences
            logWriter(" ")
            logWriter("-------------------ESTIMATED USAGE DATA-------------------")
            logWriter("Http Usage Response: ${estUseResp?.data}")
            logWriter("TodayUsage: Today\'s Usage: ${state?.currencySym}${estUseResp?.data?.day_tariff?.estimate} (${estUseResp?.data?.day_kwh?.estimate} kWh)")
            logWriter("MonthUsage: ${state?.monthName} Usage ${state?.currencySym}${estUseResp?.data?.month_tariff?.previousSum} (${estUseResp?.data.month_kwh?.previousSum} kWh)")
            logWriter("MonthEst: ${state?.monthName}\'s Cost (Est.) ${state?.currencySym}${estUseResp?.data?.month_tariff?.estimate}")
            logWriter("${state?.monthName}\'s Budget ${state?.currencySym}${estUseResp?.data?.month_budget}")
		}
        
	def params = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile_proxy/getEstCombined",
        query: ["token": state.efergyAuthToken],
        contentType: 'application/json'
    	]
	httpGet(params, estUseClosure)
    }
    catch (e) { log.error "getUsageData Exception: ${e}" }
}

// Get tariff energy metrics
private def getTariffData() {
	try {
		def tariffClosure = { 
        	tariffResp -> 
        		def tariffRate = tariffResp?.data?.tariff?.plan?.plan?.planDetail?.rate.toString().replaceAll("\\[|\\{|\\]|\\}", "")
                
            	//Sends extended metrics to tiles
            	state.tariffRate = "${tariffRate}${state.centSym}"
            	
            	//Show Debug logging if enabled in preferences
            	logWriter(" ")
            	logWriter("-------------------TARIFF RATE DATA-------------------")
                logWriter("Tariff Rate: ${state.tariffRate}")
         	}
                
		def params = [
    		uri: "https://engage.efergy.com",
    		path: "/mobile_proxy/getTariff",
        	query: ["token": state.efergyAuthToken],
        	contentType: 'application/json'
    		]
		httpGet(params, tariffClosure)
    }
    catch (e) { log.error "getTariffData Exception: ${e}" }
}
 
/* Get the sensor reading
****  Json Returned: {"cid":"PWER","data":[{"1440023704000":0}],"sid":"123456","units":"kWm","age":142156},{"cid":"PWER_GAC","data":[{"1440165858000":1343}],"sid":"123456","units":null,"age":2}
*/
private def getReadingData() {
	try {
    	def today = new Date()
    	def tf = new SimpleDateFormat("MMM d,yyyy - h:mm:ss a")
    		tf.setTimeZone(location?.timeZone)
        def tf2 = new SimpleDateFormat("MMM d,yyyy - h:mm:ss a")
    	def cidVal = "" 
		def cidData = [{}]
    	def cidUnit = ""
    	def timeVal
		def cidReading
    	def cidReadingAge
    	def readingUpdated
    	def summaryClosure = { summaryResp -> 
        	def respData = summaryResp?.data.text
            
            //Converts http response data to list
			def cidList = new JsonSlurper().parseText(respData)
			
            //Search through the list for age to determine Cid Type
			for (rec in cidList) { 
    			if (rec.age || rec?.age == 0) { 
               		cidVal = rec?.cid 
        			cidData = rec?.data
               		cidReadingAge = rec?.age
               		if(rec?.units != null)
                   		cidUnit = rec?.units
        			break 
     			}
			}
            
 			//Convert data: values to individual strings
			for (item in cidData[0]) {
     			timeVal =  item?.key
    			cidReading = item?.value
			}
            
        	//Converts timeVal string to long integer
        	def longTimeVal = timeVal?.toLong()

			//Save Cid Type to device state
			state.cidType = cidVal
            
        	//Save Cid Unit to device state
        	state.cidUnit = cidUnit
            
        	//Formats epoch time to Human DateTime Format
        	if (longTimeVal) { 
            	readingUpdated = "${tf.format(longTimeVal)}"
                //log.debug "Timezone Formatted Time: ${readingUpdated} | Raw API Formatted Time: ${tf2.format(longTimeVal)}"
            }

			//Save last Cid reading value to device state
        	if (cidReading) {
        		state.powerReading = cidReading	
            	state.energyReading = cidReading.toInteger() 
        	}

			//state.powerVal = cidReading
        	state.readingUpdated = "${readingUpdated}"
            state.readingDt = readingUpdated
            
			//Show Debug logging if enabled in preferences
        	logWriter(" ")	
        	logWriter("-------------------USAGE READING DATA-------------------")
        	logWriter("HTTP Status Response: " + respData)	/*<------Uncomment this line to log the Http response */
			logWriter("Cid Type: " + state.cidType)
        	logWriter("Cid Unit: " + cidUnit)
        	logWriter("Timestamp: " + timeVal)
        	logWriter("reading: " + cidReading)
        	logWriter("Last Updated: " + readingUpdated)
        	logWriter("Reading Age: " + cidReadingAge)
        	logWriter("Current Month: ${state.monthName}")
        	logWriter("Day of Week: ${state.dayOfWeek}")
    	}
        
		def summaryParams = [
    		uri: "https://engage.efergy.com",
    		path: "/mobile_proxy/getCurrentValuesSummary",
        	query: ["token": state.efergyAuthToken],
        	contentType: "json"]
            
		httpGet(summaryParams, summaryClosure)
    }
    catch (e) { 
    	log.error "getReadingData Exception: ${e}" 
    }
}

// Returns Hub Device Status Info 
private def getHubData() {
	def hubId = ""
    def hubMacAddr = ""
    def hubStatus = ""
    def hubTsHuman
    def hubType = ""
    def hubVersion = ""
    def statusList
    def getStatusClosure = { statusResp ->  
        	def respData = statusResp?.data.text
            //Converts http response data to list
			statusList = new JsonSlurper().parseText(respData)
			
           	hubId = statusList?.hid
            hubMacAddr = statusList?.listOfMacs.mac
    		hubStatus = statusList?.listOfMacs.status
    		hubTsHuman = statusList?.listOfMacs.tsHuman
    		hubType = statusList?.listOfMacs.type
    		hubVersion = statusList?.listOfMacs.version
            
            //Save info to device state store
            state.hubId = hubId
            state.hubMacAddr = hubMacAddr.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubStatus = hubStatus.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubTsHuman = hubTsHuman.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubType = hubType.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubVersion = hubVersion.toString().replaceAll("\\[|\\{|\\]|\\}", "")
            state.hubName = getHubName(hubType)
			
            //Show Debug logging if enabled in preferences
            logWriter(" ")	
            logWriter("-------------------HUB DEVICE DATA-------------------")
            //logWriter("HTTP Status Response: " + respData)
            logWriter("Hub ID: " + state.hubId)
            logWriter("Hub Mac: " + state.hubMacAddr)
            logWriter("Hub Status: " + state.hubStatus)
            logWriter("Hub TimeStamp: " + state.hubTsHuman)
            logWriter("Hub Type: " + state.hubType)
            logWriter("Hub Firmware: " + state.hubVersion)
            logWriter("Hub Name: " + state.hubName)
    }
    //Http Get Parameters     
	def statusParams = [
    	uri: "https://engage.efergy.com",
    	path: "/mobile_proxy/getStatus",
        query: ["token": state.efergyAuthToken],
        contentType: 'json'
    ]
	httpGet(statusParams, getStatusClosure)
}    

//Log Writer that all logs are channel through *It will only output these if Debug Logging is enabled under preferences
private def logWriter(value) {
	if (state.showLogging) {
        log.debug "${value}"
    }	
}

/******************************************************************************  
*				Application Help and License Info Variables					  *
*******************************************************************************/
private def textAppName() 	{ def text = "${appName()}" }	
private def textVersion() 	{ def text = "Version: ${appVersion()}" }
private def textModified() 	{ def text = "Updated: ${appVerDate()}" }
private def textAuthor() 	{ def text = "${appAuthor()}" }
private def textNamespace() { def text = "${appNamespace()}" }
private def textVerInfo() 	{ def text = "${appVerInfo()}" }
private def textCopyright() { def text = "Copyright© 2015 - Anthony S." }
private def textDesc() 		{ def text = "${appDesc()}" }
private def textHelp() 		{ def text = "You can enable 'Debug Logging' if you are encountering issues." }
private def textLicense() 	{ def text =
		"Licensed under the Apache License, Version 2.0 (the 'License'); "+
		"you may not use this file except in compliance with the License. "+
		"You may obtain a copy of the License at"+
		"\n\n"+
		"    http://www.apache.org/licenses/LICENSE-2.0"+
		"\n\n"+
		"Unless required by applicable law or agreed to in writing, software "+
		"distributed under the License is distributed on an 'AS IS' BASIS, "+
		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
		"See the License for the specific language governing permissions and "+
		"limitations under the License."
}
//Application Description
def appDesc() { "This app will connect to the Efergy Servers and generate a token as well as create the energy device automatically for you.  After that it will manage and update the device info about every 30 seconds" }
//Adds version changes to info page
def appVerInfo() {	
    "v2.7.3 (Apr 27th, 2018)\n" +
    "Minor code changes to work with Hubitat\n\n"+
        
    "v2.7.2 (Feb 18th, 2016)\n" +
    "Minor code optimizations\n"+
    "Changed scheduler to use runIn (works well)\n"+
    "Add in debug log entry to show people with timezone issue what timestamp they are receiving from Efergy API\n\n" +
    
	"v2.7.1 (Jan 8th, 2016)\n" +
 	"Added in logic to stop push notifications everytime you enter app preferences\n"+
    "Fixed debug logging require you to go back into setting a second time to actual update the changes\n"+
    "Minor code cleanups\n\n"+

	"v2.7.0 (Dec 11th, 2015)\n" +
    "Reworked alot of the code that sends the data to the device...\n\n" +  
 	
	"v2.6.1 (Dec 8th, 2015)\n" +
    "Fixed Tariff data to change currency symbol based on selected currency\n\n" +  

	"v2.6.0 (Nov 11th, 2015)\n" +
 	"Optimized Scheduling and Added AppTouch Button to quickly refresh device from SmartApp List"+
    "Fixed Random Errors received on reading data\n\n" +  
 	
	"v2.5.1 (Nov 2nd, 2015)\n" +
 	"Fixed Duplicate scheduling issue\n\n" +  
 	
	"v2.5.0 (Oct 26th, 2015)\n" +
 	"Restructured the main page layout of the smart app and icons\n" +  
 	"Added Currency Units (If TimeZone is America the Unit is automatically '\$'... If your not in America you can change it in preferences)\n\n" + 
 	
    "v2.4.0 (Oct 19th, 2015)\n" +
 	"Updated the code to handle bad authentication events\n\n" +
    
 	"v2.3.0 (Oct 1st, 2015)\n" +
	"Added the new single instance only platform feature. to prevent multiple installs of this service manager\n" +
    "--------------------------------------------------------------"
}