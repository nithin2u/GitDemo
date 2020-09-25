package com.mertisoft.fxo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.JsonParser;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;

public class Data_parser {
    static int json_increment = 1;
    static LinkedHashMap<String, Object> excel_data;
    public Set<String> keys;
    public Set<String> empty_keys = new HashSet<>();
    public HashSet<String> used_keys = new HashSet<>();
    List<String> filter_flag = new ArrayList<>();
    static ArrayList<String> json_keyvalues = new ArrayList<String>();
    ArrayList<HashMap<String, String>> trade_details = new ArrayList<HashMap<String, String>>();

    public LinkedHashMap<String, Object> splitToMap(String source, String entriesSeparator, String keyValueSeparator) {

        String old_key = "", old_value = "", new_value;
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        CopyOnWriteArrayList<String> entries = new CopyOnWriteArrayList<String>(Arrays.asList(source.split(entriesSeparator)));
        for (Iterator<String> itr_entry = entries.iterator(); itr_entry.hasNext(); ) {
            String entry = itr_entry.next();
//            System.out.println("Entry---------------"+entry);
            if (!entry.contains(keyValueSeparator)) {
                new_value = old_value + "," + entry.trim();
//                System.out.println("Replace Entry----key:"+old_key+"----oldvalue:"+old_value+"-----new value:"+new_value);
                map.replace(old_key, old_value, new_value);
                old_value = new_value;
            }
            try {
                if (!entry.substring(entry.indexOf("="), entry.length()).contentEquals("='") && entry.contains(keyValueSeparator)) {
                    Object[] keyValue = entry.trim().split(keyValueSeparator);
//                System.out.println("first entry----key:"+keyValue[0].toString()+"----------------value:"+keyValue[1]);
                    map.put(keyValue[0].toString(), keyValue[1]);
                    old_key = keyValue[0].toString();
                    old_value = map.get(old_key).toString();
                } else if (entry.substring(entry.indexOf("="), entry.length()).contentEquals("='")) {
                    Object[] keyValue = entry.trim().split(keyValueSeparator);
                    old_key = keyValue[0].toString();
                    empty_keys.add(old_key);
                }
            } catch (StringIndexOutOfBoundsException e) {
//                e.printStackTrace();
            } catch (ConcurrentModificationException e) {
//                e.printStackTrace();
            } finally {
                continue;
            }
        }
        excel_data = map;
        return map;

    }

    public JSONArray transform_to_json(String textvalue) {

        JSONArray jsondata = new JSONArray();
        // Split entire raw text data into key and value
        LinkedHashMap<String, Object> data = splitToMap(textvalue, ",", "=");
        // get keys of all values
        keys = data.keySet();
        Boolean merged_flag;
        String str;
        //Iterate every value using key
        for (Iterator<String> itr_key = keys.iterator(); itr_key.hasNext(); ) {
            merged_flag = false;
            String key = itr_key.next();
            if(key.equals("B20")){
                System.out.println(key);
            }
//            System.out.println(key);
//            merged_flag=getCell_MergedStatus(key);
            if (used_keys == null || !used_keys.contains(key)) {
                //get parent_rightkey and parent_downkey of the specific key
                ArrayList<String> key_pos = get_keypositions(key);
                String rightkey = "", downkey = "";
                if (key_pos.size() > 0) {
                    rightkey = key_pos.get(0);
                    downkey = key_pos.get(1);
                }
                //check if data is related to statment summary of invoice
                if(data.get(key).toString().equalsIgnoreCase("Statement Summary")){
                    str = get_DataSet("Statement Summary", key);
                    if (!str.isEmpty()) {
                        JSONObject obj = new JSONObject(str);
                        jsondata.put(obj);
                    }
                }
                //check if parent_rightkey exists
                if (keys.contains(rightkey)) {
                    //get child_rightkey and child_downkey of the specific parent_rightkey
                    ArrayList<String> key_pos_inner = get_keypositions(rightkey);
                    String rightkey_inner = key_pos_inner.get(0);
                    String downkey_inner = key_pos_inner.get(1);
                    //check if child_rightkey and child_downkey exists
                    if (keys.contains(rightkey_inner) && keys.contains(downkey_inner)) {
                        str = get_DataSet("2D_DataSet", key);
                        if (!str.isEmpty()) {
                            JSONObject obj = new JSONObject(str);
                            jsondata.put(obj);
                        }
//                       System.out.println(str);
//                        System.out.println("Its 2D array----" + key);
                        // check if only child_downkey exists
                    } else if (keys.contains(downkey_inner)) {
                        str = get_DataSet("1D_DataSet", key);
                        if (!str.isEmpty()) {
                            JSONObject obj = new JSONObject(str);
                            jsondata.put(obj);
                        }
//                        System.out.println(str);
//                        System.out.println("its 1D array----" + key);
                    }
                    // check if only parent_downkey exists
                } else if (keys.contains(downkey)) {
                    str = get_DataSet("Single_Column_DataSet", key);
                    if (!str.isEmpty()) {
                        JSONObject obj = new JSONObject(str);
                        jsondata.put(obj);
                    }
//                    System.out.println(str);
//                    System.out.println("one column data----" + key);
                    // check if its only a key
                } else {
                    str = get_DataSet("Single_Value", key);
                    if (!str.isEmpty()) {
                        JSONObject obj = new JSONObject(str);
                        jsondata.put(obj);
                    }
//                    System.out.println(str);
//                    System.out.println("single value----" + key);
                }
            }
        }
        return jsondata;
    }

