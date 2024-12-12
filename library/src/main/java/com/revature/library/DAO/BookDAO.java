package com.revature.library.DAO;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.revature.library.Models.Book;

@Repository
public interface BookDAO extends JpaRepository<Book,Integer> {
    
}

