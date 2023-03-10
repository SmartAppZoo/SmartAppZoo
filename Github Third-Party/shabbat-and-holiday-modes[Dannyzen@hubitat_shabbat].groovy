/**
 *  Shabbat and Holiday Times for Hubitat
 *
 *  OriginalAuthor: danielbarak@live.com
 *  HubitatAuthor: @dannyzen
 *  Date: 2020-06-23
 *  Ported to Hubitat on 2019-10-02 by dannyzen@. See https://github.com/SmartThingsCommunity/SmartThingsPublic/blob/master/smartapps/shabbatholidaymode/shabbat-and-holiday-modes.src/shabbat-and-holiday-modes.groovy for original
 *  5/31: Adding a Havdalah Offset with a default value of 50
 *  6/23: Last Shabbos app failed to get time from Hebcal. Now adding in an optional switch to connect rule machine to in order to affirm Shabbos time is set
 */

definition(
        name: "Shabbat and Holiday Modes",
        namespace: "ShabbatHolidayMode",
        author: "@dannyzen",
        description: "Changes the mode at candle lighting and back after havdalah. Also allows for optoinal virtual device to turn on and off. Uses the HebCal.com API to look for days that are shabbat or chag and pull real time candle lighting and havdalah times to change modes automatically",
        category: "My Apps",
        iconUrl: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png",
        iconX2Url: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png",
        iconX3Url: "http://upload.wikimedia.org/wikipedia/commons/thumb/4/49/Star_of_David.svg/200px-Star_of_David.svg.png",
        pausable: true
)

preferences {

    section("At Candlelighting Change Mode To:")
            {
                input "startMode", "mode", title: ""
            }
    section("At Havdalah Change Mode To:")
            {
                input "endMode", "mode", title: ""
            }
    section("Your ZipCode") {
        input "zipcode", "text", title: "ZipCode", required: true
    }
    section("Havdalah Offset: Time to add to sundown time in order to calculate Havdalah time (Default: 50)") {
        input "havdalahOffset", "number", title: "Havdalah Offset", defaultValue: 50, required: false
    }
	section("Optional Shabbat Switch: The switch will turn ON when shabbat or holiday time is properly set. Good for knowing if app _did or did not_ schedule Shabbat mode.") {
			input "shabbatSwitch", "capability.switch", title: "Select a virtual switch", submitOnChange: true, required: false
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
    poll();
    schedule("0 0 0/3 ? * * *", poll)
    if(shabbatSwitch) subscribe(shabbatSwitch, "switch", handler)
}

def handler(evt) {
	log.info "$evt.device $evt.value"
}

//Check hebcal for today's candle lighting or havdalah
def poll() {
    unschedule("endChag")
    unschedule("setChag")
    if(shabbatSwitch) shabbatSwitch.off() // shabbat switch starts off
    Hebcal_WebRequest()

}//END def poll()


/**********************************************
 // HEBCAL FUNCTIONS
 -----------------------------------------------*/

//This function is the web request and response parse
import groovy.time.TimeCategory
import hubitat.helper.RMUtils

def rules = RMUtils.getRuleList()
log.trace(rules)

def Hebcal_WebRequest() {

    def today = new Date() //.format("yyyy-MM-dd")
    def testDate = new Date()
    def endEventDate = new Date()
    def hebcal_date
    def hebcal_category
    def hebcal_title
    def candlelightingLocalTime

    def urlRequest = "http://www.hebcal.com/hebcal/?v=1&cfg=json&nh=off&nx=off&year=now&month=now&mf=off&c=on&zip=${zipcode}&m=${havdalahOffset}"
    log.trace "${urlRequest}"


    def hebcal = { response ->
        hebcal_date = response.data.items.date
        hebcal_category = response.data.items.category
        hebcal_title = response.data.items.title
        for (int i = 0; i < hebcal_date.size; i++) {
            candlelightingLocalTime = HebCal_GetTime24(hebcal_date[i])
            startEventDate = toDateTime(hebcal_date[i])
            if (startEventDate - today == 0) {
                if (hebcal_category[i] == "candles") {
                    log.trace "Title ${hebcal_title[i]} Event Date: ${startEventDate}"
                    //Prod - Note 'eventDate'
                    schedule(startEventDate, setChag)
                    if(shabbatSwitch) shabbatSwitch.on() // if we schedule setChag, we turn on the shabbat switch

                    //testing
                    testDate.seconds = today.seconds + 10
                    log.trace "${today}"
                    log.trace "${testDate}"
                    //schedule(testDate, setChag)
                }//END if(hebcal_category=="candles")
                else if (hebcal_category[i] == "havdalah") {
                    log.trace "Title ${hebcal_title[i]} Event Date: ${startEventDate}"
                    endEventDate = toDateTime(hebcal_date[i])
                    //Prod
                    schedule(endEventDate, endChag)
                    //Testing
                    //schedule(testDate, setChag)
                }//END if(hebcal_category=="havdalah")
            }//END if(startEventDate - today == 0)
        }//END for (int i = 0; i < hebcal_date.size; i++)
    }//END def hebcal = { response ->
    httpGet(urlRequest, hebcal);
}//END def queryHebcal()


//This function gets candle lighting time
def HebCal_GetTime12(hebcal_title) {
    def returnTime = hebcal_title.split(":")[1] + ":" + hebcal_title.split(":")[2] + " "
    return returnTime
}//END def HebCal_GetTime12()

//This function gets candle lighting time
def HebCal_GetTime24(hebcal_date) {
    def returnTime = hebcal_date.split("T")[1]
    // log.trace "${returnTime}"
    returnTime = returnTime.split("-")[0]
    return returnTime
}//END def HebCal_GetTime12()

/*-----------------------------------------------
 END OF HEBCAL FUNCTIONS
-----------------------------------------------*/

def setChag() {

    if (location.mode != startMode) {
        if (location.modes?.find { it.name == startMode }) {
            setLocationMode(startMode)
        }//END else
    }//END if (location.mode != newMode)

    unschedule("setChag")
}//END def setChag()


def endChag() {

    if (location.mode != endMode) {
        if (location.modes?.find { it.name == endMode }) {
            setLocationMode(endMode)
            if(shabbatSwitch) shabbatSwitch.off() // If we end the chag, let's turn the shabbat switch off
        }//END if (location.modes?.find{it.name == endMode})
    }//END if (location.mode != endMode)

    unschedule("endChag")

}//END def setChag()

