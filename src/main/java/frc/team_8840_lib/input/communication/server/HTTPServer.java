package frc.team_8840_lib.input.communication.server;

import com.sun.net.httpserver.HttpServer;
import frc.team_8840_lib.utils.http.Route;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

public class HTTPServer {
    private int port;

    private HttpServer server;

    private HashMap<String, Route> routes;

    public HTTPServer(int port) throws IOException {
        this.port = port;

        server = HttpServer.create(new InetSocketAddress(port), 0);

        routes = new HashMap<>();
    }

    public void route(Route handler) {
        String route = handler.getPath();
        server.createContext(route, handler);
        routes.put(route, handler);
    }

    public boolean routeExists(String route) {
        return routes.containsKey(route);
    }

    public int getPort() {
        return port;
    }

    public void listen() {
        server.setExecutor(null);
        server.start();
    }
}
