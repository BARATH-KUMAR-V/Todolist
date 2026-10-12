
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TodoApp {
    static class Task {
        int id;
        String title;
        boolean done;

        Task(int id, String title) {
            this.id = id;
            this.title = title;
            this.done = false;
        }
    }

    static List<Task> tasks = new ArrayList<>();
    static int nextId = 1;

    static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static void handle(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            String body = new String(
                ex.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Map<String, String> form = new HashMap<>();
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8);
                String value = parts.length > 1
                    ? URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8)
                    : "";
                form.put(key, value);
            }

            String action = form.getOrDefault("action", "");
            String idText = form.getOrDefault("id", "");

            if (action.equals("add")) {
                String title = form.getOrDefault("title", "").trim();
                if (!title.isEmpty() && title.length() <= 200) {
                    tasks.add(new Task(nextId++, title));
                }
            } else {
                try {
                    int id = Integer.parseInt(idText);
                    Task task = tasks.stream()
                        .filter(t -> t.id == id)
                        .findFirst().orElse(null);

                    if (task != null && action.equals("toggle")) {
                        task.done = !task.done;
                    } else if (task != null && action.equals("delete")) {
                        tasks.remove(task);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            ex.getResponseHeaders().set("Location", "/");
            ex.sendResponseHeaders(303, -1);
            ex.close();
            return;
        }

        StringBuilder html = new StringBuilder("""
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>Java To-Do List</title>
              <style>
                body {
                  font-family: Arial, sans-serif;
                  background: #eef2f7;
                  margin: 0;
                  padding: 40px 12px;
                }
                main {
                  max-width: 520px;
                  margin: auto;
                  padding: 24px;
                  background: white;
                  border-radius: 12px;
                  box-shadow: 0 4px 15px #0001;
                }
                h1 { color: #243b53; }
                input { padding: 10px; }
                button {
                  padding: 10px;
                  border: 0;
                  border-radius: 5px;
                  background: #2563eb;
                  color: white;
                  cursor: pointer;
                }
                .task {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 10px;
                  padding: 12px 0;
                  border-bottom: 1px solid #ddd;
                }
                .done { text-decoration: line-through; color: gray; }
                .delete { background: #dc2626; }
                .task form { display: inline; }
                .title { overflow-wrap: anywhere; }
              </style>
            </head>
            <body><main>
              <h1>My To-Do List</h1>
              <form method="post">
                <input type="hidden" name="action" value="add">
                <input name="title" maxlength="200"
                       placeholder="Enter a task" required>
                <button type="submit">Add</button>
              </form>
              <hr>
            """);

        if (tasks.isEmpty()) {
            html.append("<p>No tasks yet. Add your first task!</p>");
        }

        for (Task t : tasks) {
            html.append("<div class='task'><span class='title ")
                .append(t.done ? "done" : "")
                .append("'>")
                .append(escape(t.title))
                .append("</span><span>");

            html.append("<form method='post'>")
                .append("<input type='hidden' name='id' value='")
                .append(t.id)
                .append("'><button name='action' value='toggle'>")
                .append(t.done ? "Undo" : "Complete")
                .append("</button></form> ");

            html.append("<form method='post'>")
                .append("<input type='hidden' name='id' value='")
                .append(t.id)
                .append("'><button class='delete' name='action' value='delete'>")
                .append("Delete</button></form></span></div>");
        }

        html.append("</main></body></html>");

        byte[] response = html.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set(
            "Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(200, response.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(response);
        }
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(
            new InetSocketAddress("0.0.0.0", 8000), 0);

        server.createContext("/", TodoApp::handle);
        server.start();

        System.out.println("To-Do List running at http://localhost:8000");
    }
}