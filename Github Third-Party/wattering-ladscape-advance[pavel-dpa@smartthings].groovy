/**
 *  wattering ladscape advance
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
    name: "wattering ladscape_advance",
    namespace: "pavel-dpa",
    author: "Pavlo Dubovyk",
    description: "advance control watering in my terirory",
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
		input "valve_main", "capability.switch",title: "Main valve", required: true, multiple: false

		//input "water_valves", "capability.switch",title: "Valves", required: true, multiple: true


        input "valve01", "capability.switch", title: "Zone one valve", required: true, multiple: false
		input "valve01_first", type: "text" , title: "Zone one morning run", required: false, defaultValue: "5,2,5,2,5,2,5,2,5,2,5,2,5,2"
        input "valve01_second", type: "text" , title: "Zone one evening run",required: false, defaultValue: "5,1,5,1,5,1,5,1,5,1,5,1,5,1"        
        
        input "valve02", "capability.switch", title: "Zone two valve", required: false, multiple: false
        input "valve02_first", type: "text" , title: "Zone two morning run", required: false, defaultValue: "4,2,4,2,4,2,4,2,4,2,4,2,4,2"
        input "valve02_second", type: "text" , title: "Zone two evening run",required: false, defaultValue: "3,2,3,2,3,2,3,2,3,2,3,2,3,2"        
        
	
        
        input "valve03", "capability.switch", title: "Zone Three valve", required: false, multiple: false
      	input "valve03_first", type: "text" , title: "Zone Three morning run", required: false, defaultValue: "0,0,0,0,0,0,0,0,0,0,0,0,0,0"
        input "valve03_second", type: "text" , title: "Zone Three evening run",required: false, defaultValue: "0,0,0,0,0,0,0,0,0,0,0,0,0,0"              

        
        input "valve04", "capability.switch", title: "Zone Four valve", required: false, multiple: false
      	input "valve04_first", type: "text" , title: "Zone Four morning run", required: false, defaultValue: "5,1,10,1,5,1,10,1,5,1,7,1,5,1"
        input "valve04_second", type: "text" , title: "Zone Four evening run",required: false, defaultValue: "5,1,0,1,5,1,0,1,5,1,0,1,5,1"         
        
        
        input "valve05", "capability.switch", title: "Zone Five valve", required: false, multiple: false
      	input "valve05_first", type: "text" , title: "Zone Five morning run", required: false, defaultValue: "7,3,7,3,7,3,7,3,7,3,7,3,7,3"
        input "valve05_second", type: "text" , title: "Zone Five evening run",required: false, defaultValue: "5,2,5,2,5,2,5,2,5,2,5,2,5,2"         
        
        
        input "valve06", "capability.switch", title: "Zone Six valve", required: false, multiple: false
        input "valve06_first", type: "text" , title: "Zone Six morning run", required: false, defaultValue: "0,0,0,0,0,0,0,0,0,0,0,0,0,0"
        input "valve06_second", type: "text" , title: "Zone Six evening run",required: false, defaultValue: "0,0,0,0,0,0,0,0,0,0,0,0,0,0"         
        
        
        input "valve07", "capability.switch", title: "Zone Seven valve", required: false, multiple: false
		input "valve07_first", type: "text" , title: "Zone Seven morning run", required: false, defaultValue: "6,4,6,4,6,4,6,4,6,4,6,4,6,4"
        input "valve07_second", type: "text" , title: "Zone Seven evening run",required: false, defaultValue: "5,2,5,2,5,2,5,2,5,2,5,2,5,2"         

        
      
        input "valve08", "capability.switch", title: "Zone Eight valve", required: false, multiple: false
		input "valve08_first", type: "text" , title: "Zone Eight morning run", required: false, defaultValue: "7,3,7,3,7,3,7,3,7,3,7,3,7,3"
        input "valve08_second", type: "text" , title: "Zone Eight evening run",required: false, defaultValue: "4,2,4,2,4,2,4,2,4,2,4,2,4,2"      
        
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
    unschedule()
	initialize()
    
}

def initialize() {
	
    
    /* TO DO
    #1 - 	scheduler by day of week ????
        ++++++++++	- less empty session runs
            - maybe easy scheduling
     		 -flexible time for each day
            
    #2 -  Symetric waterind one day morning one day evening
    
    #3 -  sunset and sunrise by event - subscription https://github.com/SmartThingsCommunity/Code/blob/master/smartapps/sunrise-sunset/turn-on-at-sunset.groovy
    
    #4 -  wattering by weather
    */
    
    // reset order start
	state.order_manage = 1

    
    set_schedulers(true)
	
    // fixing bug when order = 1 however time to run has reach out
 	/*  def second_check = new Date(new Date().time + (30 * 60 * 1000)) //30 min
	  	runOnce(second_check, set_schedulers(false))
		log.debug "Second check for ${second_check}"
	*/

    
}

