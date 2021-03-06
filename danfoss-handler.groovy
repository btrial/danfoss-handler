metadata {
	definition (name: "Danfoss Living Connect Radiator Thermostat LC-13 v3.01", namespace: "btrial", author: "Tom Philip") {
		capability "Relative Humidity Measurement"
		capability "Temperature Measurement"
		capability "Switch Level"
		capability "Actuator"
		capability "Sensor"
		capability "Thermostat"
		capability "Battery"
		capability "Configuration"
		capability "Switch"
        capability "Image Capture"

		command "temperatureUp"
		command "temperatureDown"
        command "take"
		
		attribute "nextHeatingSetpoint", "number"

		// raw fingerprint zw:S type:0804 mfr:0002 prod:0005 model:0004 ver:1.01 zwv:3.67 lib:06 cc:80,46,81,72,8F,75,43,86,84 ccOut:46,81,8F
		fingerprint type: "0804", mfr: "0002", prod: "0005", model: "0004", cc: "80,46,81,72,8F,75,43,86,84", ccOut:"46,81,8F"
		// 0x80 = Battery v1
		// 0x46 = Climate Control Schedule v1
		// 0x81 = Clock v1
		// 0x72 = Manufacturer Specific v1
		// 0x8F = Multi Cmd v1 (Multi Command Encapsulated)
		// 0x75 = Protection v2
		// 0x43 = Thermostat Setpoint v2
		// 0x86 = Version v1
		// 0x84 = Wake Up v2
	}

	simulator {
	}

	tiles(scale: 2) {

		multiAttributeTile(name:"richtemp", type:"thermostat", width:6, height:4) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "off", action:"on", icon: "st.thermostat.heating-cooling-off", backgroundColor:"#ffffff"
				attributeState "on", action:"off", icon: "st.thermostat.heat", backgroundColor:"#79b821"
			}

			tileAttribute("device.nextHeatingSetpoint", key: "VALUE_CONTROL") {
				attributeState "VALUE_UP", action: "temperatureUp"
				attributeState "VALUE_DOWN", action: "temperatureDown"
			}

			tileAttribute("device.thermostatOperatingState", key: "OPERATING_STATE") {
				attributeState "default", label:'${currentValue}'
				attributeState "heating", label:"heating", backgroundColor:"#ffa81e", icon:"st.thermostat.heat"
				attributeState "idle", label:"idle", backgroundColor:"#1e9cbb", icon:"st.thermostat.heating-cooling-off"
			}

			tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
				attributeState "default", label:'${currentValue}'
				attributeState "heat", label:"heat", backgroundColor:"#ffa81e", icon:"st.thermostat.heat"
				attributeState "off", label:"off", backgroundColor:"#1e9cbb", icon:"st.thermostat.heating-cooling-off"
			}

			tileAttribute("device.heatingSetpoint", key: "HEATING_SETPOINT") {
				attributeState "default", label:'${currentValue}', unit:"°C"
			}
		}

		standardTile("switcher", "device.switch", height: 2, width: 2, decoration: "flat") {
			state "off", action:"on", label: "off", icon: "st.thermostat.heating-cooling-off", backgroundColor:"#ffffff"
			state "on", action:"off", label: "on", icon: "st.thermostat.heat", backgroundColor:"#79b821"
		}
		
        standardTile("take", "device.image", canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, width: 1, height: 2) {
			state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
			state "taking", label:'Taking', action: "", icon: "st.camera.dropcam", backgroundColor: "#00A0DC"
			state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
		}
		
		valueTile("humidity", "device.humidity", width: 2, height: 2) {
            state "default", label: 'Humidity: ${currentValue}%', unit:"%"
        }
		
		valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state "default", label: 'Temperature: ${currentValue}°', unit:"dC"
        }
        
        valueTile("battery", "device.battery", width: 2, height: 2) {
            state "default", label: 'Battery: ${currentValue}%', unit:"%"
        }
        
        carouselTile("cameraDetails", "device.image", width: 6, height: 10) { }

		main "switcher"
		details(["richtemp", "humidity", "temperature", "battery", "take", "cameraDetails"])
	}

	preferences {
		input "wakeUpIntervalInMins", "number", title: "Wake Up Interval (min). Default 5mins.", description: "Wakes up and send\receives new temperature setting", range: "1..30", displayDuringSetup: true
		input "quickOffTemperature", "number", title: "Quick Off Temperature. Default 4°C.", description: "Quickly turn off the radiator to this temperature", range: "4..68", displayDuringSetup: false
        input "grafanaDashboardName", "string", title: "Grafana Dashboard Name", description: "The name of grafana's dashboard to search panel in.", displayDuringSetup: false
		input "grafanaPanelId", "number", title: "Grafana Panel Id", description: "The id of grafana's panel to show", range: "*..*", displayDuringSetup: false
	}
}

