package io.underdark.app.log;

import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by chenchik on 9/27/17.
 */

public class Logger {

    static String log_directory = "underdark";
    static String log_file_name = "log.txt";

    public static boolean writeToLog(String message) {

        //create the directory if it doesn't exist
        File folder = new File(Environment.getExternalStorageDirectory().toString() + "/" + log_directory);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        //if there was an error in creating the directory, return false
        if(!success){
            return false;
        }

        //create the file if it doesn't exist and append 'message' to it
        File file = new File(Environment.getExternalStorageDirectory().toString(), "/" + log_directory + "/" + log_file_name);
        try {
            FileOutputStream fileOutput = new FileOutputStream(file, true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutput);
            outputStreamWriter.write(message + "\n");
            outputStreamWriter.flush();
            fileOutput.getFD().sync();
            outputStreamWriter.close();
        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }
}
