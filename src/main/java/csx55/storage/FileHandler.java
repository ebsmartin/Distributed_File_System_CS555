package csx55.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import csx55.dfs.ChunkServer;
import csx55.wireformats.*;

public class FileHandler {

    private ChunkServer chunkServer;
    private String fileDirectory;
    private float totalSpace = 1000000; // 1MB
    private int lastCheckedSize = 0; // used to check if new chunks have been added
    private final Set<Path> newChunks = new LinkedHashSet<>();   // set of new chunks
    private boolean corruptedFileFound = false;

    // thread safe data structure to store file information
    private final ConcurrentHashMap<String, Set<Path>> fileMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, byte[]> checksums = new ConcurrentHashMap<>();

    
    public FileHandler(ChunkServer chunkServer) {
        this.chunkServer = chunkServer;
        setFileDirectory();
    }

    public float getTotalSpace() {
        return totalSpace;
    }

    public boolean hasNewChunks() {
        int currentSize = fileMap.values().stream().mapToInt(Set::size).sum();
        boolean hasNew = currentSize > lastCheckedSize;
        lastCheckedSize = currentSize;
        return hasNew;
    }

    public List<String> getNewChunks() {
        List<String> chunkNames = newChunks.stream()
            .map(path -> path.getFileName().toString())
            .collect(Collectors.toList());
        newChunks.clear();
        return chunkNames;
    }

    public synchronized void setFileDirectory() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        fileDirectory = tmpDir + File.separator + "chunk-server" + File.separator;
        File dir = new File(fileDirectory);
    
        if (!dir.exists()) {
            dir.mkdirs();  // create the directory if it doesn't exist
        }
        System.out.println("File directory set to: " + fileDirectory);
    }

    // return the working directory of the peer node
    public Path getWorkingDirectory() {
        return Paths.get(fileDirectory);
    }

    // check if the file exists in the local storage
    public boolean fileExists(String fileName) {
        return fileMap.containsKey(fileName);
    }

    public boolean corruptedFileFound() {
        return corruptedFileFound;
    }

    public void uploadFile(Upload upload) {
        String fileName = upload.getFileName();
        Path filePath = Paths.get(upload.getFilePath());
        File chunkFile = upload.getFile();
        byte[] fileContents = upload.getFileContents();

        if (filePath == null) {
            System.out.println("File path is null.");
            return;
        }

        if (chunkServer == null) {
            System.out.println("ChunkServer node is null.");
            return;
        }
        synchronized (this) {
            try {
                Path destinationPath = getWorkingDirectory().resolve(filePath);

                Files.write(destinationPath, fileContents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("File stored locally at " + destinationPath);
                System.out.println("File directory: " + fileDirectory);  // print the file directory
                long fileSize = fileContents.length;
                this.totalSpace -= fileSize;
                Set<Path> chunkPaths = fileMap.computeIfAbsent(fileName, k -> new HashSet<>());
                chunkPaths.add(destinationPath);
                newChunks.add(destinationPath);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] checksum = md.digest(fileContents);
                checksums.put(destinationPath, checksum);
            } catch (IOException e) {
                System.out.println("Error storing file: " + e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                System.out.println("Error initializing MessageDigest: " + e.getMessage());
            }
        }
    }

    public DownloadChunkResponse downloadChunk(Path filePath, int chunkNum) {
        if (filePath == null) {
            System.out.println("File path is null.");
            // create a download response with an empty chunk
            return new DownloadChunkResponse(filePath, chunkNum, new byte[0], false);
        }

        System.out.println("Downloading chunk: " + chunkNum + " from file: " + filePath);
        String fileName = filePath.getFileName().toString();
    
        if (!fileMap.containsKey(fileName)) {
            System.out.println("File not found: " + fileName);
            return new DownloadChunkResponse(filePath, chunkNum, new byte[0], false);
        } else {
            System.out.println("File found, checking for chunk: " + chunkNum);
        }
    
        Set<Path> chunks = fileMap.get(fileName);
        for (Path chunk : chunks) {
            String chunkName = chunk.getFileName().toString();
            String chunkNumberString = chunkName.substring(chunkName.lastIndexOf("chunk") + 5);
            if (chunkNumberString.equals(String.valueOf(chunkNum))) {
                System.out.println("Chunk found: " + chunk);
                try {
                    byte[] chunkContents = Files.readAllBytes(chunk);
                    DownloadChunkResponse downloadResponse = new DownloadChunkResponse(chunk, chunkNum, chunkContents, true);
                    return downloadResponse;
                } catch (IOException e) {
                    System.out.println("Error reading chunk: " + chunk);
                    System.out.println("Exception: " + e.getMessage());
                }
            }
        }

        // If no matching chunk was found, return a response with an empty chunk
        System.out.println("Chunk not found: " + chunkNum);
        return new DownloadChunkResponse(filePath, chunkNum, new byte[0], false);
    }

    public List<String> checkChecksums() {
        List<String> failedFiles = new ArrayList<>();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error initializing MessageDigest: " + e.getMessage());
            return failedFiles;
        }

        for (Map.Entry<Path, byte[]> entry : checksums.entrySet()) {
            Path path = entry.getKey();
            byte[] expectedChecksum = entry.getValue();
            try {
                byte[] fileContents = Files.readAllBytes(path);
                byte[] actualChecksum = md.digest(fileContents);
                if (!Arrays.equals(expectedChecksum, actualChecksum)) {
                    failedFiles.add(path.getFileName().toString().split("_")[1]);
                }
            } catch (IOException e) {
                System.out.println("Error reading file: " + e.getMessage());
            }
        }
        this.corruptedFileFound = !failedFiles.isEmpty();

        return failedFiles;
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
    
        Set<Path> chunks = fileMap.get(fileName);
        for (Path chunkFilePath : chunks) {
            try {
                long fileSize = Files.size(chunkFilePath);
                this.totalSpace += fileSize;
                Files.deleteIfExists(chunkFilePath);
                System.out.println("Chunk deleted: " + chunkFilePath);
            } catch (IOException e) {
                System.out.println("Error deleting chunk: " + chunkFilePath);
                System.out.println("Exception: " + e.getMessage());
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
    
        Set<Path> chunks = fileMap.get(fileName);
        Path chunkFilePath = Paths.get(getWorkingDirectory().toString(), fileName + "_" + chunkName);
    
        if (!chunks.contains(chunkFilePath)) {
            System.out.println("Chunk not found: " + chunkName);
            return false;
        }
    
        try {
            long chunkSize = Files.size(chunkFilePath);
            this.totalSpace += chunkSize;
            Files.deleteIfExists(chunkFilePath);
            System.out.println("Chunk deleted: " + chunkFilePath);
            chunks.remove(chunkFilePath);
            return true;
        } catch (IOException e) {
            System.out.println("Error deleting chunk: " + chunkFilePath);
            System.out.println("Exception: " + e.getMessage());
            return false;
        }
    }

    // get file list
    public ConcurrentHashMap<String, Set<Path>> getFileMap() {
        return fileMap;
    }

    public void printFileList() {
        // Return a list of files stored locally
        System.out.println("Files stored locally:");
        for (Map.Entry<String, Set<Path>> entry : fileMap.entrySet()) {
            String fileName = entry.getKey();
            Set<Path> chunks = entry.getValue();
            System.out.println("File: " + fileName);
            for (Path chunk : chunks) {
                System.out.println("\tChunk: " + chunk.getFileName().toString());
            }
        }
    }
}