//	Event order
//	Scenario - temp is changed via the app
//	* BatteryReport (seem to get one of these everytime it wakes up! This is ood, as we don't ask for it)
//  * WakeUpNotification (the new temperature, set by the app is sent)
//  * ScheduleOverrideReport (we don't handle it)
//  * ThermostatSetpointReport (we receivce the new temp we sent when it woke up. We also set the heatingSetpoint and next one, all is aligned :))

//	Scenario - temp is changed via the app - alternative (this sometimes happens instead)
//	* ThermostatSetpointReport - resets the next temp back to the current temp. Not what we want :(
//	* BatteryReport
//	* WakeUpNotification
//	* ScheduleOverrideReport

//	Scenario - temp is changed on the radiator thermostat
//	* BatteryReport (seem to get one of these everytime it wakes up! This is ood, as we don't ask for it)
//  * ThermostatSetpointReport (we receive the temp set on the thermostat. We also set the heatingSetpoint and next one. If we set a next one, it's overwritten.)
//  * ScheduleOverrideReport (we don't handle it)
//  * WakeUpNotification (no temp is sent, as they're both the same coz of ThermostatSetpointReport)

//	Scenario - wakes up
//	* BatteryReport
//	* WakeUpNotification

//	WakeUpINterval 1800 (5mins)

//	defaultWakeUpIntervalSeconds: 300, maximumWakeUpIntervalSeconds: 1800 (30 mins),
//	minimumWakeUpIntervalSeconds: 60, wakeUpIntervalStepSeconds: 60

// All messages from the device are passed to the parse method.
// It is responsible for turning those messages into something the SmartThings platform can understand.
def parse(String description) {
	log.debug "Parsing '${description}'"

	def result = null
	//	The commands in the array are to map to versions of the command class. eg physicalgraph.zwave.commands.wakeupv1 vs physicalgraph.zwave.commands.wakeupv2
	//	If none specified, it'll use the latest version of that command class.
	def cmd = zwave.parse(description)
	if (cmd) {
		result = zwaveEvent(cmd)
		log.debug "Parsed ${cmd} to ${result.inspect()}"
	}
	else {
		log.debug "Non-parsed event: ${description}"
	}
	result
}

