definition(
    name: "House temp control",
    namespace: "pdubovyk",
    author: "Pdubovyk",
    description: "Control temp of house based on weather forecast and pre-defined programm",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Home/home1-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Home/home1-icn@2x.png"
)

preferences {

	section("Temp limits") {
    input (name:"winter_temp", type: "decimal" , title: "Winter temp", defaultValue: 10) 
    input (name:"summer_temp", type: "decimal" , title: "Summer temp", defaultValue: 17) 
    }

 	section("Lists of temperatures") {
    input (name:"winter_c_temp", type: "text" , title: "Winter set", defaultValue: "19,23,29,29,28,28,28,21,18,28")     
     input (name:"spring_c_temp", type: "text" , title: "Spring set", defaultValue: "19,20,25,25,25,25,25,21,10,25")
     input (name:"summer_c_temp", type: "text" , title: "Summer set", defaultValue: "14,12,15,15,15,15,23,31,32,23")
	
     }
     
           section("Switch check")
    {
	 input (name:"winter_switch_temp", type: "decimal" , title: "Winter switch", defaultValue: 21.2)  
     input (name:"summer_switch_temp", type: "decimal" , title: "Summer switch", defaultValue: 22) 
          input "tempSwitch",title:"Check temp inside", "capability.thermostat"
	}
     
          section("Check weather scheduler")
    {
		input "checktime01", "time", title: "First 3:00AM",  required: true
        input "checktime02", "time", title: "Second 10:00AM",  required: false        
        input "checktime03", "time", title: "Dinner check 3:00PM",  required: false        
        input "checktime04", "time", title: "Evening check 10:00PM",  required: false        
	}
     
	section("Thermostats") {
		input "tempLivingRoom",title:"01 - Living Room", "capability.thermostat",required: true
        input "tempGuestRoom",title:"01 - Guest Room", "capability.thermostat"
        input "tempUF_LR",title:"01 - UF Living room", "capability.thermostat"
        input "tempUF_K",title:"01 - UF Kitchen", "capability.thermostat"
        input "tempUF_H",title:"01 - UF Halfway", "capability.thermostat"
        input "tempUF_F",title:"01 - UF Foyer", "capability.thermostat"
        input "tempUF_B01",title:"01 - UF Bathrom", "capability.thermostat"
        input "tempBedRoom",title:"02 - BedRoom", "capability.thermostat"
        input "tempChildren",title:"02 - Children", "capability.thermostat"
        input "tempUF_B02",title:"02 - UF Bathrom", "capability.thermostat"

	}
        
   
	
    section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false, defaultValue: "Yes"
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    } 
	
}

def installed() {
    
  updated()
       
}

def updated() {
	//unsubscribe()
	
    state.TemeratureSet = ""
    
	unschedule()
 	checkWeather(false)
    set_schedulers()
    
    runEvery1Hour(check_indoor_temp_hour)

    
}


def set_schedulers()
{
     // 4 schedulers (3AM 10AM, 3PM, 10PM)
    
    log.debug "check 01"
 	schedule(checktime01,"checkWeather_01")
    
    if (checktime02){
    	log.debug "check 02"
        schedule(checktime02,"checkWeather_02")
        }
    if (checktime03){
    	log.debug "check 03"
        schedule(checktime03,"checkWeather_03")
        }
    if (checktime04){
    	log.debug "check 04"
        schedule(checktime04,"checkWeather_04")
        }
}

def check_indoor_temp_hour()
{

log.debug "TEMP_hourly =  $tempSwitch"
def temp_term_check = tempSwitch.temperatureState.numberValue
// def temp_term_check = tempSwitch.currentValue("temperature") 
 
 log.debug "tempSwitch hourly temp  $temp_term_check"



switch (temp_term_check)
 {
 case {state.TemeratureSet == "Winter" && it>winter_switch_temp+0.1}:
 //21.4>21.2+0.1
      checkWeather(true)  
    break;
 case {state.TemeratureSet == "Summer" && it<summer_switch_temp-0.1}:
 //21.5<22.1
 	   checkWeather(true)
 	break; 
case {state.TemeratureSet == "Spring" && (it>summer_switch_temp+0.1 || it<winter_switch_temp-0.1)}:
//22.5>22.1 or 20.9<21.2
//22.5>22.1+0.1 or 20.9<21.2-0.1
 	   checkWeather(true)
break; 
}



}

