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
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.data.web.PagedResourcesAssembler;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;


@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    private final CashCardRepository cashCardRepository;

    public CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<CashCard>>> findAll(Pageable pageable, Principal principal, PagedResourcesAssembler<CashCard> assembler){
        Page<CashCard> page = cashCardRepository.findByOwner(principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.ASC,"amount"))
                )
        );
        return ResponseEntity.ok(assembler.toModel(page, cashCard -> EntityModel.of(cashCard,
                linkTo(methodOn(CashCardController.class).findById(cashCard.id(), principal)).withSelfRel(),
                linkTo(methodOn(CashCardController.class).findAll(pageable, principal, assembler)).withRel("cashcards"))));
    }

//    Reduce Code Duplication
    private CashCard findCashCard(Long requestedId, Principal principal){
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }

    @GetMapping("/{requestedId}")
    public ResponseEntity<EntityModel<CashCard>> findById(@PathVariable Long requestedId, Principal principal) {

        CashCard cashCard = findCashCard(requestedId, principal);
        if (cashCard != null) {
            EntityModel<CashCard> entityModel = EntityModel.of(cashCard,
                    linkTo(methodOn(CashCardController.class).findById(requestedId, principal)).withSelfRel(),
                    linkTo(methodOn(CashCardController.class).findAll(PageRequest.of(0, 20), principal, null)).withRel("cashcards"));
            return ResponseEntity.ok(entityModel);
        }
        else{
            return ResponseEntity.notFound().build(); //.build() => it returns the empty body
        }

    }

    @PostMapping
    public ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb, Principal principal) {

        CashCard cashCardwithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());

        CashCard saveCashcard = cashCardRepository.save(cashCardwithOwner);
        URI locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(saveCashcard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    public ResponseEntity<Void> putCashCard(@PathVariable Long requestedId, @RequestBody CashCard cashCardUpdate, Principal principal) {
        // just return 204 NO CONTENT for now.
        CashCard cashCard = findCashCard(requestedId, principal);
        if (null != cashCard) {
            CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCashCard(@PathVariable Long id, Principal principal) {

        if (cashCardRepository.existsByIdAndOwner(id, principal.getName())) {
            cashCardRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}