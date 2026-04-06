package com.zipcode.stardust.repository;

import com.zipcode.stardust.model.Comment;
import com.zipcode.stardust.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByPostdateAsc(Post post);
}
