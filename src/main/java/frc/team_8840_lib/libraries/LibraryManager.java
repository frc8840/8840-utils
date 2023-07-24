package frc.team_8840_lib.libraries;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import frc.team_8840_lib.Info;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.utils.library.AutonomousLibraryInfo;
import frc.team_8840_lib.utils.library.IOLibraryInfo;
import frc.team_8840_lib.utils.library.LoggerLibraryInfo;
import frc.team_8840_lib.utils.library.SwerveLibraryInfo;

public class LibraryManager {
    private static ArrayList<LibraryInfo> loadedLibraries;

    private static boolean requireInternetConnection = true;
    private static boolean allowNonJavaLibraries = false;

    private static boolean downloadedNewLibraries = false;

    private static String libraryFolder = "libs";

    public static void overrideInternetCheck() {
        requireInternetConnection = false;
    }

    public static void overrideStrictlyJava() {
        allowNonJavaLibraries = true;
    }

    public static boolean hasDownloadedNewLibraries() {
        return downloadedNewLibraries;
    }

    public static void start() {
        registerLibrary(new Info());
        registerLibrary(new AutonomousLibraryInfo());
        registerLibrary(new IOLibraryInfo());
        registerLibrary(new LoggerLibraryInfo());
        registerLibrary(new SwerveLibraryInfo());
    }

    public static void registerLibrary(LibraryInfo info) {
        if (loadedLibraries == null)
            loadedLibraries = new ArrayList<>();

        System.out.println("--------------------");

        System.out.println("Registering library " + info.name() + " by " + info.author() + ", version: " + info.version() + ".");

        loadedLibraries.add(info);
        System.out.println("Loaded library " + info.name() + " by " + info.author() + ", version: " + info.version() + ".");
        if (info.experimental()) {
            System.out.println("Library " + info.name() + " is experimental. Use at your own risk!");
        }

        System.out.println("--------------------");
        System.out.println();
    }

    public static LibraryInfo getLibraryInfoByRepo(String repo) {
        for (LibraryInfo info : loadedLibraries) {
            if (info.repo().equals(repo))
                return info;
        }

        return null;
    }

    public static LibraryInfo getLibraryInfoByName(String name) {
        for (LibraryInfo info : loadedLibraries) {
            if (info.name().equals(name))
                return info;
        }

        return null;
    }

    public static ArrayList<LibraryInfo> getLoadedLibraries() {
        return loadedLibraries;
    }

