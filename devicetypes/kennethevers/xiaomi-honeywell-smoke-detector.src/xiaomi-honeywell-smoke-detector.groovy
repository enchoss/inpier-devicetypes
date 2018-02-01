/**
 *  Xiaomi Honeyweel Zigbee Smoke Detector. BETA version in very basic code that just reads SMOKE or CLEAR states at the moment
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
 *  2017/09/11 -- Inpier added Battery Level and Smoke Alarm Last Tested. Note both these are best guess from live logging data received.
 *  			  PDF Manual can be found here - http://files.xiaomi-mi.com/files/MiJia_Honeywell/MiJia_Honeywell_Smoke_Detector_EN.pdf
 *
 *
 *
 */
 
metadata {
	definition (name: "Xiaomi Smoke/Fire Detector", namespace: "XiaomiSmoke", author: "enchoss") {
		
        capability "Configuration"
        capability "Smoke Detector"
        capability "Sensor"
        capability "Battery"  // NOT WORKING #### Fixed??
        //capability "Temperature Measurement" //attributes: temperature NOT WORKING 
        capability "Refresh"
        
        attribute "lastTested", "String"
        attribute "lastCheckin", "String"
  		attribute "lastSmoke", "String"
        attribute "lastSmokeDate", "Date" 
        attribute "lastCheckinDate", "Date"
        attribute "batteryRuntime", "String" 
        
        command "resetBatteryRuntime"
        command "enrollResponse"
        command "reset"
        command "makeSmoke"
        //command "Refresh"
 
		fingerprint profileID: "0104", deviceID: "0402", inClusters: "0000,0003,0012,0500", outClusters: "0019"
        
}
 
	simulator {
 		status "clear": "on/off: 0"
        status "detected": "on/off: 1"
	}

	preferences {
           input name: "dateformat", type: "enum", title: "Set Date Format\n US (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", required: false, options:["US","UK","Other"]
		   input description: "Only change the settings below if you know what you're doing", displayDuringSetup: false, type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
	       input name: "voltsmax", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", type: "decimal", range: "2.8..3.4", defaultValue: 3, required: false
	       input name: "voltsmin", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", type: "decimal", range: "2..2.7", defaultValue: 2.5, required: false
}
 
	tiles(scale: 2) {
		multiAttributeTile(name:"smoke", type: "generic", width: 6, height: 4) {
			tileAttribute ("device.smoke", key: "PRIMARY_CONTROL") {
           		attributeState("clear", label:'CLEAR', icon:"st.alarm.smoke.clear", backgroundColor:"#00a0dc")
            	attributeState("detected", label:'SMOKE', icon:"st.alarm.smoke.smoke", backgroundColor:"#e86d13")   
 			}
             tileAttribute("device.lastSmoke", key: "SECONDARY_CONTROL") {
                attributeState("default", label:'Last smoke detected: ${currentValue}')
            }
		}
        
          valueTile("battery", "device.battery", decoration:"flat", inactiveLabel: false, width: 2, height: 2) {
            state "default", label:'${currentValue}%', unit:"%",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }
        
        //standardTile("icon", "", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            //state "default", label:'Last Tested', icon:"st.alarm.smoke.test"
       // }
         valueTile("lastTested", "device.lastTested", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Last tested: ${currentValue}'//,backgroundColor:"#44b621"
            //state "notTested", label:'Not Tested since: ${currentValue}'//, backgroundColor:"#ff3300"
        } 
         valueTile("lastcheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
            state "default", label:'Last Checkin:\n${currentValue}'
        }
         standardTile("reset", "device.reset", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "default", action:"reset", label:'Reset Smoke', icon:"st.alarm.smoke.clear"
        }
        
         standardTile("makeSmoke", "device.makeSmoke", inactiveLabel: false, decoration:"flat", width: 2, height: 2) {
            state "default", action:"makeSmoke", label:'Make Smoke', icon:"st.alarm.smoke.smoke"
        }
        valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
           state "batteryRuntime", label:'Battery Changed (tap to reset):\n ${currentValue}', unit:"", action:"resetBatteryRuntime"
         }  
        //standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		//	state "default", action:"configuration.configure", icon:"st.secondary.configure"
		//} 		
       // standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			//state "default", action:"refresh.refresh", icon:"st.secondary.refresh"
		//} 
        standardTile("refresh", "device.smoke", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
		main (["smoke"])
		details(["smoke", "battery","makeSmoke", "reset", "lastcheckin", "batteryRuntime", "refresh"])//, "icon", "lastTested"
	}
}
 

