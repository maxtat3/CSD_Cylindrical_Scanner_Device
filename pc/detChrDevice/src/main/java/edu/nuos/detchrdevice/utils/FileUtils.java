package edu.nuos.detchrdevice.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Утилитарный класс для работы с файлами
 */
public class FileUtils {

	public static void writeToCSV(StringBuilder data) {
		try {
			FileWriter file = new FileWriter(getCurrentDate() + ".csv");
			String strData = String.valueOf(data);
			file.write(strData);
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static String getCurrentDate(){
		Date date = new Date();
		SimpleDateFormat formatDate = new SimpleDateFormat("dd_MM_YYYY__HH_mm_ss");
		return formatDate.format(date);
	}
}
