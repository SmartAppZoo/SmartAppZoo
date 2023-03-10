 /*
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
    namespace: "BrianMurphy",
    author: "Mike Maxwell & Brian Murphy",
    description: "zone application for 'Keenect', do not install directly.",
    category: "My Apps",
    parent: "BrianMurphy:Keenect",
    iconUrl: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png",
    iconX2Url: "https://raw.githubusercontent.com/MikeMaxwell/smartthings/master/keen-app-icon.png",

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
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    state.vChild = "2.8.4"
    unsubscribe()
	initialize()
    
}

def initialize() {
	state.vChild = "2.8.4"
   // state?.integrator= 0 
    parent.updateVer(state.vChild)
    subscribe(tempSensors, "temperature", tempHandler)
    subscribe(vents, "level", levelHandler)
    //subscribe(zoneControlSwitch,"switch",zoneDisableHandeler)
    //subscribe(zoneindicateoffsetSwitch,"switch",allzoneoffset)
	//subscribe(zoneneedoffsetSwitch,"switch",allzoneoffset)
    state.isAC = parent.isAC() //AC enable bits
   	//fetchZoneControlState()
    zoneEvaluate(parent.notifyZone())
}

//dynamic page methods
def main(){
	//state.etf = parent.getID()
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
                /*
				only stock device types work in the list below???
                ticket submitted, as this should work, and seems to work for everyone except me...
				*/
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
                    ,options		: [["offset":"Offset from Main set point"],["fixed":"Fixed"]]
                    ,defaultValue	: "offset"
                    ,submitOnChange	: true
                )
                if (zoneControlType == "offset"){
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
                } else {
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
                  section("Optional Thermostat Zone Control, Ecobee Only"){
                 def ecobeePrograms = parent.selectProgram()
          	log.debug "programs: $ecobeePrograms"
            def ecobeeProgramshold =[["Custom":"Zone enable during hold"],["None":"Zone disabled during hold"]]

     			input "climate1", "enum", title: "Ecobee climate for Zone control", options: ecobeePrograms, required: false
     			input "climate2", "enum", title: "Additional Ecobee climates", options: ecobeePrograms, required: false
     			input "climate3", "enum", title: "Additional Ecobee climates", options: ecobeePrograms, required: false
     			input "climate4", "enum", title: "Additional Ecobee climates", options: ecobeePrograms, required: false
     			input "climate5", "enum", title: "Thermostat hold zone control", options: ecobeeProgramshold, required: false
                
            }
            section("Advanced"){
				def afDesc = "\t" + getTitle("AggressiveTempVentCurve") + "\n\t" + getTitle("ventCloseWait") + "\n\t" /*+ getTitle("zoneControlSwitchSummary")*/ + "\n\t" + getTitle("logLevelSummary") + /*"\n\t" + getTitle("isIntegrator") +*/ "\n\t" + getTitle("sendEventsToNotificationsSummary") + "\n\t" + getTitle("pressureControl")
                href( "advanced"
                    ,title			: ""
					,description	: "Settings"
					,state			: null
				)
            }
	}
}

