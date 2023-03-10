/**
 * AutoArlo
 *
 * Created by Joe Beeson <jbeeson@gmail.com>
 *
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.

 * For more information, please refer to <http://unlicense.org/>
 */
definition(
    name:        "AutoArlo",
    namespace:   "joebeeson",
    author:      "Joe Beeson",
    description: "Change Netgear Arlo mode.",
    category:    "Convenience",
    iconUrl:     "https://bitbucket-assetroot.s3.amazonaws.com/c/photos/2015/Jun/01/3175286666-5-hummingbird-logo_avatar.png",
    iconX2Url:   "https://bitbucket-assetroot.s3.amazonaws.com/c/photos/2015/Jun/01/3175286666-5-hummingbird-logo_avatar.png",
    iconX3Url:   "https://bitbucket-assetroot.s3.amazonaws.com/c/photos/2015/Jun/01/3175286666-5-hummingbird-logo_avatar.png"
)


preferences {
	section("Arlo credentials") {
    	input(name: "strCredentialsEmail", type: "text", title: "Email")
		input(name: "strCredentialsPassword", type: "password", title: "Password")
	}
    section("Change Arlo to this mode") {
    	input(name: "strArloMode", type: "text", title: "Mode")
    }
    section("When SmartThings mode becomes") {
    	input(name: "strSmartThingsMode", type: "mode", title: "Mode")
    }
}

Map getArloBaseStationDevice() {
	return getArloDevices("basestation")[0]
}

List getArloDevices(strDeviceType = "") {
	def lstDevices = []
	try {
    	httpGet(
        	[
            	uri:     "https://arlo.netgear.com",
                path:    "/hmsweb/users/devices",
                headers: getArloRequestHeaders()
            ]
        ) { objResponse ->
        	if (objResponse.data.success == true) {
            	if (strDeviceType == "") {
                	lstDevices = objResponse.data.data
                } else {
                	for (objDevice in objResponse.data.data) {
                    	if (objDevice.deviceType == strDeviceType) {
                        	lstDevices << objDevice
                        }
                	}
				}
            } else {
            	log.error "Failed to retrieve list of devices. Response: ${objResponse.data}"
                return false
            }
        }
    } catch (objException) {
    	log.error "Caught exception while querying devices: ${objException}"
    }
    return lstDevices
}

Map getArloRequestHeaders() {
    def strAuthToken
    def lstAuthCookies = []

	try {
        httpPostJson(
            [
                uri:  "https://arlo.netgear.com:443",
                path: "/hmsweb/login",
                body: [
                    email:    settings.strCredentialsEmail,
                    password: settings.strCredentialsPassword
                ],
                headers: [
                    "Accept": "text/html",
                	"User-Agent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36"
                ]
            ]
        ) { objResponse ->
        	if (objResponse.data.success == true) {
            	strAuthToken    = objResponse.data.data.token
            	objResponse.headers.each {
	                if (it.name == "Set-Cookie") {
	                    lstAuthCookies << it.value.split(";").getAt(0)
	                }
	            }
                log.debug "Retrieved authentication token, \"${strAuthToken}\""
			} else {
            	log.error "Failed to perform login. Response: ${objResponse.data}"
                return false
            }
        }
	} catch (objException) {
    	log.error "Caught exception during login: ${objException}"
    }
    return [
		Authorization: strAuthToken,
		Cookie       : lstAuthCookies.join(";"),
        "User-Agent" : "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36"   
	]
}

Boolean setArloBaseStationMode(String strModeId) {
    def mapBaseStationDevice = getArloBaseStationDevice()
    try {
    	httpPostJson(
        	[
            	uri:     "https://arlo.netgear.com",
				path:    "/hmsweb/users/devices/notify/${mapBaseStationDevice.deviceId}",
				headers: [
					xcloudId: mapBaseStationDevice.xCloudId
				] + getArloRequestHeaders(),
				body: [
					action:     "set",
					from:       "336-3439005_web",
					properties: [
						active: strModeId
					],
					active:          strModeId,
					publishResponse: false,
					resource:        "modes",
					responseUrl:     "",
					to:              mapBaseStationDevice.deviceId,
					transId:         "web!5f4ec415.8520c!1470612990228"
				]
            ]
        ) { objResponse ->
        	if (objResponse.data.success == true) {
            	log.debug "Successfully set mode, \"${strModeId}\""
                return true
            } else {
            	log.error "Failed to set mode. Response: ${objResponse.data}"
                return false
            }
        }
    } catch (objException) {
    	log.error "Caught exception while setting mode: ${objException}"
        raise objException
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def initialize() {
    subscribe(location, "mode", onModeChange)
    subscribe(app, doModeChange)
    doModeChange([])
}

def doModeChange(mapEvent) {
	log.debug "Forcibly changing mode to \"${settings.strArloMode}\""
    if (setArloBaseStationMode(settings.strArloMode)) {
		log.debug "Successfully changed!"
	} else {
    	log.error "Failed to change modes!"
   	}
}

def onModeChange(mapEvent) {
	if (mapEvent.value == settings.strSmartThingsMode) {
    	log.debug "Mode changed to \"${mapEvent.value}\". Changing Arlo mode to \"${settings.strArloMode}\""
        if (setArloBaseStationMode(settings.strArloMode)) {
        	log.debug "Successfully changed!"
        } else {
        	log.error "Failed to change modes!"
        }
    }
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}
