package dhi.projet1.dev.ticket;

import dhi.projet1.dev.ticket.enums.EntrepriseService;
import dhi.projet1.dev.ticket.enums.Priority;
import dhi.projet1.dev.ticket.enums.StatutTicket;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TicketManagerTest {

    private TicketManager manager;

    @Before
    public void setUp() {
        manager = new TicketManager(new InMemoryTicketRepository());
    }

    /**
     *  TEST 1 : Création d’un ticket
     */
    @Test
    public void shouldCreateTicketSuccessfully() {

        Ticket t = manager.create(
                "PC en panne",
                "Ne s'allume plus",
                "Alice",
                EntrepriseService.COMPTABILITE,
                Priority.HIGH,
                LocalDateTime.now()
        );

        // Vérifications
        assertNotNull(t);
        assertNotNull(t.getId());
        assertEquals(StatutTicket.OPEN, t.getStatut());
        assertEquals("PC en panne", t.getTitle());

        // Vérifie persistance
        assertEquals(1, manager.all().size());
    }

    /**
     *  TEST 2 : Recherche insensible à la casse
     */
    @Test
    public void shouldSearchTicketByTitleIgnoreCase() {

        manager.create(
                "Imprimante HS",
                "Ne fonctionne plus",
                "Bob",
                EntrepriseService.LOGISTIQUE,
                Priority.MEDIUM,
                LocalDateTime.now()
        );

        List<Ticket> result = manager.search("imprimante");

        assertFalse(result.isEmpty());
        assertEquals("Imprimante HS", result.get(0).getTitle());
    }

    /**
     *  TEST 3 : Filtrage par statut + tri
     */
    @Test
    public void shouldFilterByStatus() {

        manager.create(
                "Bug 1",
                "desc",
                "User1",
                EntrepriseService.IT,
                Priority.LOW,
                LocalDateTime.now().minusDays(1)
        );

        Ticket t2 = manager.create(
                "Bug 2",
                "desc",
                "User2",
                EntrepriseService.IT,
                Priority.HIGH,
                LocalDateTime.now()
        );

        manager.assign(t2.getId(), "Tech1"); // passe à PENDING

        List<Ticket> pending = manager.byStatus(StatutTicket.PENDING);

        assertEquals(1, pending.size());
        assertEquals(StatutTicket.PENDING, pending.get(0).getStatut());
    }

    /**
     *  TEST 4 : Assignation + changement de statut
     */
    @Test
    public void shouldAssignAndChangeStatus() {

        Ticket t = manager.create(
                "Crash app",
                "Erreur fatale",
                "Charlie",
                EntrepriseService.IT,
                Priority.CRITICAL,
                LocalDateTime.now()
        );

        manager.assign(t.getId(), "Tech1");

        assertEquals(StatutTicket.PENDING, t.getStatut());
        assertEquals("Tech1", t.getAssignedTo());

        manager.changeStatus(t.getId(), StatutTicket.RESOLVED);

        assertEquals(StatutTicket.RESOLVED, t.getStatut());
    }

    /**
     *  TEST 5 : Transition invalide → exception
     */
    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionForInvalidTransition() {

        Ticket t = manager.create(
                "Erreur réseau",
                "Pas de connexion",
                "David",
                EntrepriseService.IT,
                Priority.MEDIUM,
                LocalDateTime.now()
        );

        // Impossible de passer directement de OPEN → CLOSED
        manager.changeStatus(t.getId(), StatutTicket.CLOSED);
    }

    /**
     *  TEST 6 : Statistiques
     */
    @Test
    public void shouldComputeStatsByStatus() {

        manager.create("T1", "desc", "A", EntrepriseService.IT, Priority.LOW, LocalDateTime.now());
        manager.create("T2", "desc", "B", EntrepriseService.IT, Priority.HIGH, LocalDateTime.now());

        Map<String, Long> stats = manager.statsByStatus();

        assertEquals(2L, (long) stats.get("OPEN"));
    }
}