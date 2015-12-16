package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.conn.UART;
import edu.nuos.detchrdevice.utils.FileUtils;
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
	 * Хранение состояния нажатия на кнопку для запуска или остановки измерений
	 */
	private boolean isStartMsr = false;


	public UIEntry() {
		uiInit();
		uart = new UART(this);
		// при запуске, приложение автоматчески пытается подключиться по порту, установленному по умолчанию

		if (deviceComPortAutoConnect() == null) {
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
	 * Автоподключение к ком порту устройства.
	 * Выполняеться полный перебор всех портов в ОС. Далее каждый открытый порт
	 * проверяеться на пренадлежность к устройству, т.к. к ОС может быть подключено
	 * несколько устройств.
	 */
	private String deviceComPortAutoConnect() {
		for (String port : Const.COM_PORTS) {
			boolean isPortOpen = uart.uartInit(port);
			if (isPortOpen) {
				uart.identDevice();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (uart.isDeviceFound()) {
					ui.getJcmboxComPort().setSelectedItem(port);
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
		if (ui.getBtnRunMsr().getText().equals(Const.BTN_LABEL_START)) {
			resetPreviousMsr();
			execCmd(Const.startMsrCmd);
			isStartMsr = true;
			ui.getBtnRunMsr().setText(Const.BTN_LABEL_STOP);

		// остановка
		} else {
			execCmd(Const.stopMsrCmd);
			FileUtils.writeToCSV(adcDataBuffer);
			isStartMsr = false;
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

	private void resetPreviousMsr() {
		if (ui.getTrace().getSize() != 0) {
			ui.getTrace().removeAllPoints();
			adcDataBuffer.setLength(0);
			sampleLenCount = 0.0;
		}
	}


	/**
	 * Нумерация количества точек на графиике.
	 */
	private int adcNumberCount = 1;

	/**
	 * Счетчик зачений длины образца каждому интервалу
	 * которого соответствует значения напряжения ацп
	 */

	private double sampleLenCount = 0.0;

	/**
	 * Дискретность длины в мм .
	 */
	private static final double SAMPLE_LEN_DELTA = 0.2;

	/**
	 * Буфер сохраняемых данных для последующей записи в файл.
	 */
	private StringBuilder adcDataBuffer = new StringBuilder();

	@Override
	public void addAdcVal(int val) {
		ui.getTrace().addPoint(sampleLenCount, val);

		adcDataBuffer.append(adcNumberCount++);
		adcDataBuffer.append("\t");
		adcDataBuffer.append(sampleLenCount);
		adcDataBuffer.append("\t");
		adcDataBuffer.append(String.valueOf(val));
		adcDataBuffer.append("\n");

		sampleLenCount = roundDouble(sampleLenCount + SAMPLE_LEN_DELTA);
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


	public boolean isStartMsr() {
		return isStartMsr;
	}
}
