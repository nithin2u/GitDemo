package com.mertisoft.fxo;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class
Write_Excel {

    public static ArrayList<HashMap<String, String>> excel_data;
    public TreeMap<String, ArrayList<HashMap<String, String>>> universal_tradeData;
    String sWorkbook, sSheetName, sWorkbook_path;
    public static String excelSheetPath = System.getProperty("user.dir") + "//Excel_Folder//";
    public static String excelEXT = ".xlsx";

    Write_Excel(ArrayList<HashMap<String, String>> excel_data, String sWorkbook, String sSheetName, TreeMap<String, ArrayList<HashMap<String, String>>> universal_tradeData) {
        this.excel_data = excel_data;
        this.sWorkbook = sWorkbook;
        this.sSheetName = sSheetName;
        this.universal_tradeData=universal_tradeData;
        createexcel();
        createDatainExcel();
        createSheet_WithData();
    }

    public void createexcel() {
        sWorkbook_path = excelSheetPath + sWorkbook + excelEXT;
        XSSFWorkbook workbook = null;
        try {
            try {
                workbook = new XSSFWorkbook();
                XSSFSheet sheet = workbook.createSheet(sSheetName);
                Row initial_row = sheet.createRow(0);
                Set<String> column_names = excel_data.get(0).keySet();
                Iterator<String> it = column_names.iterator();
                for (int i = 0; i < column_names.size(); i++) {
                    if (it.hasNext()) {
                        initial_row.createCell(i).setCellValue(it.next());
                    }
                }
            } finally {
                FileOutputStream fout = new FileOutputStream(new File(sWorkbook_path));
                workbook.write(fout);
                fout.flush();
                fout.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void createDatainExcel() {
        try {
            FileInputStream file = new FileInputStream(new File(sWorkbook_path));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet(sSheetName);
            Row row = sheet.getRow(0);
            int listsize = 0;
            try {
                for (HashMap<String, String> data : excel_data) {
                    listsize++;
                    Set<String> keyset = data.keySet();
                    Iterator<String> iterator = keyset.iterator();
                    Boolean flag = false;
                    while (iterator.hasNext()) {
                        String column_name = iterator.next();
                        for (int i = 0; i < row.getLastCellNum(); i++) {
                            String eachcol_name = row.getCell(i).getStringCellValue();
                            if (column_name.equalsIgnoreCase(eachcol_name)) {
                                String actual_val = data.get(column_name);
                                if (flag == false) {
                                    sheet.createRow(listsize).createCell(i).setCellValue(actual_val);
                                    flag = true;
                                } else if (flag == true) {
                                    sheet.getRow(listsize).createCell(i).setCellValue(actual_val);
                                }

                                break;
                            }
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                FileOutputStream fout = new FileOutputStream(new File(sWorkbook_path));
                workbook.write(fout);
                fout.flush();
                fout.close();
                file.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createSheet_WithData() {
        TreeMap<String, ArrayList<HashMap<String, String>>> trade = universal_tradeData;

        try {
            FileInputStream file = new FileInputStream(new File(sWorkbook_path));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            Set<String> sheetnames=trade.keySet();
            for(String sheetname:sheetnames){
                XSSFSheet sheet = workbook.createSheet(sheetname);
                Row initial_row = sheet.createRow(0);
                Set<String> column_names = trade.get(sheetname).get(0).keySet();
                Iterator<String> it = column_names.iterator();
                for (int i = 0; i < column_names.size(); i++) {
                    if (it.hasNext()) {
                        initial_row.createCell(i).setCellValue(it.next());
                    }
                }
                Row row = sheet.getRow(0);
                int listsize = 0;
                try {
                    for (HashMap<String, String> data : trade.get(sheetname)) {
                        listsize++;
                        Set<String> keyset = data.keySet();
                        Iterator<String> iterator = keyset.iterator();
                        Boolean flag = false;
                        while (iterator.hasNext()) {
                            String column_name = iterator.next();
                            for (int i = 0; i < row.getLastCellNum(); i++) {
                                String eachcol_name = row.getCell(i).getStringCellValue();
                                if (column_name.equalsIgnoreCase(eachcol_name)) {
                                    String actual_val = data.get(column_name);
                                    if (flag == false) {
                                        sheet.createRow(listsize).createCell(i).setCellValue(actual_val);
                                        flag = true;
                                    } else if (flag == true) {
                                        sheet.getRow(listsize).createCell(i).setCellValue(actual_val);
                                    }

                                    break;
                                }
                            }

                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    FileOutputStream fout = new FileOutputStream(new File(sWorkbook_path));
                    workbook.write(fout);
                    fout.flush();
                    fout.close();
                    file.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    public static void main(String[]args){
//        ArrayList<HashMap<String,String>> list=new  ArrayList<HashMap<String,String>>();
//        HashMap<String,String> one=new HashMap<>();
//        one.put("username","nithin");
//        one.put("password","password1");
//        list.add(one);
//        HashMap<String,String> two=new HashMap<>();
//        two.put("username","rajesh");
//        two.put("password","password2");
//        list.add(two);
//        Write_Excel obj= new Write_Excel(list,"InvoiceData"+java.time.LocalDate.now(),"Data");
//        obj.createexcel();
//        obj.createDatainExcel();
//    }
}
