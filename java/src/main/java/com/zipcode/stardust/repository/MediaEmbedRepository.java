package com.zipcode.stardust.repository;

import com.zipcode.stardust.model.MediaEmbed;
import com.zipcode.stardust.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaEmbedRepository extends JpaRepository<MediaEmbed, Long> {
    List<MediaEmbed> findByPostOrderByIdAsc(Post post);
}
