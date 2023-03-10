definition(
    name: "Periodical Switch",
    namespace: "hydroponics",
    author: "zeande",
    description: "Manages power state to the specified switch given a specified start and stop time as well as power-off settings.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png"
)

preferences {
    section("Choose an icon") {
        icon(title: "Icon", required: false)
    }
    section("Trigger this switch...") {
        input "switch1", "capability.switch", title: "Which switch?", multiple: true, required: true
    }
    
    section("Start and stop times") {
        input "start", "time", title: "Start time?", required: true
        input "stop", "time", title: "Stop time?", required: true
    }

    section("On/Off cycle") {
        input "durationOn", "number", title: "On duration (minutes)?", required: false, range: "0..480"
        input "durationOff", "number", title: "Off duration (minutes)?", required: false, range: "0..480" 
    }
}

def installed()
{
    handler()
}

def updated()
{
    handler()
}

def handler()
{
    unschedule()
    
    schedule(start, startDay)
    schedule(stop, stopDay)
    
    timeOfDayIsBetween(start, stop, new Date(), location.timeZone) ? startDay() : stopDay()
}

def startDay()
{
    unschedule(toggle)
    flipSwitch(true)
    if (durationOn > 0)
    {
        runIn(durationOn * 60, toggle)
    }
}

def stopDay()
{
    unschedule(toggle)
    flipSwitch(false)
}

def toggle()
{
    flipSwitch(!state.motion)
    runIn((state.motion ? durationOn : durationOff) * 60, toggle)
}

def flipSwitch(turnOn)
{
    state.motion = turnOn
    turnOn ? switch1.on() : switch1.off()
}