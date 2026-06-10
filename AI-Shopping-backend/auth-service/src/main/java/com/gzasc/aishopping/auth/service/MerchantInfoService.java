package com.gzasc.aishopping.auth.service;

import com.gzasc.aishopping.auth.model.MerchantInfo;

public interface MerchantInfoService {

    MerchantInfo getMerchantInfoById(Integer id);

    Integer createMerchantInfo(MerchantInfo info);
}