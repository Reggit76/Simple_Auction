package com.example.simple_auction.service;

import com.example.simple_auction.model.Comment;
import com.example.simple_auction.model.Lot;
import com.example.simple_auction.model.LotStatus;
import com.example.simple_auction.model.User;
import com.example.simple_auction.repository.CommentRepository;
import com.example.simple_auction.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LotService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private CommentRepository commentRepository;

    public List<Lot> getAllLots() {
        return lotRepository.findAll();
    }

    public Lot getLotById(Integer id) {
        return lotRepository.findById(id).orElse(null);
    }

    public Lot saveLot(Lot lot, User creator) {
        lot.setOwner(creator);
        return lotRepository.save(lot);
    }

    public void deleteLot(Integer id, User user) {
        if (!user.isAdmin()) {
            throw new SecurityException("Only administrators can delete lots");
        }
        lotRepository.deleteById(id);
    }

    public List<Lot> getActiveLots() {
        return lotRepository.findAll().stream()
                .filter(lot -> lot.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }

    public List<Lot> searchLots(String searchTerm) {
        return lotRepository.findAll().stream()
                .filter(lot -> lot.getTitle().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        lot.getDescription().toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void cancelAuction(Integer lotId, User user) {
        if (!user.isAdmin()) {
            throw new SecurityException("Only administrators can cancel auctions");
        }
        Lot lot = getLotById(lotId);
        if (lot != null) {
            lot.setStatus(LotStatus.CANCELLED);
            lotRepository.save(lot);
        }
    }

    public void addComment(Integer lotId, String commentText, User user) {
        Lot lot = getLotById(lotId);
        if (lot != null) {
            Comment comment = new Comment();
            comment.setLot(lot);
            comment.setUser(user);
            comment.setText(commentText);
            comment.setTimestamp(LocalDateTime.now());
            commentRepository.save(comment);
        }
    }
}