package com.family.chat.viewmodel;

import android.text.TextUtils;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.family.chat.common.ApiResponse;
import com.family.chat.dto.UserRegisterDTO;
import com.family.chat.vo.UserRegisterVO;
import com.family.chat.repository.AuthRepository;

public class RegisterViewModel extends ViewModel {

    private final AuthRepository authRepository;

    private final MutableLiveData<ApiResponse<UserRegisterVO>> registrationResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public RegisterViewModel() {
        this.authRepository = new AuthRepository();
    }

    public LiveData<ApiResponse<UserRegisterVO>> getRegistrationResult() {
        return registrationResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 【重构业务方法】：只接收密码输入，由服务端自动分配账号
     */
    public void register(String password) {
        // 密码非空校验
        if (TextUtils.isEmpty(password) || password.trim().isEmpty()) {
            errorMessage.setValue("密码不能为空");
            return;
        }

        String trimmedPassword = password.trim();

        // 【微信级密码规则】 8-16位，必须是英文字母、数字或字符组合，严禁纯数字和纯字母
        String passwordRegex = "^(?![0-9]+$)(?![a-zA-Z]+$)[0-9a-zA-Z\\W]{8,16}$";

        if (!trimmedPassword.matches(passwordRegex)) {
            errorMessage.setValue("密码必须为8-16位，且包含字母和数字的组合");
            return;
        }

        isLoading.setValue(true);

        UserRegisterDTO userRegisterDTO = new UserRegisterDTO(trimmedPassword);

        // 4. 发起异步注册请求
        authRepository.register(userRegisterDTO, new AuthRepository.AuthCallback<ApiResponse<UserRegisterVO>>() {
            @Override
            public void onSuccess(ApiResponse<UserRegisterVO> response) {
                isLoading.setValue(false);
                if (response != null && response.getCode() == 1000) {
                    registrationResult.postValue(response);
                } else {
                    errorMessage.postValue(response != null ? response.getMessage() : "注册失败");
                }
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                errorMessage.postValue(error);
            }
        });
    }
}