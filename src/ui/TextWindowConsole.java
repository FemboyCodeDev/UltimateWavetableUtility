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

    // New flag to control window update suppression (primarily auto-scrolling and direct append)
    private volatile boolean update_lock = false;

    // 🆕 New members for non-blocking task handling using a dedicated Thread
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

        // 🆕 Removed java.util.Timer initialization

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

                        // 🆕 Non-blocking space task logic using Thread
                        if (space_task_factory == null) {
                            println("[ERROR]: Space task factory not set. Cannot run task.");
                            return;
                        }

                        // Toggle the state
                        space_toggle = !space_toggle;

                        if (!space_toggle) { // Task was running, now stop
                            if (current_space_thread != null) {
                                // 1. Crucial: Interrupt the running thread.
                                // The task's run() method must check for this interrupt.
                                current_space_thread.interrupt();
                                current_space_thread = null; // Clean up the reference
                            }
                            println("[INPUT]: Space pressed. Task STOPPED.");
                        } else { // Task was stopped, now start
                            // 1. Create a brand new Thread instance with the supplied Runnable
                            current_space_thread = new Thread(space_task_factory.get(), "SpaceTaskThread");
                            current_space_thread.setDaemon(true); // Allow JVM to exit if this is the only thread left

                            // 2. Start the new task instance non-blockingly
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
        // Must be done *after* the frame is made visible
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

    // --- 🆕 New Methods for Update Suppression ---

    /**
     * Sets the update lock. While locked, new output text is appended but auto-scrolling is suppressed.
     * Use this before a batch of prints for performance.
     */
    public void lockUpdates() {
        this.update_lock = true;
    }

    /**
     * Releases the update lock. This triggers a final auto-scroll to the bottom.
     * Call this after a batch of prints is complete.
     */
    public void unlockUpdates() {
        this.update_lock = false;
        // Force a scroll to the bottom immediately after unlocking
        SwingUtilities.invokeLater(() -> {
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }

    // ---------------------------------------------


    /**
     * Appends text to the console window without a trailing newline.
     * This method is thread-safe, ensuring GUI updates happen on the Event Dispatch Thread (EDT).
     * @param text The string to be written.
     */
    public void print(String text) {
        // Swing GUI updates must happen on the EDT. SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text);

            // Auto-scroll to the bottom of the output area ONLY if not locked
            if (!update_lock) {
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        });
    }

    /**
     * Appends a line of text to the console window, similar to System.out.println().
     * The text will be followed by a trailing newline character (\n).
     * This method is thread-safe, ensuring GUI updates happen on the Event Dispatch Thread (EDT).
     * @param text The string to be written.
     */
    public void println(String text) {
        // Swing GUI updates must happen on the EDT. SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(() -> {
            outputArea.append(text + "\n");

            // Auto-scroll to the bottom of the output area ONLY if not locked
            if (!update_lock) {
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        });
    }

    public void println(int text) {
        this.println(String.valueOf(text));

    }

    public void println() {
        println("");
    }
    public void clear() {
        // Swing GUI updates must happen on the EDT. SwingUtilities.invokeLater ensures this.
        SwingUtilities.invokeLater(() -> {
            outputArea.setText("");
        });
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Main method for demonstrating the TextWindowConsole.
     */
    public static void main(String[] args) {
        // Use the Event Dispatch Thread to initialize the GUI
        SwingUtilities.invokeLater(() -> {
            TextWindowConsole console = new TextWindowConsole("Custom Java Console - Arrow Key Input");

            console.println("--- Welcome to the Custom Console ---");
            console.println("Press the Up, Down, Left, or Right arrow keys to test input capture.");
            console.println("Press ESC to stop the background simulation.");
            console.println("-------------------------------------");

            // 🆕 Set the factory for the space task, now providing a Runnable
            console.setSpaceTaskFactory(() -> new Runnable() {
                private int counter = 0;
                @Override
                public void run() {
                    // This code runs on the dedicated "SpaceTaskThread".
                    try {
                        // The loop should continue *until* the thread is interrupted
                        while (!Thread.currentThread().isInterrupted()) {
                            // Non-blocking task logic
                            console.println("[SPACE TASK] Running non-blockingly! Count: " + ++counter);

                            // The pause should be inside the try block to catch the InterruptedException
                            // when the main thread calls thread.interrupt()
                            Thread.sleep(500); // Wait 500 milliseconds (0.5s)
                        }
                    } catch (InterruptedException e) {
                        // This is the clean way to stop the thread when 'thread.interrupt()' is called.
                        // Re-interrupt the thread for higher-level interrupt handlers (optional for this demo)
                        Thread.currentThread().interrupt();
                        // Fall through to finally or just exit the method.
                        console.println("[SPACE TASK] Interrupted/Stopped gracefully.");
                    }
                }
            });

            console.println("Press SPACE to start/stop the non-blocking Thread (runs every 500ms).");
            console.println("-------------------------------------");

            // --- Demonstration of background process output (existing simulation log) ---
            new Thread(() -> {
                int count = 0;
                while (console.isRunning()) {
                    try {
                        Thread.sleep(2000); // Wait 2 seconds
                        console.println(String.format("[LOG] Simulation step %d complete.", ++count));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                console.println("Simulation stopped.");
            }).start();

            // --- 🆕 Demonstration of update suppression ---
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // Wait 5 seconds
                    console.println("\n[DEMO] Starting a batch of 50 prints with update suppression...");
                    console.lockUpdates(); // 🔒 Lock updates

                    for (int i = 1; i <= 50; i++) {
                        console.println(String.format("[BATCH] Locked Print %d.", i));
                        Thread.sleep(10); // Short delay to simulate work, but scrolling is suppressed
                    }

                    console.println("[DEMO] Batch complete. Unlocking updates now.");
                    console.unlockUpdates(); // 🔓 Unlock updates, forces scroll
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }
}