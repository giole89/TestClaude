package com.finai.repository;

import com.finai.domain.entity.InvestorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, String> {
}
