package edu.nuos.detchrdevice.utils;

import edu.nuos.detchrdevice.conn.UART;
import jssc.SerialPortException;

/**
 * Created by max on 14.12.15.
 */
public class SystemSpecified {

	public void setUILookAndFeel() {

	}

	private enum OSType {
		WINDOWS,
		UNIX
	}

	private OSType getOS() {
		return OSType.WINDOWS;
	}

	private String getUseHomeDir() {
		return null;
	}




}
