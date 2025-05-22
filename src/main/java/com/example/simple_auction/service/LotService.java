package com.example.simple_auction.service;

import com.example.simple_auction.model.Bid;
import com.example.simple_auction.model.Comment;
import com.example.simple_auction.model.Lot;
import com.example.simple_auction.model.User;
import com.example.simple_auction.repository.BidRepository;
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

    @Autowired
    private BidRepository bidRepository;

    public List<Lot> getAllLots() {
        return lotRepository.findAll();
    }

    public Lot getLotById(Integer id) {
        return lotRepository.findById(id).orElse(null);
    }

    public Lot saveLot(Lot lot, User creator) {
        if (lot.getId() == null) { // Новый лот
            if (lot.getTitle() == null || lot.getStartingPrice() == null || lot.getMinBidStep() == null || lot.getEndTime() == null) {
                throw new IllegalArgumentException("Все обязательные поля должны быть заполнены");
            }
            lot.setOwner(creator); // Устанавливаем владельца только для нового лота
            lot.setStatus("ACTIVE");
        } else { // Существующий лот
            Lot existingLot = lotRepository.findById(lot.getId()).orElse(null);
            if (existingLot != null) {
                // Обновляем только необходимые поля, сохраняя существующего владельца
                existingLot.setTitle(lot.getTitle());
                existingLot.setDescription(lot.getDescription());
                existingLot.setImageURL(lot.getImageURL());
                existingLot.setStartingPrice(lot.getStartingPrice());
                existingLot.setMinBidStep(lot.getMinBidStep());
                existingLot.setEndTime(lot.getEndTime());
                existingLot.setStatus(lot.getStatus());
                existingLot.setWinner(lot.getWinner());
                lot = existingLot; // Используем существующий объект с неизменным owner
            }
        }
        return lotRepository.save(lot);
    }

    public Lot updateLot(Integer id, Lot updatedLot, User user) {
        Lot lot = getLotById(id);
        if (lot == null) {
            throw new IllegalArgumentException("Лот с ID " + id + " не найден");
        }
        if (!user.isAdmin() && !lot.getOwner().getId().equals(user.getId())) {
            throw new SecurityException("У вас нет прав для редактирования этого лота");
        }
        lot.setTitle(updatedLot.getTitle());
        lot.setDescription(updatedLot.getDescription());
        lot.setImageURL(updatedLot.getImageURL());
        lot.setStartingPrice(updatedLot.getStartingPrice());
        lot.setMinBidStep(updatedLot.getMinBidStep());
        lot.setEndTime(updatedLot.getEndTime());
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
                .filter(lot -> lot.getEndTime().isAfter(LocalDateTime.now()) && lot.getStatus().equals("ACTIVE"))
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
            lot.setStatus("CANCELLED");
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

    public List<Comment> getCommentsByLot(Integer lotId) {
        return commentRepository.findByLotId(lotId);
    }

    public void finalizeAuction(Integer lotId) {
        Lot lot = getLotById(lotId);
        if (lot != null && lot.getEndTime().isBefore(LocalDateTime.now()) && lot.getStatus().equals("ACTIVE")) {
            Bid highestBid = bidRepository.findTopByLotIdOrderByBidAmountDesc(lotId);
            if (highestBid != null) {
                lot.setWinner(highestBid.getUser());
                lot.setStatus("SOLD");
            } else {
                lot.setStatus("EXPIRED");
            }
            lotRepository.save(lot);
        }
    }
}
