package connect4;
import treegame.*;

public class C4Rules extends Rules {

  public final static int MAX_BOARD_DIMENSION = 8;
  public final static int WINNING_RUN = 4;

  private final static int COLUMNS = 7;
  private final static int MAX_LEGAL_MOVES = COLUMNS;
  private final static int ROWS = 7;
  private final int MAX_WINNING_CELLS = (WINNING_RUN - 1) * 7 + 1;
  private int winningCells[];
  private int winningCellTotal;
  private byte boardCells[];
  private int columnHeights[];

  public Rules makeNew() {
    return new C4Rules();
  }

  public int maxMoves() {
    return MAX_LEGAL_MOVES;
  }

  public C4Rules() {
    super();
    maxMovesPerGame = COLUMNS * ROWS;
    boardCells = new byte[COLUMNS * ROWS];
    winningCells = new int[MAX_WINNING_CELLS];
    columnHeights = new int[COLUMNS];
  }

  // copy rules image to another
  public void copyTo(Rules destParm, boolean includeImages) {
    C4Rules dest = (C4Rules)destParm;
    copyBase(dest, includeImages);

    dest.winningCellTotal = winningCellTotal;
    for (int c = 0; c<COLUMNS; c++) {
      dest.columnHeights[c] = columnHeights[c];
      for (int r = 0; r<ROWS; r++)
        dest.setCell(c,r,readCell(c,r));
    }
  }

  public String sideName(int side) {
    return (side == 0 ? "Red" : "Black");
  }

  protected void resetBoard() {
    winningCellTotal = 0;
    for (int nX=0; nX<COLUMNS; nX++) {
      columnHeights[nX] = 0;
      for (int nY=0; nY<ROWS; nY++)
        setCell(nX, nY, 0);
    }

  }

  public void prepareHistory() {
    history = new History(maxMovesPerGame);
  }

   public synchronized boolean humanTurnSync() {
      return humanTurn();
   }

  // Make a move, update the game state if necessary, and
  // build a new legal move list.
  public void makeMove(int moveIndex, boolean flushBufferFlag) {
    C4Move nMove = (C4Move)legalMoves[moveIndex];
    int nColumn = nMove.column();
    int nRow = nMove.row();

    columnHeights[nColumn]++;

    setCell(nColumn, nRow, turn() + 1);
    plrTurn ^= (0^1);

    if (flushBufferFlag) {}

    history.addMove(moveNumber, nMove);
    moveNumber++;

    //	check if game is over by seeing if piece just placed is part of
    //	four in a row.

    if (checkFourInRow(nColumn, nRow)) {
      legalMoveCount = 0;
      gameState = STATE_WON + (plrTurn ^ (0^1));
      scores[gameState - STATE_WON]++;

    } else {
      // Construct legal moves.
      buildMoveList();

      // check if draw.

      if (legalMoveCount == 0) {
        gameState = STATE_DRAWN;
        scores[2]++;
      }
    }
  }

  // Undo a move.
  public void unMove() {
    {
      if (gameState != STATE_PLAYING) {
        if (gameState >= STATE_WON)
          scores[gameState - STATE_WON]--;
        gameState = STATE_PLAYING;
        winningCellTotal = 0;
      }

      C4Move m = (C4Move)history.getMove(moveNumber - 1);

      int nColumn = m.column();
      int nRow = m.row();

      setCell(nColumn,nRow,0);
      columnHeights[nColumn]--;
      plrTurn ^= (0^1);
      moveNumber--;

      buildMoveList();
    }
  }

  public void printBoard() {
    {
      System.out.println("Status="+gameState+" Turn="+plrTurn+" Winners="+winningCellTotal+" #moves="+legalMoveCount);
      int r,c;
      for (r=ROWS-1; r>=0; r--) {
        for (c=0; c<COLUMNS; c++) {
          final String strs[] = {".","X","O"};
          System.out.print(strs[readCell(c,r)]);
        }
        System.out.print("\n");
      }
      for (c=0; c<COLUMNS; c++) {
        System.out.print(columnHeights[c]);
      }
      System.out.print("\n");
    }
  }

  // Construct a list of legal moves
  protected void buildMoveList() {
    legalMoveCount = 0;
    int c = (COLUMNS - 1) / 2;
    int cAdd = 0;
    for (int i=0; i<COLUMNS + 1; i++) {

      if (c >= 0 && c < COLUMNS) {
        int row = columnHeights[c];
        if (row != ROWS)  {
          legalMoves[legalMoveCount++] = new C4Move(c, row);
        }
      }
      cAdd++;
      c += ((i & 1) == 0) ? cAdd : -cAdd;
    }
  }

  // Determine if a cell is part of a winner.
  // If so, fills in winningCells[] array with cell numbers.
  private boolean checkFourInRow(int nX, int nY)
  {
    final int nDirections = 8;

    int nSum[] = new int [nDirections];

    int nDir = 0;
    int nTestCell = readCell(nX,nY);
    if (nTestCell == 0) return false;

    do {
      nSum[nDir] = 0;
      int x = nX;
      int y = nY;
      final int nMovesX[] = { 0,1,1,1,0,-1,-1,-1};
      final int nMovesY[] = { 1,1,0,-1,-1,-1,0,1};

      int nXM = nMovesX[nDir];
      int nYM = nMovesY[nDir];

      int nRepeat = 0;
      while (true) {
        x += nXM;
        y += nYM;

        if (!withinBoard(x,y)) break;
        if (readCell(x,y) != nTestCell) break;

        nRepeat++;
      }
      nSum[nDir] = nRepeat;

      if (nDir >= nDirections / 2) {
        int nOppCount = nSum[nDir - nDirections/2];
        int nThisDir = nSum[nDir];
        int nTotal = nOppCount + nThisDir + 1;
        if (nTotal >= WINNING_RUN) {

          // Add every square to the winners list.  Avoid adding the original square
          // more than once.

          x = nX - nOppCount * nXM;
          y = nY - nOppCount * nYM;

          int nOrigWinners = winningCellTotal;

          while (nTotal-- > 0) {
            if (!(x == nX && y == nY && nOrigWinners > 0))
              winningCells[winningCellTotal++] = x | (y << 4);
            x += nXM;
            y += nYM;
          }
        }
      }
    } while (++nDir < nDirections);

    return (winningCellTotal > 0);
  }

  public int columns() {
    return COLUMNS;
  }

  public int rows() {
    return ROWS;
  }

  // Determine cell winner #index.  If no such winner, returns -1.
  public int getWinningCell(int index) {
    return (index < winningCellTotal) ? winningCells[index] : -1;
  }

  public boolean cellIsWinner(int x, int y) {

    int nTest = x | (y << 4);

    int i;
    for (i=0; i<winningCellTotal; i++)
      if (winningCells[i] == nTest) return true;

    return false;
  }

  public int columnHeight(int column) {
    return columnHeights[column];
  }

  public int readCell(int nColumn, int nRow) {
    return boardCells[nRow * COLUMNS + nColumn];
  }

  // Determine if the square indexed by nX and nY exists.
  public boolean withinBoard(int nX, int nY) {
    return (nX >= 0 && nX < COLUMNS && nY >= 0 && nY < ROWS);
  }

  public void setCell(int nColumn, int nRow, int nContents) {
    {
      boardCells[nRow * COLUMNS + nColumn] = (byte)nContents;
    }
  }

}
