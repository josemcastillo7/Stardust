package com.zipcode.stardust.repository;

import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.model.Subforum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findBySubforumOrderByPostdateDesc(Subforum subforum);
}
