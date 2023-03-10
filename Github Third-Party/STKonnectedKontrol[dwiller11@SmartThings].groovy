/**
 *  Konnected Alarm Controller
 *  Author: Dan Wheeler
 * 	Date Created: 11/30/2020
 *	UPDATE 12/6/2020: Ran into some bugs with double triggering that I couldn't figure out.
 *		I'm no longer using this and am instead using 3 virtual switches and 3 automations with conditions on
 *		the state of the alarm status to handle arming/disarming and preventing accidential re-arming when the panel is already disarmed
 *		I'll leave this here in case anyone wants to use it but I think it may have overcomplicated the problem
 
 *  Description: 
 *	To be used with the Konnected alarm interface kits. 
 *  The Konnected interface kits don't have a way to explicitly disarm the panel using the keyswitch zone.
 * 	The way you disarm a Vista panel using a keyswitch zone is to send a arming command to the keyswitch. 
 *	This can cause problems with your automations and scheduling. 
 *	For example, if you typically disarm your alarm at 8AM but someone wakes up early and disarms the system
 *	before 8AM, when the "disarm" command (which is really an arming command) runs at 8AM, it will actually arm the system
 *	This SmartApp checks to make sure the system is armed before disarming and disarmed before arming
 
 * Pre-Reqs:
 *	1. Obviously you need your Konnected interface kit set up and configured to arm/disarm and report arming status
 *	via a keyswitch zone and arming status via Vista programmable outputs. Don't use this until you have all that set up and working
 *	2. You need to create 3 virtual switches. (change the code if you want to use simulated switches)
 *	One virtual switch for arming in STAY mode, one for arming in AWAY mode and one for disarming. For example:
 *		- VSArmAway
 *		- VSArmStay
 *		- VSDisarm

 * Configuration and Usage:
 * 1. Install this code/SmartApp via the Samsung graph IDE
 * 2. Add the SmartApp via the SmartThings app
 * 3. Select your Konnected arm stay switch, arm away switch and arming status contact sensor
 * 4. Select your arm stay, arm away and disarm switches as directed
 * 5. Use your virtual switches in automations and scenes to control the alarm system
 
 */
 
definition(
    name: "Konnected Alarm Controller",
    namespace: "smartthings",
    author: "Dan Wheeler",
    description: "Arms and disarms alarm systems which use the Konnected interface boards",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
)

preferences {
    section("Select the Konnected switch that ARMs the system in STAY mode:") {
        input "staymodeKonnectedswitch", "device.placeholder", required: true, title: "STAY mode KONNECTED switch"
    }

    section("Select the Konnected switch that ARMs the system in AWAY mode:") {
        input "awaymodeKonnectedswitch", "device.placeholder", required: true, title: "AWAY mode KONNECTED switch"
    }

    section("Select the Konnected contact sensor that indicates the current alarm arming status (armed/disarmed):") {
        input "armingstatuscontactsensor", "device.placeholder", required: true, title: "Arming status contact sensor"
    }

    section("Select the virtual switch you created to ARM the system in STAY mode:") {
        input "staymodeVirtualswitch", "device.virtualSwitch", required: true, title: "STAY mode VIRTUAL switch"
    }

    section("Select the virtual switch you created to ARM the system in AWAY mode:") {
        input "awaymodeVirtualswitch", "device.virtualSwitch", required: true, title: "AWAY mode VIRTUAL switch"
    }

    section("Select the virtual switch you created to DISARM the system:") {
        input "disarmVirtualswitch", "device.virtualSwitch", required: true, title: "DISARM VIRTUAL switch"
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
	subscribe(staymodeVirtualswitch, "switch.on", setStay)
    subscribe(awaymodeVirtualswitch, "switch.on", setAway)
    subscribe(disarmVirtualswitch, "switch.on", setDisarm)
}

/** Arm the system in STAY mode if it is disarmed. If it's already armed, do nothing */
def setStay(evt) {
    log.debug "setStay (set STAY mode) was called by: $evt"
    def armingStatus = armingstatuscontactsensor.currentState("contact")
    if (armingStatus.value == "closed")
    	{
        	log.debug "The alarm is disarmed. Arming in STAY mode."
            staymodeKonnectedswitch.on()
        }
    else
    	{
        	log.debug "The alarm is already armed. Doing nothing."
		}
	staymodeVirtualswitch.off()
}

/** Arm the system in AWAY mode if it is disarmed. If it's already armed, do nothing */
def setAway(evt) {
    log.debug "setAway (set AWAY mode) was called by: $evt"
    def armingStatus = armingstatuscontactsensor.currentState("contact")
    if (armingStatus.value == "closed")
    	{
        	log.debug "The alarm is disarmed. Arming in AWAY mode."
            awaymodeKonnectedswitch.on()
        }
    else
    	{
        	log.debug "The alarm is already armed. Doing nothing."
		}
    awaymodeVirtualswitch.off()
}

/** Disarm the system if it is armed. If it's already disarmed, do nothing */
def setDisarm(evt) {
    log.debug "setDisarm (disarm the alarm) was called by: $evt"
    def armingStatus = armingstatuscontactsensor.currentState("contact")
    if (armingStatus.value == "open")
    	{
        	log.debug "The alarm is armed. Disarming."
            staymodeKonnectedswitch.on()
        }
    else
    	{
        	log.debug "The alarm is already disarmed. Doing nothing."
		}
    disarmVirtualswitch.off()
}
