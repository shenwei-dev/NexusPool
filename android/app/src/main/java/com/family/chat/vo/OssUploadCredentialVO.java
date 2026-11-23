package com.family.chat.vo;

/**
 * 头像直传凭证: 后端下发的对象 key 与短时效预签名 PUT 地址。
 * 客户端拿到后直接把文件字节 PUT 到对象存储, 不再经过应用服务器。
 */
public class OssUploadCredentialVO {

    private String objectKey;

    private String uploadUrl;

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
}
