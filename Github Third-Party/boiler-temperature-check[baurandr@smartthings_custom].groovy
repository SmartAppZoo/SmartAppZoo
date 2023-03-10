/**
 *  Author: Baur
 */

definition(
    name: "Boiler Temperature Check",
    namespace: "baurandr",
    author: "A. Baur",
    description: "Monitor Boiler Temps and alert if they are out of range",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Notify if temperatures are below this value."){
		input "temperature1", "number", title: "Degrees F"
	}
	section("Notify if there hasn't been a temperature reading within this many minutes"){
		input "minutes1", "number", title: "Minutes"
	}
    section("Send a text message to this number (optional)") {
        input "phone", "phone", required: false
    }
}

def installed() {
	checkTemperatures()
    runEvery15Minutes(checkTemperatures)
}

def updated() {
	checkTemperatures()
    runEvery15Minutes(checkTemperatures)
}

def checkTemperatures(){
    def params = [uri: "http://baurfam.com/exportBoilerTemps"]
    try {
        httpGet(params) { resp ->
        /*    resp.headers.each {
            log.debug "${it.name} : ${it.value}"
        }
        log.debug "response contentType: ${resp.contentType}"
        log.debug "response data: ${resp.data}"*/
        def maxBoilerOut = resp.data.maxBoilerOut
        def maxBoilerIn = resp.data.maxBoilerIn
        def lastTempReadTime = resp.data.lastTempReadTime
                //2017-10-13 15:33:52 format from the website
        //def newdate = new Date().parse("YYYY-MM-DD HH:mm:ss", theDate)
		long lastReadTime = new Date().parse("yyy-MM-dd HH:mm:ss", lastTempReadTime).getTime()
		//long starttime_unix = new Date().parse("yyy-MM-dd'T'HH:mm:ss.SSSZ", starttime).getTime() / 1000

		def curTime = new Date()
        def timeZone = location.getTimeZone()
        def dST = timeZone.inDaylightTime(curTime)
        def dstOffset = 0;
			if (dST){
            	dstOffset = 60*60*1000;
            }
		long curTimeMs = curTime.getTime() + location.timeZone.rawOffset + dstOffset
        def elapsed = curTimeMs - lastReadTime
        def threshold = 1000 * 60 * minutes1

        if (elapsed > threshold){
			sendSms(phone, "No recent boiler temp readings")
        }
        def dblVal = Math.round(Double.parseDouble(maxBoilerOut))
		def intmaxBoilerOut = dblVal.toInteger()

        dblVal = Math.round(Double.parseDouble(maxBoilerIn))
		def intmaxBoilerIn = dblVal.toInteger()

        if ((intmaxBoilerOut < temperature1) && (intmaxBoilerIn < temperature1)){
			sendSms(phone, "Boiler temps low - BoilerOut: ${maxBoilerOut} BoilerIn: ${maxBoilerIn}")
        }
        
		//log.debug "TZOffset: ${location.timeZone.rawOffset}, dstOffset: ${dstOffset}, dst: ${dST}"
		log.debug "maxBoilerOut: ${maxBoilerOut}, maxBoilerIn: ${maxBoilerIn}, Temp Limit: ${temperature1}"
        //log.debug "Now:  ${curTime}, Last reading:  ${lastReadTime}, time since last reading: ${elapsed}"
        //log.debug "Now:  ${curTimeMs}, Last reading:  ${lastReadTime}, time since last reading: ${elapsed} ms"
        log.debug "Time since last reading: ${elapsed} ms"
        
        }
    }
	catch (e) {
        log.error "Something went wrong with boiler HTTP Get: $e"
        sendSms(phone, "Something went wrong with boiler HTTP Get: ${e}")
    }
}