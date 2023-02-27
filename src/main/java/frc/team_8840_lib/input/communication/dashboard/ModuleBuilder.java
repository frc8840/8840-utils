package frc.team_8840_lib.input.communication.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;

import frc.team_8840_lib.input.communication.CommunicationManager;
import frc.team_8840_lib.input.communication.dashboard.components.DashboardComponent;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.Route;
import com.sun.net.httpserver.HttpExchange;

public class ModuleBuilder {
    private static ModuleBuilder instance;

    public static ModuleBuilder getInstance() {
        if (instance == null)
            instance = new ModuleBuilder();
        
        return instance;
    }

    public ModuleBuilder() {
        instance = this;
    }

    private HashMap<String, DashboardModule> modules = new HashMap<String, DashboardModule>();

    public static void registerModule(String name, DashboardModule module) {
        getInstance().modules.put(name, module);
    }

    public static Constructor getConstructor() {
        return (new Constructor() {
            @Override
            public Route.Resolution finish(HttpExchange req, Route.Resolution res) {
                if (!req.getRequestMethod().toLowerCase().equals("get")) {
                    return res.json(this.error("This is a GET request only."));
                }

                JSONObject parent = new JSONObject();

                JSONArray names = new JSONArray();

                for (String key : getInstance().modules.keySet()) {
                    ArrayList<DashboardComponent> components = getInstance().modules.get(key).getComponents();

                    names.put(key);

                    JSONObject module_parent = new JSONObject();

                    JSONArray module = new JSONArray();

                    for (DashboardComponent component : components) {
                        JSONObject componentJSON = new JSONObject();

                        componentJSON.put("innerHTML", component.getInnerHTML());
                        componentJSON.put("tag", component.getTag());
                        
                        JSONObject keys = new JSONObject();

                        for (Entry<String, String> entry : component.getKeys().entrySet()) {
                            keys.put(entry.getKey(), entry.getValue());
                        }

                        componentJSON.put("keys", keys);

                        module.put(componentJSON);
                    }

                    module_parent.put("components", components);
                    module_parent.put("number", components.size());

                    parent.put(key, module_parent);
                }

                //Small info object.
                JSONObject infoObject = new JSONObject();

                infoObject.put("number", getInstance().modules.size());
                infoObject.put("names", names);

                parent.put(".info", infoObject);

                return res.json(parent.toString());
            }
        });
    }
}
