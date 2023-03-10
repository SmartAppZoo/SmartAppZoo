/**
 *  Toggles the specified switch every n minutes where n is a specified period between 1 and 60.
 *  Note: This is really just a specific case of the periodical switch that has equal on and off
 *  durations. Keeping it here for simplicity.
 */
definition(
    name: "Switch Cycle",
    namespace: "hydroponics",
    author: "zeande",
    description: "Cycles simulated switch periodically.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png"
)

preferences {
    section("Trigger this switch...") {
        input "switch1", "capability.switch", title: "Which switch?", multiple: true, required: true
    }
    
    section("Cycle duration...") {
    	input "period", "number", title: "Period in minutes?", required: true, range:"1..59"
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
    switch1.off()
    state.active = false

    unschedule(toggle)
    schedule("0 0/${period} * * * ?", toggle)
}

def toggle()
{
    if (state.motion) {
        state.motion = false
        switch1.off()
    } else {
        state.motion = true
        switch1.on()
    }
}
