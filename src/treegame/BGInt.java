// Board game interface
package treegame;

public interface BGInt {
	public void processMove(boolean undoFlag);
	public void processEvalMove(EvalMove e);
	public void testRepaint();
	public void processNewGame();
};
