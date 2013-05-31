package connect4;
import treegame.*;
import java.awt.*;
import java.awt.event.*;
import mytools.*;

public class BrnPnl extends FancyPanel implements ItemListener {

   /** search depth */
   public static final int OUT_SEARCHDEPTH = 0;
   /** search algorithm to use (0...n) */
   public static final int OUT_ALGORITHM = 1;
   /** non-zero if thinking ahead should occur */
   public static final int OUT_THINKAHEAD = 2;
   /** evaluation options */
   public static final int OUT_EVALOPTIONS = 3;

   private static final int REG_OUT_LEN = 4;

  BrnPnl(Rules r) {

    super("BRAIN",STYLE_PLAIN,new Color(200,200,180));

      rules = r;

      initializeOutputRegister();

      initLayoutMgr();

      smarts = new Choice();
      final String cvals[] = {"WEAK","OK","STRONG","SUPER"};
      for (int i=0; i<cvals.length; i++)
         smarts.add(cvals[i]);
      smarts.addItemListener(this);
      smarts.select(INITIAL_INTELLIGENCE);
      addOurLComponent("Intelligence:",smarts);

    depthValue = new TextField(Integer.toString(
         depthValues[INITIAL_INTELLIGENCE]), 1);
    depthValue.setEditable(false);
    addOurLComponent("Search Depth:",depthValue);

      final String algs[] = {"MINIMAX","ALPHA/BETA"};
    alg = new Choice();
      for (int i=0; i<algs.length; i++)
         alg.add(algs[i]);
      alg.select(dataOut.getArgLock(OUT_ALGORITHM));
      alg.addItemListener(this);
    addOurLComponent("Algorithm:",alg);

    thinkAheadBox = new Checkbox("", true);
    thinkAheadBox.setState(dataOut.getArgLock(OUT_THINKAHEAD) != 0);
    thinkAheadBox.addItemListener(this);
    addOurLComponent("Think Ahead:",thinkAheadBox);

    evaluation = new TextField(10);
    addOurLComponent("Board Value:",evaluation);

    nodeInfo1 = new TextField(7);
    addOurLComponent("Traversals:",nodeInfo1);
  }

   public ThreadCommand getReg(int n) {
      ThreadCommand tc = null;
      switch (n) {
      case 0:
         tc = dataIn;
         break;
      case 1:
         tc = dataOut;
         break;
      }
      return tc;
   }

   public void testRepaint() {
      synchronized(dataIn) {
         if (dataIn.testSignal()) {
            int side = dataIn.getArg(IN_SIDEAHEAD);
            if (side < 0)
               evaluation.setText("");
            else {
               int n = dataIn.getArg(IN_ADVANTAGE);
               if (n == 0)
                  evaluation.setText("EVEN");
               else
                  evaluation.setText(rules.sideName(side) + " +" + n);
            }

            int trav = dataIn.getArg(IN_NODECOUNT);
            int eval = dataIn.getArg(IN_EVALCOUNT);
            if ((trav | eval) == 0)
               nodeInfo1.setText("");
            else
               nodeInfo1.setText(Integer.toString(trav));
         }
      }
  }

   // ===========================================
   // ItemListener interface
   // ===========================================
   public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() != ItemEvent.SELECTED) return;

      int i = smarts.getSelectedIndex();
    if (e.getItemSelectable() == smarts) {
         depthValue.setText(Integer.toString(depthValues[i]));
         thinkAheadBox.setEnabled(i > 0);
    }
      synchronized(dataOut) {
         dataOut.setSignal();
//         dataOut.setArgLock(OUT_MODIFIED, 1);
         dataOut.setArgLock(OUT_SEARCHDEPTH, depthValues[i]);
         dataOut.setArgLock(OUT_ALGORITHM, alg.getSelectedIndex() );
         dataOut.setArgLock(OUT_THINKAHEAD, thinkAheadBox.getState() ? 1 : 0);
      }
  }

   // ===========================================

   private void initializeOutputRegister() {
         dataOut.setSignal();
         dataOut.setArg(OUT_SEARCHDEPTH,
               depthValues[INITIAL_INTELLIGENCE]);
         dataOut.setArg(OUT_ALGORITHM, 1);
         dataOut.setArg(OUT_THINKAHEAD, 1);
   }

  private void addOurLComponent(String text, Component c) {
    addLabel(text);
    addOurComponent(c);
  }

  private void addLabel(String text) {
    addOurComponent(new Label(text));
  }

  private void addOurComponent(Component c) {
    FancyPanel.setGBC(gc, nextX, nextY, 1,1, 0, 0);
    gc.anchor = (nextX == 0) ? GridBagConstraints.EAST : GridBagConstraints.WEST;
    gb.setConstraints(c, gc);
    add(c);
    nextX++;
    if (nextX == 2) {
      nextX = 0;
      nextY++;
    }
  }

   private void initLayoutMgr() {
    gb = new GridBagLayout();
    setLayout(gb);

    gc = new GridBagConstraints();
    gc.fill = GridBagConstraints.NONE;
    gc.insets = new Insets(1,2,1,2);
   }

   private Rules rules;

  private ThreadCommand dataIn = new ThreadCommand(REG_IN_LEN);
  private ThreadCommand dataOut = new ThreadCommand(REG_OUT_LEN);
//  private ThreadCommand dataOutDisp = new ThreadCommand(REG_OUT_LEN);

  private TextField evaluation;
  private Choice smarts;
  private Choice alg;
  private TextField nodeInfo1; //, nodeInfo2;

  private TextField depthValue;
  private static final int depthValues[] = {1,2,3,4}; //{2,4,6,8};

  private GridBagLayout gb;
  private GridBagConstraints gc;
  private int nextX = 0, nextY = 0;
  private Checkbox thinkAheadBox;

  /**
    * Input register for evaluation and search.
    * Signal : set true if registers have been modified
    *  and display needs to be updated to reflect this
    * IN_SIDEAHEAD side with advantage (-1 if undefined)
    * IN_ADVANTAGE amount of advantage (0: even)
    * IN_NODECOUNT number of nodes traversed
    * IN_EVALCOUNT number of position evaluations performed
    */
//   public final static int IN_MODIFIED = 0;
   public final static int IN_SIDEAHEAD = 0;
   public final static int IN_ADVANTAGE = 1;
   public final static int IN_NODECOUNT = 2;
   public final static int IN_EVALCOUNT = 3;
   private static final int REG_IN_LEN = 4;

   private final static int INITIAL_INTELLIGENCE = 1;
}
