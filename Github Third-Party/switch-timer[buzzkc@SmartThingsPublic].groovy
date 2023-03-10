/**
 *  Switch Timer
 *
 *  Copyright 2015 Yevgeniy 'James' Grodskiy
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
    name: "Switch Timer",
    namespace: "buzzkc",
    author: "BuzzKc",
    description: "This application allows users to specify timers on all their switches in the house.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/buzzkc/SmartThingsPublic/master/smartapps/buzzkc/icon64.png",
    iconX2Url: "https://raw.githubusercontent.com/buzzkc/SmartThingsPublic/master/smartapps/buzzkc/icon128.png",
    iconX3Url: "https://raw.githubusercontent.com/buzzkc/SmartThingsPublic/master/smartapps/buzzkc/icon256.png")



preferences {
	page(name: "timerSwitch", title: "Timer Switch", nextPage: "switchDefinition") {
        section("Switch") {
        	input "swtches", "capability.switch", title: "Switch", required: true, multiple: true
    	}
    }
    page(name: "switchDefinition", title: "Switch Preferences",nextPage: "finalPage")
    page(name: "finalPage", title: "Complete", install: true, uninstall: true){
    	section {
            paragraph "You are ready to install this application."
        }
    }
}

def switchDefinition() {
    dynamicPage(name: "switchDefinition") {
    	for(swtch in swtches)
        {
        	section(swtch.displayName) {
                input "startTime-${swtch.id}", "number", title: "Start Hour (1-24)", description: "This will be the start hour range.",range: "1..24"
                input "endTime-${swtch.id}", "number", title: "End Hour (1-24)", description: "This will be the end hour range.",range: "1..24"
                input "minutes-${swtch.id}", "number", title: "Minutes", description: "How many minutes to keep the light on.",range: "1..1440"
                input(name: "notification-${swtch.id}", type: "enum", title: "Notification", options: ["None","PUSH"])
            }
        }
    }
}

def installed() {

	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}



def initialize() {
 	atomicState.switchInfo = [  : ];
 
	subscribe(swtches, "switch.on", handleSwitchOn)
    subscribe(swtches, "switch.off", handleSwitchOff)
}

def handleSwitchOn(evt){
    def workingMap = atomicState.switchInfo;
    
   	if(timeInRange(evt.deviceId))
    {
		workingMap["${evt.deviceId}"] = new Date().time +  ((settings["minutes-${evt.deviceId}"] as long) * 60 * 1000)
        
        def list = workingMap.values().sort()
        def smallestTime = list[0];
        def smallestId = "";
   
       	def result = getNextSwitchToSchedule(workingMap);
        
        if(result != null && result.id == evt.deviceId)
        {
        	def minutesToTurnOfFrom = millisecondsToMinutesFromNow(smallestTime)            
            
            handleNotification(true,false,true,"TIMER App: Switch(${evt.deviceId}) is the soonest switch to turn off.  Scheduling to turn of in ${minutesToTurnOfFrom} minutes.")
        	
            unschedule("turnOffSwitch");
            runIn(60*millisecondsToMinutesFromNow(smallestTime), turnOffSwitch)
        }
	}
    
    atomicState.switchInfo = workingMap;
}

def handleSwitchOff(evt){
    def workingMap = atomicState.switchInfo;
    def result = getNextSwitchToSchedule(workingMap);
    
    
    if(result != null && evt.deviceId == result.id)
    {
        handleNotification(true,false,true,"TIMER App: Switch (${evt.deviceId}) was the next switch to be turned off.")
        unschedule("turnOffSwitch");
        workingMap.remove(evt.deviceId)
        
        atomicState.switchInfo = workingMap;
        turnOffSwitch(null)
    
    }
    else if(result != null)
	{
        handleNotification(true,false,true,"TIMER App: Removing switch (${evt.deviceId}) from queue.")
    	workingMap.remove(evt.deviceId)
    }

    atomicState.switchInfo = workingMap;
}

def turnOffSwitch(evt){
    def workingMap = atomicState.switchInfo;
    
    def safetyCounter = 100;
    
    while(safetyCounter > 0)
    {
    	def result = getNextSwitchToSchedule(workingMap);
        
        if(result == null)
        {
        	break;
        }
        
        if(result.minutes > 0)
        {
            handleNotification(true,false,true,"TIMER App: Switch (${result.id}) is the next switch to be turned off, in ${result.minutes} minutes.")
        	runIn(60*result.minutes, turnOffSwitch)
            break;
        }
        else
        {
        	def swtch = getSwitchBySwitchId(result.id);
            handleNotification(true,false,true,"TIMER APP: Switch (${result.id}) has ${result.minutes} minutes left, which is less than 1 and needs to be turned off.")
            swtch.off()
            
            handleNotification(false,settings["notification-${swtch.id}"] == "PUSH",true,"TIMER APP: ${swtch.displayName} is being turned off.")
                        
        	workingMap.remove(result.id)
        }
    	safetyCounter = safetyCounter-1;
    }
    
	atomicState.switchInfo = workingMap;
}

def handleNotification(debug,push,eventLog,msg)
{
	def pushHappened = false
    
	if(push)
    {
   		sendPush(msg)
        pushHappened = true
    }
    
    if(eventLog && !pushHappened)
    {
    	sendNotificationEvent(msg)
    }
    
    if(debug)
    {
    	log.debug msg
    }
}

def timeInRange(id){
	def date = new Date()
	def sdf = new java.text.SimpleDateFormat("k") 
    def hour = sdf.format(date) as int    
 
    hour >= (settings["startTime-${id}"] as int) && hour <= (settings["endTime-${id}"] as int) 
}

def millisecondsToMinutesFromNow(mili)
{
	Math.round((mili-new Date().time ) / (60000))
}

def getSwitchBySwitchId(id)
{
	def result = null
    
	for(swtch in swtches)
    {
    	if(swtch.id == id)
        {
        	result = swtch;
            break;
		}
    }
    
    result
}

def getNextSwitchToSchedule(mapOfTimes)
{
	if(mapOfTimes.size() > 0)
    {
    	def listOftimes = mapOfTimes.values().sort();
        def smallestTime = listOftimes[0];
        def smallestId = "";
   
        for(key in mapOfTimes.keySet())
        {
        	if(mapOfTimes[key] == smallestTime)
            {
            	smallestId = key;
                break;
            }
        }
        
        [
        	id: smallestId, 
            minutes : millisecondsToMinutesFromNow(smallestTime)
        ]
    }
    else
    {
    	null;
    }
}