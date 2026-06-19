package ru.rtc.warehouse.product.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.rtc.warehouse.exception.NotFoundException;
import ru.rtc.warehouse.product.model.Product;
import ru.rtc.warehouse.product.repository.ProductRepository;
import ru.rtc.warehouse.product.service.ProductEntityService;

@Service
@RequiredArgsConstructor
public class ProductEntityServiceImpl implements ProductEntityService {

    private final ProductRepository productRepository;

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product update(Product product) {
        return productRepository.save(product);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product findById(Long id) {
        return productRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public Product findByUserIdAndSkuCode(Long userId, String code) {
        return productRepository
            .findByUserIdAndSkuCodeAndIsDeletedFalse(userId, code)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    @Deprecated
    public Product findBySkuCode(String code) {
        return productRepository
            .findBySkuCodeAndIsDeletedFalse(code)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public Product findByUserIdAndSkuCodeAndWarehouseCode(
        Long userId,
        String skuCode,
        String warehouseCode
    ) {
        return productRepository
            .findByUserIdAndSkuCodeAndWarehouseCode(
                userId,
                skuCode,
                warehouseCode
            )
            .orElseThrow(() ->
                new NotFoundException(
                    "Product not found for user=" +
                        userId +
                        ", sku=" +
                        skuCode +
                        ", warehouse=" +
                        warehouseCode
                )
            );
    }

    @Override
    public Product findAnyBySkuCodeAndWarehouseCode(
        String skuCode,
        String warehouseCode
    ) {
        List<Product> products =
            productRepository.findBySkuCodeAndWarehouseCode(
                skuCode,
                warehouseCode
            );
        if (products.isEmpty()) {
            throw new NotFoundException(
                "Product not found for sku=" +
                    skuCode +
                    ", warehouse=" +
                    warehouseCode
            );
        }
        // Если несколько продуктов с одинаковым SKU на складе, возвращаем первый
        return products.get(0);
    }

    @Override
    public Product findAnyBySkuCode(String skuCode) {
        List<Product> products =
            productRepository.findAnyBySkuCodeAndIsDeletedFalse(skuCode);
        if (products.isEmpty()) {
            throw new NotFoundException("Product not found for sku=" + skuCode);
        }
        return products.get(0);
    }

    @Override
    public List<Product> findAllActiveProducts() {
        return productRepository.findAllActiveProducts();
    }

    @Override
    public List<Product> findAllActiveProductsByUserId(Long userId) {
        return productRepository.findAllActiveProductsByUserId(userId);
    }

    @Override
    public Long count() {
        return productRepository.count();
    }

    @Override
    public Product findByNameAndCategory(String name, String category) {
        return productRepository
            .findByNameAndCategoryAndIsDeletedFalse(name, category)
            .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    public List<Product> findAllByWarehouseCode(String warehouseCode) {
        return productRepository.findByWarehouseCodeAndIsDeletedFalse(
            warehouseCode
        );
    }

    @Override
    public List<String> findDistinctCategories() {
        return productRepository.findDistinctCategories();
    }

    @Override
    public List<Object[]> findUserProductsOnWarehouses(Long userId) {
        return productRepository.findUserProductsOnWarehouses(userId);
    }
}
