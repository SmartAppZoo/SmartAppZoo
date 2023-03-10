// vim: ts=4:sw=4
/**
 *	Set Input Source
 *
 *	Copyright 2020 Ross Tyler
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 */
definition(
	name			: 'Set Input Source',
	namespace		: 'rtyle',
	author			: 'Ross Tyler',
	description		: 'Set the input source of targets when triggered',
	category		: 'Convenience',
	singleInstance	: false,
	iconUrl			: 'https://raw.githubusercontent.com/rtyle/set-input-source/master/smartapps/rtyle/set-input-source.src/app.png',
	iconX2Url		: 'https://raw.githubusercontent.com/rtyle/set-input-source/master/smartapps/rtyle/set-input-source.src/app@2x.png',
	iconX3Url		: 'https://raw.githubusercontent.com/rtyle/set-input-source/master/smartapps/rtyle/set-input-source.src/app@3x.png',
)

preferences {
	section('Targets') {
		input 'switchTargets'			, 'capability.switch'			, title: 'Switch targets (turned on when triggered)'		, multiple: true, required: false
		input 'turnOffToo'				, 'bool'						, title: 'Turn off too'
		input 'mediaInputSourceTargets'	, 'capability.mediaInputSource'	, title: 'Media input source targets (set when triggered)'	, multiple: true, required: false
		input 'inputSource'				, 'text'						, title: 'Input source'														, required: false
	}
	section('Triggers') {
		input 'switchTriggers'			, 'capability.switch'			, title: 'Switch triggers (when turned on)'				, multiple: true,	required: false
		input 'contactSensorTriggers'	, 'capability.contactSensor'	, title: 'Contact Sensor triggers (when closed)'		, multiple: true,	required: false
		input 'mediaPlaybackTriggers'	, 'capability.mediaPlayback'	, title: 'Media Playback triggers (when starts playing)', multiple: true,	required: false
	}
}

private void respond(String message) {
	log.info message
	if (false
		|| switchTriggers		.find {'on'			== it.currentSwitch}
		|| contactSensorTriggers.find {'closed'		== it.currentContact}
		|| mediaPlaybackTriggers.find {'stopped'	!= it.currentPlaybackStatus}
	) {
		if (switchTargets) {
			log.info "turn on $switchTargets"
			switchTargets.on()
		}
		if (mediaInputSourceTargets && inputSource) {
			log.info "setInputSource to $inputSource on $mediaInputSourceTargets"
			mediaInputSourceTargets.setInputSource inputSource
		}
	} else {
		if (turnOffToo && switchTargets) {
			log.info "turn off $switchTargets"
			switchTargets.off()
		}
}
}

def getIndent() {/* non-breaking space */ '\u00a0' * 8}

void respondToSwitchOn(physicalgraph.app.EventWrapper e) {
	respond(indent + "⏻ $e.value $e.name $e.device")
}
void respondToSwitchOff(physicalgraph.app.EventWrapper e) {
	respond(indent + "⭘ $e.value $e.name $e.device")
}

void respondToContactClosed(physicalgraph.app.EventWrapper e) {
	respond(indent + "☒ $e.value $e.name $e.device")
}
void respondToContactOpen(physicalgraph.app.EventWrapper e) {
	respond(indent + "☐ $e.value $e.name $e.device")
}

void respondToPlaybackStatusPlaying(physicalgraph.app.EventWrapper e) {
	respond(indent + "⏵ $e.value $e.name $e.device") //
}
void respondToPlaybackStatusPaused(physicalgraph.app.EventWrapper e) {
	respond(indent + "⏸ $e.value $e.name $e.device") //
}
void respondToPlaybackStatusStopped(physicalgraph.app.EventWrapper e) {
	respond(indent + "⏹ $e.value $e.name $e.device") //
}

private void initialize() {
	subscribe switchTriggers		, 'switch.on'				, respondToSwitchOn
	subscribe contactSensorTriggers	, 'contact.closed'			, respondToContactClosed
	subscribe mediaPlaybackTriggers	, 'playbackStatus.playing'	, respondToPlaybackStatusPlaying
	subscribe mediaPlaybackTriggers	, 'playbackStatus.paused'	, respondToPlaybackStatusPaused
	if (turnOffToo) {
	subscribe switchTriggers		, 'switch.off'				, respondToSwitchOff
	subscribe contactSensorTriggers	, 'contact.open'			, respondToContactOpen
	subscribe mediaPlaybackTriggers	, 'playbackStatus.stopped'	, respondToPlaybackStatusStopped
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}
