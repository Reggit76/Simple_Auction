package com.example.simple_auction.repository;

import com.example.simple_auction.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByLotId(Integer lotId);
}