package de.bitsharesmunich.error;

/**
 * Exception thrown whenever a set of resources are in an inconsistent state.
 *
 * Created by nelson on 2/8/17.
 */
public class IllegalResourceState extends RuntimeException {
    public IllegalResourceState(String message){
        super(message);
    }
}
