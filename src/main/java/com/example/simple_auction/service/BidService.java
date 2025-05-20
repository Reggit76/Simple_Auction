package com.example.simple_auction.service;

import com.example.simple_auction.model.Bid;
import com.example.simple_auction.model.Lot;
import com.example.simple_auction.model.LotStatus;
import com.example.simple_auction.model.User;
import com.example.simple_auction.repository.BidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BidService {

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private LotService lotService;

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

    public void placeBid(Integer lotId, BigDecimal bidAmount, User bidder) {
        Lot lot = lotService.getLotById(lotId);
        if (lot == null || LotStatus.CANCELLED.equals(lot.getStatus()) || LotStatus.SOLD.equals(lot.getStatus())) {
            throw new IllegalStateException("Cannot place a bid on this lot");
        }

        if (bidder.getBalance().compareTo(bidAmount) < 0) {
            throw new IllegalStateException("Insufficient funds to place bid");
        }

        BigDecimal minRequiredBid;
        if (lot.getCurrentBid() == null) {
            minRequiredBid = lot.getStartingPrice();
        } else {
            minRequiredBid = lot.getCurrentBid().add(lot.getMinBidStep());
        }
        if (bidAmount.compareTo(minRequiredBid) < 0) {
            throw new IllegalStateException("Bid must be at least " + minRequiredBid);
        }

        Bid bid = new Bid();
        bid.setLot(lot);
        bid.setUser(bidder);
        bid.setBidAmount(bidAmount);
        bid.setTimestamp(LocalDateTime.now());
        bidRepository.save(bid);

        lot.setCurrentBid(bidAmount);
        lotService.saveLot(lot, lot.getOwner());
    }

    public List<Bid> getBidsByLot(Integer lotId) {
        return bidRepository.findByLotId(lotId);
    }
}