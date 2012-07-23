package edu.vu.isis.logger.util;

/**
 * Represents a single element in a list of log entries
 * @author Nick King
 *
 */
public class LogElement {
	
	private LogLevel mLevel;
	private String mMessage;
	
	/**
	 * Make a new LogElement with the given level and message
	 * @param level -- the level corresponding to the entry
	 * @param message -- the logged message
	 */
	public LogElement(LogLevel level, String message) {
		this.mLevel = level;
		this.mMessage = message;
	}
	
	public LogLevel getLogLevel() {
		return this.mLevel;
	}
	
	// The LogLevel field was made mutable so we can
	// efficiently and easily recolor logs if necessary
	public void setLogLevel(LogLevel newLevel) {
		mLevel = newLevel;
	}
	
	public String getMessage() {
		return this.mMessage;
	}
	
	@Override
	public String toString() {
		return "LogElement Level: [" + mLevel.toString() + "] Message: [" + mMessage + "]";
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + mLevel.hashCode();
		result = 31 * result + mMessage.hashCode();
		return result;
	}
	
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof LogElement)) return false;
		LogElement otherElement = (LogElement) other;
		return otherElement.mMessage.equals(this.mMessage) && otherElement.mLevel.equals(this.mLevel);
	}
	
}
