package concord4pi;

import java.util.Queue;
import java.util.logging.Level;

import concord4pi.SB2000.Constants;
import concord4pi.SB2000.Message;
import concord4pi.SB2000.State;

public class CommandInterpreter {

	private State alarmSystemState;
	private Queue<Message> txQueue;
	
	public CommandInterpreter(State alarmState, Queue<Message> tx) {
		alarmSystemState = alarmState;
		txQueue = tx;
	}
	
	public String[] lookupRxCommand(String rxCommand) {
		String[] stringValue = new String[2];
		LogEngine.Log(Level.FINEST, "Searching for command [" + rxCommand + "]", this.getClass().getName());
		switch(rxCommand) {
			case "01":
				stringValue[0] = "PANEL TYPE";
				stringValue[1] = "panelType";
				break;
			
			case "02":
				stringValue[0] = "AUTOMATION EVENT LOST";
				stringValue[1] = "automationEventLost";
				break;
			
			case "03":
				stringValue[0] = "SEND EQUIPMENT LIST - ZONE DATA";
				stringValue[1] = "selZoneData";
				break;
			
			case "20":
				stringValue[0] = "CLEAR AUTOMATION DYNAMIC IMAGE";
				stringValue[1] = "noOp";
				break;

			case "21":
				stringValue[0] = "ZONE STATUS";
				stringValue[1] = "zoneStatus";
				break;
	
			case "2201":
				stringValue[0] = "ARMING LEVEL";
				stringValue[1] = "armingLevel";
				break;

			case "2202":
				stringValue[0] = "ALARM/TROUBLE";
				stringValue[1] = "alarmTrouble";
				break;

			case "2203":
				stringValue[0] = "ENTRY/EXIT DELAY";
				stringValue[1] = "entryExitDelay";
				break;

			case "2205":
				stringValue[0] = "SIREN SYNCHRONIZE";
				stringValue[1] = "noOp";
				break;
			
			case "2206":
				stringValue[0] = "SIREN GO";
				stringValue[1] = "noOp";
				break;
			
			case "2209":
				stringValue[0] = "TOUCHPAD DISPLAY";
				stringValue[1] = "touchPadDisplay";
				break;
		
			default:
				stringValue[0] = "[" + rxCommand + "]";
				stringValue[1] = "noOp";
				break;
		}
		return stringValue;
		
	}
	
	public String noOp(Message currentMessage) {
		return "Ran NoOp";
	}
	
	public String zoneStatus(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		String zone = data.substring(4,8);
		int zoneStatus = Integer.parseInt(data.substring(8,10), 16);
		
		LogEngine.Log(Level.FINE, "Setting Zone State", this.getClass().getName());
		alarmSystemState.getPartition(partition).getArea(area).getZone(zone).setZoneState(zoneStatus);
		
		return "Received ZoneStatus [" + Constants.zoneStatusFlags(zoneStatus) + "] " +
						"in Area [" + area + "] " +
						"in Partition [" + partition + "] " +
						"in Zone [" + zone + "]";		
	}
	
	public String selZoneData(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		String group = data.substring(4,6);
		String zone = data.substring(6,10);
		String zoneType = data.substring(10,12);
		int zoneStatus = Integer.parseInt(data.substring(12,14), 16);
		String zoneTextTokens[] = data.substring(14,data.length()).split("(?<=\\G.{2})");
		StringBuilder zoneTextToken = new StringBuilder();
		
		for(int i = 0; i < zoneTextTokens.length; i++) {
			zoneTextToken.append(Constants.textTokenName(zoneTextTokens[i]));
		}

		return "Received Zone Data " +
				"in Partition [" + partition + "] " +
				"in Area [" + area + "] " +
				"in Group [" + group + "] " +
				"for Zone [" + zone + "] | " +
				"ZONE TYPE: [" + zoneType + "] " + 		
				"ZONE STATUS: [" + Constants.zoneStatusFlags(zoneStatus) + "] " + 		
				"ZONE TEXT: [" + zoneTextToken.toString() + "]";		
	}
	
