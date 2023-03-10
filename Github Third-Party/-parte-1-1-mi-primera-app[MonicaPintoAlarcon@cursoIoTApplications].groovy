/**
 *  Mi Primera App
 *
 *  Copyright 2018 Monica Pinto
 *
 *
 */
definition(
    name: "(Parte 1) 1. Mi Primera App",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Aplicacion de prueba para demostrar las caracteristicas basicas de SmartThing",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Selecciona el sensor de movimiento:") {
        input "elSensorMov", "capability.motionSensor", required: true, multiple:false, title: "Sensor de movimiento?"
    }
    section("Selecciona la luz a encender/apagar") {
        input "elInterruptor", "capability.switch", required: true, multiple: false, title: "Interruptor?"
        
        /* Alternativamente podemos definir mas de un interruptor dandole a multiple el valor true:
        input "elInterruptor", "capability.switch", required: true, multiple: true, title: "Interruptor?"
        */
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
	// Nos suscribimos a los eventos del sensor de movimiento
    
    /* Opción 1: Nos suscribimos a los eventos de forma individual, con el mismo o distinto manejador
    subscribe(elSensorMov,"motion.active",movimientoDetectadoManejador)
    subscribe(elSensorMov,"motion.inactive",movimientoDetectadoManejador)*/
    
    /* Opción 2: Nos suscribimos a TODOS los eventos lanzados por el sensor de movimiento*/
    subscribe(elSensorMov,"motion",movimientoDetectadoManejador)
   
}

// Implementamos el manejador de eventos
// Opción 1: Tenemos un solo manejador y distintas acciones dependiendo del evento
def movimientoDetectadoManejador(evt){
	log.debug "movimientoDetectadoManejador called: $evt"
    if (evt.value == 'active')
    	elInterruptor.on()
    else if (evt.value == 'inactive')
    	elInterruptor.off()
    
    /** Si hubiera más de un interruptor podemos interactuar con ellos de forma separada
     *  "elInterruptor" en caso de haber más de uno sería una lista a la que podemos acceder por posición
    
     *elInterruptor[0].on()
     *elInterruptor[2].on()
    */
}

/* Opción 2: Tenemos dos manejadores distintos, uno por cada evento. 
  def movimientoDetectadoManejador(evt){
	log.debug "movimientoDetectadoManejador called: $evt"
    elInterruptor.on()
}
  def movimientoNoDetectadoManejador(evt){
	log.debug "movimientoNODetectadoManejador called: $evt"
    elInterruptor.off()
}
*/