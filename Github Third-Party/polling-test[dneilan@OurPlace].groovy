/**
 *  Polling test
 *
 *  Copyright 2018 DCVN
 *
 */
definition(
    name: "Polling test",
    namespace: "OurPlace",
    author: "DCVN",
    description: "Polling test",
    category: "",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {														// stuff for android app
	section("Create Device to monitor list") {
		input "switches", "capability.switch", multiple: true
        }
 /*   section("Dimmers") {    
        input "dimmers", "capability.level", multiple: true
		}*/
    section("Send to") {
		input "serverDH", "capability.button"
		}
    section("Using this controller") {
		input "controller", "capability.button"
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
    //subscribe(theSwitch, "switch.on", switchEventHandler)					// specific switch "on" event
    subscribe(switches, "switch", switchEventHandler)						// any switch event
    subscribe(switches, "level", switchEventHandler)
    subscribe(controller, "button", buttonHandler)							// any button event
	}

def buttonHandler(evt) {
	switch(evt.jsonData?.buttonNumber) {
    case "1": 
        	buttonAction(switches[0],evt.value)
            break
    case '2':
            buttonAction(switches[1],evt.value)											
            break
    case '3':
            buttonAction(switches[2],evt.value)											
            break
    case '4':       	
            buttonAction(switches[3],evt.value)						   					
            break
    case '5': // Eco2
            buttonAction(switches[4],evt.value)
            break    
    case '6':
            buttonAction(switches[5],evt.value)
            break
    case '8':															// send status of all switchable devices to Serenity DH
    		pushStatus()
            break
    default:
    		log.debug"unknown button: ${evt.jsonData?.buttonNumber}"
            break
     }
    }
    
/*def levelEventHandler(evt) {
	log.debug "hello ${evt}"
    }*/
    
    
def switchEventHandler(evt) {
    String VarA, VarB
    switches.eachWithIndex {swtch, indx ->
    	VarA = evt.device
     	VarB = switches[indx]
        if (VarA==VarB) getDeviceStat(swtch, indx)								// when the event-device matches the string array position in 'switches'
		}			    														// call subroutine with switch-map and index number 
	}
    
private toggleState(Swtch) {
	if (Swtch.currentValue("switch") == "on")  Swtch.off()
    else  Swtch.on()
	}
    
private toggleDim(Swtch) {
	Swtch.refresh()														    // helps to get status before if statement
	if (Swtch.currentValue("level") >= 50) Swtch.setLevel(1) 
    else  Swtch.setLevel(100)
    //log.debug "Level ${Swtch.currentValue("level")}"
	}
    
private buttonAction(Swtch, evnt) {
	if (evnt == "held") {
     if (Swtch.hasCommand('setLevel')) toggleDim(Swtch)						// check to ensure the switch does have the setLevel command
     else {
        log.debug("Not so Smart Lighting: ${Swtch.displayName}")
        toggleState(Swtch)   
       }
     }
     else toggleState(Swtch)
   }

private pushStatus() {														// send all selected current device settings to DH
    switches.eachWithIndex {swtch, indx ->
    	getDeviceStat(swtch, indx)											// call subroutine with switch-map and index number
       }
	}
    
def getDeviceStat(swtch, varA) {								// Update DH with device info (name, label, level, on/off etc)
	int lvl
    if (swtch.hasCommand('setLevel')) {
    	swtch.refresh()
        lvl = swtch.currentValue("level")
        }
        else lvl = 100
    serverDH.parseStatus("${varA}","${swtch.currentValue("switch")}",lvl,"${swtch.device}")
    }