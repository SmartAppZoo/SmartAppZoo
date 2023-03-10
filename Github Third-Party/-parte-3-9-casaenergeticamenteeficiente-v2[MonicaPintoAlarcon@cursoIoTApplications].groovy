/**
 *  CasaEnergeticamenteEficiente-v2
 *
 *  Copyright 2016 Monica Pinto
 *
 *
 */
definition(
    name: "(Parte 3) 9. CasaEnergeticamenteEficiente-v2",
    namespace: "cursoIoTApplications",
    author: "Monica Pinto",
    description: "Usando planificacion de tareas y la variable static; nos aseguramos de apagar las luces en caso de que haya m\u00E1s de una encendida y dejar solo una. Dejamos la \u00FAltima en encenderse",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Title") {
        input "luces", "capability.switch", multiple:true
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
    runEvery30Minutes(comprobarLuces)
}

def comprobarLuces(){

	log.info luces[0]?.switchState.value //Devuelve el valor asociado al estado del dispositivo
    log.info luces[0]?.currentSwitch //Devuelve lo mismo que lo anterior
    log.info luces[0]?.currentState("switch").value
    log.info luces[0]?.currentValue("switch")
    def numLucesOn = luces.count {it?.latestValue("switch") == 'on'}
    log.info "numLuces = $numLucesOn"
	
    if (numLucesOn > 1) {
    	log.debug "Apaga alguna luz"
        //Encuentra todas las luces que están encendidas (devuelve una lista)
        def luzOn = luces.findAll {it?.latestValue("switch") == 'on' }
        //Ordena los elementos de la lista por la fecha en la que cambio de estado por ultima vez
        def orden = luzOn.sort {it.latestState?.date}
        def times = orden*.latestState.date
        log.debug "times: $orden orden:$times"
        def cont = luzOn.size, j = 0
        while(cont > 1 ){
        	orden[j].off()
            cont = cont - 1
            j = j + 1
        }
    }
}