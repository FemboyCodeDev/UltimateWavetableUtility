package audio_core;

public class Note {
    /* A byte object is an 8 bit signed integer

     */
    public int note;
    public byte velocity; //-128: Velocity 0, 127: Velocity 255
    public byte pan; // -128: Left speaker, 127; Right speaker, 0; Both
    public boolean active; //Define if note is active or not
    public int cycle; // TODO: DOCUMENT

    public Note(int note, boolean active, byte velocity,int cycle){
        this.note = note;
        this.active = active;
        this.velocity = velocity;
        this.cycle = cycle;
    }
    public Note(int note){
        this.note = note;
        this.active = true;
        this.velocity = 0;
        this.cycle = 0;
    }

}
