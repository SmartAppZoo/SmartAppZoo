/*Post Test
*/

definition(
    name: "Post Tester",
    namespace: "tierneykev",
    author: "tierneykev@gmail.com",
    description: "Test PostJson to Post Catcher",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("Monitor This Switch") {	
             input "theSwitch", "capability.switch", title: "Which switch?", multiple: false
	}
    section("Post to This URL") {	
                input "postCatcherID", "text", title: "postCatcher ID", required: true
	} 
    
    
}
def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"    
	initialize()
}


def initialize() {
    subscribe(theSwitch,"switch",poster)
 
}

def poster(evt){
	def url = "http://${postCatcherID}"
	def params = [
		uri: url
		]


	try {
		httpGet(params) { resp ->
			resp.headers.each {
				log.debug "Header ${it.name} : ${it.value}"
			}
			log.debug "response contentType: ${resp.    contentType}"
            log.debug("${resp.data}")
      
		}
	} catch (e) {
		log.debug "something went wrong: $e"
 }
}