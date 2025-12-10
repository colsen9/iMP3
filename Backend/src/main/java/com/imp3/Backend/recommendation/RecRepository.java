package com.imp3.Backend.recommendation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecRepository extends JpaRepository<Recommendation, Integer> {
    List<Recommendation> findAllByRecipientUid(Integer uid);

    Optional<Recommendation> findByRecIdAndRecipientUid(Integer recId, Integer uid);

    List<Recommendation> findAllByRecipientUidAndPrivacy(Integer recipientId, Recommendation.Privacy privacy);

    List<Recommendation> findAllBySenderUidAndRecipientUid(Integer senderUid, Integer recipientUid);
}
