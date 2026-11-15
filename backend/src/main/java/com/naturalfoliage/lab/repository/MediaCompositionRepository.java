package com.naturalfoliage.lab.repository;
import com.naturalfoliage.lab.model.MediaComposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface MediaCompositionRepository extends JpaRepository<MediaComposition, Long> {
    Optional<MediaComposition> findByCode(String code);

    @Modifying(flushAutomatically = true)
    @Query("update MediaComposition m set m.availableBottles = m.availableBottles - :count where m.id = :id and m.availableBottles >= :count")
    int consumeBottles(@Param("id") Long id, @Param("count") int count);
}
