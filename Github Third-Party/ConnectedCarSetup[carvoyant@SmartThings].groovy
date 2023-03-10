/**
 *  Connected Car Setup
 *
 *  Copyright 2014 Carvoyant
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 * 
 *  Guidance on writing this service manager was taken from https://github.com/yracine/device-type.myecobee
 */
 
import groovyx.net.http.HttpResponseException
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException

definition(
    name: "Connected Car Setup",
    namespace: "carvoyant",
    author: "Carvoyant",
    description: "This is the Carvoyant service manager. It is used to manage your Carvoyant OAuth2 authorization and to configure your connected car devices.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon.png",
    iconX2Url: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon2x.png",
    iconX3Url: "https://s3.amazonaws.com/shared-carvoyant-images/smartthings_icon3x.png",
    oauth: true) {

	appSetting "carvoyantClientId"
	appSetting "carvoyantSecret"
	appSetting "carvoyantApiUrl"
	appSetting "carvoyantAuthUrl"
	appSetting "carvoyantTokenUrl"
}


preferences {
  /* 
    //Uncomment this section to provide the smartthings simulator an access token directly trough the IDE and skip the usual token acquisition process.
  
    page( name:"page1", title:"Preferences Page 1", nextPage:"auth", required: true, install: false, uninstall: false ) {
      section("Access Token") {
        input "simulatorAccessToken", "string", title: "Access Token", required: false
      }
    }
  */
  
  //dynamic preferences page
  page(name: "auth", title: "Carvoyant Setup", content: "authPage", install:true, uninstall: true)


}

mappings {
  path("/geoFenceNotification") {
    action: [
      POST: "geoFenceNotification"
    ]
  }
  path("/ignitionStatusNotification") {
    action: [
      POST: "ignitionStatusNotification"
    ]
  }
  path("/receiveToken") {
    action: [
      GET: "receiveToken"
    ]
  }
}

def installed() {
  log.debug "Installed with settings: ${settings}"
  
  //refreshes the access token daily
  schedule(new Date(), "refreshToken")
}

def updated() {
  log.debug "Updated with settings: ${settings}"
  unsubscribe()
  initialize()
}

def uninstalled() {
  removeChildDevices(getChildDevices())
  deleteAllSubscriptions()
}

def initialize() {
  log.debug "initialize"

  def vehiclesToAdd = []
  def vehiclesToRemove = []

  //Determine which vehicles need to be processed for removal or creation
  if(selectedVehicles instanceof JSONArray)
  {
    vehiclesToAdd = selectedVehicles	
    for(sv in selectedVehicles)
    {
      def foundVehicle = getChildDevices().find {it.deviceNetworkId == sv}
        if(foundVehicle)
        {
          log.debug "found child " + foundVehicle
          vehiclesToAdd -= sv
          log.debug vehiclesToAdd
        }
    }

    for(child in getChildDevices())
    {
      def foundChild = selectedVehicles.find { it == child.deviceNetworkId }
      if(!foundChild)
      {
        vehiclesToRemove.add(child.deviceNetworkId)
      }
    }
  }
  else if(selectedVehicles instanceof String)	//Smarthings gives us a String if the user selects only one vehicle in the "selectedVehicles" preferece
  {
    def foundVehicle = getChildDevices().find {it.deviceNetworkId == selectedVehicles}
    if(!foundVehicle)
    {
      vehiclesToAdd.add(selectedVehicles)
    }

    for(child in getChildDevices())
    {
      if(!selectedVehicles.equals(child.deviceNetworkId.toString()))
      {
        vehiclesToRemove.add(child.deviceNetworkId)
      }
    }
  }

  for(v in vehiclesToAdd)
  {
    setupVehicle(v)
  }

  for(v in vehiclesToRemove)
  {
    removeVehicle(v)
  }
  log.debug "carvoyant subscriptions " + state.carvoyantSubscriptions

}

