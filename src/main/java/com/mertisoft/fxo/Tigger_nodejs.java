package com.mertisoft.fxo;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Tigger_nodejs {

    public static void extract_Data() {
        try {
            Process process = Runtime.getRuntime().exec(
                    "cmd /c runner.bat", null, new File("C:\\Users\\nithin\\Desktop\\today\\FXO_invoice_test\\"));
            if (process.isAlive() == true) {
                process.waitFor();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
