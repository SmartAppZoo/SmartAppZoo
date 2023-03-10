/**
 *  kvParent 0.1.3
 	0.1.3	vent close global options changed
    0.1.2a	update for todays change in todays map input change
    0.1.2	0.1.1 was a cruel and mean thing...
    0.1.1	fixed delay notification and null init issues
    0.1.0	detected setback 
    		force vent vo option, one time on page change, sets all zone vents to the selected option
    0.0.8a	fixed initial notify delay bug
    		moved vent polling to child
    0.0.8	other stuff to support the needs of the children
 	0.0.7	added fan run on delay
 *
 *  Copyright 2015 Mike Maxwell
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
    name		: "kvParent",
    namespace	: "MikeMaxwell",
    author		: "Mike Maxwell",
    description	: "parent application for 'Keen Vent Manager'",
    category	: "My Apps",
    iconUrl		: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png",
    iconX2Url	: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png"
)

preferences {
	page(name: "main")
    page(name: "reporting")
    page(name: "report")
    page(name: "advanced")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	//initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	state.vParent = "0.1.3"
    //subscribe(tStat, "thermostatSetpoint", notifyZones) doesn't look like we need to use this
    subscribe(tStat, "thermostatMode", checkNotify)
    subscribe(tStat, "thermostatFanMode", checkNotify)
    subscribe(tStat, "thermostatOperatingState", checkNotify)
    subscribe(tStat, "heatingSetpoint", checkNotify)
    subscribe(tStat, "coolingSetpoint", checkNotify)

	//init state vars
	state.mainState = state.mainState ?: getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
    state.mainMode = state.mainMode ?: getNormalizedOS(tStat.currentValue("thermostatMode"))
    state.mainCSP = state.mainCSP ?: tStat.currentValue("coolingSetpoint").toFloat()
    state.mainHSP = state.mainHSP ?: tStat.currentValue("heatingSetpoint").toFloat()
    
    checkNotify(null)
    
  	/*
    state.runMaps = []
    state.runTimes = []
    state.lastDPH = 0
    state.endTime = ""
    state.startTime = ""
    state.endTemp = ""
    state.startTemp = ""
    state.crntDtemp = ""
	state.estDtime = ""
	state.lastCalibrationStart = ""
    log.info "stat state:${tStat.currentValue("thermostatOperatingState")} runMaps:${state.runMaps.size()}"
    //app.updateLabel("${settings.zoneName} Vent Zone") 
    */
    
}

/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
        ,title		: "Zones"
        ,install	: true
        ,uninstall	: installed
        ){	if (installed){
        		section(){
        			app(name: "childZones", appName: "kvChild", namespace: "MikeMaxwell", description: "Create New Vent Zone...", multiple: true)	
                }
           		section("Reporting"){
         			href( "reporting"
						,title		: "Available reports..."
						,description: ""
						,state		: null
					)                
                }
                section("Advanced"){
                	//advanced hrefs...
					href( "advanced"
						,title			: "Advanced features..."
						,description	: ""
						,state			: null
					)
                }                
             }
		     section("Configuration"){
                   	input(
                        name			: "tStat"
                        ,title			: "Main Thermostat"
                        ,multiple		: false
                        ,required		: true
                        ,type			: "capability.thermostat"
                        ,submitOnChange	: false
                    )
					input(
            			name			: "tempSensors"
                		,title			: "Thermostat temperature sensor:"
                		,multiple		: false
                		,required		: true
                		,type			: "capability.temperatureMeasurement"
                        ,submitOnChange	: false
            		) 
                    def froTitle = 'Close vents at cycle completion is '
                    if (!fanRunOn || fanRunOn == "-1"){
                    	froTitle = froTitle + "[off]"
                    } else {
                    	froTitle = froTitle + "[on]"
                    }
                    input(
            			name			: "fanRunOn"
                        ,title			: froTitle
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                		,options		: [["-1":"Use zone settings"],["0":"Immediate"],["60":"After 1 Minute"],["120":"After 2 Minutes"],["180":"After 3 Minutes"],["240":"After 4 Minutes"],["300":"After 5 Minutes"]]
                        ,submitOnChange	: true
                   		,defaultValue	: "-1"
            		)             
            }
            if (installed){
                section (getVersionInfo()) { }
            }
	}
}

def advanced(){
    return dynamicPage(
    	name		: "advanced"
        ,title		: "Advanced Options"
        ,install	: false
        ,uninstall	: false
        ){
         section(){
 			input(
            	name			: "setVo"
               	,title			: "Force vent opening to:"
               	,multiple		: false
               	,required		: true
               	,type			: "enum"
                ,options		:[["-1":"Do not change"],["0":"Fully closed"],["10":"10%"],["20":"20%"],["30":"30%"],["40":"40%"],["50":"50%"],["60":"60%"],["70":"70%"],["80":"80%"],["90":"90%"],["100":"Fully open"]]
                ,defaultValue	: "-1"
                ,submitOnChange	: true
            )     
            def vo = -1
            if (setVo){
            	vo = setVo.toInteger()
                if (vo > -1) paragraph (setChildVents(vo))
            }
            
         	input(
            	name			: "logLevel"
               	,title			: "IDE logging level" 
               	,multiple		: false
                ,required		: true
                ,type			: "enum"
 				,options		: [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"]]
                ,submitOnChange	: false
                ,defaultValue	: "10"
            )  
            
        }
    }
}

def reporting(){
	def report
	return dynamicPage(
    	name		: "reporting"
        ,title		: "Zone reports"
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
            	report = "Configuration"
   				href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				) 
                report = "Current state"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)   
                report = "Last results"
                href( "report"
					,title		: report
					,description: ""
					,state		: null
					,params		: [rptName:report]
				)  
            }
   }
}

