/**
 *  ScheduleTest
 *
 *  Copyright 2015 Jacob Richard
 *
 */
definition(
    name: "ScheduleTest",
    namespace: "jrichard",
    author: "Jacob Richard",
    description: "Foo",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {}


def installed() {
  schedule("* * * * * ?", "scheduleCheck")
}

def updated() {
  unschedule()
  schedule("* * * * * ?", "scheduleCheck")
}

def uninstalled() {
  unschedule()
}

def scheduleCheck() {
  log.debug "Running Method"
}


