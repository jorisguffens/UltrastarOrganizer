package be.jorisg.ultrastarorganizer.transcribe;

import javax.sound.sampled.*;

/**
 * Represents the Microphone of your computer
 */
public class Microphone {

	public static final int SAMPLE_RATE = 16000;
	public static final int SAMPLE_SIZE_IN_BITS = 16;

	/**
	 * Captures the sound of the microphone
	 */
	public AudioInputStream record() {
		try {
			AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, 1, true, false);
			DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

			if (!AudioSystem.isLineSupported(info)) {
				throw new IllegalArgumentException("Line not supported");
			}
			TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
			line.open(format);
			line.start();   // start capturing

			System.out.println("Start capturing...");

			return new AudioInputStream(line);
		} catch (LineUnavailableException ex) {
			throw new RuntimeException(ex);
		}
	}

}