package com.family.chat.service.impl;

import com.family.chat.service.OssService;
import com.family.chat.util.UserIdentifierGenerator;
import com.family.chat.vo.AvatarUploadCredentialVO;
import com.family.chat.vo.ImageUploadCredentialVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    /** Avatars are cropped to JPEG on the client; the signed Content-Type must match the actual request */
    private static final String AVATAR_CONTENT_TYPE = "image/jpeg";

    /** Sentinel written at registration for users without an avatar */
    private static final String DEFAULT_AVATAR_SENTINEL = "/default_avatar.jpg";

    /** Validity of the direct-upload URL: short-lived to limit abuse */
    private static final Duration UPLOAD_URL_DURATION = Duration.ofMinutes(15);

    /** Validity of the read URL: aligned with the client-side avatar cache lifecycle */
    private static final Duration READ_URL_DURATION = Duration.ofDays(7);

    /** Redis ledger key prefix: records every object key issued by the presign endpoints */
    private static final String ISSUED_KEY_PREFIX = "oss:issued:";

    /** Ledger TTL: covers the 15-min upload window plus buffer for the follow-up persist call */
    private static final Duration ISSUED_KEY_TTL = Duration.ofHours(1);

    /** Cache presigned read URLs to avoid repeated S3 signing for the same object key. */
    private static final Duration SIGNED_URL_CACHE_TTL = Duration.ofHours(1);

    private final ConcurrentMap<String, String> signedUrlCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> signedUrlExpiry = new ConcurrentHashMap<>();

    /** Lazy proxy: only resolves the real signer when a credential is requested */
    @Lazy
    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${oss.b2.bucket-name}")
    private String bucketName;

    public String getPresignedUrl(String objectKey, Duration duration) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(getObjectRequest)
                .build();
        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public AvatarUploadCredentialVO generateAvatarUploadCredential(Long userId) {
        String objectKey = "chat/avatars/" + userId + "/" + UserIdentifierGenerator.nextId() + ".jpg";
        markKeyIssued(objectKey);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(AVATAR_CONTENT_TYPE)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return new AvatarUploadCredentialVO(objectKey, presignedRequest.url().toString());
    }

    @Override
    public ImageUploadCredentialVO generateImageUploadCredential(String suffix) {
        String normalizedSuffix = normalizeImageSuffix(suffix);
        String contentType = ".png".equals(normalizedSuffix) ? "image/png" : AVATAR_CONTENT_TYPE;
        String objectKey = "chat/images/" + UserIdentifierGenerator.nextId() + normalizedSuffix;
        markKeyIssued(objectKey);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_URL_DURATION)
                .putObjectRequest(putObjectRequest)
                .build();
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String downloadUrl = getPresignedUrl(objectKey, READ_URL_DURATION);
        return new ImageUploadCredentialVO(objectKey, presignedRequest.url().toString(), downloadUrl);
    }

    /**
     * Register an issued object key into the Redis trusted ledger.
     * Equivalent to Alibaba OSS direct-upload "callback verification" semantics:
     * only keys issued here may later be persisted and signed on read.
     */
    private void markKeyIssued(String objectKey) {
        stringRedisTemplate.opsForValue()
                .setIfAbsent(ISSUED_KEY_PREFIX + objectKey, "1", ISSUED_KEY_TTL);
    }

    @Override
    public boolean isKeyIssued(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ISSUED_KEY_PREFIX + objectKey));
    }

    private String normalizeImageSuffix(String suffix) {
        if (suffix == null) {
            return ".jpg";
        }
        String lower = suffix.toLowerCase();
        if (".png".equals(lower)) {
            return ".png";
        }
        if (".jpeg".equals(lower)) {
            return ".jpg";
        }
        return ".jpg";
    }

    @Override
    public String resolveAvatarUrl(String storedValue) {
        if (DEFAULT_AVATAR_SENTINEL.equals(storedValue)) {
            return storedValue;
        }
        return resolveStoredUrl(storedValue);
    }

    @Override
    public String resolveStoredUrl(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return "";
        }
        // Legacy absolute URL: pass through without signing
        if (storedValue.startsWith("http")) {
            return storedValue;
        }

        long now = System.currentTimeMillis();
        Long expiry = signedUrlExpiry.get(storedValue);
        if (expiry != null && expiry <= now) {
            signedUrlCache.remove(storedValue);
            signedUrlExpiry.remove(storedValue);
            expiry = null;
        }
        if (expiry != null) {
            String cachedUrl = signedUrlCache.get(storedValue);
            if (cachedUrl != null) {
                return cachedUrl;
            }
        }

        try {
            String presignedUrl = getPresignedUrl(storedValue, READ_URL_DURATION);
            signedUrlCache.put(storedValue, presignedUrl);
            signedUrlExpiry.put(storedValue, now + SIGNED_URL_CACHE_TTL.toMillis());
            return presignedUrl;
        } catch (Exception e) {
            // Degrade to empty so message APIs stay available when storage signing fails
            log.error("Failed to sign object key. [objectKey={}]", storedValue, e);
            return "";
        }
    }
}