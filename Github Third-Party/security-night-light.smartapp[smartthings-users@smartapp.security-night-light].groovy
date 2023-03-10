/**
 *  Clever Night Light
 *
 *  Author: Brian Steere
 */

 preferences {

    section("Turn on") {
        input "switches", "capability.switch", title: "Things", multiple: true
    }

    section("Until:") {
        input "onUntil", "time", title: "Leave on until"
    }

    section("Turn on when there's movement..."){
        input "motion1", "capability.motionSensor", title: "Where?"
        input "motionOnTime", "number", title: "Leave on for how long (minutes)"
    }
}


def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()

    initialize()
}

def initialize() {
	log.debug "Settings: $settings"
    
	schedule(onUntil, modeStopThings)
    schedule("1 5 0 * * ?", setupSchedule)
    
    subscribe(motion1, "motion", motionHandler)
    setupSchedule()
}

def setupSchedule() {
    def now = new Date()
    def times = getSunriseAndSunset()
    def sunrise = times.sunrise
    def sunset = times.sunset
    
    log.debug "Rise: $sunrise | Set: $sunset | Now: $now"
    
    def offTime = timeToday(onUntil)
    
    if(now > sunset) {
    	if(now < offTime) {
            log.debug "Before off time: $offTime"
            modeStartThings()
        }
    } else {
    	log.debug "Scheduling start: ${sunset}"
    	runOnce(sunset, modeStartThings)
    }
}

def motionHandler(evt) {    
    if(!state.modeStarted) {
        def now = new Date()
        def sunrise = GetSunriseTime(now, location.latitude, location.longitude)
        def sunset = GetSunsetTime(now, location.latitude, location.longitude)
        
        if (evt.value == "active" && (now > sunset || now < sunrise)) {
            log.debug "Saw Motion"

            // Unschedule the stopping of things
            unschedule(motionStopThings)

            startThings()
            state.motionStarted = true;
        }
        else if (evt.value == "inactive") {
            log.debug "No More Motion"
            runIn(motionOnTime * 60, motionStopThings)
        }
    }
}

def modeStartThings() {
    log.debug "Mode starting things"
    state.modeStarted = true
    startThings()
}

def modeStopThings() {
    log.debug "Mode stopping things"
    state.modeStarted = false
    stopThings()
}

def motionStopThings() {
    stopThings()
    state.motionStarted = false
}

def startThings() {
    log.debug "Starting things"
    state.thingsOn = true
    switches.on()
}

def stopThings() {
    log.debug "Stopping things"
    switches.off()
    state.thingsOn = false
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
