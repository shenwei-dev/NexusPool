package com.family.chat.service.impl;

import com.family.chat.common.result.ResultCode;
import com.family.chat.dto.SearchUserDTO;
import com.family.chat.dto.QueryUserInfoDTO;
import com.family.chat.entity.User;
import com.family.chat.exception.ServiceException;
import com.family.chat.mapper.UserMapper;
import com.family.chat.service.OssService;
import com.family.chat.service.UserService;
import com.family.chat.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final OssService ossService;

    @Override
    public QueryUserInfoDTO getUserById() {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        QueryUserInfoDTO queryUserInfoDTO = new QueryUserInfoDTO();
        BeanUtils.copyProperties(user, queryUserInfoDTO);
        queryUserInfoDTO.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
        return queryUserInfoDTO;
    }

    @Override
    public SearchUserDTO queryUserByAccount(Long account) {
        User user = userMapper.selectByAccount(account);
        if (user == null) {
            return null;
        }
        SearchUserDTO searchUserDTO = new SearchUserDTO();
        BeanUtils.copyProperties(user, searchUserDTO);
        searchUserDTO.setAvatarUrl(ossService.resolveAvatarUrl(user.getAvatarUrl()));
        return searchUserDTO;
    }

    @Override
    public String updateAvatar(String avatarObjectKey) {
        Long userId = SecurityUtil.getCurrentUserId();
        // Only accept object keys issued by this user's own presign flow:
        // 1. reject raw URLs / blank values
        // 2. enforce the per-user key prefix so nobody can bind another user's object
        // 3. trusted ledger check proves the key was actually issued by the presign endpoint
        String expectedPrefix = "chat/avatars/" + userId + "/";
        if (avatarObjectKey == null || avatarObjectKey.isBlank()
                || avatarObjectKey.startsWith("http")
                || !avatarObjectKey.startsWith(expectedPrefix)
                || !ossService.isKeyIssued(avatarObjectKey)) {
            throw new ServiceException(ResultCode.PARAM_ERROR);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        user.setAvatarUrl(avatarObjectKey);
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateAvatarUrl(user);
        // Return an accessible signed URL so the client can refresh its cache immediately
        return ossService.resolveAvatarUrl(avatarObjectKey);
    }

    @Override
    public void updateNickname(String nickname) {
        Long userId = SecurityUtil.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }
        user.setNickname(nickname);
        user.setUpdatedAt(System.currentTimeMillis());
        userMapper.updateNickname(user);
    }
}
