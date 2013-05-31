package treegame;

public abstract class Rules {
  // generic rules fields:

  // Game states
  public final static int STATE_PLAYING = 0;
  public final static int STATE_DRAWN = 1;
  public final static int STATE_DRAWNREP = 2; // draw by repetition
  public final static int STATE_DRAWNSTAG = 3; // draw by stagnation
  public final static int STATE_DRAWNMAXMOVES = 4; // maximum # moves in game reached
  public final static int STATE_WON = 5; //	+ player (0..1)

  protected int moveNumber;
  protected int gameState;
  protected int plrTurn;
  protected int scores[];
  protected int controllers[];
  protected History history;
  protected Move legalMoves[];
  protected int legalMoveCount;
  protected boolean fGamePaused;

  // repetition/stagnation detection:

  protected int maxMovesPerGame;
  protected boolean includeImages; // true if we are concerned with repetition images
  protected int stagnateCount;
  protected boolean reconstructingFlag;
//	protected int boardRepCount;

  private int startPlayer; // player who started the current game
  protected int repeatCountForDraw; // times a position must repeat for draw
  protected int maxStagnationMoves; // number of moves without progress for draw
  protected int boardImageOffset;
  protected int boardImages[]; // board images array
  protected int boardImagesStored; // number of images in array
  protected int maxBoardImages; // maximum capacity of board images array
  protected int boardImageLength; // length of each board image, in ints

  public abstract String sideName(int side);

  // Generic rules methods:

  public Rules() {
    scores = new int[3];
    controllers = new int[2];
    legalMoves = new Move[maxMoves()];

    // Don't allocate a bunch of empty ones!  We can't instantiate the base class
    // anyways, it's abstract.
    /*
      for (int i = 0; i<maxMoves(); i++)
       legalMoves[i] = new Move();
     */
    controllers[0] = 1;
    controllers[1] = 0;

    includeImages = false;
    fGamePaused = true;
  }

  public abstract int maxMoves();

  public abstract Rules makeNew();

  public boolean redoPossible() {
    return (moveNumber < history.getMovesStored());
  }

  public boolean isGamePaused() {
    return fGamePaused;
  }

  public void setGamePaused(boolean f) {
    fGamePaused = f;
  }

  public void clearRedo() {
    history.clearRedo(moveNumber);
  }

  public boolean undoPossible() {
    return (moveNumber > 0);
  }

  public int getMoveNumber() {
    return moveNumber;
  }

  public int legalMoveCount() {
    return legalMoveCount;
  }

  public Move legalMove(int nIndex) {
    return legalMoves[nIndex];
  }

  public abstract void prepareHistory();

  public abstract void printBoard();

  // copy rules image to another

  public abstract void copyTo(Rules dest, boolean copyImages);

  protected void copyBase(Rules dest, boolean copyImages) {
    dest.gameState = gameState;
    dest.plrTurn = plrTurn;
    dest.stagnateCount = stagnateCount;
    dest.moveNumber = moveNumber;

    if (includeImages) {
      dest.startPlayer = startPlayer;
      dest.boardImageOffset = boardImageOffset;
      dest.boardImagesStored = boardImagesStored;
      dest.boardImageLength = boardImageLength;

      if (copyImages) {
        if (!dest.includeImages) {
          dest.prepareImageBuffer(boardImageLength, maxStagnationMoves,
                                  repeatCountForDraw);
        }
        for (int i = boardImagesStored * boardImageLength - 1; i >= 0; i--) {
          dest.boardImages[i] = boardImages[i];
        }
      }
    }

    if (history != null && dest.history != null) {
      history.copyTo(dest.history);
    }

    for (int i = 0; i < 2; i++) {
      dest.scores[i] = scores[i];
      dest.controllers[i] = controllers[i];
    }
    dest.legalMoveCount = legalMoveCount;
    for (int i = 0; i < legalMoveCount; i++) {
      dest.legalMoves[i] = legalMoves[i].makeCopy();
    }
  }

  public boolean humanController(int side) {
    return controllers[side] != 0;
  }

  /*
     public synchronized boolean humanTurnSync() {
        return humanTurn();
     }
   */
  public boolean humanTurn() {
    return (state() == STATE_PLAYING && humanController(turn()));
  }

  public boolean computerTurn() {
    return (state() == STATE_PLAYING && !humanController(turn()));
  }

  public void setControl(int side, boolean human) {
    controllers[side] = human ? 1 : 0;
  }

  public int turn() {
    return plrTurn;
  }

  public int state() {
    return gameState;
  }

  public int score(int player) {
    return scores[player];
  }

  public void setTurn(int turn) {
    plrTurn = turn;
  }

