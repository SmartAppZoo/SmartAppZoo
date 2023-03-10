/**
 *  Temperature Warning
 *
 *  Author: Zach Naimon
 *	zach@smartthings.com
 *  Date: 2013-06-13
 */
preferences {
	 section("Temperature Sensors"){
		input "exSensor", "capability.temperatureMeasurement", title: "Choose Sensor"
   } 
    section("Temperature "){
		input "minTemp", "decimal", title: "Minimum Temperature"
        input "maxTemp", "decimal", title: "Maximum Temperature"
   }     
    section("Contacting You"){
   		input "phone1", "phone", title: "Phone Number (Optional.  Push Notification will be sent if left blank)", required: false
        input "freq", "decimal", title: "Frequency of notifications (In minutes.  Only one notification will be sent if left blank.)", required: false
       
   }

}


def installed() {
	subscribe(exSensor, "temperature", eTemperatureHandler) 
    
}
def updated() {
	unsubscribe()
	subscribe(exSensor, "temperature", eTemperatureHandler)
   
}
def sendNotifHi() {
        	def exTemp = exSensor.latestValue("temperature")
    		log.debug exTemp
    		def difTempHi = (exTemp - maxTemp)
    		log.debug difTempHi
    		def difTempLo = (minTemp - exTemp)
    		log.debug difTempLo
    
            if (phone1) {
			sendSms(phone1, "The outside temperature is $exTemp degrees, $difTempHi degrees above optimal temperature range.")
     
    		}
        	else{
        	sendPush("The outside temperature is $exTemp degrees, $difTempHi degrees above optimal temperature range.")
        	}
    }
    def sendNotifLo() {
            def exTemp = exSensor.latestValue("temperature")
    		log.debug exTemp
    		def difTempHi = (exTemp - maxTemp)
    		log.debug difTempHi
    		def difTempLo = (minTemp - exTemp)
    		log.debug difTempLo
    
    if (phone1) {
        sendSms(phone1, "The outside temperature is $exTemp degrees, $difTempLo degrees below optimal temperature range.")
        	
    	}
        else{
        sendPush("The outside temperature is $exTemp degrees, $difTempLo degrees below optimal temperature range.")
        }
    } 
    


def eTemperatureHandler(evt){

    
	if(exTemp > maxTemp){
    	if(freq) {
        schedule("0 0/$freq * * * ?", sendNotifHi)
        }
        else{
        sendNotifHi()
        }
    }
    else if(exTemp < minTemp){
    	if(freq) {
        schedule("0 0/$freq * * * ?", sendNotifLo)
        }
        else{
        sendNotifLo()
        }
    }
    
}
    
