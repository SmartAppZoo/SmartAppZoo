/**
 *  Synchronize one thermostat with another one
 *  This app written for my living room with conventional gas radiator heat and additional mini-split system
 *
 *  It checks every 10 minutes (or during setPointChange event) if the system needs slave thermostat to kick in.
 *
 *
 *  Copyright 2015 Andrei Zharov
 *
 */
definition(
        name: "Master-Slave thermostats",
        namespace: "belmass@gmail.com",
        author: "Andrei Zharov",
        description: "Have one thermostat control another one",
        category: "Convenience",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Master thermostat") {
        input "master", "capability.thermostat", multiple: false
    }
    section("Controls this thermostat") {
        input "slave", "capability.thermostat", multiple: false
        input "tempThreshold", "number", title: "Temperature Difference for slave to turn on"
        input "notify", "bool", title: "Notify?"
    }
}

def state = [];

/**
 * Triggered by SmartThings API when app is installed
 * @return
 */
def installed() {
    subscribeToEvents();
}

/**
 * Triggered by SmartThings API when app is updated
 * @return
 */
def updated() {
    // unsubscribe from active events
    unsubscribe();
    // subscribe to all events
    subscribeToEvents();
}

/**
 * Monitor changes in thermostat values
 * @return
 */
def subscribeToEvents() {
    subscribe(master, "heatingSetpoint", setpointHandler);
    subscribe(master, "coolingSetpoint", setpointHandler);
    subscribe(master, "temperature", setpointHandler);
    scheduleTemperatureCheck();
}

/**
 * Schedule check every 10 min
 * @return
 */
def scheduleTemperatureCheck() {
    runEvery10Minutes(checkIfSlaveIsNeeded);
}

/**
 * Check room temperature every 10 minutes and if it's more than threshold, turn on the slave,
 * if it's less or equal then turn off the slave
 */
def checkIfSlaveIsNeeded() {
    def masterMode = master.currentThermostatMode;
    def slaveMode = slave.currentThermostatMode;

    def masterHeatingTemp = master.currentHeatingSetpoint.toInteger();
    def masterCoolingTemp = master.currentCoolingSetpoint.toInteger();
    def slaveCoolingTemp = slave.currentCoolingSetpoint.toInteger();
    def slaveHeatingTemp = slave.currentHeatingSetpoint.toInteger();

    def roomTemperature = master.currentValue("temperature").toInteger();

    log.debug "---Performing scheduled check---";
    log.debug "Master mode is: $masterMode";
    log.debug "masterHeatingTemp is: $masterHeatingTemp";
    log.debug "masterCoolingTemp is: $masterCoolingTemp";
    log.debug "Slave Mode is: $slaveMode";
    log.debug "slaveCoolingTemp is: $slaveCoolingTemp";
    log.debug "slaveHeatingTemp is: $slaveHeatingTemp";
    log.debug "roomTemperature is: $roomTemperature";
    log.debug "tempThreshold is: $tempThreshold";
    log.debug "---Completed Check ---"

    def needStartSlaveToHeat = (masterMode == "heat" && masterHeatingTemp >= roomTemperature + tempThreshold);
    def needStartSlaveToCool = (masterMode == "cool" && masterCoolingTemp <= roomTemperature + tempThreshold);
    def slaveAlreadyHeatingToTemp = (slaveMode == "heat" && masterHeatingTemp == slaveHeatingTemp);
    def slaveAlreadyCoolingToTemp = (slaveMode == "cool" && masterCoolingTemp == slaveCoolingTemp);

    // if both systems in the same mode and we don't need slave and room temperature is better than set on master thermostat
    def needTurnOffSlave = (slaveMode != "off") && (slaveMode == masterMode) && (needStartSlaveToHeat == false && needSlaveToCool == false) && (masterHeatingTemp <= roomTemperature || masterCoolingTemp >= roomTemperature);

    log.debug "---Decision Points---";
    log.debug "needStartSlaveToHeat: $needStartSlaveToHeat";
    log.debug "needStartSlaveToCool: $needStartSlaveToCool";
    log.debug "needTurnOffSlave: $needTurnOffSlave";
    log.debug "slaveAlreadyHeatingToTemp: $slaveAlreadyHeatingToTemp";
    log.debug "slaveAlreadyCoolingToTemp: $slaveAlreadyCoolingToTemp";
    log.debug "--- End Decision Points---"

    if (needStartSlaveToHeat && !slaveAlreadyHeatingToTemp) {
        // if current mode is heat and desired temp more than current by tempThreshold degree
        log.debug "Current mode is heat and desired temp more than current by tempThreshold degree";
        log.debug "Syncing slave with master";
        syncSlaveWithMaster(masterHeatingTemp.toDouble());
    } else if (needStartSlaveToCool && !slaveAlreadyCoolingToTemp) {
        // if current mode is cool and desired temp less than current by tempThreshold degree
        log.debug "Syncing slave with master";
        log.debug "Current mode is cool and desired temp more than current by tempThreshold degree";
        syncSlaveWithMaster(masterCoolingTemp.toDouble());
    } else if (needTurnOffSlave) {
        // we don't need slave
        log.debug "Turn off slave as it's not needed";
        turnOffSlave();
    } else {
        log.debug "No changes this checkIfSlaveIsNeeded cycle";
    }
}

/**
 * Sync master mode to slave mode
 */
def setMode() {
    def masterMode = master.currentThermostatMode;
    def slaveMode = slave.currentThermostatMode;

    log.debug "Master thermostat mode is: $masterMode";
    log.debug "Slave thermostat mode is: $slaveMode";

    if (slaveMode != masterMode) {
        log.debug "Sync master mode to slave: $masterMode";
        slave."$masterMode"();
    }

    state.slaveMode = masterMode;
}

/**
 * Trigger for `heatingSetpoint` event from master
 * @param evt SmartThings event
 */
def setpointHandler(evt) {
	log.debug "This event name is ${evt.name}"
    log.debug "New Setpoint value: ${evt.value}";
    checkIfSlaveIsNeeded();
}

/**
 * Synchronise slave temperature and mode with master
 * setNewTemperature is delayed because both events will be send by IR and slave needs some time
 * to process them one by one
 *
 * @param temp
 * @return
 */
def syncSlaveWithMaster(temp) {
    state.temperature = temp;
    setMode();
    startTimer(10, "setNewTemperature");
}

/**
 * Set new temperature to slave
 * @return
 */
def setNewTemperature() {
    log.debug "Setting scheduled temperature value: $state.temperature";

    if (state.slaveMode == "heat") {
        slave.setHeatingSetpoint(state.temperature);
    } else if (state.slaveMode == "cool") {
        slave.setCoolingSetpoint(state.temperature);
    }

    sendMessage("Slave thermostat has been changed to $state.temperature F in $state.slaveMode mode");
}

/**
 * Turn off slave
 * @return
 */
def turnOffSlave() {
    state.slaveMode = "off";
    slave.off();
    sendMessage("Slave thermostat has been turned off");
}

/**
 * Helper function to schedule an event in x seconds
 * @param seconds Delay before calling a function
 * @param function Method to call in x seconds
 * @return
 */
def startTimer(seconds, function) {
    log.debug "Scheduling slave setpoint in $seconds seconds";

    def now = new Date();
    def runTime = new Date(now.getTime() + (seconds * 1000));
    runOnce(runTime, function) // runIn isn't reliable, use runOnce instead
}

/**
 * Send push notification if requested
 * @param msg Message to send
 * @return
 */
def sendMessage(msg) {
    if (notify) {
        sendPush msg
    }
}
