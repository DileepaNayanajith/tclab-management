package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.MediaComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MediaCompositionRepository extends JpaRepository<MediaComposition, Long> { Optional<MediaComposition> findByCode(String code); }
