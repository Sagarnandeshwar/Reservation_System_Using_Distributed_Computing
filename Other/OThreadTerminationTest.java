/**
 * Test to see if termination of a main thread will result in the termination of
 * children threads.
 */
public class OThreadTerminationTest {

	private static class NumberSpammer extends Thread {
		private int number;
		NumberSpammer(int number) {
			this.number = number;
		}

		@Override
		public void run() {
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("NumberSpammer #" + number + " shutting down.");
			}));


			while (true) {
				System.out.println(number);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public static void main(String[] args) {
		System.out.println("Main thread started.");

		NumberSpammer ns1 = new NumberSpammer(1);
		NumberSpammer ns2 = new NumberSpammer(2);
		NumberSpammer ns3 = new NumberSpammer(3);
		NumberSpammer ns4 = new NumberSpammer(4);
		NumberSpammer ns5 = new NumberSpammer(5);
		ns1.start();
		ns2.start();
		ns3.start();
		ns4.start();
		ns5.start();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Main thread shutting down");
		}));

	}



}
