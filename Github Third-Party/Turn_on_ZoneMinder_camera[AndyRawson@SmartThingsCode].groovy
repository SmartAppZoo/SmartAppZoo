/**
 *  Turn On ZoneMinder Camera and Notify
 *
 *	Author: RobtheEngineer
 *
 *  Borrowed code from: Intruder? by Matt Blackmore
 *
 *  
 *	This is for anyone using ZoneMinder to run surveilance cameras.
 *
 *	This will allow a camera to be triggered to start recording after a contact is opened or motion is dectected in a desired mode.
 *	Optional features includes being able to turn on a light and to send a push/text message.
 *
 *
 * 	Setup instructions for the Zoneminder (My setup is using Ubuntu 13.10 & ZoneMinder v1.25):
 *
 *	1.  Add www-data to the dialout group. Run the following in the terminal:
 *
 *		sudo adduser www-data dialout
 *
 *	2. Edit zmtrigger.pl and comment out the channels not used. Run the following in the terminal:
 *
 *		sudo nano /usr/bin/zmtrigger.pl
 *
 *		Find the my @connections under Channel/Connection Modules and make it look like this:
 *
 *		my @connections;
 *		push( @connections, ZoneMinder::Trigger::Connection->new( name=>"Chan1", channe$
 *		#push( @connections, ZoneMinder::Trigger::Connection->new( name=>"Chan2", chann$
 *		#push( @connections, ZoneMinder::Trigger::Connection->new( name=>"Chan3", chann$
 *		#push( @connections, ZoneMinder::Trigger::Connection->new( name=>"Chan4", chann$
 *
 *	3.	To fix a bug, edit the mapped.pm for ZoneMinder and comment out the "munmap". Run the following in the terminal:
 *
 *		sudo nano /usr/share/perl5/ZoneMinder/Memory/Mapped.pm
 *
 *	4. In ZoneMinder, under Options/System enable OPT_TRIGGERS.
 *
 *	5. In ZoneMinder, change the function of the camera you want to trigger to Nodect. Take note of the Monitor ID in the address bar (mid=??)
 */

// Automatically generated. Make future change here.
definition(
    name: "Turn On ZoneMinder Camera and Notify",
    namespace: "",
    author: "RobtheEngineer",
    description: "Start recording of a ZoneMinder camera when an open/close sensor opens or a motion sensor activates.",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Choose one or more, when..."){
		input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
		input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
	}
    section("And the current mode is...") {
		input "currentMode", "mode", title: "Mode?"
    }
    section("Turn this on...") {
    	input "switch1", "capability.switch", title: "Switch?", required: false
    }
    section( "And Notify..." ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:true
		input "phone", "phone", title: "Send a Text Message?", required: false
	}
    section( "Computer's local IP address..." ) {   //i.e. 192.168.0.###
		input "ipaddress", "number", title: "IP Address", required:true
	}
    section( "Which camera to record..." ) {
		input "monitorid", "number", title: "Montior ID", required:true
	}
    section( "How long to record..." ) {
		input "recordtime", "number", title: "How many Seconds?", required:true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(contact, "contact.open", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
}

def eventHandler(evt) {
	log.trace "$evt.value: $evt, $settings"

	if (location.mode == currentMode) {
    	if(switch1) {
    		switch1.on();
        }
		log.debug "A contact was opened, notifying user and turning on switch"
        def message = "${evt.descriptionText}"
		send(message)

	def deviceNetworkId = ipaddress + ":6802"
    def data = monitorid + "|on+" + recordtime + "|255|" + evt.descriptionText
    
	sendHubCommand(new physicalgraph.device.HubAction("$data", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
    
    } else {
    	log.debug "A contact was opened but the mode is not currect to trigger an action"
    }
}

private send(msg) {
	if ( sendPushMessage == "Yes" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phone ) {
		log.debug( "sending text message" )
		sendSms( phone, msg )
	}

	log.debug msg
}