def setupVehicle(vehicleId)
{
  log.debug "setup " + vehicleId

  //Subscribe to Carvoyant events
  try	
  {	  
    def geoFenceSub = createGeoFenceSubscription(vehicleId)
    def ignitionStatusSub = createIgnitionStatusSubscription(vehicleId)
  }

  catch(Exception e){
    log.debug "Failed to create vehicle"
    if(geoFenceSub)
    {
      deleteSubscription(geoFenceSub)
    }

    if(ignitionStatusSub)
    {
      deleteSubscription(ignitionStatusSub)
    }
  }

  //add vehicle as a "Connected Car" smartthings device 
  if(!getChildDevices().find { vehicleId.toString().equals(it.deviceNetworkId) })
  {
    def child = addChildDevice("carvoyant", "Connected Car", vehicleId, null, [name:state.vehicles[vehicleId]])
    child.init()
  }
}

def removeVehicle(vehicleId)
{
  log.debug "remove child " + vehicleId
  deleteChildDevice(vehicleId)
  deleteVehicleSubscriptions(vehicleId)
}

private removeChildDevices(delete) {
  delete.each {
    deleteChildDevice(it.deviceNetworkId)
  }
}

//Carvoyant notifies this method when Ignition Status is changed, and the device's ignitionStatus is updated.
def ignitionStatusNotification()
{
  log.debug "ignitionStatus: " + request.JSON.ignitionStatus
  def device = getChildDevices().find { request.JSON.vehicleId.toString().equals(it.deviceNetworkId) }
  device.setIgnitionStatus(request.JSON.ignitionStatus)
}

//Carvoyant notifies this method when vehicle presence at the installed location is changed, and the device's "presence" is updated.
def geoFenceNotification()
{

  log.debug "in or out " + request.JSON.recordedBoundaryCondition

  def device = getChildDevices().find { request.JSON.vehicleId.toString().equals(it.deviceNetworkId) }

  if(request.JSON.recordedBoundaryCondition.equals("INSIDE"))
  {
    device.setPresence("present")
  }

  if(request.JSON.recordedBoundaryCondition.equals("OUTSIDE"))
  {
    device.setPresence("not present")
  }

}

//Deletes all Carvoyant subscriptions
def deleteAllSubscriptions()
{
  try
  {
    def subs = state.carvoyantSubscriptions
    for(s in subs)
    {
      deleteSubscription(s)
    }
    state.carvoyantSubscriptions = []
  }

  catch(HttpResponseException hre)
  {
    log.debug "error deleting: " + hre.getResponse().getData()
  }
}

//Deletes all Carvoyant subscriptions for the specified Vehicle
def deleteVehicleSubscriptions(vehicleId)
{
  def subsToDelete = state.carvoyantSubscriptions.findAll { it.vehicleId == vehicleId }
  for(s in subsToDelete)
  {
    deleteSubscription(s)
  }
}

//Deletes a Carvoyant subscription
def deleteSubscription(subscriptionToDelete)
{
  log.debug "deleting ${subscriptionToDelete}"
  def subParams = 
  [
    uri: appSettings.carvoyantApiUrl + "/vehicle/${subscriptionToDelete.vehicleId}/eventSubscription/${subscriptionToDelete.subscriptionId}",
    headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.carvoyantAccessToken}"]
  ]

  httpDelete(subParams) 
  {
    resp ->

    log.debug "delete respoonse " + state.carvoyantSubscriptions.find { it.subscriptionId == subscriptionToDelete.subscriptionId }

    state.carvoyantSubscriptions =  state.carvoyantSubscriptions - state.carvoyantSubscriptions.find { it.subscriptionId == subscriptionToDelete.subscriptionId }
  }
}

