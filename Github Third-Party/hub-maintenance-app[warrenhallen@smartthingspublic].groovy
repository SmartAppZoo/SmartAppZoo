import java.util.concurrent.TimeoutException;
import groovy.json.JsonBuilder;

definition(
	name: "Hub Maintenance App",
	namespace: "smartthings-users",
	author: "rbarinov2009@gmail.com",
	description: "Reboots hub and start Z-Wave network repair on schedule",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png"
)

preferences {
	section("SmartThings account credentials") {
		input(name: "userName", type: "email", title: "ST Login", required: true);
		input(name: "password", type: "password", title: "ST Password", required: true);
	}
	section("Reboot hub at schedule") {
		input("needToRebootHub", "bool", title: "Reboot Hub?", defaultValue: false);
		input(name: "rebootCron", type: "text", title: "Cron Schedule", required: false, defaultValue: "57 00 3 * * ?");
	}	
	section("Repair Z-Wave network on schedule") {
		input("needToRepairZWave", "bool", title: "Repair Z-Wave?", defaultValue: false);
		input(name: "repairCron", type: "text", title: "Cron Schedule", required: false, defaultValue: "57 02 3 * * ?");
	}	
	section("Notification") {
		input(name: "pushNotification", type: "enum", title: "Push notification", required: yes, options: ["Yes", "No"]);
		input(name: "phoneNumber1", type: "phone", title: "Additionally, also send a text mesage to:", required: false);
		input(name: "phoneNumber2", type: "phone", title: "Additionally, also send a text mesage to:", required: false);
		input(name: "phoneNumber3", type: "phone", title: "Additionally, also send a text mesage to:", required: false);
	}
	section("Version: ${getVersion()}") {
	}
}

void installed() {
	trace("installed(): enter");

	initialize();

	comment("installed, v${getVersion()}");

	trace("installed(): exit");
}

void updated() {
	trace("updated(): enter");

	try {
		unsubscribe();
	} catch (e) {
		trace("error occurred while calling unsubscribe() method: $e");
	}
	try {
		unschedule();
	} catch (e) {
		trace("error occurred while calling unschedule() method: $e");
	}

	initialize();

	comment("updated, v${getVersion()}");

	trace("updated(): exit");
}

def uninstalled() {
	log.trace("uninstalled(): enter");

	//unsubscribe();
	try {
		unschedule();
	} catch (e) {
		trace("error occurred while calling unschedule() method: $e");
	}
	try {
		unschedule();
	} catch (e) {
		trace("error occurred while calling unschedule() method: $e");
	}
	
	def devices = getChildDevices();	
	devices?.each {
		deleteChildDevice(it.deviceNetworkId)
	}
	
	log.trace("uninstalled(): exit");
}

private void initialize() {
	trace("initialize(): enter, version: ${getVersion()}");

	state.appVersion = getVersion();
	state.cookies = [:];
	
	// 03:02:57 AM
	if (getParameter_NeedToRepairZWave()) {
		String cron = getParameter_RepairCron();
		trace("schedule for repairing network");
		schedule(cron, timerHandler1);
	}
	
	// 03:00:57 AM
	if (getParameter_NeedToRebootHub()) {
		String cron = getParameter_RebootCron();
		trace("schedule for rebooting");
		schedule(cron, timerHandler2);
	}
	
	comment("timers are scheduled");

	location.hubs.each{it->
		trace("maintaining hub: $it.id");
	}
	
	subscribe(location, null, locationHandler, [filterEvents: false]);
	//subscribe(location, null, locationHandler);
	
	trace("initialize(): exit");
}

void locationHandler(evt) {
	try {

		if (evt != null) {
		
			trace("locationHandler(): evt.name=${evt.name}, evt.value=${evt.value}");
			
			if (evt.name == "zwNwkRepair") {
				if (evt.value == "started") {
					String msg = "Z-wave network repair started ($evt.displayName)";
					comment(msg);
					notifyUser(msg);
				}
				if (evt.value == "finished") {
					String msg = "Z-wave network repair finished ($evt.displayName)";
					comment(msg);
					notifyUser(msg);
				}
			}
			
			if (evt.name == "register" && evt.value == "register") {
			   	String msg = "Hub is back online ($evt.displayName)";
				comment(msg);
			   	notifyUser(msg);
			}
			
			if (evt.name == "hub has disconnected" && evt.value == "hub has disconnected") {
			   	String msg = "Hub has disconnected ($evt.displayName)";
				comment(msg);
			   	notifyUser(msg);
			}
			
			if (evt.name == "mode") {
			   	String msg = "The system is switched to $evt.value mode";
				comment(msg);
			   	notifyUser(msg);
			}
			
		}
	
	} catch (Throwable t) {
		String msg = "locationHandler(): error occurred ($app.label): $t";
		log.error(msg);
		sendPush(msg);
	}
	
}

