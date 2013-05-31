package treegame;
import mytools.*;

public class Brain implements Runnable {

   private Thread thread;

	/** Holds 1+move number being considered, or -1 if no longer thinking */
//   public static final int OUT_PROGRESS_SIGNAL = 0;
   public static final int OUT_PROGRESS_MOVE = 0;
   private static final int OUT_PROGRESS_LEN = 1;

//   public static final int OUT_CMD_SIGNAL = 0;
   public static final int OUT_CMD_MOVE = 0;
   public static final int OUT_CMD_SCORE = 1;
   private static final int OUT_CMD_LEN = 2;

   /** CMD_xx value to assign a task to the brain thread */
   public static final int IN_CMD_TASK = 0;
   /** Index of move made for CMD_PROCMOVE */
   public static final int IN_CMD_MOVE = 1;

   private static final int IN_CMD_LEN = 2;

   /** No command assigned.  The brain may take this opportunity
    *  to anticipate the opponent's move and think of a suitable
    *  response.  This is 'think ahead' play.
    */
	public static final int CMD_NONE = 0;		   // no command

   /** Choose a move */
	public static final int CMD_CHOOSEMOVE = 1;

   /** Clear any thinking that may be going on, and reset any
    *  'think ahead' information that may have been produced.
    *  This command is set when the normal game progress is
    *  interrupted, for instance when a move is undone or redone.
    */
	public static final int CMD_CLEAR = 2;

   /** Process a move that was made */
	public static final int CMD_PROCMOVE = 4;

//   public static final int IN_ALG_MODIFIED = 0;
   public static final int IN_ALG_SEARCHDEPTH = 0;
   public static final int IN_ALG_ALGORITHM = 1;
   public static final int IN_ALG_THINKAHEAD = 2;
   public static final int IN_ALG_EVALOPTIONS = 3;
   private static final int IN_ALG_LEN = 4;

   /**
    * Constructor
    * @param r game's Rules object
    * @param e game's Eval object
    * @param p the number of opponent moves to consider
    *  for 'think ahead' play
    */
	public Brain(Rules r, Eval e, int p) {
		mainRules = r;
		eval = e;
		maxPredictedMoves = p;

		searchTrees = new Search[2];
		searchTrees[0] = new MiniMax(mainRules);
		searchTrees[1] = new AlphaBeta(mainRules);

		initPredictionVars();

		rules = mainRules.makeNew();
		rules.prepareHistory();

		sortedMoves = EvalMove.createBuffer(rules.maxMoves());
		sortedOppMoves = EvalMove.createBuffer(rules.maxMoves());
		sortedRespMoves = EvalMove.createBuffer(rules.maxMoves());

      // Initialize the algorithmIn registers to default values in
      // case we're not hooked up to a controlling panel.

      algorithmIn.setSignal();
      algorithmIn.setArg(IN_ALG_SEARCHDEPTH,4);
      algorithmIn.setArg(IN_ALG_ALGORITHM,1);
      algorithmIn.setArg(IN_ALG_THINKAHEAD,0);
      algorithmIn.setArg(IN_ALG_EVALOPTIONS,0);
	}

