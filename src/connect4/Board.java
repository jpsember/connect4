package connect4;
import treegame.*;

// Board.java
// Displays the game board for the Connect4 game.

import java.awt.*;
import java.awt.event.*;
import mytools.*;

public class Board extends FancyPanel
      implements MouseListener, MouseMotionListener {

   /** 0..n-1 if human has selected a move */
   public static final int CMD_OUT_MOVE = 0;
   /** Length of OUTPUT ThreadCommand */
   public static final int CMD_OUT_LEN = 1;

	// Constructor
	public Board(C4Rules r) {

		super(null, STYLE_SUNKEN, new Color(200,200,180));
      makeDoubleBuffered();

      rules = r;

      constructColors();
      titleFont = new Font("TimesRoman", Font.BOLD, 24);

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	// Plot a piece centered within a bounding rectangle
	public void plotPieceRect(Graphics g, int nPiece, Rectangle bounds)
	{
		if (nPiece == 0)
			g.setColor(colors[C_BGND]);
		else
			g.setColor(pieceColors[(nPiece - 1) * 2]);

		final int PIECE_HL_RADIUS = 3;

		int startX = bounds.x + (bounds.width - PIECE_DIAMETER) / 2;
		int startY = bounds.y + (bounds.height - PIECE_DIAMETER) / 2;

		g.fillOval(startX, startY, PIECE_DIAMETER, PIECE_DIAMETER);

		// If it's an actual piece, plot a highlight in the piece.
		if (nPiece != 0) {
			g.setColor(pieceColors[(nPiece - 1) * 2 + 1]);

			g.drawArc(startX + PIECE_HL_RADIUS, startY + PIECE_HL_RADIUS,
				PIECE_DIAMETER - PIECE_HL_RADIUS*2 - 1, PIECE_DIAMETER - PIECE_HL_RADIUS*2-1, 120, 340-120);
			// We subtract an extra 1 for the width of the pen.
		}
	}

   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 1:
         tc = cmdOut;
         break;
      }
      return tc;
   }

   public void setReg(int n, ThreadCommand tc) {
      switch (n) {
      case 1:
         cmdOut = tc;
         break;
      }
   }

   // =======================================
	// MouseMotionListener interface
   // =======================================
	public void mouseMoved(MouseEvent e) {
	}
	public void mouseDragged(MouseEvent e) {

		if (mousePressCellOrig != 0) {
			// Pressed within bounds of board?
			int cell = ptToSquare(translatePoint(e.getPoint()));
			if (cell + 1 != mousePressCell) {
				cellHRemove(HL_MOUSEPRESS);
				mousePressCell = 0;
				if (cell >= 0) {
					mousePressCell = cell + 1;
					cellHAdd(cell, HL_MOUSEPRESS);
				}
				testRepaint();
			}
		}
	}
   // =======================================

   // =======================================
	// MouseListener interface
   // =======================================
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		// Pressed within bounds of board?
		int cell = ptToSquare(translatePoint(e.getPoint()));
		if (cell < 0) return;	// no.

		// Is it a human's turn?
      if (!rules.humanTurnSync()) return;

		// Save cell number, and put into 'wait for mouseup' mode.

		mousePressCellOrig = mousePressCell = cell + 1;

		// Highlight any other cells that were MOUSEPRESS, and highlight this new one.

		cellHRemove(HL_MOUSEPRESS);
		cellHAdd(cell, HL_MOUSEPRESS);
		testRepaint();
	}
	public void mouseReleased(MouseEvent e) {
		int cell = ptToSquare(translatePoint(e.getPoint()));

		cellHRemove(HL_MOUSEPRESS);
		testRepaint();

		if (
			rules.humanTurnSync()
		 && mousePressCell != 0
		 && cell+1 == mousePressCell
		) {
			int moveIndex = matchCellToMove(cell);
			if (moveIndex >= 0) {
            // Place this move in the output register.
            synchronized(cmdOut) {
               cmdOut.setSignal();
               cmdOut.setArg(CMD_OUT_MOVE, moveIndex);
            }
			}
		}
		mousePressCell = 0;
		mousePressCellOrig = 0;
	}
   // =======================================

   // (Event thread)
	public void updateBrainThinkingMove(int hlMove) {
		if (brainMsgDrawn == hlMove)
			return;

      brainMsgDrawn = hlMove;

		cellHRemove(HL_MOUSEPRESS);

      int iMove = hlMove - 1;
		if (iMove >= 0 && iMove < drawn.legalMoveCount()) {
			C4Move move = (C4Move)drawn.legalMove(iMove);
			cellHAdd(move.cell(), HL_MOUSEPRESS);
		}
		testRepaint();
	}

	// Update board display after a move.
   // (Event thread)
	public void processMove() {
		int lastMoveCell = -1;
      synchronized(rules) {
   		if (rules.undoPossible()) {
   			C4Move m = (C4Move)rules.getUndoMove();
   			lastMoveCell = m.cell();
   		}
      }

		cellHRemove(HL_LASTMOVE);
		if (lastMoveCell >= 0)
			cellHAdd(lastMoveCell, HL_LASTMOVE);

		// Remove any old win vector, and add new if game is over.
		cellHRemove(HL_WINVECTOR);

		// If game is over, highlight the win array.

		for (int i = 0; ; i++) {
			int j = rules.getWinningCell(i);
			if (j < 0) break;
			cellHAdd(j, HL_WINVECTOR);
		}

		testRepaint();
	}

	// Paint the board if necessary.
	public void paintInterior(Graphics g, boolean fBufferValid) {

		if (
			!sizeDefined
		 || !windowSize.equals(getInteriorSize())
		)
			defineSize();

		// Determine the clipping rectangle.  We don't need to update anything that
		// is completely outside this boundary.
		Rectangle clip = g.getClipBounds();

		// If the clip bounds is totally contained within the board, we don't need
		// to update the frame or background graphics.

		Rectangle boardRect = new Rectangle(cellsRect.x, cellsRect.y,
			cellSize.width * rules.columns(), cellSize.height * rules.rows());

		if (!rectContainsRect(boardRect, clip)) {

			g.setColor(colors[C_BGND]);

         MyGraphics.fillRect(g, clip);
//			dirtyFlags = ~0;

			// Plot a title
			g.setColor(new Color(30,125,175));
			g.setFont(titleFont);
			FontMetrics titleMetrics = g.getFontMetrics(titleFont);
			g.drawString("Connect 4", 5, 0 + titleMetrics.getAscent());

			// Plot a frame around the board.
			drawFrame(g);
		}

		if (clip.intersects(boardRect))
			updateCells(g, clip);

//		dirtyFlags = 0;
	}

	private void defineSize() {

		// Determine the size of the interior of the panel.
		windowSize = getInteriorSize();

		cellSize = new Dimension(32,32);
		Dimension size = new Dimension(
			cellSize.width * rules.columns(),
			cellSize.height * rules.rows());

		boardInset = new Point( (windowSize.width - size.width) / 2,
			(windowSize.height - size.height) / 2 + TITLE_PIXELS);

		cellsRect = new Rectangle(boardInset.x, boardInset.y, size.width, size.height);

		sizeDefined = true;
	}

	// Test if current board state disagrees with the last drawn state,
	// and call repaint() if so.
	private void testRepaint() {

		// Compare rules with last drawn state, and refresh cells that have changed.

		{
			// copy current rules to thread-safe location for comparisons.
			C4Rules curr = new C4Rules();
			synchronized(rules) {
				rules.copyTo(curr, false);
			}

			for (int r=0; r<curr.rows(); r++) {
				for (int c=0; c<curr.columns(); c++) {
					if (curr.readCell(c,r) != drawn.readCell(c,r)) {
						repaintCell(c,r);
					}
				}
			}
			curr.copyTo(drawn, false);
		}

		{
			// Sort the desired array into decreasing order of cell index
			// for easy comparing.
			int i,j;
			for (i=0; i<HL_FLAG_MAX; i++) {
				for (j=i+1; j<HL_FLAG_MAX; j++) {
					if ((cellHFlags[i] & 0xff) < (cellHFlags[j] & 0xff)) {
						int temp = cellHFlags[i];
						cellHFlags[i] = cellHFlags[j];
						cellHFlags[j] = temp;
					}
				}
			}
		}

		// Compare the desired with the current.

		{
			int i = 0;
			int j = 0;

			while (true) {
				// if old is higher than new, mark old, increment old index.
				// if new is higher than old, mark new, increment new index.
				// if new == old, mark new if highlighting has changed,
				//		increment old & new indices.

				int iVal = (i < HL_FLAG_MAX) ? cellHFlags[i] : -1;
				int jVal = (j < HL_FLAG_MAX) ? drawnHFlags[j] : -1;

				if (iVal < 0 && jVal < 0) break;

				int iCell = (iVal >= 0) ? iVal & 0xff : 0;
				int jCell = (jVal >= 0) ? jVal & 0xff : 0;

				if ((iVal | jVal) == 0) break;

				int changedCell = -1;

				if (jCell > iCell) {
					changedCell = jCell;
					j++;
				} else if (iCell > jCell) {
					changedCell = iCell;
					i++;
				} else {
					if (iVal != jVal)
						changedCell = iCell;
					i++;
					j++;
				}

				if (changedCell > 0)
					repaintCell(cellX(changedCell-1), cellY(changedCell - 1));

			}

			// Update the list of what cells we've highlighted and drawn.
			for (i=0; i<HL_FLAG_MAX; i++)
				drawnHFlags[i] = cellHFlags[i];

		}

	}

	private Rectangle cellRect(int x, int y) {
		return new Rectangle(
			x * cellSize.width + cellsRect.x,
			(rules.rows() - 1 - y) * cellSize.height + cellsRect.y,
			cellSize.width, cellSize.height);
	}

	private int cellX(int cell) {
		return cell & 0xf;
	}
	private int cellY(int cell) {
		return cell >> 4;
	}

	private int cellIndex(int x, int y) {
		return x | (y << 4);
	}

	// Draw the frame around the board
	private void drawFrame(Graphics g) {

		Dimension pnlSize = getSize();

		int boardWidth = cellSize.width * rules.columns();
		int boardHeight = cellSize.height * rules.rows();

		g.setColor(new Color(120,90,90));
		final int feetHeight = 40;
		int floorStart = boardHeight + feetHeight + cellsRect.y - 10;
		g.fillRect(0, floorStart, pnlSize.width, pnlSize.height - floorStart);

		g.setColor(new Color(40,150,40));

		// Plot a poly for the left frame, then flip it around
		// the center of the board and plot it again.
		int flipped;
		for (flipped = 0; flipped < 2; flipped++) {
			final int coords[] = {
				0,0,
				0,1000,
				-4,1000,
				-8,1000-20,
				-8,10,
				-34,-feetHeight,
				-12,-feetHeight,
				3,-11,
				1000,-11,
				1000,0,
				1000,1000
			};
			Polygon poly = new Polygon();
			int i = 0;

			while (true) {
				int x = coords[i++];
				int y = coords[i++];

				//	if both coordinates are the flag value, we're done.
				if (x == 1000 && y == 1000) break;

				//	convert flag values to x or y extensions
				if (x > 500)
					x = ((boardWidth + 1) >> 1) + (x - 1000);
				if (y > 500)
					y = boardHeight + (y - 1000);

				//	flip around x if required.
				if (flipped > 0)
					x = (cellSize.width * rules.columns()) - x;

				poly.addPoint(x + cellsRect.x, cellsRect.y + (cellSize.height * rules.rows()) - y);
			}
			g.fillPolygon(poly);
		}

	}

	// Update the cells on the board that overlap the clip region.
	private void updateCells(Graphics g, Rectangle clip) {

      synchronized(rules) {
         int x,y;
         for (y=0; y<rules.rows(); y++) {
            for (x=0; x<rules.columns(); x++) {
//               int piece = rules.readCell(x,y);

               // Determine if this piece is in the update region.

               Rectangle r = cellRect(x,y);

               if (r.intersects(clip)) {

                  int cellNum = cellIndex(x,y);
                  int hlFlags = cellHRead(cellNum);

                  // Plot background of cell.
                  g.setColor(colors[C_GRID]);
                  g.fillRect(r.x+1, r.y+1, r.width - 2, r.height - 2);
                  g.setColor(colors[C_GRID2]);
                  g.drawRect(r.x, r.y, r.width - 1, r.height - 1);

                  // If it's one of the winning pieces, mark it.
                  if ((hlFlags & HL_WINVECTOR) != 0) {
                     final int winInset = 2;
                     g.setColor(Color.yellow);
                     g.fillRect(r.x + winInset, r.y + winInset, r.width - winInset*2,
                        r.height - winInset * 2);
                  }
                  plotPieceRect(g, rules.readCell(x,y), r);

                  int hlType = 0;
                  if ((hlFlags & HL_LASTMOVE) != 0)
                     hlType = 1;
                  if ((hlFlags & HL_MOUSEPRESS) != 0)
                     hlType = 2;
                  if (hlType > 0) {

                     g.setColor(colors[C_LASTMOVE + hlType - 1]);
                     int bx = r.x + (r.width - BUTTON_DIAMETER)/2;
                     int by = r.y + (r.height - BUTTON_DIAMETER)/2;
                     g.fillOval(bx,by,BUTTON_DIAMETER, BUTTON_DIAMETER);
                     g.setColor(Color.white);
                     g.drawArc(bx+3-1,by+3-1,BUTTON_DIAMETER-3,BUTTON_DIAMETER-3,
                        10,95);
                  }
               }
            }
         }
      }
	}

	// Determine which square, if any, contains a point
	// Precondition:
	//	x,y contain point on screen
	// Postcondition:
	//	returns square 0...n, or -1 of not in a square
	private int ptToSquare(Point pt) {

		if (!cellsRect.contains(pt))
			return -1;

		int column = (pt.x - cellsRect.x) / cellSize.width;
		int row = rules.rows() - 1 - ((pt.y - cellsRect.y) / cellSize.height);

		return cellIndex(column,row);
	}

	// Determine highlighting for a cell
	private int cellHRead(int cell) {
		for (int i=0; i<HL_FLAG_MAX; i++) {
			if ((cellHFlags[i] & 0xff) != cell + 1) continue;

			return (cellHFlags[i] >> 8);
		}
		return 0;
	}

	// Set highlighting flags for a cell
	// Precondition:
	//	cell = index to add highlighting for
	//	flags = flags to set for cell
	private void cellHAdd(int cell, int flags) {

		// Find this cell in the highlight list, or allocate new one.

		int i, j;
		j = -1;
		for (i=0; i < HL_FLAG_MAX; i++) {
			if (cellHFlags[i] == 0) {
				j = i;
				continue;		// we found an empty one
			}

			if ((cellHFlags[i] & 0xff) == cell + 1) {
				j = i;
				break;
			}
		}

		cellHFlags[j] |= (cell+1) | (flags << 8);
	}

	// Remove all occurrences of specified highlighting flags
	private void cellHRemove(int flags) {
		int i;
		for (i=0; i<HL_FLAG_MAX; i++) {
			int cCell = cellHFlags[i] & 0xff;
			if (cCell == 0) continue;

			int cFlags = cellHFlags[i] >> 8;

			if ((cFlags & flags) != 0) {
				cFlags &= ~flags;
				if (cFlags == 0)
					cCell = 0;
				cellHFlags[i] = cCell | (cFlags << 8);
			}
		}
	}

	private void repaintCell(int x, int y) {
		Rectangle cRect = cellRect(x, y);
		repaint(20, cRect.x, cRect.y, cRect.width, cRect.height);
	}

	// Determine which move, if any, matches a particular cell
	// Returns move 0...n, or -1 if none
	private int matchCellToMove(int cell) {
		int moveNumber = -1;
      synchronized(rules) {
         if (rules.state() == Rules.STATE_PLAYING) {
            for (int i=0; i<rules.legalMoveCount(); i++) {
               C4Move m = (C4Move)rules.legalMove(i);
               if (m.cell() == cell)
                  moveNumber = i;
            }
         }
      }
		return moveNumber;
	}

	public void processNewGame() {
		cellHRemove(HL_LASTMOVE | HL_WINVECTOR | HL_MOUSEPRESS);
		brainMsgDrawn = 0;
		testRepaint();
	}


   private void constructColors() {
		// construct background and board colors.

		{
			final int rgb[] = {
				57,162,239,
				220,220,255,
				180,180,200,
				80,210,120,
				232,232,232,
			};

			colors = createColorTable(rgb);
		}

		// construct colors for the pieces.

		{
			final int rgb[] = {
				// red pieces:
				255,80,80,
				255,200,200,		// highlight
				// black pieces:
				120,120,120,
				170,170,170			// highlight
			}	;
			pieceColors = createColorTable(rgb);
		}
   }

	private static final int PIECE_DIAMETER = 24;
	private static final int BUTTON_DIAMETER = 14;
	private static final int TITLE_PIXELS = 5;
	private int brainMsgDrawn = 0;		// 0, or 1+index of move

 	private ThreadCommand cmdOut = new ThreadCommand(CMD_OUT_LEN);

	private C4Rules rules;

	// 'drawn' will contain an image of the last board we drew, so we
	// can avoid redrawing pieces that have not changed.
	private C4Rules drawn = new C4Rules();

	private Font titleFont;

	private boolean sizeDefined = false;
	private Dimension windowSize;
	private Dimension cellSize;
	private Rectangle cellsRect;

//	private int dirtyFlags;

//	private static final int DIRTY_PIECES	= 0x0002;

	private Point boardInset;

	private static final int C_BGND = 0;
	private static final int C_GRID = 1;
	private static final int C_GRID2 = 2;
	private static final int C_LASTMOVE = 3;
//	private static final int C_TOTAL = 4;

	private Color colors[];
	private Color pieceColors[];

	private int mousePressCell = 0;		// 0: none, else 1+cell index
	private int mousePressCellOrig = 0;

	// Cell highlighting:
	private int[] cellHFlags = new int[HL_FLAG_MAX];				// cell index in lower 8 bits, flags in upper
	private int[] drawnHFlags = new int[HL_FLAG_MAX];				// the flags that were last drawn

	private static final int HL_FLAG_MAX = 30;			// maximum number of highlight flags
	private static final int HL_WINVECTOR	= 0x0001;	// part of a win vector
	private static final int HL_LASTMOVE 	= 0x0002;	// where the last move occurred
	private static final int HL_MOUSEPRESS	= 0x0004;	// tracking a mouse press

}
