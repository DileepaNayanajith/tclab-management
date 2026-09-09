package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MotherBottleRepository extends JpaRepository<MotherBottle, Long> { Optional<MotherBottle> findByBarcode(String barcode); long countByStatus(BottleStatus status); }
