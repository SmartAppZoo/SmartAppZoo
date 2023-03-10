/**
 *  Jack First App
 *
 *  Copyright 2016 Yunhan Jia
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
    name: "Jack First App",
    namespace: "jyh0082007",
    author: "Yunhan Jia",
    description: "My first smart app",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png") 
{
    appSetting "Name"
}


preferences {
    section("Title") {
        input "lock1","capability.lock",title:"Select a lock"
        // TODO: put inputs here
    }
}
mappings {
    path("/response/:data"){
        action:[
            POST: "onResponse"
        ]
    }
}
def installed() {
    log.info "Installed with settings: ${settings}"
    //Additional info that the app should maintain 
    state.actionQueue = []
    state.appName = "Jack First App"
    state.appDescription = "My first smart app"
    state.category = "Safety & Security"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    //Temporaily needed for debugging purpose, all the additional code in installed() shall be copied here
    state.actionQueue = []
    state.appName = "Jack First App"
    state.appDescription = "My first smart app"
    state.category = "Safety & Security"
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(lock1, "lock", lockHandler)
    subscribe(lock1, "response",responseHandler)
    // TODO: subscribe to attributes, devices, locations, etc.
}
def responseHandler(evt){
    log.debug "[Jack]onResponse"
}
def onResponse(){
    log.debug "OnResponse ${params.data}"
    lock1.unlock()
}
def lockHandler(evt){
    /* Original code, 
    if(evt.value=="locked"){
        lock1.unlock()
    }
    */
    def i = generateActionId()
    enqueueAction(i,"lock1","unlock","")
    sendRequest(url,"id=$i&device=lock1&action=${evt.value}&extra=66")
    log.debug "Queue ${state.actionQueue['id']}"
    //if(evt.value == "unlocked"){
    //  sendRequest("/")
    //}
    
    /* Not allowed by SmartThings
    try{
        HTTPBuilder builder = new HTTPBuilder('http://www.google.com')
        //HTTPBuilder has no direct methods to add timeouts.  We have to add them to the HttpParams of the underlying HttpClient
        builder.getClient().getParams().setParameter("http.connection.timeout", new Integer(TENSECONDS))
        builder.getClient().getParams().setParameter("http.socket.timeout", new Integer(THIRTYSECONDS))
        builder.request(GET, TEXT){
            response.success = { resp, res ->
            res.readLines().each {
                logger.debug it
            }      
        }    
  }
    }
    catch (e){
            log.error "something went wrong: $e"
    }*/
}
private Boolean sendRequest(url,data){
    try{
        httpPost("http://141.212.110.244:80/stbackend/service.php",data){
        resp -> resp.headers.each{         
        }
        //log.debug "response data: ${resp.data}"
        /*if(resp.data.toString().contains("Welcome")){
                log.debug "[Jack] OK"
        }*/
    }       
    }catch (e){
        log.error "something went wrong: $e"
        return false
    }
    return true
}
private int generateActionId(){
    //Math.abs(new Random().nextInt() % 9998) + 1
    //For testing purpose, actionID always 1
    return 1
    
}
private enqueueAction(id,device,action,extra){
    state.actionQueue << [id:id,device:device,action:action,extra:extra]
}