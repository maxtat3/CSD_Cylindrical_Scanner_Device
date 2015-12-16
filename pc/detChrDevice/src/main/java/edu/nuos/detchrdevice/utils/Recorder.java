package edu.nuos.detchrdevice.utils;

/**
 * Класс для регистрации данных во временный буфер и последующей записи их файл.
 */
public class Recorder {

	private String POINT_NUMBER_COL_DEFAULT_NAME = "Номер точки";
	private String X_AXIS_DEFAULT_NAME = "x";
	private String Y_AXIS_DEFAULT_NAME = "y";
	private String pointNumberColName;
	private String xAxisName;
	private String yAxisName;

	private boolean firstAddDataFlag = true;

	/**
	 * Нумерация количества точек на графиике.
	 * Первой точке будет присвоен номер 1.
	 */
	private int pointNumber = 1;

	/**
	 * Буфер сохраняемых данных для последующей записи в файл.
	 */
	private StringBuilder dataBuffer;


	/**
	 * В конструкторе задаються имена для колонок в первой строке файла.
	 * Все значения имен могут принимать значения null. Значение буфера
	 * должно быть > 0. В этом случае имена будут выставлены по умолчанию.
	 * @param pointNumberColName колонка с нумерацией количества точек
	 * @param xAxisName колонка с данными по оси X
	 * @param yAxisName колонка с данными по оси Y
	 * @param buffCapacity размер буфера
	 */
	public Recorder(String pointNumberColName, String xAxisName, String yAxisName, int buffCapacity) {
		this.pointNumberColName = pointNumberColName;
		this.xAxisName = xAxisName;
		this.yAxisName = yAxisName;
		dataBuffer = new StringBuilder(buffCapacity);
	}

	/**
	 * ДОбавление точек во временный буфер. После добавления всех точек для записи
	 * в файл нужно вызвать метод {@link #save()} . Это csv файл который разделен
	 * на несколько колонок в каждой из который записана последовательность данных
	 * соответствующая своему предназначению, например нумерация точек, значения оси X.
	 * @param valX значение точки по по оси Х;
	 * @param valY значение точки по по оси Y;
	 */
	public void add(double valX, double valY) {
		if (firstAddDataFlag) {
			signColumnLabel();
			firstAddDataFlag = false;
		}
		dataBuffer.append(pointNumber);
		dataBuffer.append("\t");
		dataBuffer.append(valX);
		dataBuffer.append("\t");
		dataBuffer.append(valY);
		dataBuffer.append("\n");
		pointNumber ++;
	}

	/**
	 * Запись результатов в файл.
	 * После записи буфер освобождаеться автоматиччески,
	 * т.е. метод {@link #resetBuffer()} вызывать не нужно.
	 */
	public void save(){
		FileUtils.writeToCSV(dataBuffer);
		resetBuffer();
	}

	/**
	 * Сброс буфера хранящего результаты.
	 */
	public void resetBuffer(){
		if (dataBuffer.length() != 0) {
			dataBuffer.setLength(0);
			pointNumber = 1;
		}
	}

	/**
	 * Подпись колонок
	 */
	private void signColumnLabel() {
		if (pointNumberColName != null) {
			dataBuffer.append(pointNumberColName);
		} else {
			dataBuffer.append(POINT_NUMBER_COL_DEFAULT_NAME);
		}

		dataBuffer.append("\t");

		if (xAxisName != null) {
			dataBuffer.append(xAxisName);
		} else {
			dataBuffer.append(X_AXIS_DEFAULT_NAME);
		}

		dataBuffer.append("\t");

		if (yAxisName != null) {
			dataBuffer.append(yAxisName);
		} else {
			dataBuffer.append(Y_AXIS_DEFAULT_NAME);
		}

		dataBuffer.append("\n");
	}

}
