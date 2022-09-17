package frc.team_8840_lib.input.communication;

import com.sun.net.httpserver.HttpExchange;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableValue;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.team_8840_lib.controllers.ControllerGroup;
import frc.team_8840_lib.input.communication.server.HTTPServer;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.IP;
import frc.team_8840_lib.utils.http.Route;
import frc.team_8840_lib.utils.http.html.Element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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

            server.listen();

            String ip_addr = IP.getIP();
            System.out.println("Server is listening on port " + server.getPort() + "! IP: " + ip_addr);
        } catch (IOException e) {
            System.out.println("Failed to start server!");
            e.printStackTrace();
        }

        updateStatus("Network Communication", "Connected");
    }

    public void updateStatus(String service, String status) {
        updateInfo("Status", service, status);
    }

    public void updateSpeedControllerInfo(ControllerGroup group) {
        String name = group.getName();
        if (group.isCombination()) {
            String[] subGroupNames = group.getSubGroups();

            for (String subGroup : subGroupNames) {
                double avgSpeed = group.getAverageSpeed(subGroup);
                updateInfo("SpeedControllers", name + "_" + subGroup + "_AvgSpeed", avgSpeed);
            }
        } else {
            double avgSpeed = group.getAverageSpeed();
            updateInfo("SpeedControllertabs", name + "_AvgSpeed", avgSpeed);
        }
    }

    public void updateInfo(String tab, String key, String value) {
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
    }

    public void updateInfo(String tab, String key, double value) {
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
    }

    public void updateInfo(String tab, String key, int value) {
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
    }

    public void updateInfo(String tab, String key, boolean value) {
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
    }

    public NetworkTableEntry get(String tab, String key) {
        return entries.getOrDefault(f(tab, key), null);
    }

    //Just quicker to type than (tab + "." + key)
    //Also can modify this in the future.
    private final String t_k_separator = ".";
    private String f(String tab, String key) {
        return tab + t_k_separator + key;
    }

}