void timerHandler1() {

	try{
		if (state.appVersion != getVersion())
			throw new RuntimeException("timerHandler1() event triggered with mismatching app version: $state.appVersion (${getVersion()})");

		trace("timerHandler1(): enter");
	
		if (getParameter_NeedToRepairZWave()) {
			repairZWaveNetwork();
		}
	
		trace("timerHandler1(): exit");
	
	} catch (Throwable t) {
		String msg = "timerHandler1(): error occurred ($app.label, ${getVersion()}): $t";
		log.error(msg);
		sendPush(msg);

		if (t instanceof TimeoutException) {
			runIn(30, timerHandler1, [overwrite: true]);	
			sendPush("timerHandler1(): scheduled to re-try in 30 seconds ($app.label)");
		}
		
		throw t;
	}

}

void timerHandler2() {

	try{
		if (state.appVersion != getVersion())
			throw new RuntimeException("timerHandler2(): event triggered with mismatching app version: $state.appVersion (${getVersion()})");

		trace("timerHandler2(): enter");
	
		if (getParameter_NeedToRebootHub()) {
			rebootAllHubs();
		}
	
		trace("timerHandler2(): exit");
	
	} catch (Throwable t) {
		String msg = "timerHandler2(): error occurred ($app.label, ${getVersion()}): $t";
		log.error(msg);
		sendPush(msg);

		if (t instanceof TimeoutException) {
			runIn(30, timerHandler2, [overwrite: true]);	
			sendPush("timerHandler2(): scheduled to re-try in 30 seconds ($app.label)");
		}
		
		throw t;
	}

}

void repairZWaveNetwork() {
	
	if (login()) {

		try {
		
			def hubs = getHubs();
			if (hubs != null) {
				hubs.each{it->
					log.info("Starting Z-Wave network repair for hub: $it.name");
					startNetworkRepair(it.id);
				}
			}
			
		} finally {
			logout();
		}
	} // if logged in
}

void rebootAllHubs() {
	
	if (login()) {
		try {
		
			def hubs = getHubs();
			if (hubs != null) {
				hubs.each{it->
					log.info("Rebooting hub: $it.name");
					rebootHub(it.id);
				}
			}
		
		} finally {
			logout();
		}

	} // if logged in
}

private String encodeURIComponent(value) {
	return java.net.URLEncoder.encode(value);
}

private boolean login() {

	trace("****************************** login");
	boolean result = false;

	try {
		String payload = "j_username=" + encodeURIComponent(settings.userName) + "&j_password=" + encodeURIComponent(settings.password);
	
		def params = [
			uri: "https://graph.api.smartthings.com",
			path: "/j_spring_security_check",
			headers: ["Cookie": getCookie(), "Content-Type": "application/x-www-form-urlencoded"],
			body: payload
		]
		
		httpPost(params) { resp ->
			//resp.headers.each {
			//	trace("header $it.name=$it.value");
			//}
			trace("resp.status=$resp.status");
			processCookies(resp);

			// if there is no special header
			result = true;
				
			if (resp.status == 302) {
				trace("Location: " + resp.headers["Location"]);
			} else
			if (resp.status == 200) {
				//trace("resp.contentType=$resp.contentType");
				//trace("resp.data=$resp.data");
			}

		}
	
	} catch (ex) {
		log.error(ex);
	} // try

	return result;
}

private boolean logout() {

	trace("****************************** logout");
	boolean result = false;

	try {
		def headers = ["Cookie": getCookie()];
		addNoCacheHeaders(headers);
		
		def params = [
			uri: "https://graph.api.smartthings.com",
			path: "/logout/index",
			headers: headers,
			body: payload
		]
		
		httpGet(params) { resp ->
			//resp.headers.each {
			//	trace("header $it.name=$it.value");
			//}
			trace("resp.status=$resp.status");
			processCookies(resp);
		}
	
	} catch (ex) {
		log.error(ex);
	} // try

	return result;
}

private void addNoCacheHeaders(headers) {
	headers["Cache-Control"] = "no-cache, no-store, must-revalidate"; // HTTP 1.1.
	headers["Pragma"] = "no-cache"; // HTTP 1.0.
	headers["Expires"] = 0; // Proxies.
}

private def getHubs() {

	trace("****************************** get hubs");
	def result = null;

	try {
		def headers = ["Cookie": getCookie()];
		addNoCacheHeaders(headers);
		
		def params = [
			uri: "https://graph.api.smartthings.com",
			path: "/api/hubs",
			query: ["_t": now()],
			headers: headers
		]	
		httpGet(params) { resp ->
			//resp.headers.each {
			//	trace("$it.name=$it.value");
			//}
			trace("resp.status=$resp.status");
			processCookies(resp);
			
			//trace("resp.contentType=$resp.contentType");

			result = resp.data;
			trace("result=$result");
		}
	
	} catch (ex) {
		log.error(ex);
	} // try

	return result;
}

