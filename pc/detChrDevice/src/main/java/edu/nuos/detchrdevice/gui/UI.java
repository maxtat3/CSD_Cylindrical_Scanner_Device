package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.IAxis;
import info.monitorenter.gui.chart.IRangePolicy;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.util.Range;

import javax.swing.*;
import java.awt.*;

/**
 * Компоновка UI
 */
public class UI {

	private JButton btnRunMsr = null;
	private JComboBox jcmboxComPort;
	private JCheckBox jchbRecordData;
	private Chart2D chart;
	private ITrace2D trace;
	private JFrame mainFrame;


	private UI(){
	}

	private static class UIHolder {
		static final UI UI_INSTANCE = new UI();
	}

	public static UI getInstance() {
		return UIHolder.UI_INSTANCE;
	}

	/**
	 * Содание всего UI
	 */
	public void buildUI() {
		JPanel jpChart = new JPanel();
		jpChart.setLayout(new BoxLayout(jpChart, BoxLayout.Y_AXIS));

		btnRunMsr = new JButton(Const.BTN_LABEL_START);
		btnRunMsr.setFont(Const.DEFAULT_FONT);
		btnRunMsr.setMnemonic(Const.HOT_KEY_START_STOP);
		btnRunMsr.setToolTipText("Запуск/остановка измерений [ALT + "+Const.HOT_KEY_START_STOP+"]");

		jcmboxComPort = new JComboBox();
		jcmboxComPort.setMaximumSize(new Dimension(155, Short.MAX_VALUE));
		jcmboxComPort.setToolTipText("Выбор COM порта для подключения к устройству");
		UIManager.put(Const.PROP_TOOLTIP, Const.COLOR_TOOLTIP_BACKGR);
		jcmboxComPort.setModel(new DefaultComboBoxModel(Const.COM_PORTS));
		jcmboxComPort.setFont(Const.DEFAULT_FONT);

		jchbRecordData = new JCheckBox();
		jchbRecordData.setText("Запись данных ?");
		jchbRecordData.setToolTipText("Данные сохраняются в том же каталоге " +
			"из которого запущено приложение [ALT + "+Const.HOT_KEY_RECORD+"]");
		UIManager.put(Const.PROP_TOOLTIP, Const.COLOR_TOOLTIP_BACKGR);
		jchbRecordData.setMaximumSize(new Dimension(190, Short.MAX_VALUE));
		jchbRecordData.setFont(Const.DEFAULT_FONT);
		jchbRecordData.setMnemonic(Const.HOT_KEY_RECORD);

		buildChartUI();

		JPanel jpDir = new JPanel();
		jpDir.setLayout(new BoxLayout(jpDir, BoxLayout.X_AXIS));
		jpDir.add(btnRunMsr);
		jpDir.add(Box.createHorizontalStrut(12));
		jpDir.add(jcmboxComPort);
		jpDir.add(Box.createHorizontalStrut(12));
		jpDir.add(jchbRecordData);
		jpChart.add(chart);

		mainFrame = new JFrame();
		mainFrame.getContentPane().setLayout(new BorderLayout());
		mainFrame.add(jpDir, BorderLayout.NORTH);
		mainFrame.add(jpChart, BorderLayout.CENTER);
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.pack();
		mainFrame.setSize(800, 600);
		mainFrame.setVisible(true);
	}

	/**
	 * Построение графика
	 */
	private void buildChartUI() {
		chart = new Chart2D();

		trace = new Trace2DLtd(Const.MAX_AMOUNT_POINTS_ON_CHART);
		trace.setColor(Color.RED);

		chart.addTrace(trace);

		chart.setFont(new Font("Veranda", Font.BOLD, 14));

		chart.setBackground(Const.COLOR_CHART_BACKGROUND);
		chart.setForeground(Const.COLOR_CHART_MAIN);
		chart.setGridColor(Const.COLOR_CHART_AXIS);

		IAxis axisX = chart.getAxisX();
		axisX.setPaintGrid(true);
		axisX.setAxisTitle(new IAxis.AxisTitle(Const.TITLE_X_AXIS)); // или так axisX.getAxisTitle().setTitle("abc");
		axisX.getAxisTitle().setTitleColor(Const.COLOR_CHART_MAIN);

		IAxis axisY = chart.getAxisY();
		axisY.setPaintGrid(true);
		axisY.setAxisTitle(new IAxis.AxisTitle(Const.TITLE_Y_AXIS));
		axisY.getAxisTitle().setTitleColor(Const.COLOR_CHART_MAIN);
		IRangePolicy rangePolicy = new RangePolicyFixedViewport(new Range(Const.Y_AXIS_MIN_VAL, Const.Y_AXIS_MAX_VAL));
		chart.getAxisY().setRangePolicy(rangePolicy);
	}

	/**
	 * Вывод сообщения если устройство не найдено.
	 * Вызывается как модальное окно.
	 */
	public void deviceNotFoundMsg() {
		JOptionPane.showMessageDialog(null, "Устройство не обнаружено. Попробуйте указать COM порт выручную.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Вывод сообщения если com порт закрыт.
	 * Вызывается как модальное окно.
	 */
	public void portClosedMsg(String port) {
		JOptionPane.showMessageDialog(null, "Порт " + port + " закрыт !", "Предупреждение", JOptionPane.WARNING_MESSAGE);
	}

	public JButton getBtnRunMsr() {
		return btnRunMsr;
	}

	public JCheckBox getJchbRecordData() {
		return jchbRecordData;
	}

	public JComboBox getJcmboxComPort() {
		return jcmboxComPort;
	}

	public ITrace2D getTrace() {
		return trace;
	}

	public JFrame getMainFrame() {
		return mainFrame;
	}
}
