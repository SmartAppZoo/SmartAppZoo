/*
 * virtual-presence-controller.groovy
 *
 * Copyright (C) 2010 Antoine Mercadal <antoine.mercadal@inframonde.eu>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

definition(
    name: "Virtual Presence Controller",
    namespace: "primalmotion",
    author: "primalmotion",
    description: "Allow to put multiple presence sensors in a virtual presence device",
    category: "Convenience",
    iconUrl: "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/96/people-icon.png",
    iconX2Url: "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/96/people-icon.png"
)

preferences
{
    section
    {
        input "virtualpresencedevice", "capability.presenceSensor", title: "Select Virtual Presence Device", required: true
        input "people", "capability.presenceSensor", title: "Select People", multiple: true, required: false
        input "trigger", "capability.switch", title: "Select Button", multiple: false, required: false
    }
}

def installed()
{
    initialize()
}

def updated()
{
    unsubscribe()
    initialize()
}

def initialize()
{
    subscribe(people, "presence", presence)
    subscribe(trigger, "switch", on_switch)
}

def on_switch(evt)
{
	set_virtual_presence_state(trigger.currentSwitch == "on")
}

def presence(evt)
{
    set_virtual_presence_state(check_people_present())
}

private check_people_present()
{
    def result = false

    if(people.findAll { it?.currentPresence == "present" })
        result = true

    return result
}

private set_virtual_presence_state(present)
{
    if (present)
        virtualpresencedevice.present()
    else
        virtualpresencedevice.away()

}