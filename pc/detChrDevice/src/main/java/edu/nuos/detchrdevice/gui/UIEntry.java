package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.conn.UART;
import edu.nuos.detchrdevice.utils.Recorder;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

/**
 * Управление и обработка действий UI
 */
public class UIEntry implements UART.CallbackADCData{

	private static final Logger log = Logger.getLogger(UIEntry.class.getSimpleName());
	private static UI ui;
	private UART uart;

	/**
	 * Флаг состояния нажатия на кнопку для запуска или остановки измерений.
	 * true - измерение запущено и выполняеться
	 */
	private boolean isStartMsrFlag = false;

	/**
	 * Счетчик зачений длины образца каждому интервалу
	 * которого соответствует значения напряжения ацп.
	 */

	private double sampleLenCount = 0.0;

	/**
	 * Дискретность длины в мм .
	 */
	private static final double SAMPLE_LEN_DELTA = 0.2;

	/**
	 * Экземпляр класса для записи данных в csv файл .
	 */
	private Recorder recorder = new Recorder("point number", "Длина образца. мм", "Значение напряжения, В", 300);


	public UIEntry() {
		uiInit();
		uart = new UART(this);

		// при запуске приложение автоматчески пытается найти порт устройства и подключиться к нему.
		String port = uart.deviceComPortAutoConnect();
		if (port != null) {
			ui.getJcmboxComPort().setSelectedItem(port);
		} else {
			ui.deviceNotFoundMsg();
		}
	}


	/**
	 * Создание UI
	 */
	private void uiInit() {
		ui = UI.getInstance();
		ui.buildUI();
		setUIActions();
	}

	/**
	 * Установка действий к компонентам UI
	 */
	private void setUIActions() {
		ui.getBtnRunMsr().addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				startStopMeasurement();
			}
		});

		ui.getJcmboxComPort().setSelectedIndex(0); //  устанавливаем порт по умолчанию
		ui.getJcmboxComPort().addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				String portName = ui.getJcmboxComPort().getSelectedItem().toString();
				if (uart.getSerialPort().isOpened()) {
					try {
						uart.getSerialPort().closePort();
						if (uart.uartInit(portName)) {
							uart.identDevice();
						} else {
							ui.portClosedMsg(portName);
						}
					} catch (SerialPortException e1) {
						e1.printStackTrace();
					}
				} else {
					if (!uart.uartInit(portName) ) {
						ui.portClosedMsg(portName);
					}
				}
			}
		});
	}

	/**
	 * Выполнение запуска или остановки измерений.
	 */
	private void startStopMeasurement() {
		// запуск
		if (!isStartMsrFlag) {
			resetPreviousMsr();
			execCmd(Const.CMD_START_MSR);
			isStartMsrFlag = true;
			ui.getBtnRunMsr().setText(Const.BTN_LABEL_STOP);

		// остановка
		} else {
			execCmd(Const.CMD_STOP_MSR);
			recorder.save();
			isStartMsrFlag = false;
			ui.getBtnRunMsr().setText(Const.BTN_LABEL_START);
		}
	}

	/**
	 * Запрос устройству на выполнение команды.
	 * @param cmdWord команда
	 */
	private void execCmd(String[] cmdWord) {
		try {
			for (String cmd : cmdWord) {
				uart.getSerialPort().writeString(cmd);
				Thread.sleep(75);
			}
		} catch (SerialPortException | InterruptedException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Сброс данных предыдущего измерения из графика
	 */
	private void resetPreviousMsr() {
		if (ui.getTrace().getSize() != 0) {
			ui.getTrace().removeAllPoints();
			sampleLenCount = 0.0;
		}
	}

	@Override
	public void addAdcVal(int val) {
		ui.getTrace().addPoint(sampleLenCount, val);
		sampleLenCount = roundDouble(sampleLenCount + SAMPLE_LEN_DELTA);
		recorder.add(sampleLenCount, val);
	}

	/**
	 * Округление дробного числа с ошибкой машинного округления.
	 * @param dig дробное число
	 * @return округленное дробное число
	 */
	private double roundDouble(double dig) {
		int iVal = (int) ( dig * 1000 );
		double dVal = dig * 1000;
		if ( dVal - iVal >= 0.5 ) {
			iVal += 1;
		}
		dVal = (double) iVal;
		return dVal/1000;
	}


	public boolean isStartMsrFlag() {
		return isStartMsrFlag;
	}
}
