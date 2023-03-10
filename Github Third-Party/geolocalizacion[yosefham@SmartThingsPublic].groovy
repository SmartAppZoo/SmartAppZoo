/**
 *  geolocalizacion
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
    name: "geolocalizacion",
    namespace: "IOT",
    author: "yusef",
    description: "te avisa cuando entras o sales de una determinada zona geografica",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
page (name: "home", title:"Sensores", nextPage: "Notificacion", install: false,uninstall: true){
	section("SENSOR"){
    	input "llegando", "text", required: false
        input "saliendo", "text", required: false
    	input "presence", "capability.presenceSensor", title: "Elige un sensor: ", required: true, multiple: false
    }
}
page(name:"Notificacion", title:"Modo de notificacion", uninstall:true)
} 

def Notificacion() {
	dynamicPage(name:"Notificacion",title:"Elige un modo de aviso"){
		section("AVISOS") {
		// TODO: put inputs here
            input "tipoNotificacion","enum",options: ["push","sms","ambos"], title:"Elige un tipo: ", submitOnChange:true
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
                    	if(llegando){
                        	sendNotificationToContacts(llegando, receptores)
                        }else{
 							sendNotificationToContacts("aqui estas jojojojojo", receptores)
                        }
				} else if (numero) {
                	if(llegando){
                    	sendSmsMessage(numero, llegando)
                    }else{
 						sendSmsMessage(numero, "aqui estas jejejeje")
                    }
				}
                if(llegando){
                	sendPushMessage (llegando)
                }else{
            		sendPushMessage "aqui estas xdxdxdxdxdxd"
                }
 		}else if (tipoNotificacion == "push" ){
        		if(llegando){
                	sendPushMessage (llegando)
                }else{
            		sendPushMessage "aqui estas jajajaja"
                }
       } else {
				if (location.contactBookEnabled) {
                    	if(llegando){
                        	sendNotificationToContacts(llegando, receptores)
                        }else{
 							sendNotificationToContacts("aqui estas jijijijiji", receptores)
                        }
				} else if (numero) {
                	if(llegando){
                    	sendSmsMessage(numero, llegando)
                    }else{
 						sendSmsMessage(numero, "aqui estas hahahahaha")
                    }
				}
        }
    }else{
    if ( tipoNotificacion == "ambos"){
 		if (location.contactBookEnabled) {
                    	if(saliendo){
                        	sendNotificationToContacts(saliendo, receptores)
                        }else{
 							sendNotificationToContacts("aqui estas jojojojojo", receptores)
                        }
				} else if (numero) {
                	if(saliendo){
                    	sendSmsMessage(numero, saliendo)
                    }else{
 						sendSmsMessage(numero, "aqui estas jejejeje")
                    }
				}
                if(saliendo){
                	sendPushMessage (saliendo)
                }else{
            		sendPushMessage "aqui estas xdxdxdxdxdxd"
                }
 		}else if (tipoNotificacion == "push" ){
        		if(saliendo){
                	sendPushMessage (saliendo)
                }else{
            	sendPushMessage "aqui estas jajajaja"
                }
       } else {
				if (location.contactBookEnabled) {
                    	if(saliendo){
                        	sendNotificationToContacts(saliendo, receptores)
                        }else{
 							sendNotificationToContacts("aqui estas jijijijiji", receptores)
                        }
				} else if (numero) {
                	if(saliendo){
                    	sendSmsMessage(numero, saliendo)
                    }else{
 						sendSmsMessage(numero, "aqui estas hahahahaha")
                    }
				}
        }
    
    }
}