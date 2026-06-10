package com.gzasc.aishopping.auth.service.impl;

import com.gzasc.aishopping.auth.mapper.merchant.MerchantInfoMapper;
import com.gzasc.aishopping.auth.model.MerchantInfo;
import com.gzasc.aishopping.auth.service.MerchantInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantInfoServiceImpl implements MerchantInfoService {

    private final MerchantInfoMapper merchantInfoMapper;

    @Override
    public MerchantInfo getMerchantInfoById(Integer id) {
        return merchantInfoMapper.selectById(id);
    }

    @Override
    public Integer createMerchantInfo(MerchantInfo info) {
        merchantInfoMapper.insert(info);
        if (info.getId() == null) {
            throw new RuntimeException("创建商家信息失败");
        }
        return info.getId();
    }
}