package connect4;
import treegame.*;
import mytools.*;
import java.awt.*;
import java.awt.event.*;

public class CtrlPnl extends FancyPanel implements ItemListener {

	/**
    * Output register; indicates new control has been chosen.
         signal : true if control has been chosen
			[0] player being modified
			[1] controller value for player
    */
   public static final int REG_OUT_LEN = 2;

	//private Connect4 parent;
	private Rules rules;
	private ThreadCommand cmdOut = new ThreadCommand(REG_OUT_LEN);

   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 1:
         tc = cmdOut;
         break;
      }
      return tc;
   }

//	private int drawnTurn, desiredTurn;
	private Choice choices[];
	private final String ctrlNames[] = {
		"COMPUTER",
		"HUMAN"
	};

	CtrlPnl(Rules r) {
		super("CONTROLS",STYLE_PLAIN, new Color(200,200,180));

		rules = r;

		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();

		gc.fill = GridBagConstraints.NONE;
		gc.insets = new Insets(2,3,2,3);

		setLayout(gb);

		// Add some choices and labels for the two players.

		choices = new Choice[2];

		for (int i=0; i<2; i++) {
			{
				Label l = new Label(rules.sideName(i) + ":", Label.RIGHT);
				FancyPanel.setGBC(gc, 0,i, 1,1, 4, 5);
				gc.anchor = GridBagConstraints.EAST;
				gb.setConstraints(l,gc);
				add(l);
			}

			{
				Choice l = new Choice();
				for (int j=0; j<2; j++)
					l.add(ctrlNames[j]);
				l.addItemListener(this);
				l.select(rules.humanController(i) ? 1 : 0);

				FancyPanel.setGBC(gc, 1,i, 1,1, 6, 0);
				gc.anchor = GridBagConstraints.CENTER;
				gb.setConstraints(l,gc);
				add(l);
				choices[i] = l;
			}
		}

	}

	// ActionListener interface:

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() != ItemEvent.SELECTED) return;

		// Determine which choice menu was selected from.

		for (int i=0; ; i++) {

			if (e.getItemSelectable() != choices[i]) continue;

			int newCtrl = choices[i].getSelectedIndex();
			boolean humanFlag = (newCtrl != 0);

			synchronized(cmdOut) {
				// If old cmd wasn't processed, too bad!
            cmdOut.setSignal();
				cmdOut.setArg(0, i);
				cmdOut.setArg(1, humanFlag ? 1 : 0);
			}
			break;
		}
	}
}
