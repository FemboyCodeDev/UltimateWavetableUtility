package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A custom console-like window that supports text output and captures arrow key inputs.
 * This class mimics System.out for text display within a GUI environment.
 */
public class TextWindowConsole extends JFrame {

    private final JTextArea outputArea;
    private final JScrollPane scrollPane;

    // A flag to simulate non-blocking input handling (like a game loop)
    private final AtomicBoolean running = new AtomicBoolean(true);

    // Flag to control window update suppression (now prevents both scrolling and appending)
    private volatile boolean update_lock = false;

    // 🆕 Buffer to hold text while update_lock is true
    private final StringBuilder text_buffer = new StringBuilder();

    // New members for non-blocking task handling using a dedicated Thread
    private Thread current_space_thread;
    // Supplier for the core logic (the Runnable)
    private Supplier<Runnable> space_task_factory;
    public boolean space_toggle = false; // State: false=stopped, true=running

    public String key_pressed;

    public TextWindowConsole(String title) {
        super(title);

        // --- 1. Setup the GUI Components ---

        // Output Area: Where the text will appear
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setEditable(false); // Crucial: makes it an output log

        // Scroll Pane: Ensures the output log is scrollable
        scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Add components to the frame
        this.add(scrollPane, BorderLayout.CENTER);

        // --- 2. Configure the Frame ---

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(800, 600);
        this.setLocationRelativeTo(null); // Center the window

        // --- 3. Add Key Input Handling ---

        outputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                String keyName = "";

                // Identify the specific key pressed
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        keyName = "Up Arrow";
                        break;
                    case KeyEvent.VK_DOWN:
                        keyName = "Down Arrow";
                        break;
                    case KeyEvent.VK_LEFT:
                        keyName = "Left Arrow";
                        break;
                    case KeyEvent.VK_RIGHT:
                        keyName = "Right Arrow";
                        break;
                    case KeyEvent.VK_SPACE:
                        keyName = "Space";

                        // Non-blocking space task logic using Thread
                        if (space_task_factory == null) {
                            println("[ERROR]: Space task factory not set. Cannot run task.");
                            return;
                        }

                        // Toggle the state
                        space_toggle = !space_toggle;

                        if (!space_toggle) { // Task was running, now stop
                            if (current_space_thread != null) {
                                current_space_thread.interrupt();
                                current_space_thread = null;
                            }
                            println("[INPUT]: Space pressed. Task STOPPED.");
                        } else { // Task was stopped, now start
                            current_space_thread = new Thread(space_task_factory.get(), "SpaceTaskThread");
                            current_space_thread.setDaemon(true);
                            current_space_thread.start();

                            println("[INPUT]: Space pressed. Task STARTED (runs until interrupted).");
                        }
                        return; // Handled Space key

                    case KeyEvent.VK_ESCAPE:
                        // Example of a control key to stop the simulation
                        keyName = "Escape (Exiting Simulation)";
                        running.set(false);
                        break;
                    default:
                        // Ignore other keys for this example
                        return;
                }