//Creates a new Carvoyant subscription tailored to notify smartthings when a vehicle's presence should be updated.
def createGeoFenceSubscription(selectedVehicle)
{
  def body = [
    minimumTime: 0, 
    postUrl: serverUrl + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/geoFenceNotification", 
    notificationPeriod: "STATECHANGE", 
    origin:[latitude:location.latitude, longitude:location.longitude], 
    radius:1, 
    ignitionStatus: "ANY", 
    boundaryCondition:"INSIDE"
  ]
  try
  {

    httpPostJson(
    uri: appSettings.carvoyantApiUrl + "/vehicle/${selectedVehicle}/eventSubscription/geoFence/", 
    path: '',  
    headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.carvoyantAccessToken}"], 
    body: body
    ) 
    {
      response ->
  
      if(!state.carvoyantSubscriptions)
      {
        state.carvoyantSubscriptions = []
      }
      def createdSub = [subscriptionId: response.getData().subscription.id, vehicleId: selectedVehicle]
      state.carvoyantSubscriptions.add(createdSub)
      return createdSub
    }       
  }
  catch(HttpResponseException hre)
  {
    log.debug hre.getResponse().getData()
  }
}

//Creates a new Carvoyant subscription tailored to notify smartthings when a vehicle's ignition status should be updated.
def createIgnitionStatusSubscription(selectedVehicle)
{
  def body = [
    minimumTime: 0, 
    postUrl: serverUrl + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/ignitionStatusNotification", 
    notificationPeriod: "STATECHANGE"
  ]

  try
  {

    httpPostJson(
      uri: appSettings.carvoyantApiUrl + "/vehicle/${selectedVehicle}/eventSubscription/ignitionStatus/", 
      path: '',  
      headers: ["Content-Type": "application/json", "Authorization": "Bearer ${state.carvoyantAccessToken}"], 
      body: body
    ) 
    {
      response ->

      if(!state.carvoyantSubscriptions)
      {
        state.carvoyantSubscriptions = []
      }

      def createdSub = [subscriptionId: response.getData().subscription.id, vehicleId: selectedVehicle]
      state.carvoyantSubscriptions.add(createdSub)
      return createdSub
    }     
  }
  catch(HttpResponseException hre)
  {
    log.debug hre.getResponse().getData()
  }
}

//Creates a dynamic preferences page
def authPage() {

  //checks for simulator provided Carvoyant access token
  if(simulatorAccessToken)
  {
    state.carvoyantAccessToken = simulatorAccessToken
  }

  //determines the smartthings access token 
  if(!state.accessToken) {
    createAccessToken()
    state.accessToken = state.accessToken
  }

  if(state.carvoyantAccessToken  && !isTokenRevoked()) //The token is valid, so the vehicle list page is returned
  {
    return buildVehicleListPage()
  } 
  else //The token is not valid, so the auth page is returned
  {
    def redirectUrl = oauthInitUrl()
    log.debug "RedirectUrl = ${redirectUrl}"
    return dynamicPage(name: "auth", nextPage:null, uninstall:true) {
      section()
      {
        paragraph "Tap below to log in to the Carvoyant portal and authorize SmartThings access."
        href url:redirectUrl, style:"embedded", required:true, title:"Carvoyant Authorization", description:"unauthorized"
      }
    }
  }
}

//Checks Carvoyant access token status
def isTokenRevoked()
{
  log.debug "checking for revoked token"
  def reqParams = [
    uri: appSettings.carvoyantApiUrl + "/account/",
    headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.carvoyantAccessToken}"]
  ]

  try
  {
    httpGet(reqParams) { resp ->

     if(resp.status == 200) {
        return false
      }
      else {
        log.debug "Access token check error " + resp.getData() +  ": " + resp.getStatus()
        return true
      }
    }
  }

  catch(HttpResponseException hre)
  {
    log.debug "Access token check error " + hre.getResponse().getData() +  ": " + hre.getResponse().getStatus()
    return true
  }
}

//returns a dynamic page used to list Carvoyant vehicles
def buildVehicleListPage()
{
  return dynamicPage(name: "auth", nextPage:null, uninstall:uninstallAllowed) {
    section(){
      state.vehicles = getVehicles()
      paragraph "Tap below to see the list of vehicles available in your Carvoyant account and select the ones you want to connect to SmartThings."
      input(name: "selectedVehicles", title:"Carvoyant Vehicles", type: "enum", required:true, multiple:true, description: "Tap to choose", metadata:[values:state.vehicles])
    }
  }
}


