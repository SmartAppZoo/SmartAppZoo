
definition(
		name: "HaasLockApp",
		namespace: "drandyhaas",
		author: "AndrewHaas",
		description: "Add and Delete Single User Codes for Locks",
		category: "Safety & Security",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave@2x.png"
	  )


preferences {
	page(name: "setupApp")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Lock User Management", install: true, uninstall: true) {    
        section("Select Lock(s)") {
            input "locks","capability.lock", title: "Lock", multiple: true
        }
        section("User Management") {
            input "action", "enum", title: "Add/Update/Delete User?", required: true, metadata: [values: ["Add/Update","Delete"]],  refreshAfterSelection: true
            input "user", "number", title: "User Slot Number", description: "This is the user slot number on the lock and not the user passcode"
        }

		//if (action == "Add/Update") {
            section("Add/Update User Code") {
                input "code", "text", title: "User Passcode (check your lock passcode length)", defaultValue: "X", description: "The user passcode for adding/updating a new user (enter X for deleting user)"
            }
        //}

        section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
		}
    }
}

def installed()
{
	runIn(1, appTouch)
}

def updated()
{
    log.debug "updated"
	appTouch()
}

def appTouch() {
    log.debug("app touch")
  	for (lock in locks) {
        if (action == "Delete") {
            lock.deleteCode(user)
            log.info "$lock deleted user: $user"
            sendNotificationEvent("$lock deleted user: $user")
            //sendPush "$lock deleted user: $user"
        } else {
            lock.setCode(user, code)
            log.info "$lock added user: $user, code: $code"
            sendNotificationEvent("$lock added user: $user")
            //sendPush "$lock added user: $user"
        }
    }
}
