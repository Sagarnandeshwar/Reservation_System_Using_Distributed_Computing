import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OThreadPoolSwitchTest {

	protected static class NumberSpammer implements Runnable {
		public int number;
		public int count = 0;

		NumberSpammer(int number) {
			this.number = number;
		}

		@Override
		public void run() {
			while (count < 100) {
				System.out.println(number);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static void main(String[] args) {
		ExecutorService exec = Executors.newFixedThreadPool(3);
		exec.submit(new NumberSpammer(1));
		exec.submit(new NumberSpammer(2));
		exec.submit(new NumberSpammer(3));
		exec.submit(new NumberSpammer(4));
		exec.submit(new NumberSpammer(5));
	}

}
