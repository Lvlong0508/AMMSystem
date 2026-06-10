package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;

import java.util.List;

public interface SellerProductService {

    ProductWithImageDetailDTO getSellerProductDetail(Long productId);

    List<SellerProductAbstractDTO> getSellerProductsByShopId(Long shopId);

    List<SellerProductAbstractDTO> getSellerProductsAbstract(List<Long> ids);

    boolean listProduct(Long productId);

    boolean unlistProduct(Long productId);

    int deleteProduct(Long productId);
}
