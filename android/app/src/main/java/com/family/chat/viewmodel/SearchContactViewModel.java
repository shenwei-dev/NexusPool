package com.family.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.family.chat.common.ApiResponse;
import com.family.chat.vo.SearchUserVO;
import com.family.chat.repository.UserRepository; // 导入您的 UserRepository 包

public class SearchContactViewModel extends ViewModel {

    private final UserRepository userRepository;

    // 存放搜索成功的用户信息
    private final MutableLiveData<SearchUserVO> searchResult = new MutableLiveData<>();

    // 存放错误信息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // 存放加载状态 (转圈)
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public SearchContactViewModel() {
        this.userRepository = new UserRepository();
    }

    public LiveData<SearchUserVO> getSearchResult() {
        return searchResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * 根据账号执行搜索
     * @param queryText 客户端输入框传入的文本
     */
    public void searchUser(String queryText) {
        if (queryText == null || queryText.trim().isEmpty()) {
            errorMessage.setValue("搜索账号不能为空");
            return;
        }

        // 1. 安全转换：将输入的 String 转换为 Long 类型的账号 (对标接口入参)
        long account;
        try {
            account = Long.parseLong(queryText.trim());
        } catch (NumberFormatException e) {
            // 如果用户输入的不是纯数字，直接在前台拦截，提升系统健壮性
            errorMessage.setValue("请输入纯数字的账号进行查询");
            return;
        }

        isLoading.setValue(true);

        // 2. 调用您的 UserRepository 发起异步查询
        userRepository.searchUser(account, new UserRepository.UserCallback<ApiResponse<SearchUserVO>>() {
            @Override
            public void onSuccess(ApiResponse<SearchUserVO> response) {
                isLoading.setValue(false);
                if (response != null && response.getCode() == 1000 && response.getData() != null) {
                    // 查询成功，分发用户实体
                    searchResult.setValue(response.getData());
                } else {
                    // 业务失败（例如找不到该用户）
                    searchResult.setValue(null); // 分发 null，触发 Activity “该用户不存在”的显示
                }
            }

            @Override
            public void onFailure(String error) {
                isLoading.setValue(false);
                searchResult.setValue(null); // 异常断网时，也视为没搜到
                errorMessage.setValue(error);
            }
        });
    }
}