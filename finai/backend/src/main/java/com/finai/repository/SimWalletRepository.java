package com.finai.repository;

import com.finai.domain.entity.SimWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimWalletRepository extends JpaRepository<SimWallet, String> {
}
