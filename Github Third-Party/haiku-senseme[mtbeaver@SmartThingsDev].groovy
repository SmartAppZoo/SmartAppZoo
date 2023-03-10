definition(
    name: "Haiku SenseMe Service Manager",
    namespace: "smartthings",
    author: "mtbeaver",
    description: "Service Manager for discovering Haiku SenseMe Smart fans",
    category: "Fan",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "deviceDiscovery", title: "Haiku Smart Fan Discovery", nextPage: "deviceDiscovery")
}    

private String _broadcastAddr = "FFFFFFFF"; //!< Broadcast addr 255.255.255.255 as hex string
private String _port = "7AB7"; //!< Port 31415 as hex-string

def deviceDiscovery() {
    

}

//! Called when the Smart App is initialized.  Here, we subscribe to network events,
def initialized() {

}


void sendDiscoveryRequest() {
    def action = new physicangraph.device.HubAction(
        "<ALL;DEVICE;ID;GET>",
        physicalgraph.device.Protocol.LAN,
        "${_broadcstAddr}:${_port}",
        [
            callback: discoveryResponseHandler,
            type: "LAN_TYPE_UDPCLIENT"
        ])
}

void discoveryResponseHandler(physicalgraph.device.HubResponse hubResponse) {
    
}

void addDevice() {
    
}

