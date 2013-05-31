package treegame;

import mytools.*;

public class AlphaBeta
    extends Search {

  // Declare an inner class to maintain a stack of tree data.

  private static class NodeRec {
    int score; //	best score so far
    int move; //	best move so far (-1 if none defined)
    int alpha, beta;
  }

  private NodeRec nodeRecords[];
  private final int maxRecs = (MAX_PLY + 1);
  private int recTotal;
  private int currentDepth;

  // Constructor
  public AlphaBeta(Rules r) {
    super(r);
    nodeRecords = new NodeRec[maxRecs];

    // create some objects in the array.
    for (int i = 0; i < maxRecs; i++) {
      nodeRecords[i] = new NodeRec();
    }

    // set the number of objects used to zero.
    recTotal = 0;
  }

  // Process the alpha/beta search.  Returns true if aborted.
  protected boolean proc(int evalOptionsParm) {

    boolean abortFlag;

    evalOptions = evalOptionsParm;

    // Prepare the search algorithm stack.
    // We set up the first record to be the topmost ply.

    NodeRec startRec = nodeRecords[recTotal++];

    do {
      resetTimer();

      currentDepth = searchDepth;

      startRec.move = -1; // best move is undefined
      startRec.beta = Eval.WIN_SCORE * 2;
      startRec.alpha = -startRec.beta;

//db.pr("applying alphaBeta, total moves = "+rules.legalMoveCount()+ " state="+rules.state() );
//db.pr(" currentDepth="+currentDepth);

      abortFlag = apply();
//db.pr("abortFlag="+abortFlag);

      if (abortFlag) {
        break;
      }

    }
    while (false);

    // Remove the item from the stack.
    recTotal--;

    return abortFlag;
  }

  // Recursive minimax algorithm
  // returns true if aborted
  private boolean apply() {

    // Add 1 to think timer for every traversal
    addToTimer(TIME_TRAV);

    NodeRec parentNode = nodeRecords[recTotal - 1];

    // Is this an evaluation node?
    // It is if the depth is zero or the game is over.

    if (
        currentDepth == 0
        || rules.state() != Rules.STATE_PLAYING
        ) {
      incrementNodeCounter(true);

      if (rules.state() == Rules.STATE_PLAYING) {
        // Add 5 to think timer for every evaluation
        addToTimer(TIME_EVAL);
        parentNode.score = eval.process(rules, evalOptions, false);
      }
      else {
        if (rules.state() >= Rules.STATE_WON) {
          // If the game is won, it means we are the ones who lost.

          // Add a depth factor to the score to
          //	a) try to encourage as quick a win as possible
          //	b) provide information about when the game will end

          // The moves to victory will be calculated using this function:
          //	MAX_PLY - (|score| - WIN_SCORE)
          parentNode.score = - (Eval.WIN_SCORE + MAX_PLY - searchDepth +
                                currentDepth);
        }
        else {
          // Tie game
          parentNode.score = 0;
        }
      }
//			db.pr(" evaluating to score "+parentNode.score);

      return false;
    }

    // It's not an evaluation node, so we have to test all the possible
    // moves and call the algorithm recursively.

    incrementNodeCounter(false);

    boolean abortFlag = false;

    preSortMoves();
    // Now process the moves in the pre-sorted order.

//		db.pr(" legalMoveCount = "+rules.legalMoveCount() );

    for (int j = 0; j < rules.legalMoveCount(); j++) {
//			db.pr("AlphaBeta, j="+j+" of legalMoveCount "+rules.legalMoveCount() );

      int i = preSortedMoves[preSortMoveIndStart + j].getIndex();

      if (i < 0 || i >= rules.legalMoveCount()) {
        Debug.print("preSortMoveInd " + i + " is illegal!");
        for (int k = 0; k < rules.legalMoveCount(); k++) {
          Debug.print(" sorted move " + (preSortMoveIndStart + k) + ": "
                      + preSortedMoves[preSortMoveIndStart + k]);
        }
        //db.a(i >= 0 && i < rules.legalMoveCount(), "i "+i+" is out of range");
      }

      // If some different input commands have been specified,
      // stop thinking.

      abortFlag = abortSearch();
      if (abortFlag) {
        break;
      }

      if (currentDepth == searchDepth) {
        displayMove(i);
      }

      int thisScore;

      {
        // Make the move, allocate another record, and advance the presort stack ptr.

//Debug.print("AlphaBeta, making move "+i+" of total "+rules.legalMoveCount()+" depth "+currentDepth);

//Debug.print(" here is the board:"); rules.printBoard();

//db.a(true,""); int prevTotalMoves = rules.legalMoveCount(); String mvStr = ""+rules.legalMove(i);

        rules.makeMove(i, false);
        NodeRec childNode = nodeRecords[recTotal++];
        preSortMoveIndStart += rules.maxMoves();

        currentDepth--;
        childNode.move = -1;
        childNode.alpha = -parentNode.beta;
        childNode.beta = -parentNode.alpha;

        // Call the function recursively.
        abortFlag = apply();

        // Decrement the presort stack ptr, deallocate the record, unmake the move.
        currentDepth++;
        preSortMoveIndStart -= rules.maxMoves();

        recTotal--;
//				Debug.print("AlphaBeta, unmaking move "+i+" depth "+currentDepth);

        rules.unMove();
//db.a(prevTotalMoves == rules.legalMoveCount(), "Total moves "+rules.legalMoveCount()+" has changed from "+prevTotalMoves+", move "+mvStr);

        thisScore = -childNode.score;
      }
      if (abortFlag) {
        break;
      }

      // If this is the first ply, add this move's evaluation to the list.
      if (currentDepth == searchDepth) {
        addMoveToList(new EvalMove(i, thisScore));
      }

      // Perform a beta cutoff if necessary.
      if (thisScore >= parentNode.beta) {
        parentNode.move = -1; // Leave this branch undefined.
        parentNode.score = thisScore;
        break;
      }
      else {
        if (parentNode.alpha < thisScore) {
          parentNode.alpha = thisScore;
        }

        // Determine if this is the best we've found so far.
        if (parentNode.move < 0 || thisScore > parentNode.score) {
          parentNode.move = i;
          parentNode.score = thisScore;
        }
      }
    }
    return abortFlag;
  }
}
