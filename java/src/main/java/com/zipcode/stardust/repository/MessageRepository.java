package com.zipcode.stardust.repository;

import com.zipcode.stardust.model.Message;
import com.zipcode.stardust.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByRecipientAndDeletedByRecipientFalseOrderBySentAtDesc(User recipient);

    List<Message> findBySenderAndDeletedBySenderFalseOrderBySentAtDesc(User sender);

    long countByRecipientAndReadFalseAndDeletedByRecipientFalse(User recipient);
}
