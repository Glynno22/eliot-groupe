package dhi.projet1.dev.ticket.enums;

/**
 * Cycle de vie d'un ticket.
 *
 * <p>Transitions autorisées (unidirectionnelles) :</p>
 * <pre>OPEN → PENDING → RESOLVED → CLOSED</pre>
 */
public enum StatutTicket {
    /** Ticket ouvert, en attente d'assignation. */
    OPEN,
    /** Ticket assigné à un technicien, en cours de traitement. */
    PENDING,
    /** Incident résolu, en attente de confirmation de fermeture. */
    RESOLVED,
    /** Ticket définitivement fermé. Aucune modification possible. */
    CLOSED
}