   /**
    * Return ThreadCommand object
    * @param n 0 for CMD input, 1 for ALGORITHM input, 2 for
    *  PROGRESS output, 3 for SEARCH output, 4 for CMD output
    */
   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 0:
         tc = cmdIn;
         break;
      case 1:
         tc = algorithmIn;
         break;
      case 2:
         tc = tcProgressOut;
         break;
      case 3:
         tc = searchOut;
         break;
      case 4:
         tc = cmdOut;
         break;
      }
      return tc;
   }

   /**
    * Update the OUT_PROGRESS register to reflect a move the
    * brain is thinking about.
    * @param iMove the move being considered
    */
   public void setThinkingMove(int iMove) {
      synchronized(tcProgressOut) {
         if (iMove != tcProgressOut.getArg(OUT_PROGRESS_MOVE)) {
            tcProgressOut.setSignal();
           	tcProgressOut.setArg(OUT_PROGRESS_MOVE,iMove);
         }
      }
   }

   /**
    * Start the brain thread
    */
	public void startThread() {
//      db.pr("brain startThread()");
//      db.pr(" thread = "+this.isAlive() );
//		haltThreadFlag = false;
      thread = new Thread(this);
      thread.start();
//      thread.start();
//		start();
	}

   /**
    * Stop the brain thread
    */
	public void stopThread() {
      thread = null;
//		haltThreadFlag = true;
//      db.pr("brain stopThread");
	}

   /**
    * Determine if the brain has been given a new task, different
    * than the	one we are currently working on.  Used by the search
    * algorithm to interrupt its search.
    */
	public boolean taskHasChanged() {
      return (thread == null) //haltThreadFlag
       || !cmdIn.matchesLock(cmdWork)
       || algorithmIn.readSignalLock();
	}

	public void run() {
//      db.pr("brain thread run() start");

		// Keep running the thread until someone has told us to halt.
		while (thread != null /*!haltThreadFlag*/) {
//         db.pr("haltThreadFlag = "+haltThreadFlag);
			synchronized(mainRules) {
				// Copy the rules object to our internal object.
				mainRules.copyTo(rules, true);
			}

         // If a result from the last command has not yet been processed,
         // act as if no new command has been issued.

         cmdIn.copyToLock(cmdWork);

         if (cmdOut.readSignal()) {
            cmdWork.setArg(IN_CMD_TASK,0);
            cmdWork.setArg(IN_CMD_MOVE,0);
         }

			// Update the internal search depth, think ahead registers.

//         db.pr("brain, testing algorithmIn " + algorithmIn);
			synchronized(algorithmIn) {
            if (algorithmIn.testSignal()) {
               searchDepth = algorithmIn.getArg(IN_ALG_SEARCHDEPTH);
//         db.pr("brain, read SEARCHDEPTH "+searchDepth);
               searchOptions = algorithmIn.getArg(IN_ALG_THINKAHEAD);
               thinkAheadFlag = (searchOptions != 0);
               searchAlgorithm = algorithmIn.getArg(IN_ALG_ALGORITHM);
               evaluatorOptions = algorithmIn.getArg(IN_ALG_EVALOPTIONS);
            }
			}

         Debug.ASSERT(searchDepth > 0, "search depth is "+searchDepth+" in Brain thread");
			int effectiveCmd = cmdWork.getArg(IN_CMD_TASK);

			// Is there a new command to process?  If not,
			// see if we should start predicting a human move,
			// and thinking about a response to it.

			if (
				effectiveCmd == CMD_NONE
			 && rules.humanTurn()
			 && thinkAheadFlag
			) {

				// If we have already predicted the human's move, don't
				// bother rethinking this part.

				if (!predictedMoveFlag) {
					thinkAheadDepth = 2;
					effectiveCmd = CMD_ESTMOVE;
				} else {
					// Find the predicted human move for which we have calculated
					// the lowest-ply response.
					workResponse = -1;

					int lowestDepth = Search.MAX_PLY;

					int currConditions = getAlgConditions();

					for (int i=0; i<predictedCount; i++) {
						// If algorithm has changed, clear this move's depth
						if (currConditions != (calcRespInfo[i] & ~0xff))
							calcRespInfo[i] = currConditions;

						int thisDepth = calcRespInfo[i] & 0xff;
						if (thisDepth < lowestDepth) {
							lowestDepth = thisDepth;
							workResponse = i;
						}
					}

					if (workResponse >= 0) {
						effectiveCmd = CMD_CALCRESP;
						thinkAheadDepth = Math.max(searchDepth, lowestDepth + 1);
                  if (DISP_THINKAHEAD)
                     Debug.print("Looking for response to human move "
                      +workResponse+"("+predictedMoves[workResponse].getIndex()+
                      ")"+", depth "+thinkAheadDepth);
					}
				}
			}

			EvalMove ev = new EvalMove();
			boolean abortFlag;

			switch (effectiveCmd) {
			case CMD_CHOOSEMOVE:
				// If player has just made a move we predicted, and we have a
				// response ready, respond with it.

				if (
					predictedMoveFlag
					// last move number should equal the predicted number.
             && predictedMoveNumber+1 == cmdWork.getArg(IN_CMD_MOVE)
				) {
               if (DISP_THINKAHEAD)
                  {Debug.print("Brain is responding immediately to predicted human move "
                   +responseUsedIndex+"(" + predictedMoves[responseUsedIndex].getIndex() +")"); }

					outputChosenMove(calculatedResponses[responseUsedIndex]);

					// Copy the stats that we produced for this response to the
					// output stat record.
               thinkStats[responseUsedIndex].copyToLock(searchOut);
					predictedMoveFlag = false;
					break;
				}
				predictedMoveFlag = false;
				abortFlag = chooseMove(effectiveCmd, searchDepth, ev);
				if (!abortFlag)
					outputChosenMove(ev);
				break;

			case CMD_ESTMOVE:
			case CMD_CALCRESP:
				abortFlag = chooseMove(effectiveCmd, thinkAheadDepth, ev);
				break;

			case CMD_PROCMOVE:
				processMove();
				break;

			case CMD_CLEAR:
				predictedMoveFlag = false;
            signalTaskCompleted(0,0);
            // Also clear the progress and search output objects.
            setThinkingMove(0);
            searchOut.setArgLock(0,0);
				break;
			}

			// sleep for .1 sec to let other threads go.
         ThreadCommand.sleep(100);
		}
//      Debug.print("brain thread run() stop");
	}

	private void initPredictionVars() {
		predictedMoves = EvalMove.createBuffer(maxPredictedMoves);
		calculatedResponses = EvalMove.createBuffer(maxPredictedMoves);
		calcRespInfo = new int[maxPredictedMoves];
		thinkStats = new ThreadCommand[maxPredictedMoves];
		for (int i=0; i<maxPredictedMoves; i++)
			thinkStats[i] = new ThreadCommand(2);
	}

	// Determine the algorithm conditions, but with the
   // depth masked off to zero.
	private int getAlgConditions() {
		return (
			(searchAlgorithm << 8)
		  | (searchOptions << 12)
		  | (evaluatorOptions << 20)
		);
	}

	// Choose the best move.
	// Returns best evaluated move in ev.  Returns true if thinking
	// was aborted (thread command was changed).
	private boolean chooseMove(int command, int depth, EvalMove ev) {
		Search treeObj = searchTrees[searchAlgorithm];

		boolean abortFlag = false;

		if (command == CMD_CALCRESP) {
			// Make the human move we predicted before looking for a response.
			rules.makeMove(predictedMoves[workResponse].getIndex(), false);
		}

		EvalMove sortedList[] = null;
		boolean doEvaluation = false;
		ThreadCommand info = null;
		{

			switch (command) {
			case CMD_CHOOSEMOVE:
				sortedList = sortedMoves;
				info = new ThreadCommand(2);
				// If there is only one legal move, choose it and set
				// evaluated move value to undefined.
				if (rules.legalMoveCount() == 1) {
					sortedList[0].setIndex(0);
					sortedList[0].setScore(0);
					break;
				}
				doEvaluation = true;
				break;

			case CMD_ESTMOVE:
				sortedList = sortedOppMoves;
				doEvaluation = true;
				break;

			case CMD_CALCRESP:
				if (rules.state() == Rules.STATE_PLAYING) {
					// We only use the best move returned of this array.
					sortedList = sortedRespMoves;

					// Save the stats we will generate about this move in the array.
					info = thinkStats[workResponse];

					doEvaluation = true;
				}
				break;
			}

			if (doEvaluation) {
				abortFlag = treeObj.process(
					this, command == CMD_CHOOSEMOVE,
					depth, rules, eval, sortedList, info,
					evaluatorOptions
				 );
			}
		}

		if (!abortFlag) {
			switch (command) {
			case CMD_CHOOSEMOVE:
				sortedList[0].copyTo(ev);
				info.copyToLock(searchOut);
				break;

			case CMD_ESTMOVE:
				// Copy the first n moves to the predicted array.
				predictedCount = Math.min(maxPredictedMoves, rules.legalMoveCount());
            if (DISP_THINKAHEAD)
               Debug.print("finished estimating human moves, clearing response depths");
				for (int i=0; i<predictedCount; i++) {
					calcRespInfo[i] = 0;
					sortedList[i].copyTo(predictedMoves[i]);
               if (DISP_THINKAHEAD)
               	Debug.print(" human move "+predictedMoves[i].getIndex()+
                     ", score "+predictedMoves[i].getScore() );
				}
				predictedMoveFlag = true;
				predictedMoveNumber = rules.getMoveNumber();
				break;

			case CMD_CALCRESP:
				if (doEvaluation) {
					sortedList[0].copyTo(calculatedResponses[workResponse]);
					calcRespInfo[workResponse] = thinkAheadDepth | getAlgConditions();
               if (DISP_THINKAHEAD)
                  Debug.print(" storing depth "+thinkAheadDepth+
                     " for response index "+workResponse);
				}
				break;
			}
		}

		if (command == CMD_CALCRESP)
			rules.unMove();

		return abortFlag;
	}

	private void outputChosenMove(EvalMove e) {
      signalTaskCompleted(e.getIndex(), e.getScore() );
	}

	// Process a move that was just made.
	private void processMove() {

		int moveIndex = cmdWork.getArg(IN_CMD_MOVE);

		// If we anticipated some human moves, see if this is one of them.
		if (predictedMoveFlag) {
			boolean foundMove = false;
			for (int i=0; i<predictedCount; i++) {
				if (
					predictedMoves[i].getIndex() == moveIndex
				 && (calcRespInfo[i] & 0xff) >= searchDepth
				 && (calcRespInfo[i] & ~0xff) == getAlgConditions()
				) {
					responseUsedIndex = i;
               if (DISP_THINKAHEAD)
                  Debug.print("Human move "+moveIndex+", we have response #"
                   +responseUsedIndex+" depth "+(calcRespInfo[i] & 0xff)+
                   ", >= required "+searchDepth);
					foundMove = true;
					break;
				}
			}
			if (!foundMove)
				predictedMoveFlag = false;
		}

      setThinkingMove(0);

      signalTaskCompleted(0,0);
	}

   private void signalTaskCompleted(int arg1, int arg2) {
      synchronized(cmdIn) {
         synchronized(cmdOut) {
            if (cmdIn.matches(cmdWork)) {
               cmdOut.setSignal();
               cmdOut.setArg(OUT_CMD_MOVE,arg1);
               cmdOut.setArg(OUT_CMD_SCORE,arg2);
               // Notify any threads that may have been wait()ing
               // for this task to be completed.
               cmdOut.notifyAll();
            }
         }
      }
   }

	// For thinking ahead while human is moving:
	//  estimating what human's move will be
	private static final int CMD_ESTMOVE = 5;
	//  calculating response to this move
	private static final int CMD_CALCRESP = 6;

