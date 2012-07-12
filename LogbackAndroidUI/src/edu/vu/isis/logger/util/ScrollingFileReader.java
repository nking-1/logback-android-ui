package edu.vu.isis.logger.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;

public class ScrollingFileReader {

	private static final int NUM_LINES_NOT_COUNTED = -1;

	private ByteBuffer mBuffer;
	private final File mFile;

	private final int spreadLimit;
	private int topLinePos;
	private int botLinePos;
	private int effectiveSpread;
	private int numLinesInBuffer = NUM_LINES_NOT_COUNTED;

	public ScrollingFileReader(File file, int spreadLimit)
			throws FileNotFoundException, IOException {
		mFile = file;
		mBuffer = openBuffer(mFile);
		this.spreadLimit = spreadLimit;
		topLinePos = botLinePos = 0;
		effectiveSpread = 0;
	}

	public String stepForward() {

		ByteBuffers.setBufferPosition(mBuffer, botLinePos);
		final String result = ByteBuffers.readForwardToNewline(mBuffer);

		// We don't need to adjust the positions if we were already at the
		// end of the buffer
		if (result == ByteBuffers.END_OF_TEXT_STR) {
			return result;
		}

		botLinePos = ByteBuffers.getBufferPosition(mBuffer);

		effectiveSpread++;

		if (isEffectiveSpreadInvalid()) {
			moveTopLinePosForward();
			effectiveSpread--;
		}

		return result;

	}

	public String stepBackward() {

		/*
		 * The topLinePos marks the first character in the topmost line that is
		 * within our spread. If we readBackwardToNewline() from topLinePos, we
		 * get only that first character back. If we try to subtract 1 and then
		 * read backward, we get a blank line because we were starting at the
		 * newline character. We have to subtract 2 so that we begin at the last
		 * character of the next line we will read.
		 */

		if (topLinePos == 0)
			return ByteBuffers.BEG_OF_TEXT_STR;

		if (topLinePos == 1) {
			ByteBuffers.setBufferPosition(mBuffer, 0);
		} else {
			ByteBuffers.setBufferPosition(mBuffer, topLinePos - 2);
		}

		final String result = ByteBuffers.readBackwardToNewline(mBuffer);

		/*
		 * Here we add 2 back to topLinePos if we did not reach the beginning of
		 * the buffer. This is to preserve the assertion that topLinePos always
		 * points to the first character in the topmost line that is within our
		 * spread.
		 */
		if (ByteBuffers.getBufferPosition(mBuffer) != 0) {
			topLinePos = ByteBuffers.getBufferPosition(mBuffer) + 2;
		} else {
			topLinePos = 0;
		}

		effectiveSpread++;

		if (isEffectiveSpreadInvalid()) {
			moveBotLinePosBackward();
			effectiveSpread--;
		}

		return result;

	}

	public String peekForward() {
		ByteBuffers.setBufferPosition(mBuffer, botLinePos);
		return ByteBuffers.readForwardToNewline(mBuffer);
	}

	public String peekBackward() {

		// See stepBackward for an explanation of this logic

		if (topLinePos == 0)
			return ByteBuffers.BEG_OF_TEXT_STR;

		if (topLinePos == 1) {
			ByteBuffers.setBufferPosition(mBuffer, 0);
		} else {
			ByteBuffers.setBufferPosition(mBuffer, topLinePos - 2);
		}

		return ByteBuffers.readBackwardToNewline(mBuffer);

	}

	/**
	 * Steps forward through the file as far as possible until the spread limit
	 * is reached.
	 * 
	 * @return -- the resulting array of lines, or null if no leap was made
	 */
	public String[] leapForward() {

		final int numElements = spreadLimit - effectiveSpread;
		if (numElements == 0)
			return null;
		String[] str = new String[numElements];

		for (int i = 0; i < numElements; i++) {
			str[i] = stepForward();
		}

		return str;

	}

	/**
	 * Steps backward through the file as far as possible until the spread limit
	 * is reached.
	 * 
	 * @return -- the resulting array of lines, or null if no leap was made
	 */
	public String[] leapBackward() {

		final int numElements = spreadLimit - effectiveSpread;
		if (numElements == 0)
			return null;
		String[] str = new String[numElements];

		for (int i = 0; i < numElements; i++) {
			str[i] = stepBackward();
		}

		return str;

	}

	public int getEffectiveSpread() {
		return effectiveSpread;
	}

	public int getSpreadLimit() {
		return spreadLimit;
	}

