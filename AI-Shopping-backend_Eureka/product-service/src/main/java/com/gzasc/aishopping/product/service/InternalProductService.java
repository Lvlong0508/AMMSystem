package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;

import java.util.List;

public interface InternalProductService {

    ProductWithImageDetailDTO getInternalProductDetail(Long productId);

    ProductDTO getBasicProductById(Long productId);

    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids);

    boolean restoreStock(Long productId, int quantity);
}
