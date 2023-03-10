/**
 *  Avisame cuando...
 *
 *  Copyright 2018 Jose Carlos Sanchez
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
    name: "Avisame cuando...",
    namespace: "hoskbreaker",
    author: "Jose Carlos Sanchez",
    description: "Aplicación que notifica cuando has entrado y has salido de un rango marcado. Se puede configurar la aplicación para que notifique en la noche, cuando llegas a casa o cuando sales de ella",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	page(name: "PaginaEstatica", title: "Página estática", nextPage: "PaginaDinamica", install: false, uninstall: true){
    	section(){
        	input ("SensorPresencia", "capability.presenceSensor", title: "Selecciona el sensor de presencia", multiple: false)
        	input ("tipoNotificacion","enum", options: ["push","sms", "ambos"],title: "Tipo?")
            input("TXTIN","text",title: "Introduzca el mensaje al entrar")
            input("TXTOUT", "text", title: "Introduzca el mensaje al salir")
            input("HubMode", "mode", title: "Modo de recepcion de notificaciones: ")
            input("HIN","time",title:"hora inicial: ")
            input("HOUT","time", title: "hora final: ");
            
        }
    }
	page(name: "PaginaDinamica", title: "Página dinámica", prevPage: "PaginaDinamica", install: true, uninstall: true)
}

def PaginaDinamica(){
	dynamicPage(name:"PaginaDinamica"){
    	if(tipoNotificacion == "sms" || tipoNotificacion == "ambos"|| tipoNotificacion == "1" || tipoNotificacion == "2"){
        	section(){
            	input ("receptores", "phone", title: "Introduce el teléfono: ");
            }
        }
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
	subscribe(SensorPresencia,"Presence",PresHandler)
}

// TODO: implement event handlers
def PresHandler(evt){ 
	log.debug "Sensor de presencia: $evt.value"
	if((evt.value == "present") && timeOfDayIsBetween(settings.HIN, settings.HOUT, evt.date, location.timeZone) && (settings.HubMode == location.mode)){
        //enviar mensaje
        log.debug( "mensaje de entrada: $settings.TXTIN")
        if(settings.tipoNotificacion == "0" || settings.tipoNotificacion == "push"){
        	sendPush(settings.TXTIN);
        }else if (settings.tipoNotificacion == "1"|| settings.tipoNotificacion == "sms"){
            sendSms(settings.receptores,settings.TXTIN)
        }else{
        	sendPush(settings.TXTIN)
            sendSms(settings.receptores,settings.TXTIN)
        }
    }else if ((evt.value == "not present") && timeOfDayIsBetween(settings.HIN, settings.HOUT, evt.date, location.timeZone) && (settings.HubMode == location.mode)){
    	//enviar mensaje
        log.debug( "mensaje de salida: $settings.TXTOUT")
        if(settings.tipoNotificacion == "0" || settings.tipoNotificacion == "push"){
        	sendPush(settings.TXTOUT);
        }else if (settings.tipoNotificacion == "1"|| settings.tipoNotificacion == "sms"){
            sendSms(settings.receptores,settings.TXTOUT)
        }else{
        	sendPush(settings.TXTOUT)
            sendSms(settings.receptores,settings.TXTOUT)
        }
    }
}
