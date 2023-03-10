/**
 *  Checking air pollution and Air purifier control2
 *
 *  Copyright 2019 GayoungKoh
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
    name: "Checking air pollution and Air purifier control2",
    namespace: "dianakoh",
    author: "GayoungKoh",
    description: "Checking air pollution and Air purifier control2",
    category: "Green Living",
    iconUrl: "http://cs.sookmyung.ac.kr/~uslab/gy/smartAppMonitor/images/gy.png",
    iconX2Url: "http://cs.sookmyung.ac.kr/~uslab/gy/smartAppMonitor/images/gy.png",
    iconX3Url: "http://cs.sookmyung.ac.kr/~uslab/gy/smartAppMonitor/images/gy.png")


preferences {
	section("This is a smartApp for checking air pollution and control air purifier") { 
    	input(name: "inOrOut", type: "enum", title: "Select whether to use outdoor fine dust value or indoor fine dust value", options: [0: "outdoor", 1: "indoor"])
		input "air1", "capability.airQualitySensor", title: "Select a outdoor device for checking air pollution", reqired: false
        input "air2", "capability.airQualitySensor", title: "Select a indoor device for checking air pollution", required: false
        input "color1", "capability.colorControl", title: "Select a color control device for for checking air pollution", reqired: false
        input "alarm1", "capability.alarm", title: "Select an alarm for checking air pollution", reqired: false
        input "switch1", "capability.switch", title: "Select a switch for air purifier", reqired: false
        input "setpoint", "number", title: "The point of turning on the air purifier (if you do not set the point, default is 80.0(outdoor) or 50.0(indoor))", required: false
        input "settime", "time", title: "Time to send SMS/push notification everyday"
        input(name: "push", type: "enum", title: "if you want to send push notifications?", options: [0: "no", 1: "yes"])
        input("recipients", "contact", title: "Recipients", description: "Send notifications to") {
        	input "phone", "phone", title: "Phone number?", required: false
        }
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
   /* try {
    	schedule("0 0 9 * * ?", sendMessage)
        runEvery10Minutes(executeHandler)
    } catch(e) {
    	log.error "error: executeHandler $e"
    }*/
    //runIn(0, sendMessage)
    schedule(settime, sendMessage)
    runEvery10Minutes(executeHandler)
}

// TODO: implement event handlers
def executeHandler() {
    String airQualityValue
    if("$inOrOut" == "0") {
    	airQualityValue = air1.currentValue("pm10_value")
    }
    else if("$inOrOut" == "1") {
    	airQualityValue = air2.currentValue("in_pm25_value")
    }   
    double airQ = Double.parseDouble(airQualityValue)
    colorHandler(airQualityValue, airQ)
    alarmHandler(airQualityValue, airQ)
    airPurifierControl(airQualityValue, airQ)
}

def sendMessage()
{  
    String airQualityValue = air1.currentValue("pm10_value")
    String airQualityValue2 = air2.currentValue("in_pm25_value")
    double airQ = Double.parseDouble(airQualityValue)
    double airQ2 = Double.parseDouble(airQualityValue2)
    
    /*def msg = "현재 실내 미세먼지수치는 ${airQualityValue2}로 "
    
    if(airQ2 < 25.0) {
    	//msg += "좋음 상태입니다.\n환기하기 좋은 날입니다."
        msg += "좋음 상태입니다."
    }
    else if(airQ2 >= 25.0 && airQ < 50.0) {
    	//msg += "보통 상태입니다.\n마스크를 준비해주세요."
        msg += "보통 상태입니다."
    }
    else if(airQ2 >= 50.0 && airQ < 80.0) {
    	//msg += "나쁨 상태입니다.\n마스크를 준비하시고 외부에 노출되면 유해하므로 주의하세요."
        msg += "나쁨 상태입니다."
    }
    else if(airQ2 >= 80.0) {
    	//msg += "매우 나쁨 상태입니다.\n마스크를 준비하시고 외출을 삼가주세요."
        msg += "매우 나쁨 상태입니다."
    }*/
    
    def msg = "\n현재 외부 미세먼지 수치는 ${airQualityValue}로 "
    
    if(airQ < 30.0) {
    	msg += "좋음 상태입니다.\n환기하기 좋은 날입니다."
    }
    else if(airQ >= 30.0 && airQ < 80.0) {
   	 	msg += "보통 상태입니다.\n마스크를 준비해주세요."
    }
    else if(airQ >= 80.0 && airQ < 150.0) {
    	msg += "나쁨 상태입니다.\n마스크를 준비하시고 외부에 노출되면 유해하므로 주의하세요."
    }
    else if(airQ >= 150.0) {
    	msg += "매우 나쁨 상태입니다.\n마스크를 준비하시고 외출을 삼가주세요."
    }
    if(location.contactBookEnabled) {
    	sendNotificationToContacts(msg, recipients)
    }
    else {
    	if("$push" == "1") {
        	sendPush(msg)
        }
    	if(phone) {
        	sendSms(phone, msg)
        }	
    }
}

