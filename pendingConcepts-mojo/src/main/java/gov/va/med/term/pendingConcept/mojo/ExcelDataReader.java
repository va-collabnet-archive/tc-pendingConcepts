package gov.va.med.term.pendingConcept.mojo;

import gov.va.oia.terminology.converters.sharedUtils.ConsoleUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelDataReader
{
	private XSSFWorkbook wb_;
	Sheet sheet_;

	public ExcelDataReader(File dbPath) throws IOException
	{
		ConsoleUtil.println("Opening " + dbPath + " as an Excel File");
		wb_ = new XSSFWorkbook(new FileInputStream(dbPath));
		for (int i = 0; i < wb_.getNumberOfSheets(); i++)
		{
			if (wb_.getSheetAt(i).getSheetName().toLowerCase().startsWith("pendingconcepts"))
			{
				sheet_ = wb_.getSheetAt(i);
				break;
			}
		}
		if (sheet_ == null)
		{
			throw new RuntimeException("Unable to find pending concepts sheet");
		}
	}


	public ArrayList<PendingConcept> read() throws IOException
	{
		ArrayList<PendingConcept> result = new ArrayList<>();
		for (int row = 1; row <= sheet_.getLastRowNum(); row++)
		{
			Row r = sheet_.getRow(row);
			if (r == null)
			{
				continue;
			}
			
			Double sctId = (Double)readCell(r, 0);
			String desc = (String)readCell(r, 1);
			Double parentSctId = (Double)readCell(r, 2);
			String parentDesc = (String)readCell(r, 3);
			
			if (sctId != null)
			{
				result.add(new PendingConcept(sctId.longValue(), desc, parentSctId.longValue(), parentDesc));
			}
		}
		return result;
	}
		
	/**
	 * Currently returns a String or a Double
	 * @param row
	 * @param col
	 * @return
	 */
	private Object readCell(Row row, int col)
	{
		Cell cell = row.getCell(col);
		if (cell == null)
		{
			return null;
		}
		switch (cell.getCellType())
		{
			case Cell.CELL_TYPE_BLANK:
				return null;
			case Cell.CELL_TYPE_STRING:
				return cell.getStringCellValue();
			case Cell.CELL_TYPE_NUMERIC:
				return cell.getNumericCellValue();

			default:
				throw new RuntimeException("Unhandeled cell type");
		}
	}
}
