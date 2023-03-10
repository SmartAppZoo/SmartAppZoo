 /*
 *V3.2.0 Simplified messaging between master and zone
 *V3.1.0 Added zone thermostat control
 *V3.0.0 Remove Ecobee Specific/Add mode control/Simplify code
 *V2.8.5 Changed code to use data maps and smaller functions
 *V2.8.4 bug fix for vent name setup error
 *V2.8.3 bug fix for return air vents
 *V2.8.2 set default value for return air vents to false
 *V2.8.1 fix for vent open min at cooling
 *V2.8.0 Humidifier fan vent control
 *V2.7.0 Fix for cooling vent opeing min and max error
 *V2.6.0 Stable ecobee climate zone control and added better control of fan on and heat or cool run.
 *V2.5.0 Beta ecobee climate zone control
 *V2.4.0 Removed Unstable Integrator control *BETA only for heat need to add for cool
 *V2.3.2 New logic for reduced ouput request from child to parent
 *V2.3.1 Added aditional logic for reduce output in fan only mode
 *V2.3.0 Fix install issue fan set to null
 *V2.2.1 Added logc for child if parent is AC
 *V2.2.0 New Feature in fan only mode, zone will evaluate local temperature and compare against setpoint and open vents accordingly to adjust zone temp using fan.
 *V2.1.1 Changed min/max vent control thresholds
 *V2.1.0 Added zone fan only vent level setting
 *V2.0.2 Fix isssue with fan after heat vent position
 *V2.0.1 Fix condition where zone disabled but toggling between minimum opening and output reduction state resulting in opening and closing of vents
 *V2.0 KeenectZone with proportional control of keen vents
 *
 *
 *
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
    name: "keenectZone",
    namespace: "Keenect",
    author: "Mike Maxwell & Brian Murphy & Craig Romei",
    description: "zone application for 'Keenect', do not install directly.",
    category: "My Apps",
    parent: "Keenect:KeenectMaster",
    iconUrl: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg",
    iconX2Url: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg",
	iconX3Url	: "https://raw.githubusercontent.com/napalmcsr/SmartThingsStuff/master/smartapps/keenect/clipart-thermometer-thermometer-clipart-free-6-thermometer-clip-art-clipartix-free-clipart.jpg"
   
)

preferences {
	page(name: "main")
    page(name: "advanced")
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    settings.minVo = 0
    settings.maxVo = 100
    settings.minVoC = 0
    settings.maxVoC = 100
    settings.FanVoC = 100
    settings.FanAHC = 100
    settings.coolOffset = 0
    settings.staticCSP = 70
    state?.integrator= 0 
    state.acactive = false
    state.running = false
    state.LocalTstatHSP = 50
    state.LocalTstatCSP = 100
    state.zoneneedofset = false
    state.parentneedoffset = false
    state.outputreduction =false
    state.switchDisable = false
    state.VentMode = "Install"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    state.vChild = "3.2.0"
    unsubscribe()
	initialize()
    
}

def initialize() {
    state.vChild = "3.2.0"
    log.debug("init")
    parent.updateVer(state.vChild)
    subscribe(tempSensors, "temperature", tempHandler)
    if (zoneControlType == "thermostat"){
        subscribe(LocaltStat, "heatingSetpoint", setTstatHSP)
        subscribe(LocaltStat, "coolingSetpoint", setTstatCSP)
    }

    if (UseSwitchConrol){
    	subscribe(ControlSwitch, "switch", switchHandler)
	}
    subscribe(vents, "level", levelHandler)
    log.debug("isAC")
    state.isAC = parent.isAC() //AC enable bits
    zoneEvaluate(parent.GetMasterData())
    log.debug("Done Init")
}

//dynamic page methods
def main(){
	def installed = app.installationState == "COMPLETE"
    state.outputreduction =false
   
	return dynamicPage(
    	name		: "main"
        ,title		: "Zone Configuration"
        ,install	: true
        ,uninstall	: installed
        ){	section(){
          		label(
                   	title		: "Name the zone"
                    ,required	: true
                )      
        	}
		    section("Devices in Zone"){
                input(
                    name			: "vents"
                    ,title			: "Keen vents in this zone:"
                    ,multiple		: true
                    ,required		: true
                    ,type			: "capability.switchLevel"
                    ,submitOnChange	: true
				)
           
 				input(
            		name		: "tempSensors"
                	,title		: "Temperature sensor for zone:"
                	,multiple	: false
                	,required	: true
                	,type		: "capability.temperatureMeasurement"
                    ,submitOnChange	: false
            	) 
            }
            section("Zone Settings"){
                  
                input(
                	name			: "zoneControlType"
                    ,title			: "Temperature control type"
                    ,multiple		: false
                    ,required		: true
                    ,type			: "enum"
                    ,options		: [["offset":"Offset from Main set point"],["fixed":"Fixed"],["thermostat":"Use a simulated thermostat"]]
                    ,defaultValue	: "offset"
                    ,submitOnChange	: true
                )
                switch (zoneControlType){
                    case "offset" :
                        input(
                            name			: "heatOffset"
                            ,title			: "Heating offset, (above or below main thermostat)"
                            ,multiple		: false
                            ,required		: false
                            ,type			: "enum"
                            ,options 		: zoneTempOptions()
                            ,defaultValue	: "0"
                            ,submitOnChange	: false
                        ) 
                        if (parent.isAC()){
                            input(
                                name			: "coolOffset"
                                ,title			: "Cooling offset, (above or below main thermostat)"
                                ,multiple		: false
                                ,required		: false
                                ,type			: "enum"
                                ,options 		: zoneTempOptions()
                                ,defaultValue	: "0"
                                ,submitOnChange	: false
                            )
                         }
                        break
                    case "fixed" :
                        input(
                            name			: "staticHSP"
                            ,title			: "Heating set point"
                            ,multiple		: false
                            ,required		: false
                            ,type			: "enum"
                            ,options 		: zoneFixedOptions()
                            ,defaultValue	: "70"
                            ,submitOnChange	: false
            			) 
                    	if (parent.isAC()){
                    		input(
                                name			: "staticCSP"
                                ,title			: "Cooling set point"
                                ,multiple		: false
                                ,required		: false
                                ,type			: "enum"
                                ,options 		: zoneFixedOptions()
                                ,defaultValue	: "70"
                                ,submitOnChange	: false
                            )
                        } 
                        break
                    case "thermostat" :
                        input(
                        name			: "LocaltStat"
                        ,title			: "Zone Thermostat"
                        ,multiple		: false
                        ,required		: true
                        ,type			: "capability.thermostat"
                        ,submitOnChange	: false
                    )
                        break
              	}
                
                	            
                
				input(
            		name			: "minVo"
                	,title			: "Heat minimum vent opening"
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                    ,options		: minVoptions()
                    ,defaultValue	: "0"
                    ,submitOnChange	: true
            	) 
               
					input(
            			name			: "maxVo"
                		,title			: "Heat maximum vent opening"
                		,multiple		: false
                		,required		: true
                		,type			: "enum"
                    	,options		: maxVoptions()
                    	,defaultValue	: "100"
                    	,submitOnChange	: true
            		) 
                

                    if (parent.isAC()){
                    input(
                        name            : "minVoC"
                        ,title          : "Cooling minimum vent opening"
                        ,multiple       : false
                        ,required       : false
                        ,type           : "enum"
                        ,options        : minVoptions()
                        ,defaultValue	: "0"
                        ,submitOnChange : true
                    ) 
                   
                        input(
                            name            : "maxVoC"
                            ,title          : "Cooling maximum vent opening"
                            ,multiple       : false
                            ,required       : false
                            ,type           : "enum"
                            ,options        : maxVCoptions()
                            ,defaultValue : "100"
                            ,submitOnChange : true
                        ) 
                    }
                                   		input(
            		name			: "FanAHC"
                	,title			: "Vent minimum opening during fan only"
                	,multiple       : false
                    ,required       : false
                    ,type           : "enum"
                    ,options        : FANoptions()
                    ,defaultValue : "100"
                    ,submitOnChange : true
            	)
                input(
            		name			: "FanHum"
                	,title			: "Vent minimum opening during humidifier only"
                	,multiple       : false
                    ,required       : false
                    ,type           : "enum"
                    ,options        : FANoptions()
                    ,defaultValue : "50"
                    ,submitOnChange : true
            	)
     		}
                
			section("Location Mode Control") {
          			input(
            			name			: "RunInAllModes"
               			,title			: "Run for all modes" 
               			,multiple		: false
               			,required		: true
               			,type			: "bool"
                		,submitOnChange	: true
                		,defaultValue	: true
            		)   
                
                    if(!RunInAllModes){
                        input "modes", "mode", title: "select a mode(s)", multiple: true
                        input(
                                name			: "OutofModeVO"
                                ,title			: "Percent open for the vent when not in Mode"
                                ,multiple		: false
                                ,required		: false
                                ,type			: "enum"
                                ,options 		: FANoptions()
                                ,defaultValue	: "10"
                                ,submitOnChange	: false
                        ) 
                  	}
                }
                
                section("Switch conrtol Mode") {
          			input(
            			name			: "UseSwitchConrol"
               			,title			: "Use a switch to control Keenect Zone" 
               			,multiple		: false
               			,required		: true
               			,type			: "bool"
                		,submitOnChange	: true
                		,defaultValue	: false
            		)   
                    if(UseSwitchConrol){
                    	input "ControlSwitch", "capability.switch", required: false
                        input(
                            name			: "SwitchDisable"
                            ,title			: "Switch Disables Keenect" 
                            ,multiple		: false
                            ,required		: true
                            ,type			: "bool"
                            ,submitOnChange	: true
                            ,defaultValue	: false
                        )   
                        input(
                                name			: "ControlSwitchVO"
                                ,title			: "Percent open for the vent when Switch Disables Zone"
                                ,multiple		: false
                                ,required		: false
                                ,type			: "enum"
                                ,options 		: FANoptions()
                                ,defaultValue	: "100"
                                ,submitOnChange	: false
                        ) 
                  	}
                }

            section("Advanced"){
				def afDesc = "\t" + getTitle("AggressiveTempVentCurve") + "\n\t" + getTitle("ventCloseWait") + "\n\t" + getTitle("logLevelSummary") + "\n\t" + getTitle("sendEventsToNotificationsSummary") + "\n\t" + getTitle("pressureControl")
                href( "advanced"
                    ,title			: ""
					,description	: "Settings"
					,state			: null
				)
            }
	}
}

def advanced(){
    def pEnabled = false
    try{ pEnabled = parent.hasPressure() }
    catch(e){}
    return dynamicPage(
    	name		: "advanced"
        ,title		: "Advanced Options"
        ,install	: false
        ,uninstall	: false
        ){
         section(){
          		input(
            		name			: "AggressiveTempVentCurve"
                	,title			: getTitle("AggressiveTempVentCurve") 
                	,multiple		: false
                	,required		: false
                	,type			: "bool"
                    ,submitOnChange	: true
                    ,defaultValue	: false
            	) 
                         input(
                    name			: "Rvents2"
                    ,title			: "Return air Keen vents in this zone:"
                    ,multiple		: true
                    ,required		: false
                    ,type			: "capability.switchLevel"
                    ,submitOnChange	: true
				)
                input(
            		name			: "Rventsenabled"
                	,title			: "Return air vents open during zone enabled"
                	,multiple		: false
                	,required		: false
                	,type			: "bool"
                    ,submitOnChange	: true
                    ,defaultValue	: false
          
                )
            	input(
            		name			: "ventCloseWait"
                    ,title			: getTitle("ventCloseWait")
                	,multiple		: false
                	,required		: true
                	,type			: "enum"
                	,options		: [["-2":"On disable only"],["-1":"Do not close"],["0":"Immediate"],["60":"After 1 Minute"],["120":"After 2 Minutes"],["180":"After 3 Minutes"],["240":"After 4 Minutes"],["300":"After 5 Minutes"]]
                	,submitOnChange	: true
                   	,defaultValue	: "-1"
            	)

                   
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
            		name			: "sendEventsToNotifications"
                	,title			: getTitle("sendEventsToNotifications") 
                	,multiple		: false
                	,required		: false
                	,type			: "bool"
                    ,submitOnChange	: true
                    ,defaultValue	: false
            	)      
                if (pEnabled){
          			input(
            			name			: "pressureControl"
                		,title			: getTitle("pressureControl") 
                		,multiple		: false
                		,required		: false
                		,type			: "bool"
                    	,submitOnChange	: true
                    	,defaultValue	: true
            		)                  
				}              
        }
    }
}

def zoneClimate(ecobeePrograms){
	log.debug "programs: $ecobeePrograms"
	state.ecobeeprograms= ecobeePrograms
}

	   
//zone control methods
def zoneEvaluate(params){
	//settings.logLevel=40
	settings.zoneControlSwitch = 40
	logger(40,"debug","zoneEvaluate:enter-- parameters: ${params}")
	if (isIntegrator== false) state?.integrator= 0 
    // variables
    def evaluateVents = false
    def msg = params.msg
    def data = params.data
    //main states
    def mainStateLocal = state.mainState ?: ""
    def mainModeLocal = state.mainMode ?: ""
	def mainHSPLocal = state.mainHSP  ?: 0
	def mainCSPLocal = state.mainCSP  ?: 0
	def mainOnLocal = state.mainOn  ?: ""
	def localintegrator = state.integrator.toFloat()
    localintegrator=localintegrator.round(3)
	def zoneDisabledLocal = state.zoneDisabled
    //fetchZoneControlState()
    def runningLocal
    //always fetch these since the zone ownes them
    def zoneTempLocal = tempSensors.currentValue("temperature").toFloat()
    def coolOffsetLocal 
    if (settings.coolOffset) coolOffsetLocal = settings.coolOffset.toInteger()
    def heatOffsetLocal = settings.heatOffset.toInteger()
    def zoneCloseOption = -1
    if (settings.ventCloseWait) zoneCloseOption = settings.ventCloseWait.toInteger()
    def minVoLocal = settings.minVo.toInteger() 
    def maxVoLocal = settings.maxVo.toInteger()
    if (parent.isAC()){
               //  logger(10,"info","is ac")
		def minVoCLocal = settings.minVoC.toInteger()
		def maxVoCLocal = settings.maxVoC.toInteger()
         //       logger(10,"warn","max: ${maxVoCLocal}%")
        //    logger(10,"warn","min: ${minVoCLocal}%")
    }
   // def fanVoLocal = settings.FanVoC.toInteger()
    def fanAHLocal = settings.FanAHC.toInteger()
    def VoLocal = state.zoneVoLocal
    def pEnabled = false
    try{ pEnabled = parent.hasPressure() }
    catch(e){} 
    if (pEnabled && settings.pressureControl != false){
    	//back off adjustment here
        def backOff = parent.getBackoff()
        if (backOff > 0){
            logger(10,"warn","Keenect says backoff vents by: ${backOff}%")
        	if (minVoLocal != 100 && (minVoLocal + backOff) < 100){
                logger(20,"info","zoneEvaluate- backOff minVo- current settings: ${minVoLocal}, changed to: ${minVoLocal + backOff}")
        		minVoLocal = minVoLocal + backOff
        	} else {
				logger(20,"info","zoneEvaluate- backOff could not change minVo (it's already at 100%)")
			}
        	if (maxVoLocal != 100 && (maxVoLocal + backOff) < 100){
                logger(20,"info","zoneEvaluate- backOff maxVo- current settings: ${maxVoLocal}, changed to: ${maxVoLocal + backOff}")
        		maxVoLocal = maxVoLocal + backOff
        	} else {
                logger(20,"info","zoneEvaluate- backOff could not change maxVo (it's already at 100%)")
        	}
        }
    }
    //set it here depending on zoneControlType
	def zoneCSPLocal = state.mainCSP  ?: 0
    switch (zoneControlType){
    	case "offset" :
			zoneCSPLocal = zoneCSPLocal + settings.coolOffset.toInteger()
        	break
    	case "fixed" :
			zoneCSPLocal = settings.staticCSP.toInteger()
        	break
    	case "thermostat" :
			zoneCSPLocal = state.LocalTstatCSP.toFloat().toInteger()
        	break
        }
		state.zoneCSP = zoneCSPLocal
	
	def zoneHSPLocal = state.mainHSP  ?: 0
	switch (zoneControlType){
		case "offset" :
			zoneHSPLocal = zoneHSPLocal + settings.heatOffset.toInteger()
			break
		case "fixed" :
			zoneHSPLocal = settings.staticHSP.toInteger()
			break
		case "thermostat" :
			zoneHSPLocal = state.LocalTstatHSP.toFloat().toInteger()
			break
		}
		state.zoneHSP = zoneHSPLocal
	

	logger(40,"debug","zoneEvaluate:msg ${msg}")
    switch (msg){
    	case "stat" :
                if (data.initRequest){
                	if (!zoneDisabledLocal) evaluateVents = data.mainOn
                //set point changes, ignore setbacks
                } else if (data.mainStateChange){
						//system start up
                        logger(10,"info","zoneEvaluate- data.mainState: ${data.mainState}, data.mainOn: ${data.mainOn}")  
                	if (data.mainOn && !zoneDisabledLocal){
                        evaluateVents = true
                        runningLocal = true
                        logger(30,"info","zoneEvaluate- system start up, evaluate: ${evaluateVents}")
                        logger(10,"info","Main HVAC is on and ${data.mainState}")
						//system shut down
                    } else {
                    	runningLocal = false
						def asp = state.activeSetPoint
                        if (zoneTempLocal != null && asp != null){
                            state?.zoneTempLocal=zoneTempLocal
                        }
                        runIn(60*4, integrator)
                        logger(10,"info","Main HVAC has shut down state end report available in 5 minutes.")  
						//check zone vent close options from zone
                    	if (zoneCloseOption >= 0){
                        	 closeWithOptions(zoneCloseOption)
                       	} 
     				}
                } else {
                	logger(30,"warn","zoneEvaluate- ${msg}, no matching events")
                }
                //always update data
                mainStateLocal = data.mainState
                mainModeLocal = data.mainMode
                mainHSPLocal = data.mainHSP
                mainCSPLocal = data.mainCSP
                mainOnLocal = data.mainOn
        	break
        case "temp" :
                if (!zoneDisabledLocal){
                	logger(30,"debug","zoneEvaluate- zone temperature changed, zoneTemp: ${zoneTempLocal}")
                	evaluateVents = true
                } else {
                    logger(30,"warn", "Zone temp change ignored, zone is disabled")
                }
        	break
        case "vent" :
        		logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
        	break
        case "zoneSwitch" :
                //fire up zone since it was activated
                if (!zoneDisabledLocal){
                	evaluateVents = true
                //shut it down with options
                } else {
                	if (mainOnLocal){
                  		if (zoneTempLocal != null && asp != null){
                            state?.zoneTempLocal=zoneTempLocal
                        }
                        runIn(60*5, integrator)
                        logger(10,"info","Main HVAC has shut down state end report available in 5 minutes.")    
                        runningLocal = false
    				} 
                    //check zone vent close options from zone
                    if (zoneCloseOption == -2){
                    	closeWithOptions(0)
                    }
                    logger(10,"info", "Zone was disabled, we won't be doing anything alse until it's re-enabled")
                }
        	break
        case "pressure" :
        		logger(30,"debug","zoneEvaluate- msg: ${msg}, data: ${data}")
        	break
        case "pressureAlert" :
               	logger(30,"debug","zoneEvaluate- pressureAlert, data: ${data}")
                //notifyZones([msg:"pressureAlert",data:state.voBackoff])
                logger(10,"warn","Pressure alert cleared, resetting zone VO's...")
				//if pressure is disabled locally, reset vents to previous vo
                if (settings.pressureControl == false){
                	if (state.lastVO != null){
                    	setVents(state.lastVO)
                    }
                } else {
                	evaluateVents = true
                }
        	break
    }    
	def tempBool = false
	state.fanonly =false
	state.AggressiveTempVentCurveActive = false
    if (settings.AggressiveTempVentCurve){
    	 state.AggressiveTempVentCurveActive = true
	}
    //write state
    logger(30,"debug","zoneEvaluate- Writing Params")

    state.mainState = mainStateLocal
    state.mainMode = mainModeLocal
	state.mainHSP = mainHSPLocal
	state.mainCSP = mainCSPLocal
	state.mainOn = mainOnLocal
    state.zoneCSP = zoneCSPLocal
    state.zoneHSP = zoneHSPLocal
    state.zoneTemp = zoneTempLocal
	state.zoneDisabled = zoneDisabledLocal
 	state.running = runningLocal
	if (evaluateVents){
		def outred = false  
		if (Rvents2== true){
			if (Rventsenabled== true){
				if (mainStateLocal == "HEAT"){
					setRVents(100)
				}
					if (mainStateLocal == "COOL"){
					setRVents(0)
				}
			}
		}
		evaluateVentsOpening()
	} else {
		logger(10,"info","Nothing to do, main HVAC is not running, mainState: ${mainStateLocal}, zoneTemp: ${zoneTempLocal}, zoneHSP: ${zoneHSPLocal}, zoneCSP: ${zoneCSPLocal}")
	}
/*
    logger(30,"debug","zoneEvaluate- Check for reductions")

	if (state.fanonly == false){                                                               
		//  log.info "output reduction ${state.outputreduction} zone need ${state.zoneneedofset}"
		if (state.acactive == true){
			if (state.parentneedoffset == false){
				parent.manageoutputreduction(false)
				logger (30,"info", "CHILD Clearing System Reduced Ouput")
			}
			if (state.parentneedoffset == true){
				parent.manageoutputreduction(true)
				logger (30,"info", "CHILD Requesting System Reduced Ouput")
			}      
		}
		if (state.acactive == false){
			parent.manageoutputreduction(false)
			logger (30,"info","CHILD Clearing System Reduced Ouput fan only")
		}
	}else {
		parent.manageoutputreduction(false)
		logger (30,"info", "CHILD Clearing System Reduced Ouput")
	}      
    */
    logger(40,"debug","zoneEvaluate:exit- ")
}


