package com.family.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.family.chat.common.ApiResponse;
import com.family.chat.dto.SendFriendRequestDTO;
import com.family.chat.repository.FriendRequestRepository; // 👈 导入您的仓库类

public class SendFriendRequestViewModel extends ViewModel {

    private final FriendRequestRepository friendRequestRepository;

    private final MutableLiveData<Boolean> isSendSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public SendFriendRequestViewModel() {
        // 👈 使用您最新的仓库类进行初始化
        this.friendRequestRepository = new FriendRequestRepository();
    }

    public LiveData<Boolean> getIsSendSuccess() {
        return isSendSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 发送好友申请 (重构版)
     */
    public void sendFriendRequest(Long targetAccount, String message, String remark) {
        if (targetAccount == null || targetAccount == -1L) {
            errorMessage.setValue("目标账号异常，无法发送请求");
            return;
        }

        isLoading.setValue(true);

        // 1. 【核心修改点】：打包数据至 DTO 实体中
        SendFriendRequestDTO dto = new SendFriendRequestDTO(targetAccount, message, remark);

        // 2. 【核心修改点】：使用您的全新方法签名，将 DTO 传给 Repository 发起请求
        friendRequestRepository.sendFriendRequest(dto, new FriendRequestRepository.FriendRequestCallback<ApiResponse<Void>>() {
            @Override
            public void onSuccess(ApiResponse<Void> response) {
                isLoading.setValue(false);
                if (response != null && response.getCode() == 1000) {
                    isSendSuccess.setValue(true);
                } else {
                    errorMessage.setValue(response != null ? response.getMessage() : "发送失败");
                    isSendSuccess.setValue(false);
                }
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                isSendSuccess.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }
}