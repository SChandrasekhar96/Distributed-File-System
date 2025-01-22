package com.dfs;

import java.io.*;
import java.net.*;

public class Server {
    private static final String FILES_DIRECTORY = "server_files";
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        int port = 5000;
        File directory = new File(FILES_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port);
            Thread shutdownListener = new Thread(Server::listenForShutdown);
            shutdownListener.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");
                Thread clientHandler = new Thread(() -> handleClient(clientSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            String command = in.readUTF();
            if (command.startsWith("UPLOAD")) {
                handleFileUpload(in, command.substring(7));
            } else if (command.startsWith("DOWNLOAD")) {
                handleFileDownload(out, command.substring(9));
            } else if (command.equals("LIST")) {
                listFiles(out);
            } else {
                out.writeUTF("Invalid command!");
            }

        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static void handleFileUpload(DataInputStream in, String fileName) throws IOException {
        File file = new File(FILES_DIRECTORY + "/" + fileName);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
            System.out.println("File " + fileName + " uploaded successfully!");
        }
    }

    private static void handleFileDownload(DataOutputStream out, String fileName) throws IOException {
        File file = new File(FILES_DIRECTORY + "/" + fileName);
        if (file.exists()) {
            out.writeUTF("READY");
            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } else {
            out.writeUTF("File not found!");
        }
    }

    private static void listFiles(DataOutputStream out) throws IOException {
        File directory = new File(FILES_DIRECTORY);
        File[] files = directory.listFiles();

        if (files != null && files.length > 0) {
            out.writeUTF("Files available on the server:");
            for (File file : files) {
                out.writeUTF(file.getName()); 
            }
        } else {
            out.writeUTF("No files available on the server.");
        }
        out.writeUTF("END_OF_LIST");
    }
    private static void listenForShutdown() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.  println("Enter 'shutdown' to stop the server.");
                String command = reader.readLine();
                if (command.equalsIgnoreCase("shutdown")) {
                    System.out.println("Shutting down the server...");
                    serverSocket.close();
                    System.exit(0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
