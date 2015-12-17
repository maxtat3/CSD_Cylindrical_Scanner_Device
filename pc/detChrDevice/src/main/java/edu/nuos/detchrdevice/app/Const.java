package edu.nuos.detchrdevice.app;

import java.awt.*;

/**
 * Константы приложения
 */
public class Const {

	/**
	 * Имена com портов, по одному из которых можно выполнить подключение к установке
	 */
	public static final String[] COM_PORTS = {
			"COM1", "COM2", "COM3", "COM114", "COM5",
			"COM6", "COM7", "COM8", "COM9", "COM10",
			"/dev/ttyACM1", "/dev/ttyACM2", "/dev/ttyACM3", "/dev/ttyACM0",
			"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", "/dev/ttyUSB3", "COM4"
	};

	/**
	 * Размер буфера для отображения количества точек на одном экране графика.
	 * При превышении этого количества, функция на графие начинает смещаться влево, таким образом
	 * чтобы на графике всегда помещалось указанное количество точек.
	 */
	public static final int MAX_AMOUNT_POINTS_ON_CHART = 1000;

	public static final String BTN_LABEL_START = "start";

	public static final String BTN_LABEL_STOP = "stop";
	/**
	 * Команда запуска измерений;
	 * Последовательность действия команды PC -> MCU
	 */
	public static final String [] CMD_START_MSR = {"a", "q", "l"};

	/**
	 * Команда остановки измерений;
	 * Последовательность действия команды PC -> MCU
	 */
	public static final String [] CMD_STOP_MSR = {"a", "b", "k"};

	public static final String[] REQUEST_INIT_DEVICE = {"a", "g", "d"};

	public static final char[] RESPONSE_INIT_DEVICE = {'g', 'h', 'y'};

	public static final int ADC_SAMPLE_RATE_IN_BITS = 255;
	public static final int Y_AXIS_MIN_VAL = 0;
	public static final int Y_AXIS_MAX_VAL = 400;

	public static final Color COLOR_CHART_BACKGROUND = Color.BLACK;
	public static final Color COLOR_CHART_MAIN = new Color(62, 95, 230);
	public static final Color COLOR_CHART_AXIS = new Color(87, 87, 87);
	public static final Color COLOR_CHART_AXIS_TITLE = new Color(26, 165, 165);

	public static final String TITLE_X_AXIS = "Длина образца (мм)";
	public static final String TITLE_Y_AXIS = "Напряжение (мВ)";
}
