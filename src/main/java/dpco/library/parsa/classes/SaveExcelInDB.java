package dpco.library.parsa.classes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.service.BaseLocalService;

/**
 * 
 * save uploaded excel into DB
 * 
 * @author p.mihandoost
 *
 */
public class SaveExcelInDB<T extends PersistedModel> {

	/**
	 * uploaded excel file
	 */
	private File excel;
	/**
	 * database service
	 */
	private BaseLocalService localService;
	/*
	 * model of query
	 */
	private Class<T> modelClass;
	/**
	 * it use to fetch row data
	 */
	private Row currentExcelRow;
	/**
	 * it uses to save data in table
	 */
	private T model;

	/**
	 * it maps from database column names to excel column index
	 */
	private Map<String, Integer> dBToExcel;

	public SaveExcelInDB(File excel, Class<T> modelClass, BaseLocalService service) {
		this.excel = excel;
		this.modelClass = modelClass;
		this.localService = service;
	}

	public void setDBToExcel(Map<String, Integer> dBToExcel) {
		this.dBToExcel = dBToExcel;
	}

	/**
	 * start saving excel to the DB
	 */
	public void start() {

		SimpleQuery<T> modelQuery = new SimpleQuery<>(modelClass, localService);
		Iterator<Row> rowIterator = getRows();

		boolean firstLineSkip = true;
		while (rowIterator.hasNext()) {
			currentExcelRow = rowIterator.next();

			if (firstLineSkip) {
				firstLineSkip = false;
				continue;
			}

			model = modelQuery.newModel();

			for (Map.Entry<String, Integer> entry : dBToExcel.entrySet()) {
				callModelSetter(entry.getKey(), entry.getValue());
			}

			modelQuery.save(model);
		}

		System.out.println("all cells saved!!!");
	}

	private void callModelSetter(String fieldName, Integer excelColIndex) {
		String fieldNameUppertCased = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

		try {
			Method method = modelClass.getMethod("set" + fieldNameUppertCased, String.class);

			method.invoke(model, getCell(currentExcelRow, excelColIndex));
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	/**
	 * get rows of Excel
	 * 
	 * @return Iterator<Row>
	 */
	private Iterator<Row> getRows() {
		FileInputStream file = null;
		try {
			file = new FileInputStream(excel);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = null;
		try {
			workbook = new XSSFWorkbook(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Get first/desired sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);

		// Iterate through each rows one by one
		Iterator<Row> rowIterator = sheet.iterator();

		return rowIterator;
	}

	/**
	 * get cell value by index
	 * 
	 * 
	 * @param row
	 * @param colIndex
	 * @return String
	 */
	private String getCell(Row row, int colIndex) {
		if (row.getCell(colIndex) == null)
			return "";

		return row.getCell(colIndex).getStringCellValue().trim();
	}
}
