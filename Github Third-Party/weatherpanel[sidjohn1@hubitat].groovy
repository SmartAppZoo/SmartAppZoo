/**
 *  Weather Panel
 *
 *  Copyright 2023 Sidney Johnson
 *  If you like this code, please support the developer via PayPal: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XKDRYZ3RUNR9Y
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
 *	Version: 1.0 - Initial Version
 *	Version: 1.1 - Fixed font size not changing the font size
 *	Version: 1.2 - Decoupled weather data refresh from wallpaper refresh
 *	Version: 1.3 - Minor formating tweaks, removed all static data from json
 *	Version: 2.0 - Addeded 3 day forcast and more formating and presentation tweaks. Removed weather station requirement
 *	Version: 2.1 - Preloads images for smoother transitions
 *	Version: 2.1.1 - Added dynamic API URL
 *	Version: 2.2 - Added support for user selectable Station ID
 *	Version: 2.2.1 - Added better browser support
 *	Version: 2.3 - Upgraded Icons
 *	Version: 2.4 - TWC Weather Update
 *	Version: 2.5 - Added Rain Switch
 *	Version: 2.6 - Hubitat updates
 *	Version: 2.7 - Hubitat local updates
 *
 */

import java.text.SimpleDateFormat 
import groovy.time.TimeCategory 

definition(
    name: "Weather Panel",
    namespace: "sidjohn1",
    author: "Sidney Johnson",
    description: "Weather Panel, a Hubitat web client",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
    importUrl: 'https://raw.githubusercontent.com/sidjohn1/hubitat/main/WeatherPanel/weatherpanel.groovy',
    oauth: true)

preferences {
    page(name: "selectDevices")
    page(name: "viewURL")
}

def selectDevices() {
	dynamicPage(name: "selectDevices", install: true, uninstall: true) {
	    section("About") {
			paragraph "Weather Panel displays inside and outside temp and weather infomation as a web page. Also has a random customizable background."
			paragraph "${textVersion()}\n${textCopyright()}"
		    }
		section("Select...") {
			input "insideTemp", "capability.temperatureMeasurement", title: "Inside Tempature...", multiple: false, required: false
            		input "outsideTemp", "capability.temperatureMeasurement", title: "Outside Tempature...", multiple: false, required: false
            		input "forcastDevice", "capability.temperatureMeasurement", title: "Forcast Device...", multiple: false, required: false
			input "showForcast", "bool", title:"Show Forcast", required: false, multiple:false
            		input "stationID", "text", title:"Station ID (Optional)", required: false, multiple:false
            		input "rainswitch", "capability.switch", title:"Rain Switch (Optional)", required: false, multiple:false
		}
		section(hideable: true, hidden: true, "Optional Settings") {
            		input "fontColor", "enum", title:"Select Font Color", required: false, multiple:false, defaultValue: "White", options: [3: 'Black',2: 'Ivory', 1:'White']
			input "fontSize", "enum", title:"Select Font Size", required: false, multiple:false, defaultValue: "Medium", options: [4: 'xSmall',3: 'Small',2: 'Medium', 1:'Large']
            		input "localResources", "bool", title: "Use Local Resources?", required: false, defaultValue: false
		}
		section("Wallpaper URL") {
			input "wallpaperUrl", "text", title: "Wallpaper URL",defaultValue: "http://", required:false
		}
        	section() {
			href "viewURL", title: "View URL"
		}
	}
}

def viewURL() {
	dynamicPage(name: "viewURL", title: "${title ?: location.name} Weather Pannel URL", install:false) {
		section() {
			paragraph "Copy the URL below to any modern browser to view your ${title ?: location.name}s' Weather Panel. Add a shortcut to home screen of your mobile device to run as a native app."
			input "weatherUrl", "text", title: "URL",defaultValue: "${generateURL("html")}", required:false
			href url:"${generateURL("html")}", style:"embedded", required:false, title:"View", description:"Tap to view, then click \"Done\""
		}
	}
}

mappings {
    path("/html") { action: [GET: "generateHtml"] }
	path("/json") {	action: [GET: "generateJson"] }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	unsubscribe()
	initialize()
}

def initialize() {
	log.info "Weather Panel ${textVersion()} ${textCopyright()}"
	generateURL()
}

