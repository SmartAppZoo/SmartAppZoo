/**
 *  HK Service Manager
 *
 *  Copyright 2015 Tyler Freckmann
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
    name: "HK Service Manager",
    namespace: "tylerfreckmann",
    author: "Tyler Freckmann",
    description: "Allows you to control your Omni from the SmartThings app. Perform basic functions like play, pause, stop, change track, and check artist and song name from the Things screen.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") {
    appSetting "ipAddress"}


preferences {
    page(name: "selectDevices")
}

def selectDevices() {
    log.trace "selectDevices()"
    // get SessionID
    atomicState.sessionID = atomicState.sessionID ?: initSession()["SessionID"]
    
    // get device list
    def devices = [:]
    
    deviceList(atomicState.sessionID)["DeviceList"].each {
        def roomName = it["GroupName"] == "harman" ? "" : "@"+it["GroupName"]
        devices.put(it["DeviceID"], it["DeviceName"] + roomName)
    }
    
    // have user choose devices to work with
    dynamicPage(name: "selectDevices", title: "Select Your Devices", uninstall: true, install:true) {
        section {
            paragraph "Tap below to see the list of HK Speakers available in your network and select the ones you want to connect to SmartThings."
            input(name: "speakers", type: "enum", title: "Speakers", description: "Tap to choose", required: true, options: devices, multiple:true)
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    initialize()
}

def uninstalled() {
    log.trace "uninstalled()"
    closeSession(atomicState.sessionID)
    removeChildDevices(getChildDevices())
}

def initialize() {
    log.trace "initialize()"
    
    // Create child devices
    settings.speakers.each { deviceID ->
        def existingDevice = getChildDevice(deviceID)
        // Check to see if this child device has already been created - if not: create it
        if (!existingDevice) {
            addChildDevice("tylerfreckmann", "HK Speaker", deviceID)
        }
        log.debug "added child device: ${deviceID}"
    }
    
    // Delete child devices that are no longer in the user's settings
    def delete = getChildDevices().findAll { !settings.speakers.contains(it.deviceNetworkId) }
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
    
    
    // Remove devices from the HKWHub Playback Session that are not in the user's settings
    // Get all the devices in the network
    def deviceIDs = []
    deviceList(atomicState.sessionID)["DeviceList"].each {
        deviceIDs << it["DeviceID"]
    }
    
    // Remove devices that are not in the user's settings
    deviceIDs.each { deviceID ->
        if (!getChildDevice(deviceID)) {
            removeDeviceFromSession(atomicState.sessionID, deviceID)
        }
    }
    
    // Update device settings
    settings.speakers.each { deviceID ->
        def childDevice = getChildDevice(deviceID)
        def deviceInfoMap = deviceInfo(atomicState.sessionID, deviceID)
        atomicState.currentSong = atomicState.currentSong ?: ["Title":"", "ID":""]
        deviceInfoMap.put("CurrentSong", atomicState.currentSong["Title"])
        childDevice.parseEventData(deviceInfoMap)
    }
}

def removeChildDevices(delete) {
    log.trace "removeChildDevices(${delete})"
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

// Handle commands

// Plays on the given child according to the following algorithm:
// If the given child is active, deactivate all other children and check to see if the playback session is paused
// If the playback session is paused, resume it; if not - play the first song in the playlist from the given
// child
// If the given child is not active, deactivate all other children and play the first song in the playlist on
// the given child
def play(childScript) {
    log.debug "Executing 'play'"
    
    // Check to see if the given child is active
    def devInfo = deviceInfo(atomicState.sessionID, childScript.device.deviceNetworkId)
    def active = devInfo["Active"]
    def playing = devInfo["IsPlaying"]
    
    if (active) {
        if (!playing) {
            // Check to see if playback session is paused
            def paused = playbackStatus(atomicState.sessionID)["PlaybackState"] == "PlayerStatePaused"

            if (paused) {
                resumeHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
                // Generate child event to update playing status
                def event = ["status":"playing"]
                childScript.generateEvent(event)
            } else {
                // Deactivate all children
                // Activate given child
                // Play first song in media list
                playFromChild(childScript)
            }
        }
    } else {
        // Deactivate all children
        // Activate given child
        // Play first song in media list
        playFromChild(childScript)
    }
}

// Deactivate all children
// Activate given child
// Play first song in media list
def playFromChild(childScript) {
    log.trace "playFromChild(${childScript})"
    // Get the first song in the playlist
    def playlist = mediaList(atomicState.sessionID)["MediaList"]
    if (playlist.size() == 0) {
        return
    }
    def firstSong = playlist[0]
    firstSong = ["Title":firstSong["Title"], "ID":firstSong["PersistentID"]]
    
    activateOneChild(childScript.device)
    
    // Play first song in media list
    playHubMedia(atomicState.sessionID, firstSong["ID"])
    // Generate child event to update playing status and trackDescription
    def event = ["status":"playing", "trackDescription":firstSong["Title"]]
    childScript.generateEvent(event)
    atomicState.currentSong = firstSong
}

def pause(childScript) {
    log.debug "Executing 'pause'"
    pausePlay(atomicState.sessionID)
    // Update child status
    def event = ["status":"paused"]
    childScript.generateEvent(event)
}

def stop(childScript) {
    log.debug "Executing 'stop'"
    stopPlay(atomicState.sessionID)
    // Update child status
    def event = ["status":"paused"]
    childScript.generateEvent(event)
}

def nextTrack(childScript) {
    log.debug "Executing 'nextTrack'"
    // Get media list
    def playlist = mediaList(atomicState.sessionID)["MediaList"]
    for ( int i = 0; i < playlist.size(); i++ ) {
        if (playlist[i]["PersistentID"] == atomicState.currentSong["ID"]) {
            i = i==playlist.size()-1 ? -1 : i
            atomicState.currentSong = ["Title":playlist[i+1]["Title"], "ID":playlist[i+1]["PersistentID"]]
            playHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
            def event = ["status":"playing", "trackDescription":atomicState.currentSong["Title"]]
            childScript.generateEvent(event)
            return
        }
    }
}

def playTrack(childScript, track) {
    log.debug "Executing 'playTrack'"
    
    activateOneChild(childScript.device)

    // Find song in media list
    def playlist = mediaList(atomicState.sessionID)["MediaList"]
    for ( int i = 0; i < playlist.size(); i++ ) {
        if (playlist[i]["Title"] == track) {
            atomicState.currentSong = ["Title":playlist[i]["Title"], "ID":playlist[i]["PersistentID"]]
            playHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
            def event = ["status":"playing", "trackDescription":atomicState.currentSong["Title"]]
            childScript.generateEvent(event)
            return
        }
    }
}

def activateOneChild(child) {
    // Deactivate all children
    def children = getChildDevices()
    children.each {
        removeDeviceFromSession(atomicState.sessionID, it.deviceNetworkId)
    }
    
    // Activate given child
    addDeviceToSession(atomicState.sessionID, child.deviceNetworkId)
}

def setLevel(childScript, level) {
    log.debug "Executing 'setLevel'"
    setVolumeDevice(atomicState.sessionID, childScript.device.deviceNetworkId, level)
    def event = ["level":level, "mute":(level==0 ? "muted" : "unmuted")]
    childScript.generateEvent(event)
    if (level==0) {
        atomicState.prevLevel = 20
    }
}

def playText(childScript, text) {
    log.debug "Executing 'playText'"
    // TODO: handle 'playText' command
}

def mute(childScript) {
    log.debug "Executing 'mute'"
    atomicState.prevLevel = getVolumeDevice(atomicState.sessionID, childScript.device.deviceNetworkId)["Volume"]
    setVolumeDevice(atomicState.sessionID, childScript.device.deviceNetworkId, 0)
    def event = ["level":0, "mute":"muted"]
    childScript.generateEvent(event)
}

def previousTrack(childScript) {
    log.debug "Executing 'previousTrack'"
    // Check time elapsed
    def timeElapsed = playbackStatus(atomicState.sessionID)["TimeElapsed"].toInteger()
    if (timeElapsed > 6) {
        playHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
        def event = ["status":"playing"]
        childScript.generateEvent(event)
        return
    }
    log.debug "time elapsed: $timeElapsed"
    // Get media list
    def playlist = mediaList(atomicState.sessionID)["MediaList"]
    for ( int i = 0; i < playlist.size(); i++ ) {
        if (playlist[i]["PersistentID"] == atomicState.currentSong["ID"]) {
            // Wrap around if out of bounds
            i = i==0 ? playlist.size() : i
            atomicState.currentSong = ["Title":playlist[i-1]["Title"], "ID":playlist[i-1]["PersistentID"]]
            playHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
            def event = ["status":"playing", "trackDescription":atomicState.currentSong["Title"]]
            childScript.generateEvent(event)
            return
        }
    }
}

def unmute(childScript) {
    log.debug "Executing 'unmute'"
    setVolumeDevice(atomicState.sessionID, childScript.device.deviceNetworkId, atomicState.prevLevel ?: 20)
    def event = ["level":atomicState.prevLevel ?: 20, "mute":"unmuted"]
    childScript.generateEvent(event)
}

def setTrack(track) {
    log.debug "Executing 'setTrack'"
    // TODO: handle 'setTrack' command
}

def resumeTrack(childScript) {
    log.debug "Executing 'resumeTrack'"
    // Check to see if the given child is active
    def devInfo = deviceInfo(atomicState.sessionID, childScript.device.deviceNetworkId)
    def active = devInfo["Active"]
    def playing = devInfo["IsPlaying"]
    
    if (active) {
        if (!playing) {
            // Check to see if playback session is paused
            def paused = playbackStatus(atomicState.sessionID)["PlaybackState"] == "PlayerStatePaused"

            if (paused) {
                resumeHubMedia(atomicState.sessionID, atomicState.currentSong["ID"])
                // Generate child event to update playing status
                def event = ["status":"playing"]
                childScript.generateEvent(event)
            }
        }
    }
}

def restoreTrack(track) {
    log.debug "Executing 'restoreTrack'"
    // TODO: handle 'restoreTrack' command
}

/**************************
 * REST UTILITY FUNCTIONS *
 **************************/

