/**
 *  Expert Alarm
 *
 *  Version 1.5.0 (06/Oct/2015)
 *  
 *  The latest version of this file can be found on GitHub at:
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 Experthome.cl
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  To Do:  (i)     Incorporar Movimiento Exterior (definir sensores exteriores)
 *          (ii)    Capturar estado de luces para alarma y pre-alarmas/restaurar a condicion inicial
 * 
 * 
 */

import groovy.json.JsonSlurper

definition(
    name:           "Expert Alarm",
    namespace:      "ExpertHome",
    author:         "rodrigo@experthome.cl",
    description:    "Sistema de alarma integrado a Experthome.cl",
    category:       "Safety & Security",
    iconUrl:        "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
    iconX2Url:      "http://experthome.cl/wp-content/uploads/2015/08/Security_14.png",
)

preferences {
    page name:"pageStatus"
    page name:"pageSensores" 
    page name:"pageOpcionesSensor" 
    page name:"pageOpcionesActivacion"
    page name:"pageOpcionesAlarma" 
}

def pageStatus() {
    def alarmStatus = "La Alarma está ${statusAlarmaApp()}"
    def pageProperties = [
        name:       "pageStatus",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]
    return dynamicPage(pageProperties) {
        section("Estado Alarma") {
            paragraph alarmStatus
        }
        section("Menu") {
            href "pageSensores", title:"Sensores", description:"Toca para abrir"
            href "pageOpcionesSensor", title:"Armado Afuera/Casa", description:"Toca para abrir"
            href "pageOpcionesActivacion", title:"Activacion Alarma", description:"Toca para abrir"
            href "pageOpcionesAlarma", title:"Alerta & Notificaciones", description:"Toca para abrir"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def pageSensores() {
    //log.debug("pageSensores()")
    def inputContact = [
        name:       "contacto",
        type:       "capability.contactSensor",
        title:      "Sensores de puerta/ventana",
        multiple:   true,
        required:   false
    ]
    def inputMotion = [
        name:       "movimiento",
        type:       "capability.motionSensor",
        title:      "Sensores de movimiento",
        multiple:   true,
        required:   false
    ]
    def pageProperties = [
        name:       "pageSensores",
        nextPage:   "pageOpcionesSensor",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Agrega/remueve sensores a ser monitoreados") {
            input inputContact
            input inputMotion
        }
    }
}
def pageOpcionesSensor() {
    //log.debug("pageOpcionesSensor()")
    def resumen = 
        "Cada sensor se puede configurar como Afuera o Casa."
        
    def pageProperties = [
        name:       "pageOpcionesSensor",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    //Multiples requieren reprogramacion por el tema de los brackets.
    def inputPuertaPrincipal = [
        name:       "puertaPrincipal",
        type:       "capability.contactSensor",
        title:      "Selecciona...",
        multiple:   false,
        required:   true  
    ]
    def inputDelayPuerta = [
        name:       "delayPuerta",
        type:       "enum",
        title:      "Retraso en Activacion (seg)",
        metadata:   [values:["60","120","180"]],
        defaultValue:"60",
        multiple:   false,
        required:   true  
    ]

    def tipoSensor = ["Afuera", "Casa"]
    return dynamicPage(pageProperties) {
        section("Afuera/Casa") {
            paragraph resumen
        }
        if (settings.contacto) {
            section("Contacto") {
                def devices = settings.contacto.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                    input "type_${devId}", "enum", title:displayName, metadata:[values:tipoSensor], defaultValue:"Casa"
                }
            }
        }
        if (settings.movimiento) {
            section("Movimiento") {
                def devices = settings.movimiento.sort {it.displayName}
                devices.each() {
                    def devId = it.id
                    def displayName = it.displayName
                        input "type_${devId}", "enum", title:"${it.displayName}", metadata:[values:tipoSensor],defaultValue:"Afuera"
                }
            }
        }
        section("Puerta Principal") {
            input inputPuertaPrincipal
            input inputDelayPuerta
        }
    }    
}        
def pageOpcionesActivacion() {
    //log.debug("pageOpcionesActivacion()")
    def resumenRemotos =    
        "Botones: (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico"
    
    def inputModoAfuera = [
        name:       "modosAfuera",
        type:       "mode",
        title:      "Armado Afuera en estos modos...",
        multiple:   true,
        required:   false
    ]
    def inputRemotes = [
        name:       "remoto",
        type:       "capability.button",
        title:      "Que control remoto?",
        multiple:   true,
        required:   false
    ]
    def inputMomentaryAfuera = [
        name:       "momentaryAfuera",
        type:       "capability.momentary",
        title:      "Afuera?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryCasa = [
        name:       "momentaryCasa",
        type:       "capability.momentary",
        title:      "Casa?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryDesactivar = [
        name:       "momentaryDesactivar",
        type:       "capability.momentary",
        title:      "Desarmado?",
        multiple:   false,
        required:   false
    ]
    def inputMomentaryPanico = [
        name:       "momentaryPanico",
        type:       "capability.momentary",
        title:      "Panico?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchAfuera = [
        name:       "switchAfuera",
        type:       "capability.switch",
        title:      "Afuera?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchCasa = [
        name:       "switchCasa",
        type:       "capability.switch",
        title:      "Casa?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchDesactivar = [
        name:       "switchDesactivar",
        type:       "capability.switch",
        title:      "Desarmado?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchPanico = [
        name:       "switchPanico",
        type:       "capability.switch",
        title:      "Panico?",
        multiple:   false,
        required:   false
    ]
    def inputSwitchAlarmaOn = [
        name:       "switchAlarmaOn",
        type:       "capability.switch",
        title:      "Alarma Sonando",
        multiple:   false,
        required:   false
    ]
    def pageProperties = [
        name:       "pageOpcionesActivacion",
        nextPage:   "pageStatus",
        uninstall:  false
    ]
    return dynamicPage(pageProperties) {
        section("Opciones de Activación Alarma") {
        }
        //section("Modos") {
        //    input inputModoAfuera
        //}
        section("Controles Remotos") {
           paragraph resumenRemotos
           input inputRemotes
        }
        section("Botonera") {
           input inputMomentaryAfuera
           input inputMomentaryCasa
           input inputMomentaryDesactivar
           input inputMomentaryPanico
        }
        section("Switch para Status"){
           input inputSwitchAfuera
           input inputSwitchCasa
           input inputSwitchDesactivar
           input inputSwitchPanico
           input inputSwitchAlarmaOn
        }
        
    }
}
def pageOpcionesAlarma() {
    //log.debug("pageOpcionesAlarma()")
    def inputSirena = [
        name:           "sirena",
        type:           "capability.alarm",
        title:          "Que sirenas?",
        multiple:       true,
        required:       false
    ]
    def inputLuces = [
        name:           "luces",
        type:           "capability.switch",
        title:          "Que luces?",
        multiple:       true,
        required:       false
    ]
    def inputCamaras = [
        name:           "camaras",
        type:           "capability.imageCapture",
        title:          "Que camaras?",
        multiple:       true,
        required:       false
    ]
    def inputPush = [
        name:           "pushMessage",
        type:           "bool",
        title:          "Mensaje Push?",
        defaultValue:   true
    ]
    def inputPhone1 = [
        name:           "phone1",
        type:           "phone",
        title:          "Envia a este numero",
        required:       false
    ]
    def inputPhone2 = [
        name:           "phone2",
        type:           "phone",
        title:          "Envia a este numero",
        required:       false
    ]
    
    def pageProperties = [
        name:       "pageOpcionesAlarma",
        nextPage:   "pageStatus",
        uninstall:  false
    ]

    return dynamicPage(pageProperties) {
        section("Alerta") {
            input inputSirena
            input inputLuces
            input inputCamaras
        }
        section("Notificaciones") {
            input inputPush
            input inputPhone1
            input inputPhone2
        }
    }
}

def installed() {
    log.debug("installed()")
    initialize()
}

def updated() {
    log.debug("updated()")
    unschedule()
    unsubscribe()
    initialize()
}

private def initialize() {
    //Estado inicial de alarma
    state.afuera = false
    state.casa = false
    state.panico = false
    state.desarmado = true
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    //Seteo tiempo delay
    state.delay = settings.delayPuerta?.toInteger()
    //Seteo de otras variables de la alarma
    state.alarmaOn = false
    statusAlarmaOn (state.alarmaOn)
    state.alarmaDelay = false
    state.offSwitches = []
    //Mapeo sensores y suscripcion a eventos
    sensores()
    controlRemoto()
    momentarySwitch()
}

//mapeo sensores y suscripcion
private def sensores() {
    log.debug("sensores()")
    state.sensorContacto = []
    state.sensorContacto << [
        idSensor:   null,
        tipoArmado: "Casa",
    ]
    if (settings.contacto) {
        settings.contacto.each() {
            state.sensorContacto << [
                idSensor:   it.id,
                tipoArmado: settings["type_${it.id}"] ?: "Casa",
            ]
        }
        subscribe(settings.contacto, "contact.open", onContacto)
        state.sensorContacto.each() {
            log.debug ("Instalacion Exitosa Sensor Contacto ${it.idSensor} - ${it.tipoArmado}")    
        }
    }
    state.sensorMovimiento = []
    state.sensorMovimiento << [
        idSensor:   null,
        tipoArmado: "Afuera",
    ]
    if (settings.movimiento) {
        settings.movimiento.each() {
            state.sensorMovimiento << [
                idSensor:   it.id,
                tipoArmado: settings["type_${it.id}"] ?: "Afuera",
            ]
        }
        subscribe(settings.movimiento, "motion.active", onMovimiento)
        state.sensorMovimiento.each() {
            log.debug ("Instalacion Exitosa Sensor Movimiento ${it.idSensor} - ${it.tipoArmado}")    
        }
    }
}
// Control remoto por default define botones (1) Afuera, (2) Casa, (3) Desactivar, (4) Panico
private def controlRemoto() {
    if (settings.remoto) {
        subscribe(settings.remoto, "button", onControlRemoto)
    }
}
//Nombre boton momentario debe ser mismo que funciones definidas
//Tiene que estar en ingles para Alexa (Away, Home & Panic). No se permite desactivacion por Alexa.
private def momentarySwitch() {
    log.debug("switchSimulado()")
    if (settings.momentaryAfuera) {
        subscribe(settings.momentaryAfuera,"switch.on",onMomentary)
    }
    if (settings.momentaryCasa) {
        subscribe(settings.momentaryCasa,"switch.on",onMomentary)
    }
    if (settings.momentaryDesactivar) {
        subscribe(settings.momentaryDesactivar,"switch.on",onMomentary)
    }
    if (settings.momentaryPanico) {
        subscribe(settings.momentaryPanico,"switch.on",onMomentary)
    }
}

//Alarma Armada Afuera implica que sensores armados afuera y casa tienen que activar alarma
//Alarma Armada Casa implica que solo sensores armados casa tienen que activar alarma
def onContacto(evt) {
    log.debug("Evento ${evt.displayName}")
    def contactoOk = state.sensorContacto.find() {it.idSensor == evt.deviceId}
    if (!contactoOk) {
        log.warn ("No se encuentra el dispositivo de contacto ${evt.deviceId}")
        return
    }
    //Solo aplicar delay para cuando alarma se encuentra en modo Armado Afuera.
    //Idea es que cuando uno llegue, pueda desactivar la alarma en keypad tasker.
    if((contactoOk.tipoArmado == "Afuera" && state.afuera) || (contactoOk.tipoArmado == "Casa" && state.afuera)) {
        state.evtDisplayName = evt.displayName
        if (contactoOk.idSensor == settings.puertaPrincipal.id) {
            log.debug("Se detectó apertura de Puerta Principal ${settings.puertaPrincipal.displayName}... Activacion Alarma en ${state.delay} seg.")
            runIn(state.delay, "activarAlarma")
            state.alarmaDelay = true
        } else {
            activarAlarma()    
        }
    }
    if (contactoOk.tipoArmado == "Casa" && state.casa) {
        state.evtDisplayName = evt.displayName
        activarAlarma()  
    }
}

def onMovimiento(evt) {
    log.debug("Evento ${evt.displayName}")
    def movimientoOk = state.sensorMovimiento.find() {it.idSensor == evt.deviceId}
    if (!movimientoOk) {
        log.warn ("No se encuentra el dispositivo de movimiento ${evt.deviceId}")
        return
    }
    if((movimientoOk.tipoArmado == "Afuera" && state.afuera) || (movimientoOk.tipoArmado == "Casa" && state.afuera)
    || (movimientoOk.tipoArmado == "Casa" && state.casa)) {
        state.evtDisplayName = evt.displayName
        activarAlarma()    
    }
}

def onControlRemoto(evt) {
    log.debug("onControlRemoto")
    if (!evt.data) {
        return
    }
    def slurper = new JsonSlurper()
    def data = slurper.parseText(evt.data)
    def button = data.buttonNumber?.toInteger()
    if (button) {
        log.debug("Boton ${button} fue ${evt.value}")
        //Nombre en ingles para integracion con Alexa
        if (button == 1) {
            away()
        } else if (button==2) {
            home()
        } else if (button==3) {
            disarm()
        } else if (button==4) {
            panic()
        }
        
    }
}
//Nombre Switch Momentario debe ser mismo que funciones definidas
// away, home, & panic (en ingles para uso con Amazon Echo) Disarm no puede ser accionado desde echo.
def onMomentary(evt) {
    "${evt.displayName}"()
}

private def away() {
    log.debug("Preparando Armado Afuera...")
    if (revisarContacto()) {
        if (!atomicState.afuera) {
            if (!atomicState.alarmaOn) {
                if (!state.alarmaDelay) {
                    def msg = "Armado Afuera se activa en ${state.delay} seg."
                    log.debug(msg)
                    mySendPush(msg)
                    runIn(state.delay, "armadoAlarmaAfuera")
                    state.alarmaDelay = true
                } else {
                    def msg = "Armado Afuera Fallido - Existe proceso en ejecucion"
                    log.debug(msg)
                    mySendPush(msg) 
                }
            } else {
                def msg = "Armado Afuera Fallido - Alarma está sonando"
                log.debug(msg)
                mySendPush(msg) 
            }
        } else {
            def msg = "Armado Afuera Fallido - Alarma se encuentra en Armado Afuera"
            log.debug(msg)
            mySendPush(msg)
        }
    }
}

private def home() {
    log.debug("Preparando Armado Casa...")
    if (revisarContacto()) {
        if (!atomicState.casa) {
            if (!atomicState.alarmaOn) {
                if (!state.alarmaDelay) {
                    armadoAlarmaCasa()
                } else {
                    def msg = "Armado Casa Fallido - Existe proceso en ejecucion"
                    log.debug(msg)
                    mySendPush(msg) 
                }
            } else {
                def msg = "Armado Casa Fallido - Alarma está sonando"
                log.debug(msg)
                mySendPush(msg) 
            }
        } else {
            def msg = "Armado Casa Fallido - Alarma se encuentra en Armado Casa"
            log.debug(msg)
            mySendPush(msg)
        }
    }
}
private def disarm() {
    log.debug("Preparando Desarmado...")
    if ((!atomicState.desarmado) || (atomicState.desarmado && state.alarmaDelay)){
        desactivarAlarma()
    } else {
        def msg = "Desarmado Fallido - Alarma se encuentra Desarmada"
        log.debug(msg)
        mySendPush(msg)
    }
}

private def panic() {
    log.debug("Activando Panico...")
    if (!atomicState.panico){
        if (!atomicState.alarmaOn) {
            activarPanico()
        } else {
            def msg = "Emergencia - Alarma y Boton de Panico en ${location.name} activados"
            log.debug(msg)
            mySendPush(msg)
            if (settings.phone1) {
                sendSmsMessage(phone1, msg)
            }
            if (settings.phone2) {
                sendSmsMessage(phone2, msg)
            }
        }
    } else {
        def msg = "Panico Fallido - Alarma está sonando"
        log.debug(msg)
        mySendPush(msg)
    }
}

private def activarAlarma() {
    state.alarmaOn = true
    statusAlarmaOn (state.alarmaOn)
    state.alarmaDelay = false
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    def msg = "Alarma en ${location.name}! - ${state.evtDisplayName}"
    log.debug(msg)
    mySendPush(msg)
    if (settings.phone1) {
        sendSmsMessage(phone1, msg)
    }
    if (settings.phone2) {
        sendSmsMessage(phone2, msg)
    }
}

private def activarPanico() {
    unschedule()
    state.afuera = false
    state.casa = false
    state.desarmado = false
    state.panico = true
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    state.alarmaOn = true
    statusAlarmaOn (state.alarmaOn)
    state.alarmaDelay = false
    settings.sirena*.strobe()
    settings.camaras*.take()
    def lucesOn = settings.luces?.findAll {it?.latestValue("switch").contains("off")}
    if (lucesOn) {
        lucesOn*.on()
        state.offLuces = lucesOn.collect {it.id}
    }
    def msg = "Boton de Panico en ${location.name}!"
    log.debug(msg)
    mySendPush(msg)
        if (settings.phone1) {
        sendSmsMessage(phone1, msg)
    }
    if (settings.phone2) {
        sendSmsMessage(phone2, msg)
    }
}
private def desactivarAlarma() {
    unschedule()
    state.afuera = false
    state.casa = false
    state.desarmado = true
    state.panico = false
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    state.alarmaOn = false
    statusAlarmaOn (state.alarmaOn)
    state.alarmaDelay = false
    settings.sirena*.off()
    def lucesOff = state.offLuces
    if (lucesOff) {
        settings.luces?.each() {
            if (lucesOff.contains(it.id)) {
                it.off()
            }
        }
        state.offLuces = []
    }
    def msg = "Alarma en ${location.name} desactivada"
    mySendPush(msg)
    log.debug(msg)
        if (settings.phone1) {
        sendSmsMessage(phone1, msg)
    }
    if (settings.phone2) {
        sendSmsMessage(phone2, msg)
    }
}
private def armadoAlarmaAfuera(){
    state.desarmado = false
    state.panico = false
    state.afuera = true
    state.casa = false
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    state.alarmaDelay = false
    mySendPush("Armado Afuera en ${location.name} exitoso")
    log.debug("Armado Afuera en ${location.name} exitoso")
}
private def armadoAlarmaCasa(){
    state.desarmado = false
    state.panico = false
    state.afuera = false
    state.casa = true
    statusAlarma(state.afuera, state.casa, state.panico, state.desarmado)
    mySendPush("Armado Casa en ${location.name} exitoso")
    log.debug("Armado Casa en ${location.name} exitoso")
}

private def revisarContacto(){
    def algoAbierto = settings.contacto.findAll {it?.latestValue("contact").contains("open")}
    if (algoAbierto.size() > 0) {
        algoAbierto.each() {
            log.debug("${it.displayName} esta abierta, no se puede continuar con proceso armado")
            mySendPush("${it.displayName} esta abierta, no se puede continuar con proceso armado")
        }
        return false
    }
    return true
}

private def statusAlarmaApp(){
    def statusAlarmaAhora = "No Instalada"
    if(state.afuera) {
        statusAlarmaAhora = "Armada Afuera"
    }
    if(state.casa) {
        statusAlarmaAhora = "Armada Casa"
    }
    if(state.desarmado) {
        statusAlarmaAhora = "Desarmada"
    }
    if(state.panico) {
        statusAlarmaAhora = "Panico"
    }
    return statusAlarmaAhora
}

private def mySendPush(msg) {
    // sendPush puede arrojar un error
    try {
        sendPush(msg)
    } catch (e) {
        log.error e
    }
}
//Via switch voy analizando estado de alarma.
//Agregar switch statusAlarma, para suscribirse a activacion de alarmas.
//Solo se usan para revisar estado
//Proceso ineficiente
private def statusAlarma(afueraBool, casaBool, panicoBool, desarmadoBool) {
    if (afueraBool){
        settings.switchAfuera.on()
        settings.switchCasa.off()
        settings.switchPanico.off()
        settings.switchDesactivar.off()
        //log.debug ("Status actualizado a Armado Afuera")
    }
    if (casaBool){
        settings.switchAfuera.off()
        settings.switchCasa.on()
        settings.switchPanico.off()
        settings.switchDesactivar.off()
        //log.debug ("Status actualizado a Armado Casa")
    }
    if (panicoBool){
        settings.switchAfuera.off()
        settings.switchCasa.off()
        settings.switchPanico.on()
        settings.switchDesactivar.off()
        //log.debug ("Status actualizado a Panico")
    }
    if (desarmadoBool){
        settings.switchAfuera.off()
        settings.switchCasa.off()
        settings.switchPanico.off()
        settings.switchDesactivar.on()
        //log.debug ("Status actualizado a Desarmado")
    }
}
private def statusAlarmaOn(alarmaOnBool) {
    if (alarmaOnBool){
        settings.switchAlarmaOn.on()
    } else {
        settings.switchAlarmaOn.off()
    }
}
//RunIn method 
private def myRunIn(delay_s, func) {
    if (delay_s > 0) {
        def date = new Date(now() + (delay_s * 1000))
        runOnce(date, func)
    }
}
