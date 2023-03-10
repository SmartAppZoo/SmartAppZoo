definition(
    name: "Tesla-Control (By Shyam)",
    namespace: "connectedcar",
    author: "Shyam Avvari",
    description: "Integrate your Tesla car with SmartThings",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%402x.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/tesla-app%403x.png",
    singleInstance: true 
){
	appSetting "CLIENT_ID"
    appSetting "CLIENT_SECRET"
    appSetting "OWNER_API_URI"
}
//TODO 
// *. Unable to forget the username & password after login is success
//
preferences {
	page(name: "loginToTesla", title: "Login to Tesla")
	page(name: "selectCars", title: "Select Your Tesla")
}

def loginToTesla() {
	log.debug "Showing screen 1"
    log.debug "State : ${state}"
    if(isAccessTokenValid().success == true) {
    	return dynamicPage(name: "loginToTesla", title: "Connect your MyTesla Account", nextPage:"selectCars", uninstall:true) {
        	section("Tesla Login Active, Continue to Car Selection") {}
            remove("Uninstall Tesla-Control")
        }
    } else {
		return dynamicPage(name: "loginToTesla", title: "Connect your MyTesla Account", nextPage:"selectCars", uninstall:false) {
    		section("MyTesla Credentials:") {
        		input "username", "text", title: "Username", required: true, autoCorrect:false
            	input "password", "password", title: "Password", required: true, autoCorrect:false
        	}
    	}
	}
}

def selectCars() {
	//if valid token not present @@ this need refinement
    def loginResult = []
    if(isAccessTokenValid().success == false) {
		log.debug "Tesla Login.."
		loginResult = login()
    	log.debug "Login Complete.."
    } else {
    	loginResult = [success:true]
    }
    
	if(loginResult.success){
    	def options = []
        options = carsDiscovered() 
        log.debug "Options : ${options}"
        
        //if no cars present or all cars already added
        if (options.size() == 0){
        	if(getChildDevices()){
            	return dynamicPage(name: "selectCars", title: "Nothing to Do", install: true, uninstall:true) { 
                	section("Login Success") {}
                    section("Following Cars Already Added"){
                    	getChildDevices().each {
                        	paragraph "${it.getLabel()}"                                      
                        }
                    }
                    remove("Uninstall Tesla-Control")
                }
            } else {
            	//no cars available to add
                return dynamicPage(name: "selectCars", title: "Nothing to Do", install: true, uninstall:true) { 
                    section("Login Success") {}
                    section("No cars available to add - Check mobile settings on your car") {}
                    remove("Uninstall Tesla-Control")
            	}
            }
        } else {
        	return dynamicPage(name: "selectCars", title: "Select Your Tesla", install: true, uninstall:false) { 
        		section("Login Success") {}
            	section("Select which Tesla to connect"){
            		input(name: "selectedCar", type: "enum", required:false, multiple:false, options:options)
            	}
                //for future when we can allow two cars as devices
                //section("Following Cars Already Added"){
                //	getChildDevices().each {
                //   	paragraph "${it.getLabel()}"                                      
                //    }
                //}
			}
        }
    } else {
    	return dynamicPage(name: "selectCars", title: "Tesla", uninstall:true) {
        	section("Login Failed") {}
        }
    }
}

def installed() {
	log.debug "Installed"
	initialize()
}

def updated() {
	log.debug "Updated"
    //settings.username = "a"
    //settings.password = "a"
	//unsubscribe()
	//initialize()
}

def uninstalled() {
	log.debug "Uninstalled"
    if(getChildDevices()){
    	removeChildDevices(getChildDevices())
    }
}

def initialize() {
	if (selectCars) {
		addDevice()
	}
}

