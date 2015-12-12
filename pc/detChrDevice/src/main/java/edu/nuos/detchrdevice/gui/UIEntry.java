package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import edu.nuos.detchrdevice.conn.UART;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Created by max on 12.12.15.
 */
public class UIEntry implements UART.CallbackADCData{

	private static UI ui;
	private UART uart;



	public UIEntry() {
		uiInit();
		uart = new UART(this);
		uart.uartInit(ui.getJcmboxComPort().getSelectedItem().toString());
	}


	private void uiInit() {
		ui = UI.getInstance();
		ui.buildUI();
		setActions();
	}

	private void setActions() {
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
				logData.setLength(0);
			}
		});

		ui.getJcmboxComPort().setSelectedIndex(10);
		ui.getJcmboxComPort().addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
//                uartInit();
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
	 * Хранение состояния нажатия на кнопку для запуска или остановки измерений
	 */
	private boolean isStartMsr = false;

	/**
	 * Выполнение запуска или остановки измерений
	 */
	private void startStopMeasurement() {
		/* запуск */
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
		/* остановка */
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
	private StringBuilder logData = new StringBuilder();

	@Override
	public void addAdcVal(int val) {
		ui.getTrace().addPoint(xCount += deltaX, val);

		logData.append(String.valueOf(val));
		logData.append("\n");
	}


	public boolean isStartMsr() {
		return isStartMsr;
	}
}
