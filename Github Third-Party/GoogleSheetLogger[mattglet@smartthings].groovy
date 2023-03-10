// Used https://github.com/loverso-smartthings/googleDocsLogging as a starting point

definition(
    name: "Google Sheet Logger",
    namespace: "mattglet",
    author: "Matt Gillette",
    description: "Log to Google Sheets",
    category: "My Apps",
    iconUrl: "https://raw.githubusercontent.com/loverso-smartthings/googleDocsLogging/master/img/logoSheets.png",
    iconX2Url: "https://raw.githubusercontent.com/loverso-smartthings/googleDocsLogging/master/img/logoSheets@2x.png",
    iconX3Url: "https://raw.githubusercontent.com/loverso-smartthings/googleDocsLogging/master/img/logoSheets@2x.png")

preferences {
    section("Google Spreadsheet Settings") {
        input "urlKey", "text", title: "Spreadsheet key", required: true
    }
    section("Log events for..."){
        input "accelerationSensor", "capability.accelerationSensor", title: "Acceleration", required: true, multiple: false
        input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required: false, multiple: false
    }
}

def installed() {
    setState()
    log.debug "Installed with settings: ${settings}"
    log.debug "State: ${state}"
    subscribe(accelerationSensor, "acceleration", accelerationHandler)
    subscribe(temperatures, "temperature", temperatureHandler)
}

def updated() {
    unsubscribe()
    setState()
    log.debug "Updated with settings: ${settings}"
    log.debug "State: ${state}"
    subscribe(accelerationSensor, "acceleration", accelerationHandler)
    subscribe(temperatures, "temperature", temperatureHandler)
}

def setState() {
    state.lastActive = (now() - 300000) // default value of 5 minutes ago
    state.lastInactive = (now() - 300000) // default value of 5 minutes ago
    state.delayLoggingMilliseconds = (1 * 60000) // one minute
}

def accelerationHandler(evt) {
    def status = evt.value
    def doLog = false
    
    log.debug "Acceleration: ${status}"
    log.debug "Now: ${now()}"
    log.debug "Last Active: ${state.lastActive}"
    log.debug "Last Inactive: ${state.lastInactive}"
    
    // Wait at least a minute to log a status change.  Constant vibration
    // messes with the accelerometer and can cause spamming of status changes
    if(status == 'active' && (now() > state.lastInactive + state.delayLoggingMilliseconds))
    {
        doLog = true
        state.lastActive = now()
    }
    
    if(status == 'inactive' && (now() > state.lastActive + state.delayLoggingMilliseconds))
    {
        doLog = true
        state.lastInactive = now()
    }
    
    if(doLog)
    {
        logValue("Vibration", status)
    }
}

def temperatureHandler(evt) {
    def temp = evt.value
    
    log.debug "Temperature: ${temp}"
    
    logValue("Temperature", temp)
}

private def baseUrl() {
    return "https://script.google.com/macros/s/${urlKey}/exec?"
}

private logValue(measurement, value) {
    log.info "Logging to GoogleSheets: ${measurement} ${value}"
    
    def url = baseUrl() + "${measurement}=${value}"
    log.debug "URL: ${url}"
    
    def putParams = [
        uri: url
    ]

    httpGet(putParams) { response ->
        def httpResponseStatus = response.status
        
        log.debug("Response Status:${httpResponseStatus}")
        if (httpResponseStatus != 200) {
            log.error "Google logging failed, status = ${httpResponseStatus}"
        }
    }
}
