package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PlantRepository extends JpaRepository<Plant, Long> { boolean existsByCode(String code); }
