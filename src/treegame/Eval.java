package treegame;

public abstract class Eval {
	public static final int WIN_SCORE = 10000;

	// Constructor
	public Eval() {}

	// Evaluate the board position relative to the player whose turn it is.
	// Returns a value for the board position; positive is good for the current player.
	public abstract int process(Rules absRules, int evalOptions, boolean saveResults);
}
