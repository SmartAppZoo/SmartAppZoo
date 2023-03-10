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
    name: "Azan",
    namespace: "husseinmohkhalil",
    author: "Hussein Khalil",
    description: "This is The Azaaaaan :)",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Which device(s) to Play Sound ") {
        input(name: "targets", type: "capability.musicPlayer", title: "Target dimmer switch(s)", multiple: true, required: true)
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
  
  GoAzan()
  
}

def GoAzan(){

   log.debug " I Started"

  def nextAzanTime = 60 // GetNextAzanTimeInSeconds();
  runIn(nextAzanTime, PlayAzan);
   log.debug " I am in between"
  def nextCalculate = 240 //  nextAzanTime + 120;
    runIn(nextCalculate, GoAzan);
   log.debug " I am done"

  
}

def PlayAzan() {
   log.debug " Play Azan"

    targets.setTrack("https://www.islamcan.com/audio/adhan/azan12.mp3");
}



def GetNextAzanTimeInSeconds() {

    def todatDay = new Date().format('dd') as int
    def todatMonth = new Date().format('MM') as int
    def todatYear = new Date().format('yyyy') as int

    def nextAzanTimeInSeconds

    def params = [
        uri: "http://api.aladhan.com/v1/calendar?latitude=47.9568123&longitude=7.7496747&method=2&month=${todatMonth}&year=${todatYear}",
        path: ""
    ]

    try {
        httpGet(params) {
            resp ->

                def outputJasonData = new groovy.json.JsonOutput().toJson(resp.data)
            def JsonObject = new groovy.json.JsonSlurper().parseText(outputJasonData)

            assert JsonObject instanceof Map
            assert JsonObject.data instanceof List
            assert JsonObject.data[todatDay] instanceof Map
            assert JsonObject.data[todatDay].timings instanceof Map

            //get timings [hh][mm] for each pray     
            def FajrTime = JsonObject.data[todatDay].timings.Fajr.split()[0].split(':')
            def ZohrTime = JsonObject.data[todatDay].timings.Dhuhr.split()[0].split(':')
            def AsrTime = JsonObject.data[todatDay].timings.Asr.split()[0].split(':')
            def MaghrebTime = JsonObject.data[todatDay].timings.Maghrib.split()[0].split(':')
            def IshaTime = JsonObject.data[todatDay].timings.Isha.split()[0].split(':')
            def TomorrowFajrTime = GetTomorrowFajr(JsonObject.data)

            // get corresponding UTC for each pray           
            def todayFajrUTC = GetPrayerDateTimeInUTC(FajrTime[0], FajrTime[1])
            def todayZohrUTC = GetPrayerDateTimeInUTC(ZohrTime[0], ZohrTime[1])
            def todayAsrUTC = GetPrayerDateTimeInUTC(AsrTime[0], AsrTime[1])
            def todayMaghrebUTC = GetPrayerDateTimeInUTC(MaghrebTime[0], MaghrebTime[1])
            def todayIshaUTC = GetPrayerDateTimeInUTC(IshaTime[0], IshaTime[1])
            def TomorrowFajrUTC = GetPrayerDateTimeInUTC(TomorrowFajrTime[0], TomorrowFajrTime[1], true)


            // get seconds remaining for each pray
            def todayFajrSec = GutSecondsToPrayTime(todayFajrUTC);
            def tomorrowFajrSec = GutSecondsToPrayTime(TomorrowFajrUTC);

            def nextZohrInSec = GutSecondsToPrayTime(todayZohrUTC);
            def nextAsrInSec = GutSecondsToPrayTime(todayAsrUTC);
            def nextMaghrebInSec = GutSecondsToPrayTime(todayMaghrebUTC);
            def nextIshaInSec = GutSecondsToPrayTime(todayIshaUTC);

            def nextFajrSec = todayFajrSec > 0 ? todayFajrSec : tomorrowFajrSec


            // add positive seconds to list and get the minimum value

            def allPrayingTimes = []

            if (nextFajrSec > 0)
                allPrayingTimes.add(nextFajrSec)

            if (nextZohrInSec > 0)
                allPrayingTimes.add(nextZohrInSec)

            if (nextAsrInSec > 0)
                allPrayingTimes.add(nextAsrInSec)

            if (nextMaghrebInSec > 0)
                allPrayingTimes.add(nextMaghrebInSec)

            if (nextIshaInSec > 0)
                allPrayingTimes.add(nextIshaInSec)

            def nextPrayTime = allPrayingTimes.min()

            return nextPrayTime
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

def GetTomorrowFajr(todayData) {

    def todatMonth = new Date().format('MM') as int
    def tomorrowDay = new Date().plus(1).format('dd') as int
    def tomorrowMonth = new Date().plus(1).format('MM') as int
    def tomorrowYear = new Date().plus(1).format('yyyy') as int

    if (todatMonth == tomorrowMonth) {
        def tomorrowFajrTime = todayData[tomorrowDay].timings.Fajr.split()[0].split(':')

        return tomorrowFajrTime
    } else {
        def params = [
            uri: "http://api.aladhan.com/v1/calendar?latitude=47.9568123&longitude=7.7496747&method=2&month=${TomorrowMonth}&year=${tomorrowYear}",
            path: ""
        ]

        try {
            httpGet(params) {
                resp ->

                    def outputJasonData = new groovy.json.JsonOutput().toJson(resp.data)
                def JsonObject = new groovy.json.JsonSlurper().parseText(outputJasonData)

                assert JsonObject instanceof Map
                assert JsonObject.data instanceof List
                assert JsonObject.data[tomorrowDay] instanceof Map
                assert JsonObject.data[tomorrowDay].timings instanceof Map

                def tomorrowFajrTime = JsonObject.data[tomorrowDay].timings.Fajr.split()[0].split(':')
                return tomorrowFajrTime
            }
        } catch (e) {
            log.error "something went wrong: $e"
        }
    }
}

def GetPrayerDateTimeInUTC(prayHour, prayMinutes, isTomorrowPray = false) {

    def daysToAdd = isTomorrowPray ? 1 : 0
    def dtFormat = "yyyy-MM-dd'T'HH:mm:ssZ"
    def dtPrayFormat = dtFormat.replace("HH", prayHour as String).replace("mm", prayMinutes as String)
    def PrayDateInGermanyTime = new Date().plus(daysToAdd).format(dtPrayFormat, TimeZone.getTimeZone('Europe/Berlin'))
    assert PrayDateInGermanyTime instanceof String

    def PrayDateTimeInUTC = Date.parse(dtFormat, PrayDateInGermanyTime)
    assert PrayDateTimeInUTC instanceof Date


    return PrayDateTimeInUTC

}

def GutSecondsToPrayTime(PrayTimeInUTC) {
    long timeDiff
    long unxNow = new Date().getTime() / 1000
    long unxPrayTime = PrayTimeInUTC.getTime() / 1000

    timeDiff = unxPrayTime - unxNow
    //add one min Just in case to override the delay
    timeDiff += 60
    return timeDiff
}