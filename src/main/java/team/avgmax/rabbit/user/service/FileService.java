package team.avgmax.rabbit.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.avgmax.rabbit.user.exception.UserError;
import team.avgmax.rabbit.user.exception.UserException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final AmazonS3 amazonS3;
    @Value("${app.minio.bucket}")
    private String bucket;

    public String uploadFile(MultipartFile file, String personalUserId) {
        try {
            String key = personalUserId + "_" + file.getOriginalFilename();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            amazonS3.putObject(bucket, key, file.getInputStream(), metadata);
            return amazonS3.getUrl(bucket, key).toString();
        } catch (Exception e) {
            throw new UserException(UserError.FILE_UPLOAD_FAILED);
        }
    }
    
}
