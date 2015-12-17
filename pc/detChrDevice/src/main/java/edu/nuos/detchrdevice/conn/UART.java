package edu.nuos.detchrdevice.conn;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.gui.UIEntry;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Управление COM портом
 */
public class UART {

	private CallbackADCData callbackADCData;
	private SerialPort serialPort;
	private UIEntry uiEntry;

	/**
	 * Временный буфер при приеме данных с COM порта
	 */
	private int[] rxDataBuff;

	/**
	 * Флаг обнаружения устройства
	 * true - устройство опознано
	 */
	private boolean isDeviceFound = false;

	/**
	 * Счетчик правильно полученных символов одной команды.
	 * Команда состоит из нескольких символов.  Тут работет принцип транзакции.
	 * Если все символы одной команды совпадают - команда
	 * считаеться выполненной. Этот счетчик
	 * применен для определения команды иницализации.
	 */
	private int responseCount = 0;



	public UART(UIEntry uiEntry) {
		this.uiEntry = uiEntry;
		callbackADCData = uiEntry;
	}

	/**
	 * Автоподключение к ком порту устройства.
	 * Выполняеться полный перебор всех портов в ОС. Далее каждый открытый порт
	 * проверяеться на пренадлежность к устройству, т.к. к ОС может быть подключено
	 * несколько устройств.
	 */
	public String deviceComPortAutoConnect() {
		for (String port : Const.COM_PORTS) {
			boolean isPortOpen = uartInit(port);
			if (isPortOpen) {
				identDevice();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (isDeviceFound()) {

					return port;
				}
			}
			try {
				Thread.sleep(75);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Инициализация COM порта.
	 * @param portName имя COM порта (например com1, ttyACM0)
	 * @return true - порт открыт
	 */
	public boolean uartInit(String portName){
		System.out.println("portName = " + portName);
		serialPort = new SerialPort(portName);
		try {
			serialPort.openPort();
			serialPort.setParams(SerialPort.BAUDRATE_9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			//Включаем аппаратное управление потоком (для FT232 нжуно отключать)
//            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
//                                          SerialPort.FLOWCONTROL_RTSCTS_OUT);
			serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
			System.out.println("port open.");
			return true;
		}
		catch (SerialPortException ex) {
//			System.out.println(ex);
//			JOptionPane.showMessageDialog(null, "Port is close !", "Warning", JOptionPane.WARNING_MESSAGE);
			System.out.println("port " + portName + " close");
		}
		return false;
	}

	/**
	 * Попытка связи с устройством на заданном COM порту.
	 * @return true - устройство опознано
	 */
	public boolean identDevice() {
		tryInitDeviceRequest();
		return isDeviceFound;
	}

	/**
	 * Обработчик данных получаемых данных от COM порта.
	 * Метод {@link edu.nuos.detchrdevice.conn.UART.PortReader#serialEvent(SerialPortEvent)}
	 * срабатывает при получении данных от COM порта.
	 */
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
		if (uiEntry.isStartMsrFlag()) {
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
	 * Этот интерфейс должен реализовать тот класс, который
	 * будет обрабатвыать получаемые результаты от COM порта устройства.
	 */
	public interface CallbackADCData {
		/**
		 * Метод должен вызываться при получении с COM порта данных.
		 * @param val значение
		 */
		void addAdcVal(int val);
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	public boolean isDeviceFound() {
		return isDeviceFound;
	}
}
