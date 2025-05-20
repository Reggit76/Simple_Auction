package com.example.simple_auction.service;

import com.example.simple_auction.model.Lot;
import com.example.simple_auction.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LotService {
    @Autowired
    private LotRepository lotRepository;

    public List<Lot> getAllLots() {
        return lotRepository.findAll();
    }

    public Lot getLotById(Integer id) {
        return lotRepository.findById(id).orElse(null);
    }

    public Lot saveLot(Lot lot) {
        return lotRepository.save(lot);
    }

    public void deleteLot(Integer id) {
        lotRepository.deleteById(id);
    }
}