package csx55.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.concurrent.ConcurrentHashMap;

import csx55.dfs.Peer;
import csx55.wireformats.*;

public class FileHandler {

    private Peer peerNode;
    private String fileDirectory;

    // thread safe data structure to store file information
    private final ConcurrentHashMap<String, Integer> fileMap = new ConcurrentHashMap<>();
    
    public FileHandler(Peer peerNode) {
        this.peerNode = peerNode;
        setFileDirectory(String.valueOf(peerNode.getPeerID()));
    }

    public void setFileDirectory(String peerID) {
        String tmpDir = System.getProperty("java.io.tmpdir");
        fileDirectory = tmpDir + File.separator + peerID + File.separator;
        File dir = new File(fileDirectory);

        if (!dir.exists()) {
            dir.mkdirs();  // create the directory if it doesn't exist
        }
        System.out.println("File directory set to: " + fileDirectory);
    }

    // return the working directory of the peer node
    public String getWorkingDirectory() {
        return fileDirectory;
    }

    // check if the file exists in the local storage
    public boolean fileExists(String fileName) {
        return fileMap.containsKey(fileName);
    }
    
    public void uploadFile(String filePath) {
        if (filePath == null) {
            System.out.println("File path is null.");
            return;
        }
    
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File does not exist: " + filePath);
            return;
        }
    
        if (peerNode == null) {
            System.out.println("Peer node is null.");
            return;
        }
    
        String fileName = file.getName();
        int fileHash = fileName.hashCode();
    
        if (responsibleNode == null) {
            System.out.println("Responsible node is null.");
            return;
        }
    
        if (responsibleNode.getSuccessorID() == peerNode.getPeerID()) {
            // This node is responsible, store the file locally
            try {
                Files.copy(file.toPath(), Paths.get(getWorkingDirectory(), 
                            fileName), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File " + fileHash + " stored locally at " + getWorkingDirectory());
                fileMap.put(fileName, fileHash);
            } catch (IOException e) {
                System.out.println("Error storing file: " + e.getMessage());
            }
        } else {
            // Send the file to the responsible node
            System.out.println("Sending file " + fileName + " to node: " + responsibleNode.getSuccessorHostPort());
            // create a migration  and send it to the responsible node
            Migration migration = new Migration(file, fileHash, peerNode.getNode(), false);
            peerNode.sendToNode(responsibleNode.getSuccessorHostPort(), migration);
        }
    }

    public void downloadFile(String fileName) {
        if (fileName == null) {
            System.out.println("File name is null.");
            return;
        }
        int fileHash = fileName.hashCode();
    
        if (responsibleNode == null) {
            System.out.println("Responsible node is null.");
            return;
        }
    
        if (responsibleNode.getSuccessorID() == peerNode.getPeerID()) {
            if (!fileMap.containsKey(fileName)) {
                System.out.println("File not found: " + fileName);
                System.out.println("Something went wrong!");
                return;
            } else {
                System.out.println("File found in local storage: " + fileName);
            }
    
            try {
                Files.copy(Paths.get(getWorkingDirectory(), fileName), 
                            Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File downloaded to: " + fileName);
            } catch (IOException e) {
                System.out.println("Error downloading file: " + e.getMessage());
            }
        } else {
            System.out.println("Requesting file <" + fileName + "> from node: " + responsibleNode.getSuccessorHostPort());
            DownloadRequest downloadRequest = new DownloadRequest(fileName, fileHash, peerNode.getPeerID() + " " + peerNode.getNode());
            peerNode.sendToNode(responsibleNode.getSuccessorHostPort(), downloadRequest);
        }
    }

    public void storeFile(File sourceFile, int fileIdentifier) {
        if (sourceFile == null) {
            System.out.println("Source file is null.");
            return;
        }

        System.out.println("Storing file: " + sourceFile.getPath() + " with identifier: " + fileIdentifier);
        if (!sourceFile.exists()) {
            System.out.println("Source file does not exist: " + sourceFile.getPath());
            return;
        }

        String fileName = sourceFile.getName();
        try {
            Files.copy(sourceFile.toPath(), Paths.get(getWorkingDirectory(), fileName), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File stored locally at " + getWorkingDirectory());
            fileMap.put(fileName, fileIdentifier);
        } catch (IOException e) {
            System.out.println("Error storing file: " + e.getMessage());
        }
    }

    public Boolean removeFile(String fileName) {
        if (fileName == null) {
            System.out.println("File name is null.");
            return false;
        }

        // System.out.println("Removing file: " + fileName);
        if (!fileMap.containsKey(fileName)) {
            // System.out.println("File not found: " + fileName);
            return false;
        }

        int fileIdentifier = fileMap.get(fileName);
        File file = new File(getWorkingDirectory() + fileIdentifier + "_" + fileName);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getPath());
            return false;
        }

        if (file.delete()) {
            System.out.println("File deleted: " + file.getPath());
            fileMap.remove(fileName);
            return true;
        } else {
            System.out.println("Error deleting file: " + file.getPath());
            return false;
        }
    }

    public File retrieveFile(String fileName) {
        if (fileName == null) {
            System.out.println("File name is null.");
            return null;
        }

        System.out.println("Retrieving file: " + fileName);
        if (!fileMap.containsKey(fileName)) {
            System.out.println("File not found: " + fileName);
            return null;
        }

        File file = new File(getWorkingDirectory() + fileName);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getPath());
            return null;
        }

        return file;
    }

    public void removeFromHashTable(String fileName) {
        // Implementation to remove the file from the local hash table
        System.out.println("Removing file from hash table: " + fileName);
        fileMap.remove(fileName);
    }

    // get file list
    public ConcurrentHashMap<String, Integer> getFileList() {
        return fileMap;
    }

    public void printFileList() {
        // Return a list of files stored locally
        for (String key : fileMap.keySet()) {
            System.out.println(key + " " + fileMap.get(key));
        }
    }
}