//event handlers

def levelHandler(evt){
	logger(40,"debug","levelHandler:enter- ")
    def ventData = state."${evt.deviceId}"
    def v = evt.value.toFloat().round(0).toInteger()
    def t = evt.date.getTime()
    if (ventData){
        //request
        if (evt.description == ""){
			ventData.voRequest = v	
            ventData.voRequestTS = t
            logger(30,"debug","levelHandler- request vo: ${v} t: ${t}")
		//response
		} else {
        	ventData.voResponse = v
            ventData.voResponseTS = t
            ventData.voTTC = ((t - ventData.voRequestTS) / 1000).toFloat().round(1)
            logger(30,"debug","levelHandler- response vo: ${v} t: ${t} voTTC: ${ventData.voTTC}")
        }
    } else {
    	//request
    	if (evt.description == ""){
    		state."${evt.deviceId}" =  [voRequest:v,voRequestTS:t,voResponse:null,voResponseTS:null,voTTC:null] 
            logger(30,"debug","levelHandler-init request vo: ${v} t: ${t}")
        //response
        } else {
        	state."${evt.deviceId}" =  [voRequest:null,voRequestTS:null,voResponse:t,voResponseTS:v,voTTC:null] 
            logger(30,"debug","levelHandler-init response vo: ${v} t: ${t}")
        }
    }
    
    logger(40,"debug","levelHandler:exit- ")
}

