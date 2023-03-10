/**
 *  Door Lock Entry Light
 *
 *  Copyright 2015 Jacob Richard
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
    name: "Door Lock Entry Light",
    namespace: "jrichard",
    author: "Jacob Richard",
    description: "Turn the entry light on when door unlocks",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("When the door opens..."){
        input "lock1", "capability.lock", title: "Which Door"
    }

    section("Turn on a Dimmable Light"){
        input "switches", "capability.switchLevel", 
            multiple: true, 
            title: "Which Light", 
            required: true
    }
}

def installed()
{
    subscribe(lock1, "lock.unlocked", contactOpenHandler)
    subscribe(lock1, "lock.locked", contactClosedHandler)
}

def updated()
{
    unsubscribe()
    subscribe(lock1, "lock.unlocked", contactOpenHandler)
    subscribe(lock1, "lock.locked", contactClosedHandler)
}

def contactOpenHandler(evt) {
    switches.setLevel(10)
    switches.on()
    switches.off(delay: 120000)
}

def contactClosedHandler(evt) {
    switches.off()
}

