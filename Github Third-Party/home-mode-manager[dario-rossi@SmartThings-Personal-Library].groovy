/*
     *  Copyright 2015 Dario Rossi
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
        name: "Home Mode Manager",
        namespace: "dario.rossi",
        author: "Dario Rossi",
        description: "Monitor a set of presence sensors and activate Hello, Home phrases based on whether your home is empty or occupied.  Each presence status change will check against the current 'sun state' to run phrases based on occupancy and whether the sun is up or down.",
        category: "My Apps",
        iconUrl: "http://icons.iconarchive.com/icons/icons8/ios7/512/Very-Basic-Home-Filled-icon.png",
        iconX2Url: "http://icons.iconarchive.com/icons/icons8/ios7/512/Very-Basic-Home-Filled-icon.png"
    )
    
    preferences {
      page(name: "selectPhrases")
        
      page( name:"Settings", title:"Settings", uninstall:true, install:true ) {
      	section("False alarm threshold (defaults to 10 min)") {
        	input "falseAlarmThreshold", "decimal", title: "Number of minutes", required: false
      	}
    
      	section("Zip code") {
       		input "zip", "text", required: true
      	}
    
          section("Notifications") {
            input "sendPushMessage", "enum", title: "Send a push notification when the house is empty?", metadata:[values:["Yes","No"]], required:false
            input "sendPushMessageHome", "enum", title: "Send a push notification when the house is occupied?", metadata:[values:["Yes","No"]], required:false
      	}
    
        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
        		label title: "Assign a name", required: false
    			input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false,
    				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    			input "modes", "mode", title: "Only when mode is", multiple: true, required: false
    		}
      }
    }
    
    def selectPhrases() {
    	def configured = (settings.awayDay && settings.awayNight && settings.homeDay && settings.homeNight)
        dynamicPage(name: "selectPhrases", title: "Configure", nextPage:"Settings", uninstall: true) {		
			section("Current Home Mode when app was opened:") {
         		 def locationCurrentMode = ""
                 locationCurrentMode = "$location.currentMode\n"
                 paragraph locationCurrentMode.trim()
         	}

			section("Who?") {
    			input "people", "capability.presenceSensor", title: "Monitor the presences", required: true, multiple: true,  refreshAfterSelection:false
    		}
            section("Who - School?") {
    			input "peopleSchool", "capability.presenceSensor", title: "Monitor the presences on weekday school hours", required: true, multiple: true,  refreshAfterSelection:false
    		}
            
    		def phrases = location.helloHome?.getPhrases()*.label
    		if (phrases) {
            	phrases.sort()
    			section("Run This Phrase When...") {
    				log.trace "Phrases are: " + phrases
    				input "awayDay", "enum", title: "Everyone leaves and it's day.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Day"
                    input "awayEvening", "enum", title: "Everyone leaves and it's evening.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Evening"
    				input "awayNight", "enum", title: "Everyone leaves and it's night.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Night"

					input "inAwayToDay", "enum", title: "Everyone is away and it becomes day.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Day"
                    input "inAwayToEvening", "enum", title: "Everyone is away and it becomes evening.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Evening"
    				input "inAwayToNight", "enum", title: "Everyone is away and it becomes night.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Goodbye - Night"
                    
                    input "homeDay", "enum", title: "At least one person arrives home and it's day.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "I'm Back - Day"
                    input "homeEvening", "enum", title: "At least one person arrives home and it's evening.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "I'm Back - Evening"
                    input "homeNight", "enum", title: "At least one person arrives home and it's night.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "I'm Back - Night"

					input "inHomeToDay", "enum", title: "At least one person is home and it becomes day.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Good Morning!"
                    input "inHomeToEvening", "enum", title: "At least one person is home and it becomes evening.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Good Evening!"
    				input "inHomeToNight", "enum", title: "At least one person is home and it becomes night.", required: true, options: phrases,  refreshAfterSelection:false, defaultValue: "Good Night!"

			}
                section("Select modes used for each condition. (Needed for better app logic)") {
            		input "homeModeDay", "mode", title: "Select mode used for the 'Home Day' phrase", required: true, defaultValue: "Home - Day"
            		input "homeModeEvening", "mode", title: "Select mode used for the 'Home Evening' phrase", required: true, defaultValue: "Home - Evening"
            		input "homeModeNight", "mode", title: "Select mode used for the 'Home Night' phrase", required: true, defaultValue: "Home - Night"
                    input "awayModeDay", "mode", title: "Select mode used for the 'Home Day' phrase", required: true, defaultValue: "Away - Day"
            		input "awayModeEvening", "mode", title: "Select mode used for the 'Home Evening' phrase", required: true, defaultValue: "Away - Evening"
            		input "awayModeNight", "mode", title: "Select mode used for the 'Home Night' phrase", required: true, defaultValue: "Away - Night"
      			}
				section("Set time in evening when to switch to night mode (Needed for better app logic)") {
            		input name: "nightModeInitiationTime", type: "time", title: "Set time to switch to appropriate night mode", required: true, defaultValue: "23:00"
      			}
                section ("Sunrise offset (optional)...") {
					input "sunriseOffsetValue", "text", title: "HH:MM", required: false, defaultValue: "00:00"
					input "sunriseOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"], defaultValue: "Before"
				}
				section ("Sunset offset (optional)...") {
					input "sunsetOffsetValue", "text", title: "HH:MM", required: false, defaultValue: "00:20"
					input "sunsetOffsetDir", "enum", title: "Before or After", required: false, options: ["Before","After"], defaultValue: "After"
				}
                section("Set start and end time to monitor presence sensors for School") {
            		input name: "schoolMonitorPresenseStartTime", type: "time", title: "Set start time to have Presense Sensors selected affect Home Mode", required: true, defaultValue: "8:00"
                    input name: "schoolMonitorPresenseEndTime", type: "time", title: "Set end time to have Presense Sensors selected affect Home Mode", required: true, defaultValue: "4:00"
                    input "schoolDays", "enum", title: "Only on certain days of the week", multiple: true, required: false,
    				options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
      			}

    		}
        }
    }
    
    def installed() {
      initialize()
    
    }
    
    def updated() {
      unschedule()
      unsubscribe()
      initialize()
    }
    
    def uninstalled() {
   	unschedule()
   	unsubscribe()
    }
    
    def initialize() {
    	subscribe(people, "presence", presence)
        subscribe(peopleSchool, "presence", presence)
        runIn(60, checkSun)
    	subscribe(location, "sunrise", setSunrise)
    	subscribe(location, "sunset", setSunset)
        schedule(nightModeInitiationTime, "changeToNightMode")
    }
    
    //check current sun state when installed.
    def checkSun() {
      def zip     = settings.zip as String
      log.debug "ZipCode is: ${zip}"
      def preOffsetSunInfo = getSunriseAndSunset(zipCode: zip)

	  def df = new java.text.SimpleDateFormat("hh:mm:ss a")
	  df.setTimeZone(location.timeZone)

	  def preOffsetSunriseTime = df.format(preOffsetSunInfo.sunrise)
      def preOffsetSunsetTime = df.format(preOffsetSunInfo.sunset)


      log.debug "CheckSun: preOffsetSunInfo.sunrise.time: " + preOffsetSunInfo.sunrise.time + " in readable form: ${preOffsetSunriseTime}"
      log.debug "CheckSun: preOffsetSunInfo.sunset.time: " + preOffsetSunInfo.sunset.time + " in readable form: ${preOffsetSunsetTime}"

      def sunInfo = getSunriseAndSunset(zipCode: zip, sunriseOffset: sunriseOffset, sunsetOffset: sunsetOffset)
      def current = now()
      
      def localTime = df.format(current)
      def localSunriseTime = df.format(sunInfo.sunrise)
      def localSunsetTime = df.format(sunInfo.sunset)
	  log.debug "CheckSun: Current time is: " + current + " in readable form: ${localTime}"
      log.debug "CheckSun: Offset sunInfo.sunrise.time: " + sunInfo.sunrise.time + " in readable form: ${localSunriseTime}"
      log.debug "CheckSun: Offset sunInfo.sunset.time: " + sunInfo.sunset.time + " in readable form: ${localSunsetTime}"
    
    if (sunInfo.sunrise.time < current && sunInfo.sunset.time > current) {
		state.sunMode = "sunrise"
		setSunrise()
      }
      
    else {
		state.sunMode = "sunset"
		setSunset()
      }
    }
    
    //change to sunrise mode on sunrise event
    def setSunrise(evt) {
      state.sunMode = "sunrise";
      changeSunMode(newMode);
    }
    
    //change to sunset mode on sunset event
    def setSunset(evt) {
      state.sunMode = "sunset";
      changeSunMode(newMode)
    }
   
   
    //change to night mode
    def changeToNightMode() {
		log.info("It is night time ${nightModeInitiationTime}, checking which night mode to set based on if anyone is home.")
		
        if(everyoneIsAway() && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)) {
			def message = "Performing \"${inAwayToNight}\" for you as requested."
			log.info(message)
			sendAway(message)
			location.helloHome.execute(settings.inAwayToNight)
      	}
        else if(everyoneIsAway() && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)) {
			def message = "Performing \"${awayNight}\" for you as requested."
			log.info(message)
			sendAway(message)
			location.helloHome.execute(settings.awayNight)
      	} 

		if(anyoneIsHome() && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)) {
			def message = "Performing \"${homeNight}\" for you as requested."
            log.info(message)
            sendHome(message)
            location.helloHome.execute(settings.homeNight)
		}
        else if(anyoneIsHome() && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)) {
			def message = "Performing \"${inHomeToNight}\" for you as requested."
            log.info(message)
            sendHome(message)
            location.helloHome.execute(settings.inHomeToNight)
		}
        
	}

    //change mode on sun event
    def changeSunMode(newMode) {
      if(allOk) {
    
      if(everyoneIsAway() && (state.sunMode == "sunrise")) {
        log.info("Home is Empty  Setting New Away Mode")
        def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60 
        runIn(delay, "setAway")
      }
    
      if(everyoneIsAway() && (state.sunMode == "sunset")) {
        log.info("Home is Empty  Setting New Away Mode")
        def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60 
        runIn(delay, "setAway")
      }
      
      else {
      log.info("Home is Occupied Setting New Home Mode")
      setHome()
    
    
      }
    }
    }
    
    //presence change run logic based on presence state of home
    def presence(evt) {
      if(allOk) {
      if(evt.value == "not present") {
        log.debug("Checking if everyone is away")
    
        if(everyoneIsAway()) {
          log.info("Nobody is home, running away sequence")
          def delay = (falseAlarmThreshold != null && falseAlarmThreshold != "") ? falseAlarmThreshold * 60 : 10 * 60 
          runIn(delay, "setAway")
        }
      }
      
    
    else {
    	def lastTime = state[evt.deviceId]
        if (lastTime == null || now() - lastTime >= 1 * 60000) {
      		log.info("Someone is home, running home sequence")
      		setHome()
        }    
    	state[evt.deviceId] = now()
    
      }
    }
    }
    
    //if empty set home to one of the away modes
    def setAway() {
      if(everyoneIsAway()) {
        
        if(state.sunMode == "sunset" && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)){
          def message = "Performing \"${inAwayToEvening}\" for you as requested."
          log.info(message)
          sendAway(message)
          location.helloHome.execute(settings.inAwayToEvening)
        }

        else if(state.sunMode == "sunrise" && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)) {
          def message = "Performing \"${inAwayToDay}\" for you as requested."
          log.info(message)
          sendAway(message)
          location.helloHome.execute(settings.inAwayToDay)
          }
        else if(state.sunMode == "sunset" && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)){
          def message = "Performing \"${awayEvening}\" for you as requested."
          log.info(message)
          sendAway(message)
          location.helloHome.execute(settings.awayEvening)
        }

        else if(state.sunMode == "sunrise" && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)) {
          def message = "Performing \"${awayDay}\" for you as requested."
          log.info(message)
          sendAway(message)
          location.helloHome.execute(settings.awayDay)
          }
        else {
          log.debug("Mode is the same, not evaluating")
        }
      }
    
      else {
        log.info("Somebody returned home before we set to '${newAwayMode}'")
      }
    }
    
    //set home mode when house is occupied
    def setHome() {
    
        log.info("Setting Home Mode!!")
        
        if(anyoneIsHome()) {
                if(state.sunMode == "sunset" && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)){
					if ( (location.mode != "${homeModeEvening}") ){
						def message = "Performing \"${inHomeToEvening}\" for you as requested."
                        log.info(message)
                        sendHome(message)
                        location.helloHome.execute(settings.inHomeToEvening)
					}
                }

				if(state.sunMode == "sunrise" && (location.currentMode == settings.homeModeDay || location.currentMode == settings.homeModeEvening || location.currentMode == settings.homeModeNight)){
					if (location.mode != "${homeModeDay}"){
						def message = "Performing \"${inHomeToDay}\" for you as requested."
                	    log.info(message)
                    	sendHome(message)
                    	location.helloHome.execute(settings.inHomeToDay)
                    }
				}     
                if(state.sunMode == "sunset" && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)){
					if ( (location.mode != "${homeModeEvening}") ){
						def message = "Performing \"${homeEvening}\" for you as requested."
                        log.info(message)
                        sendHome(message)
                        location.helloHome.execute(settings.homeEvening)
					}
                }

				if(state.sunMode == "sunrise" && (location.currentMode == settings.awayModeDay || location.currentMode == settings.awayModeEvening || location.currentMode == settings.awayModeNight)){
					if (location.mode != "${homeModeDay}"){
						def message = "Performing \"${homeDay}\" for you as requested."
                	    log.info(message)
                    	sendHome(message)
                    	location.helloHome.execute(settings.homeDay)
                    }
				}     
		}
    }
    
    private everyoneIsAway() {
		def result = true

		if(schoolDaysAndTimeOk) {
			if( (people.findAll { it?.currentPresence == "present" }) || (peopleSchool.findAll { it?.currentPresence == "present" }) ) {
				result = false
			}
        }
        else {
			if( (people.findAll { it?.currentPresence == "present" }) ) {
				result = false
			}
		}
    
      log.debug("everyoneIsAway: ${result}")
    
      return result
    }
    
    
    private anyoneIsHome() {
		def result = false

		if(schoolDaysAndTimeOk) {
            if( (people.findAll { it?.currentPresence == "present" }) || (peopleSchool.findAll { it?.currentPresence == "present" }) ) {
                result = true
            }
		}
		else {
			if( (people.findAll { it?.currentPresence == "present" }) ) {
				result = true
			}
		}
    
      log.debug("anyoneIsHome: ${result}")
    
      return result
    }
   
    
    def sendAway(msg) {
      if(sendPushMessage != "No") {
        log.debug("Sending push message")
        sendPush(msg)
      }
    
      log.debug(msg)
    }
    
    def sendHome(msg) {
      if(sendPushMessageHome != "No") {
        log.debug("Sending push message")
        sendPush(msg)
      }
    
      log.debug(msg)
    }
    
    private getAllOk() {
    	modeOk && daysOk && timeOk
    }
    
    private getModeOk() {
    	def result = !modes || modes.contains(location.mode)
    	log.trace "modeOk = $result"
    	result
    }
    
    private getDaysOk() {
    	def result = true
    	if (days) {
    		def df = new java.text.SimpleDateFormat("EEEE")
    		if (location.timeZone) {
    			df.setTimeZone(location.timeZone)
    		}
    		else {
    			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
    		}
    		def day = df.format(new Date())
    		result = days.contains(day)
    	}
    	log.trace "daysOk = $result"
    	result
    }
    
    private getTimeOk() {
    	def result = true
    	if (starting && ending) {
    		def currTime = now()
    		def start = timeToday(starting).time
    		def stop = timeToday(ending).time
    		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    	}
    	log.trace "timeOk = $result"
    	result
    }
    
        private getschoolAllOk() {
    	schooldDaysOk && schoolTimeOk
    }
    
    private getschoolDaysAndTimeOk() {
    	schoolDaysOk && schoolTimeOk
    }
    
    private getschoolDaysOk() {
    	def result = true
    	if (schoolDays) {
    		def df = new java.text.SimpleDateFormat("EEEE")
    		if (location.timeZone) {
    			df.setTimeZone(location.timeZone)
    		}
    		else {
    			df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
    		}
    		def day = df.format(new Date())
    		result = schoolDays.contains(day)
    	}
    	log.trace "schoolDaysOk = $result"
    	result
    }
    
    private getschoolTimeOk() {
    	def result = true
    	if (schoolMonitorPresenseStartTime && schoolMonitorPresenseEndTime) {
    		def currTime = now()
    		def start = timeToday(schoolMonitorPresenseStartTime).time
    		def stop = timeToday(schoolMonitorPresenseEndTime).time
    		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
    	}
    	log.trace "schoolTimeOk = $result"
    	result
    }
    
    
    private hhmm(time, fmt = "h:mm a")
    {
    	def t = timeToday(time, location.timeZone)
    	def f = new java.text.SimpleDateFormat(fmt)
    	f.setTimeZone(location.timeZone ?: timeZone(time))
    	f.format(t)
    }
    
    private getTimeIntervalLabel()
    {
    	(starting && ending) ? hhmm(starting) + "-" + hhmm(ending, "h:mm a z") : ""
    }
    
    private hideOptionsSection() {
    	(starting || ending || days || modes) ? false : true
    }
    private getSunriseOffset() {
		sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
	}

	private getSunsetOffset() {
		sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
	}