def generateHtml() {
	render contentType: "text/html", headers: ["Access-Control-Allow-Origin": "*"], data: "<!DOCTYPE html>\n<html>\n<head>${head()}</head>\n<body>\n${body()}\n</body></html>"
}

def generateJson() {
	render contentType: "application/json", headers: ["Access-Control-Allow-Origin": "*"], data: "${jsonData()}"
}

def head() {

def color1
def color2
def font1
def font2
def font3
def iconW
def rTimeout    
def temp1TA
def temperatureScale = getTemperatureScale()
def weatherDataContent

    
rTimeout = Math.floor(Math.random() * (1000000 - 800000 + 1) ) + 1750000
rTimeout = rTimeout.toInteger()
    
switch (settings.fontSize) {
	case "1":
	font1 = "50"
	font2 = "20"
	font3 = "10"
	break;
	case "2":
	font1 = "48"
	font2 = "18"
	font3 = "10"
	break;
	case "3":
	font1 = "46"
	font2 = "16"
	font3 = "10"
	break;
    case "4":
	font1 = "44"
	font2 = "16"
	font3 = "7"
	break;
}

switch (settings.fontColor) {
    case "1":
	color1 = "255,255,255"
	color2 = "0,0,0"
    color3 = "0,0,0"
	break;
	case "2":
	color1 = "255,248,220"
	color2 = "222,184,135"
    color3 = "0,0,0"
	break;
	case "3":
    color1 = "0,0,0"
	color2 = "255,255,255"
    color3 = "255,255,255"
	break;
}

if (settings.localResources) {
        fontURL = "/local/"
} 
else {
    fontURL = "https://sidjohn1.github.io/hubitat/weatherpanel/"
}   
    
if (showForcast == true) {
	iconW = "47"
	temp1TA = "right"
	weatherDataContent = """	    		content += '<div id="icon"><i class="wi wi-' + item.icon + '"></i></div>';
	    		content += '<div id="temp1" class="text3"><p>' + item.temp1 + '°<b>${temperatureScale}&nbsp;<br>Inside&nbsp;</b><br>' + item.temp2 + '°<b>${temperatureScale}&nbsp;<br>Outside&nbsp;</b><br></p></div>';
    			content += '<div id="cond" class="text2"><p>' + item.cond + '&nbsp;</p></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forecastDay + '<br><i class="wi wi-' + item.forecastIcon + '"></i>&nbsp;&nbsp;' + item.forecastDayHigh + '<br><u>' + item.forecastDayLow + '</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forecastDay1 + '<br><i class="wi wi-' + item.forecastIcon1 + '"></i>&nbsp;&nbsp;' + item.forecastDayHigh1 + '<br><u>' + item.forecastDayLow1 + '</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forecastDay2 + '<br><i class="wi wi-' + item.forecastIcon2 + '"></i>&nbsp;&nbsp;' + item.forecastDayHigh2 + '<br><u>' + item.forecastDayLow2 + '</u></p><br></div>';
    			content += '<div id="forecast" class="text3"><p>' + item.forecastDay3 + '<br><i class="wi wi-' + item.forecastIcon3 + '"></i>&nbsp;&nbsp;' + item.forecastDayHigh3 + '<br><u>' + item.forecastDayLow3 + '</u></p><br></div>';"""
}
   else {
	iconW = "100"
	temp1TA = "left"
   	weatherDataContent = """	    		content += '<div id="icon"><i class="wi wi-' + item.icon + '"></i></div>';
	    		content += '<div id="temp1" class="text1"><p>' + item.temp1 + '°<b>${temperatureScale}<br>Inside</b></p></div>';
	    		content += '<div id="temp2" class="text1"><p>' + item.temp2 + '°<b>${temperatureScale}<br>Outside</b></p></div>';
    			content += '<div id="cond" class="text1"><p>' + item.cond + '&nbsp;</p></div>';"""
}

"""<!-- Meta Data -->
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
	<meta name="Description" content="Weather Panel" />
	<meta name="application-name" content="Weather Panel" />
	<meta name="apple-mobile-web-app-title" content="Weather Panel">
	<meta name="keywords" content="weather,panel,hubitat" />
	<meta name="Author" content="sidjohn1" />
<!-- Apple Web App -->
	<meta name="apple-mobile-web-app-capable" content="yes" />
	<meta name="mobile-web-app-capable" content="yes" />
	<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
	<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=0" />
	<link rel="apple-touch-icon-precomposed" href="${fontURL}weatherpanel.png" />
<!-- Stylesheets -->
<style type="text/css">
body{
	background-size: cover;
	-webkit-background-size: cover;
	-moz-background-size: cover;
	-o-background-size: cover;
	background-attachment: fixed;
	background-color: rgb(${color3});
	background-position: center;
    background-repeat: no-repeat;
	overflow: hidden;
	margin: 0 0;
	width: 100%;
	height: 100%;
}
b{
	font-size: 20px;
	font-size: ${font3}vh;
	vertical-align: super;
}
p{
	font-family:Gotham, "Helvetica Neue", Helvetica, Arial, sans-serif;
	color: rgb(${color1});
	text-shadow: 2px 2px 1px rgb(${color2});
	margin:0 0;
	opacity: 0.9;
}
i{
	color: rgb(${color1});
	text-shadow: 2px 2px 1px rgb(${color2});
	vertical-align: middle;
	opacity: 0.9;
}
div{
	background: transparent;
}
u{
	text-decoration: overline;
}
.text1 {
	font-weight: bold;
	vertical-align: text-top;
	margin-top: -3%;
}
.text2 {
	font-weight: 900;
    letter-spacing: 5px;
	vertical-align: super;
	margin-top: -3%;
	margin-bottom: 1%;
}
.text3 {
	font-weight: bold;
	vertical-align: super;
}
#data {
	display: flex;
	display: -webkit-flex;
	flex-direction: row;
	-webkit-flex-direction: row;
	flex-wrap: wrap;
	-webkit-flex-wrap: wrap;
}
#icon{
	margin: 3% 0 0 1%;
	font-size: 20px;
	font-size: ${font1}vh;
	text-align: center;
	width: ${iconW}%;
}
#temp1{
	text-align: ${temp1TA};
	float: left;
	width: 48%;
	margin-left: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#temp2{
	text-align: right;
	float: right;
	width: 48%;
	margin-right: 2%;
	font-size: 20px;
	font-size: ${font2}vh;
	line-height: 13vh;
}
#cond{
	white-space: nowrap;
	text-align: right;
	width: 100%;
	font-size: 20px;
	font-size: ${font3}vh;
}
#forecast{
	white-space: nowrap;
	text-align: right;
	width: 20%;
	font-size: 20px;
	font-size: 7vh;
	background: rgba(${color3},.5);
	vertical-align: middle;
    padding-right: 5%;
}
</style>
<link type="text/css" rel="stylesheet" href="${fontURL}weatherpanel.css"/>
<link rel="shortcut icon" type="image/png" href="${fontURL}weatherpanel.png"/>
<link rel="manifest" href="${fontURL}weatherpanel.json">
    <!-- Page Title -->
    <title>Weather Panel</title>
  	<!-- Javascript -->
<script type="text/javascript" charset="utf-8" src="${fontURL}weatherpanel.js"></script>
<script type="text/javascript">
\$(window).load(function(){
	var bg = '';
	var tImage = new Image();
	\$("#data").click(function(){
		var path = "${wallpaperUrl}";
		var fileList = "index.json";
		\$.getJSON(path+fileList,function(list,status){
			var mime = '*';
			while (mime.search('image')){
				obj = list[Math.floor(Math.random()*list.length)];
				mime=obj.mime;
			}
			bg = path+obj.path;
			bg = bg.replace('#','%23');
            \$('<img src="'+bg+'"/>');
            setTimeout(function(){
				document.body.background = bg;
			},3109);
		});
        setTimeout('\$("#data").click()', ${rTimeout});
	});
	\$("#data").click();
});
</script>

<script type="text/javascript">
\$(document).ready(function(){
	weatherData = function () {
		\$.getJSON("${generateURL("json")}",function(weather){
		var content = '';
			\$.each(weather.data, function(i,item){
${weatherDataContent}
				\$("#data").empty();
    			\$(content).appendTo("#data");
    		});
    	});
    	setTimeout(weatherData, 122500);
	}
	weatherData();
});
</script>
"""
}