    public static String[] getLanguageOfGitHubRepo(String author, String repository) {
        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://api.github.com/repos/" + author + "/" + repository + "/languages");

            //Send a GET request to the URL
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            //Get the response from the server
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Logger.Log("LibraryManager", "Error getting language of GitHub repository " + author + "/" + repository + ": " + responseCode);
                return new String[0];
            }
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Error getting language of GitHub repository " + author + "/" + repository + ": " + e.getMessage());
            return new String[0];
        }

        JSONObject languages;

        try {
            languages = new JSONObject(connection.getInputStream());
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Error getting language of GitHub repository " + author + "/" + repository + ": " + e.getMessage());
            return new String[0];
        }

        ArrayList<String> langList = new ArrayList<>();

        for (String key : languages.keySet()) {
            langList.add(key);
        }
        
        String[] langArray = new String[langList.size()];
        langArray = langList.toArray(langArray);

        return langArray;
    }

    public static void loadLibraryFromGitHub(String author, String repository) {
        if (loadedLibraries == null)
            loadedLibraries = new ArrayList<>();
        
        if (getLibraryInfoByRepo(author + "/" + repository) != null)
            return;

        //First, check if this is running on a RoboRIO
        String os = System.getProperty("os.name").toLowerCase();

        if ((os == null || os.contains("linux")) && !os.contains("windows") && !os.contains("mac")) {
            if (requireInternetConnection) {
                Logger.Log("LibraryManager", "Unfortunately, this library does not support automatic installation on Linux or the RoboRIO. Please install it manually, or add LibraryManager#overrideInternetCheck() to your code to disable this check.");
                return;
            } else {
                Logger.Log("LibraryManager", "WARNING! This code may be running on the RoboRIO. If so, the program may crash if the library is not installed due to no internet connection. Remove LibraryManager#overrideInternetCheck() to re-enable this check.");
            }
        }

        //Next, check if the repository exists
        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://api.github.com/users/" + author + "/repos");
            
            //Send a GET request to the URL
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            //Check if the response code is 200 (OK)
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Logger.Log("LibraryManager", "Could not find repository " + author + "/" + repository + "!");
                return;
            }
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not find repository " + author + "/" + repository + "!");
            return;
        }
        
        //Parse the response
        JSONArray response = null;

        try {
            response = new JSONArray(connection.getInputStream());
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not parse response from GitHub!");
            return;
        }

        //Check if the repository exists
        boolean exists = false;

        for (int i = 0; i < response.length(); i++) {
            JSONObject repo = response.getJSONObject(i);

            if (repo.getString("name").equals(repository)) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            Logger.Log("LibraryManager", "Could not find repository " + author + "/" + repository + "! Check your spelling and try again.");
            return;
        }

        String[] languages = getLanguageOfGitHubRepo(author, repository);

        boolean isJava = false;

        for (String lang : languages) {
            if (lang.equals("Java")) {
                isJava = true;
                break;
            }
        }

        if (!isJava && !allowNonJavaLibraries) {
            Logger.Log("LibraryManager", "That's odd, you're trying to install " + author + "/" + repository + ", but it's not a Java library! If you're sure you want to install it, add LibraryManager#overrideStrictlyJava() to your code to disable this check.");
            return;
        } else if (!isJava) {
            Logger.Log("LibraryManager", "That's odd, we're installing a non-Java library. This is not recommended, since this may install things multiple times.");
        }

        //Finally, install the library
        Logger.Log("LibraryManager", "Installing " + author + "/" + repository + "...");

        //First, we need to find the latest release
        URL releasesURL;

        try {
            releasesURL = new URL("https://api.github.com/repos/" + author + "/" + repository + "/releases/latest");

            //Send a GET request to the URL
            connection = (HttpURLConnection) releasesURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            //Check if the response code is 200 (OK)
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Logger.Log("LibraryManager", "Could not find latest release of " + author + "/" + repository + "!");
                return;
            }

        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not find latest release of " + author + "/" + repository + "!");
            return;
        }

        //Parse the response
        JSONObject responseJSON;

        try {
            responseJSON = new JSONObject(connection.getInputStream());
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not parse response from GitHub!");
            return;
        }

        //Get all URLs of all .jar files
        ArrayList<String> jarURLs = new ArrayList<>();
        ArrayList<String> jarNames = new ArrayList<>();

        JSONArray assets = responseJSON.getJSONArray("assets");

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);

            if (asset.getString("content_type").equals("application/java-archive")) {
                jarURLs.add(asset.getString("browser_download_url"));
                String tagName = responseJSON.getString("tag_name");
                String name = asset.getString("name");
                String jarName = name.substring(0, name.length() - 4);
                jarNames.add(jarName + "-" + tagName + ".jar");
            }
        }

        Logger.Log("LibraryManager", "Discovered " + jarURLs.size() + " .jar files to download. Downloading...");

        //Download all .jar files
        ArrayList<File> jarFiles = new ArrayList<>();

        int i = 0;

        for (String jarURL : jarURLs) {
            Logger.Log("LibraryManager", "Attempting download of " + jarURL + "...");

            File jarFile = downloadJarFileFromURL(jarURL);

            //Rename the file
            String jarName = jarNames.get(jarURLs.indexOf(jarURL));

            File newJarFile = new File(jarFile.getParentFile().getAbsolutePath() + "/" + jarName);

            jarFile.renameTo(newJarFile);

            Logger.Log("LibraryManager", "Successfully downloaded " + jarURL + "!");
            Logger.Log("LibraryManager", "Successfully downloaded " + i + "/" + jarURLs.size() + " .jar files.");

            i++;
        }

        //All files should be downloaded now, and in the "libs" folder, so the process should be complete.
        Logger.Log("LibraryManager", "Installed " + author + "/" + repository + "! Sucessfully downloaded " + jarFiles.size() + " .jar files.");

        downloadedNewLibraries = true;
    }

    private static File downloadJarFileFromURL(String url) {
        URL jarURL;

        try {
            jarURL = new URL(url);
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            return null;
        }

        BufferedInputStream in = null;

        try {
            in = new BufferedInputStream(jarURL.openStream());
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            return null;
        }

        //Get directory of the project folder
        String dir = System.getProperty("user.dir");

        //Check if the "libs" folder exists
        File libsFolder = new File(dir + "/" + libraryFolder);

        if (!libsFolder.exists()) {
            libsFolder.mkdir();
        }

        //Create a temporary file
        File tempFile = null;

        try {
            tempFile = File.createTempFile("temp", ".jar", libsFolder);
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            return null;
        }

        //Write the contents of the URL to the temporary file
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(tempFile);
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            return null;
        }

        byte[] buf = new byte[1024];
        int n = 0;
        
        try {
            while ((n = in.read(buf)) != -1) {
                fos.write(buf, 0, n);
            }
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            tempFile = null;
        }

        try {
            fos.close();
            in.close();
        } catch (Exception e) {
            Logger.Log("LibraryManager", "Could not download .jar file from URL " + url + "!");
            return null;
        }

        return tempFile;
    }
}
