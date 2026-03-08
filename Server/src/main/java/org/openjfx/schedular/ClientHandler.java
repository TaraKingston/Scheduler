package org.openjfx.schedular;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private ScheduleManager manager;

    public ClientHandler(Socket clientSocket, ScheduleManager manager) {
        this.clientSocket = clientSocket;
        this.manager      = manager;
    }

    @Override
    public void run() {
        try (
                BufferedReader in  = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter    out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                String response = processMessage(message);
                out.println(response);
                if (message.trim().equalsIgnoreCase("STOP")) break;
            }
        } catch (Exception e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private String processMessage(String message) {
        try {
            String[] parts  = message.split("\\|");
            String   action = parts[0].trim().toUpperCase();

            switch (action) {
                case "ADD":
                    if (parts.length < 5) return "ERROR: ADD needs module, room, day, time.";
                    return manager.addLecture(parts[1], parts[2], parts[3], parts[4]);

                case "REMOVE":
                    if (parts.length < 4) return "ERROR: REMOVE needs module, day, time.";
                    return manager.removeLecture(parts[1], parts[2], parts[3]);

                case "DISPLAY":
                    return manager.getSchedule();

                case "STOP":
                    return "TERMINATE";

                default:
                    throw new IncorrectActionException(
                            "IncorrectActionException: '" + action +
                                    "' is not valid. Use: ADD, REMOVE, DISPLAY, STOP.");
            }

        } catch (IncorrectActionException e) {
            System.out.println("Exception thrown: " + e.getMessage());
            return "EXCEPTION: " + e.getMessage();
        }
    }
}