	public String automationEventLost(Message currentMessage) {
		txQueue.add(new Message(Constants.FullEquipmentListCommand));
		txQueue.add(new Message(Constants.DynamicDataRefreshCommand));
		return "Received Automation Event Lost - sending Dynamic Data Refresh and Full Equipment List";
	}
	
	public String touchPadDisplay(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		String messageType = data.substring(4,6);
		String textTokens[] = data.substring(6,data.length()).split("(?<=\\G.{2})");
		StringBuilder textToken = new StringBuilder();
		
		for(int i = 0; i < textTokens.length; i++) {
			textToken.append(Constants.textTokenName(textTokens[i]));
		}
		
		return
					"Received Touchpad Text Token [" + textToken + "] " +
							"in Area [" + area + "] " +
							"in Partition [" + partition + "] " +
							"as a " + Constants.messageTypeName(messageType) + " message type ";
	}
	
	public String panelType(Message currentMessage) {
		String data = currentMessage.getDataString();
		String panelType = data.substring(0,2);
		String hardwareRevision = data.substring(2, 6);
		String softwareRevision = data.substring(6, 10);
		String serialNumber = data.substring(10, 18);

		alarmSystemState.clearPanels();
		alarmSystemState.addPanel(panelType, hardwareRevision, softwareRevision, serialNumber);
		
		return "Received Panel Type [" + Constants.panelTypeName(panelType) + "] with HW Rev 57-" + hardwareRevision +
				" and SW Rev 75-" + softwareRevision + " and Serial Number [" + serialNumber + "]";
	}
	
	public String armingLevel(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		String userType = data.substring(4,6);
		String userNumber = data.substring(6,8);
		int armingLevel = Integer.parseInt(data.substring(8,10));
		
		return "Received Alarming Level " + Constants.armingLevelName(armingLevel) +
				" in Area [" + area + "] " +
				"in Partition [" + partition + "] " +
				"for " + Constants.userNumberName(userNumber) + 
				" [" + userNumber + "] (" + userType + ":" + 
				Constants.userTypeName(Integer.parseInt(userType,16)) + ")";
	}
	
	public String entryExitDelay(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		int delayFlags = Integer.parseInt(data.substring(4,6), 16);
		int delayTime = (Integer.parseInt(data.substring(6,8), 16) << 8) + Integer.parseInt(data.substring(8,10), 16);
		return "Received Entry/Exit Delay of " + delayTime +
				" in Area [" + area + "] " +
				"in Partition [" + partition + "] " +
				"with flags [" + Constants.exitEntryDelayFlags(delayFlags) + "]"; 
	}
	
	public String alarmTrouble(Message currentMessage) {
		String data = currentMessage.getDataString();
		String partition = data.substring(0,2);
		String area = data.substring(2,4);
		int sourceType = Integer.parseInt(data.substring(4,6), 16);
		String sourceNumber = data.substring(6,12);
		
		if(sourceType == Constants.SOURCETYPE_ZONE) {
			//shorten the zone number to 2 bytes if it is a zone type
			sourceNumber = sourceNumber.substring(2);
		}
		
		int generalType = Integer.parseInt(data.substring(12,14), 16);
		String generalTypeName = Constants.getGeneralType().get(generalType); 
		int specificType = Integer.parseInt(data.substring(14,16), 16);
		String specificTypeName = Constants.getSpecificType(generalType).get(specificType);

		String eventSpecificData = data.substring(16,20);
		
		return "Received Alarm/Trouble notification " + 
				"in Area [" + area + "] " +
				"in Partition [" + partition + "] " +
				"from " + Constants.getSourceTypeName(sourceType) + " " + sourceNumber +
				" of type " + generalTypeName + "." + specificTypeName + 
				"with Event Specific Data (" + Constants.getEventSpecificName(generalType, specificType) +
				") [" + eventSpecificData + "]";				
		
	}
	
}
