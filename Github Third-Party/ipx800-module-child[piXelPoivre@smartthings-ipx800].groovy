
definition(
    name: "GCE IPX800 Module Child",
    namespace: "pixelpoivre",
    author: "Ben Abonyi",
    description: "IPX800 module",
    category: "lightening",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Module/dlink.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Module/dlink@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Module/dlink@3x.png"
)

preferences {
    page(name: "mainPage", title: "Install IPX800 module", install: true, uninstall:true) {
        section("Module Name") {
            label(name: "label", title: "Name This Module", required: true, multiple: false, submitOnChange: true)
        }
        section("Add a module") {
        	input("ModuleType","enum", title: "Module", description: "Please select your module", required:false, submitOnChange: true,
            options: ["X-Dimmer", "X-Display", "X-8D", "X-GSM", "X-8R", "X-4VR", "X-DMX", "X-Eno", "X-24D", "X-4FP"], displayDuringSetup: true)
            }
        section("Module Settings:"){
        	input("ModuleIP", "string", title:"Module IP Address", description: "Please enter your Module's IP Address", required: true, displayDuringSetup: true)
    		input("ModulePort", "string", title:"Module Port", description: "Please enter your Module's HTTP Port", defaultValue: 80 , required: true, displayDuringSetup: true)
    		input("VideoIP", "string", title:"Video IP Address", description: "Please enter your Module's IP Address (use external IP if you are using port forwarding)", required: true, displayDuringSetup: true)
    		input("VideoPort", "string", title:"Video Port", description: "Please enter your Module's Video Port (use external Port if you are using port forwarding)", required: true, displayDuringSetup: true)
    		input("ModuleUser", "string", title:"Module User", description: "Please enter your Module's username", required: false, displayDuringSetup: true)
    		input("ModulePassword", "password", title:"Module Password", description: "Please enter your Module's password", required: false, displayDuringSetup: true)
            }
        section("Hub Settings"){
        	input("hubName", "hub", title:"Hub", description: "Please select your Hub", required: true, displayDuringSetup: true)
        }
    }
    
}

def installed() {
    log.debug "Installed"

    initialize()
}

def updated() {
    log.debug "Updated"

    unsubscribe()
    initialize()
}

def initialize() {

        state.ModuleIP = ModuleIP
        state.ModulePort = ModulePort
        state.VideoIP = VideoIP
        state.VideoPort = VideoPort
        state.ModuleUser = ModuleUser
        state.ModulePassword = ModulePassword
        
        
        log.debug "Module IP: ${state.ModuleIP}"
        log.debug "Module Port: ${state.ModulePort}"
        log.debug "Video IP: ${state.VideoIP}"
        log.debug "Video Port: ${state.VideoPort}"
        log.debug "Module User: ${state.ModuleUser}"
        log.debug "Module Password: ${state.ModulePassword}"
        
        
	try {
        def DNI = (Math.abs(new Random().nextInt()) % 99999 + 1).toString()
        def Modules = getChildDevices()
        if (Modules) {
            Modules[0].configure()
        }
        else {
        	def childDevice = addChildDevice("pixelpoivre", ModuleType, DNI, hubName.id, [name: app.label, label: app.label, completedSetup: true])
        }
    } catch (e) {
    	log.error "Error creating device: ${e}"
    }
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}
