package com.example.restapi;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashMap;
import java.util.Map;

public class TaskServer {
    private static final ConcurrentHashMap<Integer, String> tasks = new ConcurrentHashMap<>();
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private static final int PORT = 8081;
    private static final Map<String, String> users = new HashMap<>(); // username -> password

    public static void main(String[] args) throws IOException {
        // Add a test user
        users.put("admin", "password123");

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/auth/login", TaskServer::handleLogin);
        server.createContext("/tasks", TaskServer::handleRequest);
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port " + PORT);
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, new JSONObject().put("error", "Method not allowed").toString());
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());
        JSONObject json = new JSONObject(body);
        String username = json.getString("username");
        String password = json.getString("password");

        if (users.containsKey(username) && users.get(username).equals(password)) {
            String token = JwtUtil.generateToken(username);
            sendResponse(exchange, 200, new JSONObject()
                .put("token", token)
                .put("message", "Login successful")
                .toString());
        } else {
            sendResponse(exchange, 401, new JSONObject()
                .put("error", "Invalid credentials")
                .toString());
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        // Check for JWT token in Authorization header
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendResponse(exchange, 401, new JSONObject()
                .put("error", "No token provided")
                .toString());
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix
        if (!JwtUtil.validateToken(token)) {
            sendResponse(exchange, 401, new JSONObject()
                .put("error", "Invalid token")
                .toString());
            return;
        }

        String method = exchange.getRequestMethod();
        String response = "";
        int statusCode = 200;

        if ("GET".equals(method)) {
            JSONArray jsonArray = new JSONArray();
            tasks.forEach((id, task) -> {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("task", task);
                jsonArray.put(obj);
            });
            response = jsonArray.toString();
            if (response.equals("[]")) {
                response = "{\"tasks\": []}";
            }
        } else if ("POST".equals(method)) {
            int id = idCounter.getAndIncrement();
            String body = new String(exchange.getRequestBody().readAllBytes());
            JSONObject json = new JSONObject(body);
            tasks.put(id, json.getString("task"));
            JSONObject responseJson = new JSONObject();
            responseJson.put("message", "Task added successfully");
            responseJson.put("id", id);
            response = responseJson.toString();
        } else if ("PUT".equals(method)) {
            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                if (tasks.containsKey(id)) {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    JSONObject json = new JSONObject(body);
                    tasks.put(id, json.getString("task"));
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("message", "Task updated successfully");
                    response = responseJson.toString();
                } else {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "Task not found");
                    response = responseJson.toString();
                    statusCode = 404;
                }
            }
        } else if ("DELETE".equals(method)) {
            String[] parts = exchange.getRequestURI().getPath().split("/");
            if (parts.length == 3) {
                int id = Integer.parseInt(parts[2]);
                if (tasks.remove(id) != null) {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("message", "Task deleted successfully");
                    response = responseJson.toString();
                } else {
                    JSONObject responseJson = new JSONObject();
                    responseJson.put("error", "Task not found");
                    response = responseJson.toString();
                    statusCode = 404;
                }
            }
        } else {
            JSONObject responseJson = new JSONObject();
            responseJson.put("error", "Method not allowed");
            response = responseJson.toString();
            statusCode = 405;
        }

        sendResponse(exchange, statusCode, response);
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        byte[] responseBytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
