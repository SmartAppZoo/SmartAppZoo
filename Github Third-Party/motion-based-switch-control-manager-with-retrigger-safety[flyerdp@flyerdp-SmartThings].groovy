/**
 *  Parent App for Smart Switch Control Based on Motion with Retrigger Timeout and No Motion Auto Off
 *
 *  This Smart App will turn on/off a switch based on Motion with retrigger 
 *  safety and turn off option when nomotion is detected after timeout.
 *  I wrote this because my non local motion detectors were turning on lights
 *  right after switching off manually because of motion detection delay to cloud. 
 *  Copyright 2017 David Poprik
 *  GNU General Public License v2 (https://www.gnu.org/licenses/gpl-2.0.txt)
 *
 */

definition(
    name: "Motion Based Switch Control Manager with Retrigger Safety",
    singleInstance: true,
    namespace: "flyerdp",
    author: "flyerdp@gmail.com",
	description: "Installs Motion Sensor based switch trigger with retrigger safety and No Motion Timeout.",
    category: "Convenience",
    iconUrl: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_60x60.png",
    iconX2Url: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_120x120.png",
    iconX3Url: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_120x120.png"
)

preferences {
	page(name: "main")	
}

def main(){
	def installed = app.installationState == "COMPLETE"
	return dynamicPage(
    	name		: "main"
        ,title		: "Motion Control Switches with Retrigger Timeout"
        ,install	: true
        ,uninstall	: installed
        ){
 
            if (installed){
            	section {
                    app(name: "motionTriggeredSwitch", appName: "Motion Based Switch Control with Retrigger Safety Child", namespace: "flyerdp", title: "Create New Motion Trigger Rule...", multiple: true)
            	}
				section (getVersionInfo()) { }
            } else {
            	section(){
                	paragraph("Tap done to finish installing the Manager then re-open the app from the smartApps flyout to create your motion triggered switch rules.")
                }
            }
			remove("Delete Manager", "Delete?", "${app.label}")
        }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
	state.verParent = "1.1.5"
}
def getVersionInfo(){
	return "Versions:\n\tMotion Control Switch Manager: ${state.verParent}\n\tMotion Control Switch Configuration: ${state.verChild ?: "Unknown."}"
}

def updateVer(vChild){
    state.verChild = vChild
}