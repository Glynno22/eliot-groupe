package dhi.projet1.dev.ticket.utils;

/**
 * Codes ANSI pour la mise en couleur des sorties console.
 *
 * <p>Usage : {@code System.out.println(ConsoleColors.GREEN + "OK" + ConsoleColors.RESET)}</p>
 *
 * <p>Note : certains terminaux Windows ne supportent pas les codes ANSI
 * sans activation préalable (Windows Terminal ou activation de VirtualTerminalLevel).</p>
 */
public final class ConsoleColors {

    // Empêcher l'instanciation
    private ConsoleColors() {}

    public static final String RESET  = "\033[0m";
    public static final String GREEN  = "\033[32m";
    public static final String RED    = "\033[31m";
    public static final String YELLOW = "\033[33m";
    public static final String CYAN   = "\033[36m";
    public static final String BOLD   = "\033[1m";
}