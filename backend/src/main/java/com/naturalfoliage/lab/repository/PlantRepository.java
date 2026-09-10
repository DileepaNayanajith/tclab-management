package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PlantRepository extends JpaRepository<Plant, Long> { boolean existsByCode(String code); Optional<Plant> findByCode(String code); }