// All REST utility functions simply return the HTTP response data

def initSession() {
    log.trace "initSession()"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/init_session"
    ]
    get(params)
}

def closeSession(sessionID) {
    log.trace "closeSession(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/close_session",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def deviceCount(sessionID) {
    log.trace "deviceCount(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/device_count",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def deviceList(sessionID) {
    log.trace "deviceList(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/device_list",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def deviceInfo(sessionID, deviceID) {
    log.trace "deviceInfo(${sessionID}, ${deviceID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/device_info",
        query: ["SessionID":sessionID, "DeviceID":deviceID]
    ]
    get(params)
}

def addDeviceToSession(sessionID, deviceID) {
    log.trace "addDeviceToSession(${sessionID}, ${deviceID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/add_device_to_session",
        query: ["SessionID":sessionID, "DeviceID":deviceID]
    ]
    get(params)
}

def removeDeviceFromSession(sessionID, deviceID) {
    log.trace "removeDeviceFromSession(${sessionID}, ${deviceID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/remove_device_from_session",
        query: ["SessionID":sessionID, "DeviceID":deviceID]
    ]
    get(params)
}

def mediaList(sessionID) {
    log.trace "mediaList(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/media_list",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def playHubMedia(sessionID, persistentID) {
    log.trace "playHubMedia(${sessionID}, ${persistentID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/play_hub_media",
        query: ["SessionID":sessionID, "PersistentID":persistentID]
    ]
    get(params)
}

