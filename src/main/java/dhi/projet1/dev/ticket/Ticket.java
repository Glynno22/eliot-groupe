package dhi.projet1.dev.ticket;

import dhi.projet1.dev.ticket.enums.EntrepriseService;
import dhi.projet1.dev.ticket.enums.Priority;
import dhi.projet1.dev.ticket.enums.StatutTicket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Représente un ticket d'incident interne.
 *
 * <p>Un ticket suit un cycle de vie strict :
 * OPEN → PENDING → RESOLVED → CLOSED.
 * Il ne peut jamais revenir en arrière ni changer d'état
 * une fois CLOSED.</p>
 *
 * <p>Création via la méthode de fabrique {@link #openTicket}
 * ou via {@link #fromCSV} pour la désérialisation.</p>
 */
public class Ticket {

    // ── Formateur partagé pour la sérialisation ────────────────────────────────
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ── Champs ─────────────────────────────────────────────────────────────────
    private String          id;
    private String          title;
    private String          description;
    private String          requestedBy;      // demandeur
    private EntrepriseService service;
    private Priority        priority;
    private StatutTicket    statut;

    private String          assignedTo;
    private LocalDateTime   assignedAt;

    private LocalDateTime   occurredAt;
    private LocalDateTime   createdAt;
    private LocalDateTime   updatedAt;
    private LocalDateTime   resolvedAt;
    private LocalDateTime   closedAt;

    // Constructeur privé : forcer l'usage des fabriques
    public Ticket() {}

    /**
     * Ouvre un nouveau ticket avec le statut OPEN.
     *
     * @param title       titre court et descriptif
     * @param description détail de l'incident
     * @param requestedBy identifiant ou nom du demandeur
     * @param service     service métier concerné
     * @param priority    niveau de priorité
     * @param occurredAt  date/heure de survenue de l'incident
     * @return nouveau ticket persistable
     */
    public static Ticket openTicket(String title,
                                    String description,
                                    String requestedBy,
                                    EntrepriseService service,
                                    Priority priority,
                                    LocalDateTime occurredAt) {
        Ticket t = new Ticket();
        t.id          = UUID.randomUUID().toString();
        t.title       = title;
        t.description = description;
        t.requestedBy = requestedBy;
        t.service     = service;
        t.priority    = priority;
        t.statut      = StatutTicket.OPEN;
        t.occurredAt  = occurredAt;
        t.createdAt   = LocalDateTime.now();
        return t;
    }

    /**
     * Reconstruit un ticket à partir d'un tableau de champs CSV.
     * L'ordre doit correspondre à celui produit par {@link #toCSV()}.
     *
     * @param d tableau de 14 champs
     * @return ticket reconstruit
     * @throws IllegalArgumentException si un champ enum est invalide
     * @throws ArrayIndexOutOfBoundsException si le tableau est trop court
     */
    public static Ticket fromCSV(String[] d) {
        if (d.length < 14) {
            throw new IllegalArgumentException(
                    "Ligne CSV invalide : attendu 14 champs, reçu " + d.length);
        }
        Ticket t = new Ticket();
        t.id          = d[0];
        t.title       = d[1];
        t.description = d[2];
        t.requestedBy = emptyToNull(d[3]);
        t.service     = EntrepriseService.valueOf(d[4]);
        t.priority    = Priority.valueOf(d[5]);
        t.statut      = StatutTicket.valueOf(d[6]);
        t.assignedTo  = emptyToNull(d[7]);
        t.assignedAt  = parseDate(d[8]);
        t.occurredAt  = parseDate(d[9]);
        t.createdAt   = parseDate(d[10]);
        t.updatedAt   = parseDate(d[11]);
        t.resolvedAt  = parseDate(d[12]);
        t.closedAt    = parseDate(d[13]);
        return t;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Sérialisation CSV
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Sérialise le ticket en une ligne CSV de 14 champs.
     * Les virgules présentes dans les valeurs texte sont remplacées par un espace.
     *
     * @return ligne CSV sans saut de ligne
     */
    public String toCSV() {
        return String.join(",",
                safe(id),
                safe(title),
                safe(description),
                safe(requestedBy),
                service.name(),
                priority.name(),
                statut.name(),
                safe(assignedTo),
                format(assignedAt),
                format(occurredAt),
                format(createdAt),
                format(updatedAt),
                format(resolvedAt),
                format(closedAt)
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Comportements métier
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Assigne le ticket à un technicien et le passe en statut PENDING.
     * Si le ticket est déjà assigné, l'opération est refusée avec une exception.
     *
     * @param tech nom / identifiant du technicien
     * @throws IllegalStateException si le ticket est CLOSED ou déjà assigné
     */
    public void assign(String tech) {
        checkNotClosed();
        // BUG CORRIGÉ : l'ancienne version continuait après le if et
        // affichait "Ticket déjà assigné" même lors d'une première assignation.
        if (this.statut != StatutTicket.OPEN) {
            throw new IllegalStateException(
                    "Impossible d'assigner : le ticket n'est pas en statut OPEN (statut actuel : "
                            + statut + ")");
        }
        assignedTo = tech;
        assignedAt = LocalDateTime.now();
        statut     = StatutTicket.PENDING;
        updatedAt  = LocalDateTime.now();
    }

    /**
     * Fait progresser le statut du ticket vers {@code next}.
     * Les transitions autorisées sont :
     * <ul>
     *   <li>OPEN → PENDING</li>
     *   <li>PENDING → RESOLVED</li>
     *   <li>RESOLVED → CLOSED</li>
     * </ul>
     *
     * @param next statut cible
     * @throws IllegalStateException si la transition est invalide ou si le ticket est CLOSED
     */
    public void changeStatut(StatutTicket next) {
        checkNotClosed();

        boolean valid = switch (statut) {
            case OPEN     -> next == StatutTicket.PENDING;
            case PENDING  -> next == StatutTicket.RESOLVED;
            case RESOLVED -> next == StatutTicket.CLOSED;
            case CLOSED   -> false; // déjà géré par checkNotClosed()
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Transition invalide : " + statut + " → " + next);
        }

        statut    = next;
        updatedAt = LocalDateTime.now();

        if (next == StatutTicket.RESOLVED) resolvedAt = LocalDateTime.now();
        if (next == StatutTicket.CLOSED)   closedAt   = LocalDateTime.now();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers privés
    // ══════════════════════════════════════════════════════════════════════════

    private void checkNotClosed() {
        if (statut == StatutTicket.CLOSED)
            throw new IllegalStateException("Le ticket est fermé et ne peut plus être modifié.");
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace(",", " ");
    }

    private static String format(LocalDateTime d) {
        return d == null ? "" : d.format(FMT);
    }

    private static LocalDateTime parseDate(String s) {
        return (s == null || s.isBlank()) ? null : LocalDateTime.parse(s, FMT);
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Accesseurs
    // ══════════════════════════════════════════════════════════════════════════

    public String             getId()          { return id; }
    public String             getTitle()       { return title; }
    public String             getDescription() { return description; }
    public String             getRequestedBy() { return requestedBy; }
    public EntrepriseService  getService()     { return service; }
    public Priority           getPriority()    { return priority; }
    public StatutTicket       getStatut()      { return statut; }
    public String             getAssignedTo()  { return assignedTo; }
    public LocalDateTime      getAssignedAt()  { return assignedAt; }
    public LocalDateTime      getOccurredAt()  { return occurredAt; }
    public LocalDateTime      getCreatedAt()   { return createdAt; }
    public LocalDateTime      getUpdatedAt()   { return updatedAt; }
    public LocalDateTime      getResolvedAt()  { return resolvedAt; }
    public LocalDateTime      getClosedAt()    { return closedAt; }

    // ══════════════════════════════════════════════════════════════════════════
    //  Affichage
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Représentation lisible sur une ligne pour les listes console.
     * BUG CORRIGÉ : l'ancienne version déclarait 7 arguments dans le format
     * mais en passait 13, ce qui générait un résultat tronqué silencieusement.
     */
    @Override
    public String toString() {
        return String.format(
                "[%s] %-30s | %-12s | %-8s | %-8s | Demandeur: %-10s | Assigné: %s",
                id.substring(0, 8),
                title,
                service,
                priority,
                statut,
                requestedBy  == null ? "N/A" : requestedBy,
                assignedTo   == null ? "Non assigné" : assignedTo
        );
    }

    /** Affichage détaillé (fiche complète du ticket). */
    public String toDetail() {
        return String.format("""
                ┌─ TICKET %s ──────────────────────────────────────────┐
                │ Titre       : %s
                │ Description : %s
                │ Demandeur   : %s
                │ Service     : %s
                │ Priorité    : %s
                │ Statut      : %s
                │ Assigné à   : %s  (le %s)
                │ Survenu le  : %s
                │ Créé le     : %s
                │ Mis à jour  : %s
                │ Résolu le   : %s
                │ Fermé le    : %s
                └──────────────────────────────────────────────────────────┘""",
                id.substring(0, 8),
                title,
                description,
                orNA(requestedBy),
                service,
                priority,
                statut,
                orNA(assignedTo), format(assignedAt),
                format(occurredAt),
                format(createdAt),
                format(updatedAt),
                format(resolvedAt),
                format(closedAt)
        );
    }

    private static String orNA(String s) {
        return s == null ? "N/A" : s;
    }
}