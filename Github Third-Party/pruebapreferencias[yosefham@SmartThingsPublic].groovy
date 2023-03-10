/**
 *  pruebapreferencias
 *
 *  Copyright 2016 yusef
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
    name: "pruebapreferencias",
    namespace: "IOT",
    author: "yusef",
    description: "prueba las formas de paginas",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
page(name:"micasa", title:"Una sola pagina dinamica", uninstall:true)
} 

def micasa() {
	dynamicPage(name:"micasa",title:" a ver lo que sale"){
		section("Title") {
		// TODO: put inputs here
            input "tipoNotificacion","enum",options: ["push","sms","ambos"], title:"Tipo?", submitOnChange:true
    	}
         if ( tipoNotificacion == "ambos" || tipoNotificacion == "sms"){
 			section (){
            	input ("receptores", "contact", title: "enviar sms a ") {
                	input ("numero", "phone", title: "pon un numero: ")
                }
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
	subscribe (presence, "presence", localizacion)
	// TODO: subscribe to attributes, devices, locations, etc.
}
def localizacion (evt){
	if ("present" == evt.value){
   		if ( tipoNotificacion == "ambos"){
 			
            		if (location.contactBookEnabled) {
 					sendNotificationToContacts("aqui estas jojojojojo", receptores)
				} else if (numero) { 
 					sendSmsMessage(numero, "aqui estas jejejeje")
				}
            	sendPushMessage "aqui estas xdxdxdxdxdxd"
 		}else if (tipoNotificacion == "push" ){
            	sendPushMessage "aqui estas jajajaja"
       } else {
				if (location.contactBookEnabled) {
 					sendNotificationToContacts("aqui estas jijijiji", receptores)
				} else if (numero) { 
 					sendSmsMessage(numero, "aqui estas hahahaha")
				}
        }
    }else{
    if ( tipoNotificacion == "ambos"){
 			
            		if (location.contactBookEnabled) {
 					sendNotificationToContacts("aqui faltas jojojojojo", receptores)
				} else if (numero) { 
 					sendSmsMessage(numero, "aqui faltas jejejeje")
				}
            	sendPushMessage "aqui faltas xdxdxdxdxdxd"
 		}else if (tipoNotificacion == "push" ){
            	sendPushMessage "aqui faltas jajajaja"
       } else {
				if (location.contactBookEnabled) {
 					sendNotificationToContacts("aqui faltas jijijiji", receptores)
				} else if (numero) { 
 					sendSmsMessage(numero, "aqui faltas hahahaha")
				}
        }
    
    }
}