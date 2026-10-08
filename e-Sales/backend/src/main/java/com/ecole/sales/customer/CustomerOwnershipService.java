package com.ecole.sales.customer;

import com.ecole.sales.common.ApiException;
import com.ecole.sales.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerOwnershipService {
    private final CustomerOwnershipRepository ownership;private final SalesUserRepository users;
    public CustomerOwnershipService(CustomerOwnershipRepository ownership,SalesUserRepository users){this.ownership=ownership;this.users=users;}
    @Transactional public void assign(String productId,String tenantId,String customerName,Long ownerId){
        SalesUser owner=null;
        if(ownerId!=null){
            owner=users.findById(ownerId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Sales user not found"));
            if(!"SALES_USER".equals(owner.getRole()))throw new ApiException(HttpStatus.BAD_REQUEST,"Owner must be a sales user");
        }
        CustomerOwnership row=ownership.findByProductIdAndTenantId(productId,tenantId).orElse(null);
        if(row==null)row=new CustomerOwnership(productId,tenantId,owner,customerName);
        row.setOwner(owner);ownership.save(row);
    }
}
