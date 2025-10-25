package GUI;

import audio_core.audio_buffer;
import audio_core.presets.sine;
import audio_core.sequencer;
import audio_core.wave_script;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Timer;
import java.util.TimerTask;

/**
 * UI_Core.java
 * A Java Swing class that creates a custom component (a JPanel)
 * to display and edit a 2D byte array (byte[8][4]) and a 2D int array (int[8][4]).
 * The 'V' key toggles between editing the Sequence (int) grid and the Velocity (byte) grid.
 * SPACEBAR now toggles the sequence playback (Start/Stop).
 */
public class UI_Core extends JPanel implements KeyListener {

    // --- Data Structure and State ---
    private static final int NUM_ROWS = 8;
    private static final int NUM_COLS = 4;

    // 1. SEQUENCE GRID: Stores MIDI Note Numbers (0-127 usually, but allowing 0-255 int input)
    private int[][] sequenceGrid = new int[NUM_ROWS][NUM_COLS];

    // 2. VELOCITY GRID: Stores MIDI Velocity (0-127 usually, but allowing 0-255 byte input)
    private byte[][] velocityGrid = new byte[NUM_ROWS][NUM_COLS];

    private int selectedRow = 0;
    private int selectedCol = 0;

    // NEW: State for the active grid
    private boolean editingSequence = true; // true = editing sequenceGrid (int), false = editing velocityGrid (byte)

    private StringBuilder inputBuffer = new StringBuilder();
    private String statusMessage;

    // --- Sequence Management NEW ---
    private java.util.Timer sequenceTimer = null; // Timer to manage the playback loop
    private boolean isSequenceRunning = false;     // State flag

    // --- UI Constants ---
    private final int CELL_SIZE = 80;
    private final int PADDING_X = 50;
    private final int START_Y = 100;