def advanced(){
	//state?.integrator =0
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
              /*  input(
            		name			: "zoneControlSwitch"
                	,title			: getTitle("zoneControlSwitch") 
                	,multiple		: false
                	,required		: false
                	,type			: "capability.switch"
                    ,submitOnChange	: true
                )*/
                   
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
          		/*	def iintTitle = ""
                    if (isIntegrator()) iintTitle = "BETA Zone Integration On Integrator = ${state.integrator}"
                    else {iintTitle = "BETA Zone Integration Off Integrator = ${state.integrator}"
                    state?.integrator =0}
                    
          			input(
            			name			: "isIntegrator"
               			,title			: iintTitle 
               			,multiple		: false
               			,required		: true
               			,type			: "bool"
                		,submitOnChange	: true
                		,defaultValue	: true
            		)
                
                
                if (isIntegrator== false) state?.integrator= 0 
                
                
                */
                
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


def zonecontrol(){

state.currentprogram = parent.currentprogram()
def statehold = state.enabled ?:false
//state.currentprogram="Home"
if (climate1 || climate5){
    log.info "currentprogram = ${state.currentprogram}"
    if(state.currentprogram == climate1 || state.currentprogram == climate2 || state.currentprogram == climate3 || state.currentprogram == climate4|| state.currentprogram == climate5){
    state.enabled= true
    state.zoneDisabled = false
    log.info "climate enabled ${state.enabled}"
    }else {
            state.enabled = false
            state.zoneDisabled = true
    log.info "climate disabled ${state.enabled}"
       
    }
    }else{
           log.info "Zone is enabled no zone climate selected always enabled"
     state.enabled= true
    state.zoneDisabled = false
    }
       if (statehold != state.enabled){
       log.info "Zone is enabled ${state.enabled} via: [${state.currentprogram}]"

    	zoneEvaluate([msg:"zoneSwitch"])
        }else { log.info "no change in state"
        }
       
      
       }



//zone control methods
def zoneEvaluate(params){
zonecontrol()
//settings.logLevel=40
 settings.zoneControlSwitch = 40

	state.vChild = "2.4"
	logger(40,"debug","zoneEvaluate:enter-- parameters: ${params}")
    
    
	if (isIntegrator== false) state?.integrator= 0 
    // variables
    def evaluateVents = false
    
    def msg = params.msg
    def data = params.data
    //main states
      def mainES = state.mainES ?:""
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
    def zoneCSPLocal = mainCSPLocal + coolOffsetLocal
    if (mainCSPLocal && coolOffsetLocal) zoneCSPLocal = (mainCSPLocal + coolOffsetLocal)
    def zoneHSPLocal = mainHSPLocal + heatOffsetLocal
    if (settings.zoneControlType == "fixed"){
    	if (mainCSPLocal && settings.staticCSP)	zoneCSPLocal = settings.staticCSP.toInteger()
        zoneHSPLocal = settings.staticHSP.toInteger()
    }
    
    switch (msg){
    	case "stat" :
                //initial request for info during app install and zone update
                if (data.initRequest){
                	if (!zoneDisabledLocal) evaluateVents = data.mainOn
                //set point changes, ignore setbacks
                } else if (data.mainOn && (mainHSPLocal < data.mainHSP || mainCSPLocal > data.mainCSP) && !zoneDisabledLocal) {
                    evaluateVents = true
                    logger(30,"info","zoneEvaluate- set point changes, evaluate: ${true}")
                //system state changed
                } else if (data.mainStateChange){
                	//system start up
                	if (data.mainOn && !zoneDisabledLocal){
                        evaluateVents = true
                        state.fanonly2 = false
                        logger(30,"info","zoneEvaluate- system start up, evaluate: ${evaluateVents}")
                        logger(10,"info","Main HVAC is on and ${data.mainState}ing")
                    //system shut down
                    } else if (!data.mainOn && !zoneDisabledLocal){
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
                mainES = data.mainES
                mainStateLocal = data.mainState
                mainModeLocal = data.mainMode
                mainHSPLocal = data.mainHSP
                mainCSPLocal = data.mainCSP
                mainOnLocal = data.mainOn
                //set it again here, or rather ignore if type is fixed...
                if (zoneControlType == "offset"){
              zoneCSPLocal =  (mainCSPLocal + coolOffsetLocal)
                zoneHSPLocal = (mainHSPLocal + heatOffsetLocal)
                }
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
    
    //always check for main quick  AggressiveTempVentCurveActive
   
   def tempBool = false
   state.fanonly =false
   state.AggressiveTempVentCurveActive = false
    if (settings.AggressiveTempVentCurve){
    	 state.AggressiveTempVentCurveActive = true
            }
           
    	
   
    
    //write state
    state.mainES = mainES
    state.mainState = mainStateLocal
    state.mainMode = mainModeLocal
	state.mainHSP = mainHSPLocal
	state.mainCSP = mainCSPLocal
	state.mainOn = mainOnLocal
    state.zoneCSP = zoneCSPLocal
    state.zoneHSP = zoneHSPLocal
    state.zoneTemp = zoneTempLocal
	state.zoneDisabled = zoneDisabledLocal
    
  
    if (evaluateVents){
    def outred = false  
if (Rvents2== true){
if (Rventsenabled== true){
if (mainStateLocal == "heat"){
setRVents(100)
}
if (mainStateLocal == "cool"){
setRVents(0)
}
}
}
    	def slResult = ""
       	if (mainStateLocal == "heat"){
     //   def zoneTempLocalH = zoneTempLocal
   //     zoneTempLocal= (zoneTempLocal+localintegrator).round(3) //integrator control of heating 
//logger (10,"info", "ZoneTempLocal before integrator ${zoneTempLocal} Local Integrator ${localintegrator} and Local Integrator + ZoneTempLocal = ${zoneTempLocal}")

        
        state.acactive = true
logger (30,"info", "CHILD evaluateVents Heat")
 state.zoneneedofset = false
state.parentneedoffset = false
        	state.activeSetPoint = zoneHSPLocal
            if (zoneTempLocal < zoneHSPLocal-1.4){
            state.zoneneedofset = true
            logger(30,"info","CHILD zone needs offset")
            }
                 if (zoneTempLocal < zoneHSPLocal-2.0){
            state.parentneedoffset = true
            logger(30,"info","Parent needs offset")
            }
            if (zoneTempLocal > zoneHSPLocal-1.4) {
             state.zoneneedofset = false
             logger(30,"info","CHILD zone dose not need offset")
             }
                        if (zoneTempLocal > zoneHSPLocal-2.0) {
             state.parentneedoffset = false
             logger(30,"info","CHILD zone dose not need offset")
             }
       		if (zoneTempLocal >= zoneHSPLocal){
            	state.lastVO = minVoLocal
           		
                if (state.outputreduction){
                slResult = setVents(0)
                VoLocal=0
                } else{slResult = setVents(minVoLocal)
                VoLocal=minVoLocal}
             	logger(10,"info","Zone temp is ${tempStr(zoneTempLocal)}, heating setpoint of ${tempStr(zoneHSPLocal)} is met${slResult}")
				runningLocal = false
          	} else {
           			
                     if (state.AggressiveTempVentCurveActive){
                     
                     VoLocal=Math.round(((zoneHSPLocal - zoneTempLocal) + (1.0))*66)
                       logger(20,"info","MM >3 QR Vent request level ${VoLocal}")
                     if (VoLocal<=40){
          		  VoLocal=40
                   logger(20,"info","QR active <=40")}
                     }
                     else {VoLocal=Math.round(((zoneHSPLocal - zoneTempLocal)+(0.75))*55)
                     }
                 
                   if (VoLocal>100){
           					 VoLocal=100}
                         if (VoLocal< 0){
          		 			 VoLocal = 0}
                  
            		                        //   log.info "output reduction ${state.outputreduction} zone need ${state.zoneneedofset}"

                 if (state.outputreduction){
                 if (state.zoneneedofset){
                 log.info"output prior to multiplication for this zone ${VoLocal}"
                                  VoLocal=100
               logger(30,"info","this zone < than 1.5 degree from setpoint output multiplied ${VoLocal}")
                  }else {
                  log.info"output prior to reduction for this zone ${VoLocal}"
                 
                 outred = true
                 VoLocal=VoLocal*0.19
                  log.info"output after reduction for this zone ${VoLocal}"
                  }
                  }
                   	
                    
                    if (VoLocal >= maxVoLocal){
                        VoLocal = maxVoLocal}
                        
                        if (outred == false){
                        if (VoLocal <= minVoLocal){
                        VoLocal = minVoLocal
                        log.info"vent at min VoLocal ${VoLocal}"}
                        }
                        
                  if (VoLocal>100){
           					 VoLocal=100}
                         if (VoLocal< 0){
          		 			 VoLocal = 0}
            		slResult = setVents(VoLocal) 
                  
                  
                  
                  
					logger(10,"info", "Child zone temp is ${tempStr(zoneTempLocal)}, heating setpoint of ${tempStr(zoneHSPLocal)} is not met${slResult} output reduction: ${outred}, state active: ${state.acactive}")
					runningLocal = true
            }   
           	
        } else if (mainStateLocal == "cool"){
        
log.info "CHILD evaluateVents Cooling"
state.activeSetPoint = zoneCSPLocal
state.zoneneedofset = false
state.parentneedoffset = false
state.acactive = true
    def minVoCLocal = settings.minVoC.toInteger()
    def maxVoCLocal = settings.maxVoC.toInteger()
             if (zoneTempLocal > zoneCSPLocal+1.4){
            state.zoneneedofset = true
            logger(10,"info","CHILD zone needs offset")
            }
                 if (zoneTempLocal > zoneCSPLocal+1.7){
            state.parentneedoffset = true
            logger(10,"info","Parent needs offset")
            }
            if (zoneTempLocal < zoneCSPLocal+1.4) {
             state.zoneneedofset = false
             logger(10,"info","CHILD zone dose not need offset")
             }
                        if (zoneTempLocal < zoneCSPLocal+1.7) {
             state.parentneedoffset = false
             logger(10,"info","Parentdose not need offset")
             }
  
       		if (zoneTempLocal <= zoneCSPLocal){
            	state.lastVO = minVoCLocal
         	 	VoLocal = minVoCLocal     
           		slResult = setVents(VoLocal)
                 
             	logger(10,"info", "CHILD zone temp is ${tempStr(zoneTempLocal)}, cooling setpoint of ${tempStr(zoneCSPLocal)} is met${slResult}")
				runningLocal = false
          	} else {
                    if (state.AggressiveTempVentCurveActive) {
                     VoLocal=Math.round(((zoneTempLocal - zoneCSPLocal)+ 0.2 )*300)
                                                                logger(10,"info","agressive normal level ${VoLocal}")

                    if (VoLocal<=40){
          		  VoLocal=40
                     }


                    } else {
                     VoLocal=Math.round(((zoneTempLocal - zoneCSPLocal)+0.2)*150)
                                            logger(10,"info"," normal level ${VoLocal}")

                     }
                   
                 
                                                //          log.info "output reduction ${state.outputreduction} zone need ${state.zoneneedofset}"
      if (VoLocal>100){
           					 VoLocal=100}
                         if (VoLocal< 0){
          		 			 VoLocal = 0}
                             
            if (state.outputreduction){
                 if (state.zoneneedofset){
               logger(30,"info","this zone > than 1.5 degree from setpoint output not reduced ${VoLocal}")
                  }else {
                  logger(30,"info","output prior to reduction for this zone ${VoLocal}")
                 
                 outred = true
                 VoLocal=VoLocal*0.20
                  logger(30,"info","output reduction for this zone needed ${VoLocal}")
                  }
                  }

 
                    if (VoLocal >= maxVoCLocal){
                        VoLocal = maxVoCLocal}
   //logger(10,"info"," 2 max vo level ${maxVoCLocal}")

                        if (outred == false){
                        if (VoLocal <= minVoCLocal){
                           logger(30,"info","2 min vo level ${minVoCLocal}")

                        VoLocal = minVoCLocal
                        logger(30,"info","vent at min VoLocal ${VoLocal}")}
                        }
                        
                  if (VoLocal>100){
           					 VoLocal=100}
                         if (VoLocal< 0){
          		 			 VoLocal = 0}
            		slResult = setVents(VoLocal)
                                      // logger (30,"info", "humidifier state ${state.mainES}")

					logger(10,"info", "CHILD zone temp is ${tempStr(zoneTempLocal)}, cooling setpoint of ${tempStr(zoneCSPLocal)} is not met${slResult} output reduction ${outred}")
					runningLocal = true
            }   
            
        }else if (mainStateLocal == "fan only"){
        
       // state.acactive = true
       logger (30,"info", "humidifier state ${state.mainES}")
       if (state.mainES == "humidifier"){
       def FanHumLocal=settings.FanHum.toInteger()
         VoLocal=FanHumLocal
         logger(10,"info","Fan Only humidifier On open vents to ${VoLocal}, mainState: ${mainStateLocal}, zoneTemp: ${zoneTempLocal}, zoneHSP: ${zoneHSPLocal}, zoneCSP: ${zoneCSPLocal}, state active ${state.acactive}")
       

       }else{
        logger(10,"info","volocal entering fan only ${VoLocal}")
             if (state.acactive == false) {
            // log.info "${state.mainMode}"
             if (state.mainMode == "heat"){
             //log.info "heat"
             setRVents(100)
             VoLocal=Math.round(((zoneTempLocal- zoneHSPLocal)+0.2)*150)

                           logger(10,"info","Fan Only fan main mode heat open vents to ${VoLocal}")

            }
             if (state.mainMode == "cool"){
             setRVents(100)
                          VoLocal=Math.round(((zoneTempLocal- zoneCSPLocal)+(0.1))*70)


                            logger(10,"info","Fan Only fan main mode cool open vents to ${VoLocal}")

             }
                    if (VoLocal>100){
           					 VoLocal=100}
                         if (VoLocal< 0){
          		 			 VoLocal = 0
                             }
                             
            // VoLocal = fanVoLocal
            logger(10,"info","Fan Only fan on open vents to ${VoLocal}, mainState: ${mainStateLocal}, zoneTemp: ${zoneTempLocal}, zoneHSP: ${zoneHSPLocal}, zoneCSP: ${zoneCSPLocal}, state active ${state.acactive}")
            }
            
            
            
            if (state.acactive == true) {
//if (state.fanonly2 == false){
//runIn(60*7,fanonly)
//state.fanonly2 = true
//}
               // if (VoLocal <30){
               // VoLocal = 30}
               if (VoLocal < fanAHLocal){
                logger(10,"info","requested vent set ${VoLocal} local vent fanonly min ${fanAHLocal}")
               VoLocal = fanAHLocal
                
                 
                 }
            logger(10,"info","Fan on after Heat or AC, open vents to ${VoLocal}, mainState: ${mainStateLocal}, zoneTemp: ${zoneTempLocal}, zoneHSP: ${zoneHSPLocal}, zoneCSP: ${zoneCSPLocal}, state active ${state.acactive}")
            state.fanonly =true
            } 
            }
        setVents(VoLocal)
        } else {
            logger(10,"info","Nothing to do, main HVAC is not running, mainState: ${mainStateLocal}, zoneTemp: ${zoneTempLocal}, zoneHSP: ${zoneHSPLocal}, zoneCSP: ${zoneCSPLocal}")
       	}
if (state.fanonly == false){                                                               //  log.info "output reduction ${state.outputreduction} zone need ${state.zoneneedofset}"
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
        }else {parent.manageoutputreduction(false)
logger (30,"info", "CHILD Clearing System Reduced Ouput")
 }
        
    }
    //write state
         
             
 	state.running = runningLocal
state.zoneVoLocal =  VoLocal
    logger(40,"debug","zoneEvaluate:exit- ")
}


//event handlers
def obstructionHandler(evt){
    if (evt.value == "obstructed"){
        def vent = vents.find{it.id == evt.deviceId}
        logger(10,"warn", "Attempting to clear vent obstruction on: [${vent.displayName}]")
        vent.clearObstruction()
    }
 
    /*
      name: "switch",
      value: "obstructed",
      call: device.clearObstruction
    */
}
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

def zoneDisableHandeler(evt){
    logger(40,"debug","zoneDisableHandeler- evt name: ${evt.name}, value: ${evt.value}")
    def climateenabled = evt.value == "on"
   // if (zoneControlSwitch){
    	if (climateenabled){
       		logger(10,"warn", "Zone was enabled via: [${state.currentprogram}]")
            state.enabled= true
    	} else {
       		logger(10,"warn", "Zone was disabled via: [${state.currentprogram}]")
            state.enabled = false
    	}
    	zoneEvaluate([msg:"zoneSwitch"])
    //}
    logger(40,"debug","zoneDisableHandeler:exit- ")
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
    logger(40,"debug","tempHandler- evt name: ${evt.name}, value: ${evt.value}")
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

def fetchZoneControlState(){
	logger(40,"debug","fetchZoneControlState:enter- ")
    
   if (zoneControlSwitch){
    	state.zoneDisabled = zoneControlSwitch.currentValue("switch") == "off"
     	logger (30,"info","A zone control switch is selected and zoneDisabled is: ${state.zoneDisabled}")
    } else {
    	state.zoneDisabled = false
        logger (30,"info","A zone control switch is not selected and zoneDisabled is: ${state.zoneDisabled}")
    }
    logger(40,"debug","fetchZoneControlState:exit- ")
    return state.zoneDisabled
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

def fanonly(){
state.acactive = false
	logger(10,"info","set vents to fan only after 2min timeout for heat or ac afterfan run")

}


def setVents(newVo){
	logger(40,"debug","setVents:enter- ")
	logger(30,"warn","setVents- newVo: ${newVo}")
    def result = ""
    def changeRequired = false
    
	settings.vents.each{ vent ->
    	def changeMe = false
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
    
    def mqText = ""
    if (state.mainQuick && settings.AggressiveTempVentCurve && newVo == 100){
    	mqText = ", quick recovery active"
    }
    if (changeRequired) result = ", setting vents to ${newVo}%${mqText}"
    else result = ", vents at ${newVo}%${mqText}"
 	return result
    logger(40,"debug","setVents:exit- ")
}

def ventcheck(){
if (state.enabled == true){
def newVo=state.ventcheck
setVents(newVo)
}
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
if (state.enabled == true){
def newVo=state.Rventcheck
setRVents(newVo)
}
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
    def report =  "\n\trunning: ${s}\n\tqr active: ${qr}\n\tcurrent temp: ${tempStr(state.zoneTemp)}\n\tset point: ${tempStr(state.activeSetPoint)}"
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
state.endReport = "\n\tsetpoint: ${tempStr(asp)}\n\tend temp: ${tempStr(zoneTempLocal)}\n\tvariance: ${tempStr(d)}\n\tvent levels: ${vents.currentValue("level")}"        
//}else {
//log.info"fan only no chage of state.integrator ${state.integrator}"
//}
state.acactive = false
log.info "state accctive false end of report"

}

def isIntegrator(){
	return (settings.isIntegrator == null || settings.isIntegrator)
}