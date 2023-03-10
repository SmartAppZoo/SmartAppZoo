/**
 *  Ftibit devices
 *
 *  Copyright 2018 Dr1rrb
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Fitbit devices",
    namespace: "torick.net",
    author: "Dr1rrb",
    description: "Manage your Fitbit devices",
    category: "Health & Wellness",
    iconUrl: "https://community.fitbit.com/html/assets/fitbit_logo_1200.png",
    iconX2Url: "https://community.fitbit.com/html/assets/fitbit_logo_1200.png",
    iconX3Url: "https://community.fitbit.com/html/assets/fitbit_logo_1200.png")


preferences {
	page(name: "Authenticate", title: "Fitbit authentication", content: "authenticatePageContent", nextPage: "SelectDevices", install: false, uninstall: true)
    page(name: "AuthenticateContent")
    
    page(name: "SelectDevices", title: "Fitbit devices", content: "selectDevicesPageContent", install: true, uninstall: true)
    page(name: "SelectDevicesContent")
}

mappings {
    path("/oauth/initialize") {action: [GET: "authenticationInit"]}
    path("/oauth/callback") {action: [GET: "authenticationCallback"]}
	path("/push") { action: [POST: "parsePushNotification"] }
}

def getClientId() {
	// This ID is here to ease deployement of the app in the smartthings community.
    // If you want to create an app, please create you own app ID here: https://dev.fitbit.com/apps/new
    // It's easy and it's free!
	return "22CHM5"
}

def getClientSecret() {
	// This kind of 'secret' is here to ease deployement of the app in the smartthings community.
    // If you want to create an app, please create you own app ID here: https://dev.fitbit.com/apps/new
    // It's easy and it's free!
	return "MjJDSE01OmIyZGRiMmFjNDJmYWYzNTM3MjQ2NzNjNTQxZWYzYWZl"
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	configure()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	configure()
}

def configure() {
	unsubscribe()
	unschedule()
    
    if (!state) {
    	state = [:]
    }

	setupDevices()
    update()
    
    runEvery1Hour("authenticationRefresh")
    runEvery3Hours("updateUserProfile") // no push for user profile, and there isn't any longer timer available (as of doc on 2018-02-01).
    schedule("0 0 3 1 * ?", "updateAccessToken") // The token may expire randomly ... so force refresh it every month
}

def setupDevices() {
	if(!selectedDevices) {
		log.debug "No devices selected yet."
	} else {
		log.debug "Selected devices (${selectedDevices.size()}) : ${selectedDevices.collect{ it }}"

		selectedDevices.each { setupDevice(parseJson(it)) }
	}
}

def setupDevice(device) {
    log.debug "Setuping device (${device.id}) ${device}"

	def child = getChildDevice(device.id)
	if (child) {
		log.debug "Device ${device.id} already exists"
	} else {
		log.debug "Creating ${device.id}"
		addChildDevice("torick.net", "Fitbit device", device.id, null, ["label": device.name])
	}
}

/// Settings pages ******************************************************************************************************************************************************
def authenticatePageContent() {
	log.debug "Get authentication page content"

	dynamicPage(name: "AuthenticateContent", title: "Fitbit authentication", nextPage: "SelectDevices", install: false, uninstall: true) {
        loginSection()
    }
}

def loginSection()
{
	if(!state.accessToken) {
        createAccessToken()
    }

	def redirectUrl = "https://graph.api.smartthings.com/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${getApiServerUrl()}"
    
	if(isAuthenticated)
    {
    	try {
        	def userName = makeGet("https://api.fitbit.com/1/user/-/profile.json").user.displayName 
            
            return section() {
                paragraph "User are logged in as ${userName}. Tap below to log in with another user, or click 'Next' to select devices."
                href url: redirectUrl, style: "embedded", required: false, title: "Login", description: "Click to enter credentials"
            }
       	}
        catch(Exception e)
        {
        	log.debug "Failed to get authenticated user section: ${e}"
            
            // An exception which did not logged out the user ... (API down?) we do not allow the user to re-login
            if(isAuthenticated) {
            	return section() {
                    paragraph "Failed to get user info, check your network connection (Does Fitbit API is down?)."
                }
            }
        }
    }
    
    return section() {
        paragraph "Tap below to log in to Fitbit and authorize SmartThings access."
        href url: redirectUrl, style: "embedded", required: true, title: "Login", description: "Click to enter credentials"
    }
}

def selectDevicesPageContent()
{
    log.debug "Get select devices page content"
    
    def devices = makeGet("https://api.fitbit.com/1/user/-/devices.json")
        .findAll{ it.type == "TRACKER" }
        .collectEntries { [ new groovy.json.JsonBuilder([id: it.id, name: it.deviceVersion, type: it.type]).toString(), "${it.deviceVersion}"] }
        
    return dynamicPage(name: "SelectDevicesContent", title: "Fitbit devices", nextPage: "", install: true, uninstall: true) {
        section() {
            paragraph "Select the Fitbit devices you want to add to smartthings:"
            input "selectedDevices", "enum", required: true, title: "Select devices (${devices.size() ?: 0} found)", multiple: true, options: devices
        }
    }
}

/// Authentication process **********************************************************************************************************************************************
def getIsAuthenticated() { state.authToken != null && !state.authToken.isAllWhitespace() }

def authenticationInit() {
	log.debug "Initializing authentication process"

    // Generate a random ID to use as a our state value. This value will be used to verify the response we get back from the third-party service.
    state.oauthInitState = UUID.randomUUID().toString()

    def oauthParams = [
        response_type: "code",
        scope: "activity profile settings sleep",
        client_id: clientId,
        state: state.oauthInitState,
        redirect_uri: "https://graph.api.smartthings.com/oauth/callback",
        expires_in: "604800"
    ]

    redirect(location: "https://www.fitbit.com/oauth2/authorize?${toQueryString(oauthParams)}")
}

def authenticationCallback() {
    log.debug "Authentication process callback >> params: ${params}, params.code ${params.code}"

    def oauthCode = params.code
    def oauthState = params.state

    // Validate the response from the third party by making sure oauthState == state.oauthInitState as expected
    if (oauthState == state.oauthInitState) {
        httpPost([
            uri: "https://api.fitbit.com/oauth2/token", 
            headers: [ 
                Authorization: "Basic ${clientSecret}",
                "Content-Type": "application/x-www-form-urlencoded"],
            body: toQueryString([
                grant_type: "authorization_code",
                code: oauthCode,
                client_id : clientId,
                redirect_uri: "https://graph.api.smartthings.com/oauth/callback",
                state: state.oauthInitState,
                expires_in: "28800"
            ])
        ]) 
        {
            response ->
            	log.debug "Successfully got authentication tokens"
            	
                if(response.status == 200 && response.data) {
                    state.userId = response.data.user_id
                    state.refreshToken = response.data.refresh_token
                    state.authToken = response.data.access_token
                }
        }

        if (isAuthenticated) {
            return toHtmlDoc("""<p>Your account is now connected to SmartThings!</p><p>Click 'Done' to continue setup.</p>""")
        } else {
            return toHtmlDoc("""<p>There was an error connecting your account with SmartThings</p><p>Please try again.</p>""")
        }
    } else {
        log.error "authenticationCallback() failed. Validation of state did not match. oauthState != state.oauthInitState"
        
        return toHtmlDoc("""<p>Something went wrong (Invalid authentication state).</p><p>Please try again.</p>""")
    }
}

def authenticationRefresh() {
	log.debug "Refreshing authentication token"

	try{
    	def succeed = false
        
        httpPost([
            uri: "https://api.fitbit.com/oauth2/token", 
            headers: [ 
                Authorization: "Basic ${clientSecret}",
                "Content-Type": "application/x-www-form-urlencoded"],
            body: toQueryString([
                grant_type: "refresh_token", 
                refresh_token: state.refreshToken, 
                expires_in: "28800"])
        ])
        {
        	response -> 
            	log.debug "Refresh result (${response.status}): ${response.data}"
            
                if(response?.data?.access_token)
                    state.authToken = response.data.access_token
            	if(response?.data?.refresh_token)
                    state.refreshToken = response.data.refresh_token
                    
                succeed = response?.data?.access_token != null
		}
        
        log.debug "Token refresh result: ${succeed}"
        
        return succeed
    }
    catch(groovyx.net.http.HttpResponseException error)
    {
        if (error.statusCode == 401 || (error.statusCode == 400 && error.response.data.errors.find{ it.errorType == "invalid_grant"})) {
        	log.error "The refresh token is invalid, logoff the user"
        
        	state.userId = null
            state.refreshToken = null
            state.authToken = null
        } else {
        	log.error "Failed to refresh the token: ${error}"
        }

		return false;
    }
}

/// User's data *******************************************************************************************************************************************
def update() {
    updateAccessToken()

	updateUserProfile()
    updateUserSubscriptions()
    updateActivities()
    updateSleep()
}

def updateAccessToken() {
    // For some reason, it may happen that the token does not work anymore ... so we force refresh it every month
	revokeAccessToken()
    state.accessToken = null
}

def updateUserProfile() {
	if (isAuthenticated) {
        try {
            log.debug "Updating user profile"
            state.userProfile = makeGet("https://api.fitbit.com/1/user/-/profile.json")
        } catch(Exception e) {
            log.error "Failed to update user profile: ${e}"
        }
	} else {
    	state.userProfile = null
    }
}

def updateUserSubscriptions() {
    updateSubscription("a", "activities", "updateActivities")
    updateSubscription("s", "sleep", "updateSleep")
}

def updateSubscription(String scopeId, String scope, String fallback) {
	unschedule(fallback)
    
    if (!isAuthenticated) {
    	return
    }

	if(!state.accessToken) {
        createAccessToken()
    }

	try {
    	def accessToken = encodeId(state.accessToken)
        def locationId = encodeId(location.id)
        def subscriptionId = "${scopeId}${accessToken}~${locationId}"
        def subscriptions = makeGet("https://api.fitbit.com/1/user/-/${scope}/apiSubscriptions.json")?.apiSubscriptions
        
        log.debug "Scope: ${scope} / Expected: ${subscriptionId} / Current subscriptions: ${subscriptions}"
        
        // First delete invalid subscriptions
        subscriptions?.findAll{ it.subscriptionId.endsWith(locationId) && it.subscriptionId != subscriptionId }?.each{ 
            log.debug "Found a subscription for scope ${scope} for this location but with an invalid access token (ID: ${it.subscriptionId})"

            try {
                httpDelete([
                    uri: "https://api.fitbit.com/1/user/-/${scope}/apiSubscriptions/${it.subscriptionId.encodeAsURL()}.json",
                    headers: [ Authorization: "Bearer ${state.authToken}"]
                ])
            } catch(Exception e) {
                log.error "Failed to delete subscription ${it.subscriptionId}: ${e}"
            }
        }
        
        // Then if missing, create a new one
        if (!subscriptions?.find{ it.subscriptionId == subscriptionId }) {
            log.debug "No valid subscription for scope ${scope}, creating a new one: ${subscriptionId}"
        
        	httpPost([
                uri: "https://api.fitbit.com/1/user/-/${scope}/apiSubscriptions/${subscriptionId.encodeAsURL()}.json",
                headers: [ Authorization: "Bearer ${state.authToken}"]
            ])
        }
        
        log.debug "Push channel subscription is valid  for scope ${scope} (id: ${subscriptionId})"
    } catch(Exception e) {
    	log.error "Failed to update subscription for scope ${scope}, fallback to pulling (${fallback}): ${e}"
        
        runEvery1Hour(fallback)
    }
}

def parsePushNotification() {
	log.debug "Received push notification from fitbit : ${params} / JSON: ${request.JSON}"
    
    if (request.JSON?.find { it.collectionType == "activities" }) {
    	updateActivities()
	}
        
    if (request.JSON?.find { it.collectionType == "sleep" }) {
    	updateSleep()
	}
}

def updateActivities() {
	log.debug "Updating activities"
    
    def activitiesResponse = makeGet("https://api.fitbit.com/1/user/-/activities/date/${new Date().format("yyyy-MM-dd", location.timeZone)}.json")
    childDevices.each{ it.updateActivities(activitiesResponse) }
}

def updateSleep() {
	log.debug "Updating sleep"
    
	def sleepResponse = makeGet("https://api.fitbit.com/1/user/-/sleep/goal.json")
    childDevices.each{ it.updateSleep(sleepResponse) }
}

/// HTTP helpers *******************************************************************************************************************************************
def makeGet(String uri) { makeGet([uri: uri])}
def makeGet(Map query)
{
	if (!query.headers?.Authorization) {
		query.headers = (query.headers ?: [:]) << [ Authorization: "Bearer ${state.authToken}"]
	}

    try {
    	log.debug "Execute query: ${query}"
        
        def result;
        
        httpGet(query, { response -> result = response.data})
        
        return result
    } catch(groovyx.net.http.HttpResponseException error) {
        log.error "Query error: ${error}"

        if (error.statusCode == 401 && authenticationRefresh()) {
            return makeGet(query)
        }
        
        throw error
    }
}

String toQueryString(Map m) { m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&") }

def toHtmlDoc(message) {
    render(
    	contentType: 'text/html', 
        data: """<!DOCTYPE html><html><head></head><body><div>${message}</div></body></html>""")
}

/// Subscription ID encoding *******************************************************************************************************************************************
def encodeId(String id) {
    def bin = id.findAll{ it != '-' }.collect{ charToBits(it) }.join()
    
    def encoded = new StringBuilder();
	def i;
    for (i = 0; i < ((bin.length() / 6) as Integer)*6; i+=6)
    {
        encoded.append(encodeBitsToChar(bin.substring(i, i + 6)))
    }
    for (; i < bin.length(); i+=2)
    {
        encoded.append(encodeBitsToChar(bin.substring(i, i + 2)))
    }
    
    log.debug "id: ${id} / bin: ${bin} / encoded: ${encoded}"
    
    return encoded.toString()
}

def decodeId(String encoded) {
    def bin = encoded.collect{ decodeCharToBits(it) }.join()
    
    def decoded = new StringBuilder();
	for (def i = 0; i < bin.length(); i += 4)
    {
        decoded.append(bitsToChar(bin.substring(i, i + 4)))
    }

	log.debug "encoded: ${encoded} / bin: ${bin} / id: ${decoded}"

    return decoded.toString()
}

def charToBits(c) {
    switch (c)
    {
        case '0': return "0000";
        case '1': return "0001";
        case '2': return "0010";
        case '3': return "0011";
        case '4': return "0100";
        case '5': return "0101";
        case '6': return "0110";
        case '7': return "0111";
        case '8': return "1000";
        case '9': return "1001";
        case 'a': return "1010";
        case 'b': return "1011";
        case 'c': return "1100";
        case 'd': return "1101";
        case 'e': return "1110";
        case 'f': return "1111";

        default: throw new SecurityException("Out of range: ${c}");
    }
}

def bitsToChar(String bits) {
    switch (bits)
    {
        case "0000": return '0';
        case "0001": return '1';
        case "0010": return '2';
        case "0011": return '3';
        case "0100": return '4';
        case "0101": return '5';
        case "0110": return '6';
        case "0111": return '7';
        case "1000": return '8';
        case "1001": return '9';
        case "1010": return 'a';
        case "1011": return 'b';
        case "1100": return 'c';
        case "1101": return 'd';
        case "1110": return 'e';
        case "1111": return 'f';

        default: throw new SecurityException("Out of range: ${c}");
    }
}

def encodeBitsToChar(bits) {
    switch (bits)
    {
        case "00": return '*';
        case "01": return '$';
        case "10": return '=';
        case "11": return '^';

        case "000000": return '0';
        case "000001": return '1';
        case "000010": return '2';
        case "000011": return '3';
        case "000100": return '4';
        case "000101": return '5';
        case "000110": return '6';
        case "000111": return '7';
        case "001000": return '8';
        case "001001": return '9';
        case "001010": return 'A';
        case "001011": return 'B';
        case "001100": return 'C';
        case "001101": return 'D';
        case "001110": return 'E';
        case "001111": return 'F';
        case "010000": return 'G';
        case "010001": return 'H';
        case "010010": return 'I';
        case "010011": return 'J';
        case "010100": return 'K';
        case "010101": return 'L';
        case "010110": return 'M';
        case "010111": return 'N';
        case "011000": return 'O';
        case "011001": return 'P';
        case "011010": return 'Q';
        case "011011": return 'R';
        case "011100": return 'S';
        case "011101": return 'T';
        case "011110": return 'U';
        case "011111": return 'V';
        case "100000": return 'W';
        case "100001": return 'X';
        case "100010": return 'Y';
        case "100011": return 'Z';
        case "100100": return 'a';
        case "100101": return 'b';
        case "100110": return 'c';
        case "100111": return 'd';
        case "101000": return 'e';
        case "101001": return 'f';
        case "101010": return 'g';
        case "101011": return 'h';
        case "101100": return 'i';
        case "101101": return 'j';
        case "101110": return 'k';
        case "101111": return 'l';
        case "110000": return 'm';
        case "110001": return 'n';
        case "110010": return 'o';
        case "110011": return 'p';
        case "110100": return 'q';
        case "110101": return 'r';
        case "110110": return 's';
        case "110111": return 't';
        case "111000": return 'u';
        case "111001": return 'v';
        case "111010": return 'w';
        case "111011": return 'x';
        case "111100": return 'y';
        case "111101": return 'z';
        case "111110": return '-';
        case "111111": return '_';

        default: throw new SecurityException("Out of range: ${bits}");
    }
}

def decodeCharToBits(c) {
    switch (c)
    {
        case '*': return "00";
        case '$': return "01";
        case '=': return "10";
        case '^': return "11";

        case '0': return "000000";
        case '1': return "000001";
        case '2': return "000010";
        case '3': return "000011";
        case '4': return "000100";
        case '5': return "000101";
        case '6': return "000110";
        case '7': return "000111";
        case '8': return "001000";
        case '9': return "001001";
        case 'A': return "001010";
        case 'B': return "001011";
        case 'C': return "001100";
        case 'D': return "001101";
        case 'E': return "001110";
        case 'F': return "001111";
        case 'G': return "010000";
        case 'H': return "010001";
        case 'I': return "010010";
        case 'J': return "010011";
        case 'K': return "010100";
        case 'L': return "010101";
        case 'M': return "010110";
        case 'N': return "010111";
        case 'O': return "011000";
        case 'P': return "011001";
        case 'Q': return "011010";
        case 'R': return "011011";
        case 'S': return "011100";
        case 'T': return "011101";
        case 'U': return "011110";
        case 'V': return "011111";
        case 'W': return "100000";
        case 'X': return "100001";
        case 'Y': return "100010";
        case 'Z': return "100011";
        case 'a': return "100100";
        case 'b': return "100101";
        case 'c': return "100110";
        case 'd': return "100111";
        case 'e': return "101000";
        case 'f': return "101001";
        case 'g': return "101010";
        case 'h': return "101011";
        case 'i': return "101100";
        case 'j': return "101101";
        case 'k': return "101110";
        case 'l': return "101111";
        case 'm': return "110000";
        case 'n': return "110001";
        case 'o': return "110010";
        case 'p': return "110011";
        case 'q': return "110100";
        case 'r': return "110101";
        case 's': return "110110";
        case 't': return "110111";
        case 'u': return "111000";
        case 'v': return "111001";
        case 'w': return "111010";
        case 'x': return "111011";
        case 'y': return "111100";
        case 'z': return "111101";
        case '-': return "111110";
        case '_': return "111111";

        default: throw new SecurityException("Out of range: ${c}");
    }
}