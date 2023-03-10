definition(
    name: "TestPagesApp-Parent",
    //name: "CoRE${parent ? " - Piston" : ""}",
    namespace: "Victor",
    author: "Victor",
    description: "Test Pages and Child App.",
    category: "My Apps",
    //parent: parent ? "Victor:TestPagesApp-Parent" : null,
    parent: appName(),
    iconUrl: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png", 
    iconX2Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
    iconX3Url: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft250x250.png",
)

preferences {
    page(name: "startPage")
    page(name: "parentPage")
    page(name: "childStartPage")
    page(name: "optionsPage")
    page(name: "aboutPage")
}

def startPage() {
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
            //app(name: "childApps", appName: "TestPagesApp-Child", namespace: "Victor", title: "New ChildApp", multiple: true)
            app(name: "childApps", appName: appName(), namespace: "Victor", title: "New ChildApp", multiple: true)

        }
    }
}

def childStartPage() {
	dynamicPage(name: "childStartPage", title: "Select your devices and settings", install: true, uninstall: true) {
    
        section("Select a room temperature sensor to control the fan..."){
			input "tempSensor", "capability.temperatureMeasurement", multiple:false, title: "Temperature Sensor", required: true, submitOnChange: true  
		}
        if (tempSensor) {  //protects from a null error
    		section("Enter the desired room temperature setpoint...\n" + "NOTE: ${tempSensor.displayName} room temp is ${tempSensor.currentTemperature}° currently"){
        		input "setpoint", "decimal", title: "Room Setpoint Temp", defaultValue: tempSensor.currentTemperature, required: true
    		}
        }
        else 
        	section("Enter the desired room temperature setpoint..."){
        		input "setpoint", "decimal", title: "Room Setpoint Temp", required: true
    		}       
        section("Select the ceiling fan control hardware..."){
			input "fanDimmer", "capability.switchLevel", 
	    	multiple:false, title: "Fan Control device", required: true
		}
        section("Optional Settings (Diff Temp, Timers, Motion, etc)") {
			href (name: "optionsPage", 
        	title: "Configure Optional settings", 
        	description: none,
        	image: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/evap-cooler-thermostat.src/settings250x250.png",
        	required: false,
        	page: "optionsPage"
        	)
        }

        section("Name") {
        	label(title: "Assign a name", required: false)
        }

        section("Version Info, User's Guide") {
// VERSION
			href (name: "aboutPage", 
			title: "3 Speed Ceiling Fan Thermostat \n"+"Version:3.09232018 \n"+"Copyright © 2016 Dale Coffing", 
			description: "Tap to get user's guide.",
			image: "https://raw.githubusercontent.com/dcoffing/SmartThingsPublic/master/smartapps/dcoffing/3-speed-ceiling-fan-thermostat.src/3scft125x125.png",
			required: false,
			page: "aboutPage"
			)
		}
	}
}      

def optionsPage() {
	dynamicPage(name: "optionsPage", title: "Configure Optional Settings", install: false, uninstall: false) {
       	section("Enter the desired differential temp between fan speeds"){
			input "fanDiffTempString", "enum", title: "Fan Differential Temp", options: ["0.5","1.0","1.5","2.0","2.5","3.0","10.0"], required: false
		}
		section("Enable ceiling fan thermostat only if motion is detected at (optional, leave blank to not require motion)..."){
			input "motionSensor", "capability.motionSensor", title: "Select Motion device", required: false, submitOnChange: true
		}
        if (motionSensor) {
			section("Turn off ceiling fan thermostat when there's been no motion detected for..."){
				input "minutesNoMotion", "number", title: "Minutes?", required: true
			}
		}
		section("Enable ceiling fan thermostat only if someone is present..."){
			input "presenceSensor", "capability.presenceSensor", title: "Select Presence device", required: false, multiple:true
		}        
		section("Enable ceiling fan thermostat only if during the day (Sun is UP)..."){
			input "sunsetsunrise", "bool", title: "Select True or False:", defaultValue: false, required: false
		}            
        section("Select ceiling fan operating mode desired (default to 'YES-Auto'..."){
			input "autoMode", "enum", title: "Enable Ceiling Fan Thermostat?", options: ["NO-Manual","YES-Auto"], required: false
		}
    	section ("Change SmartApp name, Mode selector") {
		    //mode title: "Set for specific mode(s)", required: false
            input "modes", "mode", title: "select a mode(s)", required: false, multiple: true
		}
		section("Enable Debug Log at SmartThing IDE"){
			input "idelog", "bool", title: "Select True or False:", defaultValue: false, required: false
		}          
    }
}

def aboutPage() {
	dynamicPage(name: "aboutPage", title: none, install: true, uninstall: true) {
     	section("User's Guide; 3 Speed Ceiling Fan Thermostat") {
        	paragraph textHelp()
 		}
	}
}

private def appName() { return "${parent ? "3 Speed Fan Automation" : "Victor:TestPagesApp-Parent"}" }
