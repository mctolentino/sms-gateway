/**
 * @author Dino Martin / Maki Tolentino
 */
package com.volenday.sms;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.comm.CommPortIdentifier;
import javax.comm.CommPortOwnershipListener;
import javax.comm.PortInUseException;
import javax.comm.SerialPort;
import javax.comm.SerialPortEvent;
import javax.comm.SerialPortEventListener;
import javax.comm.UnsupportedCommOperationException;

public class SmsGateway implements ISmsGateway, SerialPortEventListener,
		CommPortOwnershipListener {
	// This COM Port must be connected with GSM modem or mobile phone
	private static final String COM_PORT = "COM3";

	private static final Logger log = Logger.getLogger("SMS_GATEWAY");
	private static final String SMS_TEMPLATE = "This message is from Blue Cross: Your Verification code is %s. Please show this message to your TPA to verify your account.";

	private static final String GATEWAY = "SMS_GATEWAY";
	private static final int TIMEOUT = 2000;
	private static final int BAUD_RATE = 115200;

	private static Enumeration<CommPortIdentifier> portIds;
	private static CommPortIdentifier portId;

	private static SerialPort serialPort;
	private static InputStream inputStream;
	private static OutputStream outputStream;
	private static SmsGateway smsGateway;

	public static ISmsGateway getInstance() {
		if (smsGateway == null) {
			smsGateway = new SmsGateway();
			smsGateway.connect();
		}
		return smsGateway;
	}

	private String constructMessage(String challengeCode) {
		return String.format(SMS_TEMPLATE, challengeCode);
	}

	@Override
	public boolean sendSms(String mobileNumber, String challenge) {
		if (init()) {
			try {
				connect();
				checkStatus();
				setToSmsMode();
				Thread.sleep(TIMEOUT);
				sendMessage(mobileNumber, constructMessage(challenge));
				Thread.sleep(TIMEOUT);
				hangup();
				disconnect();
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		System.out.println("Initialization failed.");
		return false;
	}

	@Override
	public void serialEvent(javax.comm.SerialPortEvent serialPortEvent) {
		switch (serialPortEvent.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
		case SerialPortEvent.DATA_AVAILABLE:
			byte[] readBuffer = new byte[2048];

			try {
				while (inputStream.available() > 0) {
					inputStream.read(readBuffer);
				}
				// print response message
				System.out.print(new String(readBuffer));
			} catch (IOException e) {
				// do nothing
			}

			break;
		}
	}

	@Override
	public void ownershipChange(int type) {
		switch (type) {
		case CommPortOwnershipListener.PORT_UNOWNED:
			System.out.printf("%s: PORT_UNOWNED\n", portId.getName());
			break;
		case CommPortOwnershipListener.PORT_OWNED:
			System.out.printf("%s: PORT_OWNED\n", portId.getName());
			break;
		case CommPortOwnershipListener.PORT_OWNERSHIP_REQUESTED:
			System.out.printf("%s: PORT_IN_USE\n", portId.getName());
			break;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean init() {
		portIds = CommPortIdentifier.getPortIdentifiers();

		while (portIds.hasMoreElements()) {
			portId = portIds.nextElement();

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (COM_PORT.equalsIgnoreCase(portId.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	private void connect() {
		if (portId != null) {
			if (portId.getName().equalsIgnoreCase(GATEWAY)) {
				portId.removePortOwnershipListener(this);
			}

			portId.addPortOwnershipListener(this);
			if(!portId.getCurrentOwner().equalsIgnoreCase("SMS_GATEWAY")){
				try {
					serialPort = (SerialPort) portId.open(GATEWAY, TIMEOUT);
					serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
					serialPort.setSerialPortParams(BAUD_RATE,
							SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);
				} catch (PortInUseException e) {
					e.printStackTrace();
				} catch (UnsupportedCommOperationException e) {
					e.printStackTrace();
				}	
			}
			
			try {
				inputStream = serialPort.getInputStream();
				outputStream = serialPort.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				/** These are the events we want to know about */
				serialPort.addEventListener(this);
				serialPort.notifyOnDataAvailable(true);
			} catch (TooManyListenersException e) {
				e.printStackTrace();
			}

			// Register to home network of SIM card
			send("ATZ\r\n", "Register to network.");
		} else {
			log.log(Level.INFO, "COM Port not found.");
		}
	}

	private void disconnect() {
		portId.removePortOwnershipListener(this);
	}

	private void checkStatus() {
		send("AT+CREG?\r\n", "Check Status");
	}

	private void setToSmsMode() {
		send("AT+CMGF=1\r\n", "Set to SMS Mode");
	}

	private void sendMessage(String phoneNumber, String message) {
		send("AT+CMGS=\"" + phoneNumber + "\"\r" + message + '\032',
				"Sending Message");
	}

	private void hangup() {
		send("ATH\r\n", "Hang Up");
	}

	private void send(String cmd, String desc) {
		try {
			outputStream.write(cmd.getBytes());
			inputStream = serialPort.getInputStream();
			formatStringBeforePrint("[Send cmd to serial port] " + cmd + " : "
					+ desc);
			System.out.printf("[Input Stream] %s%n", inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SmsGateway sg = new SmsGateway();
		sg.sendSms("09297700500", "test");
	}

	public void formatStringBeforePrint(String toPrint) {
		System.out.println(toPrint.replace("\r\n", " "));
	}

}