def max_session_valves()
{
	def result = 0
    
     //def list_max_val = valve01_first+","+valve01_second+","+valve02_first+","+valve02_second+","+valve03_first+","+valve03_second+","+valve04_first+","+valve04_second
    //	list_max_val = list_max_val+"," + valve05_first+","+valve05_second+","+valve06_first+","+valve06_second+","+valve07_first+","+valve07_second+","+valve08_first+","+valve08_second
    
    
    def list_max_val = state.valve01_count+","+state.valve02_count+","+state.valve03_count+","+state.valve04_count+","+state.valve05_count+","+state.valve06_count+","+state.valve07_count+","+state.valve08_count
    
    
    // DEFINE MAX SESSION COUNT ----- TODO ---- REORDER AFTER DAY OF WEEK AND ORDER 
   			log.debug "list_max_val: ${list_max_val}"


		def list_max_val_process = list_max_val.split(",")
        
    // log.debug "SIZE = ${list_max_val_process.size()}"
 
    	def ttt=0
    	//for (int i=1; i<=list_max_val_process.size(); i+=2) 
		for (int i=1; i<list_max_val_process.size(); i++)         
    		{
        //		log.debug "i==== $i"
        		if (list_max_val_process[i]) {ttt= list_max_val_process[i]}
				//log.debug "ttt==== ${ttt}"

    	 		if (result <  ttt) 
                {
       //        log.debug "NUM ${i} = ${ttt}"
                result = ttt
                }
        }
        
  
    
    return result

}

def day_week_valve()
{

		def result = 0

// Define day of week

  			def df = new java.text.SimpleDateFormat("EEEE")
    		// Ensure the new date object is set to local time zone
    		df.setTimeZone(location.timeZone)
    		def day = df.format(new Date())
            
    
    
		switch (day)
 			{
 				case {it=="Monday"}:
                	result = 1
                	break;
                case {it=="Tuesday"}:
                	result = 2
                	break;
                case {it=="Wednesday"}:
                	result = 3
                	break;
                case {it=="Thursday"}:
                	result = 4
                	break;
                case {it=="Friday"}:
                	result = 5
                	break;
                case {it=="Saturday"}:
                	result = 6
                	break;
                case {it=="Sunday"}:
                	result = 7
                	break;                    
                    
            }
    
    return result            

}

