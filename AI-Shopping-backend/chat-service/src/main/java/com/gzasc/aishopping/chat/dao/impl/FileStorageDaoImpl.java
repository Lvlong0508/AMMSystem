package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.FileStorageDao;
import com.gzasc.aishopping.chat.exception.FileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class FileStorageDaoImpl implements FileStorageDao {

    @Value("${app.file.storage}")
    private String storagePath;

    @Value("${app.file.finish}")
    private String finishPath;

    private Path uploadDir;
    private Path finishDir;

    @PostConstruct
    public void init() {
        this.uploadDir = Path.of(storagePath).toAbsolutePath().normalize();
        this.finishDir = Path.of(finishPath).toAbsolutePath().normalize();
    }

    private Path getStoragePath() {
        return uploadDir;
    }

    private Path getFinishPath() {
        return finishDir;
    }

    @Override
    public String storeFile(MultipartFile file, Long userId) {
        try {
            String originalName = file.getOriginalFilename();
            String storageName = resolveConflict(originalName);

            Path targetDir = getStoragePath();
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(storageName).toFile());

            String line = String.format("%s|%s|%s|%s%n",
                    storageName, originalName,
                    userId != null ? userId.toString() : "",
                    LocalDateTime.now().toString());
            Files.writeString(targetDir.resolve("index.txt"), line,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            return storageName;
        } catch (IOException e) {
            throw new FileException("文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public void moveFile(String fileName) {
        try {
            List<String[]> uploadEntries = readIndex(getStoragePath());
            String[] target = null;
            for (String[] entry : uploadEntries) {
                if (entry[0].equals(fileName)) {
                    target = entry;
                    break;
                }
            }
            if (target == null) {
                throw new FileException("文件未找到: " + fileName);
            }

            Path source = getStoragePath().resolve(fileName);
            Path targetDir = getFinishPath();
            Files.createDirectories(targetDir);
            Files.move(source, targetDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

            List<String[]> remaining = uploadEntries.stream()
                    .filter(entry -> !entry[0].equals(fileName))
                    .collect(Collectors.toList());
            writeIndex(getStoragePath(), remaining);

            String line = String.format("%s|%s|%s|%s%n",
                    target[0], target[1], target[2], LocalDateTime.now().toString());
            Files.writeString(targetDir.resolve("index.txt"), line,
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FileException("文件移动失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFileFromUpload(List<String> fileNameList) {
        deleteFiles(getStoragePath(), fileNameList);
    }

    @Override
    public void deleteFileFromFinish(List<String> fileNameList) {
        deleteFiles(getFinishPath(), fileNameList);
    }

    @Override
    public List<String> getFileNameFromUpload() {
        return getFileNames(getStoragePath());
    }

    @Override
    public List<String> getFileNameFromFinish() {
        return getFileNames(getFinishPath());
    }

    private String resolveConflict(String fileName) {
        Path target = getStoragePath().resolve(fileName);
        if (!Files.exists(target)) {
            return fileName;
        }

        int dotIndex = fileName.lastIndexOf('.');
        String base, ext;
        if (dotIndex > 0) {
            base = fileName.substring(0, dotIndex);
            ext = fileName.substring(dotIndex);
        } else {
            base = fileName;
            ext = "";
        }

        int counter = 1;
        String newName;
        do {
            newName = base + " (" + counter + ")" + ext;
            target = getStoragePath().resolve(newName);
            counter++;
        } while (Files.exists(target));

        return newName;
    }

    private List<String[]> readIndex(Path dir) throws IOException {
        Path indexFile = dir.resolve("index.txt");
        if (!Files.exists(indexFile)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(indexFile, StandardCharsets.UTF_8).stream()
                .map(line -> {
                    String[] parts = line.split("\\|", 4);
                    if (parts.length == 3) {
                        // 旧格式 name|orig|time → 补空 userId
                        return new String[]{parts[0], parts[1], "", parts[2]};
                    }
                    return parts;
                })
                .collect(Collectors.toList());
    }

    private void writeIndex(Path dir, List<String[]> entries) throws IOException {
        if (entries.isEmpty()) {
            Files.writeString(dir.resolve("index.txt"), "", StandardCharsets.UTF_8);
            return;
        }
        String content = entries.stream()
                .map(entry -> String.join("|", entry))
                .collect(Collectors.joining("\n", "", "\n"));
        Files.writeString(dir.resolve("index.txt"), content, StandardCharsets.UTF_8);
    }

    private void deleteFiles(Path dir, List<String> fileNameList) {
        try {
            List<String[]> entries = readIndex(dir);
            List<String[]> remaining = new ArrayList<>();
            Set<String> toDelete = new HashSet<>();

            for (String[] entry : entries) {
                if (fileNameList.contains(entry[0])) {
                    toDelete.add(entry[0]);
                } else {
                    remaining.add(entry);
                }
            }

            writeIndex(dir, remaining);

            for (String name : toDelete) {
                Files.deleteIfExists(dir.resolve(name));
            }
        } catch (IOException e) {
            throw new FileException("文件删除失败: " + e.getMessage());
        }
    }

    private List<String> getFileNames(Path dir) {
        try {
            return readIndex(dir).stream()
                    .map(entry -> entry[0])
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileException("读取文件列表失败: " + e.getMessage());
        }
    }
}
