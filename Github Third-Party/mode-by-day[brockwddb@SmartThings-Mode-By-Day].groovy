/**
 *  Mode By Day
 *
 *  Author: Brock Burkholder
 *  Email: brockwddb@gmail.com
 *  Website: http://www.excursion-powered.com
 */

preferences {
    section("Configuration") {
    	input "dayOfWeek", "enum", 
			title: "Which day of the week?",
			multiple: false,
			metadata: [ 
            	values: [
                    'All Week',
                    'Monday to Friday',
                    'Saturday & Sunday',
                    'Monday',
                    'Tuesday',
                    'Wednesday',
                    'Thursday',
                    'Friday',
                    'Saturday',
                    'Sunday'
                ]
			]
		input "time", "time", title: "At this time"
		input "newMode", "mode", title: "Change to this mode"
    }
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
		input "phoneNumber", "phone", title: "Send a text message?", required: false
	}
}

def installed() {
	initialize()
}

def updated() {
	unschedule()
	initialize()
}

def initialize() {
    
	log.debug "Scheduling Mode By Day for " + time
    
	schedule(time, changeModeByDay)
}

def changeModeByDay() {
    
    def doChange = false
    
    if(dayOfWeek == 'All Week'){
    	doChange = true
    }
    else if((dayOfWeek == 'Monday' || dayOfWeek == 'Monday to Friday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.MONDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Tuesday' || dayOfWeek == 'Monday to Friday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.TUESDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Wednesday' || dayOfWeek == 'Monday to Friday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.WEDNESDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Thursday' || dayOfWeek == 'Monday to Friday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.THURSDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Friday' || dayOfWeek == 'Monday to Friday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.FRIDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Saturday' || dayOfWeek == 'Saturday & Sunday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.SATURDAY){
    	doChange = true
    }
    
    else if((dayOfWeek == 'Sunday' || dayOfWeek == 'Saturday & Sunday') && Calendar.instance.DAY_OF_WEEK == Calendar.instance.SUNDAY){
    	doChange = true
    }
    
    log.debug "Calendar DOW: " + Calendar.instance.DAY_OF_WEEK
    log.debug "SET DOW: " + dayOfWeek
    
    if(doChange == true){    
        log.debug "changeModeByDay, location.mode = $location.mode, newMode = $newMode, location.modes = $location.modes"
        if (location.mode != newMode) {
            if (location.modes?.find{it.name == newMode}) {
                setLocationMode(newMode)
                send "${label} has changed the mode to '${newMode}'"
            }
            else {
                send "${label} tried to change to undefined mode '${newMode}'"
            }
        }
    }
    else {
    	log.debug "Mode change not scheduled for today."
    }
}

private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phoneNumber ) {
		log.debug( "sending text message" )
		sendSms( phoneNumber, msg )
	}

	log.debug msg
}

private getLabel() {
	app.label ?: "SmartThings"
}