/**
 *  Curb Bridge
 *
 *  Copyright 2016 Justin Haines
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
    name: "Curb Bridge",
    namespace: "jhaines0",
    author: "Justin Haines",
    description: "App to receive data from Curb home energy monitor",
    category: "",
    iconUrl: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    iconX2Url: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    iconX3Url: "http://energycurb.com/wp-content/uploads/2015/12/curb-web-logo.png",
    oauth: true)


preferences {
//	section("Settings") {
//    	input "updatePeriod", "number", required: false, title: "Update Period (in seconds)", defaultValue: 10, range: "1..*"
//    }
}

mappings {
  path("/data") {
    action: [
      PUT: "dataArrived"
    ]
  }
  path("/historical") {
    action: [
      PUT: "historicalArrived"
    ]
  }
}

def installed() {
}

def updated() {
}

def uninstalled() {
	log.debug "Uninstalling"
    removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def updateChildDevice(dni, label, values)
{
    try
    {
        def existingDevice = getChildDevice(dni)

        if(!existingDevice)
        {
            existingDevice = addChildDevice("jhaines0", "Curb Power Meter", dni, null, [name: "${dni}", label: "${label}"])
        }
		
        existingDevice.handleMeasurements(values)
    }
    catch (e)
    {
        log.error "Error creating or updating device: ${e}"
    }
}

def dataArrived()
{
    def json = request.JSON
    if(json)
    {
        //log.debug "Got Data: ${json}"
        def total = 0.0

        json.circuits.each
        {
            updateChildDevice("${it.id}", it.label, it.w)

            if(it.main)
            {
                total += it.w
            }
        }

        updateChildDevice("__MAIN__", "Main", total)
    }
}

def historicalArrived()
{
    def json = request.JSON
    if(json)
    {
        //log.debug "Got Historical Data: ${json}"
        
        def total = null
        
        json.each
        {
			updateChildDevice("${it.id}", it.label, it.values)
            
            if(it.main)
            {
            	it.values.sort{a,b -> a.t <=> b.t}
            	if(total == null)
                {
                	total = it
                }
                else
                {
                    if(it.values.size() != total.values.size())
                    {
                    	log.debug("Size mismatch")
                    }
                    else
                    {
						for(int i = 0; i < total.values.size(); ++i)
                        {
                        	if(total.values[i].t != it.values[i].t)
                            {
                            	log.debug("Time mismatch")
                            }
                            else
                            {
								total.values[i].w = (total.values[i].w) + (it.values[i].w)
							}
                        }
					}
				}
            }
        }
        
        updateChildDevice("__MAIN__", "Main", total.values)
    }
}

