/**
 *  Timer Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Timer Switch",
    namespace: "oukene/smart-switch",
    author: "oukene",
    description: "타이머 스위치",
    category: "My Apps",
    
    parent: "oukene/smart-switch/parent:Smart Switch",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/business-271/135/50-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/business-271/135/50-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/business-271/135/50-512.png"
)

preferences
{
	page(name: "dashBoardPage", install: false, uninstall: true)
	page(name: "actuatorTypePage", install: false, uninstall: true)
    page(name: "switchPage", install: false, uninstall: true)
	page(name: "optionPage", install: true, uninstall: true)
}

def dashBoardPage(){
	dynamicPage(name: "dashBoardPage", title:"[Dash Board]", refreshInterval:1) {
    	try
        {
            if(state.initialize)
            {
                section("") {
                    paragraph "- DashBoard", image: "https://cdn4.iconfinder.com/data/icons/finance-427/134/23-512.png"
                    paragraph "[ $actuatorType - $actuator, switch - $main_switch ]"
                    paragraph "스위치 켜진시각: " + new Date(state.on_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    paragraph "꺼짐 예정시각: " + new Date(state.off_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    if(state.off_time > now())
                        paragraph "남은 시간(초): " + (int)(((state.off_time - now()) / 1000) + 0.5)
                }
                if(isChildDevice)
                {
                    section("")
                    {
                        paragraph "가상디바이스명: " + counter_device_name
                        paragraph "갱신주기" + counter_device_refresh_interval + "초"
                    }
                }
            }          
            section() {
                href "actuatorTypePage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
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
                href "actuatorTypePage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
            }
        }
    }
}

def actuatorTypePage()
{
	dynamicPage(name: "actuatorTypePage", title: "설정", nextPage: "switchPage")
    {
        section()
        {
            input "actuatorType", "enum", title: "동작 조건 선택", multiple: false, required: true, submitOnChange: true, options: [
            	"button": "Button",
                "switch": "Switch"]
        }
        if(actuatorType != null)
        {
            section("$actuatorType 설정") {
                input(name: "actuator", type: "capability.$actuatorType", title: "$actuatorType 에서", required: true)
                input(name: "actuatorAction", type: "enum", title: "다음 동작이 발생하면", options: actuatorValues(actuatorType), required: true)
            }
        }
    }
}

def switchPage()
{
    dynamicPage(name: "switchPage", title: "", nextPage: "optionPage")
    {
        section("스위치 설정") {
            input(name: "main_switch", type: "capability.switch", title: "이 스위치를 켭니다.", multiple: false, required: false)
        }
    }
}


def optionPage()
{
    dynamicPage(name: "optionPage", title: "")
    {
        section("그리고 아래 옵션을 적용합니다(미 설정시 적용되지 않음)") {
        	input "appendSecond", "number", required: true, title: "추가 시간(초)", defaultValue: "10"
        	input "isChildDevice", "bool", title: "가상 스위치를 생성하시겠습니까?", defaultValue: false, required: true, submitOnChange: true
            if(isChildDevice)
            {
				input "counter_device_name", "text", title: "디바이스명", required: true, defaultValue: "virtual counter"
                input "counter_device_refresh_interval", "number", title: "갱신주기(초)", required: true, defaultValue: "5"
			}
            input "enableLog", "bool", title: "로그활성화", required: true, defaultValue: false
        }
        
        section("자동화 on/off")
        {
            input "enable", "bool", title: "활성화", required: true, defaultValue: true
        }   
        
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = actuator.displayName + ": Timer Switch"
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


private actuatorValues(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on":"켜기","off":"끄기"]
        case "button":
        	return ["pushed":"누름", "double":"두번누름", "held":"길게누름"]
        default:
            return ["UNDEFINED"]
    }
}


def installed()
{
	log("Installed with settings: ${settings}.")
	initialize()
}

def updated()
{
	log("Updated with settings: ${settings}.")
	unsubscribe()
	unschedule()
	initialize()
}

def initialize()
{
	if(!enable) return
    
	state.child_dni = "counter"
	// if the user did not override the label, set the label to the default
    if (!overrideLabel) {
        app.updateLabel(app.label)
    }
    log("$actuator : $actuatorAction")
	subscribe(actuator, "$actuatorType.$actuatorAction", eventHandler)
    
    if(main_switch != null) {
        subscribe(main_switch, "switch.on", switch_on_handler)
        subscribe(main_switch, "switch.off", switch_off_handler)
    }
    if(isChildDevice)
    {
    	log("create child device")
    	addDevice()
        def counter_device = getChildDevice(state.child_dni)
        if(counter_device) {
        	counter_device.off()
            subscribe(counter_device, "switch.off", switch_off_handler)
		}
    }
    else
    {
    	log("delete child device")
    	deleteDevice()
    }
    
    state.on_time = now()
    state.off_time = now()
    
    state.initialize = true    
}

def eventHandler(evt)
{
	log("$evt.name : $evt.value : $actuatorAction")
    
    /*
    if(main_switch)
    {
        log("switch: " + main_switch.displayName)
        def switchvalue = main_switch.currentState("switch").value
        log("switch value: $switchValue")
	}
    
    def device = getChildDevice(state.child_dni)
    if(device)
    {
        log("virtual device : " + device)
        log("virtual device Switch : " + device.currentState("switch").value)
	}
    */
    
    // 스위치 켬
    if(null != main_switch && main_switch.currentState("switch").value == "off")
    {
    	log("main switch on")
    	main_switch.on()
        state.on_time = state.off_time = now()
    }
    def device = getChildDevice(state.child_dni)
    // 가상 스위치가 생성되었다면 켬
    if(device && device.currentState("switch").value == "off")
    {
        device.on()
        device.setLevel(appendSecond)
        state.on_time = state.off_time = now()
        runIn(counter_device_refresh_interval, timer, [overwrite: true])
    }
    
    log("add second : " + appendSecond)
    
    state.off_time = state.off_time + (appendSecond * 1000)
    
    log("now:" + now())
    log("offTime : $state.off_time")
    
    log("runin: " + (state.off_time - now()) / 1000)
    
    runIn((state.off_time - now()) / 1000, switchOff, [overwrite: true])
}

def switchOff() {
    log("switchOff")
    if(main_switch)
    	main_switch.off()
    def device = getChildDevice(state.child_dni)
    if(device)
    	device.setLevel(0)
}

/// 가상 디바이스 사용시 갱신을 위해 실행됨
def timer() {
    def device = getChildDevice(state.child_dni)
    if(device) {
    	def remainTime = Math.max(0, (int)((state.off_time - now()) / 1000))
    	device.setLevel(remainTime)
        if(remainTime > 0)
        	runIn(counter_device_refresh_interval, timer, [overwrite: true])
    }
}

def switch_on_handler(evt) {
}

def switch_off_handler(evt) {
    log("switch_off_handler called: $evt")
    
    if(main_switch)
    	main_switch.off()
    def device = getChildDevice(state.child_dni)
    if(device) {
    	device.setLevel(0)
    }
    state.off_time = now()
    
}

def log(msg)
{
	if(enableLog != null && enableLog == true)
    {
    	log.debug msg
    }
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