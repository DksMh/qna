package com.act2gether.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class FileUploadUtil {
    
    @Value("${app.upload.dir:/uploads}")
    private String uploadBaseDir;
    
    @Value("${app.upload.qna-images:/qna/upload/img}")
    private String qnaImageDir;
    
    @Value("${app.upload.max-file-size:3145728}") // 3MB = 3 * 1024 * 1024
    private long maxFileSize;
    
    @Value("${app.upload.max-image-width:1920}")
    private int maxImageWidth;
    
    @Value("${app.upload.max-image-height:1080}")
    private int maxImageHeight;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp"
    );
    
    /**
     * QnA 이미지 업로드
     * 경로: /qna/upload/img/{year}/{month}/{userId}_{timestamp}_{randomUUID}.{ext}
     */
    public String uploadQnaImage(MultipartFile file, Long userId) throws IOException {
        // 파일 유효성 검증
        validateImageFile(file);
        
        // 업로드 경로 생성
        String uploadPath = createQnaImagePath(userId);
        
        // 디렉토리 생성
        Path directoryPath = Paths.get(uploadBaseDir + uploadPath);
        Files.createDirectories(directoryPath);
        
        // 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = generateFilename(userId, extension);
        
        // 전체 파일 경로
        String fullPath = uploadPath + "/" + filename;
        Path filePath = Paths.get(uploadBaseDir + fullPath);
        
        // 이미지 리사이징 및 저장
        resizeAndSaveImage(file, filePath.toFile(), extension);
        
        log.info("QnA 이미지 업로드 완료: {}", fullPath);
        
        return fullPath;
    }
    
    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        
        Path path = Paths.get(uploadBaseDir + filePath);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("파일 삭제 완료: {}", filePath);
        } else {
            log.warn("삭제할 파일이 존재하지 않음: {}", filePath);
        }
    }
    
    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        Path path = Paths.get(uploadBaseDir + filePath);
        return Files.exists(path);
    }
    
    // Private helper methods
    
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("파일 크기는 3MB 이하여야 합니다.");
        }
        
        // 파일 형식 검증
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPG, PNG, WebP만 지원)");
        }
        
        // 파일 확장자 검증
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }
        
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 확장자입니다.");
        }
    }
    
    private String createQnaImagePath(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        String year = now.format(DateTimeFormatter.ofPattern("yyyy"));
        String month = now.format(DateTimeFormatter.ofPattern("MM"));
        
        return qnaImageDir + "/" + year + "/" + month;
    }
    
    private String generateFilename(Long userId, String extension) {
        long timestamp = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return userId + "_" + timestamp + "_" + uuid + extension;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        
        return filename.substring(lastDotIndex).toLowerCase();
    }
    
    private void resizeAndSaveImage(MultipartFile file, File outputFile, String extension) throws IOException {
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        if (originalImage == null) {
            throw new IOException("이미지 파일을 읽을 수 없습니다.");
        }
        
        // 리사이징이 필요한지 확인
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        if (originalWidth <= maxImageWidth && originalHeight <= maxImageHeight) {
            // 리사이징 불필요, 원본 저장
            file.transferTo(outputFile);
            return;
        }
        
        // 리사이징 계산 (비율 유지)
        double widthRatio = (double) maxImageWidth / originalWidth;
        double heightRatio = (double) maxImageHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 리사이징된 이미지 생성
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        
        // 고품질 리사이징 설정
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        
        // 파일 형식 결정
        String formatName = "jpg"; // 기본값
        if (extension.contains("png")) {
            formatName = "png";
        } else if (extension.contains("webp")) {
            formatName = "webp";
        }
        
        // 리사이징된 이미지 저장
        ImageIO.write(resizedImage, formatName, outputFile);
        
        log.info("이미지 리사이징 완료: {}x{} -> {}x{}", originalWidth, originalHeight, newWidth, newHeight);
    }
}