def checkWeather_01(){
	
    checkWeather(true)
}

def checkWeather_02(){
	
    checkWeather(true)
}

def checkWeather_03(){
	
    checkWeather(true)
}

def checkWeather_04(){
	
    checkWeather(true)
}


def checkWeather(up_date){

 //def temp_winter_C = [19,22,29,29,28,28,28,22,22,28]
 //def temp_spring_C = [19,20,25,25,25,25,28,21,21,28]
 //def temp_summer_C = [19,22,15,15,15,15,28,22,22,28] 

def avg_day = accuweather_current_avgtem()
/*testing purpose*/ 
//def avg_day = 5.3
 
    log.debug "$avg_day"

 def thermostat_set = []
 
 
	log.debug "TEMP =  $tempSwitch"
def temp_term_check = tempSwitch.temperatureState.numberValue
// def temp_term_check = tempSwitch.currentValue("temperature") 
 
 log.debug "tempSwitch temp  $temp_term_check"
 
 switch (avg_day)
 {
 case {it<winter_temp && winter_switch_temp>=temp_term_check}:
 //  -3.5<9.9 && 21.2>=21.2 - winter
 	thermostat_set = winter_c_temp //.split(",")
     log.debug "winter set $avg_day"
     //set winter temperature
     if (state.TemeratureSet == "Winter" && up_date)
     {
     	send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Winter set is continues", "")
     } else
      {
 		send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Winter set is enabled", "push")
        thermostat_updates(thermostat_set)
      }
    
    state.TemeratureSet = "Winter"
    break;
 case {(it>=summer_temp) || (temp_term_check>=summer_switch_temp)}:
   //	2.6>=17  or  (22,3>=22)
  //	15>=17 or 21.5>=22 - spring
  //   2.6>=17 or 21.5>=22 - spring
  
 //>summer_temp
 	log.debug "summer set $avg_day"
 	thermostat_set = summer_c_temp //.split(",")
     if (state.TemeratureSet == "Summer" && up_date)
     {
   		 send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Summer set is continues", "")
     } else
      {
    	 send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Summer set is enabled", "push")
         thermostat_updates(thermostat_set)
	  }
    state.TemeratureSet = "Summer"
 	break; 
 default:
 	log.debug "spring set $avg_day"
 	thermostat_set = spring_c_temp//.split(",")
     if (state.TemeratureSet == "Spring" && up_date)
     {
      	 send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Spring set is continues", "")
     } else
      {
   		 send("Day-Night temp is $avg_day °C and check temp is $temp_term_check °C. Spring set is enabled", "push")
         thermostat_updates(thermostat_set)
    	}    
    
    state.TemeratureSet = "Spring"
 	break;
 // case {it>=summer_temp}:

}
 
 //log.debug "$thermostat_set"
 //send("SET1 $thermostat_set")

 
 

}

def thermostat_updates(tempreture_up)
{

log.debug "$tempreture_up"

//def list_temerature = tempreture_up.split(",") // impementation for text type
def list_temerature = tempreture_up.split(",")

if (list_temerature!=null){
	if (list_temerature!="") {
			def LivingRoom_t = list_temerature[0]    
            tempLivingRoom.setTempHM(LivingRoom_t, true)
            
            def GuestRoom_t = list_temerature[1]
            tempGuestRoom.setTempHM(GuestRoom_t, true)
            
            def UF_LR_t = list_temerature[2]
            tempUF_LR.setTempHM(UF_LR_t, true)
            
            def UF_K_t = list_temerature[3]
            tempUF_K.setTempHM(UF_K_t, true)
            
            def UF_H_t = list_temerature[4]
            tempUF_H.setTempHM(UF_H_t, true)
            
            def UF_F_t = list_temerature[5]
            tempUF_F.setTempHM(UF_F_t, true)
            
            def UF_B01_t = list_temerature[6]
            tempUF_B01.setTempHM(UF_B01_t, true)
            log.debug "B01 $UF_B01_t"
            
            def BedRoom_t = list_temerature[7]
            tempBedRoom.setTempHM(BedRoom_t, true)
            
            def Children_t = list_temerature[8]
            tempChildren.setTempHM(Children_t, true)
            
            def UF_B02_t = list_temerature[9]
            tempUF_B02.setTempHM(UF_B02_t, true)
            log.debug "B02 $UF_B02_t"
		}
    }
}





