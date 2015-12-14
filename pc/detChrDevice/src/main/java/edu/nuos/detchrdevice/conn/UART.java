package edu.nuos.detchrdevice.conn;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.gui.UIEntry;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import javax.swing.*;

/**
 * Управление com портом
 */
public class UART {

	private CallbackADCData callbackADCData;

	private SerialPort serialPort;


	private UIEntry uiEntry;

	private int[] rxDataBuff;

	private boolean isDeviceFound = false;

	private int responseCount = 0;



	public UART(UIEntry uiEntry) {
		this.uiEntry = uiEntry;
		callbackADCData = uiEntry;
	}


	public void uartInit(String portName){
		System.out.println("portName = " + portName);
		serialPort = new SerialPort(portName);
		try {
			//Открываем порт
			serialPort.openPort();
			//Выставляем параметры
			serialPort.setParams(SerialPort.BAUDRATE_9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			//Включаем аппаратное управление потоком (для FT232 нжуно отключать)
//            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
//                                          SerialPort.FLOWCONTROL_RTSCTS_OUT);
			//Устанавливаем ивент лисенер и маску
			serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
			//Отправляем запрос устройству
//            serialPort.writeString("a");
			System.out.println("port open.");
		}
		catch (SerialPortException ex) {
			System.out.println(ex);
			JOptionPane.showMessageDialog(null, "Port is close !", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Попытка связи с устройством
	 * @return
	 */
	public boolean searchDevice() {
		tryInitDeviceRequest();
		return isDeviceFound;
	}

	private class PortReader implements SerialPortEventListener {
		@Override
		public void serialEvent(SerialPortEvent event) {
			synchronized(event){
				if(event.isRXCHAR() && event.getEventValue() > 0){
					if (isDeviceFound) {
						readMsrResult();
					} else {
						tryInitDeviceResponse();
					}
				}
			}
		}
	}

	/**
	 * Выполнение попытки нахождения устройства.
	 * Запрос к устройству.
	 */
	private void tryInitDeviceRequest() {
		try {
			for (String rq : Const.REQUEST_INIT_DEVICE) {
				serialPort.writeString(rq);
				Thread.sleep(75);
			}
		} catch (SerialPortException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Выполнение попытки нахождения устройства.
	 * Проверка ответа от устройства.
	 */
	private void tryInitDeviceResponse() {
		try {
			rxDataBuff = serialPort.readIntArray();
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
		if (rxDataBuff[0] == Const.RESPONSE_INIT_DEVICE[responseCount]) {
			responseCount++;
		}
		if (responseCount == Const.RESPONSE_INIT_DEVICE.length) {
			isDeviceFound = true;
			responseCount = 0;
		}
	}

	/**
	 * Чтение результата измерения.
	 * Выполняеться чтение атомарного ацп преобразования.
	 */
	private void readMsrResult() {
		if (uiEntry.isStartMsr()) {
			try {
				rxDataBuff = serialPort.readIntArray();
				if (rxDataBuff != null) {
					callbackADCData.addAdcVal(rxDataBuff[0]);
				}
			}
			catch (SerialPortException ex) {
				System.out.println(ex);
				System.out.println("error - public void serialEvent(SerialPortEvent event)");
			}
		}
	}

	/**
	 * This interface must be implement class with handled chart data
	 */
	public interface CallbackADCData {
		void addAdcVal(int val);
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	public boolean isDeviceFound() {
		return isDeviceFound;
	}
}
