package treegame;
public class MiniMax extends Search {

	private int currentDepth;

	// Declare an inner class to maintain a stack of minimax data.
	private class NodeRec {
		int score;		//	best score so far
		int move;		//	best move so far (-1 if none defined)
	}

	private NodeRec nodeRecords[];
	private final int maxRecs = (MAX_PLY + 1);
	private int recTotal;

	// Constructor
	public MiniMax(Rules r) {
		super(r);
		nodeRecords = new NodeRec[maxRecs];

		// allocate node records on stack
		for (int i=0; i<maxRecs; i++)
			nodeRecords[i] = new NodeRec();

		// indicate none of the records are active
		recTotal = 0;
	}

	protected boolean proc(int evalOptionsParm) {

		evalOptions = evalOptionsParm;

		boolean abortFlag;

		// Prepare the minimax algorithm stack.
		// We set up the first record to be the topmost ply.

		NodeRec startRec = nodeRecords[recTotal++];

		do {
			resetTimer();

			currentDepth = searchDepth;
			startRec.move = -1;		// best move is undefined

			abortFlag = apply();
			if (abortFlag) break;

		} while (false);

		// Remove the item from the stack.
		recTotal--;

		return abortFlag;
	}

	// Recursive minimax algorithm
	// returns true if aborted
	private boolean apply() {

		// Add 1 to think timer for every traversal
		addToTimer(TIME_TRAV);

		NodeRec parentNode = nodeRecords[recTotal-1];

		// Is this an evaluation node?
		// It is if the depth is zero or the game is over.

		if (
			currentDepth == 0
		 ||	rules.state() != Rules.STATE_PLAYING
		) {
			incrementNodeCounter(true);

			if (rules.state() == Rules.STATE_PLAYING) {
				// Add 5 to think timer for every evaluation
				addToTimer(TIME_EVAL);
				parentNode.score = eval.process(rules, evalOptions, false);
			} else {
				if (rules.state() >= Rules.STATE_WON) {
					// Add a depth factor to the score to
					//	a) try to encourage as quick a win as possible
					//	b) provide information about when the game will end

					// The moves to victory will be calculated using this function:
					//	MAX_PLY - (|score| - WIN_SCORE)
					parentNode.score = -(Eval.WIN_SCORE + MAX_PLY - searchDepth + currentDepth);
				} else {
					// Tie game
					parentNode.score = 0;
				}
			}
			return false;
		}

		// It's not an evaluation node, so we have to test all the possible
		// moves and call the algorithm recursively.

		incrementNodeCounter(false);

		boolean abortFlag = false;
		preSortMoves();
		for (int j=0; j<rules.legalMoveCount(); j++) {
			int i = preSortedMoves[preSortMoveIndStart+j].getIndex();

			// If some different input commands have been specified,
			// stop thinking.

			abortFlag = abortSearch();
			if (abortFlag)
				break;

			if (currentDepth == searchDepth)
				displayMove(i);

			int thisScore;
			{
				rules.makeMove(i, false);
				preSortMoveIndStart += rules.maxMoves();
				NodeRec childNode = nodeRecords[recTotal++];
				currentDepth--;
				childNode.move = -1;

				abortFlag = apply();
				thisScore = -childNode.score;

				currentDepth++;
				recTotal--;
				rules.unMove();
				preSortMoveIndStart -= rules.maxMoves();
			}

			if (abortFlag) break;

			if (parentNode.move < 0 || thisScore > parentNode.score) {
				parentNode.move = i;
				parentNode.score = thisScore;
			}
			if (currentDepth == searchDepth)
				addMoveToList(new EvalMove(i, thisScore));
		}
		return abortFlag;
	}
}
