/**
 *  Repeat Switch (v.0.0.1)
 *
 *  Authors
 *   - oukene
 *  Copyright 2021
 *
 */
definition(
    name: "Repeat Switch",
    namespace: "oukene/smart-switch",
    author: "oukene",
    description: "반복 스위치",
    category: "My Apps",
    
    parent: "oukene/smart-switch/parent:Smart Switch",
    iconUrl: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png",
    iconX2Url: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png",
    iconX3Url: "https://cdn4.iconfinder.com/data/icons/music-player-47/32/repeat_rewind_arrow_interface_refresh-512.png"
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
        if(state.initialize)
        {
            try
            {
                section("현재 상태") {
                    paragraph "- DashBoard", image: "https://cdn4.iconfinder.com/data/icons/finance-427/134/23-512.png"
                    paragraph "[ " + (actuatorType ? "$actuatorType - $actuator" : "") + "switch - $main_switch ]"
                    //paragraph "[ $actuatorType - $actuator, switch - $main_switch ]"
                    paragraph "현재상태: " + main_switch.currentSwitch
                    paragraph "" + (main_switch.currentSwitch == "on" ? "종료 예정 시각: " : "작동 예정 시각: ") + new Date(state.next_operator_time).format('yyyy-MM-dd HH:mm:ss.SSS', location.getTimeZone())
                }
            }
            catch(all) { }
        }          
        section() {
            href "switchPage", title: "설정", description:"", image: "https://cdn4.iconfinder.com/data/icons/industrial-1-4/48/33-512.png"
        }
        if(state.initialize)
        {
            section()
            {
                href "optionPage", title: "옵션", description:"", image: "https://cdn4.iconfinder.com/data/icons/multimedia-internet-web/512/Multimedia_Internet_Web-16-512.png"
            }
        }
    }
}

def switchPage()
{
    dynamicPage(name: "switchPage", title: "", nextPage: "optionPage")
    {
        section("스위치 설정") {
            input(name: "main_switch", type: "capability.switch", title: "이 스위치를 켭니다(메인스위치)", multiple: false, required: true)
            input(name: "sub_switch", type: "capability.switch", title: "이 스위치들을 켭니다(보조스위치)", multiple: true, required: false)
        }
    }
}


def optionPage()
{
    dynamicPage(name: "optionPage", title: "")
    {
        section("그리고 아래 옵션을 적용합니다") {
        	input "onPeriod", "number", required: true, title: "켜짐 유지 시간(초)", defaultValue: "10"
            input "offPeriod", "number", required: true, title: "꺼짐 유지 시간(초)", defaultValue: "10"
            input "enableLog", "bool", title: "로그활성화", required: true, defaultValue: false
        }
        timeInputs()
        
        section("자동화 on/off")
        {
            input "enable", "bool", title: "활성화", required: true, defaultValue: true
        }
        
        if (!overrideLabel) {
            // if the user selects to not change the label, give a default label
            def l = main_switch.displayName + ": Repeat Switch"
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

def timeInputs() {
    section {
        input "startTime", "time", title: "자동화 작동시작", required: false
        input "endTime", "time", title: "자동화 작동종료", required: false
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
    
    subscribe(main_switch, "switch.off", switch_off_handler)
    subscribe(main_switch, "switch.on", switch_on_handler)

    if(startTime != null)
        schedule(startTime, scheduleOnHandler)
        
	if(endTime != null)
        schedule(endTime, scheduleOffHandler)
    
    state.next_operator_time = now()
    state.repeat_reservation = false
    
    /*
	if(null == startTime)
    {
    	log("initialize start")
    	runIn(1, scheduleOnHandler)
    }
    */
    state.initialize = true
}

def switch_for_repeat()
{
	log("switch_for_repeat : $main_switch.currentSwitch")
    
	if(!isBetween())
    {
    	log("IsBetween is false, cancel repeat")
    	return
    }
    
	def period = (main_switch.currentSwitch == "on" ? onPeriod : offPeriod)
    
	state.next_operator_time = now() + (period * 1000)
    state.repeat_reservation = true
    
    //schedule(state.next_operator_time, switch_on_off)
    runIn(period, switch_on_off, [overwrite: true])
}


def switch_on_handler(evt) {
	log.debug "switch_on_handler"
    
    if(sub_switch)
    {
    	sub_switch.on()
    }
    
    switch_for_repeat()
}

def switch_off_handler(evt) {
	log("switch_off_handler")
    
    // 보조 스위치를 종료 시킨 후
    if(sub_switch)
    {
    	sub_switch.off()
    }
    
    if(state.repeat_reservation == true)
    {
    	log("stop repeat")
    	resetSwitch()
    }
    else
    {
    	switch_for_repeat()
    }
}


def resetSwitch()
{
	log("reset switch")
    // 메인 스위치가 꺼져 있을 경우는 변수만 변경, 켜져 있을경우는 main 스위치를 끄면서 handler 에서 다시 호출되어 종료 처리
    if(main_switch.currentSwitch == "off")
    {
    	state.repeat_reservation = false
    }
    else
    {
        main_switch.off()
	}
}

def switch_on_off()
{
	log("switch_on_off - repeat_reservation : $state.repeat_reservation")
	if(state.repeat_reservation == false) return
    
	state.repeat_reservation = false
	if("on" == main_switch.currentSwitch)
    {
    	//state.repeat_switch = true
    	main_switch.off()
    }
    else
    {
    	if(isBetween())
        {
        	//state.repeat_switch = true
        	main_switch.on()
        }
    }
}

def isBetween()
{
	def ret = true
    if(null != startTime && null != endTime) { ret = timeOfDayIsBetween(startTime, endTime, new Date(), location.timeZone) }
    log("between: $ret")
    return ret
}

def scheduleOnHandler()
{
	// 동작 조건이 지정되어있지 않을때만 자동 on
    log("start schedule - currentSwitch : $main_switch.currentSwitch")
    // 현재 스위치가 꺼져 있으면 스위치를 켜는 동작으로 반복을 실행하고 현재 켜져 있으면 반복 액션만 실행 
    state.repeat_reservation = true
    if(main_switch.currentSwitch == "off")
    {
    	log("main switch on")
    	main_switch.on()
	}
    else
    {
        switch_for_repeat()
	}
}

def scheduleOffHandler()
{
	log("end schedule")
    resetSwitch()
}

def log(msg)
{
	if(enableLog != null && enableLog == true)
    {
    	log.debug msg
    }
}