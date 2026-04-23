package dhi.projet1.dev.ticket;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Dépôt de tickets persisté dans un fichier CSV.
 *
 * <p>Format : une ligne d'en-tête (ignorée à la lecture), puis une ligne
 * par ticket, 14 champs séparés par des virgules.</p>
 *
 * <p>Le fichier est rechargé au démarrage et réécrit entièrement à chaque
 * modification (persist-on-write). Ce comportement est adapté à des volumes
 * modestes (quelques milliers de tickets).</p>
 */
public class CSVTicketRepository implements TicketRepository {

    /** En-tête CSV pour la lisibilité humaine du fichier. */
    private static final String HEADER =
            "id,title,description,requestedBy,service,priority,statut," +
                    "assignedTo,assignedAt,occurredAt,createdAt,updatedAt,resolvedAt,closedAt";

    private final String path;
    /** Map id → ticket pour garantir l'unicité et permettre la mise à jour. */
    private final Map<String, Ticket> ticketMap = new LinkedHashMap<>();

    /**
     * Crée le dépôt en chargeant le fichier s'il existe.
     *
     * @param path chemin absolu ou relatif du fichier CSV
     */
    public CSVTicketRepository(String path) {
        this.path = path;
        load();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Chargement / persistance
    // ══════════════════════════════════════════════════════════════════════════

    private void load() {
        File file = new File(path);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            boolean firstLine = true;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                // Ignorer l'en-tête
                if (firstLine) { firstLine = false; continue; }
                if (line.isBlank()) continue;

                try {
                    String[] fields = line.split(",", -1);
                    Ticket t = Ticket.fromCSV(fields);
                    ticketMap.put(t.getId(), t);
                } catch (Exception e) {
                    System.err.println("[CSV] Ligne " + lineNumber +
                            " ignorée (format invalide) : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[CSV] Impossible de lire le fichier : " + e.getMessage());
        }
    }

    /** Réécrit le fichier CSV en totalité. */
    private void persist() {
        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            pw.println(HEADER);
            for (Ticket t : ticketMap.values()) {
                pw.println(t.toCSV());
            }
        } catch (IOException e) {
            System.err.println("[CSV] Impossible d'écrire dans le fichier : " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Implémentation de TicketRepository
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public Set<Ticket> getTickets() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(ticketMap.values()));
    }

    @Override
    public void saveTicket(Ticket ticket) {
        ticketMap.put(ticket.getId(), ticket);
        persist();
    }

    @Override
    public void deleteTicket(Ticket ticket) {
        ticketMap.remove(ticket.getId());
        persist();
    }

    @Override
    public void saveTickets(Collection<Ticket> tickets) {
        for (Ticket t : tickets) {
            ticketMap.put(t.getId(), t);
        }
        persist();
    }
}