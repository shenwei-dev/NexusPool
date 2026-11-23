package com.family.chat.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chat image direct-upload credential.
 *
 * <p>Besides the object key and the short-lived PUT URL, a signed GET URL is
 * included so the sender can persist a locally usable remote reference without
 * a second round trip.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageUploadCredentialVO {

    /** Object key persisted as the image message content */
    private String objectKey;

    /** Pre-signed PUT URL, short-lived (minutes) */
    private String uploadUrl;

    /** Pre-signed GET URL for the sender's local record */
    private String downloadUrl;
}
