import java.util.*;

/**
 * Main class for testing Texas Hold'em scenarios.
 */
public class PokerHandChecker {
    public static void main(String[] args) {
        // Scenario 1: Best possible hand (Royal Flush) vs simple pair
        List<List<Card>> scenario1PlayerCards = Arrays.asList(
            Arrays.asList(new Card(Rank.ACE, Suit.HEARTS), new Card(Rank.KING, Suit.HEARTS)),
            Arrays.asList(new Card(Rank.TWO, Suit.DIAMONDS), new Card(Rank.SEVEN, Suit.CLUBS))
        );
        List<Card> scenario1TableCards = Arrays.asList(
            new Card(Rank.QUEEN, Suit.HEARTS),
            new Card(Rank.JACK, Suit.HEARTS),
            new Card(Rank.TEN, Suit.HEARTS),
            new Card(Rank.FIVE, Suit.HEARTS),
            new Card(Rank.TWO, Suit.CLUBS)
        );
        runScenario("Scenario 1: Top hand vs simple pair", scenario1PlayerCards, scenario1TableCards);

        // Scenario 2: Pair of Jacks vs Smaller pair (Tens)
        List<List<Card>> scenario2PlayerCards = Arrays.asList(
            Arrays.asList(new Card(Rank.JACK, Suit.HEARTS), new Card(Rank.JACK, Suit.DIAMONDS)),
            Arrays.asList(new Card(Rank.TEN, Suit.DIAMONDS), new Card(Rank.SEVEN, Suit.CLUBS))
        );
        List<Card> scenario2TableCards = Arrays.asList(
            new Card(Rank.ACE, Suit.HEARTS),
            new Card(Rank.NINE, Suit.CLUBS),
            new Card(Rank.FOUR, Suit.HEARTS),
            new Card(Rank.TEN, Suit.HEARTS),
            new Card(Rank.QUEEN, Suit.CLUBS)
        );
        runScenario("Scenario 2: Big pair vs smaller pair", scenario2PlayerCards, scenario2TableCards);
    }

    /**
     * Runs a test scenario.
     * @param title  The title of the scenario to print.
     * @param playerHoleCards  Each player's two private cards.
     * @param tableCards  The five shared cards on the table.
     */
    private static void runScenario(
        String title,
        List<List<Card>> playerHoleCards,
        List<Card> tableCards
    ) {
        System.out.println("\n*** " + title + " ***");

        // Create a new game for the number of players
        PokerGame game = new PokerGame(playerHoleCards.size());

        // Deal each player their two private cards
        for (int i = 0; i < playerHoleCards.size(); i++) {
            Player player = game.getPlayers().get(i);
            player.getOwnCards().addAll(playerHoleCards.get(i));
        }

        // Put the five shared cards on the table
        game.setTableCards(new ArrayList<>(tableCards));

        // Show each player what they have and what's on the table
        for (Player player : game.getPlayers()) {
            game.showPlayerView(player);
        }

        // Check each player's best possible hand
        Map<Player, HandResult> results = game.checkAllHands();

        // Determine the winner or winners
        List<Player> winners = game.pickWinners(results);

        // Announce the results in everyday terms
        game.announceResults(results, winners);
    }

    /**
     * Suits of playing cards, with a short symbol for display.
     */
    enum Suit {
        HEARTS("H"), DIAMONDS("D"), CLUBS("C"), SPADES("S");
        private final String symbol;
        Suit(String symbol) { this.symbol = symbol; }
        public String symbol() { return symbol; }
    }

