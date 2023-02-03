package frc.team_8840_lib.input.communication.dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

import frc.team_8840_lib.input.communication.dashboard.components.DashboardComponent;
import frc.team_8840_lib.utils.http.Constructor;
import frc.team_8840_lib.utils.http.Route;
import com.sun.net.httpserver.HttpExchange;

public class ModuleBuilder {
    private static ModuleBuilder instance;

    public static ModuleBuilder getInstance() {
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
                if (req.getRequestMethod() != "GET") {
                    return res.json(this.error("This is a GET request only."));
                }

                JSONObject parent = new JSONObject();

                for (String key : getInstance().modules.keySet()) {
                    ArrayList<DashboardComponent> components = getInstance().modules.get(key).getComponents();

                    JSONObject module = new JSONObject();

                    for (DashboardComponent component : components) {
                        JSONObject componentJSON = new JSONObject();

                        componentJSON.put("innerHTML", component.getInnerHTML());
                        componentJSON.put("tag", component.getTag());
                        
                        JSONObject keys = new JSONObject();

                        for (Entry<String, String> entry : component.getKeys().entrySet()) {
                            keys.put(entry.getKey(), entry.getValue());
                        }

                        componentJSON.put("keys", keys);
                    }

                    parent.put(key, module);
                }

                return res.json(parent.toString());
            }
        });
    }
}
