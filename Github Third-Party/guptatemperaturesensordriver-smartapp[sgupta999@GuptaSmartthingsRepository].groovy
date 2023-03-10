	/**
 *  Copyright Sandeep Gupta
 *
 *  This software comes with absolutely no WARRANTIES OR CONDITIONS OF ANY KIND under any scenarioor condition
 *
 *	This is a variation of the default CT100 thermostat. I found the cycling through modes and increasing
 *  setpoints one degree at a time very cumbersome. Following is a summary of changes from the default
 *
 *	1. Displaying all possible information within the Thermostat multiTile
 *		a. Removed icon so temperature is visible in large font
 *		b. Since I have it wired to C-wire, replaced battery info with humidity
 *		c. Add a dash of color (I am not a graphic designer so...)
 *
 *	2. Instead of cycling through all the modes creating dedicated buttons for each mode
 *		b. Added some colors - slight text overlay issue (can be fixed by replacin with custom icons)
 *
 *	3. Besides the one degree up and down controls added a slider control so heating
 *		and cooling setpoints can be changed in larger increments
 *		
 */

	
	
	
	definition(
		name: "Gupta Temperature Sensor Driver",
		namespace: "gupta",
		author: "Sandeep Gupta",
		description: "To Control Switches/Contact Sensors from any temperature sensor",	
		iconUrl: "http://www.iconarchive.com/show/global-warming-icons-by-iconshock/switch-icon.html",
		iconX2Url: "http://www.iconarchive.com/show/global-warming-icons-by-iconshock/switch-icon.html",
	   iconX3Url: "http://www.iconarchive.com/show/global-warming-icons-by-iconshock/switch-icon.html")

	preferences {
			page(name: "mainPage", title: "PRIMARY Temperature Sensor", nextPage: "namePage", uninstall: true) {
				section("Pick the Temperature Sensor "){
					 input name: "activated", type: "bool", title: "Activate ",required: true, defaultValue: true
					 input "tempSensor", "capability.temperatureMeasurement", title: "Which ",hideWhenEmpty: true, required: true
				}
				section("Set Cooling Setpoint (CSP)"){
					paragraph "Target temperature to cool to. Once reached OFF event triggered"
					input name: "cspTemp", type: "decimal", title: "CSP (Degrees?)", defaultValue: "90"
					paragraph "Variance represents degrees above CSP when ON event to cool will trigger. Example - If CSP is 75 and variance is 3, ON event will trigger at CSP+Variance or 75+3 = 78 and OFF event will trigger at 75."
					input name: "cspVar", type: "decimal", title: "Variance", defaultValue: "3"
				}
				section("Set Heating Setpoint (HSP)"){
					paragraph "Target temperature to heat to. Once reached OFF event triggered"
					input name: "hspTemp", type: "decimal", title: "HSP (Degrees?)", defaultValue: "60"
					paragraph "Variance represents degrees below HSP when ON event to heat will trigger. Example - If HSP is 75 and variance is 3, ON event will trigger at CSP-Variance or 75-3 = 72 and OFF event will trigger at 75."
					input name: "hspVar", type: "decimal", title: "Variance", defaultValue: "3"
				}
			}
			
			page (name: namePage, nextPage: "cspPage" )
			
			
			page(name: "cspPage", title: "Cooling Setpoint Controls", nextPage: "hspPage", uninstall: true) {
				section("When CSP ON event"){
					 input "cspONNswitches", "capability.switch", title: "Turn these switches ON ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspONFswitches", "capability.switch", title: "Turn these switches OFF ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspONNContactSensors", "capability.contactSensor", title: "OPEN these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspONFContactSensors", "capability.contactSensor", title: "CLOSE these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
				}
				section("When CSP OFF event"){
					 input "cspOFFFswitches", "capability.switch", title: "Turn these switches OFF ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspOFFNswitches", "capability.switch", title: "Turn these switches ON ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspOFFFContactSensors", "capability.contactSensor", title: "CLOSE these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
					 input "cspOFFNContactSensors", "capability.contactSensor", title: "OPEN these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
				}
			}
			
			page(name: "hspPage", title: "Heating Setpoint Controls", install: true) {
				section("When HSP ON event"){
					 input "hspONNswitches", "capability.switch", title: "Turn these switches ON ", multiple: true, hideWhenEmpty: true, required: false
					 input "hspONFswitches", "capability.switch", title: "Turn these switches OFF ", multiple: true, hideWhenEmpty: true, required: false
					 input "hspONNContactSensors", "capability.contactSensor", title: "OPEN these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
					 input "hspONFContactSensors", "capability.contactSensor", title: "CLOSE these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
				}
				section("When HSP OFF event"){
					 input "hspOFFFswitches", "capability.switch", title: "Turn these switches OFF ", multiple: true, hideWhenEmpty: true, required: false
					 input "hspOFFNswitches", "capability.switch", title: "Turn these switches ON ", multiple: true, hideWhenEmpty: true, required: false 
					 input "hspOFFFContactSensors", "capability.contactSensor", title: "CLOSE these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
					 input "hspOFFNContactSensors", "capability.contactSensor", title: "OPEN these contact sensors ", multiple: true, hideWhenEmpty: true, required: false
				}
			}
		}
		
		def namePage() {
			dynamicPage(name: "namePage", title: "App Name (Defaults to device Name)", uninstall: true) {
				section() {
					label title: "Assign a name", required: false, defaultValue: "${tempSensor.displayName} - TC"
					mode title: "Set for specific mode(s)", required: false
					input "pushNotification", "bool", title: "Push Notification(s)", required: false, defaultValue: "true"
				}
			}
		}


		def installed() {
			initialize()
		}

		def updated(){
			unsubscribe()
			initialize()}

		def initialize()  {
		   subscribe(tempSensor, "temperatureMeasurement.temperature", tempHandler, [filterEvents: false])
		   Notification("DTH Sensor is " + tempSensor.displayName +  ", ID : " + tempSensor.deviceNetworkId)
		   processTemp(tempSensor.currentTemperature)
		}

		def tempHandler (evt) {
			def temp = tempSensor.currentTemperature
			processTemp(temp)
			
		}
		
		def processTemp(temp){		
			def cspOnTemp = cspTemp + cspVar
			def cspOffTemp = cspTemp
			def hspOnTemp = hspTemp - hspVar
			def hspOffTemp = hspTemp
			Notification("Current Temperature: "+temp+ ", CSP On: " + cspOnTemp+ ", Off:" +cspOffTemp+ ", HSP On: " + hspOnTemp+ ", Off:" +hspOffTemp)
			if (activated) {
				if (temp > cspOnTemp) cspOnEvent(temp, cspTemp)
				if (temp <= cspOffTemp) cspOffEvent(temp, cspTemp)
				if (temp < hspOnTemp) hspOnEvent(temp, hspTemp)
				if (temp >= hspOffTemp) hspOffEvent(temp, hspTemp)
			}
		}
		
		def cspOnEvent(temp, cspTemp){
			def onSwitches = 0, offSwitches = 0, openSensors = 0, closedSensors = 0
			def str1 = '', str2 = '', str3 = '', str4 = ''

			if (cspONNswitches) {
				cspONNswitches.each { tmp -> (tmp.currentValue("switch") == "off") ? tmp.on():onSwitches++}
				str1  = (onSwitches < cspONNswitches.size()) ? ("Switch(s) Turned On: "+ (cspONNswitches.size()-onSwitches) + " of "+cspONNswitches.size() + "\n") : ''
			}			
			if (cspONFswitches) {
				cspONFswitches.each { tmp -> (tmp.currentValue("switch") == "on") ? tmp.off():offSwitches++}				
				str2  = (offSwitches < cspONFswitches.size()) ? ("Switch(s) Turned Off: " + (cspONFswitches.size()-offSwitches) + " of "+cspONFswitches.size() + "\n") : ''
			}		
			if (cspONNContactSensors) {
				cspONNContactSensors.each { tmp -> (tmp.currentValue("contact") == "closed") ? tmp.open():openSensors++}
				str3  = (openSensors < cspONNContactSensors.size()) ? ("Contact Sensor(s) Opened: " + cspONNContactSensors.size()-openSensors + " of "+cspONNContactSensors.size() + "\n") : ''
			}	
			if (cspONFContactSensors) {
				cspONFContactSensors.each { tmp -> (tmp.currentValue("contact") == "open") ? tmp.close():closedSensors++}
				str4  = (openSensors < cspONFContactSensors.size()) ? ("Contact Sensor(s) Closed: " + cspONFContactSensors.size()-closedSensors + " of "+cspONFContactSensors.size() + "\n") : ''
			}
			def str = "Currently Cooling. Current Temperature: "+temp+ ", CSP: " + cspTemp + "\n"
			((str1 + str2 + str3 + str4) == '') ? '' : Notification(str + str1 + str2 + str3 + str4)
		}		
		
		def cspOffEvent(temp, cspTemp){
			def onSwitches = 0, offSwitches = 0, openSensors = 0, closedSensors = 0
			def str1 = '', str2 = '', str3 = '', str4 = ''

			if (cspOFFNswitches) {
				cspOFFNswitches.each { tmp -> (tmp.currentValue("switch") == "off") ? tmp.on():onSwitches++}
				str1  = (onSwitches < cspOFFNswitches.size()) ? ("Switch(s) Turned On: "+ (cspOFFNswitches.size()-onSwitches) + " of "+cspOFFNswitches.size() + "\n") : ''
			}			
			if (cspOFFFswitches) {
				cspOFFFswitches.each { tmp -> (tmp.currentValue("switch") == "on") ? tmp.off():offSwitches++}				
				str2  = (offSwitches < cspOFFFswitches.size()) ? ("Switch(s) Turned Off: " + (cspOFFFswitches.size()-offSwitches) + " of "+cspOFFFswitches.size() + "\n") : ''
			}		
			if (cspOFFNContactSensors) {
				cspOFFNContactSensors.each { tmp -> (tmp.currentValue("contact") == "closed") ? tmp.open():openSensors++}
				str3  = (openSensors < cspOFFNContactSensors.size()) ? ("Contact Sensor(s) Opened: " + cspOFFNContactSensors.size()-openSensors + " of "+cspOFFNContactSensors.size() + "\n") : ''
			}	
			if (cspOFFFContactSensors) {
				cspOFFFContactSensors.each { tmp -> (tmp.currentValue("contact") == "open") ? tmp.close():closedSensors++}
				str4  = (openSensors < cspOFFFContactSensors.size()) ? ("Contact Sensor(s) Closed: " + cspOFFFContactSensors.size()-closedSensors + " of "+cspOFFFContactSensors.size() + "\n") : ''
			}
			def str = "Cooling is Off. Current Temperature: "+temp+ ", CSP: " + cspTemp + "\n"
			((str1 + str2 + str3 + str4) == '') ? '' : Notification(str + str1 + str2 + str3 + str4)
		}
		
		
		
		def hspOnEvent(temp, hspTemp){
			def onSwitches = 0, offSwitches = 0, openSensors = 0, closedSensors = 0
			def str1 = '', str2 = '', str3 = '', str4 = ''

			if (hspONNswitches) {
				hspONNswitches.each { tmp -> (tmp.currentValue("switch") == "off") ? tmp.on():onSwitches++}
				str1  = (onSwitches < hspONNswitches.size()) ? ("Switch(s) Turned On: "+ (hspONNswitches.size()-onSwitches) + " of "+hspONNswitches.size() + "\n") : ''
			}			
			if (hspONFswitches) {
				hspONFswitches.each { tmp -> (tmp.currentValue("switch") == "on") ? tmp.off():offSwitches++}				
				str2  = (offSwitches < hspONFswitches.size()) ? ("Switch(s) Turned Off: " + (hspONFswitches.size()-offSwitches) + " of "+hspONFswitches.size() + "\n") : ''
			}		
			if (hspONNContactSensors) {
				hspONNContactSensors.each { tmp -> (tmp.currentValue("contact") == "closed") ? tmp.open():openSensors++}
				str3  = (openSensors < hspONNContactSensors.size()) ? ("Contact Sensor(s) Opened: " + hspONNContactSensors.size()-openSensors + " of "+hspONNContactSensors.size() + "\n") : ''
			}	
			if (hspONFContactSensors) {
				hspONFContactSensors.each { tmp -> (tmp.currentValue("contact") == "open") ? tmp.close():closedSensors++}
				str4  = (openSensors < hspONFContactSensors.size()) ? ("Contact Sensor(s) Closed: " + hspONFContactSensors.size()-closedSensors + " of "+hspONFContactSensors.size() + "\n") : ''
			}
			def str = "Currently Heating. Current Temperature: "+temp+ ", HSP: " + hspTemp + "\n"
			((str1 + str2 + str3 + str4) == '') ? '' : Notification(str + str1 + str2 + str3 + str4)
		}		
		
		def hspOffEvent(temp, hspTemp){
			def onSwitches = 0, offSwitches = 0, openSensors = 0, closedSensors = 0
			def str1 = '', str2 = '', str3 = '', str4 = ''

			if (hspOFFNswitches) {
				hspOFFNswitches.each { tmp -> (tmp.currentValue("switch") == "off") ? tmp.on():onSwitches++}
				str1  = (onSwitches < hspOFFNswitches.size()) ? ("Switch(s) Turned On: "+ (hspOFFNswitches.size()-onSwitches) + " of "+hspOFFNswitches.size() + "\n") : ''
			}			
			if (hspOFFFswitches) {
				hspOFFFswitches.each { tmp -> (tmp.currentValue("switch") == "on") ? tmp.off():offSwitches++}				
				str2  = (offSwitches < hspOFFFswitches.size()) ? ("Switch(s) Turned Off: " + (hspOFFFswitches.size()-offSwitches) + " of "+hspOFFFswitches.size() + "\n") : ''
			}		
			if (hspOFFNContactSensors) {
				hspOFFNContactSensors.each { tmp -> (tmp.currentValue("contact") == "closed") ? tmp.open():openSensors++}
				str3  = (openSensors < hspOFFNContactSensors.size()) ? ("Contact Sensor(s) Opened: " + hspOFFNContactSensors.size()-openSensors + " of "+hspOFFNContactSensors.size() + "\n") : ''
			}	
			if (hspOFFFContactSensors) {
				hspOFFFContactSensors.each { tmp -> (tmp.currentValue("contact") == "open") ? tmp.close():closedSensors++}
				str4  = (openSensors < hspOFFFContactSensors.size()) ? ("Contact Sensor(s) Closed: " + hspOFFFContactSensors.size()-closedSensors + " of "+hspOFFFContactSensors.size() + "\n") : ''
			}
			def str = "Heat is off. Current Temperature: "+temp+ ", HSP: " + cspTemp + "\n"
			((str1 + str2 + str3 + str4) == '') ? '' : Notification(str + str1 + str2 + str3 + str4)			
		}

		def Notification(String str){
			 if (pushNotification) {
				sendPush(str)
			 }
		}