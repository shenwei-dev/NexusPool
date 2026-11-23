package com.family.chat.controller;

import com.family.chat.common.result.ApiResponse;
import com.family.chat.common.result.ResultCode;
import com.family.chat.service.OssService;
import com.family.chat.util.SecurityUtil;
import com.family.chat.vo.AvatarUploadCredentialVO;
import com.family.chat.vo.ImageUploadCredentialVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oss")
public class OssController {

    private final OssService ossService;

    /**
     * Avatar direct-upload credential.
     * GET /api/oss/avatar/presign
     *
     * <p>Client PUTs the file bytes directly to object storage with the
     * returned pre-signed URL; the server only issues the credential.
     */
    @GetMapping("/avatar/presign")
    public ApiResponse<AvatarUploadCredentialVO> avatarUploadCredential() {
        try {
            AvatarUploadCredentialVO credential =
                    ossService.generateAvatarUploadCredential(SecurityUtil.getCurrentUserId());
            return ApiResponse.success(credential);
        } catch (Exception e) {
            log.error("Failed to issue avatar upload credential.", e);
            return ApiResponse.fail(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * Chat image direct-upload credential.
     * GET /api/oss/image/presign?suffix=.jpg
     *
     * <p>Client PUTs the image bytes directly to object storage; the message
     * content stores the object key, which the server signs on every read.
     */
    @GetMapping("/image/presign")
    public ApiResponse<ImageUploadCredentialVO> imageUploadCredential(
            @RequestParam(value = "suffix", required = false, defaultValue = ".jpg") String suffix) {
        try {
            ImageUploadCredentialVO credential = ossService.generateImageUploadCredential(suffix);
            return ApiResponse.success(credential);
        } catch (Exception e) {
            log.error("Failed to issue image upload credential.", e);
            return ApiResponse.fail(ResultCode.SYSTEM_ERROR);
        }
    }
}