//returns the list of vehicles from the user's Carvoyant account
def getVehicles() {
  def deviceListParams = [
    uri: appSettings.carvoyantApiUrl + "/vehicle/",
    headers: ["Content-Type": "text/json", "Authorization": "Bearer ${state.carvoyantAccessToken}"]
  ]

  def vehicles = [:]
  try
  {
    httpGet(deviceListParams) { resp ->

      if(resp.status == 200) {
        resp.data.vehicle.each { v ->
          vehicles[v.vehicleId] = v.name
        }
      }
      else 
      {
        log.error "Authentication error, invalid authentication method, lack of credentials, etc."
      }
    }
  }
  
  catch(Exception e)
  {
    log.debug "Ex " + e
  }
  
  
  return vehicles
}

//Builds the URL that will direct the user to the Carvoyant login screen
def oauthInitUrl() {
  log.debug "oauthInitUrl"

  state.oauthInitState = UUID.randomUUID().toString()

  def oauthParams = [
    response_type: "code",
    scope: "ems,smartWrite",
    client_id: appSettings.carvoyantClientId,
    state: state.oauthInitState,
    redirect_uri: serverUrl + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/receiveToken"
  ]
  
    log.debug "oauthInitUrl " + appSettings.carvoyantAuthUrl + "/OAuth/authorize?" + toQueryString(oauthParams)

  return appSettings.carvoyantAuthUrl + "/OAuth/authorize?" + toQueryString(oauthParams)
}

//This method receives an auth code from Carvoyant and requests a Carvoyant access token
def receiveToken() {
  def code = params.code
  def oauthState = params.state
  
  def tokenParams = [
    grant_type: "authorization_code",
    code: params.code,
    client_id: appSettings.carvoyantClientId,
    client_secret: appSettings.carvoyantSecret,
    redirect_uri: serverUrl + "/api/token/${state.accessToken}/smartapps/installations/${app.id}/receiveToken"
  ]

  def tokenUrl = appSettings.carvoyantTokenUrl

  def jsonMap
  try 
  {
    httpPost([uri: tokenUrl, body: tokenParams], { resp ->
      jsonMap = resp.data
    })
  } 
  catch (HttpResponseException hre) 
  {
    log.debug "Caught Exception: ${hre.statusCode}, ${hre.response}, ${hre.response.data}"
  }

  state.carvoyantRefreshToken = jsonMap.refresh_token
  state.carvoyantAccessToken = jsonMap.access_token
  state.carvoyantExpiresIn=jsonMap.expires_in
  state.carvoyantTokenType = jsonMap.token_type
  def authexptime = new Date((now() + (state.carvoyantExpiresIn * 60 * 1000))).getTime()
  state.carvoyantAuthexptime = authexptime

  render contentType: 'text/html', data: "<html><body>You have successfully authorized SmartThings to connect to your Carvoyant account. Select DONE to continue.</body></html>"
}

//Refreshes the Carvoyant access token
def refreshToken()
{
  def body = [client_id:appSettings.carvoyantClientId,
    client_secret:appSettings.carvoyantSecret,
    grant_type: "refresh_token",
    refresh_token: state.carvoyantRefreshToken ]

  try
  {
    def userPass = appSettings.carvoyantClientId + ":" + appSettings.carvoyantSecret 
    httpPostJson(uri: appSettings.carvoyantTokenUrl, headers: ["Authorization": "Basic " + userPass.encodeAsBase64().toString(), "Content-Type":"application/x-www-form-urlencoded"], body: toQueryString(body)) 
    {
      response ->

      log.debug response.getData()
      state.carvoyantRefreshToken = response.getData().refresh_token
      state.carvoyantAccessToken = response.getData().access_token
      log.debug state.carvoyantAccessToken
      log.debug state.carvoyantRefreshToken
    }       

  }
  catch(HttpResponseException hre)
  {
    log.debug hre.getResponse().getData()
  }
}


//Changes a map to a query string
def toQueryString(Map m) {
  return m.collect { k, v -> "${k}=${URLEncoder.encode(v.toString())}" }.sort().join("&")
}

def getServerUrl() { return "https://graph.api.smartthings.com" }