def report(params){
	def reportName = params.rptName
	return dynamicPage(
    	name		: "report"
        ,title		: reportName
        ,install	: false
        ,uninstall	: false
        ){
    		section(){
   				paragraph(getReport(reportName))
            }
   }
}

def getReport(rptName){
	def cMethod
    //[stat:[mainState:heat|cool|auto,mainMode:heat|cool|idle,mainCSP:,mainHSP:,mainOn:true|false]]
    
    def reports = ""
	if (rptName == "Current state"){
    	cMethod = "getZoneState"
        def t = tempSensors.currentValue("temperature")
        reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}\n\tcooling set point: ${tempStr(state.mainCSP)}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
    }
    if (rptName == "Configuration"){
    	cMethod = "getZoneConfig"
       	def t = tempSensors.currentValue("temperature")
        reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}\n\tcooling set point: ${tempStr(state.mainCSP)}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
    }
    if (rptName == "Last results") cMethod = "getEndReport"
    def sorted = childApps.sort{it.label}
    sorted.each{ child ->
    	//log.debug "getting child report for: ${child.label}"
        try {
    		def report = child."${cMethod}"()
        	reports = reports + child.label + ":${report}" + "\n"
        }
        catch(e){}
    }
    return reports
}

def checkNotify(evt){

	//logger(10|20|30|40,"error"|"warn"|"info"|"debug"|"trace",text)
    logger(40,"debug","checkNotify:enter- ")

	//log.debug "thermostat event- name: ${evt.name} value: ${evt.value} , description: ${evt.descriptionText}"
    //[stat:[mainMode:heat|cool|auto,mainState:heat|cool|idle,mainCSP:,mainHSP:,mainOn:true|false]]
	
    //[msg:"zone", data:[name:app.label,event:"installed"]]
    //def msg = params.msg
    //def initRequest = evt == "zoneRequest"
    //logger(30,"warn","checkNotify zoneRequest- from a new Zone")
	def tempStr = ''
    def tempFloat = 0.0
    def delay = settings.fanRunOn.toInteger()
	
	tempStr = getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
	def mainState = state.mainState
    def mainStateChange = mainState != tempStr
    mainState = tempStr
    logger(40,"info","checkNotify- mainState: ${mainState}, mainStateChange: ${mainStateChange}")
    
    tempStr = getNormalizedOS(tStat.currentValue("thermostatMode"))
    def mainMode = state.mainMode
    def mainModeChange = mainMode != tempStr
    mainMode = tempStr
    logger(40,"info","checkNotify- mainMode: ${mainMode}, mainModeChange: ${mainModeChange}")

	tempFloat = tStat.currentValue("coolingSetpoint").toFloat()
    def mainCSP = state.mainCSP
    def mainCSPChange = mainCSP != tempFloat
    mainCSP = tempFloat
    logger(40,"info","checkNotify- mainCSP: ${mainCSP}, mainCSPChange: ${mainCSPChange}")

	tempFloat = tStat.currentValue("heatingSetpoint").toFloat()
    def mainHSP = state.mainHSP
    def mainHSPChange = mainHSP != tempFloat
    mainHSP = tempFloat
    logger(40,"info","checkNotify- mainHSP: ${mainHSP}, mainHSPChange: ${mainHSPChange}")
    
    def mainOn = mainState != "idle"
    //always update state vars
    state.mainState = mainState
    state.mainMode = mainMode
    state.mainCSP = mainCSP
    state.mainHSP = mainHSP
    
    
    if (mainStateChange || mainModeChange || mainCSPChange || mainHSPChange){
    	//[stat:[mainState:,mainMode:,mainCSP:,mainHSP:,mainOn:]
        if (mainStateChange) logger(10,"info","Main HVAC state changed to: ${mainState}")
        if (mainModeChange) logger(10,"info","Main HVAC mode changed to: ${mainMode}")
        if (mainCSPChange) logger(10,"info","Main HVAC cooling setpoint changed to: ${mainCSP}")
        if (mainHSPChange) logger(10,"info","Main HVAC heating setpoint changed to: ${mainHSP}")
        
    	def dataSet = [msg:"stat",data:[initRequest:false,mainState:mainState,mainStateChange:mainStateChange,mainMode:mainMode,mainModeChange:mainModeChange,mainCSP:mainCSP,mainCSPChange:mainCSPChange,mainHSP:mainHSP,mainHSPChange:mainHSPChange,mainOn:mainOn,delay:delay]]
    	logger(30,"debug","dataSet: ${dataSet}")
		notifyZones(dataSet)
    }
    logger(40,"debug","checkNotify:exit- ")
}