  //	Prepare for a new game.
  public void resetGame(int nStartPlayer) {
    gameState = STATE_PLAYING;
    plrTurn = nStartPlayer;
    moveNumber = 0;

    if (!reconstructingFlag) {
      history.resetGame();
    }

    startPlayer = nStartPlayer;
    if (includeImages) { // This is new.
      boardImagesStored = 0;
      boardImageOffset = 0;
    }
    stagnateCount = 0;
//		boardRepCount = 0;
    resetBoard();

    buildMoveList();
  }

  // Prepare for redoing a move by determining the legal move index
  // associated with the next move in the history buffer.
  public int getRedoMove() {
    Move m = history.getMove(moveNumber);
    return m.getIndex();
    /*
     int i;

     for (i=0; ; i++)
      if (m.equals(legalMoves[i])) break;
     return i;
     */
  }

  public Move getUndoMove() {
    return history.getMove(moveNumber - 1);
  }

  public abstract void makeMove(int moveIndex, boolean flushBufferFlag);

  public abstract void unMove();

  protected abstract void buildMoveList();

  protected abstract void resetBoard();

  // Board image functions:

  // Remove a board image from the buffer.
  // Returns true if the buffer is now empty.
  protected void removeBoard() {
    //db.a(includeImages,"Rules: removeBoard called with no images included");
//		db.pr("removeBoard, moveNumber="+moveNumber+", imagesStored="+boardImagesStored);

    //db.a(boardImagesStored > 0, "removeBoard() called with no images stored");
    boardImagesStored--;
    if (boardImagesStored == 0 && moveNumber > 0) {
//			db.pr(" reconstructing up to this move");

      int saveMoveNumber = moveNumber;
      reconstructingFlag = true;
      resetGame(startPlayer);
//			db.pr("reset the game");
      moveNumber = 0;
      while (moveNumber != saveMoveNumber) {
//				db.pr(" redoing move "+moveNumber);
        int i = getRedoMove();
//				db.pr("  move index is "+i);
        makeMove(i, true);
      }
      reconstructingFlag = false;
    }
  }

  protected void storeInBoardImage(int value) {
    //db.a(includeImages,"Rules storeInBoardImage: with no images included");
    //db.a(!(boardImageOffset == 0 && boardImagesStored >= maxBoardImages),"Rules: out of board images");

    boardImages[boardImagesStored * boardImageLength +
        boardImageOffset] = value;
    boardImageOffset++;
    if (boardImageOffset == boardImageLength) {
      boardImagesStored++;
      boardImageOffset = 0;
//			db.pr("Rules: now stored "+boardImagesStored+" board images");
    }
  }

  // Check for a draw by examining the board image that was just added.
  // Returns Rules.STATE_xxx
  protected int checkForDraw() {
    //db.a(includeImages,"Rules checkForDraw: with no images included");

    //db.a(boardImagesStored > 0, "Rules checkForDraw: no board images stored");

//		db.pr("Rules: stag count is "+stagnateCount);

    // Check for stagnation.
    if (stagnateCount == maxStagnationMoves * 2 - 1) {
      return STATE_DRAWNSTAG;
    }

    int currBoard = (boardImagesStored - 1) * boardImageLength;

    // Compare this board image with those that came before to see if it
    // repeats.

    int repCount = 0;

    int compBoard = currBoard;
    while (true) {
      // examine only those positions for the current player.
      compBoard -= boardImageLength * 2;

      if (compBoard < 0) {
        break;
      }

      // Compare last int first, since it contains the checksum.
      int i;
      for (i = boardImageLength - 1; i >= 0; i--) {
        if (boardImages[compBoard + i] != boardImages[currBoard + i]) {
          break;
        }
      }

      if (i >= 0) {
        continue;
      }

      repCount++;
      if (repCount == repeatCountForDraw) {
        return STATE_DRAWNREP;
      }
    }

//		boardRepCount = repCount;

    if (
        maxMovesPerGame != 0
        && moveNumber == maxMovesPerGame
        ) {
      return STATE_DRAWNMAXMOVES;
    }

    return STATE_PLAYING;
  }

  protected void clearImages() {
    //db.a(includeImages,"Rules: clearImages called with no images included");
    boardImagesStored = 0;
  }

  protected void prepareImageBuffer(int l, int s, int r) {
    //db.a(!includeImages,"Rules: image buffer already prepared");
    includeImages = true;

    boardImageLength = l;

    maxStagnationMoves = s;
    repeatCountForDraw = r;

//		db.pr("preparing image buffer , s="+s+", r="+r);

    // We need space for one image before stagnation moves started, plus
    // n moves for the stagnating player plus (n-1) moves for his opponent; thus
    // 2n total.
    // But we also need to add a board image for each ply of the brain's search,
    // so add MAX_PLY to this value.
    maxBoardImages = (maxStagnationMoves * 2) + Search.MAX_PLY;

    boardImages = new int[boardImageLength * maxBoardImages];
  }

}
