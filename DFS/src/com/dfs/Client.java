package com.dfs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;

public class Client {
    private JFrame frame;
    private JTextArea serverResponseArea;
    private JTextField fileNameField;

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
        JButton listFilesButton = new JButton("List Files");

        buttonPanel.add(uploadButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(listFilesButton);

        frame.add(buttonPanel, BorderLayout.NORTH);
        serverResponseArea = new JTextArea();
        serverResponseArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(serverResponseArea);
        frame.add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());

        fileNameField = new JTextField();
        fileNameField.setToolTipText("Enter file name for download...");
        bottomPanel.add(fileNameField, BorderLayout.CENTER);

        frame.add(bottomPanel, BorderLayout.SOUTH);
        uploadButton.addActionListener(this::handleUpload);
        downloadButton.addActionListener(this::handleDownload);
        listFilesButton.addActionListener(this::handleListFiles);

        frame.setVisible(true);
    }

    private void handleUpload(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (Socket socket = new Socket("localhost", 5000);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 FileInputStream fileIn = new FileInputStream(file)) {
                out.writeUTF("UPLOAD " + file.getName());
                out.flush();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
                serverResponseArea.append("File uploaded: " + file.getName() + "\n");

            } catch (IOException e) {
                serverResponseArea.append("Error uploading file: " + e.getMessage() + "\n");
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

            try (Socket socket = new Socket("localhost", 5000);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {
                out.writeUTF("DOWNLOAD " + fileName);
                out.flush();
                String serverResponse = in.readUTF();
                if (serverResponse.equals("READY")) {
                    FileOutputStream fileOut = new FileOutputStream(savePath);
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                    }
                    serverResponseArea.append("File downloaded: " + savePath + "\n");
                    fileOut.close();
                } else {
                    serverResponseArea.append("Error: " + serverResponse + "\n");
                }


            } catch (IOException e) {
                serverResponseArea.append("Error downloading file: " + e.getMessage() + "\n");
            }
        }
    }


    private void handleListFiles(ActionEvent event) {
        try (Socket socket = new Socket("localhost", 5000);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            out.writeUTF("LIST");
            out.flush();
            serverResponseArea.append("Files on server:\n");
            String response;
            while (!(response = in.readUTF()).equals("END_OF_LIST")) {
                serverResponseArea.append(response + "\n");
            }

        } catch (IOException e) {
            serverResponseArea.append("Error listing files: " + e.getMessage() + "\n");
        }
    }
}