def allzoneoffset(val){
  //  logger(10,"info","From Parent Output reduction value: ${val}")
    if (val == true){
       		logger(30,"info", "Zone output reduction enabled")
            state.outputreduction = true
    	} else if (val == false){
       		logger(30,"info", "Zone output redution disabled")
            state.outputreduction =false
    	}
   // logger(10,"info","zone output reduction:exit- ")
}


def tempHandler(evt){
    logger(40,"debug","tempHandler- evt name: ${evt.name}, value: ${evt.value}, state.mainOn: ${state.mainOn}" )
    state.zoneTemp = evt.value.toFloat()
    if (state.mainOn){
    	logger(30,"debug","tempHandler- tempChange, value: ${evt.value}")
    	zoneEvaluate([msg:"temp", data:["tempChange"]])	
    }     
}

//misc utility methods
def closeWithOptions(zoneCloseOption){
	if (zoneCloseOption == 0){
		logger(10,"warn", "Vents closed via close vents option")
		setVents(0)
	} else if (zoneCloseOption > 0){
		logger(10,"warn", "Vent closing is scheduled in ${zoneCloseOption} seconds")
		runIn(zoneCloseOption,delayClose)
	}          	
}

def logger(displayLevel,errorLevel,text){
	//input logLevel 1,2,3,4,-1
    /*
    [1:"Lite"],[2:"Moderate"],[3:"Detailed"],[4:"Super nerdy"]
    input 	logLevel
    
    1		Lite		
    2		Moderate	
    3		Detailed
    4		Super nerdy
    
    errorLevel 	color		number
    error		red			5
    warn		yellow		4
    info		lt blue		3
    debug		dk blue		2
    trace		gray		1
    */
    def logL = 10
    if (logLevel) logL = logLevel.toInteger()
    
    if (logL == 0) {return}//bail
    else if (logL >= displayLevel){
    	log."${errorLevel}"(text)
        if (sendEventsToNotifications && displayLevel == 10){
        	def nixt = now() + location.timeZone.rawOffset
        	def today = new Date(nixt).format("HH:mm:ss.Ms")
        	text = today + ": " + text
        	sendNotificationEvent(app.label + ": " + text) 
        }
    }
}

