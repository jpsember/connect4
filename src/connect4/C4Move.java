package connect4;
import treegame.*;
public class C4Move extends Move {
	private int cell;		//	lower 4 bits: column; upper 4 bits: row

 	public C4Move(C4Move src) {
		src.copyTo(this);
	}

	public Move makeCopy() {
		return new C4Move(this);
	}

   public String toString() {
    return "X:"+(cell & 0xf)+"Y:"+(cell>>4);
   }

	public boolean equals(C4Move m) {
		return (super.equals(m) && cell == m.cell);
	}

	// Determine a value for this move to presort it by during the search algorithm.
	public int preSortValue() {
		return row();
	}

	public C4Move() {
	}

	public C4Move(int column, int row) {
		cell = column | (row << 4);
	}

	public void copyTo(C4Move dest) {
		super.copyTo(dest);
		dest.cell = cell;
	}

	public int column() {
		return cell & 0x0f;
	}

	public int row() {
		return cell >> 4;
	}

	public int cell() {
		return cell;
	}
}
