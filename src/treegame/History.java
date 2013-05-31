package treegame;

// Move history class
// Stores all the moves made in the game.
// Also stores board images for determining draws:
//	a) by repetition (the same board appearing n times)
//	b) by stagnation (n moves occurring without capture or pawn advancement)
//	c) by length (the maximum move storage capacity reached)

public class History {
  private Move moveList[];
  private int movesStored; // The total moves stored (may be more than
  //  current move number, if undoing / redoing)
  private int maxMovesPerGame;

  // Constructor
  public History(int maxMovesParm) {
    maxMovesPerGame = maxMovesParm;
    // allocate an array to hold the move list
    moveList = new Move[maxMovesPerGame];
  }

  public int getMovesStored() {
    return movesStored;
  }

  public void resetGame() {
    movesStored = 0;
  }

  public Move getMove(int index) {
    //db.a(index >=0 && index < movesStored, "History getMove bad arg "+index+", movesStored= "+movesStored);
    return moveList[index];
  }

  public void clearRedo(int currentMoveNumber) {
    movesStored = currentMoveNumber;
  }

  public void copyTo(History dest) {
    for (int i = 0; i < movesStored; i++) {
      dest.moveList[i] = moveList[i].makeCopy();
    }
    dest.movesStored = movesStored;
  }

  public void addMove(int moveNumber, Move m) {
    //db.a(moveNumber >= 0 && moveNumber < maxMovesPerGame, "History addMove "+moveNumber+" out of range");
    //db.a(moveNumber - 1 < movesStored, "History addMove "+moveNumber+" when movesStored only "+movesStored);

    moveList[moveNumber] = m.makeCopy();
    moveNumber++;

    if (movesStored < moveNumber) {
      movesStored = moveNumber;
    }
  }

}
