package com.example.simple_auction.repository;

import com.example.simple_auction.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Integer> {
    List<Bid> findByLotId(Integer lotId);
    Bid findTopByLotIdOrderByBidAmountDesc(Integer lotId);
}