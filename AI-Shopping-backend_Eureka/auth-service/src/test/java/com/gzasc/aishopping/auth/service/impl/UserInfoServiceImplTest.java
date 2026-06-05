package com.gzasc.aishopping.auth.service.impl;

import com.gzasc.aishopping.auth.mapper.user.UserInfoMapper;
import com.gzasc.aishopping.auth.model.UserInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    @Mock
    private UserInfoMapper userInfoMapper;

    @InjectMocks
    private UserInfoServiceImpl userInfoService;

    @Test
    @DisplayName("getUserInfoById 应返回用户信息")
    void getUserInfoById_shouldReturnUserInfo() {
        UserInfo expected = new UserInfo();
        expected.setId(1);
        expected.setNickname("测试");
        expected.setAvatar("avatar.jpg");

        when(userInfoMapper.selectById(1)).thenReturn(expected);

        UserInfo result = userInfoService.getUserInfoById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("测试", result.getNickname());
        assertEquals("avatar.jpg", result.getAvatar());
        verify(userInfoMapper).selectById(1);
    }

    @Test
    @DisplayName("getUserInfoById 当记录不存在时应返回 null")
    void getUserInfoById_shouldReturnNullWhenNotFound() {
        when(userInfoMapper.selectById(999)).thenReturn(null);

        UserInfo result = userInfoService.getUserInfoById(999);

        assertNull(result);
        verify(userInfoMapper).selectById(999);
    }

    @Test
    @DisplayName("createUserInfo 应插入并返回生成的 ID")
    void createUserInfo_shouldReturnGeneratedId() {
        UserInfo info = new UserInfo();
        info.setNickname("新用户");

        doAnswer(invocation -> {
            UserInfo arg = invocation.getArgument(0);
            arg.setId(1);
            return 1;
        }).when(userInfoMapper).insert(any(UserInfo.class));

        Integer result = userInfoService.createUserInfo(info);

        assertNotNull(result);
        assertEquals(1, result);
        verify(userInfoMapper).insert(info);
    }

    @Test
    @DisplayName("createUserInfo 当插入后 ID 仍为 null 时应抛出异常")
    void createUserInfo_shouldThrowExceptionWhenIdNull() {
        UserInfo info = new UserInfo();
        info.setNickname("新用户");

        when(userInfoMapper.insert(info)).thenReturn(1);

        assertThrows(RuntimeException.class, () -> userInfoService.createUserInfo(info));
        verify(userInfoMapper).insert(info);
    }

    @Test
    @DisplayName("updateUserInfo 应调用 mapper.update")
    void updateUserInfo_shouldCallMapperUpdate() {
        UserInfo info = new UserInfo();
        info.setId(1);
        info.setNickname("新昵称");
        info.setAvatar("new.jpg");

        userInfoService.updateUserInfo(info);

        verify(userInfoMapper).update(info);
    }
}