def colorHandler(String airQualityValue, double airQ)
{
	if("$inOrOut" == "0") {
    	if(airQ < 30.0) {
        	def color = [name: "blue", hue: 56, saturation: 96]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 30.0 && airQ < 80.0) {
        	def color = [name: "green", hue: 23, saturation: 99]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 80.0 && airQ < 150.0) {
        	def color = [name: "yellow", hue: 15, saturation: 98]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 150.0) {
        	def color = [name: "red", hue: 99, saturation: 97]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}  
    }
    else if("$inOrOut" == "1") {
        if(airQ < 25.0) {
        	def color = [name: "blue", hue: 56, saturation: 96]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 25.0 && airQ < 50.0) {
        	def color = [name: "green", hue: 23, saturation: 99]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 50.0 && airQ < 80.0) {
        	def color = [name: "yellow", hue: 15, saturation: 98]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	}
    	else if(airQ >= 80.0) {
        	def color = [name: "red", hue: 99, saturation: 97]
        	color1.setColor(hue:color.hue, saturation:color.saturation)
    	} 
    }
}

def alarmHandler(String airQualityValue, double airQ) {
    String alarmValue = alarm1.currentValue("alarm")
    log.debug "$alarmValue"
    if("$inOrOut" == "0") {
    	if(setpoint) {
        	if(airQ < setpoint) {
    			if(alarmValue == "both" || alarmValue == "siren" || alarmValue == "strobe"){
        			alarm1.off()
        		}
    		}
    		else if(airQ >= setpoint) {
    			if(alarmValue == "off") {
        			alarm1.both()
        		}
    		}
        }
        else {
    		if(airQ < 80.0) {
    			if(alarmValue == "both" || alarmValue == "siren" || alarmValue == "strobe"){
        			alarm1.off()
        		}
    		}
    		else if(airQ >= 80.0) {
    			if(alarmValue == "off") {
        			alarm1.both()
        		}
    		}
        }
    }
    else if("$inOrOut" == "1") {
    	if(setpoint) {
        	if(airQ < setpoint) {
    			if(alarmValue == "both" || alarmValue == "siren" || alarmValue == "strobe"){
        			alarm1.off()
        		}
    		}
    		else if(airQ >= setpoint) {
    			if(alarmValue == "off") {
        			alarm1.both()
        		}
    		}
        }
        else {
    		if(airQ < 50.0) {
    			if(alarmValue == "both" || alarmValue == "siren" || alarmValue == "strobe"){
        			alarm1.off()
        		}
    		}
    		else if(airQ >= 50.0) {
    			if(alarmValue == "off") {
        			alarm1.both()
        		}
    		}
        }
    }
}

def airPurifierControl(String airQualityValue, double airQ) {
    String switchValue = switch1.currentValue("switch")
    if("$inOrOut" == "0") {
    	if(setpoint) {
        	log.debug "$airQ"
        	if(airQ < setpoint) {
    			if(switchValue == "on") {
        			switch1.off()
                    if(phone)
                    	sendSms(phone, "미세먼지 수치가 ${airQ}(으)로 ${setpoint} 이하 입니다. 공기청정기를 끕니다.")
        		}
    		}
    		else if(airQ >= setpoint) {
    			if(switchValue == "off") {
        			switch1.on()
                    if(phone)
                    	sendSms(phone, "미세먼지 수치가 ${airQ}(으)로 ${setpoint} 이상 입니다. 공기청정기를 켭니다.")
        		}
    		}
        }
        else {
    		if(airQ < 80.0) {
    			if(switchValue == "on") {
        			switch1.off()
                    if(phone)
                    	sendSms(phone, "미세먼지 수치가 ${airQ}(으)로 보통 이하 입니다. 공기청정기를 끕니다.")
        		}
    		}
    		else if(airQ >= 80.0) {
    			if(switchValue == "off") {
        			switch1.on()
                    if(phone)
                    	sendSms(phone, "미세먼지 수치가 ${airQ}(으)로 나쁨 이상 입니다. 공기청정기를 켭니다.")
        		}
    		}
        }
    }
    else if("$inOrOut" == "1") {
    	if(setpoint) {
            if(airQ < setpoint) {
    			if(switchValue == "on") {
        			switch1.off()
        		}
    		}
    		else if(airQ >= setpoint) {
    			if(switchValue == "off") {
        			switch1.on()
        		}
    		}
        }
        else {
       		if(airQ < 50.0) {
    			if(switchValue == "on") {
        			switch1.off()
        		}
    		}
    		else if(airQ >= 50.0) {
    			if(switchValue == "off") {
        			switch1.on()
        		}
    		}
        }
    }
}