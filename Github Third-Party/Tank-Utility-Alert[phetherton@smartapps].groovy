/**
 *  Tank-Utility-Alert
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
 
// package org.springframework.security.access;
definition(
    name: "Tank-Utility-Alert",
    namespace: "phetherton",
    author: "Patrick Hetherton",
    description: "Get an alert when Gas tank is low",
    category: "My Apps",
//    iconUrl:   "https://s3.amazonaws.com/smartapp-icons/Meta/text.png",
 //   iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",
//    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",
   
    //pausable: true
)

//"https://github.com/phetherton/smartapps/blob/master/Tank-Utility-Alert/Cabin.jpg",
//"https://github.com/phetherton/smartapps/blob/master/Tank-Utility-Alert/SC_LOGO.png", 
// "https://github.com/phetherton/smartapps/blob/master/Tank-Utility-Alert/SC_LOGO.png",

//"https://s3.amazonaws.com/smartapp-icons/Meta/text.png",
//"https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",
//"https://s3.amazonaws.com/smartapp-icons/Meta/text@2x.png",

preferences {
	section {
     	input(name: "GasMeter", type: "capability.Energy Meter", title: "When This Gas Tank...", required: true,displayDuringSetup: true)
        input(name: "SystemHealthAlert", type: "number", title: "Hasn't reported in...", required: true, description: "Days.")
        input(name: "levelAlert", type: "number", title: "Gas Level...", required: true, description: "is below 30%.") }
        
   section("Send Notifications?") {
        input("recipients", "contact", title: "Send notifications to") {
            input "phone", "phone", title: "Warn with text message (optional)",
                description: "Phone Number", required: false
        }
    }
    }


def installed() {
	log.debug "Installed with settings: ${settings}"
     runEvery3Hours(initialize)
//	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	runEvery3Hours(initialize)
  //  initialize()
 
}
def initialize() {
    def GasValue = GasMeter.currentValue("level") //Lets get the Gas Level
    def HealthValue = GasMeter.currentValue("SystemHealth") //Lets get system Health 0 means we are good 1 means we haven't reported in over 24 hours.
    log.debug "The current value of the Gas Level is ${GasValue}"
    log.debug "The curent value of the threshold is ${levelAlert}"
    log.debug "The System Health Value is ${HealthValue}"
    log.debug "recipients configured: ${recipients}"
    def LevelintValue = levelAlert.toInteger()
    def GasintValue = GasValue.toInteger()
    def HealthintValue = HealthValue.toInteger()
    
    

  if (GasintValue < LevelintValue  && HealthintValue ==1)  {
         log.debug "Both Values Screwed"
         def msg = "${GasMeter} reported ${GasValue}  which is below your warning level of ${levelAlert}% and no update in over 24 hours."
         sendMessage(msg) }
         
if (GasintValue < LevelintValue  && HealthintValue ==0)  {
         log.debug "Gas Values Screwed"
         def msg = "${GasMeter} reported ${GasValue}  which is Below your warning level of ${levelAlert}%."
         sendMessage(msg) }  
         
if (GasintValue > LevelintValue  && HealthintValue ==1)  {
         log.debug "System Health Screwed"
         def msg = "${GasMeter} reported ${GasValue} No Update in over 24 Hours"
         sendMessage(msg) }   

if (GasintValue > LevelintValue  && HealthintValue ==0)  {
         log.debug "Everything is Awesome"
         }
    
}

def sendMessage(msg) {
     if (location.contactBookEnabled && recipients) {
        sendNotificationToContacts(msg, recipients)
     }
     else {
        if (sms) {
            log.debug "Enter SMS ${sms} ${msg}"
            sendSms(sms, msg)
          }
    if (pushNotification) {
        log.debug "Enter sendpush"
        sendPush(msg)
                          }     
          }
                  }