//CHILD DEVICE METHODS
def addDevice() {
	log.debug "Selected Car : ${selectedCar}"
    //double check if the device is already added
    if(!getChildDevice(selectedCar)){ 
    	//find the selected car in the car list
        def car = [:]
        state.cars.each {
        	if (it.vin == selectedCar){
        		car = it
            }
        }
        //add device
    	def d = addChildDevice("connectedcar", "Tesla", "${car.id}", null, [label: "${car.display_name}", name: "Tesla"])
        //add device for climate
   		def ch = addChildDevice("connectedcar", "Tesla-Charging", "${car.id}-Chg", null, [label: "${car.display_name} Charging", name: "Tesla-Charging"])
        //add device for climate
        def c = addChildDevice("connectedcar", "Tesla-ClimateOnOff", "${car.id}-C", null, [label: "${car.display_name} Climate", name: "Tesla-ClimateOnOff"])
        log.debug "Car added to Devices..."
    }
}
private removeChildDevices(delete)
{
	log.debug "Deleting ${delete.size()} Teslas"
	delete.each {
		//state.suppressDelete[it.deviceNetworkId] = true
		deleteChildDevice(it.deviceNetworkId)
		//state.suppressDelete.remove(it.deviceNetworkId)
	}
}
//Car LIST Methods
Map carsDiscovered() {
	def devices = getcarList()
    log.debug "Preparing Options... ${devices}"
    def map = [:]
   	devices.each {
    	//remove a device if already added
        if(!getChildDevice((String) it?.id)){
			def value = "${it?.display_name}"
			def key = "${it?.vin}"
			map["${key}"] = value
    	}
	}
    log.debug "Options are ready...${map}"
	return map    
}
def getcarList() {
	def devices = []
    def carListParams = [
    	uri: appSettings.OWNER_API_URI,
        path: "/api/1/vehicles",
        contentType: ANY,
        headers: ["Authorization": "${state.token_type} ${state.access_token}"]
    ]
    log.debug "Getting Car List...${carListParams}"
    try {
    	httpGet(carListParams) { resp ->
    		log.debug resp.status
    		if(resp.status == 200) {
            	log.debug "Preparing Cars List..."
        		resp.data.response.each {
                	if (it?.remote_start_enabled == true){
                		devices += it
                    }
                }
                log.debug "CarList ${devices}"
        	} else {
        		log.error "car list: unknown response"
        	}
    	}
	} catch (groovyx.net.http.HttpResponseException e) {
    		log.debug "Failed Http Call"
			result.reason = "Bad login"
	}
    //store car list in the state
    state.cars = devices
    log.debug "State with Cars: $state"
	return devices
}
//LOGIN METHODS
private forceLogin(){
	log.debug "Removing state values..."
	deleteState()
    login()
}
private login() {
	if(isAccessTokenValid().success == true) {
		return [success:true]
	}
	return doLogin()
}
private doLogin() {
	def grant_type = "password"
	def client_id = appSettings.CLIENT_ID
    def client_secret = appSettings.CLIENT_SECRET
	def loginParams = [
		uri: appSettings.OWNER_API_URI,
        path: "/oauth/token",
		contentType: ANY,
		body: "grant_type=${grant_type}&client_id=${client_id}&client_secret=${client_secret}&email=${username}&password=${password}"
	]
	def result = [success:false]

    try {
    	log.debug "Attempting Login.."
    	httpPost(loginParams) { resp ->
        	if (resp.status == 200) {
            	log.debug "Login Success..."
                log.debug "Setting State values..."
                setState(resp.data.access_token, resp.data.token_type, resp.data.created_at, resp.data.expires_in, resp.data.refresh_token)
                result.success = true 
            } else {
            	log.debug "Bad Response" & resp.status
            	result.reason = "Bad login"
                result.status = resp.status
            }
    	}
	} catch (groovyx.net.http.HttpResponseException e) {
    		log.debug "Failed Http Call"
			result.reason = "Bad login"
	}
	return result
}
//STATE update routines
private deleteState(){
	state.access_token = ""
    state.token_type = ""
    state.created_at = (long) 0
    state.expires_in = (long) 0
    state.refresh_token = ""
    state.cars = ""
    log.debug "Deleted State: $state"
}

