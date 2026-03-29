package com.ssafy.naeda.domain.address.repository;

import com.ssafy.naeda.domain.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserNo (Long userNo);

    Optional<Address> findByUserNoAndIsDefaultTrue(Long userNo);

    void deleteByUserNo(Long userNo);
}
