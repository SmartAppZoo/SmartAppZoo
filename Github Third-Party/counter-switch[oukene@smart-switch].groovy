/**
 *  Counter Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Counter Switch",
    namespace: "oukene/smart-switch",
    author: "oukene",
    description: "모션센서 2개를 이용한 카운터 센서",
    category: "My Apps",
    pausable: true,
    
    parent: "oukene/smart-switch/parent:Smart Switch",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/cue/72/rotate_counter_clockwise-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/cue/72/rotate_counter_clockwise-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/cue/72/rotate_counter_clockwise-512.png")

preferences {
	page(name: "dashBoardPage", install: false, uninstall: true)
   	page(name: "motionSensorPage", install: false, uninstall: true, nextPage: switchPage)
   	page(name: "switchPage", install: false, uninstall: true, nextPage: optionPage)
   	page(name: "optionPage", install: true, uninstall: true)
}

def dashBoardPage() {
	dynamicPage(name: "dashBoardPage", title:"", refreshInterval:1) {
    	try
        {
            if(state.initialize)
            {
                section("") {
                    paragraph "- DashBoard", image: "https://cdn4.iconfinder.com/data/icons/finance-427/134/23-512.png"
                    paragraph "내부 모션 - " + (inside_motion.currentMotion == "active" ? "감지됨" : "미감지") + ", 감지시각 - " + new Date(state.in_motion_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    paragraph "외부 모션 - " + (outside_motion.currentMotion == "active" ? "감지됨" : "미감지") + ", 감지시각 - " + new Date(state.out_motion_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    if(main_switch)
                    {
                        paragraph "스위치- " + (main_switch.currentSwitch == "on" ? "켜짐" : "꺼짐")
                    }
                    paragraph "인원수: " + state.counter
                    paragraph "수동모드: " + (useManualMode == true ? "사용" : "미사용")
                    if(light_meter) 
                    { 
                        paragraph "현재 조도: " + light_meter.currentIlluminance + ", 기준 조도: " + lux_max 
                    }
                    else { paragraph "조도 센서 미사용" }
                    if(true == useManualMode)
                    {
                        paragraph "모드: " + (state.autoMode ? "자동모드" : "수동모드")
                    }
                    paragraph (enable ? "활성화" : "비활성화")
                }
            }
            section() {
                href "motionSensorPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
            }
            if(state.initialize)
            {
                section()
                {
                    href "optionPage", title: "옵션", description:"", image: "https://cdn4.iconfinder.com/data/icons/multimedia-internet-web/512/Multimedia_Internet_Web-16-512.png"
                }
            }
		}
        catch(all)
        {
        	section("설정이 올바르지 않습니다. 재설정해주세요") {
                href "motionSensorPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
            }
        }
    }
}

def motionSensorPage() {
	dynamicPage(name: "motionSensorPage", title: "모션센서 설정", nextPage: switchPage, uninstall: true) {
        
    	section("[motion sensor]") {
            input "inside_motion", "capability.motionSensor", required: true, title: "내부 모션센서와"    
            input "outside_motion", "capability.motionSensor", required: true, title: "외부 모션센서의"
            input "delay", "number", required: true, title: "감지 간격 차이가 설정한 시간(초단위) 이내면 카운팅 합니다.", defaultValue: 5
        } 
    }
}

def switchPage() {
	dynamicPage(name: "switchPage", title: "모션센서 설정", nextPage: optionPage, uninstall: true) {

        section("[switch]") {
        	paragraph "카운터가 0보다 커지면 아래 스위치를 켭니다."
            paragraph "그리고 카운터가 0이 되면 스위치를 끕니다."
            input "main_switch", "capability.switch", required: false, multiple: false, title: "이 스위치를 켭니다.(메인스위치)"
            input "sub_switch", "capability.switch", required: false, multiple: true, title: "그리고 보조 스위치들을 켭니다."
        }
    }
}

def optionPage() {   
    
	dynamicPage(name: "optionPage", title: "옵션 설정", nextPage: appNamePage, uninstall: true) {
		
        section("그리고 아래 옵션을 적용합니다(미 설정시 적용되지 않음)") {
    		input "light_meter", "capability.illuminanceMeasurement", title: "조도 센서", required: false, multiple: false, submitOnChange :  true 
            if(light_meter)
            {
                input "lux_max", "number", title: "기준 조도값", required: false, 
                    defaultValue: "30"
			}
            input "useManualMode", "bool", title: "수동모드 활성화", defaultValue: true, required: true
            input "isForceOff", "bool", title: "강제 Off 사용", defaultValue: false, required: true, submitOnChange :  true 
            if(isForceOff)
            {
            	input "forceOffsecond", "number", title: "설정된 시간 이후 자동 off(초)", defaultValue: "1800", required: true
            }
            
            input "isChildDevice", "bool", title: "카운터 디바이스를 생성하시겠습니까?", defaultValue: false, required: true, submitOnChange: true
            if(isChildDevice)
            	input "counter_device_name", "text", title: "디바이스명", required: true, defaultValue: "virtual counter"
                
            input "enableLog", "bool", title: "로그활성화", required: true, defaultValue: false
        }
        
        section("자동화 on/off")
        {
            input "enable", "bool", title: "활성화", required: true, defaultValue: true
        }   
        
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = inside_motion.displayName + "-" + outside_motion.displayName + ": Counter Switch"
            log.debug "will set default label of $l"
            app.updateLabel(l)
        }
        section("자동화 이름") {
        	if (overrideLabel) {
            	label title: "자동화 이름을 입력하세요", defaultValue: app.label, required: false
            }
            else
            {
            	paragraph app.label
            }
            input "overrideLabel", "bool", title: "이름 수정", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}


def uninstalled() {
	log("uninstall")
    deleteDevice()
}

def installed() {
	log("install")
    initialize()
}

def updated() {
	log("updated")
    unsubscribe()
    initialize()
}

def initialize() {
	state.child_dni = "counter"

	log("initialize")
    
    // create child device
    if(true == isChildDevice)
    {
    	log("create child device")
    	addDevice()
    }
    else
    {
    	log("delete child device")
    	deleteDevice()
    }
    
    // if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(app.label)
    }
    
    subscribe(inside_motion, "motion.active", motion_inside_detect_handler)
    subscribe(outside_motion, "motion.active", motion_outside_detect_handler)
    
    if(light_meter != null)
    {
    	subscribe(light_meter, "illuminance", lux_change_handler)
   	}
    if(main_switch != null) {
        //subscribe(light, "switch.on", switch_on_handler)
        subscribe(main_switch, "switch.off", switch_off_handler)
    }
    
    state.in_motion_time = now() - delay
    state.out_motion_time = now() - delay
    resetCounter()
    state.autoMode = false
    
    state.initialize = true
}

def motion_inside_detect_handler(evt) {
	log("motion_inside_detect_handler called: $evt")
    
    unschedule(switchOff)
    
    state.in_motion_time = now()
    log("in motion : ${state.in_motion_time}, out motion : ${state.out_motion_time}")
   	log("millisecond : ${state.in_motion_time - state.out_motion_time}")
    
    if(state.in_motion_time - state.out_motion_time <= delay * 1000)
    {
    	incrementCounter()
        
        if(main_switch != null)
        {
            if(state.counter > 0 && main_switch.currentState("switch").value == "off" &&
            	(light_meter == null || (light_meter != null && light_meter.currentIlluminance <= lux_max)))
            {
                state.autoMode = true
                main_switch.on()
                log("main switch on")
                if(sub_switch) sub_switch.on()
                
                // 일정 시간 이후 강제 off
                if(isForceOff)
                {
                	if(0 == forceOffsecond)
                		runIn(forceOffsecond, switchOff, [overwrite: true])
					else schedule(now() + (forceOffsecond * 1000), switchOff)
                }
            }
        }
    }
}

def switchOff()
{
	if(main_switch)
    	main_switch.off()
}

def motion_outside_detect_handler(evt) {
	log("motion_outside_detect_handler called: $evt")
    
    unschedule(switchOff)
    
    state.out_motion_time = now()
    log("in motion : ${state.in_motion_time}, out motion : ${state.out_motion_time}")
    if(state.out_motion_time - state.in_motion_time <= delay * 1000)
    {
    	decrementCounter()
        if(state.counter <= 0)
        {
        	if(true == state.autoMode)
            {
            	log("switch off")
        		if(main_switch) main_switch.off()
                if(sub_switch) sub_switch.off()
            }
        }
    }
}

def incrementCounter()
{
	state.counter = state.counter + 1
    setCounterDevice(state.counter)
   	log("current counter : ${state.counter}")
}

def decrementCounter()
{
	state.counter = Math.max(0, state.counter - 1)
    setCounterDevice(state.counter)
    log("current counter : ${state.counter}")
}

def resetCounter()
{
	state.counter = 0
    setCounterDevice(state.counter)
    log("reset counter")
}

def setCounterDevice(counter)
{
	def device = getChildDevice(state.child_dni)
    if(null != device)
    {
        device.setLevel(counter)
    }
}


def switch_off_handler(evt) {
	log("switch_off_handler called: $evt")
    if(sub_switch)
    	sub_switch.off()
    resetCounter()
    state.autoMode = false;
}


def existChild(dni){
	def result = false
	def list = getChildDevices()
    list.each { child ->
        if(child.getDeviceNetworkId() == dni){
        	result = true
        }
    }
    return result
}

def addDevice(){
	log("add device")
    
    if(!existChild(state.child_dni)){
        try{
            def counter_device = addChildDevice("oukene/smart-switch", "virtual counter", state.child_dni, null, [
                "label": counter_device_name
            ])    
        }catch(err){
            log.error err
        }
    }
    
}

def deleteDevice() {
	getChildDevices().each {
    	def test = it
        def search = settings.devices.find { getChildDevice(it.id).id == test.id }
        if(!search) {
        	test.each { deleteChildDevice(it.deviceNetworkId) }
        }
    }
}

def log(msg)
{
	if(enableLog != null && enableLog == "true")
    {
    	log.debug msg
    }
}