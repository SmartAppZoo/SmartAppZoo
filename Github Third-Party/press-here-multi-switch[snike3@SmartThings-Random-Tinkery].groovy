/**
 *  Press Here Multi-Switch
 *
 *  Copyright 2016 Tim Polehna
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
    name: "Press Here Multi-Switch",
    namespace: "polehna",
    author: "Tim Polehna",
    description: "This app allows you to use redundant \"off\" and/or \"on\" switch presses to control other lights. Based on Double Duty by Pasquale Ranalli",  
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences 
{
    section("Master switch") 
    {
        paragraph "NOTE: Plain on/off switches are preferable to dimmers.  Dimmers may trigger unexpected toggles when turned off or dimmed to 0 (zero)."
        input "master", "capability.switch", title: "Select", required: true
        input "tapCount", "enum", title: "Presses to activate", required: true, options: ["Single", "Double"], defaultValue: "Single"
    }

    section("Redundant ON presses set") 
    {
        input "onSlavesToOn", "capability.switch", multiple: true, required: false, title: "Switches to ON"
        input "onSlavesToOff", "capability.switch", multiple: true, required: false, title: "Switches to OFF"
        input "onToggle", "capability.switch", multiple: true, required: false, title: "Switches to Toggle"
        
        //input "onIncludesWemo", "enum", title: "Toggle Includes WeMo Switch", multiple: false, required: true,
		//		options: ["No", "Yes"], defaultValue: "No"
    }
    
    section("Redundant OFF presses set") 
    {
        input "offSlavesToOn", "capability.switch", multiple: true, required: false, title: "Switches to ON"
        input "offSlavesToOff", "capability.switch", multiple: true, required: false, title: "Switches to OFF"
        input "offToggle", "capability.switch", multiple: true, required: false, title: "Switches to Toggle"
        
        //input "offIncludesWemo", "enum", title: "Toggle Includes WeMo Switch", multiple: false, required: true,
		//		options: ["No", "Yes"], defaultValue: "No"
    }
}

def installed()
{
	state.pressTime = 0
    state.lastToggle = false
    
    subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def updated()
{
	state.pressTime = 0
    state.lastToggle = false
    
    unsubscribe()
    subscribe(master, "switch", switchHandler, [filterEvents: false])
}

def switchHandler(evt) 
{
    if (evt.isPhysical())
    {
        boolean isStateChange = evt.isStateChange()
        boolean isSingleTap = (tapCount == "Single")
        log.debug "Master Switch Changed State: ${isStateChange}"

		if (isSingleTap)
        {
        	if (!isStateChange)
        	{
				switchAction()
        	}
        }
        else
        {
        	def currentTime = now()
            def lastTime = state.pressTime
            
        	// can't be double if it was a state change
            if (isStateChange)
            {
                state.pressTime = currentTime
            }
            // first run
            else if (!lastTime || (lastTime == 0))
            {
                state.pressTime = currentTime
            }
            // 2 second timeout for presses
            else if ((currentTime - lastTime) > 2000)
            {
            	log.debug "Double Tap timeout, restarting detection"
                state.pressTime = currentTime
            }
            else
            {
                switchAction()
            }
        }
    }	
}

private switchAction()
{
    def ss = master.latestState("switch").value
    log.debug "Master Switch Latest State: ${ss}"

    def toToggle

    // WeMo switches require a refresh before checking their state because they do
    // not always have the correct state in SmartThings.
    // This significantly slows down the response of the toggle action but makes it
    // much more reliable.

    if (ss == "on")
    {
        onSlavesToOn*.on()
        onSlavesToOff*.off()
        toToggle = onToggle

		/*
        if (onIncludesWeMo == "Yes")
        {
        	onToggle*.refresh()
        }
        */
    }
    else
    {
        offSlavesToOn*.on()
        offSlavesToOff*.off()
        toToggle = offToggle

		/*
        if (offIncludesWeMo == "Yes")
        {
        	offToggle*.refresh()
        }
        */
    }

    if (toToggle)
    {   
        toggleSwitches(toToggle)
    }
}

private getSwitchesState(switches)
{
	return switches.every { it.latestState("switch").value == "off" }
}

private toggleSwitches(switches)
{   
    boolean turnOn = getSwitchesState(switches)
    
    // So this will cause a delay if the user toggled the other switch state separately
    // from this SmartApp, but it shouldn't be any worse of a penalty than just doing a
    // refresh every time we toggle. The benefit is that we don't ALWAYS have that hit...
    if (turnOn == state.lastToggle)
    {
    	switches*.refresh()
        turnOn = getSwitchesState(switches)
    }
    
    def value = turnOn ? "on" : "off"
    log.debug "Sending \"${value}\" command"
    
    state.lastToggle = turnOn
    turnOn ? switches*.on() : switches*.off()
    
    // paranoid about the switch not actually toggling? turn on the following code...
    /*
    switches.each 
    {
        // Limit the amount of network traffic by only sending the command to switches
        // that are not in the correct state
        if (it.latestState("switch").value != value)
        {
            turnOn ? it.on() : it.off()
            
            // WeMo switches sometimes will not change, and a refresh will fix the situation
            if (it.latestState("switch").value != value)
            {
                it?.refresh()
                turnOn ? it.on() : it.off()
            }
        }
    }
    */
}