private boolean startNetworkRepair(String hubId) {

	trace("****************************** network repair, hubId=$hubId");
	boolean result = false;

	try {
		def headers = ["Cookie": getCookie()];
		addNoCacheHeaders(headers);
		
		def params = [
			uri: "https://graph.api.smartthings.com",
			path: "/hub/zwaveRepair/" + hubId,
			query: ["_t": now()],
			headers: headers
		]	
		httpGet(params) { resp ->
			//resp.headers.each {
			//	trace("$it.name=$it.value");
			//}
			trace("resp.status=$resp.status");
			processCookies(resp);
			
			//trace("resp.contentType=$resp.contentType");

			result = true;
		}
	
	} catch (ex) {
		log.error(ex);
	} // try

	return result;
}

private boolean rebootHub(String hubId) {

	trace("****************************** reboot hub, hubId=$hubId");
	boolean result = false;

	try {
		def headers = ["Cookie": getCookie()];
		addNoCacheHeaders(headers);
		
		def params = [
			uri: "https://graph.api.smartthings.com",
			path: "/hub/rebootHub/" + hubId,
			query: ["_t": now()],
			headers: headers
		]	
		httpGet(params) { resp ->
			//resp.headers.each {
			//	trace("$it.name=$it.value");
			//}
			trace("resp.status=$resp.status");
			processCookies(resp);
			
			//trace("resp.contentType=$resp.contentType");

			result = true;
		}
	
	} catch (ex) {
		log.error(ex);
	} // try

	return result;
}

private void processCookies(resp) {
	def headers = resp.getHeaders("Set-Cookie");
	headers.each{it->
		String headerValue = it.getValue();
		processSetCookie(headerValue);
	}
}

private void processSetCookie(headerValue) {
		String[] cookieParts = headerValue.split(";");
		String[] nameAndValue = cookieParts[0].split("=");
		String name = nameAndValue[0];
		String value = nameAndValue[1];

		trace("Added cookie: name=$name, value=$value");
		state.cookies[name] = value;
}

private String getCookie() {

	String result;
	
	state.cookies.each{key, value->
		if (result != null)
			result += "; ";
		else
			result = "";

		trace("Read stored cookie name=$key, value=$value");

		result += key + "=" + value;
	}

	if (result == null)
		result = "";

	trace("Current cookie value: $result");
	return result;
}

private boolean getParameter_NeedToRebootHub() {
	trace("getParameter_RebootHub(): parameter=$settings.needToRebootHub");

	if (settings.needToRebootHub != null && settings.needToRebootHub)
		return true;
	else
		return false;
}

private boolean getParameter_NeedToRepairZWave() {
	trace("getParameter_RepairZWave(): parameter=$settings.needToRepairZWave");

	if (settings.needToRepairZWave != null && settings.needToRepairZWave)
		return true;
	else
		return false;
}

private String getParameter_RebootCron() {
	trace("getParameter_RebootCron(): parameter=$settings.rebootCron");

	if (settings.rebootCron != null && settings.rebootCron != "")
		return settings.rebootCron;
	else
		return "57 00 3 * * ?";
}

private String getParameter_RepairCron() {
	trace("getParameter_RepairCron(): parameter=$settings.repairCron");

	if (settings.repairCron != null && settings.repairCron != "")
		return settings.repairCron;
	else
		return "57 02 3 * * ?";
}

private void notifyUser(msg) {

	log.info(msg);

	//String timeStamp = (new Date()).format("h:mm:ss.SSS a", location.timeZone);
	String timeStamp = (new Date()).format("h:mm:ss a", location.timeZone);
	msg = "[$timeStamp] $msg"; 
	
	if (pushNotification != null && pushNotification == "Yes") {
		sendPush(msg);
	}

	if (phoneNumber1 != null && phoneNumber1 != "") {
		sendSms(phoneNumber1, phoneNumber1 + " " + msg);
	}

	if (phoneNumber2 != null && phoneNumber2 != "") {
		sendSms(phoneNumber2, phoneNumber2 + " " + msg);
	}

	if (phoneNumber3 != null && phoneNumber3 != "") {
		sendSms(phoneNumber3, phoneNumber3 + " " + msg);
	}

}

private void comment(msg) {
	String timeStamp = (new Date()).format("dd/MM h:mm:ss a", location.timeZone);
	state.comment = "[$timeStamp] $msg"; 
	
	if (state.comments == null) {
		state.comments = [];
	}
	
	state.comments.add(state.comment);
	while (state.comments.size() > 20) {
		state.comments.remove(0);
	}
}

private void trace(String msg) {
	log.trace(msg);
}

private String getVersion() {
	return "2016.04.01.20.52";
}