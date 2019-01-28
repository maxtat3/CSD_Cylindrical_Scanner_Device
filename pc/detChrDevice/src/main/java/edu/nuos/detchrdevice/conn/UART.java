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
	 * Счетчик правильно полученных символов в секции данных при инициализации устройства .
	 * Если все символы совпадают - это устройство инициализировано.
	 * Если же нет - на открытом COM порту другое устройство.
	 */
	private int responseCount = 0;

	// Принятые команда и данные от сервера.
	private int cmd, data;



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
						decoder();
					} else {
						tryInitDeviceResponse();
					}
				}
			}
		}
	}


	/**
	 * Расшифровует принятый пакет на команды и данные.
	 */
	private void decoder() {
		try {
			rxDataBuff = serialPort.readIntArray(Const.RX_BUFF_SIZE);

			cmd = (rxDataBuff[0] & 0xFF);

			switch (cmd) {
				case Const.CMD_MAKING_MSR:
					data = rxDataBuff[1] & 0xFF;
					callbackADCData.addAdcVal(data);
//					System.out.println("making msr, data=" + data);
					break;

				case Const.CMD_STOP_MSR:
					System.out.println("stop msr");
					break;

				case Const.CMD_MAKING_PARKING:
					System.out.println("Make parking ...");
					callbackADCData.blockUI(true);
					break;

				case Const.CMD_STOP_PARKING:
					System.out.println("Parking is done.");
					callbackADCData.blockUI(false);
					break;
			}

		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Выполнение попытки нахождения устройства.
	 * Запрос к устройству.
	 */
	private void tryInitDeviceRequest() {
		try {
			serialPort.writeIntArray(new int[]{Const.CMD_INIT_DEVICE, 0, 0, 0});
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Выполнение попытки нахождения устройства.
	 * Проверка ответа от устройства.
	 */
	private void tryInitDeviceResponse() {
		try {
			rxDataBuff = serialPort.readIntArray(Const.RX_BUFF_SIZE);
			boolean rightInitCmd = false;

			System.out.println("RX arr:");
			for (int rxCh : rxDataBuff) {
				System.out.println("rxCh = " + rxCh);

				if (rxCh == Const.CMD_INIT_DEVICE) {
					rightInitCmd = true;
					continue;
				}

				if (rightInitCmd && rxCh == Const.RESPONSE_INIT_DEVICE[responseCount]) {
					responseCount++;
				}
			}
		} catch (SerialPortException e) {
			e.printStackTrace();
		}

		if (responseCount == Const.RESPONSE_INIT_DEVICE.length) {
			System.out.println("Device found !");
			isDeviceFound = true;
			responseCount = 0;
		}
	}

	/**
	 * Write request package to server
	 *
	 * @param pck package writes to port
	 */
	public void writeRqPck(int[] pck) {
		try {
			serialPort.writeIntArray(pck);
		} catch (SerialPortException e) {
			e.printStackTrace();
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

		/**
		 * Блокирование элеменотов GUI при выполнении определенных действий со стороны сервера.
		 * @param makeBlock true - выполнить блокировку, false - разблокировать
		 */
		void blockUI(boolean makeBlock);
	}

	public SerialPort getSerialPort() {
		return serialPort;
	}

	public boolean isDeviceFound() {
		return isDeviceFound;
	}
}
