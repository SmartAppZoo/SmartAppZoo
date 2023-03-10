/**
 *  webService
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
    name: "webService",
    namespace: "MikeMaxwell/start",
    author: "Mike Maxwell",
    description: "parent app for start",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "main")
}

def main() {
	def appInstalled = app.installationState == "COMPLETE"
    dynamicPage(name: "main", title: "startPOC instances", install: true, uninstall: appInstalled, submitOnChange: true) {
        section {
        	if (appInstalled){
            	app(name: "startPOC", appName: "startPOC", namespace: "MikeMaxwell", title: "Create New App...", multiple: true)
            } else {
            	paragraph "re-open webservice after completing the initial install"
            }
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
}

def updated() {
	log.debug "Updated with settings: ${settings}"
}
def backup(backupset){
	//log.info "new backup:${backupset}"
    if (!state.backups) state.backups = []
    state.backups.add(backupset)
}
def getBackupSets(locationID,appName){
	//return list appLabel and date as list
    /*
    locationID:424eee03-ad80-4a29-9df9-048c78782a4f, 
    appName:startPOC, 
    appLabel:Poc2, 
    date:2016-01-05 13:50:47
    */
    def backupsets = []
    def sets = state.backups.findAll{ it.locationID == locationID && it.appName == appName}
    sets.each{	set ->
    	backupsets.add("${set.appLabel}~${set.date}")
    }
    return backupsets.sort()
}
def getBackup(locationID,appName,selection){
	//selection="Poc2~2016-01-05 13:50:47"
    def selected = selection.split("~")
    def set = state.backups.find{ it.locationID == locationID && it.appName == appName && it.appLabel == selected[0] && it.date == selected[1] }
	return [inputs:set.inputs,state:set.state,app:set.app]    
}