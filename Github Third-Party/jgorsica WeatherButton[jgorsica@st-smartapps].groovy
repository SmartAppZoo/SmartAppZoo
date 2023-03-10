/**
 *  Speak the weather
 *
 */
definition(
    name: "WeatherButton",
    namespace: "jgorsica",
    author: "John Gorsica",
    description: "Phone speaks the weather when button is pressed.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Speak weather, when this button is pressed:") {
    	input "button", "capability.button"
    }

	//section("Out of this phone:") {
    //	input "phone", "capability.speechSynthesis"
    //}
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
	subscribe(button, "button", pushHandler)
    logTravelTime()
}

def pushHandler(evt) {
	log.debug "$evt.name: $evt.value"
    //weather
    state.forecastDay = ""
    state.forecast = ""
    if ((evt.value == "pushed")) {
        def successWeatherCall = { response ->
            log.debug "Response was successful, $response"
            state.forecastDay = response.data[0].children[3].children[0].children[1].children[0].children[3].children[0]
            state.forecast = response.data[0].children[3].children[0].children[1].children[0].children[4].children[0]
        }
        def paramsWeather = [
            uri: "http://api.wunderground.com/api/da900696864aa51f/forecast/q/IL/Round_Lake.xml",
            contentType:"text/xml",
            success: successWeatherCall
        ];
        httpGet(paramsWeather);
        //phone.speak("Weather forecast for " + state.forecastDay + " is " + state.forecast);	
    }
    //pun
    state.pun = ""
    if ((evt.value == "pushed")) {
        def successPunCall = { response ->
            log.debug "Response was successful, $response"
            state.pun = response.data[0].children[0].children[9].children[2].text().split("&")[0]
        }
        def paramsPun = [
            uri: "http://feeds.feedburner.com/PunOfTheDay",
            contentType:"text/xml",
            success: successPunCall
        ];
        httpGet(paramsPun);
        log.debug state.pun
        //state.pun=Jsoup.parse(response.data[0].children[0].children[9].children[2]).text()
        //phone.speak("Pun of the Day is " + state.pun);	
    }
}

def logTravelTime() {
	state.travel = ""
    def successTravelCall = { response ->
        log.debug "Response was successful, $response"
        state.travel = response.getData().getText()
        log.debug state.travel
    }
    def paramsTravel = [
        uri: "https://www.google.com/maps/dir/2311+S+Arden+Ln,+Round+Lake,+IL+60073/Prairie+Crossing%2FLiberityville,+3001+Midlothian+Road,+Libertyville,+IL+60048/@42.3196917,-88.1200794,12z/data=!3m1!4b1!4m13!4m12!1m5!1m1!1s0x880f9c076511ff3f:0xb6c2001e3bddf3f!2m2!1d-88.090591!2d42.305915!1m5!1m1!1s0x880f9a74663c777d:0xac413455d4ebe0fd!2m2!1d-88.017585!2d42.318421",
        contentType:"text/xml",
        success: successTravelCall
    ];
    httpGet(paramsTravel);
    
}