//	catch all unhandled events
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	return createEvent(descriptionText: "Uncaptured event for ${device.displayName}: ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.thermostatsetpointv2.ThermostatSetpointReport cmd) {
	//	Parsed ThermostatSetpointReport(precision: 2, reserved01: 0, scale: 0, scaledValue: 21.00, setpointType: 1, size: 2, value: [8, 52])

	// So we can respond with same format later, see setHeatingSetpoint()
	state.scale = cmd.scale
	state.precision = cmd.precision

	def eventList = []
	def cmdScale = cmd.scale == 1 ? "F" : "C"
	def radiatorTemperature = Double.parseDouble(convertTemperatureIfNeeded(cmd.scaledValue, cmdScale, cmd.precision)).round(1)

	def currentTemperature = currentDouble("heatingSetpoint")
	def nextTemperature = currentDouble("nextHeatingSetpoint")
	def roomTemperature = currentDouble("temperature")

	log.debug "SetpointReport. current:${currentTemperature} next:${nextTemperature} radiator:${radiatorTemperature} roomTemperature:${roomTemperature}"

	def deviceTempMap = [name: "heatingSetpoint", value: radiatorTemperature, unit: getTemperatureScale()]

	if(radiatorTemperature != currentTemperature){
		// The radiator temperature has changed. Why?
		// Is it because the app told it to change or is it coz it was done manually
		if(state.lastSentTemperature == radiatorTemperature) {
			def thermostatMode = device.currentValue("thermostatMode")
			// radiator is off
			if (thermostatMode == "off") {
				deviceTempMap.descriptionText = "Thermostat mode is off, temperature changed to ${radiatorTemperature}"
			}
			// it's the app! raise event heatingSetpoint, with desc App
			else {
				deviceTempMap.descriptionText = "Temperature changed by app to ${radiatorTemperature}"
			}
		}
		else {
			// It's manual? raise event heatingSetpoint, with desc Manual
			// And I think set the next to be the manual setting too. All aligned.
			deviceTempMap.descriptionText = "Temperature changed manually to ${radiatorTemperature}"
			state.lastSentTemperature = radiatorTemperature
			eventList << createEvent(name:"nextHeatingSetpoint", value: radiatorTemperature, unit: getTemperatureScale(), displayed: false)
			
			def offTemperature = quickOffTemperature ?: fromCelsiusToLocal(4)
			if (radiatorTemperature > offTemperature) {
				eventList << createEvent(name: "switch", value: "on", displayed: false)
				eventList << createEvent(name: "thermostatMode", value: "heat", descriptionText: "Thermostat mode is changed to heat")
				eventList << createEvent(name: "thermostatOperatingState", value: "heating", displayed: false)
			}
		}
	}

	eventList << createEvent(deviceTempMap)
	// For acting like a thermostat
	// eventList << createEvent(name: "temperature", value: radiatorTemperature, unit: getTemperatureScale(), displayed: false)
	eventList << createEvent(name: "thermostatSetpoint", value: radiatorTemperature, unit: getTemperatureScale(), displayed: false)

	if(nextTemperature == 0) {
		// initialise the nextHeatingSetpoint, on the very first time we install and get the devices temp
		state.lastSentTemperature = radiatorTemperature
		eventList << createEvent(name:"nextHeatingSetpoint", value: radiatorTemperature, unit: getTemperatureScale(), displayed: false)
	}

	eventList
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv2.WakeUpNotification cmd) {
	log.debug "Wakey wakey"

	def event = createEvent(descriptionText: "${device.displayName} woke up", displayed: false)
	def cmds = []

	// Only ask for battery if we haven't had a BatteryReport in a while
	if (!state.lastBatteryReportReceivedAt || (new Date().time) - state.lastBatteryReportReceivedAt > daysToTime(7)) {
		log.debug "Asking for battery report"
		cmds << zwave.batteryV1.batteryGet().format()
	}

	// Send the new temperature, if we haven't yet sent it
	log.debug "New temperature check. Next: ${device.currentValue("nextHeatingSetpoint")} vs Current: ${device.currentValue("heatingSetpoint")}"

	def nextHeatingSetpoint = currentDouble("nextHeatingSetpoint")
	def heatingSetpoint = currentDouble("heatingSetpoint")
	def thermostatMode = device.currentValue("thermostatMode")
	
	log.debug "Thermostat mode is ${thermostatMode}"
	if (thermostatMode == "off") {
		nextHeatingSetpoint = quickOffTemperature ?: fromCelsiusToLocal(4)
	}

	if (nextHeatingSetpoint != 0 && nextHeatingSetpoint != heatingSetpoint) {
		log.debug "Sending new temperature ${nextHeatingSetpoint}"
		state.lastSentTemperature = nextHeatingSetpoint
		cmds << setHeatingSetpointCommand(nextHeatingSetpoint).format()
		cmds << zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format()
	}

	cmds << zwave.wakeUpV1.wakeUpNoMoreInformation().format()
	[event, response(delayBetween(cmds, 1500))]
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	def map = [ name: "battery", unit: "%" ]
	if (cmd.batteryLevel == 0xFF) {  // Special value for low battery alert
		map.value = 1
		map.descriptionText = "Low Battery"
	} else {
		map.value = cmd.batteryLevel
	}
	// Store time of last battery update so we don't ask every wakeup, see WakeUpNotification handler
	state.lastBatteryReportReceivedAt = new Date().time
	createEvent(map)
}

//
//	commands (that it can handle, must implement those that match it's capabilities, so SmartApps can call these methods)
//
//	TODO: review the commands, do they have the right interface/params