def order_check_valve()
{

	def result = 0
		
        def time_first_check = null
		def time_second_check = null
        
         //get valves data to check offset
         //runIn(10,correct_valves_data(1))    
	   	def tt_3 = correct_valves_data(1)
		//calculate the offset to check
   		def v_time = calculate_the_offset()
    

                
		 if (Sunrize_Sunset_check)
    		{
				def  Sunrize_delay_FULL = -1*(Sunrize_delay+v_time)
    			def Sunset_Sunrise = getSunriseAndSunset(sunriseOffset: Sunrize_delay_FULL, sunsetOffset: Sunset_delay)
				log.debug "GET SUN SET: ${Sunset_Sunrise}"
				
                def sunset_offset = Sunset_Sunrise.sunset
                def sunrise_offset = Sunset_Sunrise.sunrise

				// ISSUE TODO when on middle however setuped only sunrize

            	if (Sunrize_check_info) {time_first_check = sunrise_offset}
                if (Sunset_check_info)  {time_second_check = sunset_offset}
            }
            else
            {
              time_first_check = start_before_W

			  time_second_check = start_after_W
            }
    		
            

			log.debug "first time to check: ${time_first_check}"
			log.debug "second time to check: ${time_second_check}"
                
                
           if ((time_first_check) && (time_second_check))
					{
                		def between = timeOfDayIsBetween(time_first_check, time_second_check, new Date(), location.timeZone)
						if (between)
                        	{
                            	// next schedule After - sunset
                                	result= 2
                            } else        
                            {
								
                                //to define result = 3 have to be defined after  time_second_check
								
                                 def midnightTime = (new Date().clearTime() + 1)
                                     log.debug "midnightTime : ${midnightTime}"

								def between_second = timeOfDayIsBetween(time_first_check, midnightTime, new Date(), location.timeZone)
                            		if (between_second)
                                    {
                                    	// next schedule BEFORE - sunrise	next day
										result = 3
                                    }
                                    else
                                    {   
                                    // next schedule BEFORE - sunrise
                                    	result = 1
                                    }
                                   
                            }

                        } else
                        {
						
						   if (time_first_check) 
                           	{
                           		def midnightTime = (new Date().clearTime() + 1)
                           		def between_3 = timeOfDayIsBetween(time_first_check, midnightTime, new Date(), location.timeZone)
                                if (between_3)
                                    {
                                    	// next schedule BEFORE - sunrise	next day
										result = 3
                                    }
                                    else
                                    {   
                                    // next schedule BEFORE - sunrise
                                    	result = 1
                                    }
                                
							}
                           
                        	else
                        	if (time_second_check) 
                            {
                            	
                                def midnightTime = (new Date().clearTime() + 1)
                           		def between_4 = timeOfDayIsBetween(time_second_check, midnightTime, new Date(), location.timeZone)
                                if (between_4)
                                    {
                                    	// next schedule BEFORE - sunrise	next day
										result = 3
                                    }
                                    else
                                    {   
                                    // next schedule BEFORE - sunrise
                                    	result = 2
                                    }
                                
							}
                            
                            

						}
	
    log.debug "PRE-ORDER: ${result}"

    return result       
    

}

