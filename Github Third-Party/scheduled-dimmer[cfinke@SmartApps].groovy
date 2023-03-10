/**
 *  Scheduled Dimmer
 *
 *  Copyright 2018 Christopher Finke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "Scheduled Dimmer",
	namespace: "cfinke",
	author: "Chris Finke",
	description: "Sets the default dimmer level of a switch based on a schedule.",
	category: "Convenience",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "examplePage")
}

def examplePage() {
	dynamicPage(name: "examplePage", title: "", install: true, uninstall: true) {
		log.debug "Generating preferences page."

		section {
			input(
				name: "theSwitch",
				type: "capability.switchLevel",
				title: "Dimmer Switch",
				description: null,
				multiple: false,
				required: true,
				submitOnChange: true
			)
		}

		// As the user adds entries to the schedule, allow more entries.
		// SmartThings doesn't have a way (that I can find) to save these
		// entries in an array and generate them dynamically, so the hardcoded
		// schedule1/schedule2/etc. is a workaround.
		if (theSwitch) {
			log.debug "Switch is chosen; generating schedule1."

			section {
				paragraph "Set up to three scheduled dimmer levels. They must be entered in order, the earliest one first."

				input(
					name: "schedule1",
					type: "time",
					title: "Starting at...",
					required: true
				)
				input(
					name: "level1",
					type: "number",
					title: "Set brightness to... (0-100)",
					range: "0..100",
					required: true,
					submitOnChange: true
				)
			}

			log.debug "schedule1 generated."

			if (schedule1 && level1) {
				log.debug "schedule1 is chosen; generating schedule2."

				section {
					input(
						name: "schedule2",
						type: "time",
						title: "Starting at...",
						required: false
					)
					input(
						name: "level2",
						type: "number",
						title: "Set brightness to... (0-100)",
						range: "0..100",
						required: false,
						submitOnChange: true
					)
				}

				log.debug "schedule2 generated."
			}

			log.debug "Done checking for schedule2"

			if (schedule2 && level2) {
				log.debug "schedule2 is chosen; generating schedule3."

				section {
					input(
						name: "schedule3",
						type: "time",
						title: "Starting at...",
						required: false
					)
					input(
						name: "level3",
						type: "number",
						title: "Set brightness to... (0-100)",
						range: "0..100",
						required: false
					)
				}

				log.debug "schedule3 generated."
			}
			log.debug "Done checking for schedule3"
		}

	}

//	log.debug "Leaving examplePage"
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
	if (theSwitch) {
		log.debug "Subscribing to on and off events."

		subscribe(theSwitch, "switch.on", switchOnHandler)
		subscribe(theSwitch, "switch.off", switchOffHandler)

		state.lastOff = null

		if (schedule1 && level1) {
			log.debug "Scheduling schedule1."
			schedule(schedule1, setDefaultLevelScheduled)

			if (schedule2 && level2) {
				log.debug "Scheduling schedule2."

				schedule(schedule2, setDefaultLevelScheduled)

				if (schedule3 && level3) {
					log.debug "Scheduling schedule3."

					schedule(schedule3, setDefaultLevelScheduled)
				}
			}
		}
	}
}

def switchOnHandler(evt) {
	log.debug "The switch has been turned on."

	setDefaultLevel()
}

def switchOffHandler(evt) {
	log.debug "The switch has been turned off."

	// Save the time that the switch was last turned off so we know later if when it is turned on, we need to change its dimmer level.
	state.lastOff = new Date()
}

def setDefaultLevelScheduled() {
	log.debug "Scheduled event is being run."

	if (theSwitch.currentSwitch == "on") {
		log.debug "The switch is on, so we're setting its level."

		setDefaultLevel()
	}
	else {
		log.debug "The switch is off, so we're not doing anything."
	}		
}

def setDefaultLevel() {
	if (!schedule1 || !level1) {
		log.debug "No schedule is set."
		return
	}

	def endTime = schedule1
	def endOfDayLevel = level1
	def lastOffDate = null

	if (state.lastOff) {
		lastOffDate = Date.parse("yyyy-MM-dd'T'HH:mm:ssZ", state.lastOff)
	}

	def now = new Date()

	if (schedule3 && level3) {
		if (timeOfDayIsBetween(schedule3, endTime, new Date(), location.timeZone)) {
			if (lastOffDate && timeOfDayIsBetween(schedule3, now, lastOffDate, location.timeZone)) {
				log.debug "The switch was turned off after the last scheduled dimness setting, so it is either already correctly dimmed or has been manually changed for this period."
				log.debug "lastOffDate date ${lastOffDate} is between schedule3 ${schedule3} and now ${now}"

				return
			}

			log.debug "Setting level to level3: " + level3
			theSwitch.setLevel(level3)
			return
		}

		endTime = schedule3
		endOfDayLevel = level3

		if (!latestScheduleChange) {
			latestScheduleChange = schedule3
		}
	}

	if (schedule2 && level2) {
		if (timeOfDayIsBetween(schedule2, endTime, new Date(), location.timeZone)) {
			if (lastOffDate && timeOfDayIsBetween(schedule2, now, lastOffDate, location.timeZone)) {
				log.debug "The switch was turned off after the last scheduled dimness setting, so it is either already correctly dimmed or has been manually changed for this period."
				log.debug "lastOffDate date ${lastOffDate} is between schedule2 ${schedule2} and now ${now}"

				return
			}

			log.debug "Setting level to level2: " + level2
			theSwitch.setLevel(level2)
			return
		}

		endTime = schedule2
		endOfDayLevel = level2

		if (!latestScheduleChange) {
			latestScheduleChange = schedule3
		}
	}

	// We check for schedule2 here so that you could use this SmartApp to reset the brightness setting every day at the same time
	// without setting any additional scheduling. If schedule2 doesn't exist, then schedule1 only exists for a reset and we shouldn't
	// consider the light turning off since the previous day to be a manual adjustment.
	if (lastOffDate && schedule2 && timeOfDayIsBetween(schedule1, now, lastOffDate, location.timeZone)) {
		log.debug "The switch was turned off after the last scheduled dimness setting, so it is either already correctly dimmed or has been manually changed for this period."
		log.debug "lastOffDate date ${lastOffDate} is between schedule1 ${schedule3} and now ${now}"

		return
	}

	log.debug "Setting level to level1: " + level1
	theSwitch.setLevel(level1)
}