def body() {  
"""<div id="data"></div>"""
}

def jsonData(){
//log.debug "refreshing weather"
sendEvent(linkText:app.label, name:"weatherRefresh", value:"refreshing weather", descriptionText:"weatherRefresh is refreshing weather", eventType:"SOLUTION_EVENT", displayed: true)

def alerts
def astronomy
def current
def currentTemp
def forecast
int forecastDayHigh
def forecastDayHigh1
def forecastDayHigh2
def forecastDayHigh3
int forecastDayLow
def forecastDayLow1
def forecastDayLow2
def forecastDayLow3
def temperatureScale = getTemperatureScale()
def forecastDay1
def forecastNight1
def forecastDay2
def forecastNight2
def forecastDay3
def forecastNight3
def forecastDay4
def forecastNight4
    
    forecastDayHigh = forcastDevice.currentValue('todaysHigh').toInteger()
    forecastDayLow = forcastDevice.currentValue('todaysLow').toInteger()

def weatherIcons = []
    
    if (forecastDayLow < forecastDayHigh){
//        log.debug "$forecastDayLow $forecastDayHigh true"
    forecastDay1 = forcastDevice.currentValue("zforecast_1").split(':')
    forecastNight1 = forcastDevice.currentValue("zforecast_2").split(':')
    forecastDay2 = forcastDevice.currentValue("zforecast_3").split(':')
    forecastNight2 = forcastDevice.currentValue("zforecast_4").split(':')
    forecastDay3 = forcastDevice.currentValue("zforecast_5").split(':')
    forecastNight3 = forcastDevice.currentValue("zforecast_6").split(':')
    forecastDay4 = forcastDevice.currentValue("zforecast_7").split(':')
    forecastNight4 = forcastDevice.currentValue("zforecast_8").split(':')
}
    else {
//        log.debug "$forecastDayLow $forecastDayHigh false"
    forecastDay1 = forcastDevice.currentValue("zforecast_2").split(':')
    forecastNight1 = forcastDevice.currentValue("zforecast_3").split(':')
    forecastDay2 = forcastDevice.currentValue("zforecast_4").split(':')
    forecastNight2 = forcastDevice.currentValue("zforecast_5").split(':')
    forecastDay3 = forcastDevice.currentValue("zforecast_6").split(':')
    forecastNight3 = forcastDevice.currentValue("zforecast_7").split(':')
    forecastDay4 = forcastDevice.currentValue("zforecast_8").split(':')
    forecastNight4 = forcastDevice.currentValue("zforecast_9").split(':')
    }
    
if (forcastDevice.currentValue("icon").contains(/day/)){
	weatherIcons = ["skc": "day-sunny", "few": "day-sunny", "sct": "day-cloudy", "bkn": "day-cloudy", "ovc": "day-cloudy", "wind_skc": "day-windy", "wind_few": "day-windy", "wind_sct": "day-windy", "wind_bkn": "day-windy", "wind_ovc": "day-windy", "snow": "day-snow", "rain_snow": "day-rain-mix", "rain_sleet": "day-sleet", "snow_sleet": "day-sleet", "fzra": "day-sleet", "rain_fzra": "day-sleet", "snow_fzra": "day-sleet", "sleet": "day-sleet", "rain": "day-showers", "rain_showers": "day-showers", "rain_showers_hi": "day-showers", "tsra": "day-storm-showers", "tsra_sct": "day-storm-showers", "tsra_hi": "day-storm-showers", "tornado": "tornado", "hurricane": "hurricane", "tropical_storm": "hurricane", "dust": "day-haze", "smoke": "day-haze", "haze": "day-haze", "hot": "hot", "cold": "snowflake-cold", "blizzard": "snowflake-cold", "fog": "day-haze"]
}
else if (forcastDevice.currentValue("icon").contains(/night/)){
	weatherIcons = ["skc": "night-clear", "few": "night-clear", "sct": "night-alt-cloudy", "bkn": "night-alt-cloudy", "ovc": "night-alt-cloudy", "wind_skc": "alt-cloudy-windy", "wind_few": "alt-cloudy-windy", "wind_sct": "alt-cloudy-windy", "wind_bkn": "alt-cloudy-windy", "wind_ovc": "alt-cloudy-windy", "snow": "night-alt-snow", "rain_snow": "night-alt-rain-mix", "rain_sleet": "night-alt-sleet", "snow_sleet": "night-alt-sleet", "fzra": "night-alt-sleet", "rain_fzra": "night-alt-sleet", "snow_fzra": "night-alt-sleet", "sleet": "night-alt-sleet", "rain": "night-alt-showers", "rain_showers": "night-alt-showers", "rain_showers_hi": "night-alt-showers", "tsra": "night-alt-storm-showers", "tsra_sct": "night-alt-storm-showers", "tsra_hi": "night-alt-storm-showers", "tornado": "tornado", "hurricane": "hurricane", "tropical_storm": "night-alt-thunderstorm", "dust": "night-fog", "smoke": "night-fog", "haze": "night-fog", "hot": "hot","cold": "snowflake-cold","blizzard": "snowflake-cold","fog": "night-fog"]
}    
   
def forecastNow = forcastDevice.currentValue("icon").split('/')
forecastNow = forecastNow[4].split('\\?')
forecastNow = forecastNow[0].split('\\,')
forecastNow = weatherIcons[forecastNow[0].toString()]
def forecastDay1Icon = forecastDay1[7].split('/') 
forecastDay1Icon = forecastDay1Icon[4].split('\\?')    
forecastDay1Icon = forecastDay1Icon[0].split('\\,')
forecastDay1Icon = weatherIcons[forecastDay1Icon[0].toString()]    
def forecastDay2Icon = forecastDay2[7].split('/')
forecastDay2Icon = forecastDay2Icon[4].split('\\?')
forecastDay2Icon = forecastDay2Icon[0].split('\\,')
forecastDay2Icon = weatherIcons[forecastDay2Icon[0].toString()]
def forecastDay3Icon = forecastDay3[7].split('/')
forecastDay3Icon = forecastDay3Icon[4].split('\\?')
forecastDay3Icon = forecastDay3Icon[0].split('\\,')
forecastDay3Icon = weatherIcons[forecastDay3Icon[0].toString()]
def forecastDay4Icon = forecastDay4[7].split('/')
forecastDay4Icon = forecastDay4Icon[4].split('\\?')
forecastDay4Icon = forecastDay4Icon[0].split('\\,')
forecastDay4Icon = weatherIcons[forecastDay4Icon[0].toString()]

def today = new Date()   
use(TimeCategory) {
date1 = (new Date())+1.day
date2 = (new Date())+2.day
date3 = (new Date())+3.day    
sdf = new SimpleDateFormat("E' - 'dd")
forecastDate1 = sdf.format(today)
forecastDate2 = sdf.format(date1)
forecastDate3 = sdf.format(date2)
forecastDate4 = sdf.format(date3)    
}
   
"""{"data": [{"icon":"${forecastNow}","cond":"${forcastDevice.currentValue("textDescription")}","temp1":"${Math.round(insideTemp.currentValue("temperature"))}","temp2":"${Math.round(outsideTemp.currentValue("temperature"))}"
,"forecastDay":"${forecastDate1}","forecastIcon":"${forecastDay1Icon}","forecastDayHigh":"${forecastDay1[2]}","forecastDayLow":"${forecastNight1[2]}"
,"forecastDay1":"${forecastDate2}","forecastIcon1":"${forecastDay2Icon}","forecastDayHigh1":"${forecastDay2[2]}","forecastDayLow1":"${forecastNight2[2]}"
,"forecastDay2":"${forecastDate3}","forecastIcon2":"${forecastDay3Icon}","forecastDayHigh2":"${forecastDay3[2]}","forecastDayLow2":"${forecastNight3[2]}"
,"forecastDay3":"${forecastDate4}","forecastIcon3":"${forecastDay4Icon}","forecastDayHigh3":"${forecastDay4[2]}","forecastDayLow3":"${forecastNight4[2]}"}]}"""
}

private def generateURL(data) {    
	if (!state?.accessToken) {
		try {
			def accessToken = createAccessToken()
			log.debug "Creating new Access Token: $state.accessToken"
		} catch (ex) {
			log.error "Enable OAuth in SmartApp IDE settings for Weather Panel"
			log.error ex
		}
    }
    def url = "${getFullLocalApiServerUrl()}/${data}?access_token=${state.accessToken}"
return "$url"
}

private def textVersion() {
    def text = "Version 2.7"
}

private def textCopyright() {
    def text = "Copyright © 2022 Sidjohn1"
}
