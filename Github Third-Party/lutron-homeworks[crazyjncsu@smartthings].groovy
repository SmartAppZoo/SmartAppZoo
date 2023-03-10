definition(
    name: 'Lutron HomeWorks',
    namespace: 'crazyjncsu',
    author: 'Jake Morgan',
    description: 'Lutron HomeWorks integration via native processor-embedded web server',
    category: 'Convenience',
    iconUrl: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png',
    iconX2Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png',
    iconX3Url: 'https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png'
)

preferences {
    input 'processorAuthority', 'text', title: 'Processor', description: 'Processor endpoint in format: host[:port]', defaultValue: '192.168.1.86:80', required: true, displayDuringSetup: true
    input 'deviceNameFormat', 'text', title: 'Device Name Format', description: 'Format of device names when added; %1$s=keypad; %2$03d=number; %3$s=engraving', defaultValue: '%1$s %3$s', required: true, displayDuringSetup: true
    input 'keypadFilterExpression', 'text', title: 'Keypad Filter Expression', description: 'Optional regex to filter keypads', defaultValue: '', required: false, displayDuringSetup: true
    input 'buttonFilterExpression', 'text', title: 'Button Filter Expression', description: 'Optional regex to filter buttons', defaultValue: '', required: false, displayDuringSetup: true
    input 'statusPollIntervalSeconds', 'number', title: 'Status Poll Interval in Seconds', defaultValue: '5', required: false, displayDuringSetup: true
	input 'pressAttemptCount', 'number', title: 'Number of Attempts to Toggle an LED', defaultValue: '1', required: false, displayDuringSetup: true
}

def installed() {
    initialize();
}

def updated() {
    unschedule();
    unsubscribe();
    initialize();
}

def uninstalled() {
    getChildDevices().each { deleteChildDevice(it.deviceNetworkId); }
}

def initialize() {
    runEvery1Hour(performEveryHourProcessing);
    runEvery1Minute(performEveryMinuteProcessing);
	performEveryHourProcessing();
	performEveryMinuteProcessing();
}

def performEveryHourProcessing() {
	// sync devices
    sendLutronHttpGets([[fileBaseName: 'keypads', queryStringMap: [sync:'1']]]);
    
    // invalidate cache just in case something somehow got out of sync
    // cache is used to avoid calls to getAllChildDevices that we suppose is expensive because they decided to log the call; it's at least noisy
    state.cachedKeypadIDs = null;
    state.cachedKeypadLedsStringMap = null;
}

def performEveryMinuteProcessing() {
	// if not restarted every minute, this guy tends to timeout and die
    if (!state.lastSyncStatusRunTime || new Date().getTime() - state.lastSyncStatusRunTime > (statusPollIntervalSeconds * 1000) + 10000) {
    	log.info("Restarted status loop because of timeout");
    	runSyncStatusLoop();
    }
}

def runSyncStatusLoop() {
    runIn(statusPollIntervalSeconds, runSyncStatusLoop);
    
    state.lastSyncStatusRunTime = new Date().getTime();
    
    if (!state.cachedKeypadIDs)
    	state.cachedKeypadIDs = getAllChildDevices().collect { it.deviceNetworkId.split(':')[0] }.unique();
    
    sendLutronHttpGets(state.cachedKeypadIDs.collect { [fileBaseName:'leds', queryStringMap: [keypad: it]] });
}

def sendLutronHttpGets(requestInfos) {
    def hubActions = requestInfos.collect {
        def hubAction = new physicalgraph.device.HubAction(
            [
                method: 'GET',
                path: "/${it.fileBaseName}.xml",
                query: it.queryStringMap,
                headers: [ Host: processorAuthority ],
            ],
            null,
            [ callback: handleLutronHttpResponse ]
        );

        hubAction.requestId = it.queryStringMap
        	.plus([fileBaseName: it.fileBaseName])
        	.plus([time: new Date().getTime().toString()])
        	.collect { "${URLEncoder.encode(it.key)}=${URLEncoder.encode(it.value)}" }.join('&');
        
        log.info("sendLutronHttpGet: ${hubAction.requestId}");

        return hubAction;
    }

	//log.info("d0: " + getAllChildDevices()[0])
	//log.info("h0: " + location.getHubs()[0].getId())
    sendHubCommand(hubActions, 400);
    //getAllChildDevices()[0].sendHubCommand(hubActions, 400);
}

