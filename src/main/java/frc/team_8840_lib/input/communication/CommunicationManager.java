package frc.team_8840_lib.input.communication;

import com.fasterxml.jackson.core.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.wpilibj.shuffleboard.ComplexWidget;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.controllers.SwerveGroup;
import frc.team_8840_lib.info.console.Logger;
import frc.team_8840_lib.input.communication.server.HTTPServer;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;
import frc.team_8840_lib.utils.http.html.Element;
import frc.team_8840_lib.utils.http.html.EncodingUtil;
import frc.team_8840_lib.utils.pathplanner.PathCallback;
import frc.team_8840_lib.utils.pathplanner.TimePoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class CommunicationManager {
    private static CommunicationManager instance;

    public static final String SpeedControllerKey = "SpeedController";

    public static CommunicationManager getInstance() {
        return instance;
    }

    public static void init() {
        instance = new CommunicationManager(true, false);

        NetworkTableInstance.getDefault().flush(); //idk why this is here but yeah
    }

    private final boolean usingShuffleboard;
    private final boolean useBoth;

    private ArrayList<String> createdTitles;
    private HashMap<String, NetworkTableEntry> entries;

    private HTTPServer server;

    private CommunicationManager(boolean usingShuffleboard, boolean useBoth) {
        instance = this;

        this.usingShuffleboard = usingShuffleboard;
        this.useBoth = useBoth;

        createdTitles = new ArrayList<>();
        entries = new HashMap<>();

        try {
            /**
             * According to rule R704 and Table 9-5 Open FMS Ports, ports 5800-5810 are open to UDP/TCP traffic.
             * This means that we are allowed to host a server on any of the ports in this range, so we choose 5805.
             * */
            server = new HTTPServer(5805);

            server.route(new Route("/", new Constructor() {
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

            server.route(new Route("/auto_path", new Constructor() {
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
                        String selection = json.getString("selection");

                        if (selection == null || selection.equals("")) {
                            Logger.Log("Received empty selection, clearing current auto path.");
                            currentAutoPath = "";
                            pathCallback.onPathComplete(new TimePoint[0]);
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

                                    int points = readAndParsePath(fileJson);

                                    if (points == -1) {
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

                    int points = readAndParsePath(json);

                    if (points == -1) {
                        Logger.Log("There was an error while parsing the JSON.");
                        return res.json(this.error("There was an issue parsing the JSON.")).status(500);
                    }

                    currentAutoPath = autoName;

                    return res.json("{\"success\": true, \"message\": \"Received path with " + points + " data points.\"}");
                }
            }));

            server.listen();

            String ip_addr = IP.getIP();
            System.out.println("Server is listening on port " + server.getPort() + "! IP: " + ip_addr);
        } catch (IOException e) {
            System.out.println("Failed to start server!");
            e.printStackTrace();
        }

        updateStatus("Network Communication", "Connected");
    }

    private PathCallback pathCallback = (points -> {
        Logger.Log("Received new autonomous path (default callback).");
    });

    private String currentAutoPath = "";

    public void waitForAutonomousPath(PathCallback callback) {
        pathCallback = callback;
        Logger.Log("Registered new autonomous path callback.");
    }

    private int readAndParsePath(JSONObject json) {
        JSONArray timeline = json.getJSONArray("generatedTimeline");

        TimePoint[] points = new TimePoint[timeline.length()];
        for (int i = 0; i < timeline.length(); i++) {
            JSONArray point = timeline.getJSONArray(i);

            TimePoint timePoint = new TimePoint();

            try {
                timePoint.parseFromJSON(point);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }

            points[i] = timePoint;
        }

        Logger.Log("Read through each point.");

        pathCallback.onPathComplete(points);

        Logger.Log("Finished reading the path.");

        return points.length;
    }

    public void updateStatus(String service, String status) {
        updateInfo("Status", service, status);
    }

    public CommunicationManager updateSwerveInfo(SwerveGroup swerveGroup) {
        final String name = "swerve_drive";
        final String groupName = EncodingUtil.encodeURIComponent(swerveGroup.getName());

        updateInfo(name, "swerve_name", groupName);

        swerveGroup.loop(((module, index) -> {
            updateInfo(name, "module_" + index + "_last_angle", module.getLastAngle());
            updateInfo(name, "module_" + index + "_speed", module.getSpeed());
            updateInfo(name, "module_" + index + "_velocity_ms", module.getState().speedMetersPerSecond);
            updateInfo(name, "module_" + index + "_rotation", module.getRotation().getDegrees());
        }));

        return this;
    }

    public CommunicationManager updateSpeedControllerInfo(ControllerGroup group) {
        String name = group.getName();
        if (group.isCombination()) {
            String[] subGroupNames = group.getSubGroups();

            for (String subGroup : subGroupNames) {
                double avgSpeed = group.getAverageSpeed(subGroup);
                updateInfo("SpeedControllers", name + "_" + subGroup + "_AvgSpeed", avgSpeed);
            }
        } else {
            double avgSpeed = group.getAverageSpeed();
            updateInfo("SpeedController", name + "_AvgSpeed", avgSpeed);
        }

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, String value) {
        if (usingShuffleboard || useBoth) {
            if (createdTitles.contains(f(tab, key))) {
                entries.get(f(tab, key)).setValue(value);
            } else {
                NetworkTableEntry newEntry = Shuffleboard.getTab(tab).add(key, value).getEntry();
                entries.put(f(tab, key), newEntry);
            }
            Shuffleboard.update();
        }
        if (!usingShuffleboard || useBoth) {
            SmartDashboard.putString(key, value);
            SmartDashboard.updateValues();
        }
        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, double value) {
        if (usingShuffleboard || useBoth) {
            if (createdTitles.contains(f(tab, key))) {
                entries.get(f(tab, key)).setValue(value);
            } else {
                NetworkTableEntry newEntry = Shuffleboard.getTab(tab).add(key, value).getEntry();
                entries.put(f(tab, key), newEntry);
            }
            Shuffleboard.update();
        }
        if (!usingShuffleboard || useBoth) {
            SmartDashboard.putNumber(key, value);
            SmartDashboard.updateValues();
        }
        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, int value) {
        if (usingShuffleboard || useBoth) {
            if (createdTitles.contains(f(tab, key))) {
                entries.get(f(tab, key)).setValue(value);
            } else {
                NetworkTableEntry newEntry = Shuffleboard.getTab(tab).add(key, value).getEntry();
                entries.put(f(tab, key), newEntry);
            }
            Shuffleboard.update();
        }
        if (!usingShuffleboard || useBoth) {
            SmartDashboard.putNumber(key, value);
            SmartDashboard.updateValues();
        }
        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager updateInfo(String tab, String key, boolean value) {
        if (usingShuffleboard || useBoth) {
            if (createdTitles.contains(f(tab, key))) {
                entries.get(f(tab, key)).setValue(value);
            } else {
                NetworkTableEntry newEntry = Shuffleboard.getTab(tab).add(key, value).getEntry();
                entries.put(f(tab, key), newEntry);
            }
            Shuffleboard.update();
        }
        if (!usingShuffleboard || useBoth) {
            SmartDashboard.putBoolean(key, value);
            SmartDashboard.updateValues();
        }
        if (!createdTitles.contains(f(tab, key))) createdTitles.add(f(tab, key));

        return this;
    }

    public CommunicationManager putSendable(String tab, String key, Sendable value) {
        if (usingShuffleboard || useBoth) {
            if (createdTitles.contains(f(tab, key))) {
                entries.get(f(tab, key)).setValue(value);
            }
            Shuffleboard.update();
        }
        if (!usingShuffleboard || useBoth) {
            SmartDashboard.putData(key, value);
            SmartDashboard.updateValues();
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
        SmartDashboard.putData("Field", field);
    }

    public void updateRobotPose(Pose2d pose) {
        if (!fieldExists()) return;
        field.setRobotPose(pose);
        SmartDashboard.updateValues();
    }

    public void updateFieldObjectPose(String name, Pose2d pose) {
        if (!fieldExists()) return;
        field.getObject(name).setPose(pose);
        SmartDashboard.updateValues();
    }

    public NetworkTableEntry get(String tab, String key) {
        return entries.getOrDefault(f(tab, key), null);
    }

    //Just quicker to type than (tab + "." + key)
    //Also can modify this in the future.
    private final String t_k_separator = ".";
    private String f(String tab, String key) {
        if (key == "") return tab;
        return tab + t_k_separator + key;
    }

}
