definition(
    name: "Virtual Color Temperature Dimmer Creator",
    singleInstance: true,
    namespace: "grussr",
    author: "Ryan Gruss",
    description: "Allows for creation of SmartThings virtual dimmer switch that also controls color temperature.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/alexa-helper.src/Alexa.png",
    iconX2Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/alexa-helper.src/Alexa@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/alexa-helper.src/Alexa@2x.png")
preferences {
    page name:"pageAbout"
    page name:"mainPage"
    page name:"pageAddSwitch"
}
def mainPage() {
    dynamicPage(name: "mainPage", uninstall: false, install: true) {
        section("New device information"){
            input "addSwitchName", "text", title: "Device Label", description: "Enter a unique label name for the virtual device", required: false, submitOnChange:true
            if (addSwitchName) href "pageAddSwitch",title: "Tap To Add The Device", description: "Device will be created based on the parameters above", image: imgURL()+"add.png"
        }        
        def switchList = ""
        state.sw1Ver = ""
        state.sw2Ver = ""
        state.dev1Ver = ""
        def noun = "${getChildDevices().size()} devices"
        if (getChildDevices().size() > 0) {
        	if (getChildDevices().size() == 1) noun = "One device"
            def count = getChildDevices().size() 
            getChildDevices().each {
            	switchList += "${it.label} (${it.typeName})"
                count --
                if (count) switchList += "\n"
            }
		}
        if (getChildDevices().size>0){
        	section ("${noun} created within Virtual Color Temperature Dimmer Creator"){paragraph switchList}
    	}
	}
}
// Show "pageAbout" page
def pageAbout(){
	dynamicPage(name: "pageAbout", uninstall: true) {
		section {
        	paragraph "${textAppName()}\n${textCopyright()}", image: "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/smartapps/michaelstruck/alexa-helper.src/Alexa@2x.png"
        }
        section ("SmartApp/Device Versions") { paragraph "${textVersion()}" }    
        section ("Apache License") { paragraph "${textLicense()}" }
    	section("Instructions") { paragraph textHelp() }
        section("Application Name") { label title:"SmartApp Name", required:false, defaultValue: textAppName() }
        section("Tap below to remove all devices and application"){ }
        remove ("Completely Remove ${textAppName()}","Warning","This will remove the entire application, including all of your virtual devices.\n\nYou will be unable to undo this action!\n\nIf you receive an error when attempting to remove this application, some of the switches may be in use with other SmartApps. " +
        "You will need to remove them first to proceed with the full removal of the application.")
	}
}
// Show "pageAddSwitch" page
def pageAddSwitch() {
	dynamicPage(name: "pageAddSwitch", title: "Add Device", install: false, uninstall: false) {
    	def repsonse
        if (getChildDevices().find{it.label == addSwitchName}){
            repsonse="There is already a device labled '${addSwitchName}'.\n\nTap < to go back and change the device label name."
        }
        else {
         	repsonse = !addSwitchName  ? "Device label name not specified.\n\nTap < to go back and enter the device information" :  addChildSwitches()
        }
        section {paragraph repsonse}
    }
}
def installed() {
    initialize()
}
def updated() {
    initialize()
}
def uninstalled(){
	deleteChildSwitches()
}
def initialize() {}
//Common modules (for adding switches)
def addChildSwitches(){
    def deviceID = "VDSC_${app.id}_${getChildDevices().size()}"
    def nameSpace = "grussr"
    def addSwitchType = "Color Temperature Virtual Dimmer"
    def result
    try {
		def childDevice = addChildDevice(nameSpace, addSwitchType, deviceID, null, [name: deviceID, label: addSwitchName, completedSetup: false])
		log.debug "Created Device ${addSwitchName}: ${deviceID}"
        result ="The ${addSwitchType} named '${addSwitchName}' has been created.\n\nIf you are using this with Alexa,\nbe sure to 'discover' the device in your Alexa app."
    } catch (e) {
		log.debug "Error creating switch: ${e}"
        result ="The ${addSwitchType} named '${addSwitchName}' could NOT be created.\n\nEnsure you have the correct device code installed and published within the SmartThings IDE."
	}
	result + "\n\nTap < to return to the device page."   
}
def deleteChildSwitches() {
    getChildDevices().each {
    	log.debug "Deleting device ID: " + it.deviceNetworkId
        try {
            deleteChildDevice(it.deviceNetworkId)
        } catch (e) {
            log.debug "Fatal exception ${e}"
        }
    }
}
//Common modules
def getSwitchAbout(){ return "Created by Alexa Virtual Device Creator" }
def imgURL() { return "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/img/" }
//Version/Copyright/Information/Help
private def textAppName() { return "Alexa Virtual Device Creator" }	
private def textVersion() {
    def version = "Version: 1.0.2 (08/29/2018)", childVersion, deviceCount= getChildDevices().size()
    childVersion = state.sw1Ver && deviceCount ? "\n${state.sw1Ver}": ""
    childVersion += state.sw2Ver && deviceCount ? "\n${state.sw2Ver}": ""
    childVersion += state.dev1Ver && deviceCount ? "\n${state.dev1Ver}": ""
    return "${version}${childVersion}"
}
private def versionInt(){ return 102 }
private def textCopyright() { return "Copyright © 2018 Michael Struck" }
private def textLicense() {
    def text =
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
private def textHelp() {
	def text =
		"Allows the creation of Virtual Momentary Button Tiles, Virtual Motion Detectors or Alexa Switches (special virtual dimmers that allow for non-state updates). "+
        "You can then attach these devices to various SmartThings automations (WebCoRE, Amazon Alexa, etc)."
}