package com.sssupermarket;

import com.sssupermarket.dto.ProductResponse;
import com.sssupermarket.exception.ProductNotFoundException;
import com.sssupermarket.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Test
    void listProducts_returnsSeededProducts() {
        List<ProductResponse> products = productService.listProducts(null, null);

        assertThat(products).isNotEmpty();
        assertThat(products).extracting(ProductResponse::getName)
                .contains("Laptop", "Smartphone", "Headphones", "Keyboard", "Smart Watch");
    }

    @Test
    void search_findsProductsByName() {
        List<ProductResponse> results = productService.listProducts("laptop", null);

        assertThat(results).isNotEmpty();
        assertThat(results).extracting(ProductResponse::getName)
                .contains("Laptop");
    }

    @Test
    void categoryFilter_returnsOnlyMatchingCategory() {
        List<ProductResponse> electronics = productService.listProducts(null, "Electronics");

        assertThat(electronics).isNotEmpty();
        assertThat(electronics).allMatch(p -> p.getCategory().equals("Electronics"));
    }

    @Test
    void searchCombinedWithCategory() {
        List<ProductResponse> results = productService.listProducts("a", "Accessories");

        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(p -> p.getCategory().equals("Accessories"));
        assertThat(results).extracting(ProductResponse::getName)
                .contains("Headphones", "Keyboard");
    }

    @Test
    void search_withNoMatches_returnsEmpty() {
        List<ProductResponse> results = productService.listProducts("zzzznotfound", null);

        assertThat(results).isEmpty();
    }

    @Test
    void getProduct_returnsValidProduct() {
        List<ProductResponse> all = productService.listProducts(null, null);
        ProductResponse expected = all.get(0);

        ProductResponse product = productService.getProduct(expected.getId());

        assertThat(product.getId()).isEqualTo(expected.getId());
        assertThat(product.getName()).isEqualTo(expected.getName());
    }

    @Test
    void getProduct_invalidId_throws() {
        assertThatThrownBy(() -> productService.getProduct(999999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getAllCategories_returnsDistinctSeedCategories() {
        List<String> categories = productService.getAllCategories();

        assertThat(categories).contains("Electronics", "Accessories", "Wearables");
    }
}
