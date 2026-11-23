package com.family.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Avatar direct-upload credential (client-side direct upload to object storage).
 *
 * <p>The backend only issues the credential; the file bytes never pass through
 * the application server, saving server bandwidth.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvatarUploadCredentialVO {

    /** Object key to persist after a successful upload */
    private String objectKey;

    /** Pre-signed PUT URL, short-lived (minutes) */
    private String uploadUrl;
}
