/**
 *  Switch Link
 *
 *  Link state between a switches and a locks and switches with presence
 *
 *  
 *  
 */

definition(	
    name: "Switch Link",
	namespace: "jasonrwise77",
	author: "Jason Wise",
	description: "Link state between a switches and a locks and switches with presence",
	category: "Convenience",
    	iconUrl: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png",
    	iconX2Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png",
    	iconX3Url: "https://raw.githubusercontent.com/jasonrwise77/My-SmartThings/master/icons/Push%20Events.png"
)

preferences {
	page(name: "main")
}

page name: "main"
            def main() {
                dynamicPage (name: "main", title: "", install: true, uninstall: true) { 
                section("Links",  uninstall: false){
               app(name: "links", appName: "Switch Link - Locks", namespace: "jasonrwise77", title: "Create a new lock link", multiple: true,  uninstall: false)
               app(name: "link", appName: "Switch Link - Presence", namespace: "jasonrwise77", title: "Create a new presence link", multiple: true,  uninstall: false)
                        }
                    }
}
