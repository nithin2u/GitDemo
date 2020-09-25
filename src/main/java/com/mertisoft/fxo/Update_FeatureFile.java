package com.mertisoft.fxo;

import java.io.*;

public class Update_FeatureFile {

    public static void main(String[] args) throws Exception {
        String oldContent = "",newContent="";
        FileWriter writer = null;
        File file = new File("C:\\Users\\nithin\\Desktop\\Feature\\Login.feature");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            //Read Fetaure file
            String line = reader.readLine();
            while (line != null) {
                oldContent = oldContent + line + System.lineSeparator();
                line = reader.readLine();
                if(line!=null){
                    if(line.contains("Examples:")){
                        oldContent.trim();
                        break;
                    }
                }

            }
            oldContent=oldContent+System.lineSeparator()+"Examples:"+System.lineSeparator()+"|Invoice FileName|"+System.lineSeparator()+"########";
            String folderpath = "C:\\Users\\nithin\\Desktop\\Invoice_repo\\";
            File folder = new File(folderpath);
            File[] listoffiles = folder.listFiles();
            for (File file1 : listoffiles) {
                String file_name = "|"+file1.getName()+"|"+System.lineSeparator()+"########";
                oldContent=oldContent.replaceAll("########",file_name);
            }
            newContent = oldContent.replaceAll("########", "");
            System.out.println(newContent);
            //Rewriting the feature file with newContent
            writer = new FileWriter(file);
            writer.write(newContent);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //Closing the resources
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
