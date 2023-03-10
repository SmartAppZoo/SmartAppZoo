definition(
    name: "SanofConnect",
    namespace: "pdubovyk",
    author: "Pdubovyk",
    description: "connect sanof switches with virtual to get right position after reset-power outsage",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {

	section("Sanof") {
   
	input "sanof01",title:"Sanof #01", "capability.switch"//,required: true
    }

 	section("Lists of virtual switches") {
    input "switch01",title:"switch #01", "capability.switch"//,required: true
   
     }
	
    /*section("Time to refresh") {
        input (name:"refresh_time", type: "number" , title: "Time to refresh", defaultValue: 10) 
	}*/


	
}

def installed() {
  
  initialize()
       
}

def updated() {
	unsubscribe()
    initialize()
    
    
}

def initialize() {
	subscribe(switch01, "switch", syncswitchesall)  
    
    syncswitchesall
    runEvery10Minutes(syncswitchesall)
}

def syncswitchesall(evt)
{
   
if (evt.isStateChange())
	{
     if (evt.value == "on") {
		log.debug "switch turned on! ${evt.displayName}"
        switch(evt.displayName) {
       		 case "switch01":
          		  sanof01.on()
           		 break
       		 case "switch02":
          		 // sanof02.on()
                break        
    		}  
    } else if (evt.value == "off") {
        	
             switch(evt.displayName) {
       		 case "switch01":
          		  sanof01.off()
           		 break
       		 case "switch02":
          		//  sanof02.off()
                break        
    		}  
            log.debug "switch turned off! ${evt.displayName}"

    	}     
	}

}