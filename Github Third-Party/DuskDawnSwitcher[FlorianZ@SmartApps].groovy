/**
 *  Dusk/Dawn Switcher with local computation of Sunrise/Sunset times.
 *
 *  Author: florianz
 *
 *  Date: 2013-12-14
 */
 
preferences {
    section ("At sunrise...") {
        input "sunriseOn", "capability.switch", title: "Turn on?", required: false, multiple: true
        input "sunriseOnLevel", "number", title: "On Level?", required: false
        input "sunriseOff", "capability.switch", title: "Turn off?", required: false, multiple: true
    }
    section ("At sunset...") {
        input "sunsetOn", "capability.switch", title: "Turn on?", required: false, multiple: true
        input "sunsetOnLevel", "number", title: "On Level?", required: false
        input "sunsetOff", "capability.switch", title: "Turn off?", required: false, multiple: true
    }
    section ("Sunrise offset (optional)...") {
        input "sunriseOffsetValue", "text", title: "HH:MM", required: false
        input "sunriseOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
    }
    section ("Sunset offset (optional)...") {
        input "sunsetOffsetValue", "text", title: "HH:MM", required: false
        input "sunsetOffsetDir", "enum", title: "Before or After", required: false, metadata: [values: ["Before","After"]]
    }
    section( "Notifications" ) {
        input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes", "No"]], required: false
        input "phoneNumber", "phone", title: "Send a text message?", required: false
    }

}

private mod(v, max) {
    while (v < 0) { v += max }
    v % max
}

private GetDayOfYear(Date date) {
    def year = date.getYear() + 1900
    def month = date.getMonth() + 1
    def day = date.getDate()
    
    def n1 = Math.floor(275 * month / 9)
    def n2 = Math.floor((month + 9) / 12)
    def n3 = (1 + Math.floor((year - 4 * Math.floor(year / 4) + 2) / 3))
    n1 - (n2 * n3) + day - 30
}

private GetRisingTime(Date date, longitude) {
    GetDayOfYear(date) + ((6 - (longitude / 15)) / 24)
}

private GetSettingTime(Date date, longitude) {
    GetDayOfYear(date) + ((18 - (longitude / 15)) / 24)
}

private GetSunLongitude(t) {
    def m = (0.9856 * t) - 3.289
    def l =
        m +
        (1.916 * Math.sin(Math.toRadians(m))) +
        (0.02 * Math.sin(Math.toRadians(2 * m))) +
        282.634
    mod(l, 360)
}

private GetSunRightAscension(sunLongitude) {
    def l = sunLongitude
    def a = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(l))))
    a = mod(a, 360)
    
    def lquadrant = Math.floor(l / 90) * 90
    def aquadrant = Math.floor(a / 90) * 90
    a = a + (lquadrant - aquadrant)
    
    a / 15
}

private GetSunHourAngle(sunLongitude, latitude) {
    def l = sunLongitude
    def sinDec = 0.39782 * Math.sin(Math.toRadians(l))
    def cosDec = Math.cos(Math.asin(sinDec))
    
    def latitudeRadians = Math.toRadians(latitude)
    def zenithAngle = Math.toRadians(90.5)
    def cosH = 
        (Math.cos(zenithAngle) - sinDec * Math.sin(latitudeRadians)) /
        (cosDec * Math.cos(latitudeRadians))
    
    // Sun never sets or rises at this location, on this date
    if (cosH > 1 || cosH < -1) {
        return null
    }
    
    Math.toDegrees(Math.acos(cosH))
}

private GetLocalMeanTime(t, ra, h) {
    h + ra - (0.06571 * t) - 6.622
}

private Date GetSunsetTime(Date date, latitude, longitude) {
    def t = GetSettingTime(date, longitude)
    def l = GetSunLongitude(t)
    def ra = GetSunRightAscension(l)
    def h = GetSunHourAngle(l, latitude) / 15
    def T = GetLocalMeanTime(t, ra, h)
    def utc = mod(T - (longitude / 15), 24)
    
    def hours = Math.floor(utc)
    def minutes = Math.floor((utc - hours) * 60)
    def seconds = Math.floor((utc - hours - (minutes / 60)) * 3600)
    
    return new Date(Date.UTC(
        date.getYear(), date.getMonth(), date.getDate(),
        hours as Integer, minutes as Integer, seconds as Integer))
}

private Date GetSunriseTime(Date date, latitude, longitude) {
    def t = GetRisingTime(date, longitude)
    def l = GetSunLongitude(t)
    def ra = GetSunRightAscension(l)
    def h = (360 - GetSunHourAngle(l, latitude)) / 15
    def T = GetLocalMeanTime(t, ra, h)
    def utc = mod(T - (longitude / 15), 24)
    
    def hours = Math.floor(utc)
    def minutes = Math.floor((utc - hours) * 60)
    def seconds = Math.floor((utc - hours - (minutes / 60)) * 3600)
    
    return new Date(Date.UTC(
        date.getYear(), date.getMonth(), date.getDate(),
        hours as Integer, minutes as Integer, seconds as Integer))
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def initialize() {
    astroCheck()
    schedule("0 1 * * * ?", astroCheck)
}

def astroCheck() {
    def now = new Date()
    def riseTime = GetSunriseTime(now, location.latitude, location.longitude)
    def setTime = GetSunsetTime(now, location.latitude, location.longitude)
    log.debug "nowTime: $now"
    log.debug "riseTime: $riseTime"
    log.debug "setTime: $setTime"

    state.riseTime = riseTime.time
    state.setTime = setTime.time
    
    unschedule("sunriseHandler")
    unschedule("sunsetHandler")
    
    if (riseTime.after(now)) {
        log.info "scheduling sunrise handler for $riseTime"
        runOnce(riseTime, sunriseHandler)
    }
    
    if (setTime.after(now)) {
        log.info "scheduling sunset handler for $setTime"
        runOnce(setTime, sunsetHandler)
    }
    
}

def sunriseHandler() {
    log.info "Executing sunrise handler"
    if (sunriseOn) {
        if (sunriseOnLevel == null) {
            sunriseOn.on()
        }
        else {
            sunriseOn?.setLevel(sunriseOnLevel)
        }
    }
    if (sunriseOff) {
        sunriseOff.off()
    }
    unschedule("sunriseHandler") // Temporary work-around for scheduling bug
    
    send("Sunrise")
}

def sunsetHandler() {
    log.info "Executing sunset handler"
    if (sunsetOn) {
        if (sunsetOnLevel == null) {
            sunsetOn.on()
        }
        else {
            sunsetOn?.setLevel(sunsetOnLevel)
        }
    }
    if (sunsetOff) {
        sunsetOff.off()
    }
    unschedule("sunsetHandler") // Temporary work-around for scheduling bug
    
    send("Sunset")
}

private send(msg) {
    if ( sendPushMessage == "Yes" ) {
        log.debug( "sending push message" )
        sendPush( msg )
    }

    if ( phoneNumber ) {
        log.debug( "sending text message" )
        sendSms( phoneNumber, msg )
    }

    log.debug msg
}

private getSunriseOffset() {
    sunriseOffsetValue ? (sunriseOffsetDir == "Before" ? "-$sunriseOffsetValue" : sunriseOffsetValue) : null
}

private getSunsetOffset() {
    sunsetOffsetValue ? (sunsetOffsetDir == "Before" ? "-$sunsetOffsetValue" : sunsetOffsetValue) : null
}

