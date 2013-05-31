package treegame;

public abstract class Move {
  // stagnationChange is xor'd with the previous stagnation count to get
  // the new value.
  private int stagnationChange;

  private int index; // position in legal moves list (0:first ... n-1)

  // Constructor.

  public Move() {
    setIndex( -1); // initialize it to undefined.
  };

  public Move(int index) {
    setIndex(index);
    setStagnation(0);
  };

  public Move(Move src) {
    src.copyTo(this);
  }

  public void setStagnation(int n) {
    stagnationChange = n;
  }

  public int getStagnation() {
    return stagnationChange;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  // Determine a value for this move to presort it by during the search algorithm.
  public abstract int preSortValue();

  public abstract Move makeCopy();

  public void copyTo(Move dest) {
    dest.index = index;
    dest.stagnationChange = stagnationChange;
  }

  public boolean equals(Move m) {
    return (stagnationChange == m.stagnationChange);
  }

  public abstract String toString();
}
