package com.gzasc.aishopping.auth.service.impl;

import com.gzasc.aishopping.auth.mapper.merchant.MerchantInfoMapper;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantInfoServiceImplTest {

    @Mock
    private MerchantInfoMapper merchantInfoMapper;

    @InjectMocks
    private MerchantInfoServiceImpl merchantInfoService;

    @Test
    @DisplayName("getMerchantInfoById 应返回商家信息")
    void getMerchantInfoById_shouldReturnMerchantInfo() {
        MerchantInfo expected = new MerchantInfo();
        expected.setId(1);
        expected.setNickname("商家");
        expected.setAvatar("shop.jpg");

        when(merchantInfoMapper.selectById(1)).thenReturn(expected);

        MerchantInfo result = merchantInfoService.getMerchantInfoById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("商家", result.getNickname());
        assertEquals("shop.jpg", result.getAvatar());
        verify(merchantInfoMapper).selectById(1);
    }

    @Test
    @DisplayName("getMerchantInfoById 当记录不存在时应返回 null")
    void getMerchantInfoById_shouldReturnNullWhenNotFound() {
        when(merchantInfoMapper.selectById(999)).thenReturn(null);

        MerchantInfo result = merchantInfoService.getMerchantInfoById(999);

        assertNull(result);
        verify(merchantInfoMapper).selectById(999);
    }

    @Test
    @DisplayName("createMerchantInfo 应插入并返回生成的 ID")
    void createMerchantInfo_shouldReturnGeneratedId() {
        MerchantInfo info = new MerchantInfo();
        info.setNickname("新商家");

        doAnswer(invocation -> {
            MerchantInfo arg = invocation.getArgument(0);
            arg.setId(5);
            return 1;
        }).when(merchantInfoMapper).insert(any(MerchantInfo.class));

        Integer result = merchantInfoService.createMerchantInfo(info);

        assertEquals(5, result);
        verify(merchantInfoMapper).insert(info);
    }

    @Test
    @DisplayName("createMerchantInfo 当插入后 ID 仍为 null 时应抛出异常")
    void createMerchantInfo_shouldThrowExceptionWhenIdNull() {
        MerchantInfo info = new MerchantInfo();
        info.setNickname("新商家");

        when(merchantInfoMapper.insert(info)).thenReturn(1);

        assertThrows(RuntimeException.class, () -> merchantInfoService.createMerchantInfo(info));
        verify(merchantInfoMapper).insert(info);
    }
}
