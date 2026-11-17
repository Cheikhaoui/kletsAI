package com.kletsai.flashcard;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService service;

    public CardController(CardService service) {
        this.service = service;
    }

    @GetMapping
    public List<CardResponse> list(@RequestParam(required = false) Card.Source source) {
        return service.list(source).stream().map(CardResponse::from).toList();
    }

    @GetMapping("/review-deck")
    public List<CardResponse> reviewDeck(@RequestParam(defaultValue = "2") int days) {
        return service.reviewDeck(days).stream().map(CardResponse::from).toList();
    }

    @GetMapping("/stats")
    public CardService.CardStats stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public CardResponse get(@PathVariable UUID id) {
        return CardResponse.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@Valid @RequestBody CardContent content) {
        return CardResponse.from(service.create(content, Card.Source.MANUAL));
    }

    @PutMapping("/{id}")
    public CardResponse update(@PathVariable UUID id, @Valid @RequestBody CardContent content) {
        return CardResponse.from(service.update(id, content));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
