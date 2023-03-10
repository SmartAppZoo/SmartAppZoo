/*
Electric Imp (Connect) 
by: David Meyer / Spindance
This is half of an application that allows for simple authenticated communicatino between the 
Electric Imp cloud and the Smart Things cloud.  This application creates virtual devices which can
be addressed by name from the Electric Imp cloud via the Smart Things Electric Imp library. 
*/

definition(
    name: "Electric Imp (Connect)",
    namespace: "smartthings",
    author: "David Meyer",
    description: "Connect your Electric Imp to SmartThings.",
    category: "SmartThings Labs",
    singleInstance: true,
    oauth: [ displayName: "Electric Imp (Connect)"]
    ){
    	appSetting "ImpAgentURL"
    }

//configuration page for sending the user to their first bounce off of the electric imp cloud
def impSubmit(params){
	def clientId = "46076a1c-9691-4d81-bf1a-2984ac6ceec8"
	def clientSecret = "3d56c748-e7de-4d2b-9872-8b75e5d084f5"
	
    dynamicPage(name: "impConfig", title: "Configure and Authenticate") {
        section ("Electric Imp Settings"){
            href(
                name: "ElectricImpAuthLink",
                title: "Start Authentication Process",
                style: "external",
                url: "$ImpAgentURL/oauth/authorize?clientId=$clientId&clientSecret=$clientSecret",
                description: "Tap here to authorize Electric Imp to talk to SmartThings.  This may take up to 30 sec to load."
            )
        }
    }
}

// configuratoin page for setting up the url for communicating with the Electric Imp Cloud
// this page is seperate from impSubmit because it is posible to enter this url and click
// on the link to go to the url, without forcing the update of the link, directing the user
// to the wrong location.
def impConfig(params){
    dynamicPage(name: "impConfig", title: "Configure and Authenticate") {
        section ("Electric Imp Settings"){
            input(
                name:"ImpAgentURL", 
                type:"text", 
                title:"Please paste your Electric Imp Agent URL.  You must hit 'Done' when finished.", 
                required:true,
                submitOnChange:true
            )


            href(
                name: "ElectricImpAuthLink",
                title: "Next",
                style:"page",
                page:"impSubmit",
                description:""
            	)
        }
    }
}

//configuration page that performs the adding of virtural devices
def deviceAdded() {
	def sensorTypes = [
    	"barometer" : "ElectricImp Development Barometeric Sensor",
	    "humidity" : "ElectricImp Development Humidity Sensor",
        "luminosity" : "ElectricImp Development Light Sensor",
    	"thermostat" : "ElectricImp Development Temperature Adapter"
    ]
    if (!getChildDevice("$sensorType$sensorId")){
    	if ("$sensorType"?.startsWith("temperautre"))
    		addChildDevice("smartthings/testing", sensorTypes["$sensorType"], "$sensorType$sensorId")
		else
        	addChildDevice("Swankdave", sensorTypes["$sensorType"], "$sensorType$sensorId")
		return dynamicPage(name: "deviceAdded") {
   	 		section ("Device $sensorType$sensorId added successfully, you can now access this device from your electric imp using the id $sensorType$sensorId")
    	}	
    }
    return dynamicPage(name: "deviceAdded") {
   		section ("Device add failed, does a device with the network id: $sensorType$sensorId this device allready exist?"){}
    }	
}

//configuration page for adding virtural devices, which can be individually removed from within the smart things system
def manageDevices(){
	dynamicPage(name: "manageDevices", title: "Add/Remove Devices") {
        section (){
        	paragraph "Please designate an ID and select a sensor type for the device you wish to create or destroy"
            input(name:"sensorId", type:"number", title:"Virtural device ID", description:"This needs to be unique within a sensor type")
            input(name:"sensorType", type:"enum", title:"Sensor Type", description:"The type of virtural device you would like to create", options: [
                "barometer" : "Barometer",
                "humidity" : "Humidity",
                "luminosity" : "Luminosity",
                "thermostat" : "Thermostat"
            ])
            href(name:"Add Device", style:"page", page:"deviceAdded")
        }
  }
}

//Main preferences page
preferences {
	page(name:"Main", title:"Electric Imp Settings", install: true, uninstall: true){
        section (){
            href(name:"impConfigLink", title: "Configure and Authenticate", style:"page", page:"impConfig", description:"go to Electric Imp Settings")
            href(name:"manageDevicesLink", title: "Add/Remove Devices", style:"page", page:"manageDevices", description:"go manage Devices")
        }
    }
	page(name: "impConfig", title: "Configure and Authenticate") 
    page(name: "impSubmit", title: "Configure and Authenticate") 
    page(name: "manageDevices", title: "Add/Remove Devices")
    page(name: "deviceAdded", title: "Device Added")
}


//The address the Electric Imp writes to to update its virtural devices
mappings {
    path("/values/:name:value") {
        action: [GET: "updateValue"]
    }
}

def updateValue() {
   	device = getChildDevice(params.name)
    if (params.name?.startsWith("temperautre"))
    	device.setTemperature(params.value)
    else
    	device.update(params.value)
}

def installed() {
	log.debug("installed")
}

def updated() {
	log.debug("updated")
}

def uninstalled() {
	log.debug("uninstalling")
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}