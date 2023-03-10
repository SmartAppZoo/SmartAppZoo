/**
*Uses a virtual switch to trigger hello home phrases
*Automatically turns back off after triggering phrase
*Works like a virtual push button
*
*
*/

// Automatically generated. Make future change here.
definition(
    name: "Hello Home Trigger",
    namespace: "",
    author: "Kevin Tierney",
    description: "Trigger Hello Home Phrase",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {

	page(name: "selectPhrases")
 	
    
}


def selectPhrases() {

    def configured = (settings.HHPhraseOn)
    
    dynamicPage(name: "selectPhrases", title: "Configure", uninstall: true,install:true) {		
		
        section("When this switch is pushed") {
			input "master",  "capability.momentary", title: "Which Switch?"
		}
        
        def phrases = location.helloHome?.getPhrases()*.label
		if (phrases) {
        	phrases.sort()
		
        section("Run These Hello Home Phrases When...") {
			log.trace phrases
			input "HHPhraseOn", "enum", title: "The Switch Turns On", required: true, options: phrases, refreshAfterSelection:true
			//input "HHPhraseOff", "enum", title: "The Switch Turns Off", required: false, options: phrases, refreshAfterSelection:true
			}
        
        section("Pause this many seconds before executing"){
        	input "delay","number",title:"Delay Seconds"
            }
        
        section("Assign a name") {
			label title: "Name", required: false
			}
		
        }
    }
}   


def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialize()
}


def onHandler(evt) {
    
    //log.debug "Performing \"${HHPhraseOn}\" because ${master} turned on."
    if (delay){
    //log.debug "Pausing ${delay} sec"
    
    

	unschedule(setMode)
    runIn(delay,setMode)}
    
    else { setMode(evt) }
    }


def setMode(evt){

    //log.debug "executing command"
    location.helloHome.execute(settings.HHPhraseOn)

    }


def initialize() {
	subscribe(master, "momentary.pushed", onHandler)
	//log.debug "Subscribed to ${master} to trigger ${HHPhraseOn}"
}