/**
 *  Pioneer VSX-1130 Integration via REST API Callback
 *
 *  Make sure and publish smartapp after pasting in code.
 *  Author: Scott Dozier
 */
definition(
    name: "Nexia Thermostat Integration",
    namespace: "scdozier",
    author: "Scott Dozier",
    description: "Nexia Thermostat Integration",
    category: "My Apps",
    iconUrl: "https://www.americanstandardair.com/content/irco-asa/us/en/products/thermostat-controls/gold-824-control.img.png/1434661623995.jpg",
    iconX2Url: "https://www.americanstandardair.com/content/irco-asa/us/en/products/thermostat-controls/gold-824-control.img.png/1434661623995.jpg",
    oauth: true
)


preferences {
	section("Allow App to Control This Nexia Thermostat...") {
		input "sensor", "capability.sensor", title: "Which Thermostat?", multiple: true
	}

}

mappings {

	path("/nexiathermostats") {
		action: [
			GET: "listThermostats"
		]
	}
	path("/sensor/:id") {
		action: [
			GET: "showSwitch"
		]
	}
	path("/nexiatherm/:id/:param/:state") {
		action: [
			GET: "updateThermostat"
		]
	}

}

def installed() { log.debug "apiServerUrl: ${apiServerUrl("my/path")}"}

def updated() {}


//sensor
def listThermostats() {
	sensor.collect{device(it,"sensor")}
}

def showSwitch() {
	show(sensor, "sensor")
}
void updateThermostat() {
	update(sensor)
}



def deviceHandler(evt) {}

private void update(devices) {
	log.debug "update, request: params: ${params}, devices: $devices.id"
	//def command = request.JSON?.command
    def param = params.param
    def state = params.state
    //let's create a toggle option here
	if (command)
    {
		def device = devices.find { it.id == params.id }
		if (!device) {
			httpError(404, "Device not found")
		} else {
        	device.update(param,state)
		}
	}
}

private show(devices, type) {
	def device = devices.find { it.id == params.id }
	if (!device) {
		httpError(404, "Device not found")
	}
	else {
		def attributeName = type == "motionSensor" ? "motion" : type
		def s = device.currentState(attributeName)
		[id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
	}
}


private device(it, type) {
	it ? [id: it.id, label: it.label, type: type] : null
}