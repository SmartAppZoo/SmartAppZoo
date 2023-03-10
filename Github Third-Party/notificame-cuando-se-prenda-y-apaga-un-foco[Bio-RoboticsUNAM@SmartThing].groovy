/**
 * Este programa manda notificaciones al Sistema Operativo donde se encuentre instalada la SmartApp de Smartthings
 *
 * 
 * Para esta APP vamos a utilisar el Arduino ThingShield, previamente configurado con el programa On/off sensor magnetic. groovy
 * Y vamos a instalar On/off sensor magnetic.ino, instalado previamente en el arduino.
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  Notificame cuando se prende y se agaga un foco
 *
 *  Author: 
 *		 Dr. Savage Carmona Jesus
 * 			Rocha Resendiz Irving
 * 			Bio-Robotics UNAM
 *  Date: 2016-06-20
 *
 */
definition(
		name: "Notificame Cuando se prenda y apaga un foco",
		namespace: "biorobotics",
		author: "bioRobotics",
		description: "Recibe notificaciones cuando se prende y apaga un foco (Rele)",
		category: "Convenience",
		iconUrl: "http://metalyluz.com/wp-content/uploads/2014/06/metalyluz.png",
		iconX2Url: "http://metalyluz.com/wp-content/uploads/2014/06/metalyluz.png"
)

preferences {
	section("Seleccione uno: "){
		input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
		input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true

	}
	section("Envia este mensaje (opcional, enviar un mensaje predefinido)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
		input("recipients", "contact", title: "Send notifications to") {
			input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
			paragraph "If outside the US please make sure to enter the proper country code"
			input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes", "No"]
		}
	}
	section("Minutos entre cada notificacion (opcional, defaults para cada mensaje)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {

    subscribe(mySwitch, "switch.on", eventHandler)
	subscribe(mySwitchOff, "switch.off", eventHandler)

}

def eventHandler(evt) {
	log.debug "Notify got evt ${evt}"
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
			sendMessage(evt)
		}
	}
	else {
		sendMessage(evt)
	}
}

private sendMessage(evt) {
	String msg = messageText
	Map options = [:]

	if (!messageText) {
		msg = defaultText(evt)
		options = [translatable: true, triggerEvent: evt]
	}
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients, options)
	} else {
		if (!phone || pushAndPhone != 'No') {
			log.debug 'sending push'
			options.method = 'push'
			sendPush(msg)
		}
		if (phone) {
			options.phone = phone
			log.debug 'sending SMS'
			sendSms(phone, msg)
		}
		sendNotification(msg, options)
	}

	if (frequency) {
		state[evt.deviceId] = now()
	}
}

private defaultText(evt) {
	if (evt.name == 'presence') {
		if (evt.value == 'present') {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has arrived at the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has arrived at {{ location.name }}'
			}
		} else {
			if (includeArticle) {
				'{{ triggerEvent.linkText }} has left the {{ location.name }}'
			}
			else {
				'{{ triggerEvent.linkText }} has left {{ location.name }}'
			}
		}
	} else {
		'{{ triggerEvent.descriptionText }}'
	}
}

private getIncludeArticle() {
	def name = location.name.toLowerCase()
	def segs = name.split(" ")
	!(["work","home"].contains(name) || (segs.size() > 1 && (["the","my","a","an"].contains(segs[0]) || segs[0].endsWith("'s"))))
}