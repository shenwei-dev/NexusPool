package com.family.chat.service;

import com.family.chat.vo.AvatarUploadCredentialVO;
import com.family.chat.vo.ImageUploadCredentialVO;
import java.time.Duration;

public interface OssService {

    String getPresignedUrl(String objectKey, Duration duration);

    /**
     * Issue a direct-upload credential for the user's avatar.
     * The client PUTs file bytes straight to object storage with the
     * pre-signed URL; nothing passes through the application server.
     */
    AvatarUploadCredentialVO generateAvatarUploadCredential(Long userId);

    /**
     * Issue a direct-upload credential for a chat image message.
     * The returned downloadUrl lets the sender keep a locally usable
     * remote reference without a second round trip.
     */
    ImageUploadCredentialVO generateImageUploadCredential(String suffix);

    /**
     * Resolve a persisted avatar value into an accessible URL.
     * Object keys are signed on read; sentinels and legacy URLs pass through.
     */
    String resolveAvatarUrl(String storedValue);

    /**
     * Resolve any persisted object reference (object key) into an accessible
     * URL. Plain object keys are signed on read; absolute URLs pass through.
     */
    String resolveStoredUrl(String storedValue);

    /**
     * Verify that an object key was actually issued by this service's presign
     * endpoint (trusted ledger check). Keys outside the ledger must never be
     * persisted or signed, otherwise the signing logic becomes an oracle that
     * signs arbitrary attacker-supplied keys.
     */
    boolean isKeyIssued(String objectKey);
}
