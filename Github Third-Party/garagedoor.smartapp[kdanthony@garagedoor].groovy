/* Garage Door Opener

Allows a Particle Photon to return back state of a garage door using
an IR sensor to detect the door position.

Copyright (c) 2015, Kevin Anthony (kevin@anthonynet.org)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

definition(
    name: "Garage Door Opener",
    namespace: "kdanthony",
    author: "Kevin Anthony",
    description: "Return state of garage door by IR sensor.",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Transportation/transportation12-icn@2x.png",
    oauth: true
)

preferences {
    section("When the garage door switch is turned on, open the garage door...") {
        input "garagedoor", "capability.contactSensor"
    }
}

def installed() {
    subscribe(app, appTouchHandler)
}

def updated() {
    unsubscribe()
    subscribe(app, appTouchHandler)
}

def appTouch(evt) {
    log.debug "appTouch: $evt.value, $evt"
}

def onCommand(evt) {
    log.debug "onCommand: $evt.value, $evt"
}

mappings {
  path("/doorstate/:doorstate") {
    action: [
      PUT: "updateDoorState"
    ]
  }
}

def updateDoorState() {
  log.debug "update: " + params.doorstate
  garagedoor.setDoorState( params.doorstate )
}
