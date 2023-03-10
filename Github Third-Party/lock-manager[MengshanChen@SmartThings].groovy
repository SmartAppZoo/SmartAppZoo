definition(
    name: "Lock Manager0730",
    namespace: "smartthings",
    author: "IoT",
    description: "This app alows you to change or delete the user codes for your smart door lock",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png")
    
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
    
preferences {
	page name: 'mainPage', title: 'Installed', install: true, uninstall: true, submitOnChange: true
    
}

def mainPage() {
  dynamicPage(name: 'mainPage', install: true, uninstall: true, submitOnChange: true) {
    section('Create') {
      app(name: 'locks', appName: 'Lock', namespace: 'ethayer', title: 'New Lock', multiple: true, image: 'https://images.lockmanager.io/app/v1/images/new-lock.png')
      app(name: 'lockUsers', appName: 'Lock User', namespace: 'ethayer', title: 'New User', multiple: true, image: 'https://images.lockmanager.io/app/v1/images/user-plus.png')
      app(name: 'keypads', appName: 'Keypad', namespace: 'ethayer', title: 'New Keypad', multiple: true, image: 'https://images.lockmanager.io/app/v1/images/keypad-plus.png')
    }
    section('Locks') {
      def lockApps = getLockApps()
      lockApps = lockApps.sort{ it.lock.id }
      if (lockApps) {
        def i = 0
        lockApps.each { lockApp ->
          i++
          href(name: "toLockInfoPage${i}", page: 'lockInfoPage', params: [id: lockApp.lock.id], required: false, title: lockApp.label, image: 'https://images.lockmanager.io/app/v1/images/lock.png' )
        }
      }
    }
    section('Advanced', hideable: true, hidden: true) {
      input(name: 'overwriteMode', title: 'Overwrite?', type: 'bool', required: true, defaultValue: true, description: 'Overwrite mode automatically deletes codes not in the users list')
      input(name: 'enableDebug', title: 'Enable IDE debug messages?', type: 'bool', required: true, defaultValue: false, description: 'Show activity from Lock Manger in logs for debugging.')
      label(title: 'Label this SmartApp', required: false, defaultValue: 'Lock Manager')
    }
  }
}

def lockInfoPage(params) {
  dynamicPage(name:"lockInfoPage", title:"Lock Info") {
    def lockApp = getLockAppByIndex(params)
    if (lockApp) {
      section("${lockApp.label}") {
        def complete = lockApp.isCodeComplete()
        if (!complete) {
          paragraph 'App is learning codes.  They will appear here when received.\n Lock may require special DTH to work properly'
          lockApp.lock.poll()
        }
        def codeData = lockApp.codeData()
        if (codeData) {
          def setCode = ''
          def usage
          def para
          def image
          def sortedCodes = codeData.sort{it.value.slot}
          sortedCodes.each { data ->
            data = data.value
            if (data.codeState != 'unknown') {
              def userApp = lockApp.findSlotUserApp(data.slot)
              para = "Slot ${data.slot}"
              if (data.code) {
                para = para + "\nCode: ${data.code}"
              }
              if (userApp) {
                para = para + userApp.getLockUserInfo(lockApp.lock)
                image = userApp.lockInfoPageImage(lockApp.lock)
              } else {
                image = 'https://images.lockmanager.io/app/v1/images/times-circle-o.png'
              }
              if (data.codeState == 'refresh') {
                para = para +'\nPending refresh...'
              }
              paragraph para, image: image
            }
          }
        }
      }

      section('Lock Settings') {
        def pinLength = lockApp.pinLength()
        def lockCodeSlots = lockApp.lockCodeSlots()
        if (pinLength) {
          paragraph "Required Length: ${pinLength}"
        }
        paragraph "Slot Count: ${lockCodeSlots}"
      }
    } else {
      section() {
        paragraph 'Error: Can\'t find lock!'
      }
    }
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
  def children = getChildApps()
  log.debug "there are ${children.size()} lock users"
}

def getLockAppByIndex(params) {
  def id = ''
  // Assign params to id.  Sometimes parameters are double nested.
  if (params.id) {
    id = params.id
  } else if (params.params){
    id = params.params.id
  } else if (state.lastLock) {
    id = state.lastLock
  }
  state.lastLock = id

  def lockApp = false
  def lockApps = getLockApps()
  if (lockApps) {
    def i = 0
    lockApps.each { app ->
      if (app.lock.id == state.lastLock) {
        lockApp = app
      }
    }
  }

  return lockApp
}

def availableSlots(selectedSlot) {
  def options = []
  (1..30).each { slot->
    def children = getLockApps()
    def available = true
    children.each { child ->
      def userSlot = child.userSlot
      if (!selectedSlot) {
        selectedSlot = 0
      }
      if (!userSlot) {
        userSlot = 0
      }
      if (userSlot.toInteger() == slot && selectedSlot.toInteger() != slot) {
        available = false
      }
    }
    if (available) {
      options << ["${slot}": "Slot ${slot}"]
    }
  }
  return options
}

def keypadMatchingUser(usedCode){
  def correctUser = false
  def userApps = getUserApps()
  userApps.each { userApp ->
    def code
    log.debug userApp.userCode
    if (userApp.isActiveKeypad()) {
      code = userApp.userCode.take(4)
      log.debug "code: ${code} used: ${usedCode}"
      if (code.toInteger() == usedCode.toInteger()) {
        correctUser = userApp
      }
    }
  }
  return correctUser
}

def findAssignedChildApp(lock, slot) {
  def childApp
  def userApps = getUserApps()
  userApps.each { child ->
    if (child.userSlot?.toInteger() == slot) {
      childApp = child
    }
  }
  return childApp
}

def getUserApps() {
  def userApps = []
  def children = getChildApps()
  children.each { child ->
    if (child.userSlot) {
      userApps.push(child)
    }
  }
  return userApps
}

def getKeypadApps() {
  def keypadApps = []
  def children = getChildApps()
  children.each { child ->
    if (child.keypad) {
      keypadApps.push(child)
    }
  }
  return keypadApps
}

def getLockApps() {
  def lockApps = []
  def children = getChildApps()
  children.each { child ->
    if (child.lock) {
      lockApps.push(child)
    }
  }
  return lockApps
}

def setAccess() {
  def lockApps = getLockApps()
  lockApps.each { lockApp ->
    lockApp.makeRequest()
  }
}

def debuggerOn() {
  // needed for child apps
  return enableDebug
}

def debugger(message) {
  def doDebugger = debuggerOn()
  if (enableDebug) {
    return log.debug(message)
  }
}