def parse(String description) {
	log.debug "description: $description"
    
    def now = formatDate()
    def nowDate = new Date(now).getTime()
    sendEvent(name: "lastCheckin", value: now)
    sendEvent(name: "lastCheckinDate", value: nowDate, displayed: false) 
    
	Map map = [:]
      if (description?.startsWith('on/off: ')) 
    {
        map = parseCustomMessage(description) 
        sendEvent(name: "lastSmoke", value: now, displayed: false)
        sendEvent(name: "lastSmokeDate", value: nowDate, displayed: false) 
    }
	else if (description?.startsWith('catchall:')) {
		map = parseCatchAllMessage(description)
	}
	else if (description?.startsWith('read attr -')) {
		map = parseReportAttributeMessage(description)
	}
    else if (description?.startsWith('zone status')) {
    	map = parseIasMessage(description)
         if (map.value == "detected") {
            sendEvent(name: "lastSmoke", value: now, displayed: false)
            sendEvent(name: "lastSmokeDate", value: nowDate, displayed: false)
        }
    }
 
	log.debug "Parse returned $map"
	def result = map ? createEvent(map) : null
    
    if (description?.startsWith('enroll request')) {
    	List cmds = enrollResponse()
        log.debug "enroll response: ${cmds}"
        result = cmds?.collect { new physicalgraph.device.HubAction(it) }
    }
    return result
}
            
private Map parseCatchAllMessage(String description) {
    def i
    Map resultMap = [:]
    def cluster = zigbee.parse(description)
    log.debug cluster
    if (cluster) {
        switch(cluster.clusterId)
        {
            case 0x0000:
            def MsgLength = cluster.data.size();

            // Original Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
            if ((cluster.data.get(0) == 0x02) && (cluster.data.get(1) == 0xFF))
            {
                for (i = 0; i < (MsgLength-3); i++)
                {
                    if (cluster.data.get(i) == 0x21) // check the data ID and data type
                    {
                        // next two bytes are the battery voltage.
                        resultMap = getBatteryResult((cluster.data.get(i+2)<<8) + cluster.data.get(i+1))
                        break
                    }
                }
            }
            else if ((cluster.data.get(0) == 0x01) && (cluster.data.get(1) == 0xFF))
            {
                for (i = 0; i < (MsgLength-3); i++)
                {
                    if ((cluster.data.get(i) == 0x01) && (cluster.data.get(i+1) == 0x21))  // check the data ID and data type
                    {
                        // next two bytes are the battery voltage.
                        resultMap = getBatteryResult((cluster.data.get(i+3)<<8) + cluster.data.get(i+2))
                        break
                    }
                }
            }
            break
        }
    }
    return resultMap
}


private boolean shouldProcessMessage(cluster) {
    // 0x0B is default response indicating message got through
    // 0x07 is bind message
    boolean ignoredMessage = cluster.profileId != 0x0104 || 
        cluster.command == 0x0B ||
        cluster.command == 0x07 ||
        (cluster.data.size() > 0 && cluster.data.first() == 0x3e)
    return !ignoredMessage
}

 
private Map parseReportAttributeMessage(String description) {
	Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
	}
	log.debug "Desc Map: $descMap"
}
 
private Map parseIasMessage(String description) {
    List parsedMsg = description.split(' ')
    String msgCode = parsedMsg[2]
    
    Map resultMap = [:]
    switch(msgCode) {
        case '0x0000': // Clear 
        	resultMap = getSmokeResult('clear')
            break
            
        case '0x0001': // Smoke 
        	resultMap = getSmokeResult('detected')
            break
        case '0x0002': // Test 
        	resultMap = getTestedResult('tested')
            //log.debug "TESTED"
            break
        case 'default':
        log.debug "IAS Message default $msgCode"
        break
    }
    return resultMap
}

private Map parseCustomMessage(String description) {
    def result
    if (description?.startsWith('on/off: ')) {
        if (description == 'on/off: 0')         //smoke clear
            result = getSmokeResult("clear")
        else if (description == 'on/off: 1')     //smoke detected
            result = getSmokeResult("detected")
        return result
    }
}

