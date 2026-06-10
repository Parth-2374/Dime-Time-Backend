package com.dimetime.repository;

import com.dimetime.entity.MtcDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MtcDocumentRepository extends JpaRepository<MtcDocument, Long> {
    Optional<MtcDocument> findTopByOrderByUploadedAtDesc();
    List<MtcDocument> findByHeatNumber(String heatNumber);
    Optional<MtcDocument> findTopByPoNumberOrderByUploadedAtDesc(String poNumber);
    List<MtcDocument> findByPoNumberOrderByUploadedAtDesc(String poNumber);
}