	/**
	 * Notifies this reader so that it can reconfigure itself to account for the
	 * new data in the file. This method <b>must</b> be called if the file is
	 * modified in order to ensure that the reader continues to work correctly.
	 * <p/>
	 * If the top line marker is out of range after the file has been modified,
	 * the top marker and the bottom marker are both moved to the beginning of
	 * the file. If only the bottom marker is out of range, then it is moved to
	 * the end of the file.
	 * <p/>
	 * This reader will no longer know how many lines are in the file, so it
	 * will have to recount them the next time the number of lines is needed
	 * after this method has been called.
	 * 
	 * @throws IOException
	 *             thrown if the file cannot be read after modification
	 * @throws FileNotFoundException
	 *             thrown if the file cannot be found after modification
	 */
	public void notifyFileModified() throws FileNotFoundException, IOException {

		mBuffer = openBuffer(mFile);

		numLinesInBuffer = NUM_LINES_NOT_COUNTED;

		if (topLinePos >= mBuffer.limit()) {
			topLinePos = botLinePos = 0;
		} else if (botLinePos >= mBuffer.limit()) {
			botLinePos = mBuffer.limit() - 1;
		}

	}

	/**
	 * Get the lines in between the top position and the bottom position.
	 * 
	 * @return
	 */
	public String[] getInnerLines() {

		final int numLines = ByteBuffers.countLinesBetween(mBuffer, topLinePos,
				botLinePos) + 1;
		String[] lineArray = new String[numLines];
		ByteBuffers.setBufferPosition(mBuffer, topLinePos);

		for (int i = 0; i < numLines; i++) {
			lineArray[i] = ByteBuffers.readForwardToNewline(mBuffer);
		}

		return lineArray;

	}

	/**
	 * Sets the line of the file for the top position. The spread limit will be
	 * enforced upon calling this, so the bottom position will also be moved
	 * appropriately.
	 * 
	 * @param line
	 *            -- the line of the file for the top position
	 */
	public void setTopLinePos(int line) {

		final int numLines = countLinesInBuffer();

		if (line <= 0) {
			throw new IllegalArgumentException(
					"The line number must be positive");
		} else if (line > numLines) {
			throw new IllegalArgumentException("Given line number " + line
					+ " but there are only " + numLines + " lines in the file!");
		}

		if (line < numLines / 2) {
			ByteBuffers.setBufferPosition(mBuffer, 0);
			ByteBuffers.skipForward(mBuffer, line - 1);
		} else {
			ByteBuffers.setBufferPosition(mBuffer,
					ByteBuffers.getBufferLimit(mBuffer) - 1);

			// Not sure why numlines-line+2 works... needs review
			ByteBuffers.skipBackward(mBuffer, numLines - line + 2);
		}

		topLinePos = ByteBuffers.getBufferPosition(mBuffer) + 2;
		ByteBuffers.setBufferPosition(mBuffer, topLinePos);
		final boolean skipSuccessful = ByteBuffers.skipForward(mBuffer,
				spreadLimit);
		botLinePos = ByteBuffers.getBufferPosition(mBuffer);

		if (skipSuccessful) {
			effectiveSpread = spreadLimit;
		} else {
			effectiveSpread = ByteBuffers.countLinesBetween(mBuffer,
					topLinePos, botLinePos);
		}

	}
	
	/**
	 * Sets the top and bottom markers to both be at the beginning of the file.
	 * The effective spread is therefore 0 after this method is called.  It is
	 * recommended to call leapForward() or stepForward() after calling this
	 * method to read the beginning lines of the file.
	 */
	public void jumpToBeginningOfFile() {
		botLinePos = topLinePos = 0;
		effectiveSpread = 0;
	}
	
	/**
	 * Sets the top and bottom markers to both be at the end of the file.
	 * The effective spread is therefore 0 after this method is called.  It is
	 * recommended to call leapBackward() or stepBackward() after calling this
	 * method to read the last lines of the file.
	 */
	public void jumpToEndOfFile() {
		botLinePos = topLinePos = (int) mFile.length();
		effectiveSpread = 0;
	}
	

	public int getNumLinesInBuffer() {
		return countLinesInBuffer();
	}

	private int countLinesInBuffer() {

		if (numLinesInBuffer == NUM_LINES_NOT_COUNTED) {

			numLinesInBuffer = 0;
			ByteBuffers.setBufferPosition(mBuffer, 0);

			while (ByteBuffers.skipForward(mBuffer, 1)) {
				numLinesInBuffer++;
			}

		}

		return numLinesInBuffer;

	}

	private boolean isEffectiveSpreadInvalid() {
		return effectiveSpread > spreadLimit;
	}

	private void moveTopLinePosForward() {

		ByteBuffers.setBufferPosition(mBuffer, topLinePos);
		ByteBuffers.skipForward(mBuffer, 1);
		topLinePos = ByteBuffers.getBufferPosition(mBuffer);

	}

	private void moveBotLinePosBackward() {

		ByteBuffers.setBufferPosition(mBuffer, botLinePos - 2);
		ByteBuffers.skipBackward(mBuffer, 1);
		botLinePos = ByteBuffers.getBufferPosition(mBuffer) + 2;

	}
	
	
	private static ByteBuffer openBuffer(File file) throws IOException {
		final FileInputStream fis = new FileInputStream(file);
		ByteBuffer buffer = fis.getChannel().map(MapMode.READ_ONLY,
				0, file.length());
		fis.close();
		return buffer;
	}


}
