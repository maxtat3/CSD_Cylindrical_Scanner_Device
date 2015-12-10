package edu.nuos.detchrdevice;

import info.monitorenter.gui.chart.Chart2D;
import info.monitorenter.gui.chart.IAxis;
import info.monitorenter.gui.chart.IRangePolicy;
import info.monitorenter.gui.chart.ITrace2D;
import info.monitorenter.gui.chart.axis.scalepolicy.AxisScalePolicyManualTicks;
import info.monitorenter.gui.chart.labelformatters.LabelFormatterNumber;
import info.monitorenter.gui.chart.rangepolicies.RangePolicyFixedViewport;
import info.monitorenter.gui.chart.traces.Trace2DLtd;
import info.monitorenter.util.Range;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;


/**
 * Главный класс приложения
 */
public class App {
	public static final String COM_PORT = "/dev/ttyACM0";

	public static final String BTN_LABEL_START = "start";
	public static final String BTN_LABEL_STOP = "stop";

	public static final String [] startMsrCmd = {"a", "q", "l"};
	public static final String [] stopMsrCmd = {"a", "b", "k"};

	private JTextField jtfRS232Port = new JTextField(COM_PORT);
	private JTextArea jtaLogDataADC;
	private ITrace2D trace = new Trace2DLtd(200);

	private SerialPort serialPort;
	private boolean isStartMsr = false;
	private int[] receiveDataArr;
	private StringBuilder logData = new StringBuilder();


	public App() {
		uiBuild();
		uartInit();
	}


	private void uiBuild() {
		JPanel jp = new JPanel();
//        jp.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));

		final JButton btnRunMsr = new JButton(BTN_LABEL_START);
		btnRunMsr.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
                /* запуск измерений */
				if (btnRunMsr.getText().equals(BTN_LABEL_START)) {
					try {
						for (String cmd : startMsrCmd) {
							serialPort.writeString(cmd);
						}
					} catch (SerialPortException e1) {
						e1.printStackTrace();
					}
					isStartMsr = true;
					btnRunMsr.setText(BTN_LABEL_STOP);
                /* остановка */
				} else {
					try {
						for (String cmd : stopMsrCmd) {
							serialPort.writeString(cmd);
							Thread.sleep(75);
						}
					} catch (SerialPortException | InterruptedException e1) {
						e1.printStackTrace();
					}
					isStartMsr = false;
					btnRunMsr.setText(BTN_LABEL_START);
				}
			}
		});

		JButton btnClear = new JButton("Clear");
		btnClear.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jtaLogDataADC.setText("");
				logData.setLength(0);
			}
		});

		jtaLogDataADC = new JTextArea();
		JScrollPane jscp = new JScrollPane();
		jscp.setViewportView(jtaLogDataADC);
		jscp.setPreferredSize(new Dimension(Short.MAX_VALUE, 25));

		// Create a chart:
		Chart2D chart = new Chart2D();
		// Create an ITrace:
		// Note that dynamic charts need limited amount of values!!!

		trace.setColor(Color.RED);

		// Add the trace to the chart. This has to be done before adding points (deadlock prevention):
		chart.addTrace(trace);

		chart.setFont(new Font("Veranda", Font.BOLD, 14));

		chart.setBackground(Color.LIGHT_GRAY);
		chart.setForeground(Color.BLUE);
		chart.setGridColor(Color.GREEN);

		IAxis axisX = chart.getAxisX();
		axisX.setPaintGrid(true);
		axisX.setAxisTitle(new IAxis.AxisTitle("Длина листа (мм)"));

		IAxis axisY = chart.getAxisY();
		axisY.setPaintGrid(true);
//        axisY.setMajorTickSpacing(200.0);
//        axisY.setPixelYTop(200);
//        axisY.setRange(new Range(0.0, 200.0));
		IRangePolicy rangePolicy = new RangePolicyFixedViewport(new Range(0, 255));
		chart.getAxisY().setRangePolicy(rangePolicy);

		// set a number formatter to get rid of the unnecessary ".0" prefixes for the X-Axis:
		NumberFormat format = new DecimalFormat("#");
		// Important!
		// Or it will allow more than 100 integer digits and rendering will be
		// confused.
		// See the comment for java.text.DecimalFormat#applyPattern(String)
		format.setMaximumIntegerDigits(3);
		axisY.setFormatter(new LabelFormatterNumber(format));

		jp.add(btnRunMsr);
		jp.add(btnClear);
//        jp.add(jtfRS232Port);
//        jp.add(jscp);
		jp.add(chart);

		JFrame mainFrame = new JFrame();
		mainFrame.getContentPane().setLayout(new BorderLayout());
		mainFrame.add(jp);
		mainFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		mainFrame.pack();
		mainFrame.setSize(300, 400);
		mainFrame.setVisible(true);
	}

	public void uartInit(){
		//Передаём в конструктор имя порта
		serialPort = new SerialPort(jtfRS232Port.getText());
		try {
			//Открываем порт
			serialPort.openPort();
			//Выставляем параметры
			serialPort.setParams(SerialPort.BAUDRATE_9600,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			//Включаем аппаратное управление потоком (для FT232 нжуно отключать)
//            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN |
//                                          SerialPort.FLOWCONTROL_RTSCTS_OUT);
			//Устанавливаем ивент лисенер и маску
			serialPort.addEventListener(new PortReader(), SerialPort.MASK_RXCHAR);
			//Отправляем запрос устройству
//            serialPort.writeString("a");
		}
		catch (SerialPortException ex) {
			System.out.println(ex);
			JOptionPane.showMessageDialog(null, "Port is close !", "Warning", JOptionPane.WARNING_MESSAGE);
		}
	}

	private static final char[] mcuComm = {'o', 'p'};
	private int successMcuCount = 0;
	private int xCount = 0;

	private class PortReader implements SerialPortEventListener {
		@Override
		public void serialEvent(SerialPortEvent event) {
			synchronized(event){
				if(event.isRXCHAR() && event.getEventValue() > 0){
					if (isStartMsr) {
						try {
							receiveDataArr = serialPort.readIntArray();
							if (receiveDataArr != null) {

								if (receiveDataArr[0] == mcuComm[successMcuCount]) {
									successMcuCount++;
								}
								if (successMcuCount == 2) {
									successMcuCount = 0;
									System.out.println("command ok !");
								}

								logData.append(String.valueOf(receiveDataArr[0]));
								logData.append("\n");
								jtaLogDataADC.setText(logData.toString());
								trace.addPoint(xCount++, receiveDataArr[0]);
							}
						}
						catch (SerialPortException ex) {
							System.out.println(ex);
							System.out.println("error - public void serialEvent(SerialPortEvent event)");
						}
					}
				}
			}
		}
	}
}
