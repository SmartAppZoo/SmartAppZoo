definition(
    name: "Simple home timer",
    namespace: "pdubovyk",
    author: "Pdubovyk",
    description: "simple timer",
    category: "Convenience",
    iconUrl: "http://cdn.device-icons.smartthings.com/Office/office6-icn.png",
    iconX2Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png",
    iconX3Url: "http://cdn.device-icons.smartthings.com/Office/office6-icn@2x.png"
)

preferences {


    input (name:"Delay_to_On", type: "number" , title: "On Delay", defaultValue: 30) 
    input (name:"Delay_to_off", type: "number" , title: "Off Delay", defaultValue: 30) 

	section("Devices") {
    	input "switches_G",title:"Devices for onoff", "capability.switch",multiple: true    	
						}
     
}

def installed() {
    
  updated()
       
}

def updated() {
	//unsubscribe()
   
	unschedule()
 	set_schedulers()    
}


def set_schedulers()
{
        
        //Delay_to_On
        //Delay_to_off
    log.debug "check 01"
// 	schedule(now()+Delay_to_On*60000,"Turn_on_all_Switches")
	
    Turn_on_all_Switches()
	
//   log.debug "check 02"
//    schedule(now()+Delay_to_On*60000+Delay_to_off*60000,"Turn_off_all_Switches")
    
}

def Turn_off_all_Switches()

{

 log.debug "turing off"
 switches_G.off()
schedule(now()+Delay_to_On*60000,"Turn_on_all_Switches")
log.debug "turned off"
}


def Turn_on_all_Switches()
{

log.debug "turing on"
switches_G.on()
schedule(now()+Delay_to_off*60000,"Turn_off_all_Switches")
log.debug "tured on"
}

