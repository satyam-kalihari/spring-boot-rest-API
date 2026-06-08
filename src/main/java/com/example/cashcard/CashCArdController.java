package com.example.cashcard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.data.domain.Pageable;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
// import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/cashcards")
class CashCardController {

    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal){
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC,"amount"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal) {

        Optional<CashCard> cashCardOptional = Optional.ofNullable(cashCardRepository
                .findByIdAndOwner(requestedId, principal.getName())
        );
        if (cashCardOptional.isPresent()) {
            return ResponseEntity.ok(cashCardOptional.get());
        }
        else{
            return ResponseEntity.notFound().build(); //.build() => it returns the empty body
        }

    }

    @PostMapping
    private ResponseEntity<Void>  createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal) {

        CashCard cashCardwithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());

        CashCard saveCashcard = cashCardRepository.save(cashCardwithOwner);
        URI locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(saveCashcard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }
}