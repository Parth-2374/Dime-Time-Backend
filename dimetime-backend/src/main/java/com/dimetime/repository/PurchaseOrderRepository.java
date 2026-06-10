package com.dimetime.repository;

import com.dimetime.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    List<PurchaseOrder> findBySupplierOrderByCreatedAtDesc(String supplier);
    List<PurchaseOrder> findByManufacturerOrderByCreatedAtDesc(String manufacturer);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @Query("SELECT po FROM PurchaseOrder po WHERE po.supplierUser.id = :supplierId ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT po FROM PurchaseOrder po WHERE po.manufacturerUser.id = :manufacturerId ORDER BY po.createdAt DESC")
    List<PurchaseOrder> findByManufacturerId(@Param("manufacturerId") Long manufacturerId);
}
