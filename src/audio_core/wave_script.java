package audio_core;

public abstract class wave_script {
    public static Note[] notes;


    public static void note_start(Note note){
        if (get_note_index(note)!=-1){

        }
    }
    public static int get_note_index(Note note){
        for (int i = 0; i<notes.length;i++){
            Note n = notes[i];
            if (n.note==note.note){
                return i;
            }
        }
        return -1;
    }
    public void remove_note(int i){
        notes[i].active = false;
    }

}
