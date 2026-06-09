package com.gzasc.aishopping.product.service.impl;

import com.gzasc.aishopping.common.dto.product.ProductCardDTO;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.SellerProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * @deprecated 请直接注入 BuyerProductService / SellerProductService / InternalProductService /
 *             ProductCommandService 等专用服务接口
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final BuyerProductService buyerProductService;
    private final SellerProductService sellerProductService;
    private final InternalProductService internalProductService;
    private final ProductCommandService productCommandService;

    @Override
    public ProductWithImageDetailDTO getProductById(Long productId) {
        return sellerProductService.getSellerProductDetail(productId);
    }

    @Override
    public List<ProductWithImageDetailDTO> getProductsByName(String name) {
        return buyerProductService.getProductsByName(name);
    }

    @Override
    public List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids) {
        return internalProductService.getAbstractProductsForBuyer(ids);
    }

    @Override
    public List<ProductCardDTO> getSalableProductCards(int page) {
        return buyerProductService.getSalableProductCards(page);
    }

    @Override
    public List<ProductCardDTO> getSalableProductCardsByShopId(Long shopId) {
        return buyerProductService.getSalableProductCardsByShopId(shopId);
    }

    @Override
    public List<ProductCardDTO> getProductCardsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page) {
        return buyerProductService.getProductCardsByPriceRange(minPrice, maxPrice, page);
    }

    @Override
    public int deleteProduct(Long productId) {
        return sellerProductService.deleteProduct(productId);
    }

    @Override
    public List<SellerProductAbstractDTO> getSellerProductsByShopId(Long shopId) {
        return sellerProductService.getSellerProductsByShopId(shopId);
    }

    @Override
    public List<SellerProductAbstractDTO> getSellerProductsAbstract(List<Long> ids) {
        return sellerProductService.getSellerProductsAbstract(ids);
    }

    @Override
    public boolean restoreStock(Long productId, int quantity) {
        return internalProductService.restoreStock(productId, quantity);
    }

    @Override
    public boolean listProduct(Long productId) {
        return sellerProductService.listProduct(productId);
    }

    @Override
    public boolean unlistProduct(Long productId) {
        return sellerProductService.unlistProduct(productId);
    }

    @Override
    public ProductDTO getBasicProductById(Long productId) {
        return internalProductService.getBasicProductById(productId);
    }

    @Override
    public int createProductWithImage(Product product, MultipartFile imageFile) {
        return productCommandService.createProductWithImage(product, imageFile);
    }

    @Override
    public int updateProductWithImage(Product product, MultipartFile image) {
        return productCommandService.updateProductWithImage(product, image);
    }
}
