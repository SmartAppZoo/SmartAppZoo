/**
 *  Azan
 *
 *  Copyright 2020 Hussein Khalil
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
    name: 'El-Azan 2.0', namespace: 'husseinmohkhalil', author: 'Hussein Khalil', description: 'this smart app trigger google home speaker or group to call for Azan ', category: 'My Apps',
    iconUrl: 'https://img2.apksum.com/7a/com.worldsalatapp.accurate.prayer/6.3/icon.png',
    iconX2Url: 'https://img2.apksum.com/7a/com.worldsalatapp.accurate.prayer/6.3/icon.png', iconX3Url: 'https://img2.apksum.com/7a/com.worldsalatapp.accurate.prayer/6.3/icon.png')

preferences {
    section('Fajr') {
        input(name: 'FajrTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'FajrVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'FajrIsActive', type: 'bool', title: 'Is Active', required: false, defaultValue: true)
        input(name: 'FajrActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    }

    section('Zoher') {
        input(name: 'ZoherTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'ZoherVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'ZoherIsActive', type: 'bool', title: 'Is Active', required: false, defaultValue: true)
        input(name: 'ZoherActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    }

    section('Asr') {
        input(name: 'AsrTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'AsrVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'AsrIsActive', type: 'bool', title: 'Is Active', required: false, defaultValue: true)
        input(name: 'AsrActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    }

    section('Maghreb') {
        input(name: 'MaghrebTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'MaghrebVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'MaghrebIsActive', type: 'bool', title: 'Is Active', required: false, defaultValue: true)
        input(name: 'MaghrebActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    }

    section('Isa') {
        input(name: 'IsaTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'IsaVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'IsaIsActive', type: 'bool', title: 'Is Active', required: false, defaultValue: true)
        input(name: 'IsaActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    }

    section('Ramadan') {
        input(name: 'IsRamadanModeActive', type: 'bool', title: 'Is Ramadan', required: true, defaultValue: false)
        input(name: 'EnableKidsMode', type: 'bool', title: 'Enable Kids Mode', required: true, defaultValue: false)
        input(name: 'KidsIftarHour', type: 'number', title: 'Kids Iftar Hour', required: false, defaultValue: '')
        input(name: 'KidsIftarTargets', type: 'capability.musicPlayer', title: 'Target Google Home Speakers or Groups', multiple: true, required: true)
        input(name: 'KidsIftarVolume', type: 'number', title: 'Target Volume', required: true, defaultValue: '50')
        input(name: 'KidsActiveDays', type: 'enum', title: 'Active Days', required: true, multiple: true, options: ['Monday': 'Monday', 'Tuesday': 'Tuesday', 'Wednesday': 'Wednesday', 'Thursday': 'Thursday', 'Friday': 'Friday', 'Saturday': 'Saturday', 'Sunday': 'Sunday'], defaultValue: ['Saturday', 'Sunday'])
    }
}

def installed() {
    log.debug "El-Azan 2.0 Installed "
    //log.debug "with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "El-Azan 2.0 Updated"
    //log.debug "with settings: ${settings}"
    unsubscribe()
    initialize()
}

def initialize() {
    log.info ' El-Azan 2.0 Smartapp Started'
    GoAzan()
}

def GoAzan() {

    //NOTE: the logic is in PlayNextAzanOnTime(output) function 
    CallAzanBridgeApi("nextprayarevent", "PlayNextAzanOnTime");

}

def PlayNextAzanOnTime(output) {
    try {
        log.debug "Starting response parsing on ${output}"
        def msg = output

        def headerMap = msg.headers // => headers as a Map
        def body = msg.body // => request body as a string
        def status = msg.status // => http status code of the response
        def data = msg.data // => either JSON or XML in response body (whichever is specified by content-type header in response)
        //log.debug "headers: ${headerMap}, status: ${status}, body: ${body}, data: ${data}"

        //what we need here is data (the json object containing the next azan time)
        log.debug "API request statusCode : ${status}"

        if (status == 200) {
log.debug "i am at 1"
log.debug "data: " + data

            def outputJasonData = new groovy.json.JsonOutput().toJson(data)
log.debug "outputJasonData: " + outputJasonData

            def JsonObject = new groovy.json.JsonSlurper().parseText(outputJasonData)
            assert JsonObject instanceof List
log.debug "i am at 2"
log.debug JsonObject[0]
log.debug "i am at 3.0"

            def nextAzanObject = JsonObject[0]
            assert nextAzanObject instanceof Map
            log.debug "i am at 3"
            

            sendPush("El-Azan 2.0: ${nextAzanObject.name} at ${nextAzanObject.time}")
            log.debug "Azan ${nextAzanObject.name} at ${nextAzanObject.time} is after ${nextAzanObject.remaingSeconds} s"
log.debug "i am at 4"
            def nextPrayEvent;
            //check first that the next Event is not Kids Iftar
            if (IsRamadanModeActive && EnableKidsMode) {
                try {
                    def KidsIftarUTC = GetPrayerDateTimeInUTC(KidsIftarHour, '00')
                    def nextKidsIftarInSec = GetSecondsToPrayTime(KidsIftarUTC)
                    if (nextKidsIftarInSec > 0) {
                        nextPrayEvent = GetPrayerTimeObject('KidsIftar', nextKidsIftarInSec, KidsIftarVolume, EnableKidsMode, KidsActiveDays);
                        nextAzanObject.name = "KidsIftar";
                        sendPush("El-Azan 2.0: KidsIftar is next")
                    }
                } catch (e) {
                    sendPush('Kids Configuration not correct')
                    log.error 'Kids Configuration not correct'
                }
            }

		//Note that the names in the switch case is the spilling from the API not the applications spilling  (Dhuhr vs Zohr) 
            switch (nextAzanObject.name) {
            case 'Fajr':
                nextPrayEvent = GetPrayerTimeObject('Fajr', nextAzanObject.remaingSeconds, FajrVolume, FajrIsActive, FajrActiveDays)
                break

            case 'Dhuhr':
                nextPrayEvent = GetPrayerTimeObject('Zohr', nextAzanObject.remaingSeconds, ZoherVolume, ZoherIsActive, ZoherActiveDays)
                break

            case 'Asr':
                nextPrayEvent = GetPrayerTimeObject('Asr', nextAzanObject.remaingSeconds, AsrVolume, AsrIsActive, AsrActiveDays)
                break

            case 'Maghrib':
                nextPrayEvent = GetPrayerTimeObject('Maghreb', nextAzanObject.remaingSeconds, MaghrebVolume, MaghrebIsActive, MaghrebActiveDays)
                break

            case 'Isha':
                nextPrayEvent = GetPrayerTimeObject('Isha', nextAzanObject.remaingSeconds, IsaVolume, IsaIsActive, IsaActiveDays)
                break
            }

            def nextAzanTime = nextPrayEvent.Time
            log.info " Azan ${nextPrayEvent.Name} is after ${nextAzanTime} s "

            runIn(nextAzanTime, "PlayAzan", [data: [nextPrayEvent: nextPrayEvent, TargetDevice: nextPrayEvent.TargetDevices]])

            def nextCalculate = nextAzanTime;
           runIn(nextCalculate, GoAzan);

        } else {
            log.debug "Unable to locate device on your network"
        }

    } catch (e) {
        log.error "something went wrong in El-Azan 2.0  $e"
        sendPush("El-Azan 2.0: Exception in GoAzan Function")
        sendPush("$e")

        //rerun after 10 min
        runIn(600, GoAzan);
    }
}

def PlayAzan(data) {
    try {
        def prayEvent = data.nextPrayEvent
        def targetDevices = GetTargetDeviceByName(prayEvent.Name)
        def shouldPlayAzan = AzanIsEnabledAndActive(prayEvent)

        if (shouldPlayAzan) {
            sendPush("El-Azan 2.0: it is ${prayEvent.Name} Azan Time :) ")
            targetDevices.setLevel(prayEvent.Volume)

            def PlayBackUrl = GetPlayBackUrlBySalahName(prayEvent.Name)
            targetDevices.setTrack(PlayBackUrl)
        }
    } catch (e) {
        log.error "something went wrong in playing Azan Function: $e"
        sendPush('El-Azan 2.0: Exception in PlayAzan Function')
        sendPush("$e")

        //rerun after 10 min
        runIn(600, GoAzan)
    }
}

def GetPrayerTimeObject(name, time, volume, isActive, ActiveDays) {
        log.debug "name:${name} --- time: ${time} --- volume: ${volume} --- isActive: ${isActive} --- ActiveDays: ${ActiveDays}"

    return [Name: name, Time: time, Volume: volume, IsActive: isActive, ActiveDays: ActiveDays]
}

def GetTargetDeviceByName(name) {
    if (name == 'Fajr') return FajrTargets

    if (name == 'Zohr') return ZoherTargets

    if (name == 'Asr') return AsrTargets

    if (name == 'Maghreb') return MaghrebTargets

    if (name == 'Isha') return IsaTargets

    if (name == 'KidsIftar') return KidsIftarTargets
}

def GetPlayBackUrlBySalahName(name) {
    if (name == 'Fajr') return 'http://192.168.178.118:1000/azan/azan_cairo1.mp3'

    if (name == 'Zohr') return 'http://192.168.178.118:1000/azan/azan_cairo1.mp3'

    if (name == 'Asr') return 'http://192.168.178.118:1000/azan/azan_cairo1.mp3'

    if (name == 'Maghreb') {
        if (IsRamadanModeActive == true) {
            return 'http://192.168.178.118:1000/azan/Ramadan_maghreb_complete.mp3'
        } else {
            return 'http://192.168.178.118:1000/azan/azan_cairo1.mp3'
        }
    }

    if (name == 'Isha') return 'http://192.168.178.118:1000/azan/azan_cairo1.mp3'

    if (name == 'KidsIftar') return 'http://192.168.178.118:1000/azan/madfa3_only.mp3'
}

def GetPrayerDateTimeInUTC(prayHour, prayMinutes, isTomorrowPray = false) {
    def daysToAdd = isTomorrowPray ? 1 : 0
    def dtFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    def dtPrayFormat = dtFormat.replace('HH', prayHour as String).replace('mm', prayMinutes as String)
    def PrayDateInGermanyTime = new Date().plus(daysToAdd).format(dtPrayFormat, TimeZone.getTimeZone('Europe/Berlin'))
    assert PrayDateInGermanyTime instanceof String

    def PrayDateTimeInUTC = Date.parse(dtFormat, PrayDateInGermanyTime)
    assert PrayDateTimeInUTC instanceof Date

    return PrayDateTimeInUTC
}

def GetSecondsToPrayTime(PrayTimeInUTC) {
    long timeDiff
    long unxNow = new Date().getTime() / 1000
    long unxPrayTime = PrayTimeInUTC.getTime() / 1000

    timeDiff = unxPrayTime - unxNow

    return timeDiff
}

def AzanIsEnabledAndActive(prayEvent) {
    // check if today is one of the preset days-of-week
    def df = new java.text.SimpleDateFormat('EEEE')
    // Ensure the new date object is set to local time zone
    df.setTimeZone(location.timeZone)
    def day = df.format(new Date())
    //Does the preference input Days, i.e., days-of-week, contain today?
    def dayCheck = prayEvent.ActiveDays.contains(day)

    return dayCheck && prayEvent.IsActive
}

def CallAzanBridgeApi(endpoint, callBackMethod) {
    def targetPath = endpoint == null ? "/prayerevent" : "/prayerevent/" + endpoint;
    log.debug targetPath
    def httpRequest = [
        path: targetPath,
        method: "GET",
        headers: [
            HOST: "192.168.178.118:2000",
            "Content-Type": "application/json"
        ]
    ]
    try {
        def hubAction = new physicalgraph.device.HubAction(httpRequest, null, [callback: callBackMethod])
        log.debug "hub action: $hubAction"
        return sendHubCommand(hubAction)
    } catch (Exception e) {
        log.debug "Hit Exception $e on $hubAction"
    }
}