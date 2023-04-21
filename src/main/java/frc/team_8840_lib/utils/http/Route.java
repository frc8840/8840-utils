package frc.team_8840_lib.utils.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import frc.team_8840_lib.utils.http.html.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

import org.json.JSONObject;

public class Route implements HttpHandler {

    private String path;
    private Constructor callback;

    public Route(String path, Constructor callback) {
        this.path = path;
        this.callback = callback;
    }

    public String getPath() {
        return path;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        Resolution res = this.callback.finish(exchange, new Resolution());
        String body = res.getBody();
        int status = res.getStatus();

        System.out.println(exchange.getRequestMethod().toUpperCase() + " Request \"" + this.getPath() + "\" " + status);

        exchange.sendResponseHeaders(status, body.length());
        OutputStream os = exchange.getResponseBody();
        os.write(body.getBytes());
        os.flush();
        os.close();
    }

    public static enum ContentType {
        HTML, JSON, TEXT
    }

    public static class Resolution {
        private int status;
        private Element body;
        private String json;
        private String text;
        private HashMap<String, String> headers;
        private ContentType contentType;

        public Resolution() {
            status = 200;
            body = Element.CreatePage(Element.CreateHead(), Element.CreateBody());
            headers = new HashMap<>();
            contentType = ContentType.HTML;
        }

        public Resolution(int status, Element body) {
            this.status = status;
            this.body = body;
            this.headers = new HashMap<>();
            contentType = ContentType.HTML;
        }

        public Resolution send(Element body) {
            this.body = body;
            contentType = ContentType.HTML;
            setHeader("Content-Type", "text/html");
            status(200);
            return this;
        }

        public Resolution json(String json) {
            this.json = json;
            this.contentType = ContentType.JSON;
            setHeader("Content-Type", "application/json");
            status(200);
            return this;
        }

        public Resolution json(JSONObject json) {
            this.json = json.toString();
            this.contentType = ContentType.JSON;
            setHeader("Content-Type", "application/json");
            status(200);
            return this;
        }

        public Resolution text(String text) {
            this.text = text;
            this.contentType = ContentType.TEXT;
            setHeader("Content-Type", "text/plain");
            status(200);
            return this;
        }

        public Resolution setHeader(String header, String value) {
            headers.put(header, value);
            return this;
        }

        public Resolution status(int status) {
            this.status = status;
            return this;
        }

        public Resolution wsUpgradeConnection() {
            return null;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return contentType == ContentType.HTML ? body.getHTML() : (contentType == ContentType.JSON ? json : text);
        }

        public HashMap<String, String> getHeaders() {
            return headers;
        }
    }
}