    /**
     * Constructor for UI_Core.
     */
    public UI_Core() {
        // Initialize the grids with some starting values
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                sequenceGrid[r][c] = 0;
                // Initialize Velocity (e.g., 100)
                velocityGrid[r][c] = 0;
            }
        }

        // Set initial status message
        updateStatusMessage();

        // Set up the panel properties
        setBackground(new Color(20, 20, 30)); // Dark background
        setFocusable(true); // Mandatory to receive key events
        requestFocusInWindow(); // Request focus immediately

        // Add this class as a KeyListener
        addKeyListener(this);
    }

    /**
     * Updates the status message based on the current editing mode and sequence state.
     */
    private void updateStatusMessage() {
        String mode = editingSequence ? "SEQUENCE (int)" : "VELOCITY (byte)";
        String status = isSequenceRunning ? "Sequence RUNNING (Space to STOP)!" : "Sequence STOPPED (Space to PLAY).";

        statusMessage = String.format("Mode: %s. Selected [%d][%d]. Enter value (0-255). Press V to toggle. | %s",
                mode, selectedRow, selectedCol, status);
    }

    /**
     * Overrides paintComponent to handle all custom drawing (grid, values, cursor).
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 16));

        // --- 1. Draw Status Message ---
        // Color depends on editing mode OR if sequence is running
        Color statusColor;
        if (isSequenceRunning) {
            statusColor = new Color(255, 50, 50); // Red when running
        } else {
            statusColor = editingSequence ? new Color(255, 255, 100) : new Color(100, 255, 100);
        }

        g2d.setColor(statusColor);
        g2d.drawString(statusMessage, PADDING_X, 50);

        // --- 2. Draw the Grid and Values ---
        for (int r = 0; r < NUM_ROWS; r++) {
            for (int c = 0; c < NUM_COLS; c++) {
                int x = PADDING_X + c * CELL_SIZE;
                int y = START_Y + r * CELL_SIZE;

                // Draw Cell Background
                g2d.setColor(new Color(40, 40, 60));
                g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                // Draw Cell Border
                g2d.setColor(new Color(60, 60, 90));
                g2d.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                // Draw Selection Indicator (Cursor)
                if (r == selectedRow && c == selectedCol) {
                    Color cursorColor = editingSequence ? new Color(255, 180, 0, 180) : new Color(0, 255, 0, 180);
                    g2d.setColor(cursorColor);
                    g2d.setStroke(new BasicStroke(3));
                    g2d.drawRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                    g2d.setStroke(new BasicStroke(1));
                }

                // Determine and draw the value based on the active grid
                g2d.setColor(Color.WHITE);
                String valueStr;

                if (editingSequence) {
                    // Display value from the INT grid (Sequence/Note)
                    valueStr = String.format("%3d", sequenceGrid[r][c]);
                } else {
                    // Display value from the BYTE grid (Velocity). Display as unsigned (0-255).
                    int unsignedValue = velocityGrid[r][c] & 0xFF;
                    valueStr = String.format("%3d", unsignedValue);
                }

                // Center the text
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(valueStr);
                int textHeight = fm.getHeight();
                int textX = x + (CELL_SIZE - textWidth) / 2;
                int textY = y + (CELL_SIZE + textHeight) / 2 - fm.getDescent();

                g2d.drawString(valueStr, textX, textY);
            }
        }

        // --- 3. Draw Current Input Buffer (at the bottom) ---
        if (inputBuffer.length() > 0) {
            g2d.setFont(new Font("Monospaced", Font.BOLD, 20));
            g2d.setColor(Color.YELLOW);

            // Display the input buffer with a blinking cursor
            String inputDisplay = "Typing: " + inputBuffer.toString() +
                    (System.currentTimeMillis() % 1000 > 500 ? "|" : " ");
            g2d.drawString(inputDisplay, PADDING_X, getHeight() - 20);

            // Set a timer to force a repaint, making the cursor blink
            // NOTE: Using javax.swing.Timer for UI-related, single-shot repaint
            javax.swing.Timer cursorTimer = new javax.swing.Timer(500, e -> repaint());
            cursorTimer.setRepeats(false);
            cursorTimer.start();
        }
    }

    /**
     * KeyListener method: Invoked when a key is pressed down.
     * Used for navigation (Arrow Keys) and the Spacebar.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                selectedRow = Math.max(0, selectedRow - 1);
                break;
            case KeyEvent.VK_DOWN:
                selectedRow = Math.min(NUM_ROWS - 1, selectedRow + 1);
                break;
            case KeyEvent.VK_LEFT:
                selectedCol = Math.max(0, selectedCol - 1);
                break;
            case KeyEvent.VK_RIGHT:
                selectedCol = Math.min(NUM_COLS - 1, selectedCol + 1);
                break;
            case KeyEvent.VK_SPACE:
                togglePlayStop(); // Call the new toggle method
                break;
        }

        updateStatusMessage();
        repaint();
    }

    /**
     * KeyListener method: Invoked when a key is typed (press + release).
     * Used for actual character input (numbers, enter, backspace) and the 'V' toggle.
     */
    @Override
    public void keyTyped(KeyEvent e) {
        char keyChar = e.getKeyChar();

        // Prevent number input while the sequence is running
        if (isSequenceRunning) {
            if (keyChar != ' ' && Character.toUpperCase(keyChar) != 'V') {
                // Ignore all other input
                statusMessage = "Cannot edit while sequence is RUNNING. Press SPACE to stop.";
                repaint();
                return;
            }
        }

        if (Character.isDigit(keyChar)) {
            // Max 3 digits for 0-255
            if (inputBuffer.length() < 3) {
                inputBuffer.append(keyChar);
            }
        } else if (keyChar == KeyEvent.VK_BACK_SPACE) {
            // Backspace: delete the last character
            if (inputBuffer.length() > 0) {
                inputBuffer.setLength(inputBuffer.length() - 1);
            }
        } else if (keyChar == KeyEvent.VK_ENTER) {
            // Enter: process the input and set the value
            processInput();
        } else if (Character.toUpperCase(keyChar) == 'V') {
            // Toggle the active grid
            editingSequence = !editingSequence;
            updateStatusMessage();
        }

        repaint();
    }

    /**
     * KeyListener method: Invoked when a key is released. (Not used here)
     */
    @Override
    public void keyReleased(KeyEvent e) {
        // Empty implementation
    }

    // --- NEW: Toggle Start/Stop Logic ---
    private void togglePlayStop() {
        if (isSequenceRunning) {
            stopSequence();
        } else {
            startSequence();
        }
    }

    /**
     * Stops the running audio sequence, if any.
     */
    private void stopSequence() {
        if (sequenceTimer != null) {
            sequenceTimer.cancel(); // Stops the timer and its scheduled tasks
            sequenceTimer.purge();  // Removes cancelled tasks from the timer queue
            sequenceTimer = null;
        }
        isSequenceRunning = false;
        System.out.println("--- Sequence STOPPED ---");
        updateStatusMessage();
        repaint();
    }

    /**
     * Starts the audio sequence loop.
     */
    private void startSequence() {
        // Ensure it's not already running
        if (isSequenceRunning) return;

        try {
            // --- Sequence Setup ---
            int SAMPLE_RATE = 44100;
            // The wave_script and audio_buffer need to be initialized BEFORE the timer task
            wave_script play_back = new sine();
            audio_buffer a_buf = new audio_buffer(SAMPLE_RATE);
            sequencer seq = new sequencer(play_back);

            // 🌟 Map the two independent grids to the sequencer 🌟
            seq.sequence = sequenceGrid;
            seq.sequence_velocity = velocityGrid;

            // Activate all sequence notes that have a non-zero velocity
            for (int r = 0; r < NUM_ROWS; r++) {
                for (int c = 0; c < NUM_COLS; c++) {
                    // Velocity values are 0-255 (unsigned), so check against 0.
                    if ((velocityGrid[r][c] & 0xFF) > 0) {
                        seq.sequence_active[r][c] = true;
                    } else {
                        seq.sequence_active[r][c] = false;
                    }
                }
            }

            // --- Sequence Playback Loop (TimerTask) ---
            sequenceTimer = new java.util.Timer();
            isSequenceRunning = true;
            updateStatusMessage(); // Update state before starting

            // This TimerTask will run in its own thread, keeping the UI responsive
            TimerTask playbackTask = new TimerTask(){
                private int currentStep = 0; // Tracks the current step in the sequence (0-15)

                public void run(){
                    // 1. Generate and play the audio for the current step
                    try {
                        byte[] data = play_back.generate(SAMPLE_RATE);
                        a_buf.play_bytes(data);
                    } catch (Exception e) {
                        // Handle audio exceptions within the thread
                        SwingUtilities.invokeLater(() -> {
                            statusMessage = "Audio Playback Error: " + e.getMessage();
                            stopSequence(); // Stop the sequence on error
                            repaint();
                        });
                        return;
                    }

                    // 2. Advance the sequencer
                    // NOTE: The original loop was `for (int i =0; i<16;i++){ play_audio(); seq.call_note_sequence(i); }`.
                    // To make it a continuous loop, we'll run one step per Timer execution.
                    seq.call_note_sequence(currentStep);

                    // 3. Move to the next step (0-15 loop)
                    currentStep = (currentStep + 1) % 16;

                    // Optional: Update the UI to show the current playing step if needed,
                    // but for now, we'll just keep the sequence running.
                }
            };

            // Schedule the task to run repeatedly
            // Assuming 120 BPM, a 1/16th note delay is (60 / 120) * 1000 / 4 = 125ms
            long beatInterval = 125; // 125 milliseconds per 1/16th note at 120 BPM
            sequenceTimer.scheduleAtFixedRate(playbackTask, 0, beatInterval);

            System.out.println("--- Sequence STARTED ---");

        } catch (Exception ex) {
            stopSequence(); // Ensure state is correctly reset on failure
            statusMessage = "Audio Error: " + ex.getMessage();
            System.err.println("Audio playback failed: " + ex.getMessage());
        }
        repaint();
    }


    /**
     * Runs an action when the Spacebar is pressed.
     * The original play() logic is now part of startSequence().
     */
    private void play() {
        // This method is now obsolete/deprecated, as `togglePlayStop()` is called from keyPressed.
        // The core logic has been moved to `startSequence()` and `stopSequence()`.
        System.out.println("The original 'play()' method is no longer used for SPACEBAR. Use togglePlayStop().");
    }

    /**
     * Handles processing the numerical input from the buffer, validating it,
     * and applying it to the selected cell in the active grid.
     */
    private void processInput() {
        if (inputBuffer.length() == 0) {
            statusMessage = "Error: Input buffer is empty. Type a number first.";
            return;
        }

        try {
            int value = Integer.parseInt(inputBuffer.toString());

            if (value < 0 || value > 255) {
                statusMessage = "Error: Value " + value + " is out of valid range (0-255).";
            } else {
                if (editingSequence) {
                    // Update the INT grid
                    sequenceGrid[selectedRow][selectedCol] = value;
                    statusMessage = String.format("Set SEQUENCE [%d][%d] to %d. Ready for next edit.",
                            selectedRow, selectedCol, value);
                } else {
                    // Update the BYTE grid (cast handles the signed storage)
                    velocityGrid[selectedRow][selectedCol] = (byte) value;
                    statusMessage = String.format("Set VELOCITY [%d][%d] to %d. Ready for next edit.",
                            selectedRow, selectedCol, value);
                }
            }
        } catch (NumberFormatException ex) {
            statusMessage = "Error: Invalid number format. Please enter digits only.";
        }

        // Clear the buffer regardless of success/failure
        inputBuffer.setLength(0);
        updateStatusMessage();
    }

    /**
     * Main method to set up the JFrame and run the application.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("UI Core - Dual Array Editor (Sequence INT / Velocity BYTE)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(800, 750));

            UI_Core uiCore = new UI_Core();

            frame.add(uiCore);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            uiCore.requestFocusInWindow();
        });
    }
}