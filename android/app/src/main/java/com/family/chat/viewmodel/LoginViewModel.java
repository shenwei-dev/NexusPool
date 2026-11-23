package com.family.chat.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.family.chat.common.ApiResponse;
import com.family.chat.dto.UserLoginDTO;
import com.family.chat.vo.UserLoginVO;
import com.family.chat.repository.AuthRepository;

public class LoginViewModel extends AndroidViewModel {

    public LoginViewModel(@NonNull Application application) {
        super(application);
    }

    private final AuthRepository authRepository = new AuthRepository();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public LiveData<String> getSuccessMessage() { return successMessage; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void login(Long account, String password) {
        // 前端校验
        if (account == null || account.toString().trim().isEmpty()) {
            errorMessage.setValue("用户名不能为空");
            return;
        }
        if (password == null || password.trim().isEmpty()) {
            errorMessage.setValue("密码不能为空");
            return;
        }
        if (!account.toString().matches("^\\d{10}$")) {
            errorMessage.setValue("用户名必须是10位数字");
            return;
        }
        @SuppressLint("HardwareIds")
        UserLoginDTO userLoginDTO = new UserLoginDTO(account, password.trim());

        authRepository.login(userLoginDTO, new AuthRepository.AuthCallback<>() {
            @Override
            public void onSuccess(ApiResponse<UserLoginVO> response) {
                successMessage.postValue(response.getMessage());
            }

            @Override
            public void onFailure(String error) {
                errorMessage.postValue(error);
            }
        });
    }
}
