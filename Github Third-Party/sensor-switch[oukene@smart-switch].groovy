/**
 *  Sensor Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Sensor Switch",
    namespace: "oukene/smart-switch",
    author: "oukene",
    description: "센서로 스위치 작동",
    category: "My Apps",
    pausable: true,
    
  	parent: "oukene/smart-switch/parent:Smart Switch",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/basic-ui-element-2-3-filled-outline/512/Basic_UI_Elements_-_2.3_-_Filled_Outline_-_44-29-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/basic-ui-element-2-3-filled-outline/512/Basic_UI_Elements_-_2.3_-_Filled_Outline_-_44-29-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/basic-ui-element-2-3-filled-outline/512/Basic_UI_Elements_-_2.3_-_Filled_Outline_-_44-29-512.png"
)

preferences
{
	page(name: "dashBoardPage", install: false, uninstall: true)
	page(name: "sensorPage", install: false, uninstall: true, nextPage: "switchPage")
    page(name: "switchPage", install: false, uninstall: true, nextPage: "optionPage")
	page(name: "optionPage", install: true, uninstall: true)
}

def dashBoardPage(){
	dynamicPage(name: "dashBoardPage", title:"[Dash Board]", refreshInterval:1) {
    	try
        {
            if(state.initialize)
            {
                section() {
                    paragraph "- DashBoard", image: "https://cdn4.iconfinder.com/data/icons/finance-427/134/23-512.png"
                    paragraph "[ $sensorType ]"
                    def isList = sensor instanceof List
                    if(isList) {
                        sensor.each {
                            paragraph "" + it.displayName + " - " + it.currentState(sensorActions(sensorType)).value	
                        }
					}
                    else 
                    {
                    	paragraph "" + sensor.displayName + " - " + sensor.currentState(sensorActions(sensorType)).value	
					}
                    paragraph "스위치 켜진시각: " + new Date(state.on_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    paragraph "스위치 꺼진시각: " + new Date(state.off_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                    paragraph "수동모드: " + (useManualMode == true ? "사용" : "미사용")
                    if(light_meter) 
                    { 
                        paragraph "현재 조도: " + light_meter.currentIlluminance + ", 기준 조도: " + lux_max 
                    }
                    else { paragraph "조도 센서 미사용" }
                    if(true == useManualMode)
                    {
                        paragraph "모드: " + (state.autoMode == true ? "자동모드" : "수동모드")
                    }
                }
            }          
            section() {
                href "sensorPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
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
                href "sensorPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
            }
        }
    }
}


def sensorPage()
{
	dynamicPage(name: "sensorPage", title: "설정", nextPage: "switchPage")
    {
        section()
        {
            input "sensorType", "enum", title: "센서 선택", multiple: false, required: true, submitOnChange: true, options: [
                "contactSensor":"열림감지 센서",
                "motionSensor":"움직임 감지 센서",
                "switch": "스위치",
                "button": "버튼",
                "waterSensor": "물 감지 센서"]
        }
        if(sensorType != null) {
            section("$sensorType 설정") {
                input(name: "sensor", type: "capability.$sensorType", title: "$sensorType 에서", required: true, multiple: true, submitOnChange: true)
                input(name: "sensorAction", type: "enum", title: "다음 동작이 발생하면", options: attributeValues(sensorType), required: true)
            }
           	def isList = sensor instanceof List
			if(true == isList && sensor.size() > 1) {
                section("") {
                    input "isAllDevices", "bool", title: "선택된 디바이스 모두에서 조건 만족 할 경우에만", required: true, defaultValue: false
                }
            }
        }
    }
}

def switchPage()
{
    dynamicPage(name: "switchPage", title: "", nextPage: "optionPage")
    {
    	log.debug "is All Device: " + isAllDevices
        section("스위치 설정") {
            input(name: "main_switch", type: "capability.switch", title: "이 스위치와", required: true, submitOnChange: true)
            input(name: "sub_switch", type: "capability.switch", title: "추가로 이 스위치들을", multiple: true, required: false)
            if(main_switch)
            {
            	input(name: "reactionValue", type: "enum", title: "아래와 같이 실행하고 조건이 변경되면 끕니다(버튼은 끄기 제외)", options: reactionValues("switch"), required: true, submitOnChange: true)
                def isList = sensor instanceof List 
                if(true == isList && reactionValue == "on" && sensor.size() > 1) {
					input "isAllDevices_off", "bool", title: "선택된 디바이스 모두에서 조건이 변경될 경우에만 꺼짐", required: true, defaultValue: false
                }
            }
        }
    }
}

def timeInputs() {
    section {
        input "startTime", "time", title: "자동화 작동시작", required: false
        input "endTime", "time", title: "자동화 작동종료", required: false
    }
}

def optionPage()
{
    dynamicPage(name: "optionPage", title: "")
    {
        section("그리고 아래 옵션을 적용합니다(미 설정시 적용되지 않음)") {
        	if(reactionValue == "on")
            {
                input "light_meter", "capability.illuminanceMeasurement", title: "조도 센서", required: false, multiple: false, submitOnChange: true
                if(light_meter)
                {
                    input "lux_max", "number", title: "기준 조도값", required: true, 
                        defaultValue: "30"
                }
                // 이 옵션들은 스위치를 켤때만 적용
                input "stay", "number", required: true, title: "동작 조건 변경 후 꺼짐지연시간(초)", defaultValue: "0"
                input "useManualMode", "bool", title: "수동모드 활성화", defaultValue: false, required: true
			}
            input "enableLog", "bool", title: "로그활성화", required: true, defaultValue: false
        }
        timeInputs()
        
        section("자동화 on/off")
        {
            input "enable", "bool", title: "활성화", required: true, defaultValue: true
        }   
        
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = main_switch.displayName + ": Sensor Switch"
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



private reactionValues(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on":"켜기","off":"끄기","toggle":"켜거나 끄기"]
        default:
            return ["UNDEFINED"]
    }
}

private attributeValues(attributeName) {
    switch(attributeName) {
        case "switch":
            return ["on":"켜짐","off":"꺼짐"]
        case "contactSensor":
            return ["open":"열림","closed":"닫힘"]
        case "motionSensor":
            return ["active":"감지됨","inactive":"감지되지않음"]
        case "waterSensor":
            return ["wet":"젖음","dry":"마름"]
        case "button":
        	return ["pushed":"누름","double":"두번누름","held":"길게누름"]
        default:
            return ["UNDEFINED"]
    }
}

private sensorActions(name) {
    switch(name) {
        case "switch":
            return "switch"
        case "contactSensor":
            return "contact"
        case "motionSensor":
            return "motion"
        case "waterSensor":
            return "water"
        case "button":
        	return "button"
        default:
            return ["UNDEFINED"]
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

def initialize()
{    
	if(!enable) return
	// if the user did not override the label, set the label to the default
    
    if (!overrideLabel) {
        app.updateLabel(app.label)
    }
    
    def action = sensorActions(sensorType)
    log("sensor: $sensor , action : $action , reactionValue : $reactionValue")
	subscribe(sensor, sensorActions(sensorType), eventHandler)
    
    //if(light_meter != null)
    //{
    //	subscribe(light_meter, "illuminance", lux_change_handler)
   	//}
    if(main_switch != null) {
        //subscribe(light, "switch.on", switch_on_handler)
        subscribe(main_switch, "switch.off", switch_off_handler)
        subscribe(main_switch, "switch.on", switch_on_handler)
    }
    
    state.on_time = now()
    state.off_time = now()
    state.autoMode = false
    
    state.initialize = true
    
    log("init finish")
}

def isAllDevicesConditionCheck(value)
{
	def ret = true
    def isList = sensor instanceof List 
    if(isList)
    {
        sensor.each {
        	if(it.currentState(sensorActions(sensorType)).value != value)
            	ret = false
        }
    }
    return ret
}

def eventHandler(evt)
{
	log("$evt.name : $evt.value : $sensorAction : $reactionValue")
    
	if (evt.value == sensorAction && reactionValue == "on")
    {
    	if(isAllDevices && !isAllDevicesConditionCheck(evt.value)) return
        
        unschedule(switchOff)
    	def isBetween = true
        if(null != startTime && null != endTime) { isBetween = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone) }
        log("between: $isBetween")
    	if(isBetween)
        {
        	log("main switch: " + main_switch.currentState("switch").value)
            log("light_meter: " + light_meter)
			// 켜짐 작동
            if(main_switch.currentSwitch == "off" &&
               (light_meter == null || (light_meter != null && light_meter.currentIlluminance <= lux_max)))
            {
                state.autoMode = true
                switchOn()
            }
            log("main switch on")
        }
    }
    else if(evt.value == sensorAction && reactionValue == "toggle")
    {
    	def isBetween = true
        if(null != startTime && null != endTime) { isBetween = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone) }
        log("between: $isBetween")
    	if(isBetween)
        {
        	main_switch.currentSwitch == "on" ? switchOff() : switchOn()
        }
    }
    else if(evt.value == sensorAction && reactionValue == "off")
    {
    	switchOff()
    }
    else if(evt.value != sensorAction)
    {
    	if(reactionValue == "on")
        {
            if(state.autoMode == true)
            {
            	if(sensorType != "button")
                {
                    //꺼짐 작동
                    if(isAllDevices_off && !isAllDevicesConditionCheck(evt.value)) return
                    
                    log("scheduled off : $stay seconds")
                    if(0 == stay) runIn(stay, switchOff, [overwrite: true])
                    else schedule(now() + (stay *1000), switchOff)
                    //runIn(stay, switchOff, [overwrite: true])
				}
            }
		}
        /*
        else if(reactionValue == "off")
        {
        	
        }
        */
    }
}

def switchOff() {
    log("switchOff")
    main_switch.off()
    if(null != sub_switch)
    	sub_switch.off()
}

def switchOn() {
	log("switchOn")
    // 지연 꺼짐을 덮어쓰기 위해 스위치 켜기 전 호출을 한번 해준다
    //runIn(0, switchOff, [overwrite: true])
    //unschedule(switchOff)
    main_switch.on()
    if(null != sub_switch)
    	sub_switch.on()
        
	state.on_time = now()
}

def switch_on_handler(evt) {
    log("switch_on_handler called: $evt")
}

def switch_off_handler(evt) {
    log("switch_off_handler called: $evt")
    state.autoMode = false;
}

def log(msg)
{
	if(enableLog != null && enableLog == true)
    {
    	log.debug msg
    }
}