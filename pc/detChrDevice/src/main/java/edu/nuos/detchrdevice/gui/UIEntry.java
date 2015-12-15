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

		deviceComPortAutoConnect();
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
				if (uart.getSerialPort().isOpened()) {
					try {
						uart.getSerialPort().closePort();
						uart.uartInit(ui.getJcmboxComPort().getSelectedItem().toString());
					} catch (SerialPortException e1) {
						e1.printStackTrace();
					}
				} else {
					uart.uartInit(ui.getJcmboxComPort().getSelectedItem().toString());
				}
			}
		});
	}

	/**
	 * Выполнение запуска или остановки измерений
	 */
	private void startStopMeasurement() {
		// запуск
		if (ui.getBtnRunMsr().getText().equals(Const.BTN_LABEL_START)) {
			try {
				for (String cmd : Const.startMsrCmd) {
					uart.getSerialPort().writeString(cmd);
					Thread.sleep(75);
				}
			} catch (SerialPortException | InterruptedException e1) {
				e1.printStackTrace();
			}
			isStartMsr = true;
			ui.getBtnRunMsr().setText(Const.BTN_LABEL_STOP);
		// остановка
		} else {
			try {
				for (String cmd : Const.stopMsrCmd) {
					uart.getSerialPort().writeString(cmd);
					Thread.sleep(75);
				}
				FileUtils.writeToCSV(adcDataBuffer);
			} catch (SerialPortException | InterruptedException e1) {
				e1.printStackTrace();
			}
			isStartMsr = false;
			ui.getBtnRunMsr().setText(Const.BTN_LABEL_START);
		}
	}

	private double xCount = 0.0;
	public static final double deltaX = 0.01;
	private StringBuilder adcDataBuffer = new StringBuilder();

	@Override
	public void addAdcVal(int val) {
		ui.getTrace().addPoint(xCount += deltaX, val);

		adcDataBuffer.append(String.valueOf(val));
		adcDataBuffer.append("\n");
	}


	public boolean isStartMsr() {
		return isStartMsr;
	}
}
