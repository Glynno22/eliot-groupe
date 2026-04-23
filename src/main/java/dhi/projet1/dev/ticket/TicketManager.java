package dhi.projet1.dev.ticket;

import dhi.projet1.dev.ticket.enums.EntrepriseService;
import dhi.projet1.dev.ticket.enums.Priority;
import dhi.projet1.dev.ticket.enums.StatutTicket;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service central de gestion des tickets d'incident.
 *
 * <p>Cette classe coordonne toutes les opérations métier :
 * création, recherche, filtrage, assignation, changement de statut,
 * statistiques, import et export CSV.</p>
 *
 * <p>Elle est indépendante de la couche de persistance grâce à
 * l'interface {@link TicketRepository}.</p>
 */
public class TicketManager {

    private final TicketRepository repo;

    /**
     * Crée un manager lié à un dépôt de persistance.
     *
     * @param repo dépôt utilisé pour stocker et lire les tickets
     */
    public TicketManager(TicketRepository repo) {
        this.repo = Objects.requireNonNull(repo, "Le dépôt ne peut pas être null");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Création
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Crée et persiste un nouveau ticket avec le statut OPEN.
     *
     * @param title       titre court
     * @param description détail de l'incident
     * @param requestedBy nom du demandeur
     * @param service     service métier concerné
     * @param priority    niveau de priorité
     * @param occurredAt  date/heure de survenue (peut être null si inconnue)
     * @return le ticket créé
     */
    public Ticket create(String title,
                         String description,
                         String requestedBy,
                         EntrepriseService service,
                         Priority priority,
                         LocalDateTime occurredAt) {

        Ticket t = Ticket.openTicket(title, description, requestedBy,
                service, priority, occurredAt);
        repo.saveTicket(t);
        return t;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Lecture / recherche
    // ══════════════════════════════════════════════════════════════════════════

    /** Retourne tous les tickets. */
    public Set<Ticket> all() {
        return repo.getTickets();
    }

    /**
     * Recherche par identifiant exact ou par titre (insensible à la casse).
     *
     * @param input identifiant complet ou fragment de titre
     * @return liste des tickets correspondants, jamais null
     */
    public List<Ticket> search(String input) {
        if (input == null || input.isBlank()) return List.of();
        String q = input.toLowerCase();
        return repo.getTickets().stream()
                .filter(t -> t.getId().equalsIgnoreCase(input)
                        || t.getTitle().toLowerCase().contains(q)
                        || t.getDescription().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    /**
     * Filtre les tickets par statut.
     *
     * @param s statut recherché
     * @return liste filtrée, triée par date de création décroissante
     */
    public List<Ticket> byStatus(StatutTicket s) {
        return repo.getTickets().stream()
                .filter(t -> t.getStatut() == s)
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Filtre les tickets par priorité.
     *
     * @param p priorité recherchée
     * @return liste filtrée, triée par date de création décroissante
     */
    public List<Ticket> byPriority(Priority p) {
        return repo.getTickets().stream()
                .filter(t -> t.getPriority() == p)
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Filtre les tickets par service.
     *
     * @param s service recherché
     * @return liste filtrée
     */
    public List<Ticket> byService(EntrepriseService s) {
        return repo.getTickets().stream()
                .filter(t -> t.getService() == s)
                .collect(Collectors.toList());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Actions métier
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Assigne un ticket à un technicien.
     *
     * @param id   identifiant complet du ticket
     * @param tech nom du technicien
     * @throws NoSuchElementException si le ticket n'est pas trouvé
     * @throws IllegalStateException  si la transition est invalide
     */
    public void assign(String id, String tech) {
        Ticket t = findOrThrow(id);
        t.assign(tech);
        repo.saveTicket(t);
    }

    /**
     * Change le statut d'un ticket.
     *
     * @param id  identifiant complet du ticket
     * @param s   nouveau statut
     * @throws NoSuchElementException si le ticket n'est pas trouvé
     * @throws IllegalStateException  si la transition est invalide
     */
    public void changeStatus(String id, StatutTicket s) {
        Ticket t = findOrThrow(id);
        t.changeStatut(s);
        repo.saveTicket(t);
    }

    /**
     * Supprime un ticket du dépôt.
     *
     * @param id identifiant complet du ticket
     * @throws NoSuchElementException si le ticket n'est pas trouvé
     */
    public void delete(String id) {
        Ticket t = findOrThrow(id);
        repo.deleteTicket(t);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers privés
    // ══════════════════════════════════════════════════════════════════════════

    private Optional<Ticket> find(String id) {
        return repo.getTickets().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    /**
     * Recherche un ticket ou lève une exception descriptive.
     *
     * @param id identifiant du ticket
     * @return ticket trouvé
     * @throws NoSuchElementException si aucun ticket ne correspond
     */
    private Ticket findOrThrow(String id) {
        return find(id).orElseThrow(() ->
                new NoSuchElementException("Ticket introuvable : " + id));
    }
}