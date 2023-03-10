/**
 *  Low Battery Alert
 *
 *  Tim Yuhl - Clean-up, added SMS capability, changed polling, added icons
 *  
 *  Based upone this work:
 *
 *  Author: Steve Meyers
 *  Date: 2015-02-06
 *    This app will poll selected devices that use a battery and send an alert when the level reaches a specified threshold.
 */
definition(
	name: "Low Battery Alert",
	namespace: "tyuhl",
	author: "Tim Yuhl",
	description: "Alert if low battery",
	category: "Convenience",
	iconUrl: "https://raw.githubusercontent.com/tyuhl/SmartThings/master/assets/BatteryIcon.png",
	iconX2Url: "https://raw.githubusercontent.com/tyuhl/SmartThings/master/assets/BatteryIcon%402x.png"
)

preferences {
	def defaultTime = Date.parse("HHmm", "0930")
	section("About") {
		paragraph "This app will poll selected devices that use a battery and send an alert or SMS when the level reaches a specified threshold."
		paragraph "You may configure up to four groups with different thresholds."
	}
	for (int i = 0; i < 4; i++) {
		section("Monitoring group ${i+1}") {
			input "group_${i}", "capability.battery", title: "Select devices to monitor", multiple: true, required: false
			input "threshold_${i}", "number", title: "Notify if battery is below", defaultValue: 25
			input "sms_${i}", "phone", title: "Send SMS notification to (optional):", required: false
		}
	}
	input "sched_time", "time", title: "Time when check is done:"
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	initialize()
}

def initialize() {
	// Run at 9:15 am every day
	// Second Minute Hour DayOfMonth Month DayOfWeek Year
	// schedule("0 15 9 * * ?", check_batteries)
	schedule(settings.sched_time, check_batteries)
	// debug setting
	// runEvery1Minute(check_batteries)
	check_batteries()
}

def check_batteries() {
	def size, batteries, device, threshold, value, sms;

	for (int i = 0; i < 4; i++) {
		size = settings["group_${i}"]?.size() ?: 0
		sms = settings."sms_${i}".toString() ?: 0;
		if (size > 0) {
			threshold = settings."threshold_${i}".toInteger()
			log.debug "***Checking batteries for group ${i+1} (threshold ${threshold})"

			batteries = settings."group_${i}".currentValue("battery")
			for (int j = 0; j < size; j++) {
				  device = settings["group_${i}"][j]
				if (device != null) {
					value = batteries[j]
					if ((value != null) && (value < threshold)) {
						log.debug "The $device battery is at ${value}, below threshold (${threshold})"
						sendPush("The $device battery is at ${value}, below threshold (${threshold})")
						if (sms) {
							sendSms(sms, "The $device battery is at ${value}, below threshold (${threshold})")
						}
					} else {
						log.debug "The $device battery is at ${value}"
					}
				}
			}
		} else {
			log.debug "***Group ${i+1} has no devices (${size} devices)"
		}
	}
}
