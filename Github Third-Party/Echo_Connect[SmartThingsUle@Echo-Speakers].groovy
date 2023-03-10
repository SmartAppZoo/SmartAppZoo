/**
 *  Echo Service Manager v 1.0.0
 *
 *  Author: Ulises Mujica 
 */
 
definition(
	name: "Echo (Connect)",
	namespace: "mujica",
	author: "Ulises Mujica",
	description: "Allows you to control your Echo from the SmartThings app. Perform basic functions like play, pause, stop, tts from the Things screen.",
	category: "SmartThings Labs",
	singleInstance: true,
	iconUrl: "https://graph.api.smartthings.com/api/devices/icons/st.secondary.smartapps-tile?displaySize=2x",
    iconX2Url: "https://graph.api.smartthings.com/api/devices/icons/st.secondary.smartapps-tile?displaySize=2x"
)

preferences {
	page(name: "iniSettings", title: "Connect your Echo devices to SmartThings", content: "iniSettings")
	page(name: "chooseDevices", title: "Choose devices to Control With SmartThings", content: "echoDiscovery")
}

/**
 *  Add in iniSettings your cookie, csrf and domain
 *  The data is a sample, you must to update with you real info
*/

def iniSettings(){
	state.cookie = "x-amzn-dat-gui-client-v=1.24.2024399.0; session-id=173-725365-55345167; ubid-main=126-3823725-97123234; x-main=T1yFnEmFeXBhTI9Gzg9LNDm9G7S4nK8a17SijZZLAeDJJ2beE4hUlkglBOKlE4M7; at-main=Atza|IwEBIMKd7Lp6suJhwl2gdWkM1kA7l6VJM-Nd-sAH6PQpIUKbpg2f3S6J9wu7UwZHr-ZeaEMd5DjTNeoYTSqQZYqYqKPz55Bt4xRSXDSMA6TQMR5ADcTnHWXk0eAf2t9AYgNXK27lQPvuZsupe_p97hxbuMz7Epad1CMahn_0hz3yOTWM7DWfR5KM4e85KBJgAjRJTD9TtXWkdKNV0kLTfNqqe5aW-_9hs15-VdCiT8Y6qe3V13mUvzkAe7nr9xWsks9iiRpIPDmsX9ybs7rj3ZJMDgguey6cVRa-RZySrfvAEWA9h--HTZcz-RoGoKtCNTJiHnVy4kWk1XEIGpOrfT35iwfTui-DR7hUh22_iWK3xVJlRac9KgKQPITKxCq8Z2RoVTwQNnkeyrsnAjC8OzAUbEA-; sess-at-main=\"jIXEXiQllwAgL2kooJ2JwtVGgFlYzQHdXJB0w9uqJbw=\"; csrf=-2168072530; session-id-time=2082787201l; appstore-devportal-locale=en_US; AMCVS_4A8581745834114C0A495E2B%40AdobeOrg=1; AMCV_4A8581745834114C0A495E2B%40AdobeOrg=-330454231%7CMMIDTS%7C17851%7CMCSID%7C43310101112267904718424637798690089617%7CMCOPTOUT-1542269871s%7CNONE%7CMCAID%7CNONE%7CvVersion%7C3.1.2; _mkto_trk=id:365-EFI-026&token:_mch-amazon.com-1542222671491-48433; referrer_session={%22full_referrer%22:%22https://www.google.com/%22}; s_cc=true; s_nr=1542262714026-New; s_lv=1542262714028; session-token=\"d+K75nKStBC6d2DIYMyDKvWktK2oF0oQ7zbxRiba6boqeBhcVCEBV/gzMbZyuVWtdC6EdHNbJggjwZPMBCJmBTwCY1WNEgnrInMf30/OkBlPlqWebRNV2dW34OgSLDg6bGyqPmoL/Jet9dlX4rTSkDa2Snn4qfKt3j1FgyK1Hl8I6H6epBktwjMhQjRRVM+c9uvwoX6Bsdo=\""
    state.csrf = "-2168072530"
    state.domain = "https://alexa.amazon.com"
    state.loadStatus = "Inactive"
    log.trace "state.loadStatus ${state.loadStatus}"
    return dynamicPage(name:"iniSettings", title:"Connect Your Echo devices to SmartThings", nextPage:"chooseDevices", install:false, uninstall: true) {
       section("Echo Remote Credentials") {
			paragraph "Get your Echo data from https://alexa.amazon.com\r\n\r\nThe cookie data is to long, you must go to Echo Conect code in IDE and add the Cookie, Domain, CSRF data in the iniSettings() section\r\n\r\nTap 'Next' after you have entered the data.\r\n\r\nOnce your request is accepted, SmartThings will scan your Echo devices."
		}
    }
}

