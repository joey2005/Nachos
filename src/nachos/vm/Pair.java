package nachos.vm;

public class Pair {
	public int first, second;
	
	public Pair() {
		first = -1;
		second = -1;
	}
	
	public Pair(int first, int second) {
		this.first = first;
		this.second = second;
	}
	
	@Override
	public int hashCode() {
		return first * 111111 + second;
	}
	
	@Override
	public boolean equals(Object o) {
		Pair a = (Pair)o;
		return first == a.first && second == a.second; 
	}
}
