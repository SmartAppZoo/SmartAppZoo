/**
 *  RESTful Music Players
 *
 *  Copyright 2017 Ben Deitch
 *
 */
definition(
  name: "RESTful Music Players",
  namespace: "bendeitch",
  author: "Ben Deitch",
  description: "Exposes a REST API that can be used to remotely control the main functions of music players.",
  category: "My Apps",
  iconUrl: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment2-icn.png",
  iconX2Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment2-icn@2x.png",
  iconX3Url: "http://cdn.device-icons.smartthings.com/Entertainment/entertainment2-icn@3x.png")

preferences {
  section {
    input "players", "capability.music player", title: "Select players to control", multiple: true
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
  // TODO: subscribe to attributes, devices, locations, etc.
}

mappings {
  // simple GET action to list players (typically used for debugging service)
  path("/players") {
    action: [
      GET: "listPlayers"
    ]
  }
  // path to capture player command without a parameter
  path("/player/:playerName/:command") {
    action: [
      POST: "playerCommand"
    ]
  }
  // path to capture player command with a parameter
  path("/player/:playerName/:command/:value") {
    action: [
      POST: "playerCommand"
    ]
  }
}

// return a list of players' display names
def listPlayers() {
  def resp = []
  players.each {
    resp << [name: it.displayName]
  }
  return resp
}

// Simple fuzzy matching algorithm (far from perfect but seems to work quite well with Google Home voice recog.):
// returns a score indicating the quality of the match between device name and player name passed to REST service
// performs case insensitive matching across multiple words split by a single space
// if the submitted name part is fully contained within the device name part then a higher score is gained
// if the device name part is contained within the submitted name part then a score is still gained but not as high
def fuzzyMatch(deviceName, playerName) {

  // upper-case and split into parts
  def deviceParts = deviceName.toUpperCase().split(" ")
  def playerParts = playerName.toUpperCase().split(" ")

  // iterate over the parts to sum the scores
  int matches = deviceParts.collect({
    devicePart -> playerParts.collect({
      playerPart ->
        if (devicePart.contains(playerPart)) {
          2
        } else if (playerPart.contains(devicePart)) {
          1
        } else {
          0
        }
      }).sum()
    }).sum()

  return matches
}

// uses the fuzzy matching defined above to find a player with a matching name
// NB: Only returns a player if that player gains a higher score than any other player,
// i.e. If two players both score the same highest value then it is not possible to choose between them so no player is returned
def findPlayer(playerName) {

  // build a list of players with their match scores
  def matches = players.collect({[ score: (fuzzyMatch(it.displayName, playerName)), player: it ]})

  // iterate over the matches to find a single player with the highest score
  def highScore = -1
  def player
  for (match in matches) {
    if (match.score > highScore) {
      player = match.player
      highScore = match.score
    } else if (match.score == highScore) {
      player = null
    }
  }
  
  log.debug("Player selected: ${player}, matches: ${matches}")
  return player
}

def playerCommand() {

  // extract variables from REST URI
  def playerName = params.playerName?.trim()
  def command = params.command?.trim()
  def value = params.value?.trim()
    
  log.debug "Command received: \"${command}\", playerName: \"${playerName}\", value: ${value}"
  
  // attempt to match a single player based on the provided player name
  def player = findPlayer(playerName)

  // if a player was found then invoke the relevant method
  if (player) {
    switch (command) {
      case "pause":
        player.pause()
        break
      case "play":
        player.play()
        break
      case "stop":
        player.stop()
        break
      case "mute":
        player.mute()
        break
      case "unmute":
        player.unmute()
        break
      case "next":
        player.nextTrack()
        break
      case "previous":
        player.previousTrack()
        break
      case "volume":
        player.setLevel(value)
        break
      case "quieter":
        player.setLevel("-" + value)
        break
      case "louder":
        player.setLevel("+" + value)
        break
      case "favorite":
        player.playFavorite(value)
        break
      default:
        log.debug "command not found: \"${command}\""
    }
  } else {
    log.debug "player not found: \"${playerName}\""
  }
}