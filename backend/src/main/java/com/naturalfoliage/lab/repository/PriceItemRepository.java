package com.naturalfoliage.lab.repository;

import com.naturalfoliage.lab.model.PriceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PriceItemRepository extends JpaRepository<PriceItem, Long> {
    Optional<PriceItem> findByPlantCode(String plantCode);
}
