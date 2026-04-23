package dhi.projet1.dev;

import dhi.projet1.dev.ticket.CSVTicketRepository;
import dhi.projet1.dev.ticket.TicketManager;
import dhi.projet1.dev.ticket.enums.EntrepriseService;
import dhi.projet1.dev.ticket.enums.Priority;
import dhi.projet1.dev.ticket.enums.StatutTicket;
import dhi.projet1.dev.ticket.utils.ConsoleColors;
import dhi.projet1.dev.ticket.Ticket;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Point d'entrée de l'application de gestion des tickets d'incident.
 *
 * <p>Lance un menu interactif en boucle dans le terminal.
 * Les tickets sont persistés dans un fichier CSV local. ok</p>
 *
 * <p>Usage : {@code java -jar ticket-manager.jar}</p>
 */
public class Main {

    private static final String CSV_FILE = "tickets.csv";
    private static TicketManager manager;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        manager = new TicketManager(new CSVTicketRepository(CSV_FILE));
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = prompt("Votre choix");
            running = handleChoice(choice);
        }

        System.out.println(ConsoleColors.CYAN + "\nAu revoir !\n" + ConsoleColors.RESET);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Menu et navigation
    // ══════════════════════════════════════════════════════════════════════════

    private static void printBanner() {
        System.out.println(ConsoleColors.CYAN + ConsoleColors.BOLD);
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║     ELIOT GROUP — Gestion des Incidents      ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println(ConsoleColors.RESET);
    }

    private static void printMenu() {
        System.out.println("\n" + ConsoleColors.BOLD + "── MENU ──────────────────────────────────" + ConsoleColors.RESET);
        System.out.println("  1. Créer un ticket");
        System.out.println("  2. Lister tous les tickets");
        System.out.println("  3. Rechercher un ticket");
        System.out.println("  4. Filtrer par statut");
        System.out.println("  5. Filtrer par priorité");
        System.out.println("  6. Assigner un ticket à un technicien");
        System.out.println("  7. Changer le statut d'un ticket");
        System.out.println("  8. Afficher la fiche détaillée d'un ticket");
        System.out.println("  9. Supprimer un ticket");
        System.out.println(" 10. Statistiques");
        System.out.println(" 11. Importer depuis un CSV");
        System.out.println(" 12. Exporter vers un CSV");
        System.out.println("  0. Quitter");
        System.out.println(ConsoleColors.BOLD + "──────────────────────────────────────────" + ConsoleColors.RESET);
    }

    /** Dispatche le choix de l'utilisateur ; retourne false pour quitter. */
    private static boolean handleChoice(String choice) {
        System.out.println();
        try {
            switch (choice.trim()) {
                case "1"  -> createTicket();
                case "2"  -> listAll();
                case "3"  -> searchTicket();
                case "4"  -> filterByStatus();
                case "5"  -> filterByPriority();
                case "6"  -> assignTicket();
                case "7"  -> changeStatus();
                case "8"  -> showDetail();
                case "9"  -> deleteTicket();
                case "10" -> showStats();
                case "11" -> importCSV();
                case "12" -> exportCSV();
                case "0"  -> { return false; }
                default   -> warn("Option invalide.");
            }
        } catch (Exception e) {
            error("Erreur : " + e.getMessage());
        }
        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Actions
    // ══════════════════════════════════════════════════════════════════════════

    private static void createTicket() {
        System.out.println(ConsoleColors.BOLD + "[ Création d'un ticket ]" + ConsoleColors.RESET);

        String title       = promptRequired("Titre");
        String description = promptRequired("Description");
        String requestedBy = promptRequired("Demandeur");
        EntrepriseService service  = pickEnum("Service", EntrepriseService.values());
        Priority          priority = pickEnum("Priorité", Priority.values());
        LocalDateTime occurredAt   = promptDateTime("Date de survenue (yyyy-MM-ddTHH:mm, vide = maintenant)");

        Ticket t = manager.create(title, description, requestedBy, service, priority,
                occurredAt == null ? LocalDateTime.now() : occurredAt);
        success("Ticket créé : " + t.getId());
        System.out.println(t);
    }

    private static void listAll() {
        Set<Ticket> all = manager.all();
        if (all.isEmpty()) {
            warn("Aucun ticket enregistré.");
            return;
        }
        printLine();
        all.stream()
                .sorted(Comparator.comparing(Ticket::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(System.out::println);
        printLine();
        System.out.printf("  Total : %d ticket(s)%n", all.size());
    }

    private static void searchTicket() {
        String query = promptRequired("Rechercher (id ou titre)");
        List<Ticket> results = manager.search(query);
        if (results.isEmpty()) {
            warn("Aucun résultat pour : " + query);
        } else {
            results.forEach(System.out::println);
        }
    }

    private static void filterByStatus() {
        StatutTicket s = pickEnum("Statut", StatutTicket.values());
        List<Ticket> results = manager.byStatus(s);
        printResults(results, "statut " + s);
    }

    private static void filterByPriority() {
        Priority p = pickEnum("Priorité", Priority.values());
        List<Ticket> results = manager.byPriority(p);
        printResults(results, "priorité " + p);
    }

    private static void assignTicket() {
        String id   = promptRequired("ID du ticket (complet)");
        String tech = promptRequired("Nom du technicien");
        manager.assign(id, tech);
        success("Ticket assigné à " + tech);
    }

    private static void changeStatus() {
        String id          = promptRequired("ID du ticket (complet)");
        StatutTicket statut = pickEnum("Nouveau statut", StatutTicket.values());
        manager.changeStatus(id, statut);
        success("Statut mis à jour → " + statut);
    }

    private static void showDetail() {
        String id = promptRequired("ID du ticket (complet)");
        manager.search(id).stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .ifPresentOrElse(
                        t -> System.out.println(t.toDetail()),
                        () -> warn("Ticket introuvable : " + id)
                );
    }

    private static void deleteTicket() {
        String id = promptRequired("ID du ticket à supprimer (complet)");
        String confirm = prompt("Confirmer la suppression ? (oui/non)");
        if ("oui".equalsIgnoreCase(confirm)) {
            manager.delete(id);
            success("Ticket supprimé.");
        } else {
            warn("Suppression annulée.");
        }
    }

    private static void showStats() {
        System.out.println(ConsoleColors.BOLD + "[ Statistiques ]" + ConsoleColors.RESET);
        System.out.println("\n  Par statut :");
        manager.statsByStatus().forEach((k, v) -> System.out.printf("    %-12s : %d%n", k, v));
        System.out.println("\n  Par priorité :");
        manager.statsByPriority().forEach((k, v) -> System.out.printf("    %-12s : %d%n", k, v));
        System.out.println("\n  Par service :");
        manager.statsByService().forEach((k, v) -> System.out.printf("    %-15s : %d%n", k, v));
        System.out.printf("%n  Total général : %d ticket(s)%n", manager.all().size());
    }

    private static void importCSV() {
        String path = promptRequired("Chemin du fichier CSV à importer");
        manager.importCSV(path);
        success("Import terminé.");
    }

    private static void exportCSV() {
        String path = promptRequired("Chemin du fichier CSV de destination");
        manager.exportCSV(path);
        success("Export terminé → " + path);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Helpers console
    // ══════════════════════════════════════════════════════════════════════════

    private static String prompt(String label) {
        System.out.print("  " + label + " : ");
        return sc.nextLine().trim();
    }

    private static String promptRequired(String label) {
        String value;
        do {
            value = prompt(label);
            if (value.isBlank()) warn("Ce champ est obligatoire.");
        } while (value.isBlank());
        return value;
    }

    private static LocalDateTime promptDateTime(String label) {
        String s = prompt(label);
        if (s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            warn("Format invalide, date ignorée.");
            return null;
        }
    }

    /** Affiche les valeurs d'un enum numérotées et demande un choix. */
    private static <E extends Enum<E>> E pickEnum(String label, E[] values) {
        System.out.println("  " + label + " :");
        for (int i = 0; i < values.length; i++) {
            System.out.printf("    %d. %s%n", i + 1, values[i]);
        }
        while (true) {
            String input = prompt("Choix (1-" + values.length + ")");
            try {
                int idx = Integer.parseInt(input) - 1;
                if (idx >= 0 && idx < values.length) return values[idx];
            } catch (NumberFormatException ignored) {}
            warn("Entrée invalide.");
        }
    }

    private static void printResults(List<Ticket> results, String filter) {
        if (results.isEmpty()) {
            warn("Aucun ticket pour " + filter);
        } else {
            printLine();
            results.forEach(System.out::println);
            printLine();
            System.out.printf("  %d ticket(s) trouvé(s) pour %s%n", results.size(), filter);
        }
    }

    private static void success(String msg) {
        System.out.println(ConsoleColors.GREEN + "  ✓ " + msg + ConsoleColors.RESET);
    }

    private static void warn(String msg) {
        System.out.println(ConsoleColors.YELLOW + "  ⚠ " + msg + ConsoleColors.RESET);
    }

    private static void error(String msg) {
        System.out.println(ConsoleColors.RED + "  ✗ " + msg + ConsoleColors.RESET);
    }

    private static void printLine() {
        System.out.println("  " + "─".repeat(80));
    }
}