private Map getSmokeResult(value) {
	log.debug 'Smoke Status' 
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} is ${value == 'detected' ? 'detected' : 'clear'}"
	return [
		name: 'smoke',
		value: value,
		descriptionText: descriptionText
	]
}


private Map getTestedResult(value) {
	def now = formatDate(true)
    log.debug 'Test Status' 
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} is ${value = 'tested'}"
	return [
		name: 'lastTested',
		value: now,
		descriptionText: descriptionText
	]
}

def resetBatteryRuntime() {
    def now = formatDate(true)   
    sendEvent(name: "batteryRuntime", value: now)
}

def installed() {
    state.battery = 0
    checkIntervalEvent("installed");
}

def updated() {
    checkIntervalEvent("updated");
}


def reset() {
    sendEvent(name:"smoke", value:"clear")
}


def makeSmoke() {
    def now = formatDate()
    def nowDate = new Date(now).getTime()
    sendEvent(name:"smoke", value:"detected")
    sendEvent(name: "lastSmoke", value: now, displayed: false)
    sendEvent(name: "lastSmokeDate", value: nowDate, displayed: false)
}


private Map getBatteryResult(rawValue) {
	def rawVolts = rawValue / 1000
	def minVolts
    def maxVolts

    if(voltsmin == null || voltsmin == "")
    	minVolts = 2.5
    else
   	minVolts = voltsmin
    
    if(voltsmax == null || voltsmax == "")
    	maxVolts = 3.0
    else
	maxVolts = voltsmax
    
    def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
    def roundedPct = Math.min(100, Math.round(pct * 100))

    def result = [
        name: 'battery',
        value: roundedPct,
        unit: "%",
        isStateChange:true,
        descriptionText : "${device.displayName} raw battery is ${rawVolts}v"
    ]

    log.debug "${device.displayName}: ${result}"
    return result
}

def configure(){ 

	log.debug "${device.displayName}: configuring"
    state.battery = 0
    checkIntervalEvent("configure");
}

def enrollResponse() {
	log.debug "Sending enroll response"
    [	
    // DOESNT WORK PROPERLY NEEDS TESTING AND ADJUSTING	
	"raw 0x500 {01 23 00 00 00}", "delay 200",
        "send 0x${device.deviceNetworkId} 1 ${endpointId}"
        
    ]
}
private hex(value) {
	new BigInteger(Math.round(value).toString()).toString(16)
}

private String swapEndianHex(String hex) {
    reverseArray(hex.decodeHex()).encodeHex()
}

private byte[] reverseArray(byte[] array) {
    int i = 0;
    int j = array.length - 1;
    byte tmp;
    while (j > i) {
        tmp = array[j];
        array[j] = array[i];
        array[i] = tmp;
        j--;
        i++;
    }
    return array
}

private checkIntervalEvent(text) {
    // Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
    log.debug "${device.displayName}: Configured health checkInterval when ${text}()"
    sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
    def correctedTimezone = ""

    if (!(location.timeZone)) {
        correctedTimezone = TimeZone.getTimeZone("GMT")
        log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
        sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
    } 
    else {
        correctedTimezone = location.timeZone
    }
    if (dateformat == "US" || dateformat == "" || dateformat == null) {
        if (batteryReset)
            return new Date().format("MMM dd yyyy", correctedTimezone)
        else
            return new Date().format("EEE MMM dd yyyy h:mm:ss a", correctedTimezone)
    }
    else if (dateformat == "UK") {
        if (batteryReset)
            return new Date().format("dd MMM yyyy", correctedTimezone)
        else
            return new Date().format("EEE dd MMM yyyy h:mm:ss a", correctedTimezone)
        }
    else {
        if (batteryReset)
            return new Date().format("yyyy MMM dd", correctedTimezone)
        else
            return new Date().format("EEE yyyy MMM dd HH:mm:ss", correctedTimezone)
    }
}

def refresh() {
   log.debug "${device.displayName}: refresh"
    return zigbee.readAttribute(0x0006, 0x0000) +
        zigbee.readAttribute(0x0008, 0x0000) +
        zigbee.configureReporting(0x0006, 0x0000, 0x10, 0, 600, null) +
        zigbee.configureReporting(0x0008, 0x0000, 0x20, 1, 3600, 0x01)
}
