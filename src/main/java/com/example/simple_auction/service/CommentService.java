package com.example.simple_auction.service;

import com.example.simple_auction.model.Comment;
import com.example.simple_auction.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    public List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    public void deleteComment(Integer id) {
        commentRepository.deleteById(id);
    }
}