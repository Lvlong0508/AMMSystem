package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;

import java.math.BigDecimal;
import java.util.List;

public interface BuyerProductService {

    ProductWithImageDetailDTO getBuyerVisibleProductDetail(Long productId);

    List<ProductWithImageDetailDTO> getProductsByName(String name);

    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids);

    List<ProductCardDTO> getSalableProductCards(int page);

    List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId);

    List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);
}
