package com.ssafy.naeda.domain.store.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "store_seed_metadata")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class StoreSeedMetadata {

    @Id
    @Column(name = "seed_key", length = 100)
    private String seedKey;

    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    @Column(name = "updated", nullable = false)
    private LocalDateTime updated;

    public void updateHash(String contentHash, LocalDateTime updated) {
        this.contentHash = contentHash;
        this.updated = updated;
    }
}
