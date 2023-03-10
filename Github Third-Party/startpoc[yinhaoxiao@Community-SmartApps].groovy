/**
 *  startPOC
 *
 *  Copyright 2016 Mike Maxwell
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
    name: "startPOC",
    namespace: "MikeMaxwell",
    author: "Mike Maxwell",
    description: "SmartThings Application Recovery Tool POC",
	parent: "MikeMaxwell/start:webService",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
	page(name: "main" )
    page(name: "generalApprovalPAGE")
}

def main(){
    def appInstalled = app.installationState == "COMPLETE"
	def pageTitle = app.label
    
    //load backup selections if any
	if (!appInstalled && !state.hasBackups){
    	state.backUpSets = getBackups(location.id,app.name)
        state.hasBackups = state.backUpSets.size() > 0
	}
    
    if (restoreSet){
    	pageTitle = loadRestore(location.id,app.name,restoreSet)
     	state.optSelected = true   
    } else if (isRestore == "false" || !state.hasBackups) {
    	state.optSelected = true
    } 
    if (appInstalled) {
		state.backUpSets = null
        state.hasBackups = null
        state.optSelected = null
    }

	def showApp =  (appInstalled || state.optSelected) 
    
    //log.info "showApp:${showApp} appInstalled:${appInstalled} optSelected:${state.optSelected} hasBackups:${state.hasBackups} restoreSet:${restoreSet != null}" 
  
    def iName 		//input name
    def iMulti 		//multiple boolean
    def iType 		//input element type
    def iDefault 	//input default value
    
	dynamicPage(name: "main", title: pageTitle, uninstall: appInstalled, install: true) {
		section(){
			if (showApp) {
        		//smart app input here
                iName = "appLabel"	//required name for app label input type
                iMulti = false
                iType = "label" 	//required name for app label input type
                iDefault = null
                label (
                    title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,required		: false //restore will not work when this is a required is true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                iName = "singleDevice"
                iMulti = false
                iType = "capability.switch"
                iDefault = null
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,multiple		: iMulti
                    ,type			: iType
                    ,required		: true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                iName = "multiDevice"
                iMulti = true
                iType = "capability.switch" 
                iDefault = null
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,multiple		: iMulti
                    ,type			: iType
                    ,required		: true
                    ,submitOnChange	: true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                iName = "iNumber"
                iMulti = false
                iType = "number"
                iDefault = null
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,multiple		: iMulti
                    ,type			: iType
                    ,required		: true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                iName = "simpleEnum"
                iMulti = true
                iType = "enum" 
                iDefault = null
                input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,multiple		: iMulti
                    ,type			: iType
                    ,options		: ["one","two","three"]
                    ,required		: true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                iName = "mapEnum"
                iMulti = true
                iType = "enum" 
                iDefault = ["3","5"]
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,type			: iType
                    ,multiple		: iMulti
                    ,options		: [["3":"three"],["4":"four"],["5":"five"]]
                    ,required		: true
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                //demo input using selected devices from the multiDevice input above
                //this input isn't part of the backup set
                iName = "devicePreselected"
                iMulti = true
                iType = "capability.switch" 
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,type			: iType
                    ,multiple		: iMulti
                    ,required		: false
                    ,defaultValue	: getSelectedDevices(multiDevice)
                )                
                iName = "modes"
                iMulti = true
                iType = "mode" 
                iDefault = []                
				input (
                	name			: iName
                    ,title			: "var:${iName}, type:${iType}, multi:${iMulti}"
                    ,type			: iType
                    ,multiple		: iMulti
                    ,required		: false                
                    ,defaultValue	: restore(iName,iType,iMulti,iDefault)
                )
                if (appInstalled){
					href( "generalApprovalPAGE"
						,title			: "Backup app now"
						,description	: ""
						,state			: null
						,params			: [method:"appBackup",title:"Application Backup"]
						//,submitOnChange	: true
					)                	
                }
        	} else {
            	input(
            		name			: "isRestore"
                    ,title			: "Select Action?"
                	,type			: "enum"
                    ,required		: false
                    ,options		: [["false":"Install Applcation"], ["true":"Restore Application"]]
                	,submitOnChange	: true
            	)
                if (isRestore == "true") {
                	//select restore set
                    input(
                    	name			: "restoreSet"
                        ,title			: "Select a backup set to restore..."
                        ,type			: "enum"
                        ,required		: false
                        ,options		: state.backUpSets
                        ,submitOnChange	: true
                    )
                }
        	}
        }
    }
}


def installed() {
	if (state.testOne == null){
    	log.info "init:state.testOne was set by the app"
    	state.testOne = false
    } else {
    	log.info "init:state.testOne was restored"
    }
	if (state.testTwo == null){
    	log.info "init:state.testTwo was set by the app"
    	state.testTwo = true
    } else {
    	log.info "init:state.testTwo was restored"
    }
	if (state.map == null){
    	log.info "init:state.map was set by the app"
    	state.map = [yada:[putz:"one",whateves:"three"]]
    } else {
    	log.info "init:state.state.map was restored"
    }
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {

}

def appBackup(){
    def iMap = []
    def sMap = []
    def aMap = []
    //input settings
    settings.each{ setting ->
        def iType = state.inputMaps."${setting.key}"
        def values = []
        if (iType) {
        	log.info "backing up input:${setting.key} type:${iType.type}"
        	if (iType.type.startsWith("capability")){
                	def devices = settings."${setting.key}"
                	devices.each{ device ->
                    	values.add(device.id)
                	}
        	} else if (iType.type == "enum" || iType.type == "mode"){
        		if (iType.multi){
                	def enums = settings."${setting.key}"
                	enums.each{ it ->
                		values.add(it)
                	}
            	} else {
					values.add(settings."${setting.key}")  
            	}
        	} else if (iType.type == "text") {
        		values.add(settings."${setting.key}")
        	} else {
        		values.add(settings."${setting.key}".value)
        	}
            iMap.add([(setting.key):[value:values]])
        } else {
        	log.info "backup of ${setting.key} was ignored"
        }
	}
    //static backup of app label
    iMap.add([appLabel:[value:[app.label]]])
    
    state.inputMaps = null
    state.inputData = null
    
	//app label
    //iMap.add([appLabel:[value:[app.label]]])
    
    //state objects
    sMap.add(state)

	def now = new Date().format("yyyy-MM-dd HH:mm:ss")
    def backupSet = [locationID:location.id,appName:app.name,appLabel:app.label,date:now,inputs:iMap,state:sMap,app:aMap]
    parent.backup(backupSet)

	return "App backup was successfull!"
}

def getBackups(myLocation,thisApp){
	//myLocation (is location ID) : 424eee03-ad80-4a29-9df9-048c78782a4f
    //thisApp (is the app.name, not the app.label) : startPOC 
    //getBackupSets returns [Poc4~2016-01-07 13:13:38, Poc6~2016-01-07 13:15:19]
    return parent.getBackupSets(myLocation,thisApp)
    
}
def loadRestore(myLocation,thisApp,restoreSet){
	//restoreSet : [Poc4~2016-01-07 13:13:38]
    //getBackup returns
    /*
    [
    inputs:[
    	[iNumber:[value:[65]]], 
        [singleDevice:[value:[79b54c3e-14e7-4ee1-97ad-9aedd2216005]]], 
        [multiDevice:[value:[403defbb-d920-4dbb-a78e-d182c2b1619f, c00d0a0c-3bd0-4b0b-9f49-d5c4b03061a7]]], 
        [simpleEnum:[value:[one]]], 
        [mapEnum:[value:[4]]], 
        [appLabel:[value:[Poc6]]]
     	], 
    state:[
    	[inputMaps:null, testOne:false, testTwo:true, hasBackups:null, map:[yada:[whateves:three, putz:one]], optSelected:null, backUpSets:null, inputData:null]
        ], 
    app:[]
    ]
    */
	def set = parent.getBackup(myLocation,thisApp,restoreSet)
    
    //input data cache
    state.inputData = set.inputs
    
    //restore app data
    //log.debug "restore app data${set.app}"
    
    //restore state data
    set.state[0].each{ it ->
        if (it.value != null) {
        	log.debug "restore state:${it}"
            state."${it.key}" = it.value
        }
    }
    
    return "Application restored"
}