                // Print the key press event to the console window
                key_pressed = keyName;
                println("[INPUT]: Key Pressed: " + keyName);
            }
        });

        // Ensure the JTextArea immediately gets focus to capture keys
        this.setVisible(true);
        outputArea.requestFocusInWindow();
    }

    /**
     * Sets the factory used to create a new Runnable instance every time the
     * space key is pressed to start the task.
     * @param factory A function that returns a new Runnable.
     */
    public void setSpaceTaskFactory(Supplier<Runnable> factory) {
        this.space_task_factory = factory;
    }

    // --- New Methods for Update Suppression ---

    /**
     * Sets the update lock. While locked, new output text is saved to a buffer
     * and is NOT appended to the display area, suppressing all visual updates.
     */
    public void lockUpdates() {
        this.update_lock = true;
    }

    /**
     * Releases the update lock. This **appends the entire buffered text** to the
     * display area in one go, followed by a final auto-scroll to the bottom.
     */
    public void unlockUpdates() {
        if (!this.update_lock) return;

        // Perform the GUI update on the EDT
        SwingUtilities.invokeLater(() -> {
            // Append the entire buffered content once
            if (text_buffer.length() > 0) {
                outputArea.append(text_buffer.toString());
                text_buffer.setLength(0); // Clear the buffer after appending
            }

            // Force a scroll to the bottom
            outputArea.setCaretPosition(outputArea.getDocument().getLength());

            this.update_lock = false; // Finally, release the lock
        });
    }

    // ---------------------------------------------


    /**
     * Core method to handle text output, either buffering or appending directly.
     * @param text The string to be written.
     * @param withNewline True if a newline should be appended.
     */
    private void write(String text, boolean withNewline) {
        String fullText = withNewline ? text + "\n" : text;

        if (update_lock) {
            // If locked, append to the in-memory buffer, preventing any GUI update.
            synchronized (text_buffer) {
                text_buffer.append(fullText);
            }
        } else {
            // If unlocked, immediately update the GUI on the EDT.
            SwingUtilities.invokeLater(() -> {
                outputArea.append(fullText);

                // Auto-scroll is only performed when not locked (which is always true here)
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            });
        }
    }

    /**
     * Appends text to the console window without a trailing newline.
     * This method is thread-safe.
     * @param text The string to be written.
     */
    public void print(String text) {
        write(text, false);
    }

    /**
     * Appends a line of text to the console window, similar to System.out.println().
     * This method is thread-safe.
     * @param text The string to be written.
     */
    public void println(String text) {
        write(text, true);
    }

    public void println(int text) {
        this.println(String.valueOf(text));

    }

    public void println() {
        println("");
    }

    // Inside TextWindowConsole.java
    public void clear() {
        if (update_lock) {
            // 🆕 If locked, just clear the pending buffer, no GUI update is needed.
            synchronized (text_buffer) {
                text_buffer.setLength(0);
            }
            // If the buffer is cleared, any subsequent prints will start from a clean slate.
        } else {
            // If unlocked, execute the clear on the EDT immediately (with potential for flicker)
            SwingUtilities.invokeLater(() -> {
                outputArea.setText("");
            });
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Main method for demonstrating the TextWindowConsole.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TextWindowConsole console = new TextWindowConsole("Custom Java Console - Complete Update Suppression");

            console.println("--- Welcome to the Custom Console ---");
            console.println("Press SPACE to start/stop the non-blocking Thread (runs every 500ms).");
            console.println("-------------------------------------");

            // Set the factory for the space task
            console.setSpaceTaskFactory(() -> new Runnable() {
                private int counter = 0;
                @Override
                public void run() {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            console.println("[SPACE TASK] Running non-blockingly! Count: " + ++counter);
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        console.println("[SPACE TASK] Interrupted/Stopped gracefully.");
                    }
                }
            });

            // --- Demonstration of complete update suppression (new behavior) ---
            new Thread(() -> {
                try {
                    Thread.sleep(3000); // Wait 3 seconds

                    // Prints before the lock will appear instantly
                    console.println("\n[DEMO] Starting a batch of 50 prints. No output will appear until unlocked...");

                    console.lockUpdates(); // 🔒 Lock updates: text is buffered

                    // This loop runs for 50 * 10ms = 500ms, but the GUI remains unchanged.
                    for (int i = 1; i <= 50; i++) {
                        // The text is saved to text_buffer, NOT appended to outputArea.
                        console.println(String.format("[BATCH] Buffered Print %d.", i));
                        Thread.sleep(10);
                    }

                    // This print is also saved to the buffer.
                    console.println("[DEMO] Batch complete. Unlocking updates now.");

                    Thread.sleep(2000); // Wait 2 seconds before unlocking (nothing is shown yet)

                    console.unlockUpdates(); // 🔓 Unlock updates: ALL 52 lines appear instantly and scroll.

                    console.println("[DEMO] Display is now updated and unlocked.");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }
}