def setVents(newVo){
	logger(40,"debug","setVents:enter- ")
	logger(30,"warn","setVents- newVo: ${newVo}")
    def result = ""
    def changeRequired = false
    
	settings.vents.each{ vent ->
    	def changeMe = false
	logger(30,"warn","setVents- newVo: ${vent.currentValue("level")}")
		def crntVo = vent.currentValue("level").toInteger()
        def isOff = vent.currentValue("switch") == "off"
        /*
        	0 = 0 for sure
        	> 90 = 100, usually
        	the remainder is a crap shoot
            0 == switch == "off"
            > 0 == switch == "on"
            establish an arbitrary +/- threshold
            if currentLevel is +/- 5 of requested level, call it good
            otherwise reset it
		*/
        if (newVo != crntVo){
        	def lB = crntVo - 10    
            
            //92-6 
            def uB = crntVo + 10
        	if (newVo == 100 && crntVo < 90){
            	//logger(10,"info","newVo == 100 && crntVo < 90: ${newVo == 100 && crntVo < 90}")
            	changeMe = true
            } else if ((newVo < lB || newVo > uB) && newVo != 100){
            	//logger(10,"info","newVo < lB || newVo > uB && newVo != 100: ${(newVo < lB || newVo > uB) && newVo != 100}")
            	changeMe = true
            }
        }
        if (changeMe || isOff){
        	changeRequired = true
        	vent.setLevel(newVo)
            state?.ventcheck=newVo
            runIn(60*1, ventcheck)
        }
        log.info("setVents- [${vent.displayName}], changeRequired: ${changeMe}, current vo: ${crntVo}, new vo: ${newVo}")
    }
    logger(40,"debug","setVents:exit- ")
}

