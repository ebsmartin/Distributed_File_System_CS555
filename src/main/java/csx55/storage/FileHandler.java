package csx55.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

import csx55.dfs.ChunkServer;
import csx55.wireformats.*;

public class FileHandler {

    private ChunkServer chunkServer;
    private String fileDirectory;

    // thread safe data structure to store file information
    private final ConcurrentHashMap<String, List<String>> fileMap = new ConcurrentHashMap<>();
    
    public FileHandler(ChunkServer chunkServer) {
        this.chunkServer = chunkServer;
        setFileDirectory();
    }

    public void setFileDirectory() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        fileDirectory = tmpDir + File.separator + "chunk-server" + File.separator;
        File dir = new File(fileDirectory).getParentFile(); // get the parent directory
    
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
    public boolean fileExists(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
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
    
        if (chunkServer == null) {
            System.out.println("ChunkServer node is null.");
            return;
        }
    
        String fileName = file.getName().split("_")[0];
        String chunkName = file.getName().split("_")[1];

        try {
            Files.copy(file.toPath(), Paths.get(getWorkingDirectory(), 
                        filePath), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File stored locally at " + getWorkingDirectory() + filePath);

            fileMap.computeIfAbsent(fileName, k -> new ArrayList<>()).add(chunkName);
        } catch (IOException e) {
            System.out.println("Error storing file: " + e.getMessage());
        }
    }

    public List<File> downloadFile(String filePath) {
        List<File> downloadedFiles = new ArrayList<>();
    
        if (filePath == null) {
            System.out.println("File path is null.");
            return downloadedFiles;
        }
    
        String myFilePath = Paths.get(getWorkingDirectory(), filePath).toString();
    
        File file = new File(myFilePath);
        String fileName = file.getName();
    
        if (!fileMap.containsKey(fileName)) {
            System.out.println("File not found: " + fileName);
            System.out.println("Something went wrong!");
            return downloadedFiles;
        } else {
            // print all chunks of that file
            System.out.println("Chunks of file found: " +  fileMap.get(fileName));
        }
    
        List<String> chunks = fileMap.get(fileName);
        for (String chunk : chunks) {
            String chunkFilePath = Paths.get(getWorkingDirectory(), chunk).toString();
            File chunkFile = new File(chunkFilePath);
            try {
                Files.copy(Paths.get(getWorkingDirectory(), chunk), 
                            Paths.get(chunk), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Chunk downloaded to: " + chunk);
                downloadedFiles.add(chunkFile);
            } catch (IOException e) {
                System.out.println("Error downloading chunk: " + e.getMessage());
            }
        }
    
        return downloadedFiles;
    }

    public Boolean removeFile(String filePath) {
        if (filePath == null) {
            System.out.println("File name is null.");
            return false;
        }
    
        String fileName = Paths.get(filePath).getFileName().toString();
    
        System.out.println("Removing file: " + fileName);
        if (!fileMap.containsKey(fileName)) {
            System.out.println("File not found: " + fileName);
            return false;
        }
    
        List<String> chunks = fileMap.get(fileName);
        for (String chunk : chunks) {
            String chunkFilePath = Paths.get(getWorkingDirectory(), fileName + "_" + chunk).toString();
            File chunkFile = new File(chunkFilePath);
            if (chunkFile.exists()) {
                if (chunkFile.delete()) {
                    System.out.println("Chunk deleted: " + chunkFilePath);
                } else {
                    System.out.println("Error deleting chunk: " + chunkFilePath);
                }
            } else {
                System.out.println("Chunk not found: " + chunkFilePath);
            }
        }
    
        fileMap.remove(fileName);
        return true;
    }

    public Boolean removeChunk(String fileName, String chunkName) {
        if (fileName == null || chunkName == null) {
            System.out.println("File name or chunk name is null.");
            return false;
        }
    
        System.out.println("Removing chunk: " + chunkName + " from file: " + fileName);
        if (!fileMap.containsKey(fileName)) {
            System.out.println("File not found: " + fileName);
            return false;
        }
    
        List<String> chunks = fileMap.get(fileName);
        if (!chunks.contains(chunkName)) {
            System.out.println("Chunk not found: " + chunkName);
            return false;
        }
    
        String chunkFilePath = Paths.get(getWorkingDirectory(), fileName + "_" + chunkName).toString();
        File chunkFile = new File(chunkFilePath);
        if (chunkFile.exists()) {
            if (chunkFile.delete()) {
                System.out.println("Chunk deleted: " + chunkFilePath);
                chunks.remove(chunkName);
                return true;
            } else {
                System.out.println("Error deleting chunk: " + chunkFilePath);
                return false;
            }
        } else {
            System.out.println("Chunk file not found: " + chunkFilePath);
            return false;
        }
    }

    // get file list
    public ConcurrentHashMap<String, List<String>> getFileList() {
        return fileMap;
    }

    public void printFileList() {
        // Return a list of files stored locally
        System.out.println("Files stored locally:");
        for (String fileName : fileMap.keySet()) {
            System.out.println(fileName);
        }
    }
}
