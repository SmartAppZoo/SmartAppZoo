/**
 *  Lock It When I Leave
 *
 *  Author: Zach Naimon @ SmartThings
 *  Date: 2013-02-11
 */
preferences {
	section("When I Leave...") {
		input "presence1", "capability.presenceSensor", title: "Who?", multiple: true
	}
	section("Lock the lock...") {
		input "lock1","capability.lock", multiple: true
        input "unlockit", "enum", title: "Unlock when presence is detected?", metadata: [values: ["Yes","No"]]
        input "spam", "enum", title: "Send Me Notifications?", metadata: [values: ["Yes","No"]]
	}
}

def installed(){
	subscribe(presence1, "presence", presence)   
}

def updated(){
	unsubscribe()
	subscribe(presence1, "presence", presence)
}

def presence(evt){
		def presenceValue = presence1.find{it.currentPresence == "present"}
		def lockValue = lock1.find{it.currentValue == "unlocked"}
    	if(unlockit == "Yes"){
			if(presenceValue && !lockValue){
				log.debug "UnLock It = $unlockit and there is somebody home, and the door is locked, so we're unlocking the door."
				lock1.unlock()
				sendNotif()	
			}
			else if(!presenceValue && lockValue){
				log.debug "UnLock It = $unlockit and there is nobody home, and the door is unlocked, so we're locking the door."
				lock1.unlock()
				sendNotif()
			}
		}
		else{
			if(!presenceValue && lockValue){
				log.debug "Lock It = $lockit and there is someone home, and the door is locked, so we're unlocking the door."
				lock1.lock()
				sendNotif()	
			}
			else if(presenceValue && !lockValue){
				log.debug "Lock It = $lockit and there is nobody home, and the door is unlocked, so we're locking the door."
				lock1.unlock()
				sendNotif()
			}
		}
}

def sendNotif(){
	def lockValue = lock1.find{it.currentValue == "unlocked"}
	if(lockValue && spam == "Yes"){
		sendPush("Door Unlocked")
	}
	if(!lockValue && spam == "Yes"){
		sendPush("Door Locked")
	}
}