def temperatureUp() {
	def nextTemp = currentDouble("nextHeatingSetpoint") + 0.5d
	// It can't handle above 28, so don't allow it go above
	// TODO: deal with Farenheit?
	if(nextTemp > fromCelsiusToLocal(28)) {
		nextTemp = fromCelsiusToLocal(28)
	}
	on()
	setHeatingSetpoint(nextTemp)
}

def temperatureDown() {
	def nextTemp = currentDouble("nextHeatingSetpoint") - 0.5d
	// It can't go below 4, so don't allow it
	if(nextTemp < fromCelsiusToLocal(4)) {
		nextTemp = fromCelsiusToLocal(4)
		off()
	}
	else {
		on()
	}
	setHeatingSetpoint(nextTemp)
}

//	Thermostat Heating Setpoint
def setHeatingSetpoint(degrees) {
	setHeatingSetpoint(degrees.toDouble())
}

def setHeatingSetpoint(Double degrees) {
	log.debug "Storing temperature for next wake ${degrees}"

	sendEvent(name:"nextHeatingSetpoint", value: degrees.round(1), unit: getTemperatureScale(), descriptionText: "Next heating setpoint is ${degrees}")
}

def setHeatingSetpointCommand(Double degrees) {
	log.trace "setHeatingSetpoint ${degrees}"
	def deviceScale = state.scale ?: 0
	def deviceScaleString = deviceScale == 1 ? "F" : "C"
	def locationScale = getTemperatureScale()
	def precision = state.precision ?: 2

	def convertedDegrees
	if (locationScale == "C" && deviceScaleString == "F") {
		convertedDegrees = celsiusToFahrenheit(degrees)
	} else if (locationScale == "F" && deviceScaleString == "C") {
		convertedDegrees = fahrenheitToCelsius(degrees)
	} else {
		convertedDegrees = degrees
	}

	zwave.thermostatSetpointV1.thermostatSetpointSet(setpointType: 1, scale: deviceScale, precision: precision, scaledValue: convertedDegrees)
}

def on() {
	sendEvent(name: "switch", value: "on", displayed: false)
	sendEvent(name: "thermostatMode", value: "heat", descriptionText: "Thermostat mode is changed to heat")
	sendEvent(name: "thermostatOperatingState", value: "heating", displayed: false)
}

def off() {
	sendEvent(name: "switch", value: "off", displayed: false)
	sendEvent(name: "thermostatMode", value: "off", descriptionText: "Thermostat mode is changed to off")
	sendEvent(name: "thermostatOperatingState", value: "idle", displayed: false)
}


def setCoolingSetpoint(temperatureValue) {
	log.debug "Executing 'real temperature in room' to $temperatureValue"
	
    if (temperatureValue != null) { 
        sendEvent(name: "temperature", value: temperatureValue, unit: "dC")
    } else {
        log.warn "Could not set measured temperature to $temperatureValue"
    }
}

def heat() {
	on()
}

def cool() {
	off()
}

def emergencyHeat() {
	on()
	setHeatingSetpoint(fromCelsiusToLocal(25))
}

def setThermostatMode(mode) {
	sendEvent(name: "switch", value: (mode == "heat") ? "on" : "off", displayed: false)
	sendEvent(name: "thermostatMode", value: (mode == "heat") ? "heat" : "off", descriptionText: "Thermostat mode is changed to ${mode}")
	sendEvent(name: "thermostatOperatingState", value: (mode == "heat") ? "heating" : "idle", displayed: false)
}

def fanOn() {
}

def fanAuto() {
}

def fanCirculate() {
}

def setThermostatFanMode(mode) {
}

def auto() {
}

def installed() {
	log.debug "installed"
	delayBetween([
		// Not sure if this is needed :/
		zwave.configurationV1.configurationSet(parameterNumber:1, size:2, scaledConfigurationValue:100).format(),
		// Get it's configured info, like it's scale (Celsius, Farenheit) Precicsion
		// 1 = SETPOINT_TYPE_HEATING_1
		zwave.thermostatSetpointV1.thermostatSetpointGet(setpointType: 1).format(),
		zwave.associationV1.associationSet(groupingIdentifier:1, nodeId:[zwaveHubNodeId]).format(),
		// Set it's time/clock. Do we need to do this periodically, like the battery check?
		currentTimeCommand(),
		// Make sure sleepy battery-powered sensor sends its
		// WakeUpNotifications to the hub every 5 mins intially
		zwave.wakeUpV1.wakeUpIntervalSet(seconds:300, nodeid:zwaveHubNodeId).format()
	], 1000)
}

