package com.mertisoft.fxo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class Read_txt {
    boolean running = true;

    public void run() throws Exception {
        BufferedInputStream reader = new BufferedInputStream(new FileInputStream("C:\\Users\\nithin\\Desktop\\FXO_invoice_test\\excel.txt"));

        while (running) {
            if (reader.available() > 0) {
                System.out.print((char) reader.read());
            } else {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    running = false;
                }
            }
        }


    }
}
