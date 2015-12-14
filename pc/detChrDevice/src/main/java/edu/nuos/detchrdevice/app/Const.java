package edu.nuos.detchrdevice.app;

/**
 * Константы приложения
 */
public class Const {

	/**
	 * Имена com портов, по одному из которых можно выполнить подключение к установке
	 */
	public static final String[] COM_PORTS = {
			"COM1", "COM2", "COM3", "COM4", "COM5",
			"COM6", "COM7", "COM8", "COM9", "COM10",
			"/dev/ttyACM0", "/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3",
			"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3"
	};

	public static final String BTN_LABEL_START = "start";
	public static final String BTN_LABEL_STOP = "stop";

	/**
	 * Команда запуска измерений;
	 * Последовательность действия команды PC -> MCU
	 */
	public static final String [] startMsrCmd = {"a", "q", "l"};

	/**
	 * Команда остановки измерений;
	 * Последовательность действия команды PC -> MCU
	 */
	public static final String [] stopMsrCmd = {"a", "b", "k"};

	public static final char[] mcuComm = {'o', 'p'};

}
