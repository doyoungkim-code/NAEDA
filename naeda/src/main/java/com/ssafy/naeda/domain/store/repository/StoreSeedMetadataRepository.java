package com.ssafy.naeda.domain.store.repository;

import com.ssafy.naeda.domain.store.entity.StoreSeedMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSeedMetadataRepository extends JpaRepository<StoreSeedMetadata, String> {
}