    public boolean getCell_MergedStatus(String key) {
        Boolean merge_flag = false;
        try {
            FileInputStream file = new FileInputStream(new File("C:\\Users\\nithin\\Desktop\\today\\FXO_invoice_test\\mleader_BGC_136_7478_GIVEUP-STR_20180930.xlsx"));
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            XSSFSheet sheet = workbook.getSheet("Invoice");
            CellReference cr = new CellReference(key);
            CellRangeAddress range = sheet.getMergedRegion(cr.getRow());

        } catch (Exception e) {

        }
        return merge_flag;
    }

    public String get_DataSet(String dimension_type, String inital_key) {
        String ret = "";
        LinkedHashMap<Object, Object> single_dimentional_data = new LinkedHashMap<>();
        ArrayList<Object> dataset = new ArrayList<>();
        ArrayList<LinkedHashMap<Object, Object>> double_dimentional_data = new ArrayList<LinkedHashMap<Object, Object>>();
        switch (dimension_type) {

            case "Statement Summary":{
                String nextkey = inital_key, itr_nextkey = inital_key;

            }
            case "2D_DataSet": {
                String nextkey = inital_key, itr_nextkey = inital_key;
                int row_size = 0;
                while (get_keypositions(nextkey).get(1) != null && keys.contains(get_keypositions(nextkey).get(1))) {
                    row_size++;
                    nextkey = get_keypositions(nextkey).get(1);
                }
                for (int i = 1; i <= row_size; i++) {
                    LinkedHashMap<Object, Object> row_data = new LinkedHashMap<Object, Object>();
                    String attribute_nextkey = inital_key;
                    String temp_itr = null;
                    int count = 0;
                    while ((itr_nextkey != null && !itr_nextkey.isEmpty()) && (keys.contains(itr_nextkey) || (empty_keys.contains(itr_nextkey)))) {
                        used_keys.add(itr_nextkey);
                        if (count == 0) {
                            temp_itr = itr_nextkey;
                            count++;
                        }
                        try {
                            if (!excel_data.get(attribute_nextkey).toString().isEmpty() && excel_data.get(get_keypositions(itr_nextkey).get(1)).toString() != null) {
                                row_data.put(excel_data.get(attribute_nextkey), excel_data.get(get_keypositions(itr_nextkey).get(1)));
                                used_keys.add(get_keypositions(itr_nextkey).get(1));
                                used_keys.add(attribute_nextkey);
                                itr_nextkey = get_keypositions(itr_nextkey).get(0);
                                attribute_nextkey = get_keypositions(attribute_nextkey).get(0);
                            }
                        } catch (NullPointerException e) {
                            if (!excel_data.get(attribute_nextkey).toString().isEmpty()) {
                                row_data.put(excel_data.get(attribute_nextkey), "NULL");
                                used_keys.add(get_keypositions(itr_nextkey).get(1));
                                used_keys.add(attribute_nextkey);
                                itr_nextkey = get_keypositions(itr_nextkey).get(0);
                                attribute_nextkey = get_keypositions(attribute_nextkey).get(0);
                            }
                            continue;
                        }
                    }
                    double_dimentional_data.add(row_data);
                    itr_nextkey = get_keypositions(temp_itr).get(1);
                }
                ret = getJSONStringFrom2D(double_dimentional_data);
                break;
            }

            case "1D_DataSet": {
                String nextkey = inital_key;
                while (nextkey != null && !nextkey.isEmpty() && keys.contains(nextkey)) {
                    used_keys.add(nextkey);
                    if (!excel_data.get(nextkey).toString().isEmpty() && !excel_data.get(get_keypositions(nextkey).get(0)).toString().isEmpty()) {
                        single_dimentional_data.put(excel_data.get(nextkey), excel_data.get(get_keypositions(nextkey).get(0)));
                        used_keys.add(get_keypositions(nextkey).get(0));
                        nextkey = get_keypositions(nextkey).get(1);
                    } else if (!excel_data.get(nextkey).toString().isEmpty()) {
                        single_dimentional_data.put(excel_data.get(nextkey), "NULL");
                        used_keys.add(get_keypositions(nextkey).get(0));
                        nextkey = get_keypositions(nextkey).get(1);
                    }
                }
                ret = getJSONStringFrom1D(single_dimentional_data);
                break;
            }
            case "Single_Value": {
                Object inital_value = excel_data.get(inital_key);
                used_keys.add(inital_key);
                ret = getJSONString(inital_value);
                break;
            }
            case "Single_Column_DataSet": {
                Object inital_value = excel_data.get(inital_key);
                if (inital_value.toString().contains(":")) {
                    String[] str_init = inital_value.toString().split(":");
                    single_dimentional_data.put(str_init[0], str_init[1]);
                    String nextkey = inital_key, nextkey_inner_down;
                    Object nextkey_inner_down_value;
                    try {
                        while (nextkey != null && !nextkey.isEmpty() && keys.contains(nextkey)) {
//                            keys.remove(nextkey);
                            used_keys.add(nextkey);
                            nextkey_inner_down = get_keypositions(nextkey).get(1);
                            if (!excel_data.get(nextkey_inner_down).toString().isEmpty()) {
                                nextkey_inner_down_value = excel_data.get(nextkey_inner_down);
                                String[] str = nextkey_inner_down_value.toString().split(":");
                                single_dimentional_data.put(str[0], str[1]);
                                nextkey = nextkey_inner_down;
                            }
                        }
                    } catch (NullPointerException e) {
                        ret = getJSONStringFrom1D(single_dimentional_data);
//                        e.printStackTrace();
                    } finally {
                        break;
                    }
                } else {
                    dataset.add(inital_value);
                    String nextkey = inital_key, nextkey_inner_down;
                    Object nextkey_inner_down_value;
                    try {
                        while (nextkey != null && !nextkey.isEmpty() && keys.contains(nextkey)) {
//                            keys.remove(nextkey);
                            used_keys.add(nextkey);
                            nextkey_inner_down = get_keypositions(nextkey).get(1);
                            if (!excel_data.get(nextkey_inner_down).toString().isEmpty()) {
                                nextkey_inner_down_value = excel_data.get(nextkey_inner_down);
                                dataset.add(nextkey_inner_down_value);
                                nextkey = nextkey_inner_down;
                            }
                        }
                    } catch (NullPointerException e) {
                        ret = getJSONString(dataset);
//                        e.printStackTrace();
                    } finally {
                        break;
                    }
                }

            }
        }
        return ret;
    }

