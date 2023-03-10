/**
 *  Unifi Protect
 *
 *  Copyright 2020 Alexander Boczar
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
    name: "Unifi Protect",
    namespace: "boczar",
    author: "Alexander Boczar",
    description: "Unifi Protect Controller",
    category: "My Apps",
    iconUrl: "https://prd-www-cdn.ubnt.com/media/images/productgroup/unifi-cloud-key-gen2/usg-g2-small.png",
    iconX2Url: "https://prd-www-cdn.ubnt.com/media/images/productgroup/unifi-cloud-key-gen2/usg-g2-small.png",
    iconX3Url: "https://prd-www-cdn.ubnt.com/media/images/productgroup/unifi-cloud-key-gen2/usg-g2-small.png",
    oauth: true,
    usesThirdPartyAuthentication: true) {
    appSetting "clientId"
}


preferences {
	section(name: "Credentials", title: "Ubiquiti", context: "authPage", install: true)
}

mappings {
    path("/receivedToken") { action: [ POST: "oauthReceivedToken", GET: "oauthReceivedToken"] }
    path("/receiveToken") { action: [ POST: "oauthReceiveToken", GET: "oauthReceiveToken"] }
    path("/webhookCallback") { action: [ POST: "webhookCallback"] }
    path("/oauth/callback") { action: [ GET: "oauthCallback" ] }
    path("/oauth/initialize") { action: [ GET: "oauthInit"] }
    path("/test") { action: [ GET: "oauthSuccess" ] }
}

def getServerUrl()               { return  appSettings.serverUrl ?: apiServerUrl }
def getCallbackUrl()             { return "${getServerUrl()}/oauth/callback" }
def apiURL(path = '/') 			 { return "https://amplifi-client.svc.ui.com/api${path}" }
def getSecretKey()               { return appSettings.secretKey }
def getClientId()                { return appSettings.clientId }
def getVendorName() { "LIFX" }


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

def authPage() {
    if (state.ubntToken) {
        def validateToken = locationOptions() ?: []
    }

    if (!state.ubntToken) {
        log.debug "no UBNT access token"
        // This is the SmartThings access token
        if (!state.accessToken) {
            log.debug "no access token, create access token"
            state.accessToken = createAccessToken() // predefined method
        }
        def description = "Tap to enter UBNT credentials"
        def redirectUrl = "${serverUrl}/oauth/initialize?appId=${app.id}&access_token=${state.accessToken}&apiServerUrl=${apiServerUrl}" // this triggers oauthInit() below
        return dynamicPage(name: "Credentials", title: "Connect to UBNT", nextPage: null, uninstall: true, install:true) {
            section {
                href(url:redirectUrl, required:true, title:"Connect to UBNT", description:"Tap here to connect your UBNT account")
            }
        }
    } else {
        log.debug "have UBNT access token"

        def options = locationOptions() ?: []
        def count = options.size().toString()

        return dynamicPage(name:"Credentials", title:"", nextPage:"", install:true, uninstall: true) {
            section("Select your location") {
                input "selectedLocationId", "enum", required:true, title:"Select location ({{count}} found)", messageArgs: [count: count], multiple:false, options:options, submitOnChange: true
                paragraph "Devices will be added automatically from your UBNT account. To add or delete devices please use the Official UBNT App."
            }
        }
    }
}

def oauthInit() {
    def oauthParams = [client_id: "${appSettings.clientId}", scope: "read", response_type: "token" ]
    log.debug("Redirecting user to OAuth setup")
    redirect(location: "https://account.ui.com/login?${toQueryString(oauthParams)}")
}

def oauthCallback() {
    def redirectUrl = null
    if (params.authQueryString) {
        redirectUrl = URLDecoder.decode(params.authQueryString.replaceAll(".+&redirect_url=", ""))
    } else {
        log.warn "No authQueryString"
    }

    if (state.ubntToken) {
        log.debug "Access token already exists"
        success()
    } else {
        def token = params.token
        if (token) {
            if (token.size() > 6) {
                // LIFX code
                log.debug "Exchanging code for access token"
                oauthReceiveToken(redirectUrl)
            } else {
                // Initiate the UBNT OAuth flow.
                oauthInit()
            }
        } else {
            log.debug "This code should be unreachable"
            success()
        }
    }
}

def oauthReceiveToken(redirectUrl = null) {
    // Not sure what redirectUrl is for
    log.debug "receiveToken - params: ${params}"
    def oauthParams = [ client_id: "${appSettings.clientId}", client_secret: "${appSettings.clientSecret}", grant_type: "authorization_code", code: params.code, scope: params.scope ] // how is params.code valid here?
    def params = [
            uri: "https://cloud.lifx.com/oauth/token",
            body: oauthParams,
            headers: [
                    "User-Agent": "SmartThings Integration"
            ]
    ]
    httpPost(params) { response ->
        state.ubntToken = response.data.access_token
    }

    if (state.ubntToken) {
        oauthSuccess()
    } else {
        oauthFailure()
    }
}

def oauthSuccess() {
    def message = """
        <p>Your UBNT Account is now connected to SmartThings!</p>
        <p>Click 'Done' to finish setup.</p>
    """
    oauthConnectionStatus(message)
}

def oauthFailure() {
    def message = """
        <p>The connection could not be established!</p>
        <p>Click 'Done' to return to the menu.</p>
    """
    oauthConnectionStatus(message)
}

def oauthReceivedToken() {
    def message = """
        <p>Your UBNT Account is already connected to SmartThings!</p>
        <p>Click 'Done' to finish setup.</p>
    """
    oauthConnectionStatus(message)
}

def oauthConnectionStatus(message, redirectUrl = null) {
    def redirectHtml = ""
    if (redirectUrl) {
        redirectHtml = """
            <meta http-equiv="refresh" content="3; url=${redirectUrl}" />
        """
    }

    def html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width">
        <title>SmartThings Connection</title>
        <style type="text/css">
            @font-face {
                font-family: 'Swiss 721 W01 Thin';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-thin-webfont.svg#swis721_th_btthin') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            @font-face {
                font-family: 'Swiss 721 W01 Light';
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot');
                src: url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.eot?#iefix') format('embedded-opentype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.woff') format('woff'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.ttf') format('truetype'),
                     url('https://s3.amazonaws.com/smartapp-icons/Partner/fonts/swiss-721-light-webfont.svg#swis721_lt_btlight') format('svg');
                font-weight: normal;
                font-style: normal;
            }
            .container {
                width: 280;
                padding: 20px;
                text-align: center;
            }
            img {
                vertical-align: middle;
            }
            img:nth-child(2) {
                margin: 0 15px;
            }
            p {
                font-size: 1.2em;
                font-family: 'Swiss 721 W01 Thin';
                text-align: center;
                color: #666666;
                padding: 0 20px;
                margin-bottom: 0;
            }
            span {
                font-family: 'Swiss 721 W01 Light';
            }
        </style>
        ${redirectHtml}
        </head>
        <body>
            <div class="container">
                <img src='https://cloud.lifx.com/images/lifx.png' alt='LIFX icon' width='100'/>
                <img src='https://s3.amazonaws.com/smartapp-icons/Partner/support/connected-device-icn%402x.png' alt='connected device icon' width="40"/>
                <img src='https://s3.amazonaws.com/smartapp-icons/Partner/support/st-logo%402x.png' alt='SmartThings logo' width="100"/>
                <p>
                    ${message}
                </p>
            </div>
        </body>
        </html>
    """
    render contentType: 'text/html', data: html
}