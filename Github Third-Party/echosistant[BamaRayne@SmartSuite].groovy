/* 
 * EchoSistant v4.5 - Total voice control of your SmartThings Home.
 
 
 ************************************ FOR INTERNAL USE ONLY ******************************************************
							
 								DON'T FORGET TO UPDATE RELEASE NUMBER!!!!!
 
 ************************************ FOR INTERNAL USE ONLY ******************************************************

 *		23 November 2018 	Version: 4.6 R.0.0.1	Version change and release
 *		26 October 2018		Version: 4.5 R.0.0.2		Re-Release
 *
 *  Copyright 2018 Jason Headley & Bobby Dobrescu
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 * //UPDATE VERSION
/**********************************************************************************************************************************************/
import groovy.json.*
import java.text.SimpleDateFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.MessageDigest

include 'asynchttp_v1'

definition(
	name			: "EchoSistant",
    namespace		: "Echo",
    author			: "JH/BD",
	description		: "Control and Feedback of your Smart Things Environment via Natural Conversations with Alexa.",
	category		: "My Apps",
    singleInstance	: true,
	iconUrl			: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/app-Echosistant.png",
	iconX2Url		: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/app-Echosistant@2x.png",
	iconX3Url		: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/app-Echosistant@2x.png")
/**********************************************************************************************************************************************
	UPDATE LINE 38 TO MATCH RECENT RELEASE
**********************************************************************************************************************************************/
private def textVersion() {
	def text = "1.0"
}
private release() {
    def text = "R.0.0.1"
}
/**********************************************************************************************************************************************/
preferences {   
    page name: "mainParentPage"
    page name: "mIntent"				
    page name: "mProfiles" 
    page name: "mSupport"
    page name: "mSettings"
    page name: "mTokens"
    page name: "mConfirmation"            
    page name: "mTokenReset"
    page name: "mBonus"
    page name: "mDashboard"

}

//dynamic page methods
page name: "mainParentPage"
def mainParentPage() {	
    dynamicPage(name: "mainParentPage", title:"", install: true, uninstall:false) {
        section ("") {
            href "mProfiles", title: "Create and Manage Rooms", description: mRoomsD(), state: mRoomsS(),
            image: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Echosistant_Routines.png"
        }
        section ("") {
            href "mIntent", title: "Configure System Settings", description: mIntentD(), state: mIntentS(), 
            image: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Echosistant_Config.png"
        }
        section ("") {    
            paragraph "The Current Mode is: ${location.currentMode}"
            paragraph "Smart Home Monitor is set to: " + getSHMStatus() 
        }
	}
}

page name: "mIntent"
def mIntent() {
    dynamicPage (name: "mIntent", title: "Settings and Support", install: false, uninstall: false) {
        section ("") {
            href "mSecurity", title: "Smart Home Monitor Status Changes", description: mSecurityD(), state: mSecurityS(),
            image: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Echosistant_Rest.png"
        }
        section ("") {
            href "mSupport", title: "Install and Support Information", description: mSupportD(), state: mSupportS(),
            image: "https://raw.githubusercontent.com/BamaRayne/Echosistant/master/smartapps/bamarayne/echosistant.src/Echosistant_About.png"
        }
        section("Donations:") {
             href url: textDonateLink(), style:"external", required: false, title:"Donations", description:"Tap to open browser",
             image: "https://raw.githubusercontent.com/BamaRayne/SmartSuite/master/Icons/Donate.png"
        }
    }
}

page name: "mSecurity"    
def mSecurity(){
    dynamicPage(name: "mSecurity", title: "",install: false, uninstall: false) {
		section ("Smart Home Monitor Status Change Feedback",hideable: true){
            input "fSecFeed", "bool", title: "Activate SHM status change announcements.", default: false, submitOnChange: true
            if (fSecFeed) {
        	input "shmEchoDevice", "device.echoSpeaksDevice", title: "Announce Changes on these Amazon Alexa Devices", multiple: true, required: false
            	input "eVolume", "number", title: "Set the volume", description: "0-100 (default value = 30)", required: false, defaultValue: 30
            	if (eVolume) {
                	input "svr", "bool", title: "Do you want to restore the volume to preannouncement levels?", required: false, default:faluse, submitOnChange: true
                    }

                input "shmSynthDevice", "capability.speechSynthesis", title: "Announce Changes On these Speech Synthesis Type Devices", multiple: true, required: false
                input "shmSonosDevice", "capability.musicPlayer", title: "Announce Changes On these Sonos Type Devices", required: false, multiple: true, submitOnChange: true    
            }
            if (shmSonosDevice) {
                input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
            }
            if (fSecFeed) {
            	input "shmSmc", "bool", title: "Announce via Smart Message Control App", default: false, submitOnChange: true
            }
        }
    }
}

page name: "mProfiles"    
def mProfiles() {
    dynamicPage (name: "mProfiles", title: "Create and Manage Rooms", install: true, uninstall: false) {
        if (childApps?.size()>0) {  
            section("",  uninstall: false){
                app(name: "EchoSistant Rooms", appName: "EchoSistant Rooms", namespace: "Echo", title: "Create a New Room", displayChildApps: false, multiple: true,  uninstall: false)
            }
        }
        else {
            section("",  uninstall: false){
                paragraph "NOTE: Looks like you haven't created any Rooms yet.\n \nPlease make sure you have installed the EchoSistant Rooms Add-on before creating a new Profile!"
                app(name: "EchoSistant Rooms", appName: "EchoSistant Rooms", namespace: "Echo", title: "Create a New Room", multiple: true,  uninstall: false)
            }
        }
    }
}  

page name: "mSettings"  
def mSettings(){
    dynamicPage(name: "mSettings", uninstall: true) {
        section("") {
            input "debug", "bool", title: "Enable Debug Logging", default: true, submitOnChange: true 
        }
        section (""){
            input "ShowLicense", "bool", title: "Show Apache License", default: false, submitOnChange: true
            def msg = textLicense()
            if (ShowLicense) paragraph "${msg}"
        }
        section ("Security Tokens", hideable: true, hidden: true) {
        	log.info "The information below is required to be copy and pasted into the AWS Lambda file. \n" +
    				"\n---------------------------------------------------------------------------------------\n" +
                    "\nvar STappID = '${app.id}' \n var STtoken = '${state.accessToken}';\n" +
                   	"var url= '${apiServerUrl("/api/smartapps/installations/")}' + STappID + '/' ;\n" +
                    "\n---------------------------------------------------------------------------------------"
            paragraph "The information below is required to be copy and pasted into the AWS Lambda file. \n" +
                "------------------------------------------------------------------------------------------------------------------------------------\n" +
                " var SmartThings Token = '${state.accessToken}' ;\n" +
                " var url = '${getApiServerUrl()}/${hubUID}/apps/${app.id}/' ;\n" +
                "------------------------------------------------------------------------------------------------------------------------------------" 
            href "mTokens", title: "Revoke/Reset Security Access Token"
        }
        section("Tap below to remove the ${textAppName()} application.  This will remove ALL Profiles and the App from your SmartThings Environment."){
        }	
    }             
}

page name: "mTokens"
def mTokens(){
    dynamicPage(name: "mTokens", title: "Security Tokens", uninstall: false){
        section(""){
            paragraph "Tap below to Reset/Renew the Security Token. You must log in to the IDE and open the Live Logs tab before tapping here. "+
                "Copy and paste the displayed tokens into your Amazon Lambda Code."
            if (!state.accessToken) {
                OAuthToken()
                paragraph "You must enable OAuth via the IDE to setup this app"
            }
        }
        def msg = state.accessToken != null ? state.accessToken : "Could not create Access Token. "+
            "OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth."
        section ("Reset Access Token / Application ID"){
            href "mConfirmation", title: "Reset Access Token and Application ID", description: none
        }
    }
} 

page name: "mConfirmation"
def mConfirmation(){
    dynamicPage(name: "mConfirmation", title: "Reset/Renew Access Token Confirmation", uninstall: false){
        section {
            href "mTokenReset", title: "Reset/Renew Access Token", description: "Tap here to confirm action - READ WARNING BELOW"
            paragraph "PLEASE CONFIRM! By resetting the access token you will disable the ability to interface this SmartApp with your Amazon Echo."+
                "You will need to copy the new access token to your Amazon Lambda code to re-enable access." +
                "Tap below to go back to the main menu with out resetting the token. You may also tap Done above."
        }
        section(" "){
            href "mainParentPage", title: "Cancel And Go Back To Main Menu", description: none 
        }
    }
}

page name: "mTokenReset"
def mTokenReset(){
    dynamicPage(name: "mTokenReset", title: "Access Token Reset", uninstall: false){
        section{
            state.accessToken = null
            OAuthToken()
            def msg = state.accessToken != null ? "New access token:\n${state.accessToken}\n\n" : "Could not reset Access Token."+
                "OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth."
            paragraph "${msg}"
            paragraph "The new access token and app ID are now displayed in the Live Logs tab of the IDE."
            log.info "New IDs: appID = '${app.id}' , Ttoken = '${state.accessToken}'"
        }
        section(" "){ 
            href "mainParentPage", title: "Tap Here To Go Back To Main Menu", description: none 
        }
    }
}

page name: "mSupport"  
def mSupport(){
    dynamicPage(name: "mSupport", uninstall: false) {
        section ("") {
            href "mSettings", title: "Security Token, Logging, App Uninstall", description: mSettingsD(), state: mSettingsS()
        }
        section ("") { 
            href url:"http://thingsthataresmart.wiki/index.php?title=EchoSistant", title: "Tap to go to the EchoSistant Wiki", description: "", state: "complete"
        }   
        section ("") {
            href url:"https://aws.amazon.com/lambda/", title: "Tap to go to the AWS Lambda Website", description: "", state: "complete"
        }
        section ("") {    
            href url:"https://developer.amazon.com/", title: "Tap to go to Amazon Developer website", description: "", state: "complete"
        }
        section (""){  
            paragraph ("You can reach out to the Echosistant Developers with the following information: \n" + 
                       "Jason Headley \n"+
                       "Forum user name @bamarayne \n" +
                       "Bobby Dobrescu \n"+
                       "Forum user name @SBDobrescu")
        }
    }	            	
}   

/*************************************************************************************************************
   CREATE INITIAL TOKEN
************************************************************************************************************/
def OAuthToken(){
	try {
		createAccessToken()
		log.debug "Creating new Access Token"
	} catch (e) {
		log.error "Access Token not defined. OAuth may not be enabled. Go to the SmartApp IDE settings to enable OAuth."
	}
}

/*************************************************************************************************************
   LAMBDA DATA MAPPING
************************************************************************************************************/
mappings {
    path("/b") { action: [GET: "processBegin"]}
	path("/t") { action: [GET: "processTts"] }
}

/************************************************************************************************************
		Base Process
************************************************************************************************************/
def textDonateLink() { "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=HETW6WY6T2FSL&source=url" }
def installed() {
	if (debug) log.debug "Installed with settings: ${settings}"
    state.ParentRelease = release()
    //Reminders
    state.esEvent = [:]
}
def updated() { 
	if (debug) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    state.esEvent = [:]
    initialize()
}
def initialize() {
		//REMINDERS
		sendLocationEvent(name: "EchoSistant v4.5", value: "refresh", data: [profiles: getProfileList()] , isStateChange: true, descriptionText: "Reminders list refresh")
		//SHM status change and keypad initialize
    		subscribe(location, locationHandler)
            subscribe(location, "alarmSystemStatus",alarmStatusHandler) //used for ES speaker feedback
            state.esProfiles = state.esProfiles ? state.esProfiles : []

			state.lambdaReleaseTxt = "Not Set"
            state.lambdaReleaseDt = "Not Set" 
            state.lambdatextVersion = "Not Set"
        //Alexa Responses
			state.pTryAgain = false
        	state.pContCmds = settings.pDisableContCmds == false ? true : settings.pDisableContCmds == true ? false : true
            state.pMuteAlexa = settings.pEnableMuteAlexa
			state.pShort = settings.pUseShort
            state.pContCmdsR = "init"       
        //Other Settings
            state.lastAction = null
			state.lastActivity = null
			state.pendingConfirmation = false
            unschedule("startLoop")
            unschedule("continueLoop")
}
def getProfileList(){
		return getChildApps()*.label
}

/************************************************************************************************************
		Begining Process - Lambda via page b
************************************************************************************************************/
def processBegin(){ 
    def versionTxt  = params.versionTxt 		
    def versionDate = params.versionDate
    def releaseTxt = params.releaseTxt
    def event = params.intentResp
        state.lambdaReleaseTxt = releaseTxt
        state.lambdaReleaseDt = versionDate
        state.lambdatextVersion = versionTxt
    def versionSTtxt = textVersion()
    def releaseSTtxt = release()
    def pPendingAns = false 
    def pContinue = state.pMuteAlexa
    def pShort = state.pShort
    def String outputTxt = (String) null 
    	state.pTryAgain = false
    if (debug) log.debug "^^^^____LAUNCH REQUEST___^^^^" 
    if (debug) log.debug "Launch Data: (event) = '${event}', (Lambda version) = '${versionTxt}', (Lambda release) = '${releaseTxt}', (ST Main App release) = '${releaseSTtxt}'"

    if (event == "noAction") {
    	state.pinTry = null
        state.savedPINdata = null
        state.pContCmdsR = null 
        state.pTryAgain = false
    }
 
// >>> NO Intent <<<<    
    if (event == "AMAZON.NoIntent"){
    	if(state.pContCmdsR == "level" || state.pContCmdsR == "repeat"){
            if (state.lastAction != null) {
            	if (state.pContCmdsR == "level") {state.pContCmdsR = "repeat"}
                def savedData = state.lastAction
                outputTxt = controlHandler(savedData) 
                pPendingAns = "level"
            }
            else {
                state.pContCmdsR = null
                pPendingAns = null
            }
        }
        if( state.pContCmdsR == "door"){
            if (state.lastAction != null) {
                state.lastAction = null
                state.pContCmdsR = null 
                pPendingAns = null 
            }
        }
        if( state.pContCmdsR == "feedback" ||  state.pContCmdsR == "bat" || state.pContCmdsR == "act" ){
            if (state.lastAction != null) {
                state.lastAction = null
                state.pContCmdsR = null 
                pPendingAns = null 
            }
        }
        if( state.pContCmdsR == "init" || state.pContCmdsR == "undefined"){
        	state.pTryAgain = false
        }
        if( state.pContCmdsR == null){
        	state.pTryAgain = false
        }
    }
// >>> YES Intent <<<<     
    if (event == "AMAZON.YesIntent") {
        if (state.pContCmdsR == "level" || state.pContCmdsR == "repeat") {
            state.pContCmdsR = null
            state.lastAction = null
            pPendingAns = "level"
        }
        else {
        	state.pTryAgain = false
        }
        if(state.pContCmdsR == "door"){
            if (state.lastAction != null) {
                def savedData = state.lastAction
 				//NEW PIN VALIDATION!!!!! ///// ADD THE THE usePIN variable below to run the PIN VALIDATION
 				if(state.usePIN_D == true) {
     				//RUN PIN VALIDATION PROCESS
                	def pin = "undefined"
               		def command = "validation"
                	def num = 0
                	def unit = "doors"
                	outputTxt = pinHandler(pin, command, num, unit)
                    pPendingAns = "pin"
                    if (state.pinTry == 3) {pPendingAns = "undefined"}
                    log.warn "try# ='${state.pinTry}'"
					return ["outputTxt":outputTxt, "pContinue":pContinue, "pShort":pShort, "pPendingAns":pPendingAns, "versionSTtxt":versionSTtxt]
            	}
                else {
                outputTxt = controlHandler(savedData) 
                pPendingAns = "door"
            	}
              return ["outputTxt":outputTxt, "pContinue":pContinue,  "pShort":pShort, "pPendingAns":pPendingAns, "versionSTtxt":versionSTtxt]
        	}
        }
        if(state.pContCmdsR == "feedback"){
            if (state.lastAction != null) {
                def savedData = state.lastAction
                outputTxt = getMoreFeedback(savedData) 
                pPendingAns = "feedback"
				return ["outputTxt":outputTxt, "pContinue":pContinue,  "pShort":pShort, "pPendingAns":pPendingAns, "versionSTtxt":versionSTtxt]
            }
         }
        //if Alexa is muted from the child, then mute the parent too / MOVED HERE ON 2/9/17
        pContinue = pContinue == true ? true : state.pMuteAlexa == true ? true : pContinue
		return ["outputTxt":outputTxt, "pContinue":pContinue, "pShort":pShort, "pPendingAns":pPendingAns, "versionSTtxt":versionSTtxt]	     
	}
	if (debug){
    	log.debug "Begining Process data: (event) = '${event}', (ver) = '${versionTxt}', (date) = '${versionDate}', (release) = '${releaseTxt}'"+ 
      	"; data sent: pContinue = '${pContinue}', pShort = '${pShort}',  pPendingAns = '${pPendingAns}', versionSTtxt = '${versionSTtxt}', releaseSTtxt = '${releaseSTtxt}' outputTxt = '${outputTxt}' ; "+
        "other data: pContCmdsR = '${state.pContCmdsR}', pinTry'=${state.pinTry}' "
	}
    return ["outputTxt":outputTxt, "pContinue":pContinue, "pShort":pShort, "pPendingAns":pPendingAns, "versionSTtxt":versionSTtxt]	 

} 

/************************************************************************************************************
   Get current SHM status for Dashboard
************************************************************************************************************/
def getSHMStatus() {
    def currentSHM = location.currentState("alarmSystemStatus")?.value
    if (currentSHM == "off") {
        currentSHM = "Security Disabled" }
    if (currentSHM == "away") {
        currentSHM = "Armed-Away" }
    if (currentSHM == "stay") {
        currentSHM = "Armed-Stay" }
    return currentSHM
}
    
/************************************************************************************************************
   TEXT TO SPEECH PROCESS - Lambda via page t
************************************************************************************************************/
def processTts() {
		//LAMBDA VARIABLES
        log.info "received text is: $params.ttstext"
    	def ptts = params.ttstext 
        def pintentName = params.intentName
        //OTHER VARIABLES
        def String outputTxt = (String) null 
 		def String pContCmdsR = (String) null
        def pContCmds = false
        def pTryAgain = false
        def pPIN = false
        def dataSet = [:]
        if (debug) log.debug "Messaging Profile Data: (ptts) = '${ptts}', (pintentName) = '${pintentName}'"   
                
        pContCmdsR = "profile"
		def tProcess = true
//try {

	if (ptts == "this is a test"){
		outputTxt = "Congratulations! EchoSistant is now setup properly" 
		return ["outputTxt":outputTxt, "pContCmds":state.pContCmds, "pShort":state.pShort, "pContCmdsR":state.pContCmdsR, "pTryAgain":state.pTryAgain, "pPIN":pPIN]       
    }
        
        if(ptts.contains("no ") || ptts == "no" || ptts == "stop" || ptts == "cancel" || ptts == "kill it" || ptts == "zip it" || ptts == "yes" && state.pContCmdsR != "wrongIntent"){
        	if(ptts == "no" || ptts == "stop" || ptts == "cancel" || ptts == "kill it" || ptts == "zip it" || ptts.contains("thank")){
                outputTxt = "ok, I am here if you need me"
                pContCmds = false
                return ["outputTxt":outputTxt, "pContCmds":pContCmds, "pShort":state.pShort, "pContCmdsR":pContCmdsR, "pTryAgain":state.pTryAgain, "pPIN":pPIN]
        	}
			else {
                outputTxt = "ok, please continue, "
                pContCmds = false
                return ["outputTxt":outputTxt, "pContCmds":pContCmds, "pShort":state.pShort, "pContCmdsR":pContCmdsR, "pTryAgain":state.pTryAgain, "pPIN":pPIN]
        	}        
        }
  		else{
             childApps.each {child ->
             	if (child.label.toLowerCase() == pintentName.toLowerCase()) { 
                    if (debug) log.debug "Found a profile: '${pintentName}'"
                    pintentName = child.label
                    // recording last message
                    state.lastMessage = ptts
                    state.lastIntent = pintentName
                    state.lastTime = new Date(now()).format("h:mm aa", location.timeZone)
                    dataSet = [ptts:ptts, pintentName:pintentName, fDevice:fDevice] 
					def childRelease = child.checkRelease()
					log.warn "childRelease = $childRelease"

					if (ptts.startsWith("did") || ptts.startsWith("tell") || ptts.startsWith("get") || ptts.endsWith("tonight") || ptts.contains("weather") || ptts.contains("temperature") || ptts.contains("forecast") || ptts.contains("humidity") || ptts.contains("rain") || ptts.contains("wind")) {
                    	def pResponse = child.profileFeedbackEvaluate(dataSet)
                        outputTxt = pResponse.outputTxt
                    	pContCmds = pResponse.pContCmds
                    	pContCmdsR = pResponse.pContCmdsR
                    	pTryAgain = pResponse.pTryAgain
                    	}
					if (ptts.startsWith("for") || ptts.startsWith("is") || ptts.startsWith("has") || ptts.startsWith("give") || ptts.startsWith("how") || ptts.startsWith("what") || ptts.startsWith("when") || ptts.startsWith("which") || ptts.startsWith("are") || ptts.startsWith("check") || ptts.startsWith("who")) {
                        def pResponse = child.profileFeedbackEvaluate(dataSet)
                        outputTxt = pResponse.outputTxt
                    	pContCmds = pResponse.pContCmds
                    	pContCmdsR = pResponse.pContCmdsR
                    	pTryAgain = pResponse.pTryAgain
                    	}
                    else {  
                        def pResponse = child.profileEvaluate(dataSet)
                		outputTxt = pResponse.outputTxt
                    	pContCmds = pResponse.pContCmds
                    	pContCmdsR = pResponse.pContCmdsR
                    	pTryAgain = pResponse.pTryAgain
                    	log.info "I have received this from the Lambda: ${outputTxt}"
                    	}
                	}
            	}

            if (outputTxt?.size()>0){
                return ["outputTxt":outputTxt, "pContCmds":pContCmds, "pShort":state.pShort, "pContCmdsR":pContCmdsR, "pTryAgain":pTryAgain, "pPIN":pPIN]
            }
            else {
                if (state.pShort != true){
                	outputTxt = "Hey, I wish I could help, but EchoSistant couldn't find a Profile named " + pintentName + " or the command may not be supported"
                }
                else {outputTxt = "I've heard " + pintentName + " , but I wasn't able to take any actions "} 
                pTryAgain = true
                return ["outputTxt":outputTxt, "pContCmds":pContCmds, "pShort":state.pShort, "pContCmdsR":pContCmdsR, "pTryAgain": pTryAgain, "pPIN":pPIN]
            }
        	
            def hText = "run a messaging and control profile"
			if (state.pShort != true){ 
				outputTxt = "Sorry, I heard that you were looking to " + hText + " but EchoSistant wasn't able to take any actions "
			}
			else {outputTxt = "I've heard " + pintentName + " , but I wasn't able to take any actions "}         
			pTryAgain = true
			return ["outputTxt":outputTxt, "pContCmds":pContCmds, "pShort":state.pShort, "pContCmdsR":pContCmdsR, "pTryAgain":pTryAgain, "pPIN":pPIN]              
    	}

} 

/***********************************************************************************************************
		SMART HOME MONITOR STATUS AND KEYPAD HANDLER
***********************************************************************************************************/
// ALARM STATUS CHANGE FEEDBACK TO SPEAKERS
def alarmStatusHandler(evt) {
    if (fSecFeed) {
        def curEvtValue = evt.value
        log.info "Smart Home Monitor status changed to: ${curEvtValue}"
        if (shmSynthDevice || shmSonosDevice || shmSmc || shmEchoDevice) {
            if (evt.value == "armAway") {
                sendAwayCommand
                def message = "Attention, The alarm system is now set to armed away"
                if (echoDevice) {
                    settings.shmEchoDevice.each { spk->
                        if(svr) {
                            int eVolume = eVolume as Integer
                            spk.setVolumeSpeakAndRestore(eVolume, String)
                        }
                        else{
                            spk.setVolumeAndSpeak(eVolume, String)
                        }
                    }
                }     
                if(shmSynthDevice) shmSynthDevice?.speak(message)
                if (shmSonosDevice) 
                shmSonosDevice?.playTextAndRestore(message)
                if (shmSmc) sendLocationEvent(name: "EchoSistantMsg", value: "ESv4.5 SHM Status Announcement", isStateChange: true, descriptionText: "${message}")
            }
            else if (evt.value == "stay") {
                def message = "Attention, The alarm system is now set to armed stay"
                if (echoDevice) {
                    settings.shmEchoDevice.each { spk->
                        if(svr) {
                            int eVolume = eVolume as Integer
                            spk.setVolumeSpeakAndRestore(eVolume, String)
                        }
                        else{
                            spk.setVolumeAndSpeak(eVolume, String)
                        }
                    }
                }     
                if(shmSynthDevice) shmSynthDevice?.speak(message)
                if (shmSonosDevice) 
                shmSonosDevice?.playTextAndRestore(message)
                if (shmSmc) sendLocationEvent(name: "EchoSistantMsg", value: "ESv4.5 SHM Status Announcement", isStateChange: true, descriptionText: "${message}")
            }
            else if(evt.value == "off") {
                def message = "Attention, The alarm system has been disarmed"
                if (echoDevice) {
                    settings.shmEchoDevice.each { spk->
                        if(svr) {
                            int eVolume = eVolume as Integer
                            spk.setVolumeSpeakAndRestore(eVolume, String)
                        }
                        else{
                            spk.setVolumeAndSpeak(eVolume, String)
                        }
                    }
                }     
                if(shmSynthDevice) shmSynthDevice?.speak(message)
                if (shmSonosDevice) 
                shmSonosDevice?.playTextAndRestore(message)
                if (shmSmc) sendLocationEvent(name: "EchoSistantMsg", value: "ESv4.5 SHM Status Announcement", isStateChange: true, descriptionText: "${message}")
            }
        }
    }
}


/*^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
X 																											X
X                       					UI FUNCTIONS													X
X                        																					X
/*^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
/************************************************************************************************************
   		UI - Version/Copyright/Information/Help
************************************************************************************************************/
private def textAppName() {
	def text = app.label // Parent Name
}
private def textLicense() {
	def text =
	"Licensed under the Apache License, Version 2.0 (the 'License'); "+
	"you may not use this file except in compliance with the License. "+
	"You may obtain a copy of the License at"+
	" \n"+
	" http://www.apache.org/licenses/LICENSE-2.0"+
	" \n"+
	"Unless required by applicable law or agreed to in writing, software "+
	"distributed under the License is distributed on an 'AS IS' BASIS, "+
	"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
	"See the License for the specific language governing permissions and "+
	"limitations under the License."
}

/************************************************************************************************************
   Page status and descriptions 
************************************************************************************************************/       
//	Naming Conventions: 
// 	description = pageName + D (E.G: description: mIntentD())
// 	state = pageName + S (E.G: state: mIntentS(),
/************************************************************************************************************/       

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
/** General Settings Page **/
def mSettingsS() {
    def result = ""
    if (ShowLicense || debug) {
    	result = "complete"	
    }
    result
}
def mSettingsD() {
    def text = "Tap here to Configure"
    if (ShowLicense || debug) { 
            text = "Configured"
    }
    text
}
/** Install and Support Page **/
def mSupportS() {
    def result = ""
    if (notifyOn || securityOn) {
    	result = "complete"	
    }
    result
}
def mSupportD() {
    def text = ""
    if (notifyOn || securityOn) { 
            text = ""
    }
    text
}

def mIntentS(){
	def result = ""
    def IntentS = ""
    if (fSecFeed) {
    	IntentS = "comp"
        result = "complete"
    }    	
    	result
}
def mIntentD() {
    def text = "Tap here to Configure"
	def mIntentS = mIntentS()
    if (mIntentS) 
    {
        text = "Configured"
    }
    else text = "Tap here to Configure"
	    text
} 
def mSecurityS() {def result = ""
    if (cMiscDev || cRoutines || uPIN_SHM || uPIN_Mode || fSecFeed || shmSynthDevice || shmSonosDevice || volume || resumePlaying) {
    	result = "complete"}
   		result}
def mSecurityD() {def text = "Tap here to configure settings" 
    if (cMiscDev || cRoutines || uPIN_SHM || uPIN_Mode || fSecFeed || shmSynthDevice || shmSonosDevice || volume || resumePlaying) {
    	text = "Configured"}
    	else text = "Tap to Configure"
		text}
        
        
