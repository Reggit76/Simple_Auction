package com.example.simple_auction.repository;

import com.example.simple_auction.model.Lot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotRepository extends JpaRepository<Lot, Integer> {
}