def correct_valves_data(order_manage_valve)
{
	def result = 0
    def week_day_num = day_week_valve()
    
    
    if (order_manage_valve ==3)
    	{
		
        order_manage_valve =1
        
        week_day_num = week_day_num +1
        if (week_day_num>7)
        	{
        		week_day_num = 1
        	}
    	}
    
        log.debug "Second PRE-ORDER: ${order_manage_valve}"

    
 	if (order_manage_valve ==1)
        {
        	def v1_list = valve01_first.split(",")
        	state.valve01_Timer = v1_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        	state.valve01_count = v1_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            if (valve02) {
					
                    def v2_list = valve02_first.split(",")
            		state.valve02_Timer = v2_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 }
        			state.valve02_count = v2_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            			}
			if (valve03) {   
					
                    def v3_list = valve03_first.split(",")
            		state.valve03_Timer = v3_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve03_count = v3_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            	}
            if (valve04) {
                    def v4_list = valve04_first.split(",")
					state.valve04_Timer = v4_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve04_count = v4_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            	}
            if (valve05) {
                    def v5_list = valve05_first.split(",")
          			state.valve05_Timer = v5_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve05_count = v5_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            		}
            if (valve06) {
                    def v6_list = valve06_first.split(",")
					state.valve06_Timer = v6_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve06_count = v6_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            	}
            if (valve07) {
                    def v7_list = valve07_first.split(",")
					state.valve07_Timer = v7_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve07_count = v7_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
            if (valve08) {
                    def v8_list = valve08_first.split(",")
					state.valve08_Timer = v8_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve08_count = v8_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            	}
            
            
         }
        
        if (order_manage_valve ==2)
        {
			def v1_list = valve01_second.split(",")
        	state.valve01_Timer = v1_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        	state.valve01_count = v1_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            if (valve02) {
					def v2_list = valve02_second.split(",")
            		state.valve02_Timer = v2_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve02_count = v2_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
            if (valve03) {
					def v3_list = valve03_second.split(",")
            		state.valve03_Timer = v3_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve03_count = v3_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
            if (valve04) {
					def v4_list = valve04_second.split(",")
					state.valve04_Timer = v4_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve04_count = v4_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
            	}
            if (valve05) {
					def v5_list = valve05_second.split(",")
					state.valve05_Timer = v5_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve05_count = v5_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
            if (valve06) {
					def v6_list = valve06_second.split(",")
					state.valve06_Timer = v6_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve06_count = v6_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
			if (valve07) {
					def v7_list = valve07_second.split(",")
					state.valve07_Timer = v7_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve07_count = v7_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                    }
			if (valve08) {                    
					def v8_list = valve08_second.split(",")
					state.valve08_Timer = v8_list[((week_day_num-1)*2)] //0 2 4 6 8 10 12 
        			state.valve08_count = v8_list[(week_day_num*2-1)] //1 3 5 7 9 11 13
                 }
         }
     return result            
  
}

def setup_valves_schedulers(time_delay_valve,message_type)
{
	
    def result = 0
    
if (Sunrize_Sunset_check)
    {
    	
        def  Sunrize_delay_FULL = -1*(Sunrize_delay+time_delay_valve)

    
   		def Sunset_Sunrise = getSunriseAndSunset(sunriseOffset: Sunrize_delay_FULL, sunsetOffset: Sunset_delay)

		log.debug "GET SUN SET: ${Sunset_Sunrise}"

    	
   		def sunset_offset = Sunset_Sunrise.sunset.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
  		def sunrise_offset =Sunset_Sunrise.sunrise.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
          
   
   		log.debug "Sunrise - offset: ${sunrise_offset}"
   		log.debug "Sunsets + offset: ${sunset_offset}"
    
   		log.debug "setup schedules are using sunset and sunerise"
   
   		def date_h= new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
			
            //shedule for this day - state.order_manage =1 
            if ((Sunrize_check_info) && (state.order_manage ==1) //&& (date_h<=sunrise_offset))
            	){
					schedule(sunrise_offset,wattering)
                	sendMessage("watterind setuped to : $sunrise_offset",message_type)
					log.debug "schedules sunrise wattering for: $sunrise_offset"
                }
                else
                
            if ((Sunrize_check_info) && (state.order_manage == 3))// (state.order_manage ==1) &&(date_h>sunrise_offset))
            	{
				//shedule for next day
                def Sunset_Sunrise_tomorrow = getSunriseAndSunset(sunriseOffset: Sunrize_delay_FULL, sunsetOffset: Sunset_delay, date: new Date()+1)
  				def sunrise_offset_tomorrow =Sunset_Sunrise_tomorrow.sunrise.format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
			
                 
        		schedule(sunrise_offset_tomorrow,wattering)
               	sendMessage("watterind setuped to next day : $sunrise_offset_tomorrow",message_type)
        		log.debug "schedules sunset wattering for next day: $sunrise_offset_tomorrow"
                state.order_manage =1
                }
            
            
            if ((Sunset_check_info) && (state.order_manage ==2)) {
        		schedule(sunset_offset,wattering)
               sendMessage("watterind setuped to : $sunset_offset",message_type)
        		log.debug "schedules sunset wattering for: $sunset_offset"
                }
                
			

	 
    	}
    else {
    
			log.debug "setup schedules just by time"
	
           
    
    
   			 if ((start_before_W) && (state.order_manage ==1 || state.order_manage ==3)){
						
                        def processing_time = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSSZ", start_before_W)
                        def start_b_w_time = new Date( processing_time.time - time_delay_valve*60 * 1000).format("yyyy-MM-dd'T'HH:mm:ss.SSSZ",location.timeZone)
                        def processing_time_Final = Date.parse("yyyy-MM-dd'T'HH:mm:ss", start_b_w_time)
                        //def sch_string =  "00"+processing_time_Final.hours + " " + processing_time_Final.minutes+ " * * * ?"	
                        def sch_string =  "0 "+processing_time_Final.minutes+" "+processing_time_Final.hours + " ? * MON-SUN"	

                        //schedule("0 30 0 ? * MON-SUN", set_schedulers)   

                       schedule(sch_string,wattering)
                       sendMessage("watterind setuped to : $processing_time_Final",message_type)
                        log.debug "schedules wattering for: $processing_time_Final" 
                        state.order_manage ==1
        
        				}
    
   			 if ((start_after_W) && (state.order_manage ==2)){
    
                        def processing_time_Final_A = Date.parse("yyyy-MM-dd'T'HH:mm:ss", start_after_W)
                        //def sch_string_A =  processing_time_Final_A.hours + " " + processing_time_Final_A.minutes+ " * * * ?"	
                        def sch_string_A =  "0 "+processing_time_Final_A.minutes+" "+processing_time_Final_A.hours + " ? * MON-SUN"	

                        schedule(sch_string_A,wattering)
                        sendMessage("watterind setuped to : $processing_time_Final_A",message_type)
                        log.debug "schedules wattering for: $processing_time_Final_A"     
                        }
   			 }

    return result            

}


def calculate_the_offset()
{
	def result= 0

	result = state.valve01_Timer.toInteger()*state.valve01_count.toInteger()
    if (state.valve02_Timer) {result = result+state.valve02_Timer.toInteger()*state.valve02_count.toInteger()}
    if (state.valve03_Timer) {result = result+state.valve03_Timer.toInteger()*state.valve03_count.toInteger()}
    if (state.valve04_Timer) {result = result+state.valve04_Timer.toInteger()*state.valve04_count.toInteger()}
    if (state.valve05_Timer) {result = result+state.valve05_Timer.toInteger()*state.valve05_count.toInteger()}
    if (state.valve06_Timer) {result = result+state.valve06_Timer.toInteger()*state.valve06_count.toInteger()}
    if (state.valve07_Timer) {result = result+state.valve07_Timer.toInteger()*state.valve07_count.toInteger()}
    if (state.valve08_Timer) {result = result+state.valve08_Timer.toInteger()*state.valve08_count.toInteger()}

    return result            

}


def set_schedulers(message_type)
{
   
	unschedule()
	state.VALVE_NUMBER = 1
    state.VALVE_SESSION = 1
    
    
	
    def week_day_num = day_week_valve()
	log.debug "DAY OF WEEK: ${week_day_num}"

    	
    //line 288 right define where and right usage state #3    
    // remove before run check line 624
    
    //Define next schedule for next run
    state.order_manage = order_check_valve()
    log.debug "Order runs: ${state.order_manage}"
        
    //get valves data based on order manage
   	//runIn(10,correct_valves_data(state.order_manage))    
    def tt_1= correct_valves_data(state.order_manage)
        
    state.MAX_VALVE_SESSION = max_session_valves()    
    log.debug "Max sessions count: ${state.MAX_VALVE_SESSION}"
        
		//calculate the offset
   		def v_time = calculate_the_offset()

    
	 //runIn(10, setup_valves_schedulers(v_time)) 
	def tt_2= setup_valves_schedulers(v_time,message_type)
   
      
             
	// setup every day schedule to correct setup sunrise and sunsets             
  // schedule("24 00 * * * ?", sheduler_24_mid )
	//log.debug "schedules check for: 00:00 AM"
        	//	schedule("0 30 0 ? * MON-SUN", set_schedulers)   
   
}

// TODO: implement event handlers
/*
def sheduler_24_mid(message_type)
{
	set_schedulers (message_type)
}
*/

def wattering ()
{
    log.debug "wattering start session ${state.VALVE_SESSION}"
    


    
    
    valve_main.on()
	log.debug "valve_main ON"

// fixing bug with not closed valves as internet connections
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
			sendMessage ("Wattering is starting",true)
            
            // NOT REQUIRED IF STAGE  = 3 works
            //update valves date in case schedule was setuped from yesterday
			//def tt_1= correct_valves_data(state.order_manage)

	}
    
    
    


switch (state.VALVE_NUMBER)
 {
 case { it==1}:
 	if (valve01) {
  		if (state.valve01_Timer.toInteger()*state.valve01_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve01_count.toInteger())
        {
        	def v1_time = state.valve01_Timer.toInteger()
            log.debug "wattering v1 start for, min $v1_time"
        	
        	valve01.on()
        	runIn(v1_time*60,valves_off)      
        } else {runIn(10,valves_off)}
        	
       } else
        	{         // GO NEXT VALVE
        	runIn(10,valves_off)
            }
 	  break;
 case {it==2}:
      if (valve02) {  
        if (state.valve02_Timer.toInteger()*state.valve02_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve02_count.toInteger())
        {
        	
        	def v2_time = state.valve02_Timer.toInteger()
            log.debug "wattering v2 start for, min $v2_time"
        	valve02.on()
        	runIn(v2_time*60,valves_off)    
         } else {runIn(10,valves_off)}
   		} else {
         // GO NEXT VALVE
         runIn(10,valves_off)}
    
    break; 
	
 case {it==3}:
 		if (valve03) {
          if (state.valve03_Timer.toInteger()*state.valve03_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve03_count.toInteger())
          {
                  
        		  def v3_time = state.valve03_Timer.toInteger()
                  log.debug "wattering v3 start for, min $v3_time"
       		  valve03.on()
        		  runIn(v3_time*60,valves_off) 
        	} else {runIn(10,valves_off)}
        } else {runIn(10,valves_off)}
   	break; 
	
 case {it==4}:
 	if (valve04) {
        if (state.valve04_Timer.toInteger()*state.valve04_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve04_count.toInteger())
          {
                
                def v4_time = state.valve04_Timer.toInteger()
                log.debug "wattering v4 start for, min $v4_time"
               valve04.on()
                runIn(v4_time*60,valves_off)    
        	} else {runIn(10,valves_off)}
        } else {runIn(10,valves_off)}
   	break; 
	
 case {it==5}:
 	if (valve05) {
        if (state.valve05_Timer.toInteger()*state.valve05_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve05_count.toInteger())
          {
            
            def v5_time = state.valve05_Timer.toInteger()
            log.debug "wattering v5 start for, min $v5_time"
           valve05.on()
            runIn(v5_time*60,valves_off)    
       	  } else {runIn(10,valves_off)}
        } else {runIn(10,valves_off)}
   	break; 
	
 case {it==6}:
 	if (valve06) {
        if (state.valve06_Timer.toInteger()*state.valve06_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve06_count.toInteger())
          {
                
                def v6_time = state.valve06_Timer.toInteger()
                log.debug "wattering v6 start for, min $v6_time"
               valve06.on()
                runIn(v6_time*60,valves_off)    
        	} else {runIn(10,valves_off)}
        } else {runIn(10,valves_off)}
   	break; 
	
 case {it==7}:
 	if (valve07) {
        if (state.valve07_Timer.toInteger()*state.valve07_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve07_count.toInteger())
          {
            
            def v7_time = state.valve07_Timer.toInteger()
            log.debug "wattering v7 start for, min $v7_time"
           valve07.on()
            runIn(v7_time*60,valves_off)    
        	} else {runIn(10,valves_off)}
        } else {runIn(10,valves_off)}
   	break; 
	
 case {it==8}:
 	if (valve08) {
        if (state.valve08_Timer.toInteger()*state.valve08_count.toInteger()>0 && state.VALVE_SESSION.toInteger()<=state.valve08_count.toInteger())
          {
            
            def v8_time = state.valve08_Timer.toInteger()
            log.debug "wattering v8 start for, min $v8_time"
            valve08.on()
            runIn(v8_time*60,valves_off)    
        	} else {runIn(10,valves_off)}
        } else {
        
        log.debug "valve v8 exiting"
        runIn(10,valves_off)
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
          runIn(10,wattering)
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
                    runIn(10,vallve_all_off)    

                }
                else
                { 

                log.debug "Next session starting ${state.VALVE_SESSION} from ${state.MAX_VALVE_SESSION}"

                //continue wattering - next session
                runIn(10,wattering) 

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

	sendMessage ("Wattering has done", true)
     
     // run schedulers to setup next wattering
    set_schedulers(false)
}

def sendMessage(message , message_type)
{
	def stamp = new Date().format('hh:mm:ss ', location.timeZone)
    
  	if (people && message_type)
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