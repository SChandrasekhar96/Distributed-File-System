package com.dfs1;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class DistributedServer {
    private static final int THREAD_POOL_SIZE = 10;
    private static boolean[] serverStatus = {false, false, false};
    private static ServerSocket[] serverSockets = new ServerSocket[3];
    private static ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public static void startServer(int port, String directory, int serverIndex) {
        System.out.println("Starting server on port " + port + "...");
        File dir = new File(directory);
        if (!dir.exists()) {
            boolean dirCreated = dir.mkdirs();
            if (!dirCreated) {
                System.out.println("Error creating directory: " + directory);
                return;
            }
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSockets[serverIndex] = serverSocket;
            serverStatus[serverIndex] = true;
            System.out.println("Server on port " + port + " started.");

            while (serverStatus[serverIndex]) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(() -> handleClient(clientSocket, directory));
                } catch (SocketException e) {
                    if (serverStatus[serverIndex]) {
                        System.out.println("Server socket closed unexpectedly.");
                    }
                }
            }

            System.out.println("Server on port " + port + " has shut down.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSockets[serverIndex] = null;
        }
    }

    private static void listenForCommands() {
        try (BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Type command to control servers:");
            System.out.println("startserver1, startserver2, startserver3");
            System.out.println("startall, shutdown1, shutdown2, shutdown3, shutdownall");

            while (true) {
                String command = consoleReader.readLine();

                if (command.equals("startall")) {
                    boolean anyStarted = false;
                    for (int i = 0; i < 3; i++) {
                        if (!serverStatus[i]) {
                            String directory = "server_files" + (i + 1);
                            int port = 5001 + i;
                            final int index = i;
                            new Thread(() -> startServer(port, directory, index)).start();
                            anyStarted = true;
                            System.out.println("Server " + (i + 1) + " started.");
                        }
                    }
                    if (!anyStarted) {
                        System.out.println("All servers are already running.");
                    }
                } else if (command.equals("shutdownall")) {
                    boolean anyShutdown = false;
                    for (int i = 0; i < 3; i++) {
                        if (serverStatus[i]) {
                            shutdownServer(i);
                            anyShutdown = true;
                        }
                    }
                    if (!anyShutdown) {
                        System.out.println("No servers are currently running.");
                    }
                } else if (command.startsWith("startserver")) {
                    int serverNumber = Integer.parseInt(command.substring(11)) - 1;
                    if (!serverStatus[serverNumber]) {
                        String directory = "server_files" + (serverNumber + 1);
                        int port = 5001 + serverNumber;
                        new Thread(() -> startServer(port, directory, serverNumber)).start();
                    } else {
                        System.out.println("Server " + (serverNumber + 1) + " is already running.");
                    }
                } else if (command.startsWith("shutdown")) {
                    try {
                        int serverNumber = Integer.parseInt(command.substring(8)) - 1;
                        if (serverStatus[serverNumber]) {
                            shutdownServer(serverNumber);
                        } else {
                            System.out.println("Server " + (serverNumber + 1) + " is not running.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid shutdown command! Please specify a valid server number (e.g., shutdown1, shutdown2, shutdown3).");
                    }
                } else {
                    System.out.println("Invalid command! Try again.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void shutdownServer(int serverIndex) {
        serverStatus[serverIndex] = false;
        System.out.println("Server " + (serverIndex + 1) + " is shutting down...");
        if (serverSockets[serverIndex] != null) {
            try {
                serverSockets[serverIndex].close();
            } catch (IOException e) {
                System.out.println("Error closing server socket for Server " + (serverIndex + 1) + ": " + e.getMessage());
            }
        }
    }

    private static void handleClient(Socket clientSocket, String directory) {
        try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            String command = in.readUTF();

            if (command.startsWith("UPLOAD")) {
                uploadFile(command.substring(7), in, directory, out);
            } else if (command.startsWith("DOWNLOAD")) {
                downloadFile(command.substring(9), out, directory);
            } else if (command.startsWith("DELETE")) {
                deleteFile(command.substring(7), out, directory);
            } else if (command.equals("LIST")) {
                listFiles(out, directory);
            } else {
                out.writeUTF("Invalid command!");
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        }
    }

    private static void uploadFile(String fileName, DataInputStream in, String directory, DataOutputStream out) throws IOException {
        File file = new File(directory, fileName);
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
            }
        }
        out.writeUTF("File uploaded successfully: " + fileName);
        System.out.println("File uploaded: " + fileName);
    }

    private static void downloadFile(String fileName, DataOutputStream out, String directory) throws IOException {
        File file = new File(directory, fileName);
        if (file.exists()) {
            out.writeUTF("READY");
            try (FileInputStream fileIn = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.writeUTF("Download complete: " + fileName);
        } else {
            out.writeUTF("File not found.");
        }
    }

    private static void deleteFile(String fileName, DataOutputStream out, String directory) throws IOException {
        File file = new File(directory, fileName);
        if (file.exists()) {
            if (file.delete()) {
                out.writeUTF("File deleted successfully: " + fileName);
                System.out.println("File deleted: " + fileName);
            } else {
                out.writeUTF("Failed to delete the file: " + fileName);
            }
        } else {
            out.writeUTF("File not found.");
        }
    }

    private static void listFiles(DataOutputStream out, String directory) throws IOException {
        File dir = new File(directory);
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            for (File file : files) {
                out.writeUTF(file.getName());
            }
        } else {
            out.writeUTF("No files available.");
        }
        out.writeUTF("END_OF_LIST");
    }

    public static void main(String[] args) {
        new Thread(() -> listenForCommands()).start();
    }
}
