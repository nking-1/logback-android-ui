package edu.vu.isis.logger.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Collection of static helper methods for use with a ByteBuffer object.
 * @author Nick King
 */

public class ByteBuffers {

	public static final char BEG_OF_TEXT_CHAR = (char) 0x00000010;
	public static final String BEG_OF_TEXT_STR = Character.toString(BEG_OF_TEXT_CHAR);
	public static final char END_OF_TEXT_CHAR = (char) 0x00000011;
	public static final String END_OF_TEXT_STR = Character.toString(END_OF_TEXT_CHAR);
	
	// Hide the default constructor to prevent instantiation
	private ByteBuffers() { }
	
	
	// /////////////////////////////////////////////////////////////
	// STATIC HELPER METHODS
	// /////////////////////////////////////////////////////////////

	public static String readForwardToNewline(ByteBuffer buffer) {

		StringBuilder sb = new StringBuilder();

		if (getBufferPosition(buffer) == getBufferLimit(buffer))
			return END_OF_TEXT_STR;

		char next = (char) buffer.get();

		while (isNotBreak(next) && !isBufferUnderflow(buffer)) {
			sb.append(next);
			next = (char) buffer.get();
		}

		if (getBufferPosition(buffer) == getBufferLimit(buffer)) {
			// Signal the end of the file if it is reached
			sb.append(END_OF_TEXT_CHAR);
		}

		return sb.toString();

	}

	public static int getBufferLimit(ByteBuffer buffer) {
		return buffer.limit();
	}

	public static String readBackwardToNewline(ByteBuffer buffer) {

		StringBuilder sb = new StringBuilder();

		// Return if we are already at the beginning of the buffer
		if (getBufferPosition(buffer) == 0)
			return BEG_OF_TEXT_STR;

		char next = (char) buffer.get(getBufferPosition(buffer));
		decrementBufferPosition(buffer);

		// We have to stop at position 1 to avoid decrementing the buffer
		// position to -1
		while (isNotBreak(next) && getBufferPosition(buffer) >= 1) {
			sb.append(next);
			next = (char) buffer.get(getBufferPosition(buffer));
			decrementBufferPosition(buffer);
		}

		if (getBufferPosition(buffer) == 0) {

			// The char at position 1 was not appended because we reached
			// position 0, so we append it if it's not a newline char
			if (next != '\n') {
				sb.append(next);
			}

			// Add the first char in the file if we reached the beginning
			// and it is not a newline
			next = (char) buffer.get(0);
			if(next != '\n') {
				sb.append(next);
			}

			// Signal the beginning of the file
			sb.append(BEG_OF_TEXT_CHAR);

		}

		return sb.reverse().toString();

	}

	static boolean isNotBreak(char ch) {
		return ch != '\n' && ch != '\r';
	}

	public static int getBufferPosition(Buffer buffer) {
		return buffer.position();
	}

	public static void setBufferPosition(Buffer buffer, int newPosition) {
		buffer.position(newPosition);
	}

	public static void decrementBufferPosition(Buffer buffer) {
		setBufferPosition(buffer, getBufferPosition(buffer) - 1);
	}

	/**
	 * Moves the buffer forward a given number of lines
	 * 
	 * @param buffer
	 *            -- the buffer to use
	 * @param numLines
	 *            -- the number of lines to skip forward
	 * @return -- true if the given number of lines were skipped, false if not
	 */
	public static boolean skipForward(ByteBuffer buffer, int numLines) {

		if (getBufferPosition(buffer) == getBufferLimit(buffer))
			return false;

		// Waste no time if we were given a dumb argument
		if (numLines <= 0)
			return true;

		int numSkipped = 0;

		for (; numSkipped < numLines; numSkipped++) {

			char next = (char) buffer.get();

			while (isNotBreak(next) && !isBufferUnderflow(buffer)) {

				next = (char) buffer.get();

			}

			if (isBufferUnderflow(buffer)) {
				// We reached the end of the buffer, so we need to determine
				// whether or not we read all of the lines requested
				return numSkipped + 1 == numLines;
			}

		}

		return numSkipped == numLines;

	}

	/**
	 * Moves the buffer backward a given number of lines
	 * 
	 * @param buffer
	 *            -- the buffer to use
	 * @param numLines
	 *            -- the number of lines to skip backward
	 * @return -- true if the given number of lines were skipped, false if not
	 */
	public static boolean skipBackward(ByteBuffer buffer, int numLines) {

		// Return if we are already at the beginning of the buffer
		if (getBufferPosition(buffer) == 0)
			return false;

		// Waste no time if we were given a dumb argument
		if (numLines <= 0)
			return true;

		int numSkipped = 0;

		for (; numSkipped < numLines; numSkipped++) {

			char next = (char) buffer.get(getBufferPosition(buffer));
			decrementBufferPosition(buffer);

			// We have to stop at position 1 to avoid decrementing the buffer
			// position to -1
			while (isNotBreak(next) && getBufferPosition(buffer) >= 1) {
				next = (char) buffer.get(getBufferPosition(buffer));
				decrementBufferPosition(buffer);
			}

			if (getBufferPosition(buffer) == 0) {
				// We reached the beginning of the buffer, so we need to
				// determine
				// whether or not we read all of the lines requested
				return numSkipped + 1 == numLines;
			}

		}

		return numSkipped == numLines;

	}

	/**
	 * Checks if the buffer is currently in an underflow state
	 * 
	 * @param buffer
	 *            -- the buffer to check
	 * @return -- whether the buffer's position is not smaller than its limit
	 */
	public static boolean isBufferUnderflow(ByteBuffer buffer) {
		return !(getBufferPosition(buffer) < getBufferLimit(buffer));
	}

	static boolean isEOF(String str) {
		return str.endsWith(END_OF_TEXT_STR);
	}

	static boolean isBOF(String str) {
		return str.startsWith(BEG_OF_TEXT_STR);
	}

	public static int countLinesBetween(ByteBuffer buffer, int startPos,
			int endPos) {
		
		setBufferPosition(buffer, startPos);

		int numBetween = 0;
		while (skipForward(buffer, 1)) {
			if (getBufferPosition(buffer) < endPos) {
				numBetween++;
			} else {
				break;
			}
		}

		return numBetween;

	}

}