def notifyZone(){
	//initial data request for new zone
    def mainState = getNormalizedOS(tStat.currentValue("thermostatOperatingState"))
    def mainMode = getNormalizedOS(tStat.currentValue("thermostatMode"))
    def mainCSP = tStat.currentValue("coolingSetpoint").toFloat()
    def mainHSP = tStat.currentValue("heatingSetpoint").toFloat()
    def mainOn = mainState != "idle"
	def dataSet = [msg:"stat",data:[initRequest:true,mainState:mainState,mainMode:mainMode,mainCSP:mainCSP,mainHSP:mainHSP,mainOn:mainOn]]
    logger(40,"debug","notifyZone:enter- map:${dataSet}")
    return dataSet
}

def notifyZones(map){
    logger(40,"debug","notifyZones:enter- map:${map}")
    childApps.each {child ->
    	child.zoneEvaluate(map)
    }
    logger(40,"debug","notifyZones:exit- ")
}

def setChildVents(vo){
	logger(40,"debug","setChildVents:enter- vo:${vo}")
    def result = "Setting zone vents to ${vo}%\n"
    childApps.each {child ->
    	child.setVents(vo)
        result = result + "\t${child.label}, was set...\n"
    }
    logger(40,"debug","setChildVents:exit- ")
    return result
}

def getNormalizedOS(os){
	def normOS = ""
    if (os == "heating" || os == "pending heat" || os == "heat" || os == "emergency heat"){
    	normOS = "heat"
    } else if (os == "cooling" || os == "pending cool" || os == "cool"){
    	normOS = "cool"
    } else if (os == "auto"){
    	normOS = "auto"
    } else if (os == "off"){
    	normOS = "off"
    } else {
    	normOS = "idle"
    }
    //log.debug "normOS- in:${os}, out:${normOS}"
    return normOS
}

def statHandler(evt){
	log.info "event:${evt.value}"

    def key = evt.date.format("yyyy-MM-dd HH:mm:ss")
    def v  = evt.value
    def evtTime = evt.date.getTime()
    if (v == "heating"){
        state.lastCalibrationStart = key
        state.startTime = evtTime
        state.startTemp = tempSensors.currentValue("temperature")
        log.info "start -time:${state.startTime} -temp:${state.startTemp}"
    	if (!state.lastDPH){
        	state.lastDPH = 0	
        } else {
        	state.crntDtemp = tStat.currentValue("heatingSetpoint") -  state.startTemp
            state.estDtime = state.crntDtemp / state.lastDPH
            
        }
    } else if (v == "idle" && state.startTime) {
    	//end
        state.endTime = evtTime
        def BigDecimal dTime = (state.endTime - state.startTime) / 3600000
        state.endTemp = tempSensors.currentValue("temperature")
        log.info "end -time:${state.endTime} -temp:${state.endTemp}"
        if (state.runTimes.size == 0){
        	state.runTimes = ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        } else {
        	state.runTimes << ["${key}":"runTime:${dTime} startTemp:${state.startTemp} endTemp:${state.endTemp}"]
        }
        if (state.endTime > state.startTime && state.endTemp > state.startTemp ){
        	def BigDecimal dTemp  = (state.endTemp - state.startTemp)
            
            def BigDecimal dph = dTemp / dTime
            if (dTime >= 0.5) {
               	def value = ["CurrentDPH":"${dph}","lastDPH":"${state.lastDPH}" ,"ActualRunTime":"${dTime}","EstimatedRunTime":"${state.estDtime}" ,"ActualTempRise":"${dTemp}","EstimatedTempRise":"${state.crntDtemp}"]
        		log.info "${value}"
            	if (state.runMaps.size == 0){
            		state.runMaps = ["${key}":"${value}"]
            	} else {
            		state.runMaps << ["${key}":"${value}"]
            	}
            }
            state.lastDPH = dph
            state.endTime = ""
            state.startTime = ""
            state.endTemp = ""
            state.startTemp = ""
        }
        
    }
}

def getVersionInfo(){
	return "Versions:\n\tkvParent: ${state.vParent}\n\tkvChild: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}

def getSelectedDevices(deviceList){
	def deviceIDS = []
    deviceList.each{ device ->
    	deviceIDS.add(device.id)
    }
	return deviceIDS
}

def logger(displayLevel,errorLevel,text){
	//logger(10|20|30|40,"error"|"warn"|"info"|"debug"|"trace",text)
    /*
    [10:"Lite"],[20:"Moderate"],[30:"Detailed"],[40:"Super nerdy"]
 
    errorLevel 	color		number
    error		red			5
    warn		yellow		4
    info		lt blue		3
    debug		dk blue		2
    trace		gray		1
    */
    def logL = 0
    if (logLevel) logL = logLevel.toInteger()
    
    if (logL == 0) return //bail
    else if (logL >= displayLevel) log."${errorLevel}"(text)

 }

