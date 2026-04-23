package dhi.projet1.dev.ticket;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Implémentation volatile des tickets, stockés uniquement en mémoire JVM.
 *
 * <p>Utile pour les tests unitaires et les démonstrations rapides.
 * Toutes les données sont perdues à l'arrêt de l'application.</p>
 */
public class InMemoryTicketRepository implements TicketRepository {

    private final Set<Ticket> tickets = new HashSet<>();

    @Override
    public Set<Ticket> getTickets() {
        return Collections.unmodifiableSet(tickets);
    }

    @Override
    public void saveTicket(Ticket ticket) {
        // HashSet.add ne remplace pas un élément existant à valeur égale ;
        // on supprime d'abord pour garantir la mise à jour.
        tickets.remove(ticket);
        tickets.add(ticket);
    }

    @Override
    public void deleteTicket(Ticket ticket) {
        tickets.remove(ticket);
    }

    @Override
    public void saveTickets(Collection<Ticket> tickets) {
        tickets.forEach(this::saveTicket);
    }
}