    /**
     * Ranks of playing cards, each with a numeric value.
     */
    enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7),
        EIGHT(8), NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);
        private final int value;
        Rank(int value) { this.value = value; }
        public int value() { return value; }
    }

    /**
     * Represents a single playing card with a rank and suit.
     */
    static class Card {
        private final Rank rank;
        private final Suit suit;

        /**
         * Create a card given its rank and suit.
         */
        Card(Rank rank, Suit suit) {
            this.rank = rank;
            this.suit = suit;
        }

        public Rank rank() { return rank; }
        public Suit suit() { return suit; }

        @Override
        public String toString() {
            // Example: "AH" for Ace of Hearts
            return rank.name().charAt(0) + suit.symbol();
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Card)) return false;
            Card o = (Card) other;
            return rank == o.rank && suit == o.suit;
        }

        @Override
        public int hashCode() {
            return Objects.hash(rank, suit);
        }
    }

    /**
     * Represents one player, holding their two private cards.
     */
    static class Player {
        private final int id;
        private final List<Card> ownCards = new ArrayList<>();

        /**
         * Create a player with a given number (1, 2, ...).
         */
        Player(int id) {
            this.id = id;
        }

        public int id() { return id; }
        public List<Card> getOwnCards() { return ownCards; }

        @Override
        public String toString() {
            return "Player " + id;
        }
    }

    /**
     * The ranking categories for poker hands, from lowest to highest.
     */
    enum HandRank {
        HIGH_CARD, PAIR, TWO_PAIR, THREE_OF_A_KIND,
        STRAIGHT, FLUSH, FULL_HOUSE, FOUR_OF_A_KIND,
        STRAIGHT_FLUSH, ROYAL_FLUSH
    }

    /**
     * Result of evaluating a 5-card hand: its rank, cards, and a score.
     */
    static class HandResult {
        private final HandRank rank;
        private final List<Card> bestFive;
        private final long score;

        /**
         * @param rank  The category (e.g., FLUSH or PAIR)
         * @param bestFive  The five cards that make this hand
         * @param score  Numeric score for tie-breaking
         */
        HandResult(HandRank rank, List<Card> bestFive, long score) {
            this.rank = rank;
            this.bestFive = new ArrayList<>(bestFive);
            this.score = score;
        }

        public HandRank rank() { return rank; }
        public List<Card> bestFive() { return bestFive; }
        public long score() { return score; }
    }

    /**
     * Finds the best 5-card hand from 7 cards (2 private + 5 table).
     */
    static class HandEvaluator {

        /**
         * Returns the best hand result for one player.
         */
        static HandResult findBest(Player player, List<Card> tableCards) {
            // Combine the player's cards with the table cards
            List<Card> allCards = new ArrayList<>(player.getOwnCards());
            allCards.addAll(tableCards);

            // Generate all possible 5-card combos
            List<List<Card>> combos = new ArrayList<>();
            buildCombos(allCards, 0, new ArrayList<>(), combos);

            // Pick the highest-scoring combo
            HandResult best = null;
            for (List<Card> combo : combos) {
                HandResult result = evaluateFive(combo);
                if (best == null || result.score() > best.score()) {
                    best = result;
                }
            }
            return best;
        }

        /**
         * Recursively build all 5-card subsets from a list of cards.
         */
        private static void buildCombos(
            List<Card> cards,
            int start,
            List<Card> current,
            List<List<Card>> output
        ) {
            if (current.size() == 5) {
                output.add(new ArrayList<>(current));
                return;
            }
            for (int i = start; i < cards.size(); i++) {
                current.add(cards.get(i));
                buildCombos(cards, i + 1, current, output);
                current.remove(current.size() - 1);
            }
        }

        /**
         * Evaluates exactly five cards: counts pairs, checks flush/straight, etc.
         */
        private static HandResult evaluateFive(List<Card> hand) {
            // Count how many of each rank we have
            Map<Rank, Integer> rankCount = new HashMap<>();
            // Group cards by suit to check for flush
            Map<Suit, List<Card>> suitGroups = new HashMap<>();
            for (Card c : hand) {
                rankCount.put(c.rank(), rankCount.getOrDefault(c.rank(), 0) + 1);
                suitGroups.computeIfAbsent(c.suit(), k -> new ArrayList<>()).add(c);
            }

            // Flush if any suit has all 5 cards
            boolean isFlush = suitGroups.values().stream()
                .anyMatch(list -> list.size() == 5);

            // Straight: look for 5 cards in a row
            Set<Integer> values = new HashSet<>();
            for (Card c : hand) values.add(c.rank().value());
            List<Integer> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            boolean isStraight = false;
            int topStraightValue = 0;
            for (int i = 0; i < sorted.size(); i++) {
                int count = 1;
                int last = sorted.get(i);
                for (int j = i + 1; j < sorted.size(); j++) {
                    if (sorted.get(j) == last + 1) {
                        count++; last++;
                    } else if (sorted.get(j) != last) {
                        break;
                    }
                }
                if (count >= 5) {
                    isStraight = true;
                    topStraightValue = last;
                }
            }
            // Special case A-2-3-4-5
            if (!isStraight && values.containsAll(Arrays.asList(14, 2, 3, 4, 5))) {
                isStraight = true;
                topStraightValue = 5;
            }

            // Determine hand category by checking counts and flags
            HandRank category;
            if (isStraight && isFlush) {
                category = (topStraightValue == 14)
                    ? HandRank.ROYAL_FLUSH
                    : HandRank.STRAIGHT_FLUSH;
            } else if (rankCount.containsValue(4)) {
                category = HandRank.FOUR_OF_A_KIND;
            } else if (rankCount.containsValue(3) && rankCount.containsValue(2)) {
                category = HandRank.FULL_HOUSE;
            } else if (isFlush) {
                category = HandRank.FLUSH;
            } else if (isStraight) {
                category = HandRank.STRAIGHT;
            } else if (rankCount.containsValue(3)) {
                category = HandRank.THREE_OF_A_KIND;
            } else if (Collections.frequency(rankCount.values(), 2) >= 2) {
                category = HandRank.TWO_PAIR;
            } else if (rankCount.containsValue(2)) {
                category = HandRank.PAIR;
            } else {
                category = HandRank.HIGH_CARD;
            }

            // Score: base by category plus tie-breakers from ranks
            long baseScore = category.ordinal() * 1_000_000_000L;
            List<Integer> kickers = new ArrayList<>();
            rankCount.entrySet().stream()
                .sorted((a, b) -> {
                    int diff = b.getValue() - a.getValue();
                    if (diff != 0) return diff;
                    return b.getKey().value() - a.getKey().value();
                })
                .forEach(e -> {
                    for (int i = 0; i < e.getValue(); i++) {
                        kickers.add(e.getKey().value());
                    }
                });
            while (kickers.size() < 5) kickers.add(0);
            long kickerScore = 0;
            for (int i = 0; i < 5; i++) {
                kickerScore += kickers.get(i) * Math.pow(100, 4 - i);
            }

            return new HandResult(category, hand, baseScore + kickerScore);
        }
    }

    /**
     * Simple game manager: holds players, table cards, evaluates and announces winners.
     */
    static class PokerGame {
        private final List<Player> players;
        private List<Card> tableCards;

        /**
         * Create a game with a fixed number of players.
         */
        PokerGame(int numberOfPlayers) {
            this.players = new ArrayList<>();
            for (int i = 1; i <= numberOfPlayers; i++) {
                this.players.add(new Player(i));
            }
        }

        /**
         * Get the list of players in this game.
         */
        List<Player> getPlayers() {
            return Collections.unmodifiableList(players);
        }

        /**
         * Set the shared table cards (5 cards).
         */
        void setTableCards(List<Card> cards) {
            this.tableCards = cards;
        }

        /**
         * Show what a given player sees: their cards and the table.
         */
        void showPlayerView(Player player) {
            System.out.println(
                player + " sees cards: " +
                player.getOwnCards() +
                " and table: " + tableCards
            );
        }

        /**
         * Check each player's best hand and return the results.
         */
        Map<Player, HandResult> checkAllHands() {
            Map<Player, HandResult> results = new LinkedHashMap<>();
            for (Player player : players) {
                HandResult best = HandEvaluator.findBest(player, tableCards);
                results.put(player, best);
            }
            return results;
        }

        /**
         * Pick the winner(s) based on highest hand scores.
         */
        List<Player> pickWinners(Map<Player, HandResult> results) {
            long topScore = results.values().stream()
                .mapToLong(HandResult::score)
                .max()
                .orElse(0);
            List<Player> winners = new ArrayList<>();
            for (Map.Entry<Player, HandResult> entry : results.entrySet()) {
                if (entry.getValue().score() == topScore) {
                    winners.add(entry.getKey());
                }
            }
            return winners;
        }

        /**
         * Print out each player's best five cards and announce the winner(s).
         */
        void announceResults(
            Map<Player, HandResult> results,
            List<Player> winners
        ) {
            for (Map.Entry<Player, HandResult> entry : results.entrySet()) {
                Player player = entry.getKey();
                HandResult result = entry.getValue();
                System.out.println(
                    player + " best combo: " +
                    result.bestFive() +
                    ", called a " + friendlyName(result.rank())
                );
            }
            System.out.println(
                "--> " + winners +
                " win with " + friendlyName(
                    results.get(winners.get(0)).rank()
                )
            );
        }

        /**
         * Convert HandRank enum to a human-friendly name.
         */
        private String friendlyName(HandRank hr) {
            switch (hr) {
                case HIGH_CARD:       return "High Card";
                case PAIR:            return "One Pair";
                case TWO_PAIR:        return "Two Pair";
                case THREE_OF_A_KIND: return "Three of a Kind";
                case STRAIGHT:        return "Straight";
                case FLUSH:           return "Flush";
                case FULL_HOUSE:      return "Full House";
                case FOUR_OF_A_KIND:  return "Four of a Kind";
                case STRAIGHT_FLUSH:  return "Straight Flush";
                case ROYAL_FLUSH:     return "Royal Flush";
                default:              return hr.name();
            }
        }
    }
}
