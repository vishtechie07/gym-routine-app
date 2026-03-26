package com.gymtracker.repository;

import com.gymtracker.entity.LabYoutubeVideo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabYoutubeVideoRepository extends JpaRepository<LabYoutubeVideo, String> {

    long countByEmbeddingJsonIsNull();

    List<LabYoutubeVideo> findByEmbeddingJsonIsNull();

    List<LabYoutubeVideo> findAllByOrderByPublishedAtDesc(Pageable pageable);

    List<LabYoutubeVideo> findByChannelIdOrderByPublishedAtDesc(String channelId, Pageable pageable);
}
