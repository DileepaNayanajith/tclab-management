package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SubcultureRepository extends JpaRepository<Subculture, Long> { Optional<Subculture> findByBarcode(String barcode); boolean existsByParentId(Long parentId); long countByStatus(BottleStatus status); }
