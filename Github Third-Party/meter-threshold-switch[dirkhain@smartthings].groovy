/**
 *  Meter Threshold Switch
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
        name: "Energy Meter Threshold Switch",
        namespace: "dirkhain",
        author: "dirkhain",
        description: "Turn things on or off when an energy meter passes a threshold",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
        pausable: true
)

preferences {
    page name: "mainPage", title: "Select meter, threshold and switch", install: false, uninstall: true, nextPage: "namePage"
    page name: "namePage", title: "Name this automation (optional)", install: true, uninstall: true
}

// main page to select lights, the action, and turn on/off times
def mainPage() {
    dynamicPage(name: "mainPage") {
        section {
            triggerInputs()
            thresholdInputs()
            onOffInputs()
        }
    }
}

def namePage() {
    if (!customLabel) {
        // if the user selects to not change the label, give a default label
        def l = defaultLabel()
        log.debug "will set default label of $l"
        app.updateLabel(l)
    }
    dynamicPage(name: "namePage") {
        if (customLabel) {
            section("Automation name") {
                label title: "Enter custom name", defaultValue: app.label, required: false
            }
        } else {
            section("Automation name") {
                paragraph app.label
            }
        }
        section {
            input "customLabel", "bool", title: "Edit automation name", defaultValue: "false", required: "false", submitOnChange: true
        }
    }
}

// ========================================================
// Lifecycle methods
// ========================================================

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
    if (!overrideLabel) {
        app.updateLabel(defaultLabel())
    }
    subscribe(meter, "power", meterHandler)
}

// ========================================================
// Helpers
// ========================================================

def triggerInputs() {
    input(name: "meter", type: "capability.powerMeter", title: "Meter that triggers", required: true, multiple: false, description: null, submitOnChange: true)
}

def thresholdInputs() {
    if(meter) {
        input(name: "aboveThreshold", type: "number", title: "Reports Above...", required: false, description: "in either watts or kw.", submitOnChange: true)
        input(name: "belowThreshold", type: "number", title: "Or Reports Below...", required: false, description: "in either watts or kw.", submitOnChange: true)
    }
}

def onOffInputs() {
    if(aboveThreshold || belowThreshold) {
        input(name: "switchesOn", type: "capability.switch", title: "Turn On This Switch", required: false, multiple: false, description: null)
        input(name: "switchesOff", type: "capability.switch", title: "Turn Off This Switch", required: false, multiple: false, description: null)
    }
}

// Event handler triggering actions
def meterHandler(evt) {
    def meterValue = evt.value as double
    if (aboveThreshold) {
        def aboveThresholdValue = aboveThreshold as int
        if (meterValue > aboveThresholdValue) {
            log.debug "${meter} reported energy consumption above ${threshold}. Turning switches."
            switchSwitches()
        }
    }

    if (belowThreshold) {
        def belowThresholdValue = belowThreshold as int
        if (meterValue < belowThresholdValue) {
            log.debug "${meter} reported energy consumption below ${threshold}. Turning switches."
            switchSwitches()
        }
    }
}

def switchSwitches() {
    if(switchesOn) switchesOn.on()
    if(switchesOff) switchesOff.off()
}

def defaultLabel() {
    def meterLabel = settings.meter ? meter.displayName : "TriggerMeter"
    def switchOnLabel = settings.switchesOn ? "Switch on " + settings.switchesOn.displayName : ""
    def switchOffLabel = settings.switchesOff ? "Switch off " + settings.switchesOff.displayName : ""

    if (aboveThreshold) {
        "$switchOnLabel $switchOffLabel when $meterLabel reads above $aboveThreshold"
    } else if (belowThreshold) {
        "$switchOnLabel $switchOffLabel when $meterLabel reads below $belowThreshold"
    } else {
        "No threshold specified."
    }
}
