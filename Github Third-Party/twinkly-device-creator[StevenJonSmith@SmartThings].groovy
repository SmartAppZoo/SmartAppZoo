/**
 *  Twinkly Device Creator
 *
 *  Copyright 2019 Steven Jon Smith
 *
 *  Please read carefully the following terms and conditions and any accompanying documentation
 *  before you download and/or use this software and associated documentation files (the "Software").
 *
 *  The authors hereby grant you a non-exclusive, non-transferable, free of charge right to copy,
 *  modify, merge, publish, distribute, and sublicense the Software for the sole purpose of performing
 *  non-commercial scientific research, non-commercial education, or non-commercial artistic projects.
 *
 *  Any other use, in particular any use for commercial purposes, is prohibited. This includes, without
 *  limitation, incorporation in a commercial product, use in a commercial service, or production of other
 *  artefacts for commercial purposes.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 *  OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  You understand and agree that the authors are under no obligation to provide either maintenance services,
 *  update services, notices of latent defects, or corrections of defects with regard to the Software. The authors
 *  nevertheless reserve the right to update, modify, or discontinue the Software at any time.
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions
 *  of the Software. You agree to cite the Steven Jon Smith in your notices.
 *
 */
definition(
    name: "Twinkly Device Creator",
    namespace: "StevenJonSmith",
    author: "Steven Jon Smith",
    description: "Helps users to create Twinkly devices for Twinkly devices pre-connected to the same local network.",
    category: "My Apps",
    iconUrl: "https://twinkly.com/wp-content/uploads/2019/09/cropped-twinkly-icon-32x32.png",
    iconX2Url: "https://twinkly.com/wp-content/uploads/2019/09/cropped-twinkly-icon-180x180.png",
    iconX3Url: "https://twinkly.com/wp-content/uploads/2019/09/cropped-twinkly-icon-270x270.png") {
    appSetting "deviceIP"
}

preferences {
	page(name: "addDevice", title: "Create Twinkly Device", install: true, uninstall: true) {
        section("Which hub shares the same network as the Twinkly device?") {
        	input name: "installHub", type: "hub", required: true
        }
		section("IP address of Twinkly device:") {
            input "deviceIP", "text", required: true
        }
		section("Name of Twinkly device to add:") {
            input "deviceName", "text", required: true
        }
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
    addDevice()

    log.debug "Application Initialized"
}

def addDevice() { 
    def ipAddressHex = convertIPToHex(deviceIP)
    def ipAddress = convertHexToIP(ipAddressHex)

    def dni = "${ipAddressHex}:${convertPortToHex(80)}"
    def d = getChildDevice(dni)
    if(!d) {
        log.debug "Hub: " + installHub.id
        
        d = getAuthToken(dni)
    } else {
        log.debug "Device with id $dni already created"
    }
}

def createChild() {
	def ipAddressHex = convertIPToHex(deviceIP)
	def dni = "${ipAddressHex}:${convertPortToHex(80)}"
    
    try {
        addChildDevice("StevenJonSmith", "Twinkly Device", dni, installHub.id, 
                       [
                           name: "Twinkly Device", 
                           label: "$deviceName", 
                           completedSetup: true,
                           preferences: [
                               deviceIP: "$deviceIP"
                           ]
                       ]
                      )
        def d = getChildDevice(dni)
        log.debug "Created ${d.displayName} with device id $dni"
    } catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPToHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex.toUpperCase()
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport.toUpperCase()
}

def String getAuthToken(dni) {
	def challenge = challengeGenerator()
    def commandText = "{\"challenge\":\"$challenge\"}"    
    def httpRequest = [
        path: "/xled/v1/login",
      	method: "POST",
        headers: [
			HOST: "$deviceIP:80",
			"Content-Type":	"application/json"
		],
        body: "$commandText"
	]
    
    try {
    	def hubAction = new physicalgraph.device.HubAction(httpRequest, null, [callback: parse])
        log.debug "hub action: $hubAction"
        return sendHubCommand(hubAction)
    }
    catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

private String challengeGenerator() {
	def pool = ['a'..'z','A'..'Z',0..9].flatten()
    def rand = new Random()
    def challenge = ""
    
    for (def i = 0; i < 32; i++) {
    	challenge = challenge + pool[rand.nextInt(pool.size())]
	}
    log.debug "Created challenge: $challenge"
    
    challenge = challenge.bytes.encodeBase64()
    log.debug "Encoded challenge: $challenge"

	return challenge
}

def parse(physicalgraph.device.HubResponse output) {
	log.debug "Starting response parsing on ${output}"

	def headers = ""
	def parsedHeaders = ""
    
    def msg = output

    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    def json = msg.json              // => any JSON included in response body, as a data structure of lists and maps
    def xml = msg.xml                // => any XML included in response body, as a document tree structure
    def data = msg.data              // => either JSON or XML in response body (whichever is specified by content-type header in response)

	log.debug "headers: ${headerMap}, status: ${status}, body: ${body}, data: ${data}"
    
    if (status == 200) {
    	if (body.contains("authentication_token")) {
   			def authToken = null
            body = new groovy.json.JsonSlurper().parseText(body)
            log.debug "$body"
            authToken = body['authentication_token']

            if (authToken != null && authToken != "") {
                log.debug "Auth Token: $authToken"
                createChild()
    		}
        }
    }
    else {
    	log.debug "Unable to locate device on your network"
    }
}