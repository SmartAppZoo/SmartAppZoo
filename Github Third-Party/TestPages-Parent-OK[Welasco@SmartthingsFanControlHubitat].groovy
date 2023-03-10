definition(
    name: "TestPagesApp-Parent",
    //name: "CoRE${parent ? " - Piston" : ""}",
    namespace: "Victor",
    author: "Victor",
    description: "Test Pages and Child App.",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
    iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
    iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
)

preferences {
	page(name: "parentPage")
}

def parentPage() {
	dynamicPage(name: "parentPage", title: "", nextPage: "", install: true, uninstall: true) {
      section("Create a new fan automation. 5") {
            //app(name: "childApps", appName: appName(), namespace: "Victor", title: "New ChildApp", multiple: true)
            app(name: "childApps", appName: "TestPagesApp-Child", namespace: "Victor", title: "New ChildApp", multiple: true)

        }
    }
}

def updated() {
    initialize()
}

def installed() {
    initialize()
}

def uninstalled() {

}

def initialize() {

}

def childUninstalled() {

}