    public static void initialize_keyword() {

        json_keyvalues.add("Bank");
        json_keyvalues.add("Brokerage");
        json_keyvalues.add("Tel");
        json_keyvalues.add("Invoice");
    }

    private static String getJSONString(Object object) {
        String ret = "";
        String str1 = "value" + json_increment;
        initialize_keyword();
        String json_key = null;
        boolean flag = false;
        for (String str : json_keyvalues) {
            if (object.toString().contains(str)) {
                json_key = str;
                flag = true;
            } else {
                json_key = str1;
                flag = true;
            }
        }
        if (flag == true) {
            JSONObject json_create = new JSONObject();
            json_create.put(json_key, object);
            ret = json_create.toString();
            json_increment++;
        }
        return ret;
    }

    private static String getJSONString(ArrayList<Object> dataset) {
        String ret = "";
        initialize_keyword();
        String json_key = null;
        boolean flag = false;
        for (Object obj : dataset) {
            for (String str : json_keyvalues) {
                if (obj.toString().contains(str)) {
                    json_key = str;
                    flag = true;
                }
            }

        }
        if (flag == true) {
            JSONObject json_create = new JSONObject();
            json_create.put(json_key, dataset);
            ret = json_create.toString();
        } else if (flag == false) {
            JSONObject json_create = new JSONObject();
            json_create.put("details", dataset);
            ret = json_create.toString();
        }
        return ret;
    }

