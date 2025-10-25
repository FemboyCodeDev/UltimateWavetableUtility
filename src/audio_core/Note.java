package audio_core;

public class Note {
    /* A byte object is an 8 bit signed integer

     */
    public int note;
    public byte velocity; //-128: Velocity 0, 127: Velocity 255
    public byte pan; // -128: Left speaker, 127; Right speaker, 0; Both
    public boolean active; //Define if note is active or not
    public byte cycle; // TODO: DOCUMENT



}