def restore(input,type,multiple,defaultValue){
    def element = defaultValue
    
    if (settings.restoreSet) {
      	def v = state.inputData["${input}"].value[0]
        log.info "restore input:${input}, type:${type}, multi:${multiple}, value:${v}"
        element = v
    } else {
    	if (type != "label"){
        	//build a list of items to backup
        	if (!state.inputMaps){
            	//log.info "save input details:${input} type:${type} multi:${multiple}"
            	state.inputMaps = [(input):["type":type,"multi":multiple]]
        	} else if (!state.inputMaps."${input}") {
        		//log.info "save input details:${input} type:${type} multi:${multiple}"
				state.inputMaps << [(input):["type":type,"multi":multiple]]        	
                state.inputMaps << [(input):["type":type,"multi":multiple]]        	
        	} else {
        		//log.info "read:${input}"
        	}
        }
    }
    return element
}
def getSelectedDevices(deviceList){
	def deviceIDS = []
    deviceList.each{ device ->
    	deviceIDS.add(device.id)
    }
    return deviceIDS
}

def generalApprovalPAGE(params){
	def title = params.title
	def method = params.method
	def result
	dynamicPage(name: "generalApprovalPAGE", title: title ){
		section() {
			if (method) {
				result = app."${method}"()
				paragraph "${result}"
			}
		}
	}
}
