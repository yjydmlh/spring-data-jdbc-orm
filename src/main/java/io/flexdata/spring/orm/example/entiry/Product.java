package io.flexdata.spring.orm.example.entiry;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 产品实体示例 - 展示多种数据库类型支持
 * 包含PostgreSQL、MySQL等数据库的特殊数据类型
 */
@Table("products")
public class Product {
    
    @Id
    private Long id;
    
    @Column("product_uuid")
    private UUID productUuid;  // PostgreSQL UUID类型
    
    private String name;
    
    private String description;
    
    private BigDecimal price;
    
    @Column("category_id")
    private Integer categoryId;
    
    @Column("tag_list")
    private String[] tags;  // PostgreSQL 数组类型
    
    @Column("attributes")
    private Map<String, Object> attributes;  // JSON类型（MySQL/PostgreSQL）
    
    @Column("specifications")
    private List<String> specifications;  // JSON数组类型
    
    @Column("status")
    private ProductStatus status;  // 枚举类型
    
    @Column("image_data")
    private byte[] imageData;  // BLOB类型
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    @Column("updated_at")
    private LocalDateTime updatedAt;
    
    // 构造函数
    public Product() {
        this.productUuid = UUID.randomUUID();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Product(String name, String description, BigDecimal price) {
        this();
        this.name = name;
        this.description = description;
        this.price = price;
        this.status = ProductStatus.ACTIVE;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public UUID getProductUuid() {
        return productUuid;
    }
    
    public void setProductUuid(UUID productUuid) {
        this.productUuid = productUuid;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }
    
    public String[] getTags() {
        return tags;
    }
    
    public void setTags(String[] tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
    
    public List<String> getSpecifications() {
        return specifications;
    }
    
    public void setSpecifications(List<String> specifications) {
        this.specifications = specifications;
    }
    
    public ProductStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProductStatus status) {
        this.status = status;
    }
    
    public byte[] getImageData() {
        return imageData;
    }
    
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 产品状态枚举
     */
    public enum ProductStatus {
        ACTIVE,
        INACTIVE,
        DISCONTINUED,
        OUT_OF_STOCK
    }
    
    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", productUuid=" + productUuid +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", categoryId=" + categoryId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}