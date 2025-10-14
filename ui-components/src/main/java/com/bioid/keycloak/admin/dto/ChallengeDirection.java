package com.bioid.keycloak.admin.dto;

/**
 * Enumeration of challenge directions for challenge-response liveness detection.
 * 
 * Requirements addressed: 3.1, 4.2
 */
public enum ChallengeDirection {
    /**
     * Head movement upward.
     */
    UP("up"),
    
    /**
     * Head movement downward.
     */
    DOWN("down"),
    
    /**
     * Head movement to the left.
     */
    LEFT("left"),
    
    /**
     * Head movement to the right.
     */
    RIGHT("right");
    
    private final String value;
    
    ChallengeDirection(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Converts string value to ChallengeDirection enum.
     * 
     * @param value the string value
     * @return the corresponding ChallengeDirection
     * @throws IllegalArgumentException if value is not recognized
     */
    public static ChallengeDirection fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Challenge direction value cannot be null");
        }
        
        for (ChallengeDirection direction : values()) {
            if (direction.value.equalsIgnoreCase(value) || direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        
        throw new IllegalArgumentException("Unknown challenge direction: " + value);
    }
    
    /**
     * Returns the opposite direction for validation purposes.
     */
    public ChallengeDirection getOpposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: throw new IllegalStateException("Unknown direction: " + this);
        }
    }
    
    /**
     * Returns whether this direction is horizontal (left/right).
     */
    public boolean isHorizontal() {
        return this == LEFT || this == RIGHT;
    }
    
    /**
     * Returns whether this direction is vertical (up/down).
     */
    public boolean isVertical() {
        return this == UP || this == DOWN;
    }
    
    @Override
    public String toString() {
        return value;
    }
}