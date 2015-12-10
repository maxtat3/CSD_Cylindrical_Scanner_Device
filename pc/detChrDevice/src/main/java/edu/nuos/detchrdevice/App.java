package edu.nuos.detchrdevice;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * Главный класс приложения
 */
public class App {
	public static final String COM_PORT = "/dev/ttyACM0";

	public static final String BTN_LABEL_START = "start";
	public static final String BTN_LABEL_STOP = "stop";

	public static final String [] startMsrCmd = {"a", "q", "l"};
	public static final String [] stopMsrCmd = {"a", "b", "k"};

	private JTextArea jtaLogDataADC;
	private SerialPort serialPort;
	private boolean isStartMeasuring = false;
	private int[] receiveDataArr;
	private StringBuilder logData = new StringBuilder();
	private JTextField jtfRS232Port = new JTextField(COM_PORT);


	public App() {
		uiBuild();
		uartInit();
	}


	private void uiBuild() {
		JPanel jp = new JPanel();
//        jp.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		jp.setLayout(new BoxLayout(jp, BoxLayout.Y_AXIS));

		final JButton btnRunSM = new JButton(BTN_LABEL_START);
		btnRunSM.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
                /* запуск измерений */
				if (btnRunSM.getText().equals(BTN_LABEL_START)) {
					try {
//                        serialPort.writeString("a");
//                        serialPort.writeString("q");
//                        serialPort.writeString("l");
						for (String cmd : startMsrCmd) {
							serialPort.writeString(cmd);
						}
					} catch (SerialPortException e1) {
						e1.printStackTrace();
					}
					isStartMeasuring = true;
					btnRunSM.setText(BTN_LABEL_STOP);
                /* остановка */
				} else {
					try {
//                        serialPort.writeString("a");
//                        Thread.sleep(75);
//                        serialPort.writeString("b");
//                        Thread.sleep(75);
//                        serialPort.writeString("k");
						for (String cmd : stopMsrCmd) {
							serialPort.writeString(cmd);
							Thread.sleep(75);
						}
					} catch (SerialPortException | InterruptedException e1) {
						e1.printStackTrace();
					}
					isStartMeasuring = false;
					btnRunSM.setText(BTN_LABEL_START);
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
		jscp.setPreferredSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		jp.add(btnRunSM);
		jp.add(btnClear);
		jp.add(jtfRS232Port);
		jp.add(jscp);

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

	private class PortReader implements SerialPortEventListener {
		@Override
		public void serialEvent(SerialPortEvent event) {
			synchronized(event){
				if(event.isRXCHAR() && event.getEventValue() > 0){
					if (isStartMeasuring) {
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