def ventcheck(){
	//if (state.enabled == true){
    logger(40,"debug","ventcheck:enter- ")
		def newVo=state.ventcheck
		setVents(newVo)
    logger(40,"debug","ventcheck:exit- ")
	//}
}

def setRVents(newVo){
	logger(40,"debug","setRVents:enter- ")
	logger(30,"warn","setRVents- newVo: ${newVo}")
    def result = ""
    def changeRequired = false
    if(Rvents2){
		settings.Rvents2.each{ Rvent2 ->
			def changeMe = false
			def crntVo = Rvent2.currentValue("level").toInteger()
			def isOff = Rvent2.currentValue("switch") == "off"
			/*
				0 = 0 for sure
				> 90 = 100, usually
				the remainder is a crap shoot
				0 == switch == "off"
				> 0 == switch == "on"
				establish an arbitrary +/- threshold
				if currentLevel is +/- 5 of requested level, call it good
				otherwise reset it
			*/
			if (newVo != crntVo){
				def lB = crntVo - 5    
				
				//92-6 
				def uB = crntVo + 10
				if (newVo == 100 && crntVo < 90){
					//logger(10,"info","newVo == 100 && crntVo < 90: ${newVo == 100 && crntVo < 90}")
					changeMe = true
				} else if ((newVo < lB || newVo > uB) && newVo != 100){
					//logger(10,"info","newVo < lB || newVo > uB && newVo != 100: ${(newVo < lB || newVo > uB) && newVo != 100}")
					changeMe = true
				}
			}
			if (changeMe || isOff){
                if (Rventsenabled == true){
                    logger(10,"warn","rvents true")
                    log.info("XX NewVO- [${newVo}]")
                    changeRequired = true
                    Rvents2.setLevel(newVo)
                    state.Rventcheck=newVo
                    runIn(60*1, Rventcheck)
                }
			}
			log.info("setVents- [${Rvent2.displayName}], changeRequired: ${changeMe}, new vo: ${newVo}, current vo: ${crntVo}")
		}
		

		if (changeRequired) result = ", setting Rvents to ${newVo}"
		else result = ", vents at ${newVo}"
		return result
		logger(40,"debug","setRVents:exit- ")
    }
}

