package com.kletsai.review;

import com.kletsai.flashcard.CardResponse;
import com.kletsai.review.ReviewService.ReviewItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {

    public record SyncResponse(List<UUID> applied, List<UUID> alreadySynced, List<UUID> unknownCards,
                               List<CardResponse> cards) {
    }

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    @PostMapping("/sync")
    public SyncResponse sync(@RequestBody @Size(max = 1000) List<@Valid ReviewItem> items) {
        ReviewService.SyncResult r = service.sync(items);
        return new SyncResponse(r.applied(), r.alreadySynced(), r.unknownCards(),
                r.cards().stream().map(CardResponse::from).toList());
    }
}
