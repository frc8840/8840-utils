package frc.team_8840_lib.input.communication;

import com.sun.net.httpserver.HttpExchange;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.BooleanArrayPublisher;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerArrayPublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.networktables.Publisher;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.wpilibj.motorcontrol.MotorController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.controllers.SwerveDrive;
import frc.team_8840_lib.controllers.SwerveModule;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.dashboard.ModuleBuilder;
import frc.team_8840_lib.input.communication.dashboard.pages.PageHandler;
import frc.team_8840_lib.input.communication.server.HTTPServer;
import frc.team_8840_lib.listeners.Preferences;
import frc.team_8840_lib.listeners.Robot;
import frc.team_8840_lib.pathing.PathConjugate;
import frc.team_8840_lib.pathing.PathPlanner;
import frc.team_8840_lib.pathing.PathConjugate.ConjugateType;
import frc.team_8840_lib.utils.files.FileUtils;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;
import frc.team_8840_lib.utils.http.html.Element;
import frc.team_8840_lib.utils.http.html.EncodingUtil;
import frc.team_8840_lib.utils.interfaces.Callback;
import frc.team_8840_lib.utils.math.units.Unit.Type;
import frc.team_8840_lib.utils.pathplanner.PathCallback;
import frc.team_8840_lib.utils.pathplanner.TimePoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class CommunicationManager {
    private static CommunicationManager instance;

    public static final String SpeedControllerKey = "SpeedController";

    public static int port = 5805;

    /**
     * Get the instance of the CommunicationManager.
     * This is the same as {@link #i()}.
     * @return The instance of the CommunicationManager.
     */
    public static CommunicationManager getInstance() {
        return instance;
    }

    /**
     * Get the instance of the CommunicationManager. Shorter name for convenience.
     * @return The instance of the CommunicationManager.
     */
    public static CommunicationManager i() {
        return instance;
    }

    /**
     * Start the communication manager. This method is used internally and should not be called.
     */
    public static void init() {
        instance = new CommunicationManager();

        NetworkTableInstance.getDefault().flush(); //idk why this is here but yeah
    }

    /**
     * Returns the base path of the network table for 8840-utils
     * @return The base path of the network table for 8840-utils.
     */
    public static String base() {
        return "8840-lib";
    }

    private ArrayList<String> createdTitles;
    private NetworkTable table;
    private HashMap<String, Publisher> entries;

    private HTTPServer server;

    private CommunicationManager() {
        instance = this;

        if (!Robot.getInstance().isFTC()) {
            NetworkTableInstance ntinst = NetworkTableInstance.getDefault();

            ntinst.startServer();

            table = ntinst.getTable(base());
        }

        createdTitles = new ArrayList<>();
        entries = new HashMap<>();

        try {
            /**
             * According to rule R704 and Table 9-5 Open FMS Ports, ports 5800-5810 are open to UDP/TCP traffic.
             * This means that we are allowed to host a server on any of the ports in this range, so we choose 5805.
             * */
            if (port < 5800 || port > 5810) {
                Logger.Log("API", "Port " + port + " is not in the range of 5800-5810, which is the range of ports allowed by the FMS. Please change the port to a value in this range.");
                Logger.Log("API", "Changing port automatically to 5805.");
                port = 5805;
            }

            PageHandler.addPages();

            server = new HTTPServer(port);

            server.route(new Route("/", PageHandler.get()));

            server.route(new Route("/confirmation", new Constructor() {
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    return res.send(Element.CreatePage(Element.CreateHead(
                            new Element("title").setTextContent("8840Lib")
                    ), Element.CreateBody(
                            new Element("h1").setTextContent("8840-lib server is running!")
                    )));
                }
            }));

            server.route(new Route("/json", new Constructor() {
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    return res.json("{\"success\": true, \"message\": \"Hello World!\"}");
                }
            }));

            server.route(new Route("/nt", new Constructor() {
                /**
                 * Examples of requests for the /nt (Network Table REST API) endpoint:
                 * /nt?tab=list - returns a list of all the tabs.
                 * /nt?tab=Status - returns a list of the keys in the Status tab.
                 * /nt?tab=Status&key=Network%20Communication&operation=get&type=string - returns the value of the Network Communication key in the Status tab, as a string.
                 * /nt?tab=Status&key=Network%20Communication&operation=set&type=string&value=Hello%20World - sets the value of the Network Communication key in the Status tab to Hello World.
                 */
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    String rawQuery = req.getRequestURI().getQuery();
                    HashMap<String, String> query = this.parseQuery(rawQuery);

                    if (!query.containsKey("operation") || !query.containsKey("key") || !query.containsKey("tab") || !query.containsKey("type")) {
                        if (query.containsKey("tab")) {
                            String tab = "";
                            try {
                                tab = URLDecoder.decode(query.get("tab"), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                return res.json(this.error("Invalid tab name")).status(400);
                            }

                            ArrayList<String> tks = CommunicationManager.getInstance().createdTitles;
                            String relatedTks = "";

                            for (String tk : tks) {
                                String t = tk.replace(t_k_separator, "%%%%").split("%%%%")[0];
                                String k = tk.replace(t_k_separator, "%%%%").split("%%%%")[1];

                                //If the tab in param is equal to "list", list out the tabs. Else, just list the keys in the tab.
                                if (tab.equals("list")) {
                                    if (!relatedTks.contains(t)) {
                                        relatedTks += "\"" + t + "\", ";
                                    }
                                } else if (t.equals(tab)) {
                                    relatedTks += "\"" + k + "\", ";
                                }
                            }
                            //Remove the last ", " from the end of the string
                            relatedTks = relatedTks.length() > 0 ? relatedTks.substring(0, relatedTks.length() - 2) : "";
                            return res.json("{\"success\": true, \"" + (tab.equals("list") ? "tabs" : "keys") + "\": [" + relatedTks + "]}");
                        }

                        return res.json(this.error("Missing param(s) [operation, key, tab, type].")).status(400);
                    }

                    String operationType = query.get("operation");
                    String key = query.get("key");
                    String tab = query.get("tab");
                    String type = query.get("type");
                    try {
                        key = URLDecoder.decode(key, "UTF-8");
                        tab = URLDecoder.decode(tab, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return res.json(this.error("Invalid key/tab name")).status(400);
                    }

                    if (operationType.equals("get")) {
                        if (CommunicationManager.getInstance().get(tab, key) == null) {
                            return res.status(404);
                        }
                        NetworkTableValue value = CommunicationManager.getInstance().get(tab, key).getValue();
                        String valueString = "";
                        if (type.equals("double")) {
                            valueString = String.valueOf(value.getDouble());
                        } else if (type.equals("int")) {
                             valueString = String.valueOf((int) Math.round(value.getDouble()));
                        } else if (type.equals("string")) {
                            valueString = value.getString();
                        } else if (type.equals("boolean")) {
                            valueString = value.getBoolean() + "";
                        } else if (type.equals("raw")) {
                            valueString = Arrays.toString(value.getRaw());
                        } else {
                            return res.json(this.error("Invalid type")).status(400);
                        }
                        return res.json("{\"success\": true, \"value\": \"" + (valueString) + "\"}");
                    } else if (operationType.equals("set")) {
                        if (!query.containsKey("value")) {
                            return res.json(this.error("Does not contain param 'value'.")).status(400);
                        }

                        String value = query.get("value");

                        switch (type) {
                            case "string":
                                try {
                                    updateInfo(tab, key, URLDecoder.decode(value, "UTF-8"));
                                } catch (UnsupportedEncodingException e) {
                                    return res.json(this.error("There was an issue decoding the value.")).status(400);
                                }
                                break;
                            case "double":
                                updateInfo(tab, key, Double.parseDouble(value));
                                break;
                            case "int":
                                updateInfo(tab, key, Integer.parseInt(value));
                                break;
                            case "boolean":
                                updateInfo(tab, key, Boolean.parseBoolean(value));
                                break;
                            default:
                                return res.json(this.error("Type was not found. Currently only supporting doubles and strings.")).status(400);
                        }

                        return res.json("{\"success\": true, \"message\": \"Value set\"}");
                    } else {
                        return res.json(this.error("Unknown operation.")).status(400);
                    }
                }
            }));

            server.route(new Route("/get_registered_paths", new Constructor() {
                /**
                 * Returns a list of the paths that are registered in the PathPlanner class.
                 * /get_registered_paths, GET Request
                 * 
                 * Looking for the paths stored in file? Use /auto_path with a GET request.
                 */
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    if (!req.getRequestMethod().equalsIgnoreCase("GET")) {
                        return res.json(this.error("Invalid request method.")).status(400);
                    }

                    String[] keys = PathPlanner.getAutoNames();

                    String keyString = "";

                    for (String key : keys) {
                        keyString += "\"" + key + "\", ";
                    }

                    keyString = keyString.length() > 0 ? keyString.substring(0, keyString.length() - 2) : "";

                    return res.json("{\"success\": true, \"paths\": [" + keyString + "], \"selected_path\": \"" + PathPlanner.getSelectedAutoName() + "\"}");
                }
            }));

            server.route(new Route("/auto/path", new Constructor() {
                /**
                 * Examples of requests for the /auto_path endpoint:
                 * /auto_path, POST Request with payload from 8840-app
                 */
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    String homePath = System.getProperty("user.home");
                    File folder = new File(homePath + "/8840appdata");

                    if (!req.getRequestMethod().equalsIgnoreCase("POST")) {
                        if (req.getRequestMethod().equalsIgnoreCase("GET")) {
                            //Read folder and return list of files
                            if (folder.exists()) {
                                File[] files = folder.listFiles();

                                String fileNames = "";
                                int skipped = 0;
                                for (File file : files) {
                                    if (file.getName().startsWith(".")) { skipped += 1; continue; }

                                    fileNames += "\"" + file.getName().replace(".json", "") + "\", ";
                                }

                                if (skipped >= files.length) {
                                    Logger.Log("Only found hidden files, returning empty list.");
                                    return res.json("{\"success\": true, \"files\": [], \"selected\": \"" + currentAutoPath + "\"}");
                                }

                                fileNames = fileNames.length() > 0 ? fileNames.substring(0, fileNames.length() - 2) : "";
                                Logger.Log("Successfully read files from folder.");
                                return res.json("{\"success\": true, \"files\": [" + fileNames + "], \"selected\": \"" + currentAutoPath + "\"}");
                            } else {
                                Logger.Log("/auto_path did not find a folder to read from - send a POST request to create one. It's fine though, no concerns here.");
                                return res.json("{\"success\": true, \"files\": [], \"selected\": \"" + currentAutoPath + "\"}");
                            }
                        }
                        Logger.Log("Received malformed request, only POST/GET requests are supported for /auto_path. Please check your request and try again.");
                        return res.json(this.error("Only POST/GET requests are supported for this endpoint.")).status(400);
                    }

                    InputStream is = req.getRequestBody();
                    String body = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));

                    Logger.Log("Received new autonomous path.");

                    //Store to file
                    JSONObject json = new JSONObject(body);

                    if (json.has("selection")) {
                        if (json.has("legacy")) { //Legacy support.
                            String selection = json.getString("selection");

                            if (selection == null || selection.equals("")) {
                                Logger.Log("Received empty selection, clearing current auto path.");
                                currentAutoPath = "";
                                legacyPathCallback.onPathComplete(new TimePoint[0]);
                                return res.json("{\"success\": true, \"message\": \"Selection was empty, cleared current auto path.\"}");
                            }

                            if (folder.exists()) {
                                File[] files = folder.listFiles();
                                for (File file : files) {
                                    //Ignore any hidden files.
                                    if (file.toString().startsWith(".")) continue;

                                    if (file.getName().equals(selection + ".json")) {
                                        String fileContents;

                                        //Read file
                                        try {
                                            fileContents = new String(Files.readAllBytes(file.toPath()));
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Logger.Log("There was an error while reading the file. Did the file get corrupted?");
                                            return res.json(this.error("There was an issue reading the file.")).status(500);
                                        }

                                        JSONObject fileJson = new JSONObject(fileContents);

                                        TimePoint[] points = readAndParsePath(fileJson, true);

                                        if (points == null || points.length == 0) {
                                            Logger.Log("There was an error while parsing the file. Did you edit the JSON?");
                                            return res.json(this.error("There was an issue parsing the JSON.")).status(500);
                                        }

                                        currentAutoPath = EncodingUtil.fileProofName(fileJson.getString("name"));

                                        Logger.Log("Successfully loaded autonomous path " + currentAutoPath + " with " + points + " points from file.");

                                        return res.json("{\"success\": true, \"message\": \"Set to path with " + points + " data points.\"}");
                                    }
                                }

                                Logger.Log("The file " + selection + " was not found. Was the file deleted, or was the request malformed?");
                                return res.json(this.error("The selected autonomous path was not found.")).status(404);
                            } else {
                                Logger.Log("The folder " + folder.getAbsolutePath() + " was not found. Maybe you haven't saved any paths yet?");
                                return res.json(this.error("No autonomous paths were found.")).status(404);
                            }
                        } else { //Else if it's not legacy method.
                            String selection = json.getString("selection");

                            Logger.Log("Recieved selection: " + selection + ".");

                            //check if selection is in keys
                            if (selection == null || !PathPlanner.validAuto(selection)) {
                                Logger.Log("Recieved invalid selection, not setting current auto path to " + selection + ".");
                                return res.json(this.error("Invalid selection.")).status(400);
                            }

                            PathPlanner.selectAuto(selection);

                            Logger.Log("Successfully set current auto path to " + selection + ".");

                            currentAutoPath = selection;
                            pathCallback.run();

                            return res.json("{\"success\": true, \"message\": \"Set to path " + selection + ".\"}").status(200);
                        }
                    }

                    //Create folder if it doesn't exist
                    if (!folder.exists()) {
                        boolean b = folder.mkdir();
                        if (!b) {
                            Logger.Log("Failed to create folder for autonomous paths.");
                            return res.json(this.error("Failed to create folder for autonomous paths.")).status(500);
                        }
                    }

                    String autoName = EncodingUtil.fileProofName(json.getString("name"));

                    Logger.Log("Received " + autoName + " path.");

                    //Create file if it doesn't exist
                    File file = new File(homePath + "/8840appdata/" + autoName + ".json");
                    if (!file.exists()) {
                        try {
                            boolean b = file.createNewFile();
                            if (!b) {
                                Logger.Log("Failed to create file for autonomous path.");
                                return res.json(this.error("Failed to create file for autonomous path.")).status(500);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            return res.json(this.error("Failed to create file for autonomous path.")).status(500);
                        }
                    }

                    try {
                        FileWriter fw = new FileWriter(file);
                        fw.write(json.toString());
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        return res.json(this.error("Failed to write to file for autonomous path.")).status(500);
                    }

                    TimePoint[] points = readAndParsePath(json, true);

                    if (points == null || points.length == 0) {
                        Logger.Log("There was an error while parsing the JSON.");
                        return res.json(this.error("There was an issue parsing the JSON.")).status(500);
                    }

                    legacyPathCallback.onPathComplete(points);

                    currentAutoPath = autoName;

                    return res.json("{\"success\": true, \"message\": \"Received path with " + points + " data points.\"}");
                }
            }));

            server.route(new Route("/preferences", new Constructor() {
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    if (!req.getRequestMethod().equals("POST")) {
                        if (req.getRequestMethod().equals("GET")) {
                            String currentStoredLogWriter = Preferences.getSelectedLogger();
                            String currentStoredEventListener = Preferences.getSelectedEventListener();

                            boolean lockedLogWriter = Logger.logWriterIsLockedToCode();
                            boolean lockedEventListener = Robot.eventListenerIsLockedToCode();

                            if (lockedEventListener) {
                                currentStoredEventListener = Robot.getEventListenerName();
                            }

                            if (lockedLogWriter) {
                                currentStoredLogWriter = Logger.getLogWriterName();
                            }

                            return res.json("{\"success\": true, \"logWriter\": \"" + currentStoredLogWriter + "\", \"eventListener\": \"" + currentStoredEventListener + "\", \"lockedLogWriter\": " + lockedLogWriter + ", \"lockedEventListener\": " + lockedEventListener + "}");
                        }

                        return res.json(this.error("Invalid request method.")).status(405);
                    }

                    JSONObject json;

                    try {
                        json = new JSONObject(new String(req.getRequestBody().readAllBytes()));
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();

                        return res.json(this.error("Invalid JSON code.")).status(400);
                    }

                    if (!json.has("logWriter") || !json.has("eventListener")) {
                        return res.json(this.error("Invalid JSON format.")).status(400);
                    }

                    String logWriter = json.getString("logWriter");
                    String eventListener = json.getString("eventListener");

                    if (!Logger.logWriterIsLockedToCode()) Preferences.setSelectedLogger(logWriter);
                    if (!Robot.eventListenerIsLockedToCode()) Preferences.setSelectedEventListener(eventListener);

                    Logger.Log("[Preferences] Successfully loaded in preferences from /preferences endpoint.");

                    Preferences.savePreferences(Preferences.getDefaultPreferencesPath());

                    return res.json("{\"success\": true, \"message\": \"Preferences set.\"}");
                }
            }));

            server.route(new Route("/custom_modules", ModuleBuilder.getConstructor()));

            server.route(new Route("/auto/selected", new Constructor() {
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    if (!req.getRequestMethod().equals("GET")) {
                        return res.json(this.error("Invalid request method.")).status(405);
                    }

                    JSONObject json = new JSONObject();

                    JSONArray pathConjugates = new JSONArray();

                    for (PathConjugate conjugate : PathPlanner.getSelectedAuto().getAllConjugates()) {
                        JSONObject conjugateParent = new JSONObject();

                        //First off, we'll do the info about the conjugate
                        JSONObject info = new JSONObject();

                        info.put("type", conjugate.getType().name());
                        info.put("name", conjugate.getName());

                        conjugateParent.put(".info", info);

                        //Now we'll do the actual path, if it's a path object.
                        if (conjugate.getType() == ConjugateType.Path) {
                            JSONArray path = new JSONArray();

                            for (TimePoint point : conjugate.getPath().getAllPoints()) {
                                JSONObject pointParent = new JSONObject();

                                pointParent.put("x", point.getX());
                                pointParent.put("y", point.getY());
                                pointParent.put("d", point.getAngle());

                                path.put(pointParent);
                            }

                            conjugateParent.put("path", path);
                        }

                        pathConjugates.put(conjugateParent);
                    }

                    json.put("conjugates", pathConjugates);

                    JSONObject generalInfoObject = new JSONObject();

                    generalInfoObject.put("name", PathPlanner.getSelectedAutoName());
                    generalInfoObject.put("set_name", currentAutoPath);
                    generalInfoObject.put("number", PathPlanner.getSelectedAuto().getAllConjugates().length);

                    json.put(".info", generalInfoObject);
                    json.put("success", true);

                    return res.json(json.toString());
                }
            }));
            
            server.route(new Route("/files", new Constructor() {
                //This method will either return a list of files in the 8840 directory, or it will upload a file to the 8840 directory.
                @Override
                public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                    String reqMethod = req.getRequestMethod();
                    if (!reqMethod.equals("POST")) {
                        return res.json(this.error("Invalid request method.")).status(405);
                    }

                    JSONObject json;

                    try {
                        json = new JSONObject(new String(req.getRequestBody().readAllBytes()));
                    } catch (Exception e) {
                        return res.json(this.error("Invalid JSON code.")).status(400);
                    }

                    String home = System.getProperty("user.home");

                    if (json.has("folder")) {
                        Path parentFolder = Path.of(home, "8840appdata");

                        //System.out.println("Folder: " + json.getString("folder"));
                        
                        if (json.getString("folder").equalsIgnoreCase("paths")) {
                            parentFolder = Path.of(home, "8840appdata");
                        } else if (json.getString("folder").equalsIgnoreCase("logs")) {
                            parentFolder = Path.of(home, "8840applogs");
                        } else {
                            return res.json(this.error("Invalid folder name.")).status(400);
                        }

                        //Get every single file in the 8840appdata folder, including files in subfolders.
                        List<File> files = FileUtils.listall(parentFolder.toAbsolutePath().toString());

                        JSONArray filesArray = new JSONArray();

                        for (File file : files) {
                            JSONObject fileObject = new JSONObject();

                            if (file.getName().startsWith(".")) continue;

                            fileObject.put("name", file.getName());
                            fileObject.put("path", file.getAbsolutePath());
                            fileObject.put("size", file.length());

                            filesArray.put(fileObject);
                        }
                        
                        JSONObject response = new JSONObject();

                        response.put("success", true);
                        response.put("files", filesArray);

                        return res.json(response);
                    }

                    if (json.has("data") && json.has("path")) {
                        String encodedData = json.getString("data");

                        if (encodedData.equalsIgnoreCase("FOLDER")) {
                            String path = json.getString("path");

                            if (path == null) {
                                return res.json(this.error("Invalid JSON format.")).status(400);
                            }

                            FileUtils.mkdir(Path.of(home, path));

                            Logger.Log("[FileSystem] Successfully created folder at " + path + ".");

                            return res.json("{\"success\": true, \"refresh\": true}");
                        }
                        
                        //Encoded data is in base64, so we need to decode it.
                        byte[] decodedData = Base64.getDecoder().decode(encodedData);

                        String data = new String(decodedData);

                        String path = json.getString("path");

                        if (data == null || path == null) {
                            return res.json(this.error("Invalid JSON format.")).status(400);
                        }

                        FileUtils.write(Path.of(home, path), data, false);

                        Logger.Log("[FileSystem] Successfully wrote file to " + path + ".");
                        
                        return res.json("{\"success\": true, \"refresh\": true}");
                    }

                    if (json.has("path")) {
                        String path = json.getString("path");

                        if (path == null) {
                            return res.json(this.error("Invalid JSON format.")).status(400);
                        }

                        String data = FileUtils.read(Path.of(home, path));

                        Logger.Log("[FileSystem] Successfully deleted file at " + path + ".");

                        String encodedData = Base64.getEncoder().encodeToString(data.getBytes());

                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("data", encodedData);

                        return res.json(response);
                    }

                    return res.json(this.error("Invalid JSON format.")).status(400);
                }
            }));

            HashMap<String, Constructor> customRoutes = new HashMap<>();
            for (String route : customRoutes.keySet()) {
                server.route(new Route(route, customRoutes.get(route)));
            }

            server.listen();

            String ip_addr = IP.getIP();
            System.out.println("Server is listening on port " + server.getPort() + "! IP: " + ip_addr);
        } catch (IOException e) {
            System.out.println("Failed to start server!");
            e.printStackTrace();
        }

        updateStatus("Network Communication", "Connected");
    }

    private PathCallback legacyPathCallback = (points -> {
        Logger.Log("[LEGACY_PATHING] Received new autonomous path (default callback).");
    });

    private Callback pathCallback = (() -> {
        Logger.Log("[PATHING] Received new autonomous path (default callback).");
    });

    private String currentAutoPath = "";

    /**
     * This method is the legacy version of waitForAutonomousPath. It is only used for backwards compatibility.
     * @param callback The callback to be called when a new autonomous path is received.
     * @return The CommunicationManager instance.
     */
    @Deprecated(since = "2023.2.1")
    public CommunicationManager legacyWaitForAutonomousPath(PathCallback callback) {
        legacyPathCallback = callback;
        Logger.Log("[PATHING] Registered new legacy autonomous path callback.");
        return this;
    }

    /**
     * This method is the new version of waitForAutonomousPath. It is used for the new autonomous pathing system.
     * @param callback The callback to be called when a new autonomous path is received.
     * @return The CommunicationManager instance.
     */
    public CommunicationManager waitForAutonomousPath(Callback callback) {
        pathCallback = callback;
        Logger.Log("[PATHING] Registered new autonomous path callback.");
        return this;
    }

    /**
     * This method is used to read a json file and parse it into a TimePoint array.
     * @param json The json object to read.
     * @param toLastMovementPoint Whether or not to only parse up to the last movement point, or the full timeline.
     * @return The parsed TimePoint array.
     */
    public TimePoint[] readAndParsePath(JSONObject json, boolean toLastMovementPoint) {
        JSONArray timeline = json.getJSONArray("generatedTimeline");

        TimePoint[] points = new TimePoint[timeline.length()];
        int lastMovementIndex = -1;
        for (int i = 0; i < timeline.length(); i++) {
            JSONArray point = timeline.getJSONArray(i);

            TimePoint timePoint = new TimePoint();

            try {
                timePoint.parseFromJSON(point);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            points[i] = timePoint;

            if (timePoint.driving) {
                lastMovementIndex = i;
            }
        }

        if (toLastMovementPoint && lastMovementIndex > -1) {
            TimePoint[] newPoints = new TimePoint[lastMovementIndex + 1];
            System.arraycopy(points, 0, newPoints, 0, lastMovementIndex + 1);
            points = newPoints;
        }

        return points;

        // Logger.Log("Read through each point.");

        // pathCallback.onPathComplete(points);

        // Logger.Log("Finished reading the path.");

        // return points.length;
    }

    /**
     * Update the status of a service.
     * @param service The service to update.
     * @param status The new status of the service.
     * @return The CommunicationManager instance.
     */
    public CommunicationManager updateStatus(String service, String status) {
        updateInfo("Status", service, status);
        return this;
    }

    public CommunicationManager updateSwerveInfo(SwerveDrive swerveGroup) {
        final String name = "swerve_drive";
        final String groupName = EncodingUtil.encodeURIComponent(swerveGroup.getBaseName());

        try {
            updateInfo(name, "swerve_name", groupName);
        } catch (Exception e) {
            //just wait and try again later, since the server is probably not ready yet.
            return this;
        }

        SwerveModule[] modules = swerveGroup.getModules();

        for (int i = 0; i < modules.length; i++) {
            SwerveModule module = modules[i];

            updateInfo(name, "module_" + i + "/last_angle", module.getDesiredAngle().getDegrees());
            updateInfo(name, "module_" + i + "/speed", module.getSpeed().get(Type.METERS));
            updateInfo(name, "module_" + i + "/velocity_ms", module.getState().speedMetersPerSecond);
            updateInfo(name, "module_" + i + "/rotation", module.getAngle().getDegrees());
        }

        Pose2d swervePose = swerveGroup.getPose();

        updateInfo(name, "pose/x", swervePose.getX());
        updateInfo(name, "pose/y", swervePose.getY());
        updateInfo(name, "pose/angle", swervePose.getRotation().getDegrees());

        return this;
    }

    public CommunicationManager updateSpeedControllerInfo(ControllerGroup group) {
        String name = group.getName();

        push();
        if (group.isCombination()) {
            String[] subGroupNames = group.getSubGroups();

            updateInfo(SpeedControllerKey, name + "/hasSubGroup", true);
            updateInfo(SpeedControllerKey, name + "/subGroupNames", subGroupNames);

            updateInfo(SpeedControllerKey, name + "/AvgSpeed", group.getAverageSpeed());

            for (String subGroup : subGroupNames) {
                double avgSpeed = group.getAverageSpeed(subGroup);
                updateInfo(SpeedControllerKey, name + "/" + subGroup + "/AvgSpeed", avgSpeed);
                updateInfo(SpeedControllerKey, name + "/" + subGroup + "/Name", name);
                for (int port : group.subgroupSpeeds(subGroup).keySet()) {
                    updateInfo(SpeedControllerKey, name + "/" + subGroup + "/Controller_" + port + "_Speed", group.subgroupSpeeds(subGroup).get(port));
                }
            }
        } else {
            double avgSpeed = group.getAverageSpeed();
            updateInfo(SpeedControllerKey, name + "/hasSubGroup", false);
            updateInfo(SpeedControllerKey, name + "/AvgSpeed", avgSpeed);
            updateInfo(SpeedControllerKey, name + "/Name", name);
            for (int i = 0; i < group.getControllers().length; i++) {
                ControllerGroup.SpeedController controller = group.getControllers()[i];
                updateInfo(SpeedControllerKey, name + "/Controller_" + controller.getPort() + "_Speed", controller.getSpeed());
            }
        }
        pop();

        return this;
    }

    public CommunicationManager updateMotorControllerInfo(MotorController controller, String name) {
        double speed = controller.get();
        
        updateInfo(SpeedControllerKey, name + "/hasSubGroup", false);
        updateInfo(SpeedControllerKey, name + "/AvgSpeed", speed);
        updateInfo(SpeedControllerKey, name + "/Name", name);
        updateInfo(SpeedControllerKey, name + "/Controller_0_Speed", speed);

        return this;
    }

    private boolean pushingLargeAmount = false;

    private void push() {
        pushingLargeAmount = true;
    }

    private void pop() {
        pushingLargeAmount = false;
    }

    public CommunicationManager updateInfo(String tab, String key, String value) {
        if (createdTitles.contains(f(tab, key))) {
            ((StringPublisher) entries.get(f(tab, key))).set(value);
        } else {
            StringPublisher pub = table.getStringTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, String[] list) {
        String[] encodedList = new String[list.length];

        for (int i = 0; i < list.length; i++) {
            encodedList[i] = EncodingUtil.encodeURIComponent(list[i]);
        }

        String value = "[\"" + String.join("\",\"", encodedList) + "\"]";

        if (createdTitles.contains(f(tab, key))) {
            ((StringPublisher) entries.get(f(tab, key))).set(value);
        } else {
            StringPublisher pub = table.getStringTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, double value) {
        if (createdTitles.contains(f(tab, key))) {
            ((DoublePublisher) entries.get(f(tab, key))).set(value);
        } else {
            DoublePublisher pub = table.getDoubleTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, int value) {
        if (createdTitles.contains(f(tab, key))) {
            ((IntegerPublisher) entries.get(f(tab, key))).set(value);
        } else {
            IntegerPublisher pub = table.getIntegerTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, boolean value) {
        if (createdTitles.contains(f(tab, key))) {
            ((BooleanPublisher) entries.get(f(tab, key))).set(value);
        } else {
            BooleanPublisher pub = table.getBooleanTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, byte[] value) {
        String toBeUpdated = "";

        for (byte b : value) {
            toBeUpdated += (char) b;
        }

        updateInfo(tab, key, toBeUpdated);

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, double[] value) {
        if (createdTitles.contains(f(tab, key))) {
            ((DoubleArrayPublisher) entries.get(f(tab, key))).set(value);
        } else {
            DoubleArrayPublisher pub = table.getDoubleArrayTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, long[] value) {
        if (createdTitles.contains(f(tab, key))) {
            ((IntegerArrayPublisher) entries.get(f(tab, key))).set(value);
        } else {
            IntegerArrayPublisher pub = table.getIntegerArrayTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }

        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, boolean[] value) {
        if (createdTitles.contains(f(tab, key))) {
            ((BooleanArrayPublisher) entries.get(f(tab, key))).set(value);
        } else {
            BooleanArrayPublisher pub = table.getBooleanArrayTopic(f(tab, key)).publish();
            entries.put(f(tab, key), pub);
            pub.set(value);
        }
        
        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    private Field2d field = null;

    public boolean fieldExists() {
        return field != null;
    }

    public void createField() {
        field = new Field2d();
        if (!pushingLargeAmount) SmartDashboard.putData("Field", field);
    }

    public void updateRobotPose(Pose2d pose) {
        if (!fieldExists()) return;
        field.setRobotPose(pose);
        if (!pushingLargeAmount) SmartDashboard.updateValues();
    }

    public void updateFieldObjectPose(String name, Pose2d pose) {
        if (!fieldExists()) return;
        field.getObject(name).setPose(pose);
        if (!pushingLargeAmount) SmartDashboard.updateValues();
    }
    
    /**
     * Returns the NetworkTableEntry for a given tab and key.
     * @param tab The tab to get the entry from.
     * @param key The key to get the entry from.
     * @return The NetworkTableEntry for the given tab and key.
     */
    public NetworkTableEntry get(String tab, String key) {
        return table.getEntry(f(tab, key));
    }

    /**
     * Closes all network communications.
     */
    public void closeComms() {
        for (String key : entries.keySet()) {
            entries.get(key).close();
        }
    }

    /**
     * Sets the API server notifications to be enabled or disabled.
     * @param enabled
     */
    public static void setServerNotifications(boolean enabled) {
        Route.setServerNotifications(enabled);
    }

    //Just quicker to type than (tab + "." + key)
    //Also can modify this in the future.
    private final String t_k_separator = "/";
    private String f(String tab, String key) {
        if (key == "") return tab;
        return tab + t_k_separator + key;
    }

}
