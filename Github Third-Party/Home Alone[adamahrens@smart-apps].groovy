/**
 *  Home Alone
 *
 *  Copyright 2015 Adam Ahrens
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
    name: "Home Alone",
    namespace: "adamahrens",
    author: "Adam Ahrens",
    description: "Turn on random lights for based on their average on time to mimic normal behavior while away from home.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("When I'm in mode") {
		input "desiredMode", "mode", title: "Mode"
	}
    section("Turn on these lights randomly") {
        input "switches", "capability.switch", multiple: true, required: true
    }
    section("How long to pause between random light selection") {
    	input "timeInMinutes", "number", title: "How many minutes", required: true
    }
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
	// Need to know whenever the mode changes
    subscribe(location, "mode", changedLocationMode)
    subscribe(switches, "switch", switchHandler)
    
    // Create an array of "switch" dictionaries
    // Update currentOn and currentOff in the switchHandler method
    // When we have a currentOn and currentOff update the time illuminated in seconds
    // Then recalculate the averageTime the switch has been on
    def allSwitches = []
    switches.each(){
    	allSwitches.add([displayName: it.displayName, currentOn : null, currentOff : null, timeIlluminated : [], averageTime : 0])
    }
    
    state.myStateLights = allSwitches
    state.currentLightIndex = -1
}

// Event handlers
def changedLocationMode(event) {
	if (location.getCurrentMode() != desiredMode) {
    	unschedule(turnOnRandomLights)
        return
    }
    
    //log.debug "Starting Random Light Schdule in 1 minute...."
    runIn(60, turnOnRandomLights)
}

def switchHandler(event) {
    // Need to track how long lights are on an off
    // To get a sense of the average amount of time
    // Only want to track when not in the desired mode
    if (location.getCurrentMode() == desiredMode) {
    	//log.debug "Not tracking the switch behavior"
        return
    }
    
    log.debug "Tracking switch behavior"
    def lights = state.myStateLights
    
    // Find the light in preferences
    // Update the currentOn or currentOff
    // If both have values then we need to update the timeIlluminated
    def light = lights.findAll(){ dictionary -> event.displayName == dictionary["displayName"] }[0]
    if (event.value == "on") {
        light["currentOn"] = now()
    } else if (event.value == "off") {
        light["currentOff"] = now()
    }
            
    // Do we have both currentOn and currentOf
    // If so find how long in seconds
    // save it back
    if (light["currentOn"] != null && light["currentOff"] != null) {
    	// now() is in milliseconds
    	def seconds = Math.round((Math.abs(light["currentOn"] - light["currentOff"]) / 1000) * 100) / 100
        def listOfTimes = light["timeIlluminated"]
        listOfTimes.add(seconds)
        light["timeIlluminated"] = listOfTimes
 
        // Reset the current on and off
        light["currentOn"] = null
        light["currentOff"] = null
        
        // Calculate the averageTime the light has been on
        def averageTime = calculateAverageTimeOn(light["timeIlluminated"])
        light["averageTime"] = averageTime
    }
    
    state.myStateLights = lights
}

// Grabs a random number of lights that the User selected
// Turns them on
def turnOnRandomLights() {
    //log.debug "Executing turnOnRandomLights...."
    
    // Get the stored state
	def currentLightIndex = state.currentLightIndex
    log.debug "Previously Stored State Random Switches = ${state.currentLights}"
    log.debug "Current light index ${currentLightIndex}"
    
    // Next time to run turnOnRandomLights, Setting default value
    def runAgainInSeconds = 60 * 3
    
    // Have we already found random switches?
    if (currentLightIndex != -1) {
    	log.debug "Already on a current round of random lights"
    	runAgainInSeconds = handleCurrentRoundOfLights()
    } else {
    	log.debug "Need a new batch"
    	runAgainInSeconds = selectFreshBatchOfLights()
	} 
    
    // Run again in 2 minutes
    def maxRunAgain = Math.max(120.0, runAgainInSeconds)
    runIn(maxRunAgain, turnOnRandomLights)
}

def handleCurrentRoundOfLights() {
	// Get the saved state
    def currentLights = state.currentLights
   	def currentLightIndex = state.currentLightIndex
    def currentRandomDisplayName = currentLights[currentLightIndex]["displayName"]
    def runAgainInSeconds = 60 * 3
    
    // Turn off the current light
    switches.each(){ lightSwitch -> 
    	if (lightSwitch.displayName == currentRandomDisplayName) {
        	lightSwitch.off()
        }
    }
    
    // Determine how long the previous light should have been on for
    def howLongInSecondsPreviousLight = currentLights[currentLightIndex]["averageTime"]
    currentLightIndex++
    if (currentLightIndex == currentLights.size()) {
       	// Reset stored state and start over
        runAgainInSeconds = resetStoredState()     
    } else {
       	// How long should this light be on for?
        def howLongOnInSeconds = currentLights[currentLightIndex]["averageTime"]
            
        // Subtract the previous lights time on from this
        // This gives the remaining time this Light should be on
        def remainingTime = howLongOnInSeconds - howLongInSecondsPreviousLight
            
        log.debug "Need to callback back in ${remainingTime} seconds"
    	state.currentLightIndex = currentLightIndex
        runAgainInSeconds = remainingTime
    }
    
    runAgainInSeconds
}

// Selects a number of new Lights to turn on & randomize
def selectFreshBatchOfLights() {
	// Need a fresh batch of random lights
    def currentRandomSwitches = collectRandomSwitches(switches)
   	log.debug "Random Switches = ${currentRandomSwitches}"
   	def randomSwitchDisplayNames = currentRandomSwitches.collect(){ it.displayName }
    def myLightsHistory = state.myStateLights
    def myLightHistoryFiltered = myLightsHistory.findAll{ lightDictionary -> randomSwitchDisplayNames.contains(lightDictionary["displayName"])}
        
    // Took the lights that I have data on
    // Sorted from smallest to largest average time
    def currentLights = myLightHistoryFiltered.sort{ first,second ->
        def averageTimeFirst = first["averageTime"]
        def averageTimeSecond = second["averageTime"]
        def compareTo = 0
        if (averageTimeFirst < averageTimeSecond) {
           	compareTo = -1
        } else if (averageTimeFirst > averageTimeSecond) {
            compareTo = 1
        }
       
    	compareTo
   	}
        
    log.debug "All my lights filtered and sorted by averageTime = ${currentLights}"
    state.currentLights = currentLights
        
    // TODO: handle averageTime when it's zero!!!!!!
    def currentLightIndex = 0
    state.currentLightIndex = currentLightIndex
  
    // Turn on all the lights!
    // They will be turned off after they've been on for an averageTime
    currentRandomSwitches*.on()
        
    // AverageTime is saved in seconds
    def runInSeconds = currentLights[currentLightIndex]["averageTime"]
    log.debug "Calling back in ${runInSeconds} seconds"
    
    runInSeconds
}

// Takes a list of switches the User specified in preferences page
// Shuffles the list and selects a random number of those switches
def collectRandomSwitches(switches) {
	// grab some random number based on switches.size
	def randomNumber = new Random().nextInt(switches.size() - 1) + 1
	
    // shuffle the order of switches
	def randomSwitches = switches.collect()
	Collections.shuffle(randomSwitches)
	
    // return the switches between 0 and randomNumber indexes
	randomSwitches[0..randomNumber]
}

// Takes a list of seconds and determines the average time
def calculateAverageTimeOn(arrayOfSeconds) {
	def totalSeconds = 0
    arrayOfSeconds.each() { totalSeconds += it }
    totalSeconds / arrayOfSeconds.size()
}

// Resets the stored state of the Smart App
// Used when we are going to select new random switches
def resetStoredState() {
	log.debug "Resetting the stored state"
    state.currentLights = null
    state.currentLightIndex = -1
    60.0 * timeInMinutes
}
