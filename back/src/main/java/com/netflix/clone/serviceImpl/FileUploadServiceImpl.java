package com.netflix.clone.serviceImpl;

import com.netflix.clone.service.FileUploadService;
import com.netflix.clone.util.FileHandlerUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private Path videoStorageLocation;
    private Path imageStorageLocation;

    @Value("${file.upload.video-dir:uploads/videos}")
    private String videoDir;

    @Value("${file.upload.image-dir:uploads/images}")
    private String imageDir;

    @PostConstruct
    public void init() {
        this.videoStorageLocation = Path.of(videoDir).toAbsolutePath().normalize();
        this.imageStorageLocation = Path.of(imageDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.videoStorageLocation);
            Files.createDirectories(this.imageStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not initialize file upload directory: " + ex.getMessage());
        }
    }

    @Override
    public String storeVideoFile(MultipartFile file) {
        return storeFile(file, videoStorageLocation);
    }

    @Override
    public String storeImageFile(MultipartFile file) {
        return storeFile(file, imageStorageLocation);
    }

    @Override
    public ResponseEntity<Resource> serveVideo(String uuid, String rangeHeader) {
        try {
            Path filePath = FileHandlerUtil.findFileByuUid(videoStorageLocation, uuid);
            Resource resource = FileHandlerUtil.createFullResource(filePath);

            String filename = resource.getFilename();
            String contentType = FileHandlerUtil.detectVideoContentType(filename);
            long fileLength = resource.contentLength();

            if(isFullContentRequest(rangeHeader)) {
                return buildFullVideoResponse(resource, contentType, filename, fileLength);
            }

            return buildPartialVideoResponse(filePath, rangeHeader, contentType, filename, fileLength);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @Override
    public ResponseEntity<Resource> serveImage(String uuid) {
        try {
            Path filePath = FileHandlerUtil.findFileByuUid(imageStorageLocation, uuid);
            Resource resource = FileHandlerUtil.createFullResource(filePath);

            String filename = resource.getFilename();
            String contentType = FileHandlerUtil.detectImageContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Resource> buildPartialVideoResponse(Path filePath, String rangeHeader, String contentType, String filename, long fileLength) throws IOException {
        long[] range = FileHandlerUtil.parseRangeHeader(rangeHeader, fileLength);
        long rangeStart = range[0];
        long rangeEnd = range[1];

        if(!isValidRange(rangeStart, rangeEnd, fileLength)) {
            return buildRangeNotSatisfiableResponse(fileLength);
        }

        long contentLength = rangeEnd - rangeStart + 1;
        Resource rangeResource = FileHandlerUtil.createRangeResource(filePath, rangeStart, contentLength);

        return ResponseEntity.status(206)
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                .body(rangeResource);
    }

    private ResponseEntity<Resource> buildRangeNotSatisfiableResponse(long fileLength) {
        return ResponseEntity.status(416)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileLength)
                .build();
    }

    private boolean isValidRange(long rangeStart, long rangeEnd, long fileLength) {
        return rangeStart <= rangeEnd && rangeStart >= 0 && rangeEnd < fileLength;
    }

    private ResponseEntity<Resource> buildFullVideoResponse(Resource resource, String contentType, String filename, long fileLength) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileLength))
                .body(resource);
    }

    private boolean isFullContentRequest(String rangeHandler) {
        return rangeHandler == null || rangeHandler.isEmpty();
    }

    private String storeFile(MultipartFile file, Path storageLocations) {
        String fileExtension = FileHandlerUtil.extractFileExtention(file.getOriginalFilename());
        String uuid = UUID.randomUUID().toString();
        String fileName = uuid + fileExtension;

        try {
            if(file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file " + fileName);
            }

            Path targetLocation = storageLocations.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return uuid;
        } catch (Exception ex) {
            throw new RuntimeException("Could not store file " + fileName + ": " + ex.getMessage());
        }
    }
}
