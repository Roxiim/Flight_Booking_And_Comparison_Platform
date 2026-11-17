package com.flightplatform.backend.repository;

import com.flightplatform.backend.entity.Addon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddonRepository extends JpaRepository<Addon, Long> {
    List<Addon> findByName(String name);
}