def Rventcheck(evt){
	//if (state.enabled == true){
		def newVo=state.Rventcheck
		setRVents(newVo)
	//}
}


def delayClose(){
    setVents(0)
    logger(10,"warn","Vent close executed")
}

def tempStr(temp){
    def tc = state.tempScale ?: location.temperatureScale
    if (temp) return "${temp.toString()}°${tc}"
    else return "No data available yet."
}

//dynamic page helpers
def getTitle(name){
	def title = ""
	switch(name){
    	case "AggressiveTempVentCurve" :
        	title = settings.AggressiveTempVentCurve ?  "Aggressive Temp vent Curve is [on]" : "Aggressive Temp vent Curve is  [off]"
        	break
        case "ventCloseWait" :
        	title = 'Close vent options are '
            if (!settings.ventCloseWait || settings.ventCloseWait == "-1"){
               	title = title + "[off]"
            } else {
             	title = title + "[on]"
            }
        	break
        case "zoneControlSwitch" :
        	title = settings.zoneControlSwitch ? "Optional zone control switch:\n\twhen on, zone is enabled\n\twhen off, zone is disabled " : "Optional zone control switch"
        	break
        case "zoneControlSwitchSummary" :
        	title = settings.zoneControlSwitch ? "Zone control switch: selected" : "Zone control switch: not selected"
        	break            
        case "logLevelSummary" :
        	title = "Log level is " + getLogLevel(settings.logLevel)
        	break            
        case "sendEventsToNotifications" :
        	title = settings.sendEventsToNotifications ?  "Send Lite events to notification feed is [on]" : "Send Lite events to notification feed is [off]" 
        	break   
        case "sendEventsToNotificationsSummary" :
        	title = settings.sendEventsToNotifications ?  "Notification feed is [on]" : "Notification feed is [off]" 
        	break   
		case "pressureControl" :
        	def pEnabled = false
            try{ pEnabled = parent.hasPressure() }
    		catch(e){}
        	if (pEnabled){
            	if (settings.pressureControl == false){
            		title = "Pressure management is [off]" 
            	} else {
            		title = "Pressure management is [on]"
            	}
            }
        	break               
	}
    return title
}

def minVoptions(){
	return [["0":"Fully closed"],["5":"5%"],["10":"10%"],["15":"15%"],["20":"20%"],["25":"25%"],["30":"30%"],["35":"35%"],["40":"40%"],["45":"45%"],["50":"50%"],["55":"55%"],["60":"60%"]]
}
def maxVCoptions(){
    def opts = []
    def start = 0
    start.step 95, 5, {
        opts.push(["${it}":"${it}%"])
    }
    opts.push(["100":"Fully open"])
    return opts
}
def maxVoptions(){
	def opts = []
    def start = 0
    start.step 95, 5, {
   		opts.push(["${it}":"${it}%"])
	}
    opts.push(["100":"Fully open"])
    return opts
}

def getLogLevels(){
    return [["0":"None"],["10":"Lite"],["20":"Moderate"],["30":"Detailed"],["40":"Super nerdy"]]
}

