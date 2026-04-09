package com.zipcode.stardust.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.zipcode.stardust.model.Comment;
import com.zipcode.stardust.model.Post;
import com.zipcode.stardust.model.Reaction;
import com.zipcode.stardust.model.User;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {

    // check if a user already reacted to a post
    Reaction findByUserAndPost(User user, Post post);

    // check if a user already reacted to a comment
    Reaction findByUserAndComment(User user, Comment comment);

    // count likes or dislikes on a post
    Long countByPostAndType(Post post, String type);

    // count likes or dislikes on a comment
    Long countByCommentAndType(Comment comment, String type);
}
