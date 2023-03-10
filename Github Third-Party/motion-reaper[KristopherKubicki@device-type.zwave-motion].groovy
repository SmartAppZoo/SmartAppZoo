/*
 *  Motion Monitor - SmartThings sometimes misses a reset on motion sensors.  
 *     This app just monitors the sensor and accordingly resets the motion every X minutes.
 */

definition(
    name: "Motion Reaper",
    namespace: "KristopherKubicki",
    author: "Kristopher Kubicki",
    description: "Monitors specified motion sensors for stuck states and updates them.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("Reap these devices...") {
        input "reap_group", "capability.refresh", title:"Select devices to be reaped", multiple:true, required:true
    }
    section("Every...") {
        input "reap_interval", "number", title:"Set reaping interval (in minutes)", defaultValue:5
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    initialize()
}

def reapTask() {
    for ( reaped in settings.reap_group) { 
     def last = reaped.latestValue("motion")
        if(reaped.latestValue("motion") == "active") { 
            reaped.refresh()
        }
    }
}


private def initialize() {
    log.debug("initialize() with settings: ${settings}")
 
    def minutes = settings."reap_interval".toInteger()
    if (minutes > 0) {
       log.debug("Scheduling reaping task to run every ${minutes} minutes.")
       def sched = "0 0/${minutes} * * * ?"
       schedule(sched, reapTask)
    }
}