private send(msg, typem) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
        	if (typem == "push") {
            	log.debug("sending push message")
            	sendPush(msg)
            } else
            {
              log.debug("sending send notification event")
              sendEvent(name: $msg, value: 0)
              sendNotificationEvent(msg)
            }
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}



def accuweather_current_avgtem()
{

	///http://dataservice.accuweather.com/forecasts/v1/daily/5day/1218844?apikey=MNRps0GCqAbArykpkM5zj6bPShIRKT2y&details=true&metric=true"
	def keylocation = "1218844" // location key based on accuweather data - you can get that on their website
    def APIkey = "MNRps0GCqAbArykpkM5zj6bPShIRKT2y"
    def details = "true"
	def metrics = "true"
	
    def  response = ""
	def  temp_result = -100 

	def URI_HTTP ="http://dataservice.accuweather.com/forecasts/v1/daily/5day/$keylocation?apikey=$APIkey&details=$details&metric=$metrics" 
	//log.debug "URI_HTTP: $URI_HTTP"
    
	try
    	{
                
                
                	httpGet(uri: URI_HTTP,contentType: 'application/json',)
                				{resp ->           
                          //  log.debug "resp data: ${resp.data}"
                          //  log.debug "result: ${resp.data.result}" 
									response = resp.data

								}
							
							//log.debug "result: $response"
                            
                            
                            //def rainDay = response?.DailyForecasts?.Day?.TotalLiquid?.Value
                            //def rainNight = response?.DailyForecasts?.Night?.TotalLiquid?.Value
                            def day_temp = response?.DailyForecasts?.Temperature?.Maximum?.Value
                            def night_temp = response?.DailyForecasts?.Temperature?.Minimum?.Value
                            
                            def day_temp_RF = response?.DailyForecasts?.RealFeelTemperature?.Maximum?.Value
                            def night_temp_RF = response?.DailyForecasts?.RealFeelTemperature?.Minimum?.Value
                            
                            

							//log.debug "temp day result: $day_temp"
                            //log.debug "temp night result: $night_temp"
                            
                           // log.debug "temp day RF result: $day_temp_RF"
                           // log.debug "temp night RF result: $night_temp_RF"
                           //CHECK DAY temp
                           	if (day_temp[0]<day_temp_RF[0])
                            {
                            	temp_result = day_temp[0]
                            }
                            else
                            {
                            	temp_result = day_temp_RF[0]
                            }
                           
                           
                           ///CHECK Night Temp
                           	if (night_temp[0]<night_temp_RF[0])
                            {
                            	temp_result = temp_result+night_temp[0]
                            }
                            else
                            {
                            	temp_result = temp_result+night_temp_RF[0]
                            }
                            
                            //AVG calcuation
                            temp_result = temp_result/2
                            
							//def today_rain =  rainDay[0]+rainNight[0]
                            //def tomorrow_rain =  rainDay[1]+rainNight[1]
							
                            //log.debug "day: $rainDay"
                            //log.debug "night: $rainNight"
                            //log.debug "rain: $today_rain & $tomorrow_rain"                                    
                            
							//rain_result << today_rain
                            //rain_result << tomorrow_rain
                            
                            //log.debug "rain_result: $rain_result"                                              		
         }
   		catch (e)
		{
        sendMessage("Check: Exception $e")
		}
   
     /*if (!temp_result.isEmpty())
		{
        	//log.debug "return rain_result: $rain_result"     
			return temp_result
		}  else
        {*/
			//log.debug "fasle return rain_result: $rain_result"     
   			return temp_result 
   	//	}
}