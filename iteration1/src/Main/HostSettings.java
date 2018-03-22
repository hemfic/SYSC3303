package Main;

public class HostSettings {
	private int mode = 1;
	private int delay = 1000;
	private int packetNumber = 1;
	public synchronized int getMode() {
		return mode;
	}
	public synchronized int getDelay() {
		return delay;
	}
	public synchronized int getPacketNumber() {
		return packetNumber;
	}
	public synchronized void setMode(int mode) {
		this.mode = mode;
	}
	public synchronized void setDelay(int delay) {
		this.delay = delay;
	}
	public synchronized void setPacketNumber(int packetNum) {
		this.packetNumber = packetNum;
	}
	public synchronized void allSettings(int mode, int delay, int packetNum) {
		this.mode = mode;
		this.delay = delay;
		this.packetNumber = packetNum;
	}

}
