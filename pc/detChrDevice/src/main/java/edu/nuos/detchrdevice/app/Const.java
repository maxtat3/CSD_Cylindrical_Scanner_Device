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
	public static final int MAX_AMOUNT_POINTS_ON_CHART = 300;

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

	/**
	 * Запрос инициализации устройства. Приложение посылает устройству эту строку.
	 */
	public static final String[] REQUEST_INIT_DEVICE = {"a", "g", "d"};

	/**
	 * Ответ инициализации устройства. Устройство возвращает эту строку если
	 * запрос иницализации {@link #REQUEST_INIT_DEVICE} прошел успешно.
	 */
	public static final char[] RESPONSE_INIT_DEVICE = {'g', 'h', 'y'};

	/**
	 * Разрядность АЦП при передаче данных.
	 * Указываеться макс. значение для соотв. разрядной сетки.
	 * Например для ацп: 8 бит -> 255 ; 10 бит -> 1023 и т.д.
	 */
	public static final int ADC_SAMPLE_RATE_IN_BITS = 255;

	/**
	 * Масштабирование оси Y - минимальное значение.
	 */
	public static final int Y_AXIS_MIN_VAL = 0;

	/**
	 * Масштабирование оси Y - максимальное значение.
	 */
	public static final int Y_AXIS_MAX_VAL = 400;

	/**
	 * Цвет фона на графике
	 */
	public static final Color COLOR_CHART_BACKGROUND = Color.BLACK;

	/**
	 * Акцентный цвет на графике. Например он может применяться для раскрашивания
	 * осей, цифоровой линейки.
	 */
	public static final Color COLOR_CHART_MAIN = new Color(62, 95, 230);

	/**
	 * Цвет сетки осей
	 */
	public static final Color COLOR_CHART_AXIS = new Color(87, 87, 87);

	/**
	 * Цвет подписи осей
	 */
	public static final Color COLOR_CHART_AXIS_TITLE = new Color(26, 165, 165);

	/**
	 * Подпись оси X
	 */
	public static final String TITLE_X_AXIS = "Длина образца (мм)";

	/**
	 * Подпись оси Y
	 */
	public static final String TITLE_Y_AXIS = "Напряжение (мВ)";
}