    private static String getJSONStringFrom2D(ArrayList<LinkedHashMap<Object, Object>> dataTable) {
        String ret = "";

        if (dataTable != null) {
            int rowCount = 0;
            JSONObject tableJsonObject = new JSONObject();
            if (dataTable.size() > 0) {
                for (LinkedHashMap<Object, Object> dataset : dataTable) {
                    rowCount++;
                    JSONObject rowJsonObject = new JSONObject();
                    Set<Object> keys = dataset.keySet();
                    for (Object key : keys) {
                        rowJsonObject.put(key.toString(), dataset.get(key));
                    }
                    tableJsonObject.put("Row " + rowCount, rowJsonObject);

                }

            }
            ret = tableJsonObject.toString();
        }
        return ret;
    }

    private static String getJSONStringFrom1D(LinkedHashMap<Object, Object> single_dimentional_data) {
        String ret = "";

        if (single_dimentional_data != null) {
            JSONObject singleJsonObject = new JSONObject();
            if (single_dimentional_data.size() > 0) {
                Set<Object> keys = single_dimentional_data.keySet();
                for (Object key : keys) {
                    singleJsonObject.put(key.toString(), single_dimentional_data.get(key));
                }
            }
            ret = singleJsonObject.toString();
        }
        return ret;
    }


