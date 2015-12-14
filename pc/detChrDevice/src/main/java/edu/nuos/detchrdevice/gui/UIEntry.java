package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.conn.UART;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Управление и обработка действий UI
 */
public class UIEntry implements UART.CallbackADCData{

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
		uart.uartInit(ui.getJcmboxComPort().getSelectedItem().toString());
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

		ui.getBtnClear().addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ui.getJtaLogDataADC().setText("");
				adcDataBuffer.setLength(0);
			}
		});

		ui.getJcmboxComPort().setSelectedIndex(10);
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
