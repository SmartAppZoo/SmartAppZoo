/**
 *	Leviton VRCS4 Association Manager
 *
 *	Author: yg
 *	Date: 2017-11-17
 * 
 */

definition(
    name: "VRCS4 Association Manager",
    namespace: "ygelfand",
    author: "ygelfand",
    description: "Direct associate light switches/dimmer to buttons on Leviton VRCS4",
    category: "Convenience",
    iconUrl: 'http://s3.amazonaws.com/cesco-content/unilog/Batch3/078477/612780-ProductImageURL.jpg',
    iconX2Url: 'http://s3.amazonaws.com/cesco-content/unilog/Batch3/078477/612780-ProductImageURL.jpg'
)

preferences {
	page(name: "selectButtons")
}

def selectButtons() {
	dynamicPage(name: "selectButtons", title: "VRCS4 controller", uninstall: true, install:true) {
		section {
			input "buttonDevice", "capability.button", title: "Controller", multiple: false, required: true
		}
    (1..4).each {
        def buttonNum = it
		    section(title: "Button ${buttonNum} will toggle") {
			    input "switches_${buttonNum}", "capability.switch", title: "Switches:", multiple: true, required: false
		    }
        section(title: "Button ${buttonNum} LED Status Color when on:") {
    	    input "color_${buttonNum}", "enum", title: "Choose LED Color.", required: false, default: 1, multiple: false, options: [1: 'Green', 17: 'Orange']
        }
      }
   section {
        	label(title: "Label this SmartApp", required: false, defaultValue: "VRCS4 Associator")
   }  

	}
}

def installed() {
	initialize()
    state.installed = true
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(buttonDevice, "button", buttonEvent)
    (1..4).each {
    	def devices = settings['switches_'+it]
        if(devices) {
        	buttonDevice.setButton(it, devices.deviceNetworkId.join(','))
    		  subscribe(devices, 'switch', updateLights)
         }
    }
    buttonDevice.configure()
}

def configured() {
	return  buttonDevice
}

def buttonEvent(evt){
    (1..4).each {
      settings['switches_'+it].each {
      	if(it.hasCommand('poll')) {
          it.poll()
        } else if (it.hasCommand('ping')) {
          it.ping()
        }
      }
      //settings['switches_'+it]*.poll()
     }
	return;
}

def updateLights(evt)
{
	def one = switches_1*.currentValue('switch').contains('on')
	def two = switches_2*.currentValue('switch').contains('on')
	def three = switches_3*.currentValue('switch').contains('on')
	def four = switches_4*.currentValue('switch').contains('on')
  buttonDevice.setLightStatus((one ? color_1 : 0),(two ? color_2 : 0),(three ? color_3 : 0) , (four ? color_4 : 0))
}
