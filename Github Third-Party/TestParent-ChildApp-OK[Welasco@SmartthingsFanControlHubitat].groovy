definition(
    name: "TestPagesApp-Child",
    namespace: "Victor",
    author: "Victor",
    description: "Test Pages and Child App.",
    category: "My Apps",
    singleInstance: true,
    //parent: parent ? "Victor.TestPagesApp" : null,
    parent: "Victor:TestPagesApp-Parent",
    iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
    iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
    iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",    
)

preferences {
	page(name: "startpage")
	page(name: "parentPage")
	page(name: "childStartPage")
}

def startpage() {
    if (parent) {
        childStartPage()
    } else {
        parentPage()
    }
}

def parentPage() {
	dynamicPage(name: "parentPage", title: "", nextPage: "", install: false, uninstall: true) {
      section("Create a new fan automation. 5") {
            //app(name: "childApps", appName: appName(), namespace: "Victor", title: "New ChildApp", multiple: true)
            //app(name: "pistons", appName: "TestPagesApp", namespace: "Victor", title: "New ChildApp", multiple: true, uninstall: false)
            app(name: "pistons", appName: appName(), namespace: "Victor", title: "New ChildApp", multiple: true, uninstall: false)
            

        }
    }
}

def childStartPage() {
	dynamicPage(name: "childStartPage", title: "Select your devices and settings", install: true, uninstall: false) {
        section("Name") {
        	label(title: "Assign a name", required: false)
        }
        // Here is the problem if I have at least one input the app get stuck to add a childapp in the mobile app
        // If I comment the lines bellow it work
        section("4 speed fan control:"){
            input("speedfan4", "bool", title: "Enable if using 4 speed fan:", defaultValue: false, required: false)
        }           
	}
}

private def appName() { return "${parent ? "The Child" : "TestPagesApp"}" }