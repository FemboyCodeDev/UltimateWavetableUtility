package audio_core;

public abstract class wave_script {
    public static Note[] notes = new Note[16];
    int SAMPLE_RATE = 44100;

    public static void note_start(Note note){
        //if (get_note_index(note)!=-1){
            for (int i = 0; i<notes.length;i++) {
                if ((notes[i]==null) || (notes[i].active==false)) {
                    notes[i] = note;
                    return;
                }
            }
        //}
    }
    public static int get_note_index(Note note){
        for (int i = 0; i<notes.length;i++){
            Note n = notes[i];
            if (n != null){
            if (n.note==note.note){
                return i;
            }}
        }
        return -1;
    }
    public void remove_note(int i){
        notes[i].active = false;
    }

    public byte[] generate(int cycles){
        byte[] values = new byte[cycles];
        for (int i=0; i<cycles;i++){
            values[i] = this.generate_cycle();
        }
        return values;
    }
    public byte generate_cycle(){
        int total_value = 0;
        for (Note n: notes){
            if (n != null){
            if (n.active){
                double t = (double) n.cycle / this.SAMPLE_RATE;
                double freq = 440* Math.pow(2,(n.note-69)/12);
                double value = (n.velocity+127) * Math.sin(2.0 * Math.PI * freq * t);
                total_value += (int)value;

                n.cycle +=1;
            }}

        }
        //System.out.println(total_value);
        return (byte)Math.clamp(total_value,-127,127);
        //return (byte) Math.max(-127,Math.min(127,total_value));
    }

}