def pausePlay(sessionID) {
    log.trace "pausePlay(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/pause_play",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def resumeHubMedia(sessionID, persistentID) {
    log.trace "resumeHubMedia(${sessionID}, ${persistentID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/resume_hub_media",
        query: ["SessionID":sessionID, "PersistentID":persistentID]
    ]
    get(params)
}

def stopPlay(sessionID) {
    log.trace "stopPlay(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/stop_play",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def playbackStatus(sessionID) {
    log.trace "playbackStatus(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/playback_status",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def isPlaying(sessionID) {
    log.trace "isPlaying(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/is_playing",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def getVolume(sessionID) {
    log.trace "getVolume(${sessionID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/get_volume",
        query: ["SessionID":sessionID]
    ]
    get(params)
}

def getVolumeDevice(sessionID, deviceID) {
    log.trace "getVolumeDevice(${sessionID}, ${deviceID})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/get_volume_device",
        query: ["SessionID":sessionID, "DeviceID":deviceID]
    ]
    get(params)
}

def setVolume(sessionID, volume) {
    log.trace "setVolume(${sessionID}, ${volume})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/set_volume",
        query: ["SessionID":sessionID, "Volume":volume]
    ]
    get(params)
}

def setVolumeDevice(sessionID, deviceID, volume) {
    log.trace "setVolumeDevice(${sessionID}, ${deviceID}, ${volume})"
    def params = [
        uri: appSettings.ipAddress,
        path: "/v1/set_volume_device",
        query: ["SessionID":sessionID, "DeviceID":deviceID, "Volume":volume]
    ]
    get(params)
}

def get(params) {
    log.trace "get(${params})"
    httpGet(params) { resp ->
        return resp.data
    }
}