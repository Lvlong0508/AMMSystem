package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.dto.ProductAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductDetailDTO;
import com.gzasc.aishopping.product.dto.ProductImageDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    // ==================== CRUD 操作 ====================

    /**
     * 根据商品ID查询商品详情
     * @param productId 商品ID
     * @return 商品信息，null表示不存在
     */
    ProductWithImageDetailDTO getProductById(String productId);

    /**
     * 根据商品名称模糊搜索
     * @param name 商品名称关键字
     * @return 符合条件的商品列表
     */
    List<ProductWithImageDetailDTO> getProductsByName(String name);

    /**
     * 用户端: 根据可售商品ID批量查询抽象信息
     * (id, name, price, tags, imageId)
     */
    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<String> ids);

    /**
     * 用户端: 分页查询可售商品抽象信息
     */
    List<ProductWithImageAbstractDTO> getSalableProductsAbstract(int page);

    /**
     * 创建新商品
     * @param product 商品信息
     * @return 影响的行数
     */
    int createProduct(Product product);

    /**
     * 删除商品
     * @param productId 商品ID
     * @return 影响的行数
     */
    int deleteProduct(String productId);

    /**
     * 更新商品信息
     * @param product 商品信息（需包含ID）
     * @return 影响的行数
     */
    int updateProduct(Product product);

    /**
     * 商家端: 根据商品ID批量查询抽象信息（包含isSale字段）
     * (id, name, price, tags, isSale, imageId)
     */
    List<ProductWithImageAbstractDTO> getAbstractProductsForMerchant(List<String> ids);


    // ==================== 库存管理 ====================

    /**
     * 扣减商品库存（用于下单）
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功（库存不足返回false）
     */
    boolean deductStock(String productId, int quantity);

    /**
     * 恢复商品库存（用于取消订单、退货）
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否恢复成功
     */
    boolean restoreStock(String productId, int quantity);


    // ==================== 图片管理 ====================

    /**
     * 添加商品图片
     * @param image 图片信息
     * @return 影响的行数
     */
    int addImage(ProductImageInfo image);

    /**
     * 删除商品图片
     * @param imageId 图片ID
     * @return 影响的行数
     */
    int removeImage(int imageId);

    /**
     * 根据图片ID查询图片
     * @param imageId 图片ID
     * @return 图片信息，null表示不存在
     */
    ProductImageInfo getImageById(int imageId);

    /**
     * 批量查询图片
     * @param ids 图片ID列表
     * @return 对应的图片列表
     */
    List<ProductImageInfo> getImagesByIds(List<Integer> ids);


    // ==================== 上下架管理 ====================

    /**
     * 上架商品
     * @param productId 商品ID
     * @return 是否上架成功
     */
    boolean listProduct(String productId);

    /**
     * 下架商品
     * @param productId 商品ID
     * @return 是否下架成功
     */
    boolean unlistProduct(String productId);

    /**
     * 检查商品是否可售
     * @param productId 商品ID
     * @return 是否可售
     */
    boolean isProductSalable(String productId);


    // ==================== 高级查询 ====================

    /**
     * 根据价格区间查询商品（无分页）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 对应的商品列表
     */
    List<ProductWithImageAbstractDTO> getProductsByPriceRange(Double minPrice, Double maxPrice);

    /**
     * 分页按价格区间查询商品
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param page 页码，从0开始
     * @return 当前页的商品列表
     */
    List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);

    // ==================== DTO 查询接口 ====================

    /**
     * 用户端: 根据ID批量查询抽象信息 DTO
     */
    List<ProductAbstractDTO> getAbstractProductDTOs(List<String> ids);

    /**
     * 用户端: 根据ID查询商品详情 DTO
     */
    ProductDetailDTO getProductDetailDTO(String productId);

    /**
     * 商家端: 根据ID批量查询抽象信息 DTO（包含isSale）
     */
    List<ProductAbstractDTO> getMerchantAbstractProductDTOs(List<String> ids);

    /**
     * 根据图片ID查询图片 DTO
     */
    ProductImageDTO getImageDTO(int imageId);

    /**
     * 批量查询图片 DTO
     */
    List<ProductImageDTO> getImageDTOs(List<Integer> ids);


    // ==================== 统一创建/更新（含图片） ====================

    /**
     * 创建商品并关联图片（同一事务：先写图片 → 获取ID → 再写商品）
     * @param product 商品信息（不含 imageId，由方法内部设置）
     * @param imageUrl 图片URL，为空则不创建图片
     * @return 影响的行数
     */
    int createProductWithImage(Product product, String imageUrl);

    /**
     * 更新商品并关联新图片（同一事务：先写图片 → 获取ID → 再更新商品）
     * @param product 商品基本信息（需包含ID）
     * @param imageUrl 新图片URL，为空则不更新图片
     * @return 影响的行数
     */
    int updateProductWithImage(Product product, String imageUrl);

    /**
     * 根据店铺ID分页查询商品
     * @param shopId 店铺ID
     * @param page 页码，从1开始
     * @param size 每页数量
     * @return 商品列表
     */
    List<ProductWithImageAbstractDTO> getProductsByShopId(Long shopId, int page, int size);
}