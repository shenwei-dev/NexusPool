package com.family.chat.vo;

/**
 * 图片消息直传凭证: objectKey(作为消息内容持久化) + 短时效预签名 PUT 地址
 * + 签名后的 GET 下载地址(供发送方本地记录在本地副本丢失时兜底展示)。
 */
public class ImageUploadCredentialVO {

    private String objectKey;

    private String uploadUrl;

    private String downloadUrl;

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
