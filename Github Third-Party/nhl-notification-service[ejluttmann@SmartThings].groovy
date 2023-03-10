/**
* NHL Notification Service 
*
*  Copyright 2017 Eric Luttmann
*
*  Description:
*  Handles the sport services for NHL notifications.
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

include 'asynchttp_v1'

def handle() { return "NHL Notification Service" }
def version() { return "0.9.9" }
def copyright() { return "Copyright © 2017" }
def getDeviceID() { return "VSM_${app.id}" }

definition(
    name: "${handle()}",
    namespace: "sport-notifications/nhl",
    author: "Eric Luttmann",
    description: "Handles the sport services for NHL notifications.",
    category: "My Apps",
    parent: "sport-notifications/manager:Sport Notifications",
    iconUrl: "https://cloud.githubusercontent.com/assets/2913371/22167524/3390bf68-df24-11e6-94c6-b099063842df.png",
    iconX2Url: "https://cloud.githubusercontent.com/assets/2913371/22167524/3390bf68-df24-11e6-94c6-b099063842df.png",
    iconX3Url: "https://cloud.githubusercontent.com/assets/2913371/22167524/3390bf68-df24-11e6-94c6-b099063842df.png")


preferences {
    page name: "pageMain"
    page name: "pageGoals"
    page name: "pageText"
    page name: "pageGame"
}

def pageMain() {
    intitInitalStates()

    if (!settings.overrideLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }

    dynamicPage(name: "pageMain", title: "${handle()}", install: true, uninstall: true) {
        section {
            input "nhlTeam", "enum", title: "Select NHL Team", required: true, displayDuringSetup: true, options: getTeamEnums()
        }

        section {
            input "enableGoals", "bool", title: "Enable Goal Notifications", defaultValue: "false", required: "false", submitOnChange: true

            if (enableGoals) {
                href(name: "goals",
                     title:"Goal Notifications", description:"Tap to setup goal scoring",
                     required: false,
                     page: "pageGoals")
            }
        }
        section {
            input "enableTextNotifications", "bool", title: "Enable Text Notifications", defaultValue: "false", required: "false", submitOnChange: true

            if (enableTextNotifications) {
                href(name: "notify",
                     title:"Text Notifications", description:"Tap to setup game notifications",
                     required: false,
                     page: "pageText")
            }
        }
        section {
            input "enableGame", "bool", title: "Enable Game Actions", defaultValue: "false", required: "false", submitOnChange: true

            if (enableGame) {
                href(name: "game",
                     title:"Game Actions", description:"Tap to setup game state actions",
                     required: false,
                     page: "pageGame")
            }
        }

        section("Misc Options") {
            input "useTeamLocation", "bool", title: "Use Time Zone of Selected Team?", defaultValue: "false", required: "false"
        }

        section("Service name") {
            if (settings.overrideLabel) {
                label title: "Enter custom name", defaultValue: app.label, required: false
            } else {
                paragraph app.label
            }

            input "overrideLabel", "bool", title: "Edit service name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

def pageGoals() {
    dynamicPage(name: "pageGoals", title: "Goal Notifications") {
        section( "Buttons (ie. Doorbell, Alarm)"){
            input "buttonGoals", "capability.momentary", title: "Select Buttons", required: false, multiple: true, displayDuringSetup: true, submitOnChange: true
            if (buttonGoals) {
                input "buttonDelay", "number", title: "Delay after goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
            }
        }

        section("Turn On/Off Switches") {
            input "switchDevices", "capability.switch", title: "Select Switches", required: false, multiple: true, displayDuringSetup: true, submitOnChange: true
            if (switchDevices) {
                input "switchOnFor", "number", title: "Turn Off After", description: "1-120 seconds", required: false, multiple: false, displayDuringSetup: true, range: "1..120"
                input "switchDelay", "number", title: "Delay after goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
            }
        }

        section("Flashing Lights"){
            input "flashLights", "capability.switch", title: "Select Lights", multiple: true, required: false, displayDuringSetup: true, submitOnChange: true
            if (flashLights) {
                input "numFlashes", "number", title: "Number Of Times To Flash", description: "1-50 times", required: false, range: "1..50"
                input "flashOnFor", "number", title: "On For (default 1000ms)", description: "milliseconds", required: false
                input "flashOffFor", "number", title: "Off For (default 1000ms)", description: "milliseconds", required: false
                input "flashDelay", "number", title: "Delay After Goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
            }
        }
        
        section("Lighting Level And Color Settings"){
            input "lightColor", "enum", title: "Lighting Color?", required: false, multiple:false, options: ["White", "Red","Green","Blue","Yellow","Orange","Purple","Pink"]
            input "lightLevel", "enum", title: "Lighting Level?", required: false, options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]]
        }

        section("Sirens To Trigger"){
            input "sirens", "capability.alarm", title: "Select Sirens", required: false, multiple: true, displayDuringSetup: true, submitOnChange: true
            if (sirens) {
                input "sirensOnly", "bool", title: "Don't Use The Strobe", defaultValue: "false", displayDuringSetup: true, required:false
                input "sirensOnFor", "number", title: "Turn Off After", description: "1-10 seconds", required: false, multiple: false, displayDuringSetup: true, range: "1..10"
                input "sirenDelay", "number", title: "Delay After Goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
            }
        }

        section ("Speaker Used To Play Goal Scoring Horn"){
            input "sound", "capability.musicPlayer", title: "Select Speaker", required: false, displayDuringSetup: true, submitOnChange: true
            if (sound) {
                input "volume", "number", title: "Speaker Volume", description: "1-100%", required: false, range: "1..100"
                input "soundDuration", "number", title: "Duration To Play (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
                input "soundDelay", "number", title: "Delay After Goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
	            input "soundBooOpponent", "bool", title: "Boo When The Opponent Scores?", defaultValue: "true", displayDuringSetup: true, required:false
            }
        }
    }
}

def pageText() {
    dynamicPage(name: "pageText", title: "Text Notifications") {
        section("Notification Types") {
            input "sendGoalMessage", "bool", title: "Enable Goal Score Notifications?", defaultValue: "true", displayDuringSetup: true, required:false
            input "sendGameDayMessage", "bool", title: "Enable Game Day Status Notifications?", defaultValue: "false", displayDuringSetup: true, required:false
            input "sendPregameMessage", "bool", title: "Send Custom Pregame Message?", required: false, displayDuringSetup: true, submitOnChange: true
            if (sendPregameMessage) {
                input "pregameMinutesBefore", "number", title: "Minutes Before Game", description: "1-120 minutes (default 10)", default: 10, required: false, range: "1..2400"
                input "pregameMessage", "text", title: "Pregame Message", description: "Tap to Set Message", required: false
            }
        }
        section("Notification Options") {
            input "sendPushMessage", "bool", title: "Send Push Notifications?", defaultValue: "false", displayDuringSetup: true, required:false
            input "sendPhoneMessage", "phone", title: "Send Texts to Phone?", description: "phone number", displayDuringSetup: true, required: false
            input "sendDelay", "number", title: "Delay After Goal (in seconds)", description: "1-120 seconds", required: false, range: "1..120"
        }
    }
}

def pageGame() {
    dynamicPage(name: "pageGame", title: "Game Actions") {
        section("Turn On At Start Of Game"){
            input "gameSwitches", "capability.switch", title: "Select Switches", required: false, multiple: true, displayDuringSetup: true, submitOnChange: true
            if (gameSwitches) {
                input "gameSwitchOff", "bool", title: "Turn Off After Game?", defaultValue: "true", displayDuringSetup: true, required:false
            }
        }
        section("Misc Game Actions") {
            input "gameGoalIfWin", "bool", title: "Send Goal Notification If Team Wins", defaultValue: "false", displayDuringSetup: true, required:false
        }
    }
}

// a method that will set the default label of the service.
// It uses the team selected to create the default label 
def defaultLabel() {
    return (settings.nhlTeam ? settings.nhlTeam + " NHL Notifications" : "NHL Notifications")
}

// states only iniitalized once
def intitInitalStates() {
    log.info "${handle()}: Initialize static states"

    state.NHL_URL = "http://statsapi.web.nhl.com"
    state.NHL_API = "/api/v1"
    state.NHL_API_URL = "${state.NHL_URL}${state.NHL_API}"
    state.HORN_URL = "http://wejustscored.com/audio/"
	state.BOO_URL = "http://soundbible.com/mp3/Crowd Boo 5-SoundBible.com-339165240.mp3"

    state.GAME_STATUS_SCHEDULED            = '1'
    state.GAME_STATUS_PREGAME              = '2'
    state.GAME_STATUS_IN_PROGRESS          = '3'
    state.GAME_STATUS_IN_PROGRESS_CRITICAL = '4'
    state.GAME_STATUS_UNKNOWN              = '5'
    state.GAME_STATUS_FINAL6               = '6'
    state.GAME_STATUS_FINAL7               = '7'
    
    state.Game = null
    state.gameStatus = null
    state.gameDate = null
    state.gameStations = null
    state.gameLocation = null
}

def installed() {
	// create during install only 
    state.enableGameNotifications = true //default to Enabled
    state.prevTeamList = null

    intitInitalStates()
    initialize()
}

def updated() {
    intitInitalStates()

    unschedule()
    unsubscribe()

    initialize()
}

def uninstalled() {
	removeSwitch()
}

def initialize() {
    log.info "${handle()}: version ${childVersion()}"

    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }

    log.debug "App = ${app}"
    
    state.Team = null
    state.teamList = null
    
    state.teamScore = 0
    state.opponentScore = 0

    state.gameStarted = false   
    
    state.lightsPrevious = [:]

    setGameStates(null)
    
    setupSwitch()

    getTeam()
    
    if (state.enableGameNotifications) {
        startGameDay()
    } else {
        unschedule()
    }
}

def setupSwitch() {
    def switchDevice = getChildDevice(getDeviceID())

    if (!parent.parentCreateSwitches()) {
    	if (switchDevice) {
            log.debug "Swtiches are disabled!"

            // unsubscribe and remove swtich device if it exists
            removeSwitch()
        }
        
        return
    }

    // if switch does not exist, add it
    if (!switchDevice) {
        try {
            def newSwitchDevice = addChildDevice("sport-notifications", "Sport Notification Switch", getDeviceID(), null, [name: getDeviceID(), label: app.label, completedSetup: true])

            if (newSwitchDevice) {
                log.debug "Adding switch device ${newSwitchDevice.label}!"

                // default to ON when first created
                newSwitchDevice.on()
                state.enableGameNotifications = true
                switchDevice = newSwitchDevice
            } else {
                log.error "Unable to create switch device!"
            }
        } catch (e) {
            log.error "Failed to create switch device!"
            log.error("caught exception", e)
        }
    } else {
        log.debug "switch device already exists"

        // unsubscribe if switch exists
        unsubscribe()
    }

    // update and subscribe to swtich device
    if (switchDevice) {
        if (switchDevice.label != app.label) {
            log.info "Updating switch device label from ${switchDevice.label} to ${app.label}"
            switchDevice.label = app.label
        }

        log.debug "Subscribe to switch handler ${switchDevice.label}"
        subscribe(switchDevice, "switch", notificationSwitchHandler)
    }
}

def removeSwitch() {
    def switchDevice = getChildDevice(getDeviceID())

    if (switchDevice) {
        log.debug "Remove switch device ${switchDevice.label}!"

        // remove switch device
        deleteChildDevice(switchDevice.deviceNetworkId)

        // unsubscribe if switch device is found
        unsubscribe()
    }
}

def setGameNotifications(enable) {
    log.debug "${app.label} enabled=${enable}"
    if ( state.enableGameNotifications != enable) {
        state.enableGameNotifications = enable

        if (enable) {
        	startGameDay()
        } else {
	        unschedule()
        }
        
    } else {
	    log.debug "${app.label}: enable state remained unchanged"
    }
}

def notificationSwitchHandler(evt) {
    log.debug "notificationSwitchHandler: evt=${evt.value}"
    
    try {
        if (evt.value == "on") {
            log.debug "Enabling Sport Notification"
            setGameNotifications(true)
        } else if (evt.value == "off") {
            log.debug "Disabling Sport Notification"
            setGameNotifications(false)
        }
    } catch (e) {
        log.error("caught exception", e)
    }
}

def getTeamEnums() {
    try {
	    def teams = []
        def params = [
            uri: "${state.NHL_API_URL}/teams",
        ]
        if (state.teamList == null) {
            log.debug "httpGet: ${params}"
            httpGet(params) { resp ->
                def json = resp.data
                for (rec in json.teams) {
                    teams += rec.teamName
                } 

            }
            
    		state.teamList = teams
            state.prevTeamList = teams
		    log.debug "New Team List: ${state.teamList}"
       } else {
		    log.debug "Use existing team list"
       }
    } catch (e) {
        log.error("caught exception", e)
    }

    if (state.teamList == null) {
    	if (state.prevTeamList == null) {
            log.debug "Initialize team list"
            state.teamList = []
        } else {
            log.debug "Reset to previous Team list"
    		state.teamList = state.prevTeamList
        }
    }
    
    return state.teamList.sort()
}

def getTeam() {
    log.debug "Setup for team ${settings.nhlTeam}"
    def found = false
    def params = [
        uri: "${state.NHL_API_URL}/teams",
    ]
    try {
        log.debug "httpGet: ${params}"
        httpGet(params) { resp ->
            def json = resp.data
            for (rec in json.teams) {
                if (settings.nhlTeam == rec.teamName) {
                    state.Team = rec
                    log.debug "Found info on team ${state.Team.teamName}, id=${state.Team.id}"
                    found = true
                    break
                }
            } 
        }
    } catch (e) {
        log.error("caught exception", e)
    }

    if (!found) {
        log.debug "Unable to find team, trying again in 30 seconds"

        def now = new Date()
        def runTime = new Date(now.getTime() + (30 * 1000))
        runOnce(runTime, getTeam)
    }
}

def setGameStates(game) {
  
	if (game) {
        // set current game info
        state.Game = game

		// set game status and date
        state.gameStatus = game.status.statusCode
        state.gameDate = game.gameDate

        // set game day stations and locations
        state.gameStations = getBroadcastStations(game)
        state.gameLocation = getLocation(game)
    } else {
        state.Game = null
        state.gameStatus = null
        state.gameDate = null
        state.gameStations = null
        state.gameLocation = null
    }
}


// game day URL fuctions and handlers
def startGameDay() {
    if (checkIfGameDay()) {
        def gameStartDate =  gameDateTime()

        if (gameStartDate) {
            def hoursBefore = parent.parentHourBeforeGame() ?: 0
            def now = new Date()
            def gameTime = new Date(gameStartDate.getTime())
            def gameCheck = new Date(gameStartDate.getTime() - (((hoursBefore * (60 * 60))+30) * 1000))

			// try not to have all notification services run at the exact same time
            def startTime = randomizeRunTime(gameCheck, 30)

			// if startTime is later than game time, set to run at game time minus 5 seconds
            if (gameTime < startTime) {
            	log.debug "Reset start time to game time minus 5 seconds"
                startTime = new Date(gameTime.getTime() - (5 * 1000))
            }

			// if startTime is prior to current time, set to run current time plus 5 seconds
            if (startTime <= now) {
            	log.debug "Reset start time to current time plus 5 seconds"
                startTime = new Date(now.getTime() + (5 * 1000))
            }
            
            // check for pregame message
            if (settings.sendPregameMessage) {
                def minutesBefore = settings.pregameMinutesBefore ?: 10
	            def pregameTime = new Date(gameStartDate.getTime() - ((minutesBefore * 60) * 1000))
                
                if (pregameTime <= now) {
                    log.debug "Past pregame reminder, just ignore"
                } else {
                    log.debug "Schedule pregame message for ${app.label} at ${pregameTime.format('h:mm:ss a', getTimeZone())}"
                    runOnce(pregameTime, sendPregameText)
                }
            }

            log.debug "Schedule game status checks for ${app.label} at ${startTime.format('h:mm:ss a', getTimeZone())}"
            runOnce(startTime, checkGameStatus)

        } else {
            log.error "Unable to retrieve game time from ${app.label}"
            unschedule()
        }
    } else {
        log.debug "Not a gameday for ${app.label}"
        unschedule()
    }
}

def checkIfGameDayHandler(resp) {    
    def isGameDay = false

    if (resp.status == 200) {
        def result = resp.data

        for (date in result.dates) {
            for (game in date.games)
            {
                isGameDay = true

				setGameStates(game)
                
                log.debug "A game is scheduled for today - ${game.teams.away.team.name} vs ${game.teams.home.team.name} at ${gameTimeText()}"

                // break out of loop
                break
            }
        }
    } else {
        log.error "${app.label}: resp.status = ${resp.status}"
    }

    return isGameDay
}

def checkIfGameDay() {
	def isGameDay = false
    try {
        if (state.enableGameNotifications == true) {
            def todaysDate = new Date().format('yyyy-MM-dd', getTimeZone())
            def params = [uri: "${state.NHL_API_URL}/schedule?teamId=${state.Team.id}&date=${todaysDate}&expand=schedule.teams,schedule.broadcasts.all"] 

            log.debug "Determine if it is game day for team ${settings.nhlTeam}, requesting game day schedule for ${todaysDate}"
            httpGet(params) { resp ->
                isGameDay = checkIfGameDayHandler(resp)
            }
        }
    } catch (e) {
        log.error("caught exception", e)
    }

    return isGameDay
}

def sendPregameText() {
    try {
        if (settings.sendPregameMessage) {
            sendTextNotification(settings.pregameMessage)
        }
    } catch (e) {
        log.error("caught exception", e)
    }
}

def checkGameStatusHandler(resp, data) {
   def rescheduleNextCheck = true
   def runDelay = 30
    
    // check for valid response
    if (resp.status == 200) {
        def slurper = new groovy.json.JsonSlurper()
        def result = slurper.parseText(resp.getData())
        def gamveOver = false
        def gameFound = false

        for (date in result.dates) {
            for (game in date.games)
            {
                // set current game info
                state.Game = game

                // set game status and date
                state.gameStatus = game.status.statusCode
                state.gameDate = game.gameDate
                
                gameFound = true

                log.debug "Current game status = ${state.gameStatus}"
                switch (state.gameStatus) {
                    case state.GAME_STATUS_SCHEDULED:
                    log.debug "${game.teams.away.team.name} vs ${game.teams.home.team.name}  - scheduled for today at ${gameTimeText()}!"

                    // delay for 2 minutes before checking game day status again
                    runDelay = (2 * 60) 
                    
                    //done
                    break

                    case state.GAME_STATUS_PREGAME:
                    log.debug "${game.teams.away.team.name} vs ${game.teams.home.team.name} - pregame!"

                    // start checking every 15 seconds now that it is pregame status
                    runDelay = 15

                    //done                    
                    break

                    case state.GAME_STATUS_IN_PROGRESS:
                    case state.GAME_STATUS_IN_PROGRESS_CRITICAL:
                    log.debug "${game.teams.away.team.name} vs ${game.teams.home.team.name} - game is on!!!"

                    // check every 5 seconds when game is active, looking for score changes asap
                    runDelay = 5

                    // first time just initialize the scores - this is preventing the issue of sending 
                    // goal notifications when app is started in the middle of the game after scores have 
                    // occurred.
                    if (!state.gameStarted) {
                        def team = getTeamScore(game.teams)
                        def opponent = getOpponentScore(game.teams)

                        log.debug "Game started, initialize scores and start switches..."
                        state.teamScore = team
                        state.opponentScore = opponent

                        if (settings.enableGame) {
                            // turn on any game action switches at start of game
                            if (settings.gameSwitches) {
                                setSwitches(settings.gameSwitches, true)
                            }
                        }

                        // indicate game has started
                        state.gameStarted = true
                    }

                    // check for new goal
                    checkForGoal()

                    //done
                    break

                    case state.GAME_STATUS_FINAL6:
                    case state.GAME_STATUS_FINAL7:
                    log.debug "${game.teams.away.team.name} vs ${game.teams.home.team.name} - game is over!"

                    // check for overtime score
                    def overtimeScore = checkForGoal()
    
                    // execute goal routine if team wins
                    if (settings.gameGoalIfWin) {
                        // execute goal at end of game, if there was no overtime goal already
                        if (overtimeScore == false) {
                            def teamGoals = getTeamScore(game.teams)
                            def opponentGoals = getOpponentScore(game.teams)

                            if (teamGoals > opponentGoals) {
                                def delay = settings.goalDelay ?: 0
                                runIn(delay, teamGoalScored)
                            }
                        }
                    }
                    
					if (settings.enableGame) {
                        // turn off any game action switches at end of game
                        if (settings.gameSwitches) {
                            if (settings.gameSwitchOff == true) {
                                setSwitches(settings.gameSwitches, false)
                            } else {
                                log.debug "Switches are being left on after game!"
                            }
                        }
                    } 

                    // game over, no more game day status checks required for the day
                    gamveOver = true

                    //done
                    break

                    case state.GAME_STATUS_UNKNOWN:
                    default:
                        log.debug "${game.teams.away.team.name} vs ${game.teams.home.team.name} game status is unknown!"

                    // check again in 15 seconds if game day status is unknown
                    runDelay = 15

                    //done
                    break
                }

                if (state.gameStatus != state.GAME_STATUS_UNKNOWN && state.notifiedGameStatus != state.gameStatus) {
                    if (settings.enableTextNotifications) {
                    	// use goal delay if set, ensure messages arrive after goal messages
                        def delay = settings.goalDelay ?: 0 
                        runIn(delay, triggerStatusNotifications)
                    }

                    //  set game status notified
                    state.notifiedGameStatus = state.gameStatus    	    	
                }

                // break out of loop
                break
            }
        }

        if (gamveOver) {
            log.debug "Game is over, no more game status checks required for today."
            rescheduleNextCheck = false
            state.gameStarted = false
        }
        
        if (!gameFound) {
            log.error "Game info was not found!"
        }

    } else {
        log.debug "Request Failed!"
        log.debug "Response: $resp.errorData"
    }
    
    if (rescheduleNextCheck) {
        def now = new Date()
        def nextGameCheck = new Date(now.getTime() + (runDelay * 1000))

        log.debug "Checking game status again in ${runDelay} seconds..."
        runOnce(nextGameCheck, checkGameStatus)
    }
}

def checkGameStatus() {
    try {

        if (state.enableGameNotifications == false) {
            log.debug "Game Notifications has been disabled, ignore Game Status checks."
            return
        }

        def todaysDate = new Date().format('yyyy-MM-dd', getTimeZone())
        if (settings.debugCheckDate) {
            todaysDate = settings.debugCheckDate
        }
        def params = [uri: "${state.NHL_API_URL}/schedule?teamId=${state.Team.id}&date=${todaysDate}"] 

        log.debug "Requesting ${settings.nhlTeam} game schedule for ${todaysDate}"
        asynchttp_v1.get(checkGameStatusHandler, params)
    } catch (e) {
        log.error("caught exception", e)
    }
}

def checkForGoal() {
	def goalScored = false
    def game = state.Game
    
    if (game) {
        def team = getTeamScore(game.teams)
        def opponent = getOpponentScore(game.teams)

        // check for change in scores
        def delay = settings.goalDelay ?: 0
        if (team > state.teamScore) {
            log.debug "Change in team score"
            goalScored = true
            state.teamScore = team
           	runIn(delay, teamGoalScored)
        }
        
        if (opponent > state.opponentScore) {
            log.debug "Change in opponent score"
            goalScored = true
            state.opponentScore = opponent
            runIn(delay, opponentGoalScored)
        } 
    } else {
        log.debug "No game setup yet!"
    }
    
    return goalScored
}


// misc helper functions

def getBroadcastStations(game) {
    def stations = null

    try {
        def broadcasts = game.broadcasts

        if (broadcasts) {
            for (station in broadcasts) {
                if (station.name) {
                    if (stations == null) {
                        stations = station.name
                    } else {
                        stations = stations + ", " + station.name
                    }
                }
            } 
        }
    } catch(ex) {
        log.error("caught exception", e)
        stations = null
    }

    return stations
}

def getTimeZone() {
    try {
        if (useTeamLocation) {
            if (state.Team) {
                def tz = state.Team.venue.timeZone.id
                return TimeZone.getTimeZone(tz)                        
            }
        }
    } catch(ex) {
        log.error("caught exception", e)
    }

	return location.timeZone
}

def getLocation(game) {
    def location = null

    try {
        def team = game.teams.home.team
        location = team.venue.name + ", " + team.venue.city
    } catch(ex) {
        log.error("caught exception", e)
        location = null
    }

    return location
}

def gameDateTime() {
	def gameStartTime = null
    
    if (state.gameDate) {
    	gameStartTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss'Z'", state.gameDate)
    }
    
	return gameStartTime
}

def gameTimeText() {
    def gameTime = gameDateTime()
    
    if (gameTime) {
		return gameTime.format('h:mm:ss a', getTimeZone())
    }
    
    return "?:??:??"
}

def setSwitches(switches, turnon) {
    switches.eachWithIndex {s, i ->
    	if (turnon) {
            s.on()
            log.debug "Switch=$s.id on"
        } else {
            s.off()
            log.debug "Switch=$s.id off"
        }
    }
}

def setLightPrevious(lights) {
    lights.each {
        if (it.hasCapability("Color Control")) {
            log.debug "save light color values"
            state.lightsPrevious[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
                "hue": it.currentValue("hue"),
                "saturation": it.currentValue("saturation")
            ]
        } else if (it.hasCapability("Switch Level")) {
            log.debug "save light level"
            state.lightsPrevious[it.id] = [
                "switch": it.currentValue("switch"),
                "level" : it.currentValue("level"),
            ]
        } else {
            log.debug "save light switch"
            state.lightsPrevious[it.id] = [
                "switch": it.currentValue("switch"),
            ]
        }
        
        log.debug "$it.id - old light values = $state.lightsPrevious"
    }
}

def setLightOptions(lights) {
    def color = settings.lightColor
    def level = (settings.lightLevel as Integer) ?: 100

    // default to Red
    def hueColor = 100
    def saturation = 100

    if (color) {
        switch(color) {
            case "White":
            hueColor = 52
            saturation = 19
            break;
            case "Blue":
            hueColor = 70
            break;
            case "Green":
            hueColor = 39
            break;
            case "Yellow":
            hueColor = 25
            break;
            case "Orange":
            hueColor = 10
            break;
            case "Purple":
            hueColor = 75
            break;
            case "Pink":
            hueColor = 83
            break;
            case "Red":
            hueColor = 100
            break;
        }
    }

    setLightPrevious(lights)

    lights.each {
        if (settings.lightColor && it.hasCapability("Color Control")) {
            def newColorValue = [hue: hueColor, saturation: saturation, level: level]
            log.debug "$it.id - new light color values = $newColorValue"
            it.setColor(newColorValue)
        } 

        if (settings.lightLevel && it.hasCapability("Switch Level")) {
            log.debug "$it.id - new light level = $level"
            it.setLevel(level)
        } 
    }
}

def restoreLightOptions(lights) {
    lights.each {
        if (settings.lightColor && it.hasCapability("Color Control")) {
           def oldColorValue = [hue: state.lightsPrevious[it.id].hue, saturation: state.lightsPrevious[it.id].saturation, level: state.lightsPrevious[it.id].level]
           log.debug "$it.id - restore light color = $oldColorValue"
            it.setColor(oldColorValue) 
        } else if (settings.lightLevel && it.hasCapability("Switch Level")) {
            def level = state.lightsPrevious[it.id].level ?: 100
            log.debug "$it.id - restore light level = $level"
            it.setLevel(level) 
        }

        def lightSwitch = state.lightsPrevious[it.id].switch ?: "off"
        log.debug "$it.id - turn light $lightSwitch"
        if (lightSwitch == "on") {
            it.on()
        } else {
            it.off()
        }
    }
}

def randomizeRunTime(runTime, seconds) {
    def randSecs = new Random().nextInt(seconds) + 1
	def returnTime = new Date(runTime.getTime() + (randSecs * 1000))

    return returnTime
}

def getHornURL(team) {
    def hornURL = null

    try {
        def audio = null

        switch (team.teamName) {
            case "Devils":
            audio = "njd.mp3"
            break

            case "Islanders":
            audio = "nyi.mp3"
            break

            case "Rangers":
            audio = "nyr.mp3"
            break

            case "Flyers":
            audio = "phi.mp3"
            break

            case "Penguins":
            audio = "pit.mp3"
            break

            case "Bruins":
            audio = "bos.mp3"
            break

            case "Sabres":
            audio = "buf.mp3"
            break

            case "Canadiens":
            audio = "mon.mp3"
            break

            case "Senators":
            audio = "ott.mp3"
            break

            case "Maple Leafs":
            audio = "tor.mp3"
            break

            case "Hurricanes":
            audio = "car.mp3"
            break

            case "Panthers":
            audio = "fla.mp3"
            break

            case "Lightning":
            audio = "tbl.mp3"
            break

            case "Capitals":
            audio = "wsh.mp3"
            break

            case "Blackhawks":
            audio = "chi.mp3"
            break

            case "Red Wings":
            audio = "det.mp3"
            break

            case "Predators":
            audio = "nsh.mp3"
            break

            case "Blues":
            audio = "stl.mp3"
            break

            case "Flames":
            audio = "cgy.mp3"
            break

            case "Avalanche":
            audio = "col.mp3"
            break

            case "Oilers":
            audio = "edm.mp3"
            break

            case "Canucks":
            audio = "van.mp3"
            break

            case "Ducks":
            audio = "ana.mp3"
            break

            case "Stars":
            audio = "dal.mp3"
            break

            case "Kings":
            audio = "lak.mp3"
            break

            case "Sharks":
            audio = "sjs.mp3"
            break

            case "Blue Jackets":
            audio = "cbj.mp3"
            break

            case "Wild":
            audio = "min.mp3"
            break

            case "Jets":
            audio = "wpg.mp3"
            break

            case "Coyotes":
            audio = "ari.mp3"
            break

            default:
                break
        }

        if (audio) {
            hornURL = state.HORN_URL + audio
        }

    } catch(ex) {
        log.error("caught exception", e)
        hornURL = null
    }

    return hornURL
}

def getBooURL() {
    def booURL = state.BOO_URL

    return booURL
}

def getTeamScore(teams) {
    return getScore(teams, false)
}

def getOpponentScore(teams) {
    return getScore(teams, true)
}

def getScore(teams, opponent) {
    log.debug "Getting current score"

    def score = 0

    if (state.Team.id == teams.away.team.id) {
        if (opponent) {
            score = teams.home.score
        } else {
            score = teams.away.score
        }
    } else {
        if (opponent) {
            score = teams.away.score
        } else {
            score = teams.home.score
        }
    }

    if (opponent) {
        log.debug "found opponent score ${score}"
    } else {
        log.debug "found team score ${score}"
    }

    return score
}

def getTeamName(teams) {
    return getName(teams, false)
}

def getOpponentName(teams) {
    return getName(teams, true)
}

def getName(teams, opponent) {
    def name = "unknown"

    if (state.Team.id == teams.away.team.id) {
        if (opponent) {
            return teams.home.team.name
        } else {
            return teams.away.team.name
        }
    } else {
        if (opponent) {
            return teams.away.team.name
        } else {
            return teams.home.team.name
        }
    }

    return name
}


// goal and message notifications

def teamGoalScored() {
    log.debug "GGGOOOAAALLL!!!"

	// Only send goal text notifications if game in progress, if game
    // is over there will be a final score sent already
	if (state.gameStarted) { 
        if (settings.enableTextNotifications) {
            triggerTeamGoalNotifications()
        }
	}
    
    if (settings.enableGoals) {
        triggerButtons()
        triggerSwitches()
        triggerFlashing()
        triggerSirens()
        triggerHorn()
    }
}

def opponentGoalScored() {
     log.debug "BOOOOOOO!!!"
    
    // Only send goal text notifications if game in progress, if game
    // is over there will be a final score sent already
    if (state.gameStarted) { 
        if (settings.enableTextNotifications) {
            triggerOpponentGoalNotifications()
        }
    }

    if (settings.enableGoals) {
    	if (settings.soundBooOpponent) {
	        triggerBoo()
        }
    }
}

def triggerButtons() {
    try {
        def delay = settings.buttonDelay ?: 0
        if (settings.buttonGoals) {
            runIn(delay, buttonHandler)
        }
    } catch(ex) {
        log.error "Error triggering buttons: $ex"
    }
}

def buttonHandler() {
    try {
        if (settings.buttonGoals) {
            settings.buttonGoals.eachWithIndex {b, i ->
                b.push()
                log.debug "Buttton=$b.id pushed"
            }
        }
    } catch(ex) {
        log.error "Error pushing buttons: $ex"
    }
}

def triggerSwitches() {
    try {
        def delay = settings.switchDelay ?: 0
        if (settings.switchDevices) {
            runIn(delay, switchOnHandler)
        }
    } catch(ex) {
        log.error "Error triggering switches: $ex"
    }
}

def switchOnHandler() {
    try {
        def switchOffSecs = settings.switchOnFor ?: 5

		setSwitches(settings.switchDevices, true)

        runIn(switchOffSecs, switchOffHandler)
    } catch(ex) {
        log.error "Error turning on switches: $ex"
    }
}

def switchOffHandler() {
    try {        
        log.debug "turn switches off"
        
		setSwitches(settings.switchDevices, false)
    } catch(ex) {
        log.error "Error turning off switches: $ex"
    }
}

def triggerFlashing() {
    try {
        def delay = settings.flashDelay ?: 0
        if (settings.flashLights) {
            runIn(delay, flashingHandler)
        }
   } catch(ex) {
        log.error "Error Flashing Lights: $ex"
    }
}

def flashingHandler() {
    try {
        def doFlash = true
        def numFlash = settings.numFlashes ?: 3
        def onFor = settings.flashOnFor ?: 1000
        def offFor = settings.flashOffFor ?: 1000

        setLightOptions(settings.flashLights)

        log.debug "LAST ACTIVATED IS: ${state.lastActivated}"
        if (state.lastActivated) {
            def elapsed = now() - state.lastActivated
            def sequenceTime = (numFlash + 1) * (onFor + offFor)
            doFlash = elapsed > sequenceTime
            log.debug "DO FLASH: $doFlash, ELAPSED: $elapsed, LAST ACTIVATED: ${state.lastActivated}"
        }

        if (doFlash) {
            log.debug "FLASHING $numFlash times"
            state.lastActivated = now()
            log.debug "LAST ACTIVATED SET TO: ${state.lastActivated}"
            def initialActionOn =  settings.flashLights.collect{it.currentSwitch != "on"}
            def delay = 0L
            numFlash.times {
                log.debug "Switch on after  $delay msec"
                settings.flashLights.eachWithIndex {s, i ->
                    if (initialActionOn[i]) {
                        s.on(delay:delay)
                    }
                    else {
                        s.off(delay:delay)
                    }
                }
                delay += onFor
                log.debug "Switch off after $delay msec"
                settings.flashLights.eachWithIndex {s, i ->
                    if (initialActionOn[i]) {
                        s.off(delay:delay)
                    }
                    else {
                        s.on(delay:delay)
                    }
                }
                delay += offFor
            }

            def restoreDelay = (delay/1000) + 1
            log.debug "restore flash devices after $restoreDelay seconds"
            runIn(restoreDelay, flashRestoreLightsHandler)
        }

    } catch(ex) {
        log.error "Error Flashing Lights: $ex"
    }
}

def flashRestoreLightsHandler() {
    try {
        log.debug "restoring flash devices"
        restoreLightOptions(settings.flashLights)
    } catch(ex) {
        log.error "Error restoring flashing lights: $ex"
    }
}


def triggerSirens() {
    try {
        def delay = settings.sirenDelay ?: 0
        if (settings.sirens) {
            runIn(delay, sirenOnHandler)
        }
    } catch(ex) {
        log.error "Error triggering sirens: $ex"
    }
}

def sirenOnHandler() {
    try {
        def sirensOffSecs = settings.sirensOnFor ?: 3

        settings.sirens.eachWithIndex {s, i ->
            if (settings.sirensOnly) {
                s.siren()
            } else {
                s.both()
            }
            log.debug "Siren=$s.id on"
        }

        runIn(sirensOffSecs, sirenOffHandler)
    } catch(ex) {
        log.error "Error turning on sirens: $ex"
    }
}

def sirenOffHandler() {
    try {
        log.debug "turn sirens off"
        settings.sirens.eachWithIndex {s, i ->
            s.off()
            log.debug "Siren=$s.id off"
        }
    } catch(ex) {
        log.error "Error turning off sirens: $ex"
    }
}

def triggerHorn() {
    try {
        def delay = settings.soundDelay ?: 0
        if (settings.sound) {
           	runIn(delay, playHornHandler)
        }
    } catch(ex) {
        log.error "Error running horn: $ex"
    }
}

def triggerBoo() {
    try {
        def delay = settings.soundDelay ?: 0
        if (settings.sound) {
           	runIn(delay, playBooHandler)
        }
    } catch(ex) {
        log.error "Error running boo: $ex"
    }
}

def playHornHandler() {
    def hornURI = getHornURL(state.Team)
    playSoundURI(hornURI)
}

def playBooHandler() {
    def booURI = getBooURL()
    playSoundURI(booURI)
}

def playSoundURI(uri) {
    try {
        log.debug "play URI = ${uri}"
        if (uri) {
            if (settings.soundDuration) {
                if (settings.volume) {
                    settings.sound.playTrackAndRestore(uri, settings.soundDuration, settings.volume)
                } else {
                    settings.sound.playTrackAndRestore(uri, settings.soundDuration)
                }
            } else {
                if (settings.volume) {
                    settings.sound.playTrackAtVolume(uri, settings.volume)
                } else {
                    settings.sound.playTrack(uri)
                }
            }
        } else {
            log.debug "Error, could not get URI"
        }
    } catch(ex) {
        log.error "Error playing uri: $ex"
    }
}

def triggerTeamGoalNotifications() {
    if (settings.sendGoalMessage) {
        def game = state.Game
        def msg = null

        if (game) {           
            def goals = getTeamScore(game.teams)

            if (goals == 1) {
                msg = getTeamName(game.teams) + " scored their first goal!"
            } else {
                msg = getTeamName(game.teams) + " have scored ${goals} goals!"
            }
            msg = msg + "\n${game.teams.away.team.name} ${game.teams.away.score}, ${game.teams.home.team.name} ${game.teams.home.score}"
        } else {
            msg = "${settings.nhlTeam} just Scored!"
        }

        sendTextNotification(msg)
    } else {
    	log.debug "Goal notifications are OFF"
    }
}

def triggerOpponentGoalNotifications() {
    if (settings.sendGoalMessage) {
        def game = state.Game
        def msg = null

        if (game) {           
            def goals = getOpponentScore(game.teams)

            msg = getOpponentName(game.teams) + " scored."
            msg = msg + "\n${game.teams.away.team.name} ${game.teams.away.score}, ${game.teams.home.team.name} ${game.teams.home.score}"
        } else {
            msg = "Opponent Scored."
        }

        sendTextNotification(msg)
    }
}

def triggerStatusNotifications() {
    
    if (settings.sendGameDayMessage) {
        def game = state.Game
        def msg = null
        def msg2 = null

        if (game) {
            switch (state.gameStatus) {
                case state.GAME_STATUS_SCHEDULED:
                msg = "${game.teams.away.team.name} vs ${game.teams.home.team.name}"
                
                if (state.gameStations) {
                    msg = msg + "\nToday, ${gameTimeText()} on ${state.gameStations}"
                } else {
                    msg = msg + "\nToday, ${gameTimeText()}"
                }
                if (state.gameLocation) {
                    msg = msg + "\n${state.gameLocation}"
                }
                break

                case state.GAME_STATUS_PREGAME:
                msg = "Pregame for ${game.teams.away.team.name} vs ${game.teams.home.team.name} is starting soon, game is at ${gameTimeText()}!"
                break

                case state.GAME_STATUS_IN_PROGRESS:
                msg = "${game.teams.away.team.name} vs ${game.teams.home.team.name} is now in progress!"
                break

                case state.GAME_STATUS_IN_PROGRESS_CRITICAL:
                msg = "${game.teams.away.team.name} vs ${game.teams.home.team.name} is in critial last minutes of the game, Go " + getTeamName(game.teams) + "!"
                break

                case state.GAME_STATUS_FINAL6:
                case state.GAME_STATUS_FINAL7:
                msg = "Final Score:\n${game.teams.away.team.name} ${game.teams.away.score}\n${game.teams.home.team.name} ${game.teams.home.score}"

                if (getTeamScore(game.teams) > getOpponentScore(game.teams)) {
                    msg2 =  getTeamName(game.teams) + " win!!!"
                } else if (getTeamScore(game.teams) < getOpponentScore(game.teams)) {
                    msg2 =  getTeamName(game.teams) + " lost."
                } else {
                    msg2 = "Tie game!"
                }
                break

                case state.GAME_STATUS_UNKNOWN:
                default:
                    break
            }

            if (msg) {
                if (sendTextNotification(msg)) {
                    if (msg2) {
                        sendTextNotification(msg2)
                    }
                }
            }
        } else {
            log.debug( "invalid game object")
        }
    }
    else {
    	log.debug "Game Status notifications are OFF"
    }
}

def sendTextNotification(msg) {
    try {
    	parent.parentSendNotification(app.label, sendPushMessage, sendPhoneMessage, msg)
    } catch(ex) {
        log.error "Error sending notifications: $ex"
        return false
    }

    return true
}


// child routine calls from parent

def childVersion() {
    return version()
}

def childForceInit() {
	initialize()
}

def childIsGameDay() {
    def gameDay = checkIfGameDay()

    log.debug "${app.label}: gameday=${gameDay}"
    return gameDay
}

def childGameDate() {
	return  gameDateTime()
}

def childStartGame() {
	startGameDay()
}

def childTouchTrigger() {
    def currentGameStart = state.gameStarted

	log.debug "${app.label} triggered goal!"
	state.gameStarted = true
    teamGoalScored()    
  	state.gameStarted = currentGameStart

}