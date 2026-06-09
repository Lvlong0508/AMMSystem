package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * @deprecated 请使用 BuyerProductService / SellerProductService / InternalProductService /
 *             ProductCommandService / ProductShopInfoService 按业务领域调用
 */
@Deprecated
public interface ProductService {

    // ==================== CRUD 操作 ====================

    ProductWithImageDetailDTO getProductById(Long productId);

    List<ProductWithImageDetailDTO> getProductsByName(String name);

    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids);

    List<ProductCardDTO> getSalableProductCards(int page);

    List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId);

    List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);

    int deleteProduct(Long productId);

    List<SellerProductAbstractDTO> getSellerProductsByShopId(Long shopId);

    List<SellerProductAbstractDTO> getSellerProductsAbstract(List<Long> ids);

    // ==================== 库存管理 ====================

    boolean restoreStock(Long productId, int quantity);

    // ==================== 上下架管理 ====================

    boolean listProduct(Long productId);

    boolean unlistProduct(Long productId);

    ProductDTO getBasicProductById(Long productId);

    // ==================== 统一创建/更新（含图片） ====================

    int createProductWithImage(Product product, MultipartFile imageFile);

    int updateProductWithImage(Product product, MultipartFile image);

}
