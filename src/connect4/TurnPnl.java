package connect4;
import treegame.*;
import java.awt.*;
import mytools.*;

public class TurnPnl extends FancyPanel {

	TurnPnl (Rules r, Board b) {
		super("TURN",STYLE_PLAIN,new Color(200,200,180));
      makeDoubleBuffered();
		board = b;
		rules = r;
		testRepaint();
	}

	public void paintInterior(Graphics g, boolean fBufferValid) {

		drawnTurn = desiredTurn;
      clearRect(g);
		if (drawnTurn >= 0)
   		board.plotPieceRect(g, 1 + drawnTurn, g.getClipBounds());
	}

	public void testRepaint() {
		synchronized(rules) {
			desiredTurn = (rules.state() == Rules.STATE_PLAYING) ?
            rules.turn() : -1;
		}
		if (drawnTurn != desiredTurn)
			repaint();
	}
	private Rules rules;
	private Board board;
	private int drawnTurn, desiredTurn;
}
