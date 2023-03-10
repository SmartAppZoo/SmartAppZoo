/**
 *
 *	Copyright 2016 Christian Madden
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *			http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
definition(
	name: "HomeAPI",
	namespace: "christianmadden",
	author: "Christian Madden",
	description: "Endpoints for SmartThings integtration with HomeAPI.",
	category: "",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
	oauth: [displayName: "HomeAPI", displayLink: "http://christianmadden.ddns.net"])

preferences{}

mappings {
	path("/routine/:name") { action: [POST: "executeRoutine"] }
}

void executeRoutine()
{
	log.debug("EXEC_ROUTINE")
	log.debug(params.name)

	def last_routine = state.last_routine
	if(last_routine && last_routine == params.name){ return }

	def routines =
	[
		[ name: "morning", phrase: "Morning" ],
		[ name: "morning-away", phrase: "Morning, Away" ],
		[ name: "daytime", phrase: "Daytime" ],
		[ name: "daytime-away", phrase: "Daytime, Away" ],
		[ name: "night", phrase: "Night" ],
		[ name: "night-away", phrase: "Night, Away" ],
		[ name: "late-night", phrase: "Late Night" ],
		[ name: "late-night-away", phrase: "Late Night, Away" ],
		[ name: "sleepy", phrase: "Sleepy" ],
		[ name: "bedtime", phrase: "Bedtime" ],
		[ name: "tivo", phrase: "TiVo" ],
		[ name: "appletv", phrase: "Apple TV" ],
		[ name: "playstation", phrase: "Playstation" ],
		[ name: "xbox", phrase: "Xbox" ],
		[ name: "turntable", phrase: "Turntable" ],
		[ name: "shutdown", phrase: "Shutdown" ]
	]

	if(params.name)
	{
		def routine = routines.find { it.name == params.name }
		if(routine)
		{
			location.helloHome.execute(routine.phrase)
			state.last_routine = routine.name
		}
	}
}

def installed(){}
def updated(){}
