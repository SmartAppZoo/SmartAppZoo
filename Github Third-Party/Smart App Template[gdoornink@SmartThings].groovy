/*
 *  [NAME]
 *
 *  Copyright 2017 Greg Doornink
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

/*
	GOAL BEHAVIOR
	- 
 */

definition(
    name: "[NAME]",
    namespace: "GTDoor",
    author: "Greg Doornink",
    description: "[DESCRIPTION]",
    category: "Family",
    //parent: "GTDoor:[parent app name]",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

/* * * * * * * * * *
  PAGES AND INPUTS
 * * * * * * * * * */

preferences {
	page(name:"page1")
}

def page1() {
	dynamicPage(name: "page1", title: "Title") {
		// TODO: put inputs here
	}
}

/* * * * * * * * * *
  SET-UP
 * * * * * * * * * */

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

/* * * * * * * * * *
  CONTEXT TOOLS
 * * * * * * * * * */

	// TODO: add/edit context tools as needed

def contextCheck() {
	return (contextSwitch() && contextPresence() && contextTime() && contextWeekday() && contextCalendar())
}

def contextSwitch() {
	return true
}

def contextPresence() {
	return true
}

def contextTime() {
	return true
}

def contextWeekday() {
	return true
}

def contextCalendar() {
	return true
}

/* * * * * * * * * *
  DEVICE HANDLERS
 * * * * * * * * * */
 
def templateHandler(evt) {
  if(contextCheck()) {
    
    // TODO: create handlers using this template
    
  }
}

/* * * * * * * * * *
  ACTIONS
 * * * * * * * * * */
 
 // TODO: create actions
