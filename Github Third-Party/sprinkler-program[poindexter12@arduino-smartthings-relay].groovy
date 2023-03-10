definition(
  name: "Irrigation Timer",
  namespace: "poindexter12",
  author: "http://github.com/poindexter12",
  description: "Irrigation timer for sprinklers",
  version: "1.0",
  iconUrl: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor12-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Outdoor/outdoor12-icn@2x.png"
  )
  preferences {
    page(name: "generalPage", title: "Create An Irrigation Schedule", install: true, uninstall: true, nextPage: "schedulePage") {
      section {
        label name: "schedulename", title: "Schedule Name", required: true, multiple: false
        input name: "relays", type: "capability.switch", title: "Select a relay switch", require: true, multiple: false
        input name: "zipcode", type: "number", title: "Enter rain delay zip code", required: false
      }
      section{
        input name: "days", type: "enum", title: "Choose days", required: true, multiple: true, metadata: [values: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']]
        input name: "starttime", type: "time", title: "Enter a start time", required: true
      }
      section{
        app(name: "relays", appName: "Sprinkler Relay", namespace: "poindexter12", title: "Add Relay...", multiple: true)
      }
    }
  }

  def installed() {
    unsubscribe()
    log.debug "Installed with settings: ${settings}"
    initialize()
  }

  def updated() {
    log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
  }

  def initialize() {
    // build schedule
    state.wateringSchedule = buildSchedule()
    // set current state
    state.wateringState = ""
    // schedule to run once a minute
    schedule("0 * * * * ?", minuteHandler)
  }

  def minuteHandler() {
    // check for a watering day
    def currentDay = new Date().format("EEEE", location.timeZone)
    if (!days.contains(currentDay)) {
      log.debug "not a watering day, leaving"
      changeWateringState("")
      return
    }
    // check for precipitation, return if too large
    if (zipcode != null && zipcode != "")
    {
      def recentPrecipitation = getPrecipitationInches();
      if (recentPrecipitation > 0.5){
        log.debug "too much precipitation, leaving"
        changeWateringState("")
        return
      }
    }

    // toggle state
    def time = getLocalTime();
    def totalMinutes = time[0] * 60 + time[1];
    def newWateringState = state.wateringSchedule.get(totalMinutes.toString());
    changeWateringState(newWateringState)
  }

  def getLocalTime(date = new Date()){
    // get offset hours
    def offset = location.timeZone.getOffset(date.getTime()) / 1000 / 60 / 60;
    def utcHours = date.getHours();
    int difference = utcHours + offset;
    int paddedDifference = difference + 24;
    int offsetHours = paddedDifference % 24;
    return [offsetHours, date.getMinutes()];
  }

  def getScheduleStartMinutes(){
    // ignore offset because we want local time
    def startTime = Date.parse("yyy-MM-dd'T'HH:mm:ss.SSSz", settings.starttime);
    def offsetTime = getLocalTime(startTime);
    return offsetTime[0] * 60 + offsetTime[1];
  }

  def buildSchedule(){
    // get child apps
    def children = getChildApps()
    // map for times
    def timingMap = [:]
    // start minutes
    def currentMinute = getScheduleStartMinutes()

    children.each { child ->
      child.time.times{
        timingMap.put(currentMinute.toString(), child.relay)
        currentMinute++
      }
    }
    log.debug "map: $timingMap"
    return timingMap;
  }

  def changeWateringState(wateringState){
    // if they are the same, then just return
    if (wateringState == state.wateringState){
      return;
    }
    // turn off current state
    if (state.wateringState != ""){
      log.debug "turning off $state.wateringState"
      relays.relayoff(state.wateringState);
      // allow the command to process
      pause(2000);
    }
    // set the new state
    state.wateringState = wateringState
    // turn on current state
    if (state.wateringState != ""){
      log.debug "turning on $state.wateringState"
      relays.relayon(state.wateringState);
      // allow the command to process
      pause(2000);
    }
  }

  def getPrecipitationInches() {
    // rain yesterday
    def yesterdaysWeather = getWeatherFeature("yesterday", "$zipcode")
    def yesterdaysPrecipitation = floatOrZero(yesterdaysWeather.history.dailysummary.precipi.toArray()[0])
    // rain today
    def todaysWeather = getWeatherFeature("conditions", "$zipcode")
    def todaysPrecipitation = floatOrZero(todaysWeather.current_observation.precip_today_in)
    // forecast rain for today
    def forecastWeather = getWeatherFeature("forecast", "$zipcode")
    def forecastPrecipitation = floatOrZero(forecastWeather.forecast.simpleforecast.forecastday.qpf_allday.in.toArray()[0])
    // add them up
    def totalPrecipitation = yesterdaysPrecipitation + todaysPrecipitation + forecastPrecipitation;
    log.debug "Total precipitation for $zipcode: $totalPrecipitation inches"
    return totalPrecipitation;
  }

  def floatOrZero(value) {
    return value && value.isFloat() ? value.toFloat() : 0.0;
  }
