package frc.team_8840_lib.utils.files;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    //https://stackoverflow.com/questions/14676407/list-all-files-in-the-folder-and-also-sub-folders
    public static void listall(String directoryName, List<File> files) {
        File directory = new File(directoryName);
    
        // Get all files from a directory.
        File[] fList = directory.listFiles();
        if (fList != null) {
            for (File file : fList) {      
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    listall(file.getAbsolutePath(), files);
                }
            }
        }
    }

    public static List<File> listall(String directoryName) {
        List<File> files = new ArrayList<>();
        listall(directoryName, files);
        return files;
    }

    public static void write(Path path, String contents) {
        
        File file = path.toFile();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        //Append string to file
        try {
            java.io.FileWriter fw = new java.io.FileWriter(file, true);
            fw.write(contents);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String read(Path path) {
        File file = path.toFile();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        return read(file);
    }

    public static String read(File file) {
         //Read string from file
         try {
            java.io.FileReader fr = new java.io.FileReader(file);
            StringBuilder sb = new StringBuilder();
            int i;
            while ((i = fr.read()) != -1) {
                sb.append((char) i);
            }
            fr.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void mkdir(Path path) {
        File file = path.toFile();

        if (!file.exists()) {
            try {
                file.mkdirs();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
