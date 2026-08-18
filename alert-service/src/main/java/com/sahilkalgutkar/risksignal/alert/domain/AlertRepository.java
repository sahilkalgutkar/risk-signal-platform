package com.sahilkalgutkar.risksignal.alert.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, String> {
}
