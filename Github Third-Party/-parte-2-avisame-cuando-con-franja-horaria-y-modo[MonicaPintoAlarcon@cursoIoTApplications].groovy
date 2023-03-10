/**
 *  Avisame cuando con franja horaria y modo...
 *
 *  Copyright 2018 Monica Pinto
 *
 *
 */
definition(
    name: "(Parte 2) Avisame Cuando con franja horaria y modo...",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Avisa cuando alguien llega pero solo cuando esta en una franja horaria determinada y en un modo determinado",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name:"pagina1", title:"", install:false, uninstall:true, nextPage:"pagina2"){
    	section("Elige el sensor de presencia..."){
			input "presencia", "capability.presenceSensor", title: "Llegada/Marcha de", required: true, multiple: false
		}
		section("Envia este mensaje cuando alguien llega (opcional, envia un mensaje estandar si este no se especifica)"){
			input "textoMensajeLlegada", "text", title: "Texto del Mensaje", required: false, defaultValue: "Mensaje llegada por defecto"
		}
        section("Envia este mensaje cuando alguien se marcha (opcional, envia un mensaje estandar si este no se especifica)"){
			input "textoMensajeMarcha", "text", title: "Texto del Mensaje", required: false, defaultValue: "Mensaje salida por defecto"
		}
        section("Elige la franja horaria"){
        	input "horaInicio", "time", title: "Hora de inicio", required: true
            input "horaFin","time",title: "Hora de finalizacion", required: true
        }
        section("Elige el modo o modos en el que recibir las notificaciones"){
     		mode name:"modo", title: "Selecciona el Modo:", required: true, multiple:false
        }
    }
    
	page(name:"pagina2")
}

def pagina2(){
	dynamicPage(name:"pagina2",title:"",install:true, uninstall:true){
    	
    	section(){
    		input "pushYTelefono", "enum", title: "Tanto Push como SMS?", required: true, submitOnChange: true, options: ["Solo Push", "Solo SMS", "Push y SMS"]
    	}
    
   		if (pushYTelefono == "Solo SMS" || pushYTelefono == "1" || pushYTelefono == "2" || pushYTelefono == "Push y SMS"){
			section("Telefonos para mensaje SMS"){
				input("receptores", "contact", title: "Enviar notifications a") {
                	paragraph "Si esta fuera de US indique el codigo del Pais"
					input "telefono", "phone", title: "Numero de Telefono (para SMS, opcional)", required: false
				}
			}
    	}
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
	subscribe(presencia, "presence", manejadorEventos)
}

def manejadorEventos(evt) {
	log.debug "Recibida notificacion. Evento ${evt}"
	sendMessage(evt)
}

//Método privado
private sendMessage(evt) {
	String msg
    
    /** Muestra en la consola el contenido de todas las horas, para observar el formato de cada una */
    log.debug "horaInicio: $horaInicio"
    log.debug "horaFin: $horaFin"
    log.debug "evt.date: $evt.date"
    log.debug "location.timeZone: $location.timeZone"
    log.debug "location.mode: $location.mode"
    
    /** Aunque horaInicio y horaFin están en la hora local, evt.date devuelve la hora UTC.
        Sin embargo, el método timeOfDayIsBetween hace las conversiones necesarias usando
        la zona horaria pasada como último parámetro **/
    if (timeOfDayIsBetween(horaInicio, horaFin, evt.date, location.timeZone) && location.mode == modo){ 
    //if (timeOfDayIsBetween(horaInicio, horaFin, evt.date, location.timeZone)){ //Si el modo no funciona y se inicializa a null
        if (evt.value == "present" && textoMensajeLlegada){
    		msg = textoMensajeLlegada
    	}else if (evt.value == "present" && !textoMensajeLlegada){
    		msg = "Alguien ha llegado a $location"
    	}else if (evt.value == "not present" && textoMensajeSalida){
    		msg = textoMensajeSalida
    	}else
			msg = "Alguien se ha marchado de $location"
		
        
        /* Si funcionara el valor por defecto para los mensajes
        if (evt.value == "present")
        	msg = textoMensajeLlegada
        else
        	msg = textoMensajeSalida
        */
		log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

		if (pushYTelefono == "Solo Push"){
    		log.debug 'enviando Push'
    		sendPush(msg)
    	}else{
    		if (location.contactBookEnabled){
        		log.debug 'enviando a la lista de contactos'
        		sendNotificationToContacts(msg,receptores)
        	}else{
        		if (telefono){
            		log.debug 'enviando a movil'
            		sendSms(telefono,msg)
            	}
        	}	
        	if (pushYTelefono == "Push y SMS"){
        		log.debug 'enviando Push'
        		sendPush(msg)
        	}
    	}
    }
}
