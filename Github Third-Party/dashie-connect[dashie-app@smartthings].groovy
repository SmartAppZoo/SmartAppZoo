/**
 *  dashie-connect 
 *
 *  Copyright 2020 Tobias Haerke
 *  Based on smartthings-rest-api by Julian Werfel https://github.com/Jwerfel/smartthings-rest-api
 *
 *  Licensed under The GNU General Public License is a free, copyleft license for
software and other kinds of works. 
 *
 *     https://www.gnu.org/licenses/gpl-3.0.en.html
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "dashie-connect",
    namespace: "dashie-app.dashie-connect",
    author: "Tobias Haerke",
    description: "Connect to dashie.app",
    category: "SmartThings Labs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: [displayName: "dashie-app", displayLink: ""]
)

mappings {
  path("/devices") {
    action: [
        GET: "listDevices"
    ]
  }
  path("/device/:id") {
    action: [
        GET: "deviceDetails"
    ]
  }
  path("/device/:id/attribute/:name") {
    action: [
        GET: "deviceGetAttributeValue"
    ]
  }
  path("/device/:id/attributes") {
    action: [
      GET: "deviceGetAttributes"
    ]
  }
  path("/devices/attribute/:name") {
    action: [
        GET: "deviceGetAttributeValueForDevices"
    ]
  }
  path("/devices/attributes") {
    action: [
      GET: "devicesGetAttributes"
    ]
  }
  path("/device/:id/command/:name") {
    action: [
        POST: "deviceCommand"
    ]
  }
  path("/device/status/:id") {
  	action: [
    	GET: "deviceStatus"
    ]
  }
  path("/devices/statuses") {
  	action: [
    	GET: "devicesStatuses"
    ]
  }
	path("/device/events/:id") {
    action: [
      GET: "deviceEvents"
    ]
  }
  path("/test") {
  	action: [
      GET: "test"
    ]
  }
  path("/routines") {
  	action: [
      GET: "getRoutines"
    ]
  }
  path("/routine") {
    action: [
      POST: "executeRoutine"
    ]
  }
  path("/modes") {
    action: [
      GET: "getModes"
    ]
  }
  path("/mode") {
    action: [
      GET: "getCurrentMode",
      POST: "setCurrentMode"
    ]
  }
}

preferences {
  section() {
    input "devices", "capability.actuator", title: "Devices", multiple: true, required: false
    input "sensors", "capability.sensor", title: "Sensors", multiple: true, required: false
    input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", multiple: true, required: false
    input "presenceSensor", "capability.presenceSensor", title: "Presence", multiple: true, required: false
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
}

// devices

def listDevices() {
  def items = [*devices, *sensors, *presenceSensor, *temperatures] - null
    def resp = []
    
    items.each {
      resp << parseDevice(it)
    }
    
    // return resp
    render contentType: "application/javascript", data: "${params.callback}(${resp.encodeAsJSON()})"
}

def deviceDetails() {
  def device = getDeviceById(params.id)
  def resp = parseDevice(device)
    
    resp.status = getDeviceStatus(device)
    
    // return resp
    render contentType: "application/javascript", data: "${params.callback}(${resp.encodeAsJSON()})"
}

def parseDevice(def device) {
  return [
        id: device?.id,
        label: device?.label,
        manufacturerName: device?.manufacturerName,
        modelName: device?.modelName,
        name: device?.name,
        displayName: device?.displayName,
        capabilities: getDeviceAttributes(device),
        commands: getDeviceCommands(device),
    ]
}

def getDeviceAttributes(def device) {
  def attributes = []
  device?.getSupportedAttributes().each { attributes << it.name }
    return attributes
}

def getDeviceCommands(def device) {
  def cmds = []
  device?.getSupportedCommands().each { cmds << it.name }
    return cmds
}

def getDeviceStatus(def device) {
    def resp = [:]
    getDeviceAttributes(device).each {
      resp.put(it, device?.currentState(it)?.getValue())
    }
    return resp;
}


def deviceCommand() {
  def device = getDeviceById(params.id)
  def command = params.command
  def parameters = parseCommandParameters(params?.parameters)
  
  log.debug "device cmd: ${command} parameters: ${parameters}"
  
  if(device.hasCommand(command)) {
    if (parameters.size() < 1) {
      device."${command}"() //  TODO: this is really unsafe... (⸝⸝⸝ಠ︿ಠ⸝⸝⸝)
    }
    if (parameters.size() == 1) {
      device."${command}"(parameters[0])
    }
    if (parameters.size() == 2) {
      device."${command}"(parameters[0], parameters[1])
    }
    if (parameters.size() == 3) {
      device."${command}"(parameters[0], parameters[1], parameters[2])
    }
  }
  
  render contentType: "application/javascript", data: "${params.callback}(${[].encodeAsJSON()})"
}

def parseCommandParameters(def params) {
  def resp = []
    params = params ? [*((params+',').split(',') - null)] : []
    
    params?.each {
        if (it.isInteger()) { resp << (it as int) }
        else if (it.isLong()) { resp << (it as long) }
        else if (it.isBigInteger()) { resp << (it as BigInteger) }
        else if (it.isDouble()) { resp << (it as double) }
        else { resp << it }
    }
    
    return resp
}

// routines

def getRoutines() {
  def resp = []
    
    location.helloHome?.getPhrases()?.each {
      resp << parseRoutine(it)
    }
    
    // return resp;
    render contentType: "application/javascript", data: "${params.callback}(${resp.encodeAsJSON()})"
}

def getRoutineById() {
  def resp = []
    def routine = location.helloHome?.getPhrases().find { it?.id == params.id }
    
    // return parseRoutine(routine);
    render contentType: "application/javascript", data: "${params.callback}(${parseRoutine(routine).encodeAsJSON()})"
}

def parseRoutine(def routine) {
  return [
      id: routine?.id,
        name: routine?.label,
    ]
}

def executeRoutine(){
  def routine = location.helloHome?.getPhrases().find { it?.id == params.id }
    log.info("Executing routine: " + routine);
    if(routine) { location.helloHome?.execute(routine.label) }
    render contentType: "application/javascript", data: "${params.callback}(${[].encodeAsJSON()})"
}

// modes

def getModes() {
  def resp = []
    
    location.modes?.each {
      resp << parseMode(it)
    }
    
  // return resp
    render contentType: "application/javascript", data: "${params.callback}(${resp.encodeAsJSON()})"
}

def parseMode(def mode) {
  return [
      id: mode?.id,
        name: mode?.name,
    ]
}

def getCurrentMode() {
  def mode = location.modes?.find {it.name == location.mode}
    render contentType: "application/javascript", data: "${params.callback}(${parseMode(mode).encodeAsJSON()})"
}

def setCurrentMode() {
  def id = params?.id;
    
    log.info("Executing setModes id: " + id);
    
    if (id) {
      def found = location.modes?.find {it.id == id}
        if (found) {
        log.info("setModes found: " + found);
          setLocationMode(found);
        }
    }
    
  render contentType: "application/javascript", data: "${params.callback}(${[].encodeAsJSON()})"
}

def getDeviceById(id) { // TODO: isn't there an st api for that?
  def device = devices.find { it.id == id }
  if(device == null)
    device = sensors.find{it.id == id}
  if(device == null)
    device = temperatures.find{it.id == id}
  if(device == null)
    device = presenceSensor.find{it.id == id}
  return device;
}