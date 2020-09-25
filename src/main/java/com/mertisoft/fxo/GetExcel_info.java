package com.mertisoft.fxo;


import java.io.*;

public class GetExcel_info {


    public static String getDatatxt() {
        String textfile_data = "";
        try {
            File file = new File("C:\\Users\\nithin\\Desktop\\today\\FXO_invoice_test\\excel.txt");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                textfile_data = textfile_data + line + System.lineSeparator();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textfile_data;
    }

}