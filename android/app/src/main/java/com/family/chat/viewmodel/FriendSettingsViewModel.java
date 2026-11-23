package com.family.chat.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.family.chat.common.ApiResponse;
import com.family.chat.repository.FriendRepository;

public class FriendSettingsViewModel extends ViewModel {

    private final FriendRepository friendRepository;

    private final MutableLiveData<Boolean> blacklistStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isDeleteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public FriendSettingsViewModel() {
        this.friendRepository = new FriendRepository();
    }

    public LiveData<Boolean> getBlacklistStatus() {
        return blacklistStatus;
    }

    public LiveData<Boolean> getIsDeleteSuccess() {
        return isDeleteSuccess;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 删除好友
     */
    public void deleteFriend(Long friendId) {
        isLoading.postValue(true);

        friendRepository.deleteFriend(friendId, new FriendRepository.FriendCallback<ApiResponse<Void>>() {
            @Override
            public void onSuccess(ApiResponse<Void> response) {
                Log.d("DeleteFriend", "onSuccess called");
                isLoading.postValue(false);
                isDeleteSuccess.postValue(true);
            }

            @Override
            public void onFailure(String error) {
                Log.d("DeleteFriend", "onSuccess called");
                isLoading.postValue(false);
                isDeleteSuccess.postValue(false);
                toastMessage.postValue(error);
            }
        });
    }
}
