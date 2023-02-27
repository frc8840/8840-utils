package frc.team_8840_lib.input.communication.dashboard.components;

import java.util.HashMap;

import org.json.JSONObject;

public class DashboardComponent {
    private HashMap<String, String> keys = new HashMap<String, String>();

    public DashboardComponent() {
        
    }

    public String getInnerHTML() {
        return "";
    }

    public String getTag() {
        return "";
    }

    public HashMap<String, String> getKeys() {
        return keys;
    }

    public void addKey(String key, String value) {
        if (key == "style") {
            throw new IllegalArgumentException("Cannot use key 'style'! Use setInlineStyle() instead.");
        }
        
        keys.put(key.replace("__ignore__: ", ""), value);
    }

    public void setInlineStyle(JSONObject style) {
        addKey("__ignore__: style", style.toString());
    }
}
