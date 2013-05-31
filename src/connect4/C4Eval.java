package connect4;
import treegame.*;

public class C4Eval extends Eval {
	public static final int WIN_SCORE = 10000;

	private int evalMatrix[];
	private int evalMatrixSize;
	private int evalCellFlags[];
	private int evalNeighborsFwd[];
	private int evalNeighborsBwd[];

	// Constructor
	C4Eval(C4Rules rules) {
		evalMatrixSize = C4Rules.MAX_BOARD_DIMENSION * rules.rows();
		evalMatrix = new int[evalMatrixSize];

		evalCellFlags = new int[C4Rules.MAX_BOARD_DIMENSION];
		evalNeighborsFwd = new int[C4Rules.MAX_BOARD_DIMENSION];
		evalNeighborsBwd = new int[C4Rules.MAX_BOARD_DIMENSION];
	}

	// Evaluate the board position relative to the player whose turn it is.
	// Returns a value for the board position; positive is good for the current player.
	public int process(Rules rulesParm, int evalOptions, boolean saveResults) {
      C4Rules rules = (C4Rules)rulesParm;

		int nTotalScore = 0;

		int plr;
		for (plr = 0; plr < 2; plr++) {
			int nScore = 0;

			int ourPiece = (plr ^ rules.turn()) + 1;
			int enemyPiece = ourPiece ^ (1^2);

			// Clear the evaluation matrix to zero, indicating that no
			// pieces have been evaluated in a particular direction yet.

			int i;
			for (i=0; i<evalMatrixSize; i++)
				evalMatrix[i] = 0;

			int row, col;
			int matBase = 0;
			for (row=0; row < rules.rows(); row++, matBase += rules.columns()) {
				int matIndex = matBase;
				for (col=0; col < rules.columns(); col++, matIndex++) {
					int nPiece = rules.readCell(col,row);
					if (nPiece != ourPiece) continue;

					// The nature of the scan is that we need examine only
					// four directions:  up+left, up, up+right, and right.

					// Scan each of the three directions, ignoring those that
					// have already been included in a previous scan.

					int dir, dirFlag;
					for (dir=0,dirFlag=1; dir<4; dir++, dirFlag <<= 1) {
						final int xMoves[] = {-1,0,1,1};
						final int yMoves[] = {1,1,1,0};

						final int EVALFLAG_FILLED = 0x0001; // if our piece is at the cell

						int xMove = xMoves[dir];
						int yMove = yMoves[dir];

						// Has this cell already been included in a previous scan
						// in this direction?
						if ((evalMatrix[matIndex] & dirFlag) != 0) continue;	// yes.

						// Scan backward along the current direction to the last
						// cell not occupied by an enemy.

						int nx = col;
						int ny = row;
						do {
						 	nx -= xMove;
							ny -= yMove;
						} while (
								rules.withinBoard(nx,ny)
							&& rules.readCell(nx,ny) != enemyPiece
						);
						nx += xMove;
						ny += yMove;

						// Scan until we hit a wall or an enemy, storing flags
						// 0 if cell is empty or 1 if it is our piece.

						int scanLength = 0;
						while (true) {
							int nPieceFwd = rules.readCell(nx,ny);
							if (nPieceFwd == enemyPiece) break;

							int nFlag = 0;
							if (nPieceFwd > 0)
								nFlag = EVALFLAG_FILLED;

							evalCellFlags[scanLength++] = nFlag;

							// Mark this cell as having been scanned in this direction
							// already.

							evalMatrix[nx | (ny << 3)] |= dirFlag;

							nx += xMove;
							ny += yMove;
							if (!rules.withinBoard(nx,ny)) break;
						}

						// If it was a run of less than the goal,
						// ignore this direction.

						if (scanLength < C4Rules.WINNING_RUN)
							continue;

						int j,k;
						for (k=j=0; k<scanLength; k++) {
							evalNeighborsFwd[k] = j;
							j++;
							if ((evalCellFlags[k] & EVALFLAG_FILLED) == 0)
								j = 0;
						}

						for (k=scanLength-1,j=0; k>=0; k--) {
							evalNeighborsBwd[k] = j;
							j++;
							if ((evalCellFlags[k] & EVALFLAG_FILLED) == 0)
								j = 0;
						}

						int scanScore = 0;
						for (k=0; k<scanLength; k++) {
							final int evalScores[] = { 0, 1, 3, 10, 20, 20, 20, 20};

							int nbTotal = evalNeighborsFwd[k] + evalNeighborsBwd[k];
							int cellScore = evalScores[nbTotal];

							scanScore += cellScore;
						}

						nScore += scanScore;
					}
				}
			}

			if (plr == 0)
				nTotalScore = nScore;
			else
				nTotalScore -= nScore;

		}
		return nTotalScore;
	}
}
