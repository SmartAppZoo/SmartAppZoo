/**
 *  CondorStatusParent
 *
 *  Copyright 2014 CaesarsGhost
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
    name: "CondorStatusParent",
    namespace: "CaesarsGhost",
    author: "CaesarsGhost",
    description: "Handles the virtual devices for the CondorShield",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Zones") {
		input "zone1", "device.virtualContact", title:"Zone 1"
        input "zone2", "device.virtualContact", title:"Zone 2"
        input "zone3", "device.virtualContact", title:"Zone 3"
        input "zone4", "device.virtualContact", title:"Zone 4"
        input "zone5", "device.virtualContact", title:"Zone 5"
        input "zone6", "device.virtualContact", title:"Zone 6"
	}
	section("Which Arduino Is your Condor Interface?") {
		input "condor", "device.condorShield"
	}    
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {

	for(int i=1; i<=6; i++ )
    {
		subscribe( condor, "zone${i}.open", zoneOpen )
    	subscribe( condor, "zone${i}.closed", zoneClose )
    }
}

def zoneOpen(evt)
{
	log.debug "Setting Device Open"   
    switch( evt.name )
    {
        case "zone1":
        zone1.open( "Zone 1" )
        break;
        case "zone2":
        zone2.open( "Zone 2" )
        break;
        case "zone3":
        zone3.open( "Zone 3" )
        break;
        case "zone4":
        zone4.open( "Zone 4" )
        break;
        case "zone5":
        zone5.open( "Zone 5" )
        break;
        case "zone6":
        zone6.open( "Zone 6" )
        break;        
    };
}

def zoneClose(evt)
{
	log.debug "Setting Device Closed"
    switch( evt.name )
    {
        case "zone1":
        zone1.closed( "Zone 1" )
        break;
        case "zone2":
        zone2.closed( "Zone 2" )
        break;
        case "zone3":
        zone3.closed( "Zone 3" )
        break;
        case "zone4":
        zone4.closed( "Zone 4" )
        break;
        case "zone5":
        zone5.closed( "Zone 5" )
        break;
        case "zone6":
        zone6.closed( "Zone 6" )
        break;        
    };
}