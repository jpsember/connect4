package treegame;
// Evaluated move class.
// This is used to manipulate moves that the brain is thinking about.

public class EvalMove {
	private int index;		// the index of the move; -1 if undefined
	private int score;		// the evaluated score

	// Determine the # moves to victory.  Returns -1 if unknown.
	public int movesToVictory() {
		int moves = -1;
		int score = Math.abs(getScore());
		if (score > Eval.WIN_SCORE)
			moves = Search.MAX_PLY - (score - Eval.WIN_SCORE);
		return moves;
	}

	public String toString() {
		String out = new String();
		out += "[Ind:" + index + " Score:" + score + "]";
		return out;
	}

	public int getIndex() {
		return index;
	}
	public int getScore() {
		return score;
	}

	public void setIndex(int i) {
		index = i;
	}

	public void setScore(int s) {
		score = s;
	}

	public EvalMove(int indexParm, int scoreParm) {
		setIndex(indexParm);
		setScore(scoreParm);
	}

   public void clear() {
      setIndex(-1);
   }

	public EvalMove() {
      clear();
	}

	public static EvalMove[] createBuffer(int size) {
		EvalMove array[] = new EvalMove[size];

		for (int i = 0; i<size; i++)
			array[i] = new EvalMove();

		return array;
	}

	public static void sortArray(EvalMove[] array, int start, int count) {
		for (int i=1; i<count; i++) {
			for (int j=0; j<i; j++) {
				if (array[i+start].getScore() > array[j+start].getScore())
					array[i+start].swapWith(array[j+start]);
			}
		}
	}

	public void copyTo(EvalMove dest) {
		dest.setIndex(index);
		dest.setScore(score);
	}

	public void swapWith(EvalMove dest) {
		int temp = dest.index;
		dest.index = index;
		index = temp;

		temp = dest.score;
		dest.score = score;
		score = temp;
	}
}
