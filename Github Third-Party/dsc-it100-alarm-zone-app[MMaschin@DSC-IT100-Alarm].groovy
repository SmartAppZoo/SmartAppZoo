/**
 *  DSC-IT100-Alarm Zone App
 *
 *  Author: Matt Maschinot
 *  
 *  Date: 04/10/2019
 */

// for the UI
definition ( 
name: "DSC-IT100-Alarm-Zone-App",
namespace: "mmaschin/DSCIT100Alarm",
author: "mmaschin@gmail.com",
description: "SmartApp DSCAlarmZoneApp",
category: "Safety & Security",
singleInstance: true)

import groovy.json.JsonBuilder 
import groovy.json.JsonSlurper

preferences {
    page name: "mainPage", title: "DSC Zone Information", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "DSC Zone Information", install: true, uninstall: true
}

def mainPage() {
 
    dynamicPage(name: "mainPage") {
        section {
      		input "zoneNumber", "text", title: "Zone Number", description: "Zone Number Between 1-64", required: true
      		input "zoneType", "enum", multiple: false, title: "Select Zone Type", submitOnChange: true, required: true, options: ['Contact','Motion','Smoke']
        }
    }
}

// page for allowing the user to give the automation a custom name
def namePage() {
     def l = "Alarm_Zone"+settings.zoneNumber
     app.updateLabel(l)
 
    dynamicPage(name: "namePage") {
            section() {
                input "zoneLabel", "text", title: "Zone Label", defaultValue: "Alarm_Zone"+settings.zoneNumber, required: true
            }
     }
}


// When installed - initialize
def installed() {

	//add zone DH to this child app
	addDSCZoneDeviceType()  

  	initialize() 
}


// When updated - initialize
def updated() {

  	initialize()   
}


//nothing really to initialize
def initialize() {

}


// remove the DH
def uninstalled() {
    
    getAllChildDevices().each { 
    	deleteChildDevice(it.deviceNetworkId) 
    }
}


//Update DSC Alarm Device with data from IT-100 (from parent APP)
public updateAlarmZoneDevice(String cmd) {

  def deviceId = 'Alarm_Zone'+settings.zoneNumber
  def zonedevice = getChildDevice(deviceId)

  if (zonedevice) {
  	writeLog("updateAlarmZoneDevice-${cmd}")
    
    zonedevice.updatedevicezone("${cmd}")
    zonedevice.refresh()
  }
}


//Add the zone DH to the child app.
public addDSCZoneDeviceType() {
  
    def deviceId = 'Alarm_Zone'+settings.zoneNumber
    
    def l = "Alarm_Zone"+settings.zoneNumber
    app.updateLabel(l)
    
    if (!getChildDevice(deviceId)) {
        if (settings.zoneType == "Motion") {
            addChildDevice("mmaschin/DSCIT100Alarm", "DSC-IT100-Alarm-Type-Motion", deviceId, parent.hostHub.id, [completedSetup: true, label: "${settings.zoneLabel}", 
                            isComponent: false, componentName: deviceId, componentLabel: "${settings.zoneLabel}"])
        } else if (settings.zoneType == "Contact") {
            addChildDevice("mmaschin/DSCIT100Alarm", "DSC-IT100-Alarm-Type-Contact", deviceId, parent.hostHub.id, [completedSetup: true, label: "${settings.zoneLabel}", 
                            isComponent: false, componentName: deviceId, componentLabel: "${settings.zoneLabel}"])
        } else if (settings.zoneType == "Smoke") {
            addChildDevice("mmaschin/DSCIT100Alarm", "DSC-IT100-Alarm-Type-Smoke", deviceId, parent.hostHub.id, [completedSetup: true, label: "${settings.zoneLabel}", 
                            isComponent: false, componentName: deviceId, componentLabel: "${settings.zoneLabel}"])
        }
    }
 }


// Write debug messages when turned on 
private writeLog(message)
{
  if(parent.idelog){
    log.debug "${message}"
  }
}