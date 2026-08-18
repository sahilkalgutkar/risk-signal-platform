package com.sahilkalgutkar.risksignal.riskscoring.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskScoreRepository extends JpaRepository<RiskScoreEntity, String> {
}
