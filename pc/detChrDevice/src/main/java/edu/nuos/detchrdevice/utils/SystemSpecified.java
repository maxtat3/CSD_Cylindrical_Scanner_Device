package edu.nuos.detchrdevice.utils;


/**
 * Created by max on 14.12.15.
 */
public class SystemSpecified {

	public void setUILookAndFeel() {

	}

	/**
	 * Возможные типы ОС
	 */
	private enum OSType {
		WINDOWS,
		UNIX
	}

	/**
	 * Определение типа ОС на которой работает это приложение
	 * @return тип ОС
	 */
	private OSType getOS() {
		return OSType.WINDOWS;
	}

	/**
	 * Получение абсолютного пути к каталогу ползователя
	 * @return
	 */
	private String getUseHomeDir() {
		return null;
	}




}
