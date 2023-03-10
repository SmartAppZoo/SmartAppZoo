/**
 * 	Color Coordinator 
 *  Version 1.1.0 - 11/4/16
 *  By Michael Struck
 *
 *  1.0.0 - Initial release
 *  1.1.0 - Fixed issue where master can be part of slaves. This causes a loop that impacts SmartThings. 
 *
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
	name: "Color Coordinator2",
	namespace: "wbrussell",
	author: "Brian Russell",
	description: "Ties multiple colored lights to one specific light's settings",
	category: "Convenience"
    /*,
	iconUrl: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/ColorCoordinator/CC.png",
	iconX2Url: "https://raw.githubusercontent.com/MichaelStruck/SmartThings/master/Other-SmartApps/ColorCoordinator/CC@2x.png"*/
)

preferences {
        section("Master Light") {
			input "master", "capability.colorControl", title: "Colored Light"
		}
		section("Lights that follow the master settings") {
			input "slaves", "capability.colorControl", title: "Colored Lights",  multiple: true, required: false, submitOnChange: true
		}
    	section([mobileOnly:true], "Options") {
			input "randomYes", "bool",title: "When Master Turned On, Randomize Color", defaultValue: false
		
        }
}

/*def mainPage() {
	dynamicPage(name: "mainPage", title: "", install: true, uninstall: true) {
		def masterInList = slaves.id.find{it==master.id}
        if (masterInList) {
        	section ("**WARNING**"){
            	paragraph "You have included the Master Light in the Slave Group. This will cause a loop in execution. Please remove this device from the Slave Group.", image: "https://raw.githubusercontent.com/MichaelStruck/SmartThingsPublic/master/img/caution.png"
            }
        }
        section("Master Light") {
			input "master", "capability.colorControl", title: "Colored Light"
		}
		section("Lights that follow the master settings") {
			input "slaves", "capability.colorControl", title: "Colored Lights",  multiple: true, required: false, submitOnChange: true
		}
    	section([mobileOnly:true], "Options") {
			input "randomYes", "bool",title: "When Master Turned On, Randomize Color", defaultValue: false
			href "pageAbout", title: "About ${textAppName()}", description: "Tap to get application version, license and instructions"
        }
	}
}

page(name: "pageAbout", title: "About ${textAppName()}", uninstall: true) {
	section {
    	paragraph "${textVersion()}\n${textCopyright()}\n\n${textLicense()}\n"
	}
	section("Instructions") {
		paragraph textHelp()
	}
    section("Tap button below to remove application"){
    }
}
*/
def installed() {   
	init() 
}

def updated(){
	unsubscribe()
    init()
}

def init() {
    	def masterInList = slaves.id.find{it==master.id}
        if (masterInList) {
			log.debug("Error - master is in slave list!")
        }
        else {
            subscribe(master, "switch", onOffHandler)
            subscribe(master, "level", colorHandler)
            subscribe(master, "hue", colorHandler)
            subscribe(master, "saturation", colorHandler)
            subscribe(master, "colorTemperature", tempHandler)
            
        state.masterTemp = master.currentValue("colorTemperature")
    }
}
//-----------------------------------
def onOffHandler(evt){
	if (!slaves.id.find{it==master.id}){
        if (master.currentValue("switch") == "on"){
            if (randomYes) getRandomColorMaster()
			else slaves?.on()
        }
        else {
            slaves?.off()  
        }
	}
}

def colorHandler(evt) {
    log.debug("colorHandler: evt:$evt.name, $evt.value")
   	if (!slaves.id.find{it==master.id} && master.currentValue("switch") == "on"){
		log.debug "Changing Slave units H,S,L"
        
        try {
        	def tempLevel = master.currentValue("colorTemperature")
            if (tempLevel != null && state.masterTemp != tempLevel)
            {
     		    log.debug "Changing Slave color temp, $tempLevel"
                state.masterTemp = tempLevel
            	slaves?.setColorTemperature(tempLevel)
            }
    	}
   		catch (e){
    		log.debug "Error - Color temp for master --"
    	}
        
    	def dimLevel = master.currentValue("level")
    	def hueLevel = master.currentValue("hue")
    	def saturationLevel = master.currentValue("saturation")
		def newValue = [hue: hueLevel, saturation: saturationLevel, level: dimLevel as Integer]
    	log.debug "Changing Slave color, ${newvalue}, hue:$hueLevel,sat:$saturationLevel,level:$dimLevel"
        slaves?.setColor(newValue)
	}
}

def getRandomColorMaster(){
    def hueLevel = Math.floor(Math.random() *1000)
    def saturationLevel = Math.floor(Math.random() * 100)
    def dimLevel = master.currentValue("level")
	def newValue = [hue: hueLevel, saturation: saturationLevel, level: dimLevel as Integer]
    log.debug hueLevel
    log.debug saturationLevel
    master.setColor(newValue)
    slaves?.setColor(newValue)   
}

def tempHandler(evt){
    log.debug("tempHandler: evt:$evt.name, $evt.value")
    if (!slaves.id.find{it==master.id} && master.currentValue("switch") == "on"){
        if (evt.value != "--") {
            def tempLevel = master.currentValue("colorTemperature")
            log.debug "TempHandler: Changing Slave color temp based on Master change. tempLevel:$tempLevel"
            if (tempLevel !=null)
            {
            slaves?.setColorTemperature(tempLevel)
            }
        }
	}
}

//Version/Copyright/Information/Help

private def textAppName() {
	def text = "Color Coordinator"
}	

private def textVersion() {
    def text = "Version 1.1.0 (11/04/2016)"
}

private def textCopyright() {
    def text = "Copyright © 2016 Michael Struck"
}

private def textLicense() {
    def text =
		"Licensed under the Apache License, Version 2.0 (the 'License'); "+
		"you may not use this file except in compliance with the License. "+
		"You may obtain a copy of the License at"+
		"\n\n"+
		"    http://www.apache.org/licenses/LICENSE-2.0"+
		"\n\n"+
		"Unless required by applicable law or agreed to in writing, software "+
		"distributed under the License is distributed on an 'AS IS' BASIS, "+
		"WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. "+
		"See the License for the specific language governing permissions and "+
		"limitations under the License."
}

private def textHelp() {
	def text =
    	"This application will allow you to control the settings of multiple colored lights with one control. " +
        "Simply choose a master control light, and then choose the lights that will follow the settings of the master, "+
        "including on/off conditions, hue, saturation, level and color temperature."
}