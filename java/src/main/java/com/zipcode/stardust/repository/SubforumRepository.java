package com.zipcode.stardust.repository;

import com.zipcode.stardust.model.Subforum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubforumRepository extends JpaRepository<Subforum, Long> {
    List<Subforum> findByParentIsNull();
    List<Subforum> findByParent(Subforum parent);
    Optional<Subforum> findByTitle(String title);
}
