/**
 *  V3.2.0 Changed to having 1 master mode passed to zones
 *  V3.1.0 Changed zone to map control
 *  V3.0.0 Change control algo to use a single map and control/add mode control/start remove ecobee specific
 *	V2.7.0 Add humidifer fan control feature
 *	V2.6.0 equipment status for humidifier 
 *  V2.5.1 Fixed missing indicator issue
 *  V2.5.0 BETA Ecobee Climate Zone control
 * 	V2.1.0 Removed Unstable Integrator Control 
 *  V2.0.9 Heat and AC indicator logic updated
 *  V2.0.8 Ac now set as heat for non ac appliations
 *  V2.0.7 ac check for subscritpions
 *  V2.0.6 Fix install issue fan, heat, ac set to null
 *  V2.0.4 Fix night mode return vent opening
 *	V2.0.3 Fixed Reporting error by disabling reporting
 *  V2.0.2 Fixed namespace error
 *  V2.0.1 Change reducted output flag to match seting of 30%
 *  V2.0 Keenect with proportional control of keen vents
 *
 *  Copyright 2016 Brian Murphy
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
    name		: "KeenectMaster",
    namespace	: "Keenect",
    author		: "Mike Maxwell & Brian Murphy & Craig Romei",
    description	: "Keen Vent Manager",
    category	: "My Apps",
    iconUrl		: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg",
    iconX2Url	: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg",
	iconX3Url	: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg"
    )

preferences {
	page(name: "main")
    page(name: "reporting")
    page(name: "report")
    page(name: "advanced")
    
    
}

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
	state.vParent = "3.2"
	state.etf = app.id == '07d1abe4-352f-441e-a6bd-681929b217e4' //5
	
    //subscribe(tStat, "thermostatSetpoint", notifyZones) doesn't look like we need to use this
    subscribe(tStat, "thermostatMode", checkNotify)
    subscribe(tStat, "thermostatFanMode", checkNotify)
    subscribe(tStat, "thermostatOperatingState", checkNotify)
    subscribe(tStat, "equipmentStatus", checkNotify)

    subscribe(tStat, "heatingSetpoint", checkNotify)
    subscribe(tStat, "coolingSetpoint", checkNotify)
    //tempSensors
    subscribe(tempSensors, "temperature", checkNotify)
    //pressure switch
    subscribe(pressureSwitch, "contact", managePressure)

	//init state vars
    state.reduceoutput = false
    state.cool =false
    state.night = false
    subscribe(nightmode,"switch",nighthandler)
    state.mainMode = state.mainMode ?: tStat.currentValue("thermostatMode")
	state.mainState = state.mainState ?: getMainTstatState()
    if (state.mainMode.toUpperCase().contains("AUTO")||state.mainMode.toUpperCase().contains("COOL")){ 
		state.mainCSP = state.mainCSP ?: tStat.currentValue("coolingSetpoint").toFloat()
    }else {state.mainCSP = state.mainCSP ?: tStat.currentValue("heatingSetpoint").toFloat()
    }
    if (state.mainMode.toUpperCase().contains("AUTO")||state.mainMode.toUpperCase().contains("HEAT")){ 
		state.mainHSP = state.mainHSP ?: tStat.currentValue("heatingSetpoint").toFloat()
    }else {state.mainHSP = state.mainHSP ?: tStat.currentValue("coolingSetpoint").toFloat()
    }
    state.mainTemp = state.mainTemp ?: tempSensors.currentValue("temperature").toFloat()
    state.voBackoff = 0
    checkNotify(null)
}

def nighthandler(evt){
    log.info"nighthandler- evt name: ${evt.name}, value: ${evt.value}"
    def nightEnabled = evt.value == "on"
    if (nightmode){
    	if (nightEnabled){
        state.night = false
       		log.info"Zone was enabled via: [${nightmode.displayName}]"
    	} else {
        state.night =true
       		log.info"Zone was disabled via: [${nightmode.displayName}]"
    	}
    }
}
/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
def main(){
state.vParent = "3.1"
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
      //  ,title		: "Zones"
        ,install	: true
        ,uninstall	: installed
        ){	
             if (installed){
             	section("Reporting"){
         			href( "reporting"
						,title		: "Available reports..."
						,description: ""
						,state		: null
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

  		
                    
		    		def iacTitle = ""
                    if (isAC()) iacTitle = "System is AC capable"
                    else iacTitle = "System is heat only"
          			input(
            			name			: "isACcapable"
               			,title			: iacTitle 
               			,multiple		: false
               			,required		: true
               			,type			: "bool"
                		,submitOnChange	: true
                		,defaultValue	: true
            		)            
            }       
             if (installed){
        		section(){
        			app(name: "childZones", appName: "keenectZone", namespace: "Keenect", description: "Create New Vent Zone...", multiple: true)	
                }
             }
            if (installed){
                section("Advanced"){
                	def afDesc = "\t" + getTitle("logLevelSummary") + "\n\t" + getTitle("sendEventsToNotificationsSummary") + "\n\t" + getTitle("fanRunOn") + "\n\t" + getTitle("pressureSwitchSummary")
					href( "advanced"
						,title			: "" 
						,description	: "Configure advanced settings"
						,state			: null
					)
                }   
          	
                def dev  = ""
                if (state.etf) dev = "\n(development instance)"
            	section (getVersionInfo() + dev) { }    
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
            	name			: "logLevel"
               	,title			: "IDE logging level" 
               	,multiple		: false
                ,required		: true
                ,type			: "enum"
 				,options		: getLogLevels()
                ,submitOnChange	: false
                ,defaultValue	: "10"
            )  
         input(
            			name			: "indicators"
               			,title			: "Optional virtural indicator switches" 
               			,multiple		: false
               			,required		: true
               			,type			: "bool"
                		,submitOnChange	: true
                		,defaultValue	: false
            		)     
         
        if (indicators){
        input(
            			name			: "ACind"
                		,title			: "Optional Virtual Switch to indicate AC on/off"
                		,multiple		: false
                		,required		: false
                		,type			: "capability.switch"
                        ,submitOnChange	: false
            		)
                        input(
            			name			: "Heatind"
                		,title			: "Optional Virtual Switch to indicate Heat on/off"
                		,multiple		: false
                		,required		: false
                		,type			: "capability.switch"
                        ,submitOnChange	: false
            		)
                        input(
            			name			: "Fanind"
                		,title			: "Optional Virtual Switch to indicate Fan on/off"
                		,multiple		: false
                		,required		: false
                		,type			: "capability.switch"
                        ,submitOnChange	: false
            		)
                                         input(
            			name			: "outputreductionind"
                		,title			: "Optional Virtual Switch to indicate OutputReduction on/off"
                		,multiple		: true
                		,required		: false
                		,type			: "capability.switchLevel"
                        ,submitOnChange	: false
            		)
                    }
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
            if (settings.setVo){
            	vo = settings.setVo.toInteger()
                if (vo > -1) paragraph (setChildVents(vo))
            }
 
          	input(
            	name			: "sendEventsToNotifications"
               	,title			: getTitle("sendEventsToNotifications") 
               	,multiple		: false
               	,required		: false
               	,type			: "bool"
                ,submitOnChange	: true
                ,defaultValue	: false
            ) 
            input(
            	name			: "fanRunOn"
                ,title			: getTitle("fanRunOn")
            	,multiple		: false
                ,required		: true
                ,type			: "enum"
                ,options		: [["0":"Off"],["60":"1 Minute"],["120":"2 Minutes"],["180":"3 Minutes"],["240":"4 Minutes"],["300":"5 Minutes"]]
                ,submitOnChange	: true
                ,defaultValue	: "0"
            ) 
            input(
                name			: "pressureSwitch"
                ,title			: getTitle("pressureSwitch")
                ,multiple		: false
                ,required		: false
                ,type			: "capability.contactSensor"
                ,submitOnChange	: true
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
    def standardReport = false
    def t = tempSensors.currentValue("temperature")
    def reports = ""
    def cspStr = ""
    if (isAC()) cspStr = "\n\tcooling set point: ${tempStr(state.mainCSP)}"
	if (rptName == "Current state"){
    	standardReport = true
    	cMethod = "getZoneState"
        //get whole house average temp
        def averageTemp = 0
        childApps.each{ child ->
        	def zt = child.getZoneTemp()
        	if (zt) averageTemp = averageTemp + zt
        }
        averageTemp = (averageTemp / childApps.size()).toDouble().round(1)
        reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}${cspStr}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
        reports = reports + "Average zone temp: ${tempStr(averageTemp)}\n\n"
    } 
    if (rptName == "Configuration"){
    	standardReport = true
    	cMethod = "getZoneConfig"
     //   reports = "Main system:\n\tstate: ${state.mainState}\n\tmode: ${state.mainMode}\n\tcurrent temp: ${tempStr(t)}${cspStr}\n\theating set point: ${tempStr(state.mainHSP)}\n\n"
    }  
    
    
    if (rptName == "Last results"){
    	standardReport = true
    	cMethod = "getEndReport"
        def stime = "No data available yet"
        def etime = "No data available yet"
        def sTemp = tempStr(state.startTemp)
        def eTemp  = tempStr(state.endTemp)
        def rtm = "No data available yet"
        if ((state.startTime && state.endTime) && (state.startTime < state.endTime)){
        	stime = new Date(state.startTime).format("yyyy-MM-dd HH:mm")
            etime =  new Date(state.endTime).format("yyyy-MM-dd HH:mm")
            rtm = ((state.endTime - state.startTime) / 60000).toInteger()
            rtm = "${rtm} minutes"
        } 
        reports = "Main system:\n\tstart: ${stime}\n\tend: ${etime}\n\tduration: ${rtm}\n\n"
    }
    if (standardReport){
    	def sorted = childApps.sort{it.label}
    	sorted.each{ child ->
       		try {
    			def report = child."${cMethod}"()
       			reports = reports + "Zone: " + child.label + "${report}" + "\n"
       		}
       		catch(e){}
        }
    } else {
    	//non standard reports
 
	}
    return reports
}

// main methods
def checkNotify(evt){
    logger(40,"debug","checkNotify:enter- ")
    logger(40,"debug","checkNotify:evt: ${evt}")
	logger(40,"debug","checkNotify:MainTstatState: ${getMainTstatState()}")
	def tempStr = ''
    def tempFloat = 0.0
    def tempBool = false
    def delay = 0
    if (settings.fanRunOn) delay = settings.fanRunOn.toInteger()
    def mainTemp = tempSensors.currentValue("temperature").toFloat()
    state.mainTemp = mainTemp
	
    //thermostat state
	tempStr = getMainTstatState()
	logger(40,"debug","checkNotify:MainTstatState: ${getMainTstatState()}")
	def mainState = state.mainState
    def mainStateChange = mainState != tempStr
    mainState = tempStr
	state.mainState = mainState
	def mainOn = mainState != "IDLE"
    logger(40,"info","checkNotify- mainState: ${mainState}, mainStateChange: ${mainStateChange}")
  
	//cooling set point
    def mainCSPChange = false
    def mainCSP
    if (isAC()){
		tempFloat = tStat.currentValue("coolingSetpoint").toFloat()
    	mainCSP = state.mainCSP
    	mainCSPChange = mainCSP != tempFloat
    	mainCSP = tempFloat
		state.mainCSP = mainCSP
    	logger(40,"info","checkNotify- mainCSP: ${mainCSP}, mainCSPChange: ${mainCSPChange}")
    }

	//heating set point
	tempFloat = tStat.currentValue("heatingSetpoint").toFloat()
    def mainHSP = state.mainHSP
    def mainHSPChange = mainHSP != tempFloat
    mainHSP = tempFloat
    state.mainHSP = mainHSP
    logger(40,"info","checkNotify- mainHSP: ${mainHSP}, mainHSPChange: ${mainHSPChange}")
    
  
    //Set indicators and start and stop timing for reports
    if(mainStateChange){
		if(indicators){
			switch (mainState){
				case "HEAT" :
						ACind.off()
						Fanind.off()
						Heatind.on()
					break
				case "COOL" :
						ACind.on()
						Fanind.off()
						Heatind.off()
					break
				case "FAN" :
						ACind.off()
						Fanind.on()
						Heatind.off()
					break
				default:
						ACind.off()
						Fanind.off()
						Heatind.off()
					break
			}
		}
		if (mainOn){
			//main start
			logger(10,"info","start log info cycle ended")
            
			state.startTime = now() + location.timeZone.rawOffset
			state.startTemp = mainTemp
			state.voBackoff = 0
			state.reduceoutput = false
		
			ChildNormalOutput()
		} else{
			//main end
			state.endTime = now() + location.timeZone.rawOffset
			state.endTemp = mainTemp
			state.voBackoff = 0
			state.reduceoutput = false
			logger(10,"info","write log info cycle ended")
		}
    }
    
    
    
    if (mainStateChange || mainCSPChange || mainHSPChange){
    	def dataSet = [msg:"stat",data:[initRequest:false,mainState:mainState,mainStateChange:mainStateChange,mainCSP:mainCSP,mainCSPChange:mainCSPChange,mainHSP:mainHSP,mainHSPChange:mainHSPChange,mainOn:mainOn]]
        if (dataSet == state.dataSet){
        	//dup dataset..., should never ever happen
            logger(30,"warn","duplicate dataset, zones will not be notified... dataSet: ${state.dataSet}")
        } else {
        	logger(30,"debug","dataSet: ${dataSet}")
            if (mainStateChange) logger(10,"info","Main HVAC state changed to: ${mainState}")
        	if (mainModeChange) logger(10,"info","Main HVAC mode changed to: ${mainMode}")
        	if (mainCSPChange && isAC()) logger(10,"info","Main HVAC cooling setpoint changed to: ${mainCSP}")
        	if (mainHSPChange) logger(10,"info","Main HVAC heating setpoint changed to: ${mainHSP}")
            state.dataSet = dataSet
            if (delay > 0 && mainState == "IDLE"){
				logger(10,"info", "Mainstate ${mainState}")
                logger(10,"info", "Zone notification is scheduled in ${delay} delay")
				runIn(delay,notifyZones)
        	} else {
        		notifyZones()
               //  logger(10,"info", "else Zone notification")
                
        	}
        }
    }
    logger(40,"debug","checkNotify:exit- ")
}


def GetMasterData(){
 	//initial data request for new zone
     logger(10,"info", "Zone notification now")
     def mainState = state.mainState
     def mainMode = state.mainMode
     def mainCSP =state.mainCSP
     def mainHSP = state.mainHSP
     def mainOn = mainState != "IDLE"
 	def dataSet = [msg:"stat",data:[initRequest:true,mainState:mainState,mainMode:mainMode,mainCSP:mainCSP,mainHSP:mainHSP,mainOn:mainOn,mainES:mainES]]
     logger(10,"debug","notifyZone:enter- map:${dataSet}")
     return dataSet
 }
 
def getMainTstatState(){
	def TstatState = tStat.currentValue("thermostatOperatingState")
	if (TstatState!=null){
		TstatState = TstatState.toUpperCase()
	} else {
    	TstatState = "NULL"
    }
     logger(10,"debug","getMainTstatState:TstatState: ${TstatState}")
	def mainES = tStat.currentValue("equipmentStatus")
	if (mainES!=null){
		mainES = mainES.toUpperCase()
	} else {
    	mainES = "NULL"
    }
     logger(10,"debug","getMainTstatState:mainES: ${mainES}")
	def mainMode = tStat.currentValue("thermostatMode")
	if (mainMode!=null){
		mainMode = mainMode.toUpperCase()
	} else {
    	mainMode = "NULL"
    }
     logger(10,"debug","getMainTstatState:mainMode: ${mainMode}")
	 state.mainMode = mainMode
	def mainFanMode = tStat.currentValue("thermostatFanMode")
	if (mainFanMode!=null){
		mainFanMode = mainFanMode.toUpperCase()
	} else {
    	mainFanMode = "NULL"
    }
     logger(10,"debug","getMainTstatState:mainFanMode: ${mainFanMode}")
	 
	if (TstatState.contains("HEAT")||mainES.contains("HEAT")) {
		TstatState = "HEAT"
	} else if (TstatState.contains("COOL")||mainES.contains("COOL")){
		TstatState = "COOL"
	} else if (mainES.contains("HUMIDIFIER")){
    	TstatState = "HUMIDIFIER"  
	}else if (TstatState.contains("FAN ONLY")||mainES.contains("FAN RUNNING")||mainFanMode.contains("ON")){
    	TstatState = "FAN"  
	} 
	
	return TstatState
 }
 
 
def notifyZones(altDS){
    logger(40,"debug","notifyZones:enter- ")
    def dataSet 
    if (altDS) dataSet = altDS
    else dataSet = state.dataSet
    childApps.each {child ->
    	child.zoneEvaluate(dataSet)
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

def managePressure(evt){
	//"open" = OK/clear "closed" = no good baby...
    def backOffRate = 5
	//pressure alert
    if (evt.value == "closed"){
    	state.voBackoff = state.voBackoff + 5
    	//first instance...
 		if (state.voBackoff == backOffRate){
           	logger(10,"warn","Initial pressure alert!, opening vents to 100%, initial backOff set to ${state.voBackoff}%")
       	} else {
           	logger(10,"warn","Continued pressure alert!, opening vents to 100%, backOff set to ${state.voBackoff}%")
       	}
       	setChildVents(100)
    } else {
       	if (state.voBackoff == backOffRate){
       		logger(10,"info","Initial alert cleared, trying with backOff at ${state.voBackoff}%")
       	} else {
       		logger(10,"info","Alert cleared, trying again with backOff at ${state.voBackoff}%")
       	}
    	notifyZones([msg:"pressureAlert",data:state.voBackoff])
    }
}

def manageoutputreduction(nor){
	//"false" = not needed 
logger (30,"info", "nor ${nor}")
  //state.outputflag = true
    if (nor == true){
    	state.reduceoutput = true
       ChildReduceOutput()
       logger (30,"info", "PARENT state.reduceoutput = true") 
 
}
  // if (nor == false){
    //    runIn (70, ChildNormalOutput)
     //          log.info "PARENT schedule ouput state.reduceoutput = ${state.reduceoutput}"          
//}
}

def ChildNormalOutput(){
 state.reduceoutput = false
 if(outputreductionind){
 outputreductionind.setLevel(90)
 }
logger (30,"info", "PARENT Ouput Reduction Off")
   childApps.each {child ->
    	child.allzoneoffset(false)}
        }


def ChildReduceOutput(){
logger (30,"info","PARENT Output Reduction On")
if(outputreductionind){
outputreductionind.setLevel(15)
}
  runIn (65, ChildNormalOutput)
        state.ouputflag = false
   childApps.each {child ->
    	child.allzoneoffset(true)}
}


def getVersionInfo(){
	return "Versions:\n\tKeenect: ${state.vParent ?: "No data available yet."}\n\tkeenectZone: ${state.vChild ?: "No data available yet."}"
}

def updateVer(vChild){
    state.vChild = vChild
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp != 0 && temp != null) return "${temp.toString()}°${tc}"
    else return "No data available yet."
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
    else if (logL >= displayLevel){
    	log."${errorLevel}"(text)
        if (sendEventsToNotifications && displayLevel == 10) {
          	def nixt = now() + location.timeZone.rawOffset
        	def today = new Date(nixt).format("HH:mm:ss.Ms")
        	text = "Main:" + today + ": " + text
        	sendNotificationEvent(text) //sendEvent(name: "kvParent", value: text, descriptionText: text, isStateChange : true)
        }
    }
}

def getLogLevel(val){
	def logLvl = 'Lite'
    def l = getLogLevels()
    if (val){
    	logLvl = l.find{ it."${val}"}
        logLvl = logLvl."${val}".value
    }
    return '[' + logLvl + ']'
}

def getLogLevels(){
    return [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"]]
}

def getID(){
	return state.etf
}

def isAC(){
	return (settings.isACcapable == null || settings.isACcapable)
}

def hasPressure(){
	return (settings.pressureSwitch) ?: false
}

def getTitle(name){
	def title = ""
	switch(name){
        case "logLevelSummary" :
        	title = "Log level is " + getLogLevel(settings.logLevel)
        	break            
        case "sendEventsToNotifications" :
        	title = settings.sendEventsToNotifications ?  "Send Lite events to notification feed is [on]" : "Send Lite events to notification feed is [off]" 
        	break   
        case "sendEventsToNotificationsSummary" :
        	title = settings.sendEventsToNotifications ?  "Notification feed is [on]" : "Notification feed is [off]" 
        	break   
		case "fanRunOn" :
            title = 'Delay zone cycle end notification is '
            if (!settings.fanRunOn || settings.fanRunOn == "0"){
               	title = title + "[off]"
            } else {
               	title = title + "[on]"
            }  			
        	break  
		case "pressureSwitch" :
 			title = settings.pressureSwitch ? "Over pressure contact:\n\twhen closed, pressure is over limit\n\twhen open, pressure is under limit " : "Optional over pressure contact"
        	break  
		case "pressureSwitchSummary" :
        	if (hasPressure()){
 				title = settings.pressureSwitch ? "Over pressure contact: selected" : "Over pressure contact: not selected"
            }
        	break             
	}
    return title
}