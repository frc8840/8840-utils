package frc.team_8840_lib.utils.http;

import com.sun.net.httpserver.HttpExchange;

import java.util.HashMap;

public class Constructor {
    public Constructor() {}

    public Route.Resolution finish(HttpExchange exchange, Route.Resolution res) {
        return res.status(404);
    }

    public HashMap<String, String> parseQuery(String query) {
        HashMap<String, String> map = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    public String error(String msg) {
        return "{\"success\": false, \"message\": \"" + msg + "\"}";
    }
}
