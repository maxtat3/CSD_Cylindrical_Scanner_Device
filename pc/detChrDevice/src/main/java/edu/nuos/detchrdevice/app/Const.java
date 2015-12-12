package edu.nuos.detchrdevice.app;

/**
 * Created by max on 12.12.15.
 */
public class Const {

	public static final String[] COM_PORTS = {"COM1", "COM2", "COM3", "COM4",
			"COM5", "COM6", "COM7", "COM8",
			"COM9", "COM10", "/dev/ttyACM0", "/dev/ttyACM1",
			"/dev/ttyACM2", "/dev/ttyACM3", "/dev/ttyACM4", "/dev/ttyUSB0"
	};

	public static final String BTN_LABEL_START = "start";
	public static final String BTN_LABEL_STOP = "stop";

	public static final String [] startMsrCmd = {"a", "q", "l"};
	public static final String [] stopMsrCmd = {"a", "b", "k"};
	public static final char[] mcuComm = {'o', 'p'};

}
