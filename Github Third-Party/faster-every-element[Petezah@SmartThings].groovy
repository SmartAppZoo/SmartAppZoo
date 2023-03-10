/**
 *  Faster Every Element
 *
 *  Copyright 2015 Peter Dunshee
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
    name: "Faster Every Element",
    namespace: "Petezah",
    author: "Peter Dunshee",
    description: "A faster (single page) Every element demonstration app",
    category: "SmartThings Internal",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	page(name: "firstPage")
	page(name: "deadEnd", title: "Nothing to see here, move along.", content: "foo")
}

def firstPage() {
	dynamicPage(name: "firstPage", title: "All the elements", install: true, uninstall: true) {
        section("input") {
        	input(type: "enum", name: "enumSegmented", title: "enumSegmented", required: false, multiple: true, options: ["one", "two", "three"], style: "segmented")
			input(type: "enum", name: "enum", title: "enum", required: false, multiple: false, options: ["one", "two", "three"])
			input(type: "text", name: "text", title: "text", required: false, multiple: false)
			input(type: "number", name: "number", title: "number", required: false, multiple: false)
			input(type: "boolean", name: "boolean", title: "boolean", required: false, multiple: false)
			input(type: "password", name: "password", title: "password", required: false, multiple: false)
			input(type: "phone", name: "phone", title: "phone", required: false, multiple: false)
			input(type: "email", name: "email", title: "email", required: false, multiple: false)
			input(type: "decimal", name: "decimal", title: "decimal", required: false, multiple: false)
			input(type: "mode", name: "mode", title: "mode", required: false, multiple: false)
			input(type: "icon", name: "icon", title: "icon", required: false, multiple: false)
			input(type: "capability.switch", name: "capability", title: "capability", required: false, multiple: false)
			input(type: "hub", name: "hub", title: "hub", required: false, multiple: false)
			input(type: "device.switch", name: "device", title: "device", required: false, multiple: false)
			input(type: "time", name: "time", title: "time", required: false, multiple: false)
            input("recipients", "contact", title: "contact", description: "Send notifications to") {
                input(type: "phone", name: "phone", title: "Send text message to", required: false, multiple: false)
                input(type: "boolean", name: "boolean", title: "Send push notification", required: false, multiple: false)
            }
		}
        section("app") {
			app(
				name: "app",
				title: "required:false, multiple:false",
				required: false,
				multiple: false,
				namespace: "Steve",
				appName: "Child SmartApp"
			)
		}
        section("label") {
			label(name: "label", title: "required:false, multiple:false", required: false, multiple: false)
		}
        section("mode") {
			mode(name: "mode", title: "required:false, multiple:false", required: false, multiple: false)
		}
        section("paragraph") {
			paragraph "This us how you should make a paragraph element"
		}
        section("icon") {
			icon(name: "icon", title: "required:false, multiple:false", required: false, multiple: false)
		}
        section("href") {
			href(name: "hrefPage", title: "required:false, multiple:false", required: false, multiple: false, page: "deadEnd")
		}
        section("buttons") {
			buttons(name: "buttons", title: "buttons", required: false, multiple: false, buttons: [
				[label: "foo", action: "foo"],
				[label: "bar", action: "bar"]
			])
			buttons(name: "buttonsColoredString", title: "buttonsColoredString", buttons: [
				[label: "green", action: "foo", backgroundColor: "green"],
				[label: "red", action: "foo", backgroundColor: "red"],
				[label: "both fg and bg", action: "foo", color: "red", backgroundColor: "green"]
			])
		}
        section("image") {
			image "http://www.deargrumpycat.com/wp-content/uploads/2013/02/Grumpy-Cat1.jpg"
			image(name: "imageWithImage", title: "This element has an image and a long title.", description: "I am setting long title and descriptions to test the offset", required: false, image: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png")
		}
	}
}

def foo() {
	dynamicPage(name: "deadEnd") {

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