//	private boolean haltThreadFlag;			// to stop thread run() loop
	private ThreadCommand cmdWork = new ThreadCommand(IN_CMD_LEN);
	private Search searchTrees[];

	// for CMD_ESTMOVE process:
	private int maxPredictedMoves;			// maximum # human moves we'll consider
	private int predictedMoveNumber;		// the rules move number for the predicted move
	private int thinkAheadDepth;			// depth to use for computer response
	private boolean predictedMoveFlag;		// true if we have predicted the next move
	private EvalMove predictedMoves[];		// opponent moves that were predicted
	private EvalMove sortedOppMoves[];		// opponent moves, sorted into order
	private EvalMove sortedRespMoves[];		// sorted response moves for single
											//  predicted opponent move; we only
											//	use the first in the array however
	private int predictedCount;				// number of moves we predicted

	// for CMD_CALCRESP process:
	private int workResponse;				// the index of the human move we are thinking of
											//	a response for

	private int responseUsedIndex;			// index into predictedMoves[] of human's move
	private EvalMove calculatedResponses[];	// response to this move that we thought of
	private int calcRespInfo[];
	// This holds the algorithm conditions each response was calculated with.  We
	// want to make sure the old conditions are still valid, or we throw out the result.
	// [0..7]	search depth
	// [8..11]	algorithm used
	// [12..19] search options
	// [20..27] evaluation options

	private ThreadCommand thinkStats[];			// stats to report for each think ahead move

	private boolean thinkAheadFlag;
	private int searchDepth;
	private int searchAlgorithm;
	private int evaluatorOptions;
	private int searchOptions;
	private Rules mainRules;				//	the rules sent from the caller
	private Rules rules;					//	our internal rules object, to scan ahead with
	private Eval eval;
	private EvalMove sortedMoves[];
   private static final boolean DISP_THINKAHEAD = false;
	// The brain receives commands and returns information using these ThreadCommand objects:
	private ThreadCommand cmdIn = new ThreadCommand(IN_CMD_LEN);
	// [0]: command (CMD_xxx)

   private ThreadCommand cmdOut = new ThreadCommand(OUT_CMD_LEN);
	// [0]: true if command was processed
	// When CMD_CHOOSEMOVE has been processed, these values are stored:
	// [1]: move index to make
	// [2]: evaluated score for this move

	private ThreadCommand tcProgressOut = new ThreadCommand(OUT_PROGRESS_LEN);
	// [0]:	Holds 1+move number being considered, or -1 if no longer thinking

	private ThreadCommand searchOut = new ThreadCommand(2);
	// Returns information about the move selection.
	// [0]: number of nodes traversed
	// [1]: number of position evaluations performed
	private ThreadCommand algorithmIn = new ThreadCommand(IN_ALG_LEN);
	// See BrnPnl:dataOut for a breakdown on these values:
	// [0]: search depth (1..n)
	// [1]: algorithm to use (0:minimax 1:alphabeta)
	// [2]: search options
	// [3]: evaluator options
	// This object must be defined before the brain thread is started.
}
