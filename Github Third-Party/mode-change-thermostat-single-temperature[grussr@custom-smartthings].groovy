/* **DISCLAIMER**
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
 * 1. the software will meet your requirements or expectations;
 * 2. the software or the software content will be free of bugs, errors, viruses or other defects;
 * 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
 * 4. the software will be compatible with third party software;
 * 5. any errors in the software will be corrected.
 * The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
 */ 
 
/**
 *  Mode Change Thermostat Single Temperature
 *
 * Copyright RBoy, redistribution of any changes or modified code is not allowed without permission
 * Updated: 2015-5-17
 *
 */
definition(
		name: "Mode Change Thermostat Single Temperature",
		namespace: "rboy",
		author: "RBoy",
		description: "Change the thermostat temperature (single temp for all thermostats) on a mode change",
    	category: "Green Living",
    	iconUrl: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving.png",
    	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@2x.png",
    	iconX3Url: "https://s3.amazonaws.com/smartapp-icons/GreenLiving/Cat-GreenLiving@3x.png")

preferences {
	section("Choose thermostat (s)") {
		input "thermostats", "capability.thermostat", required: true, multiple: true
	}

    section("Set mode temperatures") {
        input "opHeatSet", "decimal", title: "When Heating", description: "Heating temperature for mode"
        input "opCoolSet", "decimal", title: "When Cooling", description: "Cooling temperature for mode"
    }
}

def installed()
{
	subscribeToEvents()
}

def updated()
{
    unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
    subscribe(location, modeChangeHandler)
}

// Handle mode changes, reinitialize the current temperature and timers after a mode change
def modeChangeHandler(evt) {
    thermostats.setHeatingSetpoint(opHeatSet)
    thermostats.setCoolingSetpoint(opCoolSet)
    log.info "Set $thermostats Heat $opHeatSet??, Cool $opCoolSet?? on $evt.value mode"
    sendNotificationEvent("Set $thermostats Heat $opHeatSet??, Cool $opCoolSet?? on $evt.value mode")
}