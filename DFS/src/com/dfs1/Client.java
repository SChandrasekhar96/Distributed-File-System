package com.dfs1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    private JFrame frame;
    private JTextArea serverResponseArea;
    private JTextField fileNameField;

    private static final String[] SERVER_ADDRESSES = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};
    private static final int[] SERVER_PORTS = {5001, 5002, 5003};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        GUI();
    }

    private void GUI() {
        frame = new JFrame("Distributed File System - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        frame.setLayout(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        JButton uploadButton = new JButton("Upload File");
        JButton downloadButton = new JButton("Download File");
        JButton deleteButton = new JButton("Delete File");
        JButton listFilesButton = new JButton("List Files");

        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(listFilesButton);

        frame.add(buttonPanel, BorderLayout.NORTH);
        serverResponseArea = new JTextArea();
        serverResponseArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(serverResponseArea);
        frame.add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        fileNameField = new JTextField();
        fileNameField.setToolTipText("Enter file name...");
        bottomPanel.add(fileNameField, BorderLayout.CENTER);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        uploadButton.addActionListener(this::handleUpload);
        downloadButton.addActionListener(this::handleDownload);
        deleteButton.addActionListener(this::handleDelete);
        listFilesButton.addActionListener(this::handleListFiles);

        frame.setVisible(true);
    }

    private boolean isServerAvailable(String serverAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverAddress, port), 1000);
            return true; 
        } catch (IOException e) {
            return false;
        }
    }

    private void handleUpload(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            for (int i = 0; i < SERVER_ADDRESSES.length; i++) {
                if (isServerAvailable(SERVER_ADDRESSES[i], SERVER_PORTS[i])) {
                    try (Socket socket = new Socket(SERVER_ADDRESSES[i], SERVER_PORTS[i]);
                         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                         FileInputStream fileIn = new FileInputStream(file)) {

                        out.writeUTF("UPLOAD " + file.getName());
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = fileIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        serverResponseArea.append("File uploaded to server on port " + SERVER_PORTS[i] + ": " + file.getName() + "\n");

                    } catch (IOException e) {
                        serverResponseArea.append("Error uploading to server on port " + SERVER_PORTS[i] + ": " + e.getMessage() + "\n");
                    }
                }
            }
        }
    }

    private void handleDelete(ActionEvent event) {
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a file name to delete!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (int i = 0; i < SERVER_ADDRESSES.length; i++) {
            if (isServerAvailable(SERVER_ADDRESSES[i], SERVER_PORTS[i])) {
                try (Socket socket = new Socket(SERVER_ADDRESSES[i], SERVER_PORTS[i]);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    out.writeUTF("DELETE " + fileName);
                    String serverResponse = in.readUTF();
                    serverResponseArea.append("Server on port " + SERVER_PORTS[i] + ": " + serverResponse + "\n");

                } catch (IOException e) {
                    serverResponseArea.append("Error deleting from server on port " + SERVER_PORTS[i] + ": " + e.getMessage() + "\n");
                }
            }
        }
    }

    private void handleDownload(ActionEvent event) {
        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a file name to download!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Folder to Save the File");
        int result = fileChooser.showSaveDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDirectory = fileChooser.getSelectedFile();
            String savePath = selectedDirectory.getAbsolutePath() + File.separator + "downloaded_" + fileName;

            for (int i = 0; i < SERVER_ADDRESSES.length; i++) {
                if (isServerAvailable(SERVER_ADDRESSES[i], SERVER_PORTS[i])) {
                    try (Socket socket = new Socket(SERVER_ADDRESSES[i], SERVER_PORTS[i]);
                         DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                         DataInputStream in = new DataInputStream(socket.getInputStream())) {

                        out.writeUTF("DOWNLOAD " + fileName);
                        String serverResponse = in.readUTF();
                        if (serverResponse.equals("READY")) {
                            try (FileOutputStream fileOut = new FileOutputStream(savePath)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    fileOut.write(buffer, 0, bytesRead);
                                }
                            }
                            serverResponseArea.append("File downloaded from server on port " + SERVER_PORTS[i] + ": " + savePath + "\n");
                            break;
                        } else {
                            serverResponseArea.append("Error from server on port " + SERVER_PORTS[i] + ": " + serverResponse + "\n");
                        }
                    } catch (IOException e) {
                        serverResponseArea.append("Error downloading from server on port " + SERVER_PORTS[i] + ": " + e.getMessage() + "\n");
                    }
                }
            }
        }
    }

    private void handleListFiles(ActionEvent event) {
        for (int i = 0; i < SERVER_ADDRESSES.length; i++) {
            if (isServerAvailable(SERVER_ADDRESSES[i], SERVER_PORTS[i])) {
                try (Socket socket = new Socket(SERVER_ADDRESSES[i], SERVER_PORTS[i]);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    out.writeUTF("LIST");
                    serverResponseArea.append("Files on server (port " + SERVER_PORTS[i] + "):\n");
                    String response;
                    while (!(response = in.readUTF()).equals("END_OF_LIST")) {
                        serverResponseArea.append(response + "\n");
                    }

                } catch (IOException e) {
                    serverResponseArea.append("Error listing files from server on port " + SERVER_PORTS[i] + ": " + e.getMessage() + "\n");
                }
            }
        }
    }
}
