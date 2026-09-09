package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.DiscardRecord;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DiscardRepository extends JpaRepository<DiscardRecord, Long> { boolean existsByBarcode(String barcode); }
