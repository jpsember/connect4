package treegame;

import mytools.*;

public abstract class Search {
  public static final int MAX_PLY = 16;

  protected Rules rules;
  protected Eval eval;
  protected Brain brain;
  private EvalMove sortedList[];
  private int sortedListCount;
  protected int searchDepth;
  private boolean displayThinkingFlag;
  private int nodeProcTotal;
  private int evaluations;
  protected int evalOptions;

  // Arrays for pre-sorting the moves by some criterion to try
  // to look at the moves approximately best...worst.
  protected EvalMove preSortedMoves[];

  protected int preSortMoveIndStart;

  protected int thinkTimer; //	keeps track of # traversals & evaluations
  protected final int TIME_TRAV = 1;
  protected final int TIME_EVAL = 5;
  protected final int TIME_YIELD = 50;

  protected void addMoveToList(EvalMove e) {
    e.copyTo(sortedList[sortedListCount++]);
  }

  protected Search(Rules r) {
    preSortedMoves = EvalMove.createBuffer(r.maxMoves() * MAX_PLY);
  }

  // Pre-sort the moves before examining them.
  protected void preSortMoves() {

    for (int i = 0; i < rules.legalMoveCount(); i++) {
      preSortedMoves[preSortMoveIndStart + i].setIndex(i);
      preSortedMoves[preSortMoveIndStart +
          i].setScore(rules.legalMove(i).preSortValue());
    }

    EvalMove.sortArray(preSortedMoves, preSortMoveIndStart,
                       rules.legalMoveCount());
  }

  // Display the move we're thinking about
  protected void displayMove(int moveNumber) {
    if (!displayThinkingFlag) {
      return;
    }

    brain.setThinkingMove(moveNumber + 1);
    //progressOut.setArgLock(0, moveNumber + 1);
//		synchronized(brain.progressOut) {
//			brain.progressOut.set(0,moveNumber+1,true);
//		}
  }

  // Add a factor to the think timer.  If it exceeds a particular value,
  // yield the thread to let some others have a go.
  protected void addToTimer(int time) {
    thinkTimer += time;
    if (thinkTimer > TIME_YIELD) {
      resetTimer();
//         ThreadCommand.sleep(200);
      Thread.yield(); // can we do this?
    }
  }

  protected void resetTimer() {
    thinkTimer = 0;
  }

  protected abstract boolean proc(int evalOptionsParm);

  protected void incrementNodeCounter(boolean evaluation) {
    nodeProcTotal++;
    if (evaluation) {
      evaluations++;
    }
  }

  // Process a search.  Returns true if search was aborted.
  public boolean process(Brain brainParm, boolean dispThinkParm, int depthParm,
                         Rules rulesParm, Eval evalParm, EvalMove sortListParm[],
                         ThreadCommand nodeInfo,
                         int evalOptions) {

//	 	db.pr("Search process() depth="+depthParm+" rules total moves="+rulesParm.legalMoveCount() + " evalOptions "+evalOptions);

    brain = brainParm;
    searchDepth = depthParm;
    rules = rulesParm;
    eval = evalParm;
    sortedList = sortListParm;
    sortedListCount = 0;

    displayThinkingFlag = dispThinkParm;

    nodeProcTotal = 0;
    evaluations = 0;
    preSortMoveIndStart = 0;

    boolean abortFlag = proc(evalOptions);

    if (!abortFlag) {
      EvalMove.sortArray(sortedList, 0, rules.legalMoveCount());
    }

    if (nodeInfo != null) {
      synchronized (nodeInfo) {
        nodeInfo.setArg(0, nodeProcTotal);
        nodeInfo.setArg(1, evaluations);
      }
    }

    return abortFlag;
  }

  protected boolean abortSearch() {
    return brain.taskHasChanged();
  }
}
