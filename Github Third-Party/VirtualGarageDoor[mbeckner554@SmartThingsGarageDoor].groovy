/**
 *  Name: Virtual Garage Door
 *  Source Available: https://github.com/mbeckner554/SmartThingsGarageDoor/blob/master/VirtualGarageDoor.groovy
 *  Description: Works with two devices (sensor and opener switch).  Added options to use the .off() command to allow two garage 
 * 		doors to be controlled by a single ESP8266 Switch - https://github.com/mbeckner554/SmartThingsGarageDoor/blob/master/ESP8266Switch.groovy
 *  Copyright 2017 Mbeckner
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
 *  Derived From LGK Virtual Garage Door:
 *   - Author: LGKahn kahn-st@lgk.com
 *   
 */
 
definition(
    name: "Virtual Garage Door",
    namespace: "MB",
    author: "Mbeckner",
    description: "Sync the Simulated garage door device with 2 actual devices, either a tilt or contact sensor and a switch or relay. The simulated device will then control the actual garage door. In addition, the virtual device will sync when the garage door is opened manually, \n It also attempts to double check the door was actually closed in case the beam was crossed. ",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png"
)

preferences {
	section("Choose the switch/relay that opens closes the garage?"){
		input "opener", "capability.switch", title: "Physical Garage Opener?", required: true
	}
    section("Use a off() trigger for garage switch?"){
		input "customTrigger", "enum", title: "Use off() trigger?", options: ["Yes", "No"], required: true
	}
	section("Choose the sensor that senses if the garage is open closed? "){
		input "sensor", "capability.contactSensor", title: "Physical Garage Door Open/Closed?", required: true
	}
    
	section("Choose the Virtual Garage Door Device? "){
		input "virtualgd", "capability.doorControl", title: "Virtual Garage Door?", required: true
	}
    
	section("Choose the Virtual Garage Door Device sensor (same as above device)?"){
		input "virtualgdbutton", "capability.contactSensor", title: "Virtual Garage Door Open/Close Sensor?", required: true
	}
    
 	section("Choose the virtual switch that Alexa uses to control the garage?"){
		input "alexavirtualswitch", "capability.switch", title: "Alexa Garage Opener?", required: true
	}

	section("Timeout before checking if the door opened or closed correctly?"){
		input "checkTimeout", "number", title: "Door Operation Check Timeout?", required: true, defaultValue: 25
	}

     section( "Notifications" ) {
        input("recipients", "contact", title: "Send notifications to") {
            input "sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: false
            input "phone1", "phone", title: "Send a Text Message?", required: false
        }
    }

}

def subscribeDevices()
{
	subscribe(sensor, "contact", contactHandler)
    subscribe(virtualgdbutton, "contact", virtualgdcontactHandler)
	subscribe(alexavirtualswitch, "switch", alexacontactHandler)
  }

def installed()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
//log.debug "in installed ... current state=  $realgdstate"
//log.debug "gd state= $virtualgd.currentContact"

	subscribeDevices()
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate)
     {
        if (realgdstate == "open")
           {
             virtualgd.open()
            }
         else virtualgd.close()
      }
 }

def updated()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
//log.debug "in updated ... current state=  $realgdstate"
//log.debug "in updated ... gd state= $virtualgd.currentContact"


	unsubscribe()
    
    // sync them up if need be set virtual same as actual
    if (realgdstate != virtualgdstate)
     {
        if (realgdstate == "open")
           {
             log.debug "opening virtual door"
             mysend(virtualgd.displayName + " opened!")     
             virtualgd.open()
            }
         else {
              virtualgd.close()
              log.debug "closing virtual door"
              mysend(virtualgd.displayName + " closed!")   
     		 }
      }
      
   	runIn(5,subscribeDevices)
    
  // for debugging and testing uncomment  temperatureHandlerTest()
}

