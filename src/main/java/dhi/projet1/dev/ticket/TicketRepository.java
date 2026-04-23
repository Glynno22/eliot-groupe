package dhi.projet1.dev.ticket;

import java.util.Collection;
import java.util.Set;

/**
 * Contrat de persistance des tickets.
 *
 * <p>Cette interface permet de découpler la logique métier ({@link TicketManager})
 * de la couche de stockage. Trois implémentations sont fournies :</p>
 * <ul>
 *   <li>{@link InMemoryTicketRepository} – stockage volatile en mémoire (tests)</li>
 *   <li>{@link CSVTicketRepository} – persistance dans un fichier CSV</li>
 *   <li>{@link ExcelTicketRepository} – persistance dans un fichier Excel (.xlsx)</li>
 * </ul>
 */
public interface TicketRepository {

    /** Retourne tous les tickets stockés. */
    Set<Ticket> getTickets();

    /**
     * Sauvegarde (ou met à jour) un ticket.
     * Si un ticket avec le même identifiant existe, il est remplacé.
     */
    void saveTicket(Ticket ticket);

    /** Supprime un ticket. Sans effet si le ticket n'existe pas. */
    void deleteTicket(Ticket ticket);

    /** Sauvegarde une collection de tickets en bloc. */
    void saveTickets(Collection<Ticket> tickets);
}