def handleLutronHttpResponse(physicalgraph.device.HubResponse response) {
    def requestData = response.requestId.split('&').collect { it.split('=') }.collectEntries { [(URLDecoder.decode(it[0])): URLDecoder.decode(it[1])] };
    log.info("handleLutronHttpResponse (${new Date().getTime() - requestData.time.toLong()} ms): ${response.requestId}");

    switch (response.xml?.name()) {
        case 'Project':
            def matchingKeypadNodes = response.xml?.HWKeypad?.findAll { keypadFilterExpression == null || it.Name.text() =~ keypadFilterExpression };
            def keypadAddressSet = matchingKeypadNodes.collect { it.Address.text() }.toSet();
        
            getAllChildDevices().findAll { !keypadAddressSet.contains(it.deviceNetworkId.split(':')[0]) }.each { deleteChildDevice(it.deviceNetworkId) };
            
            sendLutronHttpGets(matchingKeypadNodes.collect { [fileBaseName: 'buttons', queryStringMap: [keypad: it.Address.text(), name: it.Name.text()]] });
            
            break;
        case 'List':
            def matchingButtonNodes = response.xml?.Button?.findAll { it.Type.text() == 'LED' && it.Engraving.text() }.findAll { buttonFilterExpression == null || it.Engraving.text() =~ buttonFilterExpression };
            def buttonNumberSet = matchingButtonNodes.collect { it.Number.text() }.toSet();

            getAllChildDevices().findAll { it.deviceNetworkId.split(':')[0] == requestData.keypad && !buttonNumberSet.contains(it.deviceNetworkId.split(':')[1]) }.each { deleteChildDevice(it.deviceNetworkId) };
            
            matchingButtonNodes.each {
                def deviceNetworkId = requestData.keypad + ':' + it.Number.text();
                def deviceName = String.format(deviceNameFormat, requestData.name, it.Number.text().toInteger(), it.Engraving.text());
                def existingChildDevice = getChildDevice(deviceNetworkId);

				if (existingChildDevice == null) {
                    addChildDevice('erocm123', 'Switch Child Device', deviceNetworkId, location.getHubs()[0].id, [name: deviceName, completedSetup: true]);
                } else {
                	// TODO test; probably doesn't work
                	if (existingChildDevice.name != deviceName) existingChildDevice.name = deviceName;
                }
            }
        
            if (matchingButtonNodes.size() != 0)
                sendLutronHttpGets([[fileBaseName: 'leds', queryStringMap: [keypad: it.keypad]]]);    

            state.cachedKeypadIDs = null;

            break;
        case 'LED':
            def ledsString = response.xml?.LEDs.text();
            
            if (!state.cachedKeypadLedsStringMap)
            	state.cachedKeypadLedsStringMap = [:];

            if (requestData.state || state.cachedKeypadLedsStringMap[requestData.keypad] != ledsString) {
                log.info("Keypad LEDs changed (keypad: ${requestData.keypad})");
                
                getAllChildDevices().findAll { it.deviceNetworkId.split(':')[0] == requestData.keypad }.each {
                    def buttonNumberString = it.deviceNetworkId.split(':')[1];
                    def currentState = ledsString.charAt(buttonNumberString.toInteger()) == '1' ? 'on' : 'off';

                    it.sendEvent(name: 'switch', value: currentState);
                    log.info("Device state discovered (name: ${it.name}; currentState: $currentState)");

                    if (buttonNumberString == requestData.button && requestData.state != 'unspecified') {
                        def attempts = requestData.attempts ? requestData.attempts.toInteger() : 0;

                        if (currentState != requestData.state && attempts++ < pressAttemptCount) {
                        	log.info("Sending press/release (keypad: ${requestData.keypad}; button: $buttonNumberString; name: ${it.name})");
                            sendLutronHttpGets([
                                [fileBaseName: 'action', queryStringMap: [keypad: requestData.keypad, button: buttonNumberString, action: 'press']],    
                                [fileBaseName: 'action', queryStringMap: [keypad: requestData.keypad, button: buttonNumberString, action: 'release']],
                                [fileBaseName: 'leds', queryStringMap: [keypad: requestData.keypad, button: requestData.button, state: requestData.state, attempts: attempts.toString() ]],
                            ]);
                        }
                    }
                }
                
                state.cachedKeypadLedsStringMap[requestData.keypad] = ledsString;
            }
            
            break;
        case 'Action':
            // don't care here, as an leds was always issued after this one
            break;
    }
}

def childOn(childDeviceNetworkId) {
    setChildDeviceState(childDeviceNetworkId, 'on');
}

def childOff(childDeviceNetworkId) {
    setChildDeviceState(childDeviceNetworkId, 'off');
}

def childRefresh(childDeviceNetworkId) {
    setChildDeviceState(childDeviceNetworkId, 'unspecified');
}

def setChildDeviceState(childDeviceNetworkId, state) {
    log.info("Setting child device state (childDeviceNetworkId: $childDeviceNetworkId; state: $state)");
    // check that the state of the leds to make sure we even have to press the button to achieve the desired state
    sendLutronHttpGets([[fileBaseName: 'leds', queryStringMap: [keypad: childDeviceNetworkId.split(':')[0], button: childDeviceNetworkId.split(':')[1], state: state]]]);    
}