def updated() {
	log.debug("updated")
	response(configure())
}

def configure() {
	log.debug("configure")
	def wakeUpEvery = (wakeUpIntervalInMins ?: 5) * 60
	[
		zwave.wakeUpV1.wakeUpIntervalSet(seconds:wakeUpEvery, nodeid:zwaveHubNodeId).format()
	]
}

private setClock() {
	// set the clock once a week
	def now = new Date().time
	if (!state.lastClockSet || now - state.lastClockSet > daysToTime(7)) {
		state.lastClockSet = now
		currentTimeCommand()
	}
	else {
		null
	}
}

private daysToTime(days) {
	days*24*60*60*1000
}

private fromCelsiusToLocal(Double degrees) {
	if(getTemperatureScale() == "F") {
		return celsiusToFahrenheit(degrees)
	}
	else {
		return degrees
	}
}

private currentTimeCommand() {
	def nowCalendar = Calendar.getInstance(location.timeZone)
	log.debug "Setting clock to ${nowCalendar.getTime().format("dd-MM-yyyy HH:mm z", location.timeZone)}"
	def weekday = nowCalendar.get(Calendar.DAY_OF_WEEK) - 1
	if (weekday == 0) {
		weekday = 7
	}
	zwave.clockV1.clockSet(hour: nowCalendar.get(Calendar.HOUR_OF_DAY), minute: nowCalendar.get(Calendar.MINUTE), weekday: weekday).format()
}

private currentDouble(attributeName) {
	if(device.currentValue(attributeName)) {
		return device.currentValue(attributeName).doubleValue()
	}
	else {
		return 0d
	}
}

def take() {

	if (grafanaDashboardName != null && grafanaPanelId != null) {

		String grafanaDashboardNameStr = grafanaDashboardName.toLowerCase().replace(" ", "-");
		Date previousDay = new Date();
		long beginOfDayTime = beginOfDay(previousDay).getTime();
		long endOfDayTime = endOfDay(previousDay).getTime();

		def params = [
			uri: "http://your.domain.com:3000/render/dashboard-solo/db/$grafanaDashboardNameStr?orgId=1&panelId=$grafanaPanelId&from=$beginOfDayTime&to=$endOfDayTime&width=1000&height=500&tz=UTC%2B01%3A00",
			headers: [
				"Authorization" : "Bearer yourToken"
			]
		]

		log.debug "Taking picture of $params"
		
		try {
			httpGet(params) { response ->
				// we expect a content type of "image/png" from the third party in this case
				
				log.debug "Response status: $response.status, headers: response.headers.'Content-Type' "
				
				if (response.status == 200 && response.headers.'Content-Type'.contains("image/png")) {
					def imageBytes = response.data
					if (imageBytes) {
						def name = getImageName()
						try {
							storeImage(name, imageBytes)
						} catch (e) {
							log.error "Error storing image ${name}: ${e}"
						}

					}
				} else {
					log.error "Image response not successful or not a png response"
				}
			}
		} catch (err) {
			log.debug "Error making request: $err"
		}
	}
}

public static Date beginOfDay(Date date) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Warsaw"));
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    return cal.getTime();
}

public static Date endOfDay(Date date) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 23);
    cal.set(Calendar.MINUTE, 59);
    cal.set(Calendar.SECOND, 59);
    cal.set(Calendar.MILLISECOND, 999);

    return cal.getTime();
}

def getImageName() {
    return java.util.UUID.randomUUID().toString().replaceAll('-','')
}


private Integer boundInt(Number value, IntRange theRange) {
    value = Math.max(theRange.getFrom(), Math.min(theRange.getTo(), value))
    return value.toInteger()
}

def setLevel(humidityValue) {
	
	log.debug "Executing 'setHumidityPercent' to $humidityValue"
	
    if (humidityValue != null) { 
        Integer hum = boundInt(humidityValue, (0..100))
        if (hum != humidityValue) {
            log.warn "Corrrected humidity value to $hum"
            humidityValue = hum
        }
        sendEvent(name: "humidity", value: humidityValue, unit: "%")
    } else {
        log.warn "Could not set measured huimidity to $humidityValue%"
    }
}