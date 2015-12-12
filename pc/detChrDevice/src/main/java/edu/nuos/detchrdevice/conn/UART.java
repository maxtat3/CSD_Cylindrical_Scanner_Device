package edu.nuos.detchrdevice.conn;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.gui.UIEntry;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import javax.swing.*;

/**
 * Created by max on 12.12.15.
 */
public class UART {

	private CallbackADCData callbackADCData;

	private SerialPort serialPort;

	private int successMcuCount = 0;

	private UIEntry uiEntry;

	private int[] receiveDataArr;



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

	private class PortReader implements SerialPortEventListener {
		@Override
		public void serialEvent(SerialPortEvent event) {
			synchronized(event){
				if(event.isRXCHAR() && event.getEventValue() > 0){
					if (uiEntry.isStartMsr()) {
						try {
							receiveDataArr = serialPort.readIntArray();
							if (receiveDataArr != null) {
								if (receiveDataArr[0] == Const.mcuComm[successMcuCount]) {
									successMcuCount++;
								}
								if (successMcuCount == 2) {
									successMcuCount = 0;
									System.out.println("command ok !");
								}
								callbackADCData.addAdcVal(receiveDataArr[0]);
							}
						}
						catch (SerialPortException ex) {
							System.out.println(ex);
							System.out.println("error - public void serialEvent(SerialPortEvent event)");
						}
					}
				}
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
}
