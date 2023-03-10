/*
 * 20200619 - jog up, down 추가
 */
definition(
    name: "EasyRollApp",
    namespace: "Inoshade",
    author: "Nouvothoth",
    description: "For easyroll devices with DTH",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences
{
	page(name: "configurations")
	page(name: "options")
}

def configurations()
{
	dynamicPage(name: "configurations", title: "Configurations", uninstall: true, nextPage: "options")
    {
        section("Device")
        {
            input "devices", "device.easyrollsingle", title: "EasyRoll(DTH)", multiple: true, required: true
            input "actions", "enum", title: "Action", multiple: false, required: true, options: ["UP", "STOP", "DOWN", "LEVEL", "JOG UP", "JOG DOWN", "M1", "M2", "M3","SETBOTTOM","SETTOP","REFRESH"]
        } 
        section("How")
        {
            input "linker", "enum", title: "Target", multiple: false, required: true, options: ["Switch", "Button", "MotionSensor", "LightSensor", "Sunrise", "Sunset", "Time"]
        }   
        section ("Assign a name")
        {
            label title: "Assign a name", required: false
        }
    }
}

def options()
{
	//log.debug "linker: $linker"
    dynamicPage(name: "options", title: "DTH will run the action selected prev.page when the target acts like below...", install: true, uninstall: true)
    {
        if(linker=="Switch"){
            section(title: "When switch acts...")
            {
                input "switches", "capability.switch", title: "Switch", multiple: false, required: true
                input "switchAction", "enum", title:"On/Off", multiple:false, required:true, options:["On", "Off"]
            }
        }else if(linker=="Button"){
            section(title: "When button acts...")
            {
                input "button", "capability.button", title: "Button", multiple: false, required: true
                input "buttonAction", "enum", title:"pushed/held", multiple:false, required:true, options:["Pushed", "Held"]
            }
        }else if(linker=="MotionSensor"){
            section(title: "When motionsensor senses...")
            {
                input "motionSensor", "capability.motionSensor", title: "MotionSensor", multiple: false, required: true
            }
        }else if(linker=="LightSensor"){
            section(title: "Light Sensing")
            {
                input "lightSensor", "capability.illuminanceMeasurement", title: "LightSensor", multiple: false, required: true
                input "range", "enum", title:"Range", multiple:false, required:true, options:["less", "more"]
                input "brightLevel", "number", title:"Brightness", required: true
            }
        }else if(linker=="Time"){
            section(title: "Time")
            {
                input "time", "time", title: "Time", required: true
                input "days", "enum", title: "Only on certain days of the week:", multiple: true, required: false, options: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
            }
        }else if(linker=="Sunrise" || linker=="Sunset"){
            section ("Zip code (optional, defaults to location coordinates when location services are enabled)...")
            {
                input "zipCode", "text", title: "Zip Code?", required: true, description: "Local Zip Code", defaultValue: "KS013"
                input "days", "enum", title: "Only on certain days of the week:", multiple: true, required: false, options: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
            }
        }
        
        if(actions == "LEVEL"){
        	section(title: "Movement Level")
            {
                input "level", "number", title: "Height", required:true, description: "0% to 100%"
            }
        }
    }
}

def installed()
{
	log.debug "Installed with settings: ${settings}."
	initialize()
}

def updated()
{
	log.debug "Updated with settings: ${settings}."
	unsubscribe()
	unschedule()
	initialize()
}

def initialize()
{
	//switch, motionSensor, lightSensor는 즉시동작으로 event에 대한 핸들러만을 필요로 함
    //"Switch", "Button", "MotionSensor", "LightSensor", "Sunrise", "Sunset", "Time"
    switch(linker){
    	case "Switch":
        	subscribe(switches, "switch", switchHandler)
        break
        case "Button":
        	subscribe(button, "button", buttonHandler)
        break
        case "MotionSensor":
        	subscribe(motionSensor, "motion", motionHandler)
        break
        case "LightSensor":
        	subscribe(lightSensor, "illuminance", illuminanceHandler, [filterEvents: false])
        break
        //time과 sunset&sunrise만 scheduler필요   
        case "Sunrise":
            //subscribe(location, "position", locationPositionChange)
            subscribe(location, "sunriseTime", sunriseHandler)
        break
        case "Sunset":
            //subscribe(location, "position", locationPositionChange)
            subscribe(location, "sunsetTime", sunsetHandler)
        break
        case "Time":
        	schedule(time, alarmAction)
        break
    }
}

def sunriseHandler()
{
	actionRun()
}

def SunsetHandler()
{
	actionRun()
}

/*
def locationPositionChange(evt)
{
	log.trace "locationChange()"
}
*/

def alarmAction()
{
	log.debug "alarmAction- days: $days"
    if(days==null){
    	//log.debug "days.isEmpty()"
    	actionRun()
    }else{
    	//log.debug "NOT days.isEmpty()"
    	def tempDate = new Date()
        def day = tempDate.toString().split(" ")
        if(days.contains(day[0])){
        	actionRun()
        }
    }
}
            
def buttonHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	if (evt.value == "pushed" && buttonAction == "Pushed")
    {
    	actionRun()
    }
    else if(evt.value == "held" && buttonAction == "Held")
    {
    	actionRun()
    }
}

def motionHandler(evt)
{
	log.debug "$evt.name: $evt.value"
	if (evt.value == "active")
    {
    	actionRun()
    }
}

def switchHandler(evt)
{
	log.debug "switch Handler $evt.name: $evt.value"
    if (evt.value == "on" && switchAction == "On")
    {
        //log.debug "switch: $switches now ON."
        actionRun()
    }
	else if (evt.value == "off" && switchAction == "Off")
    {
        //log.debug "switch: $switches now OFF."
        actionRun()
    }
}

def illuminanceHandler(evt)
{
	log.debug "$evt.name: $evt.value, lastStatus lights: $state.lightsState, lastStatus dimmers: $state.dimmersState, motionStopTime: $state.motionStopTime"
	if(range == "less" && brightLevel>=evt.integerValue){
    	//log.debug "brightLevel($brightLevel)>=evt.integerValue"
   		actionRun()
    }else if(range == "more" && brightLevel<=evt.integerValue){
    	//log.debug "brightLevel($brightLevel)<=evt.integerValue"
    	actionRun()
    }
}

def actionRun(){
    for (device in devices) {
    	switch(actions){
        	case "UP":
            	device.up()
            break
            case "STOP":
            	device.stop()
            break
            case "DOWN":
            	device.down()
            break
            case "LEVEL":
            	device.setLevel(level)
            break
            case "JOG UP":
            	device.jogUp()
            break
            case "JOG DOWN":
            	device.jogDown()
            break
            case "M1":
            	device.m1()
            break
            case "M2":
            	device.m2()
            break
            case "M3":
            	device.m3()
            break
           case "SETBOTTOM":
            	device.bottomSave()
            break
           case "SETTOP":
            	device.topSave()
            break
            case "REFRESH" :
            	device.refresh()
            break
      
        }
    }
}