def contactHandler(evt) 
{
def virtualgdstate = virtualgd.currentContact
// how to determine which contact
//log.debug "in contact handler for actual door open/close event. event = $evt"
  unsubscribe()
  if("open" == evt.value)
    {
    // contact was opened, turn on a light maybe?
    log.debug "Contact is in ${evt.value} state"
    // reset virtual door if necessary
    if (virtualgdstate != "open")
      {
        mysend(virtualgd.displayName + " changed to open!")   
        virtualgd.open()
      }
     }  
  if("closed" == evt.value)
   {
   // contact was closed, turn off the light?
    log.debug "Contact is in ${evt.value} state"
    //reset virtual door
     if (virtualgdstate != "closed")
      {
        mysend(virtualgd.displayName + " changed to closed!")   
  	    virtualgd.close()
      }
   }
   runIn(5,subscribeDevices)
}

def virtualgdcontactHandler(evt) {
// how to determine which contact
def realgdstate = sensor.currentContact
//log.debug "in virtual gd contact/button handler event = $evt"
//log.debug "in virtualgd contact handler check timeout = $checkTimeout"

  if("open" == evt.value)
    {
    // contact was opened, turn on a light maybe?
    log.debug "Contact is in ${evt.value} state"
    // check to see if door is not in open state if so open
    if (realgdstate != "open")
      {
        log.debug "opening real gd to correspond with button press"
         mysend(virtualgd.displayName + " opening with app!")   
         if (customTrigger=="No")
         	{
         		opener.on()
            }
            else {
            	opener.off()
                }
         runIn(checkTimeout, checkActualState)
        
      }
     }
  if("closed" == evt.value)
   {
    // contact was closed, turn off the light?
    log.debug "Contact is in ${evt.value} state"
    if (realgdstate != "closed")
      {
        log.debug "closing real gd to correspond with button press"
        mysend(virtualgd.displayName + " closing with app!")   
         if (customTrigger=="No")
         	{
         		opener.on()
            }
            else {
            	opener.off()
                }
        runIn(checkTimeout, checkActualState)
      }
   }
}



def alexacontactHandler(evt) {
// how to determine which contact
def realgdstate = sensor.currentContact
//log.debug "in virtual gd contact/button handler event = $evt"
//log.debug "in virtualgd contact handler check timeout = $checkTimeout"
  
//mysend(evt.device?.displayName + " given command " + evt.value)
  
  if("on" == evt.value)
    {
    // alexa button was pushed - opening garage?
    log.debug "Contact is in ${evt.value} state"
    // check to see if door is not in open state if so open
    if (realgdstate != "open")
      {
        log.debug "opening real gd to correspond with alexa switch press"
         mysend(virtualgd.displayName + " opening with Alexa!")   
         if (customTrigger=="No")
         	{
         		opener.on()
            }
            else {
            	opener.off()
                }

		unsubscribe()
        virtualgd.open()
	    runIn(checkTimeout, subscribeDevices)
		runIn(checkTimeout, checkActualState)      }
     }
  if("off" == evt.value)
   {
    // Alexa button was pushed, closing garage?
    log.debug "Contact is in ${evt.value} state"
    if (realgdstate != "closed")
      {
        log.debug "closing real gd to correspond with alexa button press"
        mysend(virtualgd.displayName + " closing with Alexa!")   
         if (customTrigger=="No")
         	{
         		opener.on()
            }
            else {
            	opener.off()
                }
       
        unsubscribe()
        virtualgd.close()
	    runIn(5, subscribeDevices)
		runIn(checkTimeout, checkActualState)
      }
   }
}

private mysend(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}


def checkActualState()
{
def realgdstate = sensor.currentContact
def virtualgdstate = virtualgd.currentContact
   
    // sync them up if need be set virtual same as actual
    if (realgdstate!=virtualgdstate)
     {
         log.debug "Refreshing Virtual Door - State Mismatch!!"
         mysend("WARNING!! "+ virtualgd.displayName + " operations did not complete!  Fixing Virtual Status!")   
         if (realgdstate != virtualgdstate)
         {
	      	unsubscribe()
             if (realgdstate == "open")
             {
                 virtualgd.open()
             }
             else virtualgd.close()
            runIn(5,subscribeDevices)
            runIn(checkTimeout, checkActualState)
         }    
     }   
}


