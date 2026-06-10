package com.dimetime.repository;

import com.dimetime.entity.MaterialUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialUploadRepository extends JpaRepository<MaterialUpload, Long> {
    Optional<MaterialUpload> findTopByOrderByUploadedAtDesc();
    List<MaterialUpload> findByHeatNumber(String heatNumber);
    Optional<MaterialUpload> findTopByPoNumberOrderByUploadedAtDesc(String poNumber);
}