private setState(String access_token, String token_type, long created_at, long expires_in, String refresh_token){
	state.access_token = access_token
    state.token_type = token_type
    state.created_at = created_at
    state.expires_in = expires_in
    state.refresh_token = refresh_token
    log.debug "New State: $state"
}
private setStateCarsList(String[] cars){
	state.cars = cars
    log.debug "New State: $state"
}
private isAccessTokenValid(){
	def result = [success:false]
    if(state.access_token){
		if ((long) state.created_at != 0 ){
    		log.debug "Checking previous token validity..."
			def exp = new Date( (state.created_at + state.expires_in) * 1000)
            log.debug "Token Expires: ${exp}"
    		def now = new Date()
    		if (state.created_at + state.expires_in > ((long) now.getTime() /1000) ){
            	log.debug "Valid Token"
	        	result = [success:true]
    	    } 
    	}
    } 	
    return result
}

//car status
private refresh(String dev_id){
	def vehicle_id = dev_id.split("-")[0]
	def dev = getChildDevice(vehicle_id)
    def pollParams = getParams()
    log.info "Refreshing status for ${dev.getLabel()}"
    //GUI Settings 
    pollParams.path = "/api/1/vehicles/${id}/data_request/gui_settings" 
    def guiStat = doGet(pollParams)
    if(guiStat){
    	log.debug "GUI Data ${guiStat.data}"
	    dev.sendEvent(name: "distUnit", value: guiStat.data.response.gui_distance_units.substring(0,2)) 
    	dev.sendEvent(name: "tempUnit", value: guiStat.data.response.gui_temperature_units)
    }
    //climate state 
    pollParams.path = "/api/1/vehicles/${vehicle_id}/data_request/climate_state"
    def cliStat = doGet(pollParams)
    if(cliStat){
    	log.debug "Climate Data ${cliStat.data}"
    	dev.sendEvent(name: "temperature", value: cliStat.data.response.inside_temp)
        dev.sendEvent(name: "thermostatSetpoint", value: cliStat.data.response.driver_temp_setting)
        if(cliStat.data.response.is_climate_on){
        	if(cliStat.data.response.inside_temp < cliStat.data.response.driver_temp_setting){
    			dev.sendEvent(name: "thermostatOperatingState", value: "heating")
        		dev.sendEvent(name: "thermostatMode", value: "heat")
                dev.sendEvent(name: "heatingSetpoint", value: cliStat.data.response.driver_temp_setting)
        	} else {
            	dev.sendEvent(name: "thermostatOperatingState", value: "cooling")
        		dev.sendEvent(name: "thermostatMode", value: "cool")
                dev.sendEvent(name: "coolingSetpoint", value: cliStat.data.response.driver_temp_setting)
			}
            dev.sendEvent(name: "switch", value: "on")
        } else {
        	dev.sendEvent(name: "thermostatOperatingState", value: "idle")
            dev.sendEvent(name: "thermostatMode", value: "off")
            dev.sendEvent(name: "switch", value: "off")
        }
    }
    //vehicle state 
    pollParams.path = "/api/1/vehicles/${vehicle_id}/data_request/vehicle_state"
    def vehStat = doGet(pollParams)
    if(vehStat){
    	log.debug "Vehicle Status ${vehStat.data}"
        dev.sendEvent(name: "odo", value: (long) vehStat.data.response.odometer)
    	//dev.sendEvent(name: "door", value: "lock") //data.locked is true
    }
    //charge state
    pollParams.path = "/api/1/vehicles/${vehicle_id}/data_request/charge_state"
    def batStat = doGet(pollParams)
    if(batStat){
    	log.debug "Charge Status ${batStat.data}"
        dev.sendEvent(name: "soclimit", value: batStat.data.response.charge_limit_soc.toString())
        getChildDevice("${vehicle_id}-Chg").sendEvent(name: "level", value: batStat.data.response.charge_limit_soc.toString())
    	//dev.sendEvent(name: "connected", value: "on") //data.charging_state.toString()
    	dev.sendEvent(name: "batmiles", value: batStat.data.response.est_battery_range.toString())
    	dev.sendEvent(name: "battery", value: batStat.data.response.battery_level.toString())
	}
}
//car command events
private command(cmd, dev_id){ 
	def vehicle_id = dev_id.split("-")[0]
	log.debug "Command received ${cmd} For Car ${vehicle_id}"
    //check if the login token valid
    if(isAccessTokenValid().success == false) {
    	log.debug "Token expired.. "
        return
    }
    //first find the child
    def car = getChildDevice(vehicle_id)
	if(!car){
    	log.debug "Couldn't find the car ${vehicle_id}"
        return
    }
    //prepare params + header
  	def commParams = getParams();
    switch(cmd){
    	case "honk":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/honk_horn"
            break
        case "flash":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/flash_lights"
            break
        case "lock":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/door_lock"
            break
        case "unlock":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/door_unlock"
            break
        case "openport":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/charge_port_door_open"
            break
        case "chargelevel":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/set_charge_limit"
            def lvl = getChildDevice("${vehicle_id}-Chg").currentState("level").value.toFloat().intValue()
           	commParams.body = "percent=${lvl}"
            break
        case "chargestandard":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/charge_standard"
            break
        case "chargemax":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/charge_max_range"
            break
        case "startcharge":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/charge_start"
            break
        case "stopcharge":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/charge_stop"
            break
        case "starthvac":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/auto_conditioning_start"
            break
        case "stophvac":
        	commParams.path = "/api/1/vehicles/${vehicle_id}/command/auto_conditioning_stop"
            break
        default:
        	log.debug "No Command received..."
            return
    }
    log.debug "Sending Command : ${commParams}"
    def resp = doPost(commParams)
    if(resp){
    	log.debug "Command status ${resp.data}"
        refresh(vehicle_id)
    }
}
//climate control
private setTemp(temp, vehicle_id){ 
	log.debug "Climate setting received For Car ${vehicle_id}"
    //check if the login token valid
    if(isAccessTokenValid().success == false) {
    	log.debug "Token expired.. "
        return
    }
    //first find the child
    def car = getChildDevice(vehicle_id)
	if(!car){
    	log.debug "Couldn't find the car ${vehicle_id}"
        return
    }
    //prepare params + header
  	def commParams = getParams();
    commParams.path = "/api/1/vehicles/${vehicle_id}/command/set_temps"
    commParams.body = "driver_temp=${temp}&passenger_temp=${temp}"
    log.debug "Command is ${commParams.path}"
    def resp = doPost(commParams)
    if(resp){
    	log.debug "Command status ${resp.data}"
        refresh(vehicle_id)
    }
}
private setChgLevel(level, vehicle_id){
	log.info "Changing Charge Limit for Car ${vehicle_id}"
    //first get the charging device state
    def chgDev = getChildDevice("${vehicle_id}-Chg")
    def setLvl = chgDev.currentState("level").value.toFloat().intValue()
    setLvl += level
    chgDev.setLevel(setLvl)
}
//http functions
def getParams(){
	def params = [
    	uri: appSettings.OWNER_API_URI,
        contentType: ANY,
        path: "",
        body: "",
        headers: ["Authorization": "${state.token_type} ${state.access_token}"]
    ]
    return params
}
def doGet(params){
	try {
    	httpGet(params) { resp -> 
        	if(resp.status == 200) {
        		return resp
            } else {
            	log.debug "Http Failed Status - ${resp.staus} - ${resp}"
            }
        } 
     } catch (groovyx.net.http.HttpResponseException e) {
    		log.debug "Failed Http Call - Event - ${e} - Parameters ${params}"
	}
}
def doPost(params){
	try {
    	httpPost(params) { resp -> 
        	if(resp.status == 200) {
        		return resp
            } else {
            	log.debug "Http Failed Status - ${resp.staus} - ${resp}"
            }
        } 
     } catch (groovyx.net.http.HttpResponseException e) {
    		log.debug "Failed Http Call - Event - ${e} - Parameters ${params}"
	}
}
