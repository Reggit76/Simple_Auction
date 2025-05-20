package com.example.simple_auction.service;

import com.example.simple_auction.model.Bid;
import com.example.simple_auction.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BidService {
    @Autowired
    private BidRepository bidRepository;

    public List<Bid> getAllBids() {
        return bidRepository.findAll();
    }

    public Bid getBidById(Integer id) {
        return bidRepository.findById(id).orElse(null);
    }

    public Bid saveBid(Bid bid) {
        return bidRepository.save(bid);
    }

    public void deleteBid(Integer id) {
        bidRepository.deleteById(id);
    }
}