def FANoptions(){
	def opts = []
    def start = 0
    start.step 95, 5, {
        opts.push(["${it}":"${it}%"])
    }
    opts.push(["100":"Fully open"])
    return opts
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

def zoneFixedOptions(){
	def opts = []
    def start
    if (!state.tempScale) state.tempScale = location.temperatureScale
	if (state.tempScale == "F"){
    	start = 60
        start.step 81, 1, {
   			opts.push(["${it}":"${it}°F"])
		}
    } else {
    	start = 15
        start.step 27, 1, {
   			opts.push(["${it}":"${it}°C"])
		}
    }
	return opts
}

def zoneTempOptions(){
	def zo
    if (!state.tempScale) state.tempScale = location.temperatureScale
	if (state.tempScale == "F"){
    	zo = [["8":"8°F"],["5":"5°F"],["4":"4°F"],["3":"3°F"],["2":"2°F"],["1":"1°F"],["0":"0°F"],["-1":"-1°F"],["-2":"-2°F"],["-3":"-3°F"],["-4":"-4°F"],["-5":"-5°F"],["-8":"-8°F"]]
    } else {
    	zo = [["5":"5°C"],["4":"4°C"],["3":"3°C"],["2":"2°C"],["1":"1°C"],["0":"0°C"],["-1":"-1°C"],["-2":"-2°C"],["-3":"-3°C"],["-4":"-4°C"],["-5":"-5°C"]]
    }
	return zo
}

//report methods, called from parent
def getEndReport(){
	return state.endReport ?: "\n\tNo data available yet."
}

def getZoneConfig(){
	//zoneControlSwitch
    def zc = "Not Activated"
    def cspStr = ""
    if (parent.isAC()){
        if (zoneControlType == "fixed") cspStr = "\n\tcooling set point: ${tempStr(settings.staticCSP)}"
        else cspStr = "\n\tcooling offset: ${tempStr(settings.coolOffset)}"
    }
    def hspStr = ""
    if (zoneControlType == "fixed") hspStr = "heating set point: ${tempStr(settings.staticHSP)}"
    else hspStr = "heating offset: ${tempStr(settings.heatOffset)}"
    
    def zt = hspStr + cspStr
    //if (zoneControlSwitch) zc = "is ${zoneControlSwitch.currentValue("switch")} via [${zoneControlSwitch.displayName}]"
	return "\n\tVents: ${vents}\n\ttemp sensor: [${tempSensors}]\n\tminimum vent opening: ${minVo}%\n\tmaximum vent opening: ${maxVo}%\n\t${zt}\n\tzone control: ${zc}"
}

def getZoneInt(){
	float t = state.integrator
  //  log.info "${t}"
  t= t.round(4)
   // log.info "${t}"
	return ": Integrator: ${t}"
}

def getZoneState(){
    def s 
    if (state.running == true) s = true
    else s = false
    def qr = false
    if (settings.AggressiveTempVentCurve && state.AggressiveTempVentCurveActive && s) qr = true
		def report =  "\n\tVentMode: ${state.VentMode}\n\trunning: ${s}\n\tqr active: ${qr}\n\tcurrent temp: ${tempStr(state.zoneTemp)}\n\tset point: ${tempStr(state.activeSetPoint)}"
		vents.each{ vent ->
 		def b = vent.currentValue("battery") ? vent.currentValue("battery") + "%" : "No data yet"
        def l = vent.currentValue("level").toInteger()
        
        def d = state."${vent.id}"
        def lrd = "No data yet"
        def rtt = "response time: No data yet"
        if (d){
        	def t = d.voResponseTS
            def r = d.voTTC
            if (t) lrd = (new Date(d.voResponseTS + location.timeZone.rawOffset ).format("yyyy-MM-dd HH:mm")).toString()
            if (r) rtt = "response time: ${r}s"
        }
		report = report + "\n\tVent: ${vent.displayName}\n\t\tlevel: ${l}%\n\t\tbattery: ${b}\n\t\t${rtt}\n\t\tlast response: ${lrd}"
    }
    return report
}

def getZoneTemp(){
	return state.zoneTemp
}

def integrator(){
	log.info "Starting Generate Integrator"
	//if (state.acactive == true){
		//log.info "Last run state.integrator ${state.integrator}"
		def zoneTempLocal = state.zoneTempLocal
		def asp = state.activeSetPoint
		def d
		d = (zoneTempLocal - asp).toFloat()
		/* d = (d*0.50).round(2)

		if (d>0.15 || d<-0.15){
			log.info "${d}>0.15 || ${d}<-0.15"
			}else { d= 0}
			log.info "zonetemplocal - active set point ${d}"
			if (d > 0.4){
				d=0.4}
			if (d < -0.4){
				d=-0.4}
			state?.integrator = (state.integrator + (d))
			if (state.integrator >= 3) {
				state.integrator =3
				log.info "state.integrator truncated to 3"
			}
			if (state.integrator <= -3) {
				state.integrator =-3
				log.info "state.integrator truncated to -3"
			}
			float intround= state.integrator 
			intround=intround.round(4)
			state.integrator=intround
			log.info "new state.integrator ${state.integrator}"

			*/                   
			state.endReport = "\n\tVentMode: ${state.VentMode}\n\tsetpoint: ${tempStr(asp)}\n\tend temp: ${tempStr(zoneTempLocal)}\n\tvariance: ${tempStr(d)}\n\tvent levels: ${vents.currentValue("level")}"        
		//}else {
		//log.info"fan only no chage of state.integrator ${state.integrator}"
	//}
	state.acactive = false
	log.info "state accctive false end of report"

}

def isIntegrator(){
	return (settings.isIntegrator == null || settings.isIntegrator)
}

//zone control methods
def evaluateVentsOpening(){

	logger(10,"info","evaluateVentsOpening: Setting vent Params")
    
	def ventmode = "none"
    ventmode = getVentMode()
	Map VentParams = [:]
   
    logger(40,"debug","evaluateVentsOpening: ventmode: ${ventmode}")
	switch (ventmode){
    	case "HEAT" :
			VentParams = SetHeatVentParams()
        	break
    	case "COOL" :
			VentParams = SetCoolVentParams()
        	break
    	case "FAN" :
			VentParams = SetFanVentParams()
        	break
    	case "HUMIDIFIER" :
			VentParams = SetHumVentParams()
        	break
    	case "OUT OF MODE" :
			VentParams = SetOutofModeVentParams()
        	break
    	case "SWITCH DISABLE" :
			VentParams = SetSwitchDisableParams()
        	break
    	default :
			VentParams = SetErrorVentParams()
        	break
               
	} 
	logger(10,"info","evaluateVentsOpening:Calculate Vent Opening")
	CalculteVent(VentParams)
    if (VentParams.allowReduction){
        logger(10,"info","evaluateVentsOpening:Check for Vent Reductions")
        logger(40,"debug","evaluateVentsOpening: VentParams.ventOpening before reduction checks: ${VentParams.ventOpening}")
        CheckReductions(VentParams)
        logger(40,"debug","evaluateVentsOpening:VentParams.ventOpening after reduction checks: ${VentParams.ventOpening}")
	}
	logger(10,"info","evaluateVentsOpening:Set The Vent")
	state.zoneVoLocal =  VentParams.ventOpening
	setVents(VentParams.ventOpening)
	logger(10,"info","evaluateVentsOpening: --EXIT")
}

//zone control methods
def getVentMode(){
    def allModes = settings.modes
	def ventmode = state.mainState ?: ""
    if (settings.RunInAllModes||allModes.contains(location.mode)) {
    } else {
        ventmode = "OUT OF MODE"
    }
    if (state.switchDisable&&settings.UseSwitchConrol){
		ventmode = "SWITCH DISABLE"
    }
	logger(40,"debug","GetVentMode: ventmode: ${ventmode}")
    state.VentMode = ventmode
    return ventmode
}

def CalculteVent(Map VentParams){
	if (VentParams.tempDelta<0){
		VentParams.ventOpening = 1
	} else{
    	
		VentParams.ventOpening = Math.round(VentParams.tempDelta*VentParams.ventSlope+VentParams.ventIntercept)
	}
	if (VentParams.ventOpening>VentParams.maxVentOpen){
		logger(30,"warn","CalculteVent- VentParams.ventOpening greater than Max")
		VentParams.ventOpening = VentParams.maxVentOpen
	}
	if (VentParams.ventOpening<VentParams.minVentOpen){
		logger(30,"warn","CalculteVent- VentParams.ventOpening less than Min")
		VentParams.ventOpening = VentParams.minVentOpen
	}
	logger(40,"debug","CalculteVent- VentParams.ventOpening after limit checks: ${VentParams.ventOpening}")
	
}
def CheckReductions(Map VentParams){
    logger(40,"debug","Evaluating reductions")
    if (VentParams.tempDelta>1.4){
        state.zoneneedofset = true
        logger(30,"info","CHILD zone needs offset")
    }else{
        state.zoneneedofset = false
        logger(30,"info","CHILD zone does not need offset")
    }
    if (VentParams.tempDelta>2){
        state.parentneedoffset = true
        logger(30,"info","Parent needs offset")
    }else{
        state.parentneedoffset = false
        logger(30,"info","Parent does not need offset")
    }
    if (state.outputreduction){
        if (state.zoneneedofset){
            logger(30,"info","This zone is far away from setpoint, opening to MAX")
            VentParams.ventOpening = maxVentOpen
        } else{
            logger(30,"info","This zone is being reduced due to other zones")
            VentParams.ventOpening = Math.round(VentParams.ventOpening*0.2)
        }
    }
				
}
	
def SetCoolVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Cool")
	def zoneCSPLocal = state.zoneCSP  ?: 0
    state.activeSetPoint = zoneCSPLocal
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = zoneTempLocal - zoneCSPLocal
	logger(40,"debug","SetCoolVentParams- resultMap.tempDelta: ${resultMap.tempDelta}")
	if (state.AggressiveTempVentCurveActive){
	logger(40,"debug","Setting Aggressive")
		resultMap.ventSlope = 200
		resultMap.ventIntercept = 60
	} else{
		resultMap.ventSlope = 70
		resultMap.ventIntercept = 30
	}
	resultMap.maxVentOpen = settings.maxVoC.toInteger()
	resultMap.minVentOpen = settings.minVoC.toInteger()
	resultMap.ventOpening = 50
	resultMap.allowReduction = true
	return resultMap
}

def SetHeatVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Heat")
	def zoneHSPLocal = state.zoneHSP  ?: 0
    state.activeSetPoint = zoneHSPLocal
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = zoneHSPLocal - zoneTempLocal
	if (state.AggressiveTempVentCurveActive){
		logger(40,"debug","Setting Aggressive")
		resultMap.ventSlope = 66
		resultMap.ventIntercept = 66
	} else{
		resultMap.ventSlope = 55
		resultMap.ventIntercept = 14
	}
	resultMap.maxVentOpen = settings.maxVo.toInteger()
	resultMap.minVentOpen = settings.minVo.toInteger()
	resultMap.ventOpening = 50
	resultMap.allowReduction = true
	return resultMap
}

def SetFanVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Fan")
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = 0.1
	resultMap.ventSlope = 0
	resultMap.ventIntercept = 50
	resultMap.maxVentOpen = 100
	resultMap.minVentOpen = settings.FanAHC.toInteger()
	resultMap.ventOpening = 50
	resultMap.allowReduction = false
	return resultMap
}

def SetHumVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Humidifier")
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = 0.1
	resultMap.ventSlope = 0
	resultMap.ventIntercept = settings.FanHum.toInteger()
	resultMap.maxVentOpen = 100
	resultMap.minVentOpen = 0
	resultMap.ventOpening = 50
	resultMap.allowReduction = false
	return resultMap
}

def SetErrorVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params ERROR")
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = 0.1
	resultMap.ventSlope = 0	
	resultMap.ventIntercept = 100
	resultMap.maxVentOpen = 100
	resultMap.minVentOpen = 0
	resultMap.ventOpening = 50
	resultMap.allowReduction = false
	return resultMap
}

def SetOutofModeVentParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Out of Mode")
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = 0.1
	resultMap.ventSlope = 0	
	resultMap.ventIntercept = settings.OutofModeVO.toInteger()
	resultMap.maxVentOpen = 100
	resultMap.minVentOpen = 0
	resultMap.ventOpening = 50
	resultMap.allowReduction = false
	return resultMap
}

def SetSwitchDisableParams(){
	Map resultMap = [:]
	logger(40,"debug","Setting vent Params Switch Disable")
	def zoneTempLocal = state.zoneTemp
	resultMap.tempDelta = 0.1
	resultMap.ventSlope = 0	
	resultMap.ventIntercept = settings.ControlSwitchVO.toInteger()
	resultMap.maxVentOpen = 100
	resultMap.minVentOpen = 0
	resultMap.ventOpening = 50
	resultMap.allowReduction = false
	return resultMap
}
def setTstatHSP(evt){
	logger(40,"debug","setTstatHSP- evt name: ${evt.name}, value: ${evt.value}")
    state.LocalTstatHSP = evt.value
    if (state.mainOn){
    	logger(30,"debug","setTstatCSP- run evaluate")
    	zoneEvaluate([msg:"temp", data:["tempChange"]])	
    }    
    
}
def setTstatCSP(evt){
	logger(40,"debug","setTstatHSP- evt name: ${evt.name}, value: ${evt.value}")
    state.LocalTstatCSP = evt.value
    if (state.mainOn){
    	logger(30,"debug","setTstatHSP- run evaluate")
    	zoneEvaluate([msg:"temp", data:["tempChange"]])	
    }    
}
def switchHandler(evt){
	logger(40,"debug","switchHandler- evt name: ${evt.name}, value: ${evt.value}")
    if (evt.value == "on") {
    	if (settings.SwitchDisable){
    		state.switchDisable = true
       	} else{
    		state.switchDisable = false
      	}
    } else if (evt.value == "off") {
    	if (settings.SwitchDisable){
    		state.switchDisable = false
       	} else{
    		state.switchDisable = true
      	}
	}
    if (state.mainOn){
    	logger(30,"debug","switchHandler- run evaluate")
    	zoneEvaluate([msg:"temp", data:["tempChange"]])	
    }    

}