    public ArrayList<String> get_keypositions(String key) {

        ArrayList<String> key_pos = new ArrayList<>();
        String append_char;
        char next_char;
        int next_int;
        ArrayList<Integer> pos = new ArrayList<>();
        if (key.length() == 2 && Character.isLetter(key.charAt(0)) && Character.isDigit(key.charAt(1))) {
            next_char = key.charAt(0);
            if (next_char == 'Z') {
                append_char = "AA";
            } else {
                append_char = String.valueOf(++next_char);
            }
            next_int = Integer.parseInt(key.substring(1, 2));
            next_int++;
            String adjacent_key = append_char + String.valueOf(key.charAt(1));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("2 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);

        } else if ((key.length() == 3) && (Character.isLetter(key.charAt(0))) && (Character.isDigit(key.charAt(1))) && (Character.isDigit(key.charAt(2)))) {
            next_char = key.charAt(0);
            if (next_char == 'Z') {
                append_char = "AA";
            } else {
                append_char = String.valueOf(++next_char);
            }
            next_int = Integer.parseInt(key.substring(1, 3));
            next_int++;
            String adjacent_key = append_char + String.valueOf(key.charAt(1)) + String.valueOf(key.charAt(2));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("3 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        } else if ((key.length() == 3) && (Character.isLetter(key.charAt(0))) && (Character.isLetter(key.charAt(1))) && (Character.isDigit(key.charAt(2)))) {
            next_char = key.charAt(1);
            if (next_char == 'Z') {
                next_char = 'A';
            } else {
                next_char++;
            }
            next_int = Integer.parseInt(key.substring(2, 3));
            next_int++;
            String adjacent_key = String.valueOf(key.charAt(0)) + String.valueOf(next_char) + String.valueOf(key.charAt(2));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(key.charAt(1)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("3 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        } else if ((key.length() == 4) && (Character.isLetter(key.charAt(0))) && (Character.isLetter(key.charAt(1))) && (Character.isLetter(key.charAt(2))) && (Character.isDigit(key.charAt(3)))) {
            next_char = key.charAt(2);
            if (next_char == 'Z') {
                next_char = 'A';
            } else {
                next_char++;
            }
            next_int = Integer.parseInt(key.substring(3, 4));
            next_int++;
            String adjacent_key = String.valueOf(key.charAt(0)) + String.valueOf(key.charAt(1)) + String.valueOf(next_char) + String.valueOf(key.charAt(3));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(key.charAt(1)) + String.valueOf(key.charAt(2)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("4 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        } else if ((key.length() == 4) && (Character.isLetter(key.charAt(0))) && (Character.isLetter(key.charAt(1))) && (Character.isDigit(key.charAt(2))) && (Character.isDigit(key.charAt(3)))) {
            next_char = key.charAt(1);
            if (next_char == 'Z') {
                next_char = 'A';
            } else {
                next_char++;
            }
            next_int = Integer.parseInt(key.substring(2, 4));
            next_int++;
            String adjacent_key = String.valueOf(key.charAt(0)) + String.valueOf(next_char) + String.valueOf(key.charAt(2)) + String.valueOf(key.charAt(3));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(key.charAt(1)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("4 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        } else if ((key.length() == 4) && (Character.isLetter(key.charAt(0))) && (Character.isDigit(key.charAt(1))) && (Character.isDigit(key.charAt(2))) && (Character.isDigit(key.charAt(3)))) {
            next_char = key.charAt(0);
            if (next_char == 'Z') {
                append_char = "AA";
            } else {
                append_char = String.valueOf(++next_char);
            }
            next_int = Integer.parseInt(key.substring(1, 4));
            next_int++;
            String adjacent_key = append_char + String.valueOf(key.charAt(1)) + String.valueOf(key.charAt(2)) + String.valueOf(key.charAt(3));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("4 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        } else if ((key.length() == 5) && (Character.isLetter(key.charAt(0))) && (Character.isLetter(key.charAt(1))) && (Character.isDigit(key.charAt(2))) && (Character.isDigit(key.charAt(3))) && (Character.isDigit(key.charAt(4)))) {
            next_char = key.charAt(1);
            char prev_char = key.charAt(0);
            if (next_char == 'Z') {
                next_char = 'A';
                prev_char++;
            } else {
                next_char++;
            }
            next_int = Integer.parseInt(key.substring(2, 5));
            next_int++;
            String adjacent_key = String.valueOf(prev_char) + String.valueOf(next_char) + String.valueOf(key.charAt(2)) + String.valueOf(key.charAt(3)) + String.valueOf(key.charAt(4));
            String downward_key = String.valueOf(key.charAt(0)) + String.valueOf(key.charAt(1)) + String.valueOf(next_int);
            key_pos.add(adjacent_key);
            key_pos.add(downward_key);
//            System.out.println("4 digit-----"+key+"-----right:"+adjacent_key+"------down:"+downward_key);
        }

        return key_pos;
    }

    HashMap<String, String> getrequiredData(List<String> keys, JSONArray jsondata, String excel_filename) {
        HashMap<String, String> invoicedata = new HashMap<>();
        ArrayList<HashMap<String, String>> trade_details = new ArrayList<HashMap<String, String>>();
        String excel_key = "Statement Summary";
        String excel_key1 = "Current Month Activity";
        String invoice_Legal_Entity = "'Legal Entity";
        String invoice_Trade_Date = "'Trade Date";
        String TradeType_Business_Grp = "'Business Grp";
        String TradeType_Prd_Grp = "'Prd Grp";
        String TradeType_Prd_Type = "'Prd Type";
        String invoice_broker_key1 = "HSBC";
        String Invoice_Total_key1 = "Row";
        String Invoice_Total_key2 = "value";
        Boolean trade_flag = true;
        Boolean flag = false;
        for (String key : keys) {
            if(key.equals("Invoice Total")){
                System.out.println();
            }
            Iterator<Object> iterator = jsondata.iterator();
            Boolean trade_flag1 = true;
            while (iterator.hasNext()) {
                JSONObject obj = (JSONObject) iterator.next();
                if(obj.toString().contains("BGC-Sep-2018-0000778")){
                    System.out.println();
                }
                Map<String, Object> inner_data = new LinkedHashMap<>();
                inner_data = obj.toMap();
                TreeMap<String, Object> inner_data_sorted = new TreeMap<>(inner_data);
                Set<Map.Entry<String, Object>> inner_keyset = inner_data_sorted.entrySet();
                if (key.equalsIgnoreCase("Broker")) {
                    if (inner_keyset.toString().contains("details") && inner_data.get("details").toString().contains(invoice_broker_key1)) {
                        invoicedata.put(key, inner_data_sorted.get("details").toString());
                        break;
                    } else if (inner_keyset.toString().contains("value") && inner_data.get(inner_keyset.iterator().next().getKey()).toString().contains(invoice_broker_key1)) {
                        invoicedata.put(key, inner_data_sorted.get(inner_keyset.iterator().next().getKey()).toString());
                        break;
                    }
                }
                if (key.equalsIgnoreCase("Invoice Total") || key.equalsIgnoreCase("entity name") || key.equalsIgnoreCase("Trade Type") || key.equalsIgnoreCase("Invoice Trade Date")) {
                    if (obj.toString().contains(Invoice_Total_key1)) {
                        flag = true;
                        if (trade_flag == true) {
                            if (trade_flag1 == true) {
                                Set<String> trade_keys = inner_data_sorted.keySet();
                                Iterator<String> trade_it = trade_keys.iterator();
                                while (trade_it.hasNext()) {
                                    trade_details.add((HashMap<String, String>) inner_data_sorted.get(trade_it.next()));
                                }
                            }
                            if (trade_flag1 == false) {
                                Set<String> trade_keys = inner_data_sorted.keySet();
                                Iterator<String> trade_it = trade_keys.iterator();
                                String keyval=null;
                                while (trade_it.hasNext()) {
                                     keyval=trade_it.next();
                                    trade_details.add((HashMap<String, String>) inner_data_sorted.get(keyval));
                                }
                                if(inner_data_sorted.lastKey().contains(keyval)){
                                    trade_flag = false;
                                }
                            }
                            trade_flag1 = false;
                        }
                        if (inner_data.toString().contains(invoice_Legal_Entity) && key.equalsIgnoreCase("entity name")) {
                            HashMap<String, String> grid_data = new HashMap<>();
                            grid_data = (HashMap<String, String>) inner_data_sorted.get(inner_keyset.iterator().next().getKey());
                            invoicedata.put(key, grid_data.get(invoice_Legal_Entity).toString());
                            break;
                        }
                        if (inner_data.toString().contains(invoice_Legal_Entity) && key.equalsIgnoreCase("Trade Type")) {
                            HashMap<String, String> grid_data = new HashMap<>();
                            grid_data = (HashMap<String, String>) inner_data_sorted.get(inner_keyset.iterator().next().getKey());
                            String value = grid_data.get(TradeType_Business_Grp) + "," + grid_data.get(TradeType_Prd_Grp) + "," + grid_data.get(TradeType_Prd_Type);
                            invoicedata.put(key, value);
                            break;
                        }
                    }
                    if (inner_data.toString().contains(invoice_Trade_Date) && key.equalsIgnoreCase("Invoice Trade Date")) {
                        HashMap<String, String> grid_data = new HashMap<>();
                        grid_data = (HashMap<String, String>) inner_data_sorted.get(inner_keyset.iterator().next().getKey());
                        String value = grid_data.get(invoice_Trade_Date);
                        invoicedata.put(key, value);
                        break;
                    }
                    if (flag == true && key.equalsIgnoreCase("Invoice Total")) {
                        if (obj.toString().contains(Invoice_Total_key2)) {
                            flag = false;
                            invoicedata.put(key, inner_data_sorted.get(inner_keyset.iterator().next().getKey()).toString());
                            break;
                        }
                    }
                }
                String value = compare(obj, key).trim();
                if ((value.length() > 0) && !value.isEmpty()) {
                    invoicedata.put(key, value);
                    break;
                }
            }
            if (trade_flag1 == false){
                trade_flag = false;
            }

        }
        if(!trade_details.isEmpty()){
            setTrade_details(trade_details);
        }
        return invoicedata;
    }

    public void setTrade_details(ArrayList<HashMap<String, String>> trade_details ){
        this.trade_details=trade_details;
    }
    ArrayList<HashMap<String, String>> getTrade_details(){
        return trade_details;
    }
    String compare(JSONObject obj, String key) {
        String value = "";
        if (key.contains(" ")) {
            String[] key_array = key.split(" ");
            if (key_array.length > 0) {
                for (String str : key_array) {
                    Set<String> keyset = obj.keySet();
                    for (String filter : keyset) {
//                      String filter=filter1.replaceAll("'","");
                        if (filter.contains(str) && !filter_flag.contains(str)) {
                            value = obj.get(filter).toString();
                            filter_flag.add(filter);
                            filter_flag.add(str);
                            break;
                        }
                    }
                }
            }
        } else {
            if (obj.toString().contains(key)) {
            }
        }
        return value;
    }
}
