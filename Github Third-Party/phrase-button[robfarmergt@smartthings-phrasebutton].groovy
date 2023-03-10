/**
 *  Phrase Button
 *
 *  Author: Rob Farmer
 */

definition(
    name: "Phrase Button",
    namespace: "smartthings",
    author: "Rob Farmer",
    description: "Execute your Phrase on when the SmartApp is tapped or activated.",
    category: "Convenience"
)

preferences {
	page(name: "firstPage")
}

def firstPage() {
    dynamicPage(name: "firstPage", title: "First, select your Phrase",uninstall: true, install: true) {
    	
        def phrases = location.helloHome?.getPhrases()*.label
        if (phrases) {
            section("Phrase") {
                input "onPhrase", "enum", title: "Phrase", required: false, options: phrases
            }
            section("UI") {
            	icon(title: "Icon", required: true)
                label(name: "label",
                      title: "Name",
                      required: true,
                      multiple: false)
            }
        }
    }
}

def installed()
{
	subscribe(app, appTouch)
}

def updated()
{
	unsubscribe()
	subscribe(app, appTouch)
}

def appTouch(evt) {
	log.debug "appTouch: $evt"
	location.helloHome.execute(onPhrase)
}