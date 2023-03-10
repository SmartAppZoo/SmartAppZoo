/**
 *  Roomie Agent Connect
 *
 *  Copyright 2014 Roomie Remote, Inc.
 *
 *	Date: 2014-11-18
 */

definition(
    name: "Roomie Agent Connect",
    namespace: "roomieremote-raconnect",
    author: "Roomie Remote, Inc.",
    description: "Allow you to integrate your Roomie controlled home with SmartThings via Roomie Agent.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/roomieuser/remotes/roomie-st-60.png",
    iconX2Url: "https://s3.amazonaws.com/roomieuser/remotes/roomie-st-120.png",
    iconX3Url: "https://s3.amazonaws.com/roomieuser/remotes/roomie-st-120.png")

preferences()
{
	page(name: "mainPage", title: "Roomie Setup", content: "mainPage", refreshTimeout: 5)
    page(name:"agentDiscovery", title:"Roomie Agent Discovery", content:"agentDiscovery", refreshTimeout:5)
}

def mainPage()
{
	if (canInstallLabs())
    {
       	return agentDiscovery()
    }
    else
    {
        def upgradeNeeded = """To use SmartThings Labs, your Hub should be completely up to date.

To update your Hub, access Location Settings in the Main Menu (tap the gear next to your location name), select your Hub, and choose "Update Hub"."""

        return dynamicPage(name:"mainPage", title:"Upgrade needed!", nextPage:"", install:false, uninstall: true) {
            section("Upgrade")
            {
                paragraph "$upgradeNeeded"
            }
        }
    }
}

def agentDiscovery(params=[:])
{
	int refreshCount = !state.refreshCount ? 0 : state.refreshCount as int
    state.refreshCount = refreshCount + 1
    def refreshInterval = refreshCount == 0 ? 2 : 5
	
    if (!state.subscribe)
    {
        subscribe(location, null, locationHandler, [filterEvents:false])
        state.subscribe = true
    }
	
    //ssdp request every fifth refresh
    if ((refreshCount % 5) == 0)
    {
        discoverAgents()
    }
	
    def agentsDiscovered = agentsDiscovered()
    
    return dynamicPage(name:"agentDiscovery", title:"Pair with Roomie Agent", nextPage:"", refreshInterval: refreshInterval, install:true, uninstall: true) {
        section("")
        {
            input "selectedAgent", "enum", required:true, title:"Select Roomie Agent \n(${agentsDiscovered.size() ?: 0} found)", multiple:false, options:agentsDiscovered
        }
    }
}

def discoverAgents()
{
    def urn = getURN()
    
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery $urn", physicalgraph.device.Protocol.LAN))
}

def agentsDiscovered()
{
    def gAgents = getAgents()
    def agents = gAgents.findAll { it?.value?.verified == true }
    def map = [:]
    agents.each
    {
        map["${it.value.uuid}"] = it.value.name
    }
    map
}

def getAgents()
{
    if (!state.agents)
    {
    	state.agents = [:]
    }
    
    state.agents
}

def installed()
{
	initialize()
}

def updated()
{
	initialize()
}

def initialize()
{
    unsubscribe()
    state.subscribe = false
    
    if (selectedAgent)
    {
    	addOrUpdateAgent(state.agents[selectedAgent])
    }
}

def addOrUpdateAgent(agent)
{
	def children = getChildDevices()
	def dni = agent.ip + ":" + agent.port
    def found = false
	
	children.each
	{
		if ((it.getDeviceDataByName("mac") == agent.mac))
		{
        	found = true
            
            if (it.getDeviceNetworkId() != dni)
            {
				it.setDeviceNetworkId(dni)
			}
		}
        else if (it.getDeviceNetworkId() == dni)
        {
        	found = true
        }
	}
    
	if (!found)
	{
        addChildDevice("roomieremote-agent", "Roomie Agent", dni, agent.hub, [label: "Roomie Agent"])
	}
}

def locationHandler(evt)
{
    def description = evt?.description
    def urn = getURN()
    def hub = evt?.hubId
    def parsedEvent = parseEventMessage(description)
    
    parsedEvent?.putAt("hub", hub)
    
    //SSDP DISCOVERY EVENTS
	if (parsedEvent?.ssdpTerm?.contains(urn))
	{
        def agent = parsedEvent
        def ip = convertHexToIP(agent.ip)
        def agents = getAgents()
        
        agent.verified = true
        agent.name = "Roomie Agent $ip"
        
        if (!agents[agent.uuid])
        {
        	state.agents[agent.uuid] = agent
        }
    }
}

private def parseEventMessage(String description)
{
	def event = [:]
	def parts = description.split(',')
    
	parts.each
    { part ->
		part = part.trim()
		if (part.startsWith('devicetype:'))
        {
			def valueString = part.split(":")[1].trim()
			event.devicetype = valueString
		}
		else if (part.startsWith('mac:'))
        {
			def valueString = part.split(":")[1].trim()
			if (valueString)
            {
				event.mac = valueString
			}
		}
		else if (part.startsWith('networkAddress:'))
        {
			def valueString = part.split(":")[1].trim()
			if (valueString)
            {
				event.ip = valueString
			}
		}
		else if (part.startsWith('deviceAddress:'))
        {
			def valueString = part.split(":")[1].trim()
			if (valueString)
            {
				event.port = valueString
			}
		}
		else if (part.startsWith('ssdpPath:'))
        {
			def valueString = part.split(":")[1].trim()
			if (valueString)
            {
				event.ssdpPath = valueString
			}
		}
		else if (part.startsWith('ssdpUSN:'))
        {
			part -= "ssdpUSN:"
			def valueString = part.trim()
			if (valueString)
            {
				event.ssdpUSN = valueString
                
                def uuid = getUUIDFromUSN(valueString)
                
                if (uuid)
                {
                	event.uuid = uuid
                }
			}
		}
		else if (part.startsWith('ssdpTerm:'))
        {
			part -= "ssdpTerm:"
			def valueString = part.trim()
			if (valueString)
            {
				event.ssdpTerm = valueString
			}
		}
		else if (part.startsWith('headers'))
        {
			part -= "headers:"
			def valueString = part.trim()
			if (valueString)
            {
				event.headers = valueString
			}
		}
		else if (part.startsWith('body'))
        {
			part -= "body:"
			def valueString = part.trim()
			if (valueString)
            {
				event.body = valueString
			}
		}
	}

	event
}

def getURN()
{
    return "urn:roomieremote-com:device:roomie:1"
}

def getUUIDFromUSN(usn)
{
	def parts = usn.split(":")
	
	for (int i = 0; i < parts.size(); ++i)
	{
		if (parts[i] == "uuid")
		{
			return parts[i + 1]
		}
	}
}

def String convertHexToIP(hex)
{
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

def Integer convertHexToInt(hex)
{
	Integer.parseInt(hex,16)
}

def Boolean canInstallLabs()
{
    return hasAllHubsOver("000.011.00603")
}

def Boolean hasAllHubsOver(String desiredFirmware)
{
    return realHubFirmwareVersions.every { fw -> fw >= desiredFirmware }
}

def List getRealHubFirmwareVersions()
{
    return location.hubs*.firmwareVersionString.findAll { it }
}