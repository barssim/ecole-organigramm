package com.ecole.sales.customer;

import com.ecole.sales.user.SalesUser;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="customer_ownership", uniqueConstraints=@UniqueConstraint(name="customer_product_tenant_unique",columnNames={"product_id","tenant_id"}))
public class CustomerOwnership {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="product_id",nullable=false,length=24) private String productId;
    @Column(name="tenant_id",nullable=false,length=160) private String tenantId;
    @ManyToOne(fetch=FetchType.EAGER) @JoinColumn(name="owner_id") private SalesUser owner;
    @Column(name="customer_name",nullable=false,length=200) private String customerName;
    @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now();
    protected CustomerOwnership(){}
    public CustomerOwnership(String productId,String tenantId,SalesUser owner,String customerName){this.productId=productId;this.tenantId=tenantId;this.owner=owner;this.customerName=customerName;}
    public Long getId(){return id;} public String getProductId(){return productId;} public String getTenantId(){return tenantId;}
    public SalesUser getOwner(){return owner;} public String getCustomerName(){return customerName;} public Instant getCreatedAt(){return createdAt;}
    public void setOwner(SalesUser owner){this.owner=owner;}
}
