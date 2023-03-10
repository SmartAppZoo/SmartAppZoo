/**
 *  Copyright 2015 SmartThings
 *
 *  Author: Monica Pinto
 *  Date: 2016-07-06
 */
definition(
    name: "(Parte 3) 4. Te has dejado la puerta abierta - version Octubre 2018",
    namespace: "cursoIoTApplications",
    author: "SmartThings",
    description: "Notifies you when you have left a door open longer that a specified amount of time.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/ModeMagic/bon-voyage%402x.png"
)

preferences {

	section("Monitoriza estas puertas") {
		input "puertas", "capability.contactSensor", multiple:true //multiple debe ser true
	}
	section("Y notificame cuando se queden abiertas mas de este número de minutos (por defecto 10)") {
		input "tiempoAbiertas", "number", description: "Número de minutos", required: false
	}
	section("Mediante un mensaje de texto a este numero (o mediante un push si no se especifica numero") {
        input("receptores", "contact", title: "Enviar notificaciones a") {
            input "telefono", "phone", title: "Número teléfono (opcional)", required: false
        }
	}
}

def installed() {
	log.trace "installed()"
	initialize()
}

def updated() {
	log.trace "updated()"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(puertas, "contact.open", puertaAbierta)
	subscribe(puertas, "contact.closed", puertaCerrada)
}

def puertaAbierta(evt)
{
	log.trace "puertaAbierta($evt.name: $evt.value)"
	
    //El retraso se expresa en segundos
    //El valor por defecto es 10 (10 minutos * 60 = 600 segundos)
	def retraso = (tiempoAbiertas != null && tiempoAbiertas != "") ? tiempoAbiertas * 60 : 600
	runIn(retraso, puertaAbiertaDemasiadoTiempo)
	
    log.debug "planificación de puerta abierta demasiado tiempo ..."
}

//Se puede omitir, solo muestra traza por pantalla
def puertaCerrada(evt)
{
	log.trace "puertaCerrada($evt.name: $evt.value)"
}

def puertaAbiertaDemasiadoTiempo() {
	//def estadoPuertas = puertas.currentState("contact")
    def estadoPuertas = puertas.contactState //Nos quedamos con los estados porque tienen información temporal
    log.debug "estadoPuertas = $estadoPuertas"
    log.debug "estado puertas = ${estadoPuertas*.value}"
	def estadoPuertasAbiertas = estadoPuertas.findAll {it.value == "open"} //Nos quedamos con los estados de las puertas que estén abiertas
    if (estadoPuertasAbiertas.size() > 0){ 
		log.debug "Alguna puerta lleva abierta demasiado tiempo ..."
		sendMessage(estadoPuertasAbiertas)
    }
	else {
		log.warn "La tarea se ha ejecutado pero no se hace nada ..."
	}
}

void sendMessage(estadoPuertasAbiertas)
{
	//Obtenemos el tiempo en segundos que cada puerta ha estado abierta
    def tiempoAbiertas = estadoPuertasAbiertas.collect {it -> (now() - it.date.time)/1000}
    log.debug "nombre puertas abiertas = ${estadoPuertasAbiertas*.displayName}"
	log.debug "tiempo abiertas = ${tiempoAbiertas} segundos"
    def msg = "Las puertas ${estadoPuertasAbiertas*.displayName} llevan abiertas ${tiempoAbiertas} segundos"   
    if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (phone) {
            sendSms phone, msg
        } else {
            sendPush msg
        }
    }
}