package edu.nuos.detchrdevice.gui;

import edu.nuos.detchrdevice.app.Const;
import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.IAxis;
import info.monitorenter.gui.chart.IRangePolicy;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterNumber;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.util.Range;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Компоновка UI
 */
public class UI {

	private JButton btnRunMsr = null;
	private JComboBox jcmboxComPort;
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


	public void buildUI() {
		JPanel jpChart = new JPanel();
		jpChart.setLayout(new BoxLayout(jpChart, BoxLayout.Y_AXIS));

		btnRunMsr = new JButton(Const.BTN_LABEL_START);

		jcmboxComPort = new JComboBox();
		jcmboxComPort.setModel(new DefaultComboBoxModel(Const.COM_PORTS));

		buildChartUI();

		JPanel jpDir = new JPanel();
		jpDir.setLayout(new BoxLayout(jpDir, BoxLayout.X_AXIS));
		jpDir.add(btnRunMsr);
		jpDir.add(jcmboxComPort);
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

		// set a number formatter to get rid of the unnecessary ".0" prefixes for the X-Axis:
		NumberFormat format = new DecimalFormat("#");
		// Important!
		// Or it will allow more than 100 integer digits and rendering will be
		// confused.
		// See the comment for java.text.DecimalFormat#applyPattern(String)
		format.setMaximumIntegerDigits(3);
		axisY.setFormatter(new LabelFormatterNumber(format));
	}

	public void deviceNotFoundMsg() {
		JOptionPane.showMessageDialog(null, "Устройство не обнаружено. Попробуйте указать COM порт выручную.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
	}

	public void portClosedMsg(String port) {
		JOptionPane.showMessageDialog(null, "Порт " + port + " закрыт !", "Предупреждение", JOptionPane.WARNING_MESSAGE);
	}

	public JButton getBtnRunMsr() {
		return btnRunMsr;
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
