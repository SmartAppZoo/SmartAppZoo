/**
 *  wattering ladscape
 *
 *  Copyright 2021 Pavlo Dubovyk
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
    name: "wattering ladscape",
    namespace: "pavel-dpa",
    author: "Pavlo Dubovyk",
    description: "control watering in my terirory",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/* TODO
 
 - schedule for each valave
 - valve by mm per hour
 - valve by mm per hour + weather today yesterday - tomorrow

*/




preferences {

		       
        section("Timers")
    {
		input "Sunrize_Sunset_check", "bool", title: "Would you like use sunerise and sunset?",  required: true
        input "Sunrize_check_info", "bool", title: "Would you like use sunerise?",  required: true
        input "Sunset_check_info", "bool", title: "Would you like use suneset?",  required: true
        input "Sunrize_delay", "number", title: "Sunrize delay", required: false, defaultValue: 0
        input "Sunset_delay", "number", title: "Sunset delay", required: false, defaultValue: 0
		

	}


	section("Timers")
    {
		input "start_before_W", "time", title: "First end (normally 4:30AM)",  required: false
        input "start_after_W", "time", title: "Second start (21:50AM )", required: false
        
	}
    section("Valves to adjust...")
    {
		input "valve_main", "capability.switch",description: "Main valve", required: true, multiple: false
        
        input "valve01", "capability.switch", required: true, multiple: false
		input "valve01_Timer", type: "number" , title: "Zone One time", description: "Zone One Time", required: false, defaultValue: 0        
        input "valve01_count", type: "number" , title: "Zone One count", description: "Zone One Count", required: false, defaultValue: 1        
        
        input "valve02", "capability.switch", required: false, multiple: false
        input "valve02_Timer", type: "number" , title: "Zone Two time", description: "Zone Two Time", required: false, defaultValue: 0
		input "valve02_count", type: "number" , title: "Zone Two count", description: "Zone Two Count", required: false, defaultValue: 1        

        
        input "valve03", "capability.switch", required: false, multiple: false
        input "valve03_Timer", "number", title: "Zone Three time", description: "Zone Three Time", required: false, defaultValue: 0
		input "valve03_count", type: "number" , title: "Zone Three count", description: "Zone Three Count", required: false, defaultValue: 1        

        
        input "valve04", "capability.switch", required: false, multiple: false
        input "valve04_Timer", "number", title: "Zone Four", description: "Zone Four Time", required: false, defaultValue: 0
        input "valve04_count", type: "number" , title: "Zone Four count", description: "Zone Four Count", required: false, defaultValue: 1        
        
        input "valve05", "capability.switch", required: false, multiple: false
        input "valve05_Timer", "number", title: "Zone Five", description: "Zone Five Time", required: false, defaultValue: 0
        input "valve05_count", type: "number" , title: "Zone Five count", description: "Zone Five Count", required: false, defaultValue: 1        
        
        input "valve06", "capability.switch", required: false, multiple: false
        input "valve06_Timer", "number", title: "Zone Six", description: "Zone Six Time", required: false, defaultValue: 0
        input "valve06_count", type: "number" , title: "Zone Six count", description: "Zone Six Count", required: false, defaultValue: 1        
        
        input "valve07", "capability.switch", required: false, multiple: false
        input "valve07_Timer", "number", title: "Zone Seven", description: "Zone Seven Time", required: false, defaultValue: 0
        input "valve07_count", type: "number" , title: "Zone Seven count", description: "Zone Seven Count", required: false, defaultValue: 1        
        
        input "valve08", "capability.switch", required: false, multiple: false
        input "valve08_Timer", "number", title: "Zone Eight", description: "Zone Eight Time", required: false, defaultValue: 0
        input "valve08_count", type: "number" , title: "Zone Eight count", description: "Zone Eight Count", required: false, defaultValue: 1        
        
	}
	section("Send Notifications?") {
               input "people", "capability.presenceSensor", multiple: true

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
	// TODO: subscribe to attributes, devices, locations, etc.
    
    
    //DAYS to wattering
    // Symetric waterind one day morning one day evening
    
    // reset order start
	//state.order_manage = 0

    
    set_schedulers()

    
}


def set_schedulers()
{
   
	unschedule()
	state.VALVE_NUMBER = 1
    state.VALVE_SESSION = 1
    
    def list_max_val = []
    
    list_max_val << valve01_count
    list_max_val << valve02_count
    list_max_val << valve03_count
    list_max_val << valve04_count
    list_max_val << valve05_count
    list_max_val << valve06_count
    list_max_val << valve07_count
    list_max_val << valve08_count
    
    
    state.MAX_VALVE_SESSION = list_max_val.max()
       
       log.debug "Max sessions count: ${state.MAX_VALVE_SESSION}"


   
    
           //calculate the offset
    def v_time = valve01_Timer.toInteger()*valve01_count.toInteger()
    if (valve02_Timer) {v_time = v_time+valve02_Timer.toInteger()*valve02_count.toInteger()}
    if (valve03_Timer) {v_time = v_time+valve03_Timer.toInteger()*valve03_count.toInteger()}
    if (valve04_Timer) {v_time = v_time+valve04_Timer.toInteger()*valve04_count.toInteger()}
    if (valve05_Timer) {v_time = v_time+valve05_Timer.toInteger()*valve05_count.toInteger()}
    if (valve06_Timer) {v_time = v_time+valve06_Timer.toInteger()*valve06_count.toInteger()}
    if (valve07_Timer) {v_time = v_time+valve07_Timer.toInteger()*valve07_count.toInteger()}
    if (valve08_Timer) {v_time = v_time+valve08_Timer.toInteger()*valve08_count.toInteger()}




    
	   
   
    if (Sunrize_Sunset_check)
    {
    
    	def  Sunrize_delay_FULL = -1*(Sunrize_delay+v_time)
    
   		def Sunset_Sunrise = getSunriseAndSunset(sunriseOffset: Sunrize_delay_FULL, sunsetOffset: Sunset_delay)

		log.debug "GET SUN SET: ${Sunset_Sunrise}"

   
   		def sunset_offset = Sunset_Sunrise.sunset
  		def sunrise_offset = Sunset_Sunrise.sunrise
   
   
   		log.debug "Sunrise: ${sunrise_offset}"
   		log.debug "Sunsets: ${sunset_offset}"
    
   		log.debug "setup schedules are using sunset and sunerise"
   
   
			
            
            if (Sunrize_check_info){
				schedule(sunrise_offset,wattering)
                sendMessage("watterind setuped to : $sunrise_offset")
				log.debug "schedules sunrise wattering for: $sunrise_offset"
                }
       		
            if (Sunset_check_info) {
        		schedule(sunset_offset,wattering)
                sendMessage("watterind setuped to : $sunset_offset")
        		log.debug "schedules sunset wattering for: $sunset_offset"
                }

	    schedule("24 00 * * * ?", set_schedulers)
        log.debug "schedules check for: 00:00 AM"
        	//	schedule("0 30 0 ? * MON-SUN", set_schedulers)   
    }
    else {
    
	log.debug "setup schedules just by time"
	
           
    
    
    if (start_before_W){

	    def processing_time = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", start_before_W)
		def start_b_w_time = new Date( processing_time.time - v_time*60 * 1000).format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
		def processing_time_Final = Date.parse("yyyy-MM-dd'T'HH:mm:ss", start_b_w_time)
		//def sch_string =  "00"+processing_time_Final.hours + " " + processing_time_Final.minutes+ " * * * ?"	
        def sch_string =  "0 "+processing_time_Final.minutes+" "+processing_time_Final.hours + " ? * MON-SUN"	
    
		//schedule("0 30 0 ? * MON-SUN", set_schedulers)   
    
       schedule(sch_string,wattering)
       sendMessage("watterind setuped to : $processing_time_Final")
   		log.debug "schedules wattering for: $sch_string"        
        
        }
    
    if (start_after_W){
    
    	def processing_time_Final_A = Date.parse("yyyy-MM-dd'T'HH:mm:ss", start_after_W)
		//def sch_string_A =  processing_time_Final_A.hours + " " + processing_time_Final_A.minutes+ " * * * ?"	
        def sch_string_A =  "0 "+processing_time_Final_A.minutes+" "+processing_time_Final_A.hours + " ? * MON-SUN"	
        
        schedule(sch_string_A,wattering)
        sendMessage("watterind setuped to : $processing_time_Final_A")
        log.debug "schedules wattering for: $sch_string_A"     
        }
    }   
}

// TODO: implement event handlers

def wattering ()
{
    log.debug "wattering start session ${state.VALVE_SESSION}"
    
    
    
    
    valve_main.on()
	log.debug "valve_main ON"


	if (valve01) {valve01.off()}
	if (valve02) {valve02.off()}
	if (valve03) {valve03.off()}
    if (valve04) {valve04.off()}
    if (valve05) {valve05.off()}
    if (valve06) {valve06.off()}
    if (valve07) {valve07.off()}
    if (valve08) {valve08.off()}


if (state.VALVE_SESSION.toInteger()==1 && state.VALVE_NUMBER.toInteger()==1) 

	{
			sendMessage ("Wattering is starting")

	}


switch (state.VALVE_NUMBER)
 {
 case { it==1}:
 	if (valve01) {
  		if (valve01_Timer.toInteger()*valve01_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve01_count.toInteger())
        {
        	log.debug "wattering v1 start for, min $valve01_Timer"
        	def v1_time = valve01_Timer.toInteger()
        	valve01.on()
        	runIn(v1_time*60,valves_off)      
        } else {runIn(1,valves_off)}
        	
       } else
        	{         // GO NEXT VALVE
        	runIn(1,valves_off)
            }
 	  break;
 case {it==2}:
      if (valve02) {  
        if (valve02_Timer.toInteger()*valve02_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve02_count.toInteger())
        {
        	log.debug "wattering v2 start for, min $valve02_Timer"
        	def v2_time = valve02_Timer.toInteger()
        	valve02.on()
        	runIn(v2_time*60,valves_off)    
         } else {runIn(1,valves_off)}
   		} else {
         // GO NEXT VALVE
         runIn(1,valves_off)}
    
    break; 
	
 case {it==3}:
 		if (valve03) {
          if (valve03_Timer.toInteger()*valve03_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve03_count.toInteger())
          {
                  log.debug "wattering v3 start for, min $valve03_Timer"
        		  def v3_time = valve03_Timer.toInteger()
       		  valve03.on()
        		  runIn(v3_time*60,valves_off) 
        	} else {runIn(1,valves_off)}
        } else {runIn(1,valves_off)}
   	break; 
	
 case {it==4}:
 	if (valve04) {
        if (valve04_Timer.toInteger()*valve04_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve04_count.toInteger())
          {
                log.debug "wattering v4 start for, min $valve04_Timer"
                def v4_time = valve04_Timer.toInteger()
               valve04.on()
                runIn(v4_time*60,valves_off)    
        	} else {runIn(1,valves_off)}
        } else {runIn(1,valves_off)}
   	break; 
	
 case {it==5}:
 	if (valve05) {
        if (valve05_Timer.toInteger()*valve05_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve05_count.toInteger())
          {
            log.debug "wattering v5 start for, min $valve05_Timer"
            def v5_time = valve05_Timer.toInteger()
           valve05.on()
            runIn(v5_time*60,valves_off)    
       	  } else {runIn(1,valves_off)}
        } else {runIn(1,valves_off)}
   	break; 
	
 case {it==6}:
 	if (valve06) {
        if (valve06_Timer.toInteger()*valve06_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve06_count.toInteger())
          {
                log.debug "wattering v6 start for, min $valve06_Timer"
                def v6_time = valve06_Timer.toInteger()
               valve06.on()
                runIn(v6_time*60,valves_off)    
        	} else {runIn(1,valves_off)}
        } else {runIn(1,valves_off)}
   	break; 
	
 case {it==7}:
 	if (valve07) {
        if (valve07_Timer.toInteger()*valve07_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve07_count.toInteger())
          {
            log.debug "wattering v7 start for, min $valve07_Timer"
            def v7_time = valve07_Timer.toInteger()
           valve07.on()
            runIn(v7_time*60,valves_off)    
        	} else {runIn(1,valves_off)}
        } else {runIn(1,valves_off)}
   	break; 
	
 case {it==8}:
 	if (valve08) {
        if (valve08_Timer.toInteger()*valve08_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=valve08_count.toInteger())
          {
            log.debug "wattering v8 start for, min $valve08_Timer"
            def v8_time = valve08_Timer.toInteger()
            valve08.on()
            runIn(v8_time*60,valves_off)    
        	} else {runIn(1,valves_off)}
        } else {
        
        log.debug "valve v8 exiting"
        runIn(1,valves_off)
        }
   	break; 
   }


}


def valves_off()
{

switch (state.VALVE_NUMBER)
 {
 case {it==1}:
		if (valve01) {valve01.off()
        log.debug "wattering v1 finish"}
        break;
 case {it==2}:
		if (valve02) {valve02.off()
        log.debug "wattering v2 finish"}
 		break;
case {it==3}:
		if (valve03) {valve03.off()
        log.debug "wattering v3 finish"}
 		break;
case {it==4}:
		if (valve04) {valve04.off()
        log.debug "wattering v4 finish"}
 		break;
case {it==5}:
		if (valve05) {valve05.off()
        log.debug "wattering v5 finish"}
 		break;
case {it==6}:
		if (valve06) {valve06.off()
        log.debug "wattering v6 finish"}
 		break;
case {it==7}:
		if (valve07) {valve07.off()
        log.debug "wattering v7 finish"}
 		break;
case {it==8}:
		if (valve08) {valve08.off()
        log.debug "wattering v8 finish"}
 		break;
 }
 		

state.VALVE_NUMBER = state.VALVE_NUMBER + 1

if (state.VALVE_NUMBER.toInteger()<9)
	{
    	 
	  log.debug "GO TO NEXT VALVE ${state.VALVE_NUMBER}"
      runIn(1,wattering)
    }
    else
    { 
    	//GO TO NEXT SESSION
		
        log.debug "Prepare next session"
        state.VALVE_NUMBER = 1
    	state.VALVE_SESSION = state.VALVE_SESSION+1  
        
		if (state.VALVE_SESSION.toInteger()>state.MAX_VALVE_SESSION.toInteger())
            {
				
                log.debug "EXIT"
                runIn(1,vallve_all_off)    
            }
            else
            { 

            log.debug "Next session starting ${state.VALVE_SESSION} from ${state.MAX_VALVE_SESSION}"

            //continue wattering - next session
            runIn(1,wattering) 
            
            }
    }
    
   

}


def vallve_all_off()
{
	log.debug "all valves off - START"
	
    state.VALVE_NUMBER = 1
	state.VALVE_SESSION = 1

    
    
    valve_main.off()
	if (valve01) {valve01.off()}
    if (valve02) {valve02.off()}
    if (valve03) {valve03.off()}
    if (valve04) {valve04.off()}
    if (valve05) {valve05.off()}
    if (valve06) {valve06.off()}
    if (valve07) {valve07.off()}
    if (valve08) {valve08.off()}
    
	log.debug "all valves off - DONE"

	sendMessage ("Wattering has done")
       
    
}

def sendMessage(message)
{
	def stamp = new Date().format('hh:mm:ss ', location.timeZone)
    
  	if (people)
            {
				sendPush(stamp + "$app.label " + message)
             }
             
    /*
    if (location.contactBookEnabled && recipients) 
   	{
    	sendNotificationToContacts(stamp + "$app.label " + message, recipients)
   	}/*
   	else
  	{
   		sendSms(phone, stamp + "$app.label " + message)
  	}*/
    
    log.debug "MESSAGE: $stamp $app.label $message"
}