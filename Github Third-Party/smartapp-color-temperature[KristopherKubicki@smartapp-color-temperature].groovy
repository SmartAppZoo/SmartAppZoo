/**
 *  Color Temperature
 *   
 */

definition(
	name: "Color Temperature",
	namespace: "KristopherKubicki",
	author: "kristopher@acm.org",
	description: "Set your lights to a specific color temperature",
	category: "Green Living",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Partner/hue.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Partner/hue@2x.png"
)

preferences {
	section("Control these bulbs...") {
		input "bulbs", "capability.colorControl", title: "Which Hue Bulbs?", multiple:true, required: false
	}
    section("Control these Temperature Changing bulbs...") {
		input "cbulbs", "capability.switchLevel", title: "Which Temperature Changing Bulbs?", multiple:true, required: false
	}
    section("Color Temperature") {
		input "ct", "number", title: "Temperature (Kelvin)", required: true
	}
    section("While in this mode") {
		input "smode", "mode", title: "Which mode?", multiple:false, required: true
	}
}


def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

private def initialize() {
	log.debug("initialize() with settings: ${settings}")
	subscribe(bulbs, "switch.on", sunHandler)
    subscribe(cbulbs, "switch.on", sunHandler)
    
	subscribe(location, "mode", modeHandler)
}

// If we detect the Hub moving into a sleep mode, also activate the handler
def modeHandler(evt) {
log.debug "chanigng mode $evt"
    	if(location.mode == smode) { 
        	sunHandler(evt)
        }
}

def sunHandler(evt) {
	def hsb = rgbToHSB(ctToRGB(ct))
    
    if(location.mode != smode) { 
    	return
    }
    
    for (cbulb in cbulbs) { 
		if(cbulb.currentValue("kelvin") as Integer != ct as Integer && cbulb.currentValue("switch") == "on") { 
			cbulb.setColorTemp(ct)
        }
	}
    
	for (bulb in bulbs) {
    	def hue = bulb.currentValue("hue") ?: 0 
    	def sat = bulb.currentValue("saturation") ?: 0 
    	def bri = bulb.currentValue("level") ?: 0
    
//  log.debug "1 $bulb current $hue vs ${hsb.h} : $sat vs ${hsb.s} : $bri vs ${hsb.b} [$ison]"
		if(bulb.currentValue("switch") == "on" && (hue != hsb.h || sat != hsb.s || bri != hsb.b)) { 
    	    log.debug "Setting $bulb to $colorTemp Kelvin at ${hsb.b}% brightness"
 	 		def newValue = [hue: hsb.h, saturation: hsb.s, level: hsb.b ]
			bulb.setColor(newValue) 
		}
	}
}

// Based on color temperature converter from 
//  http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code/
// Will not work for color temperatures from 2700 to 6000 
def ctToRGB(ct) { 

	ct = ct / 100
	def r = 255
	def g = 99.4708025861 * Math.log(ct) - 161.1195681661
	def b = 138.5177312231 * Math.log(ct - 10) - 305.0447927307

//	log.debug("r: $r g: $g b: $b")

	def rgb = [:]
	rgb = [r: r, g: g, b: b] 
	rgb
}


// Based on color calculator from
//  http://codeitdown.com/hsl-hsb-hsv-color/
// 
def rgbToHSB(rgb) {
	def r = rgb.r
	def g = rgb.g
	def b = rgb.b
	float hue, saturation, brightness;

	float cmax = (r > g) ? r : g;
	if (b > cmax) cmax = b;
	float cmin = (r < g) ? r : g;
	if (b < cmin) cmin = b;

	brightness = cmax / 255;
	if (cmax != 0) saturation = (cmax - cmin) / cmax;
	else saturation = 0;
		
	if (saturation == 0) hue = 0;
	else hue = 0.60 * ((g - b) / (255 -  cmin)) % 360

//	log.debug("h: $hue s: $saturation b: $brightness")
 
	def hsb = [:]    
	hsb = [h: hue * 100, s: saturation * 100, b: brightness * 100]
	hsb
}