def echoDiscovery() {
	debugOut "echoDiscovery()"
	//getToken()
	state.token = "1234567890"
    
    if (state.loadStatus == "Inactive"){
    	state.count = 0
    	state.loadStatus = "Loading"
        log.trace "state.loadStatus ${state.loadStatus}"
    	deviceDiscovery()
    }
    log.trace "state.count ${state.count}"
    state.count = state.count + 1 
    log.trace "state.count ${state.count}"
    if(state.loadStatus == "Loaded" ){
        def options = devicesDiscovered() ?: []
		log.trace "state.loadStatus ${state.loadStatus}"
        return dynamicPage(name:"chooseDevices", title:"", nextPage:"", install:true, uninstall: true) {
            section("Tap Below to View Device List") {
                input "selectedEchos", "enum", required:false, title:"Select Echo", multiple:true, options:options
                paragraph """Tap 'Done' after you have selected the desired devices."""
            }
        }
    }else{
    	if (state.count)
    	log.trace "state.loadStatus ${state.loadStatus}"
        def msg = state.count >= 3 ? "The server is not responding, please verify the cookie, domain and csrf data" : "Please wait while we discover your devices. Discovery can take some minutes or more, so sit back and relax! Select your device below once discovered."
        return dynamicPage(name:"chooseDevices", title:"", nextPage:"", refreshInterval:5) {
            section(msg) {}
        }
    }
}


def installed() {
	debugOut "Installed with settings: ${settings}"

	unschedule()
	unsubscribe()

	setupEchos()
}

def updated() {
	debugOut "Updated with settings: ${settings}"
	unschedule()
	setupEchos()
}

def uninstalled()
{
	unschedule() //in case we have hanging runIn()'s
}

private removeChildDevices(delete)
{
	debugOut "deleting ${delete.size()} Echos"
	debugOut "deleting ${delete}"
	delete.each {
		deleteChildDevice(it.device.deviceNetworkId)
	}
}

def uninstallFromChildDevice(childDevice)
{
	def errorMsg = "uninstallFromChildDevice was called and "
	if (!settings.selectedEchos) {
		debugOut errorMsg += "had empty list passed in"
		return
	}

	def dni = childDevice.device.deviceNetworkId

	if ( !dni ) {
		debugOut errorMsg += "could not find dni of device"
		return
	}

	def newDeviceList = settings.selectedEchos - dni
	app.updateSetting("selectedEchos", newDeviceList)
	debugOut errorMsg += "completed succesfully"
}


def setupEchos() {
	debugOut "setupEchos()"
	def echos = state.devices
	def deviceFile = "TCP Echo"

	selectedEchos.each { did ->
		//see if this is a selected echo and install it if not already
		def d = getChildDevice(did)

		if(!d) {
			def newEcho = echos.find { (it.serialNumber) == did }
			d = addChildDevice("mujica", "Echo", did, null, [name: "${newEcho?.accountName}", label: "Echo ${newEcho?.accountName}", completedSetup: true,"data":newEcho])
		} else {
			infoOut "Avoid add existent device ${did}"
		}
	}
	def delete = getChildDevices().findAll { !selectedEchos?.contains(it.deviceNetworkId) }
	removeChildDevices(delete)
}


def deviceDiscovery() {
	
    def logText =""
    
    def data
    def devices = []
    
    def params = [
    uri: state.domain + "/api/devices-v2/device?cached=true",
    headers:[
            "Cookie": state.cookie,
            ]
	]
    log.trace "params $params"
    try {
        httpGet(params) { resp ->
 //          resp.headers.each {
 //               log.debug "${it.name} : ${it.value}"
 //           }
 //           log.debug "response contentType: ${resp.contentType}"
            data = resp.data
//            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
	
    if (data){
        state.devices = data["devices"]
    	state.loadStatus = "Loaded"

        data["devices"].each {
                logText +=" accountName:  ${ it["accountName"]} deviceAccountId:  ${ it["deviceAccountId"]} deviceFamily:  ${ it["deviceFamily"]} deviceOwnerCustomerId:  ${ it["deviceOwnerCustomerId"]} deviceType:  ${ it["deviceType"]} deviceTypeFriendlyName:  ${ it["deviceTypeFriendlyName"]} online:  ${ it["online"]} serialNumber:  ${ it["serialNumber"]} softwareVersion:  ${ it["softwareVersion"]} "
        	}
    	log.trace logText

    } else{
    	log.trace "No data"
    }
}

Map devicesDiscovered() {
	def devices =  state.devices
	def map = [:]

	devices.each {
    	if (it?.deviceFamily !="VOX"){
            def value = "${it?.accountName}"
            def key = it?.serialNumber
            map["${key}"] = value
        }
    }
	map
}

def getDevices()
{
	state.devices = state.devices ?: [:]
}


def debugOut(msg) {
	log.debug msg
}

def traceOut(msg) {
	log.trace msg
}

def infoOut(msg) {
	log.info msg
}
