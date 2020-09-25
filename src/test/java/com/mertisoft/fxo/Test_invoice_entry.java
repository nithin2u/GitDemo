package com.mertisoft.fxo;


import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class Test_invoice_entry {

    public static void main(String[] args) throws Exception {
        TreeMap<String, ArrayList<HashMap<String, String>>> universal_tradeData=new TreeMap<String, ArrayList<HashMap<String, String>>>();
        String folderpath = "C:\\Users\\nithin\\Desktop\\Invoice_repo\\";
        File folder = new File(folderpath);
        File[] listoffiles = folder.listFiles();
        String old_string = "########";
        int i = 0;
        String jsfile_path = "C:\\Users\\nithin\\Desktop\\today\\FXO_invoice_test\\script.js";
        ArrayList<HashMap<String, String>> Parse_data = new ArrayList<HashMap<String, String>>();
        for (File file : listoffiles) {
            ArrayList<HashMap<String, String>> trade_details = new ArrayList<HashMap<String, String>>();
            i++;
            String file_name = file.getName();
            if(file_name.equalsIgnoreCase("mleader_BGC_136_7478_GIVEUP-STR_20180930.xlsx")){
                System.out.println();
            }
            JsFile_modification.modifyFile(jsfile_path, old_string, file_name);
            old_string = file_name;
            //tigger nodejs sript file for data extraction.
            Tigger_nodejs.extract_Data();
            // Second the extracted data from text file
            String textdata = GetExcel_info.getDatatxt();
//        Thrid transform the data with key and value
            Data_parser data = new Data_parser();
            JSONArray jsondata = data.transform_to_json(textdata);
            List<String> keys = Arrays.asList("Invoice Ref Key", "Broker", "Broker Legal Entity Name", "Invoice Diff", "Invoice Total", "Bill CCY", "Invoice Ref", "entity name", "Trade Type", "Invoice Trade Date");
            HashMap<String, String> invoice_data = data.getrequiredData(keys, jsondata, file_name);
            trade_details=data.getTrade_details();
            universal_tradeData.put(file_name,trade_details);
            if (!invoice_data.isEmpty()) {
                invoice_data.put("File Name",file_name);
                Parse_data.add(invoice_data);
                System.out.println(invoice_data);
            }
            if (i == listoffiles.length) {
                JsFile_modification.modifyFile(jsfile_path, old_string, "########");
            }
        }
        Write_Excel write= new Write_Excel(Parse_data,"InvoiceData"+java.time.LocalDate.now(),"Data",universal_tradeData);
    }

}
