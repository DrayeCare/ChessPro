import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.sound.sampled.*;

public class ChessGame extends JFrame {

    // --- ENUMS ---
    enum GameMode { PVE, PVP }
    enum BotDifficulty { EASY, MEDIUM, HARD }
    enum PieceColor { WHITE, BLACK }
    enum PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }

    // --- KELAS PEMBANTU ---
    class Arrow { 
        int sr, sc, er, ec; 
        public Arrow(int sr, int sc, int er, int ec) { 
            this.sr = sr; 
            this.sc = sc; 
            this.er = er; 
            this.ec = ec; 
        } 
    }
    
    class Move { 
        int startR, startC, endR, endC; 
        boolean isCapture; 
        public Move(int startR, int startC, int endR, int endC, boolean isCapture) { 
            this.startR = startR; 
            this.startC = startC; 
            this.endR = endR; 
            this.endC = endC; 
            this.isCapture = isCapture; 
        } 
    }
    
    class Piece { 
        PieceColor color; 
        PieceType type; 
        boolean hasMoved = false; 
        
        public Piece(PieceColor color, PieceType type) { 
            this.color = color; 
            this.type = type; 
        } 
        
        public String getSymbol() { 
            if (color == PieceColor.WHITE) { 
                switch (type) { 
                    case KING: return "\u2654"; 
                    case QUEEN: return "\u2655"; 
                    case ROOK: return "\u2656"; 
                    case BISHOP: return "\u2657"; 
                    case KNIGHT: return "\u2658"; 
                    case PAWN: return "\u2659"; 
                } 
            } else { 
                switch (type) { 
                    case KING: return "\u265A"; 
                    case QUEEN: return "\u265B"; 
                    case ROOK: return "\u265C"; 
                    case BISHOP: return "\u265D"; 
                    case KNIGHT: return "\u265E"; 
                    case PAWN: return "\u265F"; 
                } 
            } 
            return ""; 
        } 
    }

    // --- GAME CONFIG VARIABLES ---
    private GameMode currentMode = GameMode.PVE;
    private BotDifficulty botDifficulty = BotDifficulty.MEDIUM;
    private PieceColor playerColor = PieceColor.WHITE; 
    
    // --- CUSTOM RENDERING VARIABLES ---
    private java.util.List<Arrow> arrows = new ArrayList<>();
    private int arrowStartR = -1, arrowStartC = -1;
    private int currentArrowEndR = -1, currentArrowEndC = -1;
    private Piece draggedPiece = null;
    private int dragStartR = -1, dragStartC = -1;
    private int dragX = -1, dragY = -1;

    private Piece[][] boardState = new Piece[8][8];
    private int selectedRow = -1;
    private int selectedCol = -1;
    private boolean isWhiteTurn = true;
    private JLabel statusLabel;
    private static Map<String, ImageIcon> pieceImages = new HashMap<>();

    // --- NEW VARIABLES FOR UI ---
    private CardLayout cardLayout;
    private JPanel mainContainer;
    private JTextArea moveHistoryArea;
    private JPanel boardPanel;
    private int moveCount = 1;
    private int playerElo = 1200;
    private String playerRank = "Silver";

    // --- TAMBAHAN FITUR WAKTU & HIGHLIGHT ---
    private JLabel botTimeLabel;
    private JLabel playerTimeLabel;
    private Timer gameTimer;
    private int playerSecondsLeft;
    private int botSecondsLeft;

    // --- FITUR PREMOVE ---
    private int premoveStartRow = -1;
    private int premoveStartCol = -1;
    private int premoveEndRow = -1;
    private int premoveEndCol = -1;
    private boolean hasPremove = false;

    public ChessGame() {
        setTitle("Game Catur Pro - Edition");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        loadImages();
        
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        
        mainContainer.add(createMainMenu(), "MainMenu");
        mainContainer.add(createGameUI(), "GameScreen");
        mainContainer.add(createPostGameUI("", null), "PostGame");
        
        add(mainContainer);
        cardLayout.show(mainContainer, "MainMenu");
    }

    // --- AUDIO SYSTEM (Synthesized SFX) ---
    private void playSound(String type) {
        new Thread(() -> {
            try {
                int hz = 400; int msecs = 100; double vol = 0.2;
                if (type.equals("capture")) { hz = 800; msecs = 150; }
                else if (type.equals("check")) { hz = 1000; msecs = 300; vol = 0.3; }
                else if (type.equals("move")) { hz = 300; msecs = 80; }
                
                byte[] buf = new byte[msecs * 8];
                AudioFormat af = new AudioFormat(8000f, 8, 1, true, false);
                SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
                sdl.open(af);
                sdl.start();
                for (int i = 0; i < buf.length; i++) {
                    double angle = i / (8000f / hz) * 2.0 * Math.PI;
                    buf[i] = (byte) (Math.sin(angle) * 127.0 * vol);
                }
                sdl.write(buf, 0, buf.length);
                sdl.drain();
                sdl.stop();
                sdl.close();
            } catch (Exception e) {}
        }).start();
    }

    // --- 1. MAIN MENU ---
    private JPanel createMainMenu() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(40, 40, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("CHESS PRO", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 48));
        title.setForeground(Color.WHITE);
        panel.add(title, gbc);

        JLabel profileLabel = new JLabel("Player123 | Rank: " + playerRank + " (" + playerElo + " ELO)", SwingConstants.CENTER);
        profileLabel.setForeground(Color.LIGHT_GRAY);
        panel.add(profileLabel, gbc);

        JButton btnPlayBot = createMenuButton("Play vs Bot (PvE)");
        btnPlayBot.addActionListener(e -> showPvESetupDialog());
        panel.add(btnPlayBot, gbc);

        JButton btnPlayLocal = createMenuButton("Play Local (PvP)");
        btnPlayLocal.addActionListener(e -> showPvPSetupDialog());
        panel.add(btnPlayLocal, gbc);

        JButton btnPlayOnline = createMenuButton("Play Online (Matchmaking)");
        btnPlayOnline.addActionListener(e -> showComingSoon("Matchmaking Server"));
        panel.add(btnPlayOnline, gbc);

        JButton btnExit = createMenuButton("Exit");
        btnExit.addActionListener(e -> System.exit(0));
        panel.add(btnExit, gbc);

        return panel;
    }

    private void showPvESetupDialog() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        JComboBox<String> timeCombo = new JComboBox<>(new String[]{"1 Menit", "3 Menit", "10 Menit"});
        timeCombo.setSelectedIndex(2);
        
        JComboBox<String> diffCombo = new JComboBox<>(new String[]{"Easy", "Medium", "Hard"});
        diffCombo.setSelectedIndex(1);
        
        JComboBox<String> colorCombo = new JComboBox<>(new String[]{"Putih", "Hitam"});
        
        panel.add(new JLabel("Waktu:")); panel.add(timeCombo);
        panel.add(new JLabel("Tingkat Bot:")); panel.add(diffCombo);
        panel.add(new JLabel("Pilih Tim:")); panel.add(colorCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Pengaturan PvE", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int mins = timeCombo.getSelectedIndex() == 0 ? 1 : (timeCombo.getSelectedIndex() == 1 ? 3 : 10);
            BotDifficulty diff = BotDifficulty.values()[diffCombo.getSelectedIndex()];
            PieceColor pColor = colorCombo.getSelectedIndex() == 0 ? PieceColor.WHITE : PieceColor.BLACK;
            
            startGame(mins, GameMode.PVE, diff, pColor);
        }
    }

    private void showPvPSetupDialog() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 10));
        JComboBox<String> timeCombo = new JComboBox<>(new String[]{"1 Menit", "3 Menit", "10 Menit"});
        timeCombo.setSelectedIndex(2);
        panel.add(new JLabel("Waktu:")); panel.add(timeCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Pengaturan PvP Offline", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            int mins = timeCombo.getSelectedIndex() == 0 ? 1 : (timeCombo.getSelectedIndex() == 1 ? 3 : 10);
            startGame(mins, GameMode.PVP, BotDifficulty.EASY, PieceColor.WHITE); // PvP selalu white di bawah
        }
    }

    private JButton createMenuButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setFocusPainted(false);
        btn.setBackground(new Color(70, 130, 180));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(250, 45));
        return btn;
    }

    private void showComingSoon(String feature) {
        JOptionPane.showMessageDialog(this, "Fitur [" + feature + "] masih dalam tahap pengembangan.", "Coming Soon", JOptionPane.INFORMATION_MESSAGE);
    }

    // --- HELPER UNTUK MEMUTAR PAPAN VISUAL ---
    private int screenR(int r) { return playerColor == PieceColor.BLACK ? 7 - r : r; }
    private int screenC(int c) { return playerColor == PieceColor.BLACK ? 7 - c : c; }
    private int boardR(int y, int sqH) { int r = y / sqH; return playerColor == PieceColor.BLACK ? 7 - r : r; }
    private int boardC(int x, int sqW) { int c = x / sqW; return playerColor == PieceColor.BLACK ? 7 - c : c; }

    // --- 3. IN-GAME UI & HUD ---
    private JPanel createGameUI() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        
        boardPanel = createCustomBoardPanel();
        gamePanel.add(boardPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(250, 0));
        rightPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel historyTitle = new JLabel("Move History", SwingConstants.CENTER);
        historyTitle.setFont(new Font("Arial", Font.BOLD, 14));
        rightPanel.add(historyTitle, BorderLayout.NORTH);

        moveHistoryArea = new JTextArea();
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        DefaultCaret caret = (DefaultCaret) moveHistoryArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(moveHistoryArea);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JButton btnResign = new JButton("Resign \uD83C\uDFF3\uFE0F");
        btnResign.addActionListener(e -> {
            if (gameTimer != null) gameTimer.stop();
            showPostGame(isWhiteTurn ? "Hitam Menang (Putih Menyerah)" : "Putih Menang (Hitam Menyerah)", !isWhiteTurn);
        });
        JButton btnDraw = new JButton("Offer Draw \uD83E\uDD1D");
        btnDraw.addActionListener(e -> JOptionPane.showMessageDialog(this, currentMode == GameMode.PVE ? "Bot menolak tawaran Draw!" : "Lawan Menolak Draw!"));
        actionPanel.add(btnDraw);
        actionPanel.add(btnResign);
        rightPanel.add(actionPanel, BorderLayout.SOUTH);

        gamePanel.add(rightPanel, BorderLayout.EAST);

        JPanel topHUD = new JPanel(new BorderLayout());
        topHUD.setBorder(new EmptyBorder(5, 10, 5, 10));
        topHUD.add(new JLabel(currentMode == GameMode.PVE ? "\uD83E\uDD16 Bot (" + botDifficulty + ")" : "\uD83D\uDC64 Player 2 (Hitam)"), BorderLayout.WEST);
        botTimeLabel = new JLabel("10:00", SwingConstants.RIGHT);
        botTimeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topHUD.add(botTimeLabel, BorderLayout.EAST);

        JPanel bottomHUD = new JPanel(new BorderLayout());
        bottomHUD.setBorder(new EmptyBorder(5, 10, 5, 10));
        statusLabel = new JLabel("Giliran: Putih", SwingConstants.CENTER);
        bottomHUD.add(new JLabel("\uD83D\uDC64 Player 1 (" + playerColor + ")"), BorderLayout.WEST);
        bottomHUD.add(statusLabel, BorderLayout.CENTER);
        playerTimeLabel = new JLabel("10:00", SwingConstants.RIGHT);
        playerTimeLabel.setFont(new Font("Arial", Font.BOLD, 16));
        bottomHUD.add(playerTimeLabel, BorderLayout.EAST);

        gamePanel.add(topHUD, BorderLayout.NORTH);
        gamePanel.add(bottomHUD, BorderLayout.SOUTH);

        return gamePanel;
    }

    private void startGame(int minutes, GameMode mode, BotDifficulty difficulty, PieceColor color) {
        this.currentMode = mode;
        this.botDifficulty = difficulty;
        this.playerColor = color;
        
        setupPieces();
        clearPremove();
        selectedRow = -1; selectedCol = -1; draggedPiece = null;
        arrows.clear();
        
        playerSecondsLeft = minutes * 60;
        botSecondsLeft = minutes * 60;
        updateTimeLabels();
        
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(1000, e -> {
            if (isWhiteTurn) {
                playerSecondsLeft--;
                if (playerSecondsLeft <= 0) { gameTimer.stop(); showPostGame("Waktu Habis! Hitam Menang.", false); }
            } else {
                botSecondsLeft--;
                if (botSecondsLeft <= 0) { gameTimer.stop(); showPostGame("Waktu Habis! Putih Menang.", true); }
            }
            updateTimeLabels();
        });
        gameTimer.start();
        
        moveHistoryArea.setText("");
        moveCount = 1;
        isWhiteTurn = true;
        statusLabel.setText("Giliran: Putih");

        // Jika Bot adalah Putih, trigger jalan duluan
        if (currentMode == GameMode.PVE && playerColor == PieceColor.BLACK) {
            statusLabel.setText("Giliran: Bot Putih sedang berpikir...");
            Timer startTimer = new Timer(500, e -> makeBotMove());
            startTimer.setRepeats(false); startTimer.start();
        }

        updateBoardUI();
        cardLayout.show(mainContainer, "GameScreen");
    }

    private void updateTimeLabels() {
        playerTimeLabel.setText(String.format("%02d:%02d", playerSecondsLeft / 60, playerSecondsLeft % 60));
        botTimeLabel.setText(String.format("%02d:%02d", botSecondsLeft / 60, botSecondsLeft % 60));
    }

    private JPanel createPostGameUI(String resultMessage, Boolean isPlayerWin) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;

        JLabel resultLabel = new JLabel(isPlayerWin == null ? "Seri!" : (isPlayerWin ? "MENANG!" : "GAME OVER"));
        resultLabel.setFont(new Font("Arial", Font.BOLD, 36));
        resultLabel.setForeground(isPlayerWin == null ? Color.ORANGE : (isPlayerWin ? Color.GREEN : Color.RED));
        panel.add(resultLabel, gbc);

        gbc.gridy++;
        JLabel msgLabel = new JLabel(resultMessage);
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        panel.add(msgLabel, gbc);

        if (currentMode == GameMode.PVE) {
            int eloChange = isPlayerWin == null ? 0 : (isPlayerWin ? 15 : -10);
            playerElo += eloChange;
            gbc.gridy++;
            JLabel eloLabel = new JLabel("ELO Change: " + (eloChange >= 0 ? "+" : "") + eloChange + " (Your ELO: " + playerElo + ")");
            eloLabel.setForeground(eloChange >= 0 ? Color.GREEN : Color.RED);
            panel.add(eloLabel, gbc);
        }

        gbc.gridy++;
        JButton btnMenu = createMenuButton("Back to Main Menu");
        btnMenu.addActionListener(e -> {
            Component[] comps = ((JPanel)mainContainer.getComponent(0)).getComponents();
            for(Component c : comps) {
                if(c instanceof JLabel && ((JLabel)c).getText().contains("Rank:")) {
                    ((JLabel)c).setText("Player123 | Rank: " + playerRank + " (" + playerElo + " ELO)");
                }
            }
            cardLayout.show(mainContainer, "MainMenu");
        });
        panel.add(btnMenu, gbc);

        return panel;
    }

    private void showPostGame(String result, Boolean isPlayerWin) {
        playSound("check"); // Bunyi peringatan game selesai
        mainContainer.add(createPostGameUI(result, isPlayerWin), "PostGame_Dynamic");
        cardLayout.show(mainContainer, "PostGame_Dynamic");
    }

    private String getChessNotation(int r, int c) {
        char colChar = (char) ('a' + c);
        int rowNum = 8 - r;
        return "" + colChar + rowNum;
    }

    private void loadImages() {
        try {
            // Aset "Neo" Modern Chess.com Theme
            String baseUrl = "https://images.chesscomfiles.com/chess-themes/pieces/neo/150/";
            pieceImages.put("WHITE_KING", new ImageIcon(ImageIO.read(new URL(baseUrl + "wk.png"))));
            pieceImages.put("WHITE_QUEEN", new ImageIcon(ImageIO.read(new URL(baseUrl + "wq.png"))));
            pieceImages.put("WHITE_ROOK", new ImageIcon(ImageIO.read(new URL(baseUrl + "wr.png"))));
            pieceImages.put("WHITE_BISHOP", new ImageIcon(ImageIO.read(new URL(baseUrl + "wb.png"))));
            pieceImages.put("WHITE_KNIGHT", new ImageIcon(ImageIO.read(new URL(baseUrl + "wn.png"))));
            pieceImages.put("WHITE_PAWN", new ImageIcon(ImageIO.read(new URL(baseUrl + "wp.png"))));
            
            pieceImages.put("BLACK_KING", new ImageIcon(ImageIO.read(new URL(baseUrl + "bk.png"))));
            pieceImages.put("BLACK_QUEEN", new ImageIcon(ImageIO.read(new URL(baseUrl + "bq.png"))));
            pieceImages.put("BLACK_ROOK", new ImageIcon(ImageIO.read(new URL(baseUrl + "br.png"))));
            pieceImages.put("BLACK_BISHOP", new ImageIcon(ImageIO.read(new URL(baseUrl + "bb.png"))));
            pieceImages.put("BLACK_KNIGHT", new ImageIcon(ImageIO.read(new URL(baseUrl + "bn.png"))));
            pieceImages.put("BLACK_PAWN", new ImageIcon(ImageIO.read(new URL(baseUrl + "bp.png"))));
        } catch (Exception e) {
            System.out.println("Gagal memuat gambar Neo. Pastikan terkoneksi internet!");
        }
    }

    private JPanel createCustomBoardPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sqW = getWidth() / 8;
                int sqH = getHeight() / 8;

                for (int r = 0; r < 8; r++) {
                    for (int c = 0; c < 8; c++) {
                        int drawR = screenR(r);
                        int drawC = screenC(c);
                        
                        boolean isLight = (r + c) % 2 == 0;
                        g2.setColor(isLight ? new Color(240, 217, 181) : new Color(181, 136, 99));
                        g2.fillRect(drawC * sqW, drawR * sqH, sqW, sqH);

                        if (r == selectedRow && c == selectedCol) {
                            g2.setColor(new Color(255, 255, 102, 150));
                            g2.fillRect(drawC * sqW, drawR * sqH, sqW, sqH);
                        }
                        if (r == premoveStartRow && c == premoveStartCol) {
                            g2.setColor(new Color(255, 120, 120, 150));
                            g2.fillRect(drawC * sqW, drawR * sqH, sqW, sqH);
                        }
                        if (hasPremove && r == premoveEndRow && c == premoveEndCol) {
                            g2.setColor(new Color(220, 50, 50, 150));
                            g2.fillRect(drawC * sqW, drawR * sqH, sqW, sqH);
                        }

                        if (selectedRow != -1 && selectedCol != -1 && isLegalMove(selectedRow, selectedCol, r, c)) {
                            if (boardState[r][c] == null) {
                                g2.setColor(new Color(100, 100, 100, 150));
                                g2.fillOval(drawC * sqW + sqW / 2 - 15, drawR * sqH + sqH / 2 - 15, 30, 30);
                            } else {
                                g2.setColor(new Color(255, 99, 71, 150));
                                g2.fillRect(drawC * sqW, drawR * sqH, sqW, sqH);
                            }
                        }

                        Piece p = boardState[r][c];
                        if (p != null && (p != draggedPiece || draggedPiece == null)) {
                            ImageIcon icon = pieceImages.get(p.color.name() + "_" + p.type.name());
                            if (icon != null) {
                                g2.drawImage(icon.getImage(), drawC * sqW, drawR * sqH, sqW, sqH, null);
                            }
                        }
                    }
                }

                for (Arrow a : arrows) drawArrow(g2, screenR(a.sr), screenC(a.sc), screenR(a.er), screenC(a.ec), new Color(255, 165, 0, 200));
                if (arrowStartR != -1 && currentArrowEndR != -1) {
                    drawArrow(g2, screenR(arrowStartR), screenC(arrowStartC), screenR(currentArrowEndR), screenC(currentArrowEndC), new Color(255, 165, 0, 200));
                }

                if (draggedPiece != null) {
                    ImageIcon icon = pieceImages.get(draggedPiece.color.name() + "_" + draggedPiece.type.name());
                    if (icon != null) {
                        g2.drawImage(icon.getImage(), dragX - sqW / 2, dragY - sqH / 2, sqW, sqH, null);
                    }
                }
            }
        };

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int sqW = panel.getWidth() / 8;
                int sqH = panel.getHeight() / 8;
                int r = boardR(Math.max(0, Math.min(panel.getHeight()-1, e.getY())), sqH);
                int c = boardC(Math.max(0, Math.min(panel.getWidth()-1, e.getX())), sqW);

                if (SwingUtilities.isRightMouseButton(e)) {
                    arrowStartR = r; arrowStartC = c;
                    currentArrowEndR = r; currentArrowEndC = c;
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    arrows.clear();
                    Piece p = boardState[r][c];
                    
                    boolean isMyTurn = (isWhiteTurn && playerColor == PieceColor.WHITE) || (!isWhiteTurn && playerColor == PieceColor.BLACK);
                    if (currentMode == GameMode.PVP) isMyTurn = true; // Di PvP selalu bisa nge-klik (sesuai giliran)

                    if (isMyTurn && ((isWhiteTurn && p != null && p.color == PieceColor.WHITE) || (!isWhiteTurn && p != null && p.color == PieceColor.BLACK))) {
                        selectedRow = r; selectedCol = c;
                        draggedPiece = p;
                        dragStartR = r; dragStartC = c;
                        dragX = e.getX(); dragY = e.getY();
                        clearPremove();
                    } else if (currentMode == GameMode.PVE && !isMyTurn && p != null && p.color == playerColor) {
                        // Premove drag logic
                        if (!hasPremove) {
                            premoveStartRow = r; premoveStartCol = c;
                            draggedPiece = p;
                            dragStartR = r; dragStartC = c;
                            dragX = e.getX(); dragY = e.getY();
                        }
                    }
                }
                panel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int sqW = panel.getWidth() / 8;
                int sqH = panel.getHeight() / 8;
                int r = boardR(Math.max(0, Math.min(panel.getHeight()-1, e.getY())), sqH);
                int c = boardC(Math.max(0, Math.min(panel.getWidth()-1, e.getX())), sqW);

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (arrowStartR != -1 && (arrowStartR != r || arrowStartC != c)) {
                        arrows.add(new Arrow(arrowStartR, arrowStartC, r, c));
                    }
                    arrowStartR = -1; currentArrowEndR = -1;
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    boolean isMyTurn = (isWhiteTurn && playerColor == PieceColor.WHITE) || (!isWhiteTurn && playerColor == PieceColor.BLACK);
                    if (currentMode == GameMode.PVP) isMyTurn = true;
                    
                    Piece activePieceColorCheck = (isWhiteTurn) ? new Piece(PieceColor.WHITE, PieceType.PAWN) : new Piece(PieceColor.BLACK, PieceType.PAWN);

                    if (draggedPiece != null) {
                        if (dragStartR != r || dragStartC != c) {
                            if (isMyTurn) {
                                if (isLegalMove(dragStartR, dragStartC, r, c)) executePlayerMove(dragStartR, dragStartC, r, c);
                                else { selectedRow = -1; selectedCol = -1; }
                            } else if (currentMode == GameMode.PVE) {
                                premoveEndRow = r; premoveEndCol = c; hasPremove = true;
                            }
                        }
                        draggedPiece = null;
                    } else {
                        if (isMyTurn) {
                            if (selectedRow != -1) {
                                if (isLegalMove(selectedRow, selectedCol, r, c)) {
                                    executePlayerMove(selectedRow, selectedCol, r, c);
                                } else {
                                    Piece p = boardState[r][c];
                                    if (p != null && p.color == activePieceColorCheck.color) {
                                        selectedRow = r; selectedCol = c;
                                    } else {
                                        selectedRow = -1; selectedCol = -1;
                                    }
                                }
                            }
                        } else if (currentMode == GameMode.PVE) {
                            if (hasPremove) clearPremove();
                        }
                    }
                }
                panel.repaint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int sqW = panel.getWidth() / 8;
                    int sqH = panel.getHeight() / 8;
                    currentArrowEndR = boardR(Math.max(0, Math.min(panel.getHeight()-1, e.getY())), sqH);
                    currentArrowEndC = boardC(Math.max(0, Math.min(panel.getWidth()-1, e.getX())), sqW);
                } else if (SwingUtilities.isLeftMouseButton(e) && draggedPiece != null) {
                    dragX = e.getX(); dragY = e.getY();
                }
                panel.repaint();
            }
        };

        panel.addMouseListener(ma);
        panel.addMouseMotionListener(ma);
        return panel;
    }

    private void drawArrow(Graphics2D g2, int drawSr, int drawSc, int drawEr, int drawEc, Color color) {
        int sqW = boardPanel.getWidth() / 8;
        int sqH = boardPanel.getHeight() / 8;
        int x1 = drawSc * sqW + sqW / 2;
        int y1 = drawSr * sqH + sqH / 2;
        int x2 = drawEc * sqW + sqW / 2;
        int y2 = drawEr * sqH + sqH / 2;

        g2.setColor(color);
        g2.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x1, y1, x2, y2);

        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowLen = 20;
        int ax1 = (int) (x2 - arrowLen * Math.cos(angle - Math.PI / 6));
        int ay1 = (int) (y2 - arrowLen * Math.sin(angle - Math.PI / 6));
        int ax2 = (int) (x2 - arrowLen * Math.cos(angle + Math.PI / 6));
        int ay2 = (int) (y2 - arrowLen * Math.sin(angle + Math.PI / 6));
        g2.drawLine(x2, y2, ax1, ay1);
        g2.drawLine(x2, y2, ax2, ay2);
    }

    private void setupPieces() {
        for (int i = 0; i < 8; i++) for (int j = 0; j < 8; j++) boardState[i][j] = null;

        boardState[0][0] = new Piece(PieceColor.BLACK, PieceType.ROOK); boardState[0][1] = new Piece(PieceColor.BLACK, PieceType.KNIGHT);
        boardState[0][2] = new Piece(PieceColor.BLACK, PieceType.BISHOP); boardState[0][3] = new Piece(PieceColor.BLACK, PieceType.QUEEN);
        boardState[0][4] = new Piece(PieceColor.BLACK, PieceType.KING); boardState[0][5] = new Piece(PieceColor.BLACK, PieceType.BISHOP);
        boardState[0][6] = new Piece(PieceColor.BLACK, PieceType.KNIGHT); boardState[0][7] = new Piece(PieceColor.BLACK, PieceType.ROOK);
        for (int i = 0; i < 8; i++) boardState[1][i] = new Piece(PieceColor.BLACK, PieceType.PAWN);

        boardState[7][0] = new Piece(PieceColor.WHITE, PieceType.ROOK); boardState[7][1] = new Piece(PieceColor.WHITE, PieceType.KNIGHT);
        boardState[7][2] = new Piece(PieceColor.WHITE, PieceType.BISHOP); boardState[7][3] = new Piece(PieceColor.WHITE, PieceType.QUEEN);
        boardState[7][4] = new Piece(PieceColor.WHITE, PieceType.KING); boardState[7][5] = new Piece(PieceColor.WHITE, PieceType.BISHOP);
        boardState[7][6] = new Piece(PieceColor.WHITE, PieceType.KNIGHT); boardState[7][7] = new Piece(PieceColor.WHITE, PieceType.ROOK);
        for (int i = 0; i < 8; i++) boardState[6][i] = new Piece(PieceColor.WHITE, PieceType.PAWN);
    }

    private void updateBoardUI() {
        if (boardPanel != null) boardPanel.repaint();
    }

    private void clearPremove() {
        premoveStartRow = -1; premoveStartCol = -1;
        premoveEndRow = -1; premoveEndCol = -1;
        hasPremove = false;
    }

    private void executePlayerMove(int startR, int startC, int endR, int endC) {
        Piece movingP = boardState[startR][startC];
        boolean isCapture = boardState[endR][endC] != null;
        
        playSound(isCapture ? "capture" : "move");

        String moveStr = movingP.getSymbol() + " " + getChessNotation(startR, startC) + (isCapture ? " x " : " -> ") + getChessNotation(endR, endC);
        moveHistoryArea.append(moveCount + ". " + moveStr + "\n");
        if(isWhiteTurn) moveCount++;

        movingP.hasMoved = true;
        boardState[endR][endC] = boardState[startR][startC];
        boardState[startR][startC] = null;

        if (movingP.type == PieceType.KING && Math.abs(endC - startC) == 2) {
            int rookStartC = (endC == 6) ? 7 : 0;
            int rookEndC = (endC == 6) ? 5 : 3;
            Piece rook = boardState[startR][rookStartC];
            if(rook != null) { rook.hasMoved = true; boardState[startR][rookEndC] = rook; boardState[startR][rookStartC] = null; }
        }

        if (movingP.type == PieceType.PAWN && (endR == 0 || endR == 7)) boardState[endR][endC].type = PieceType.QUEEN;
        
        isWhiteTurn = !isWhiteTurn;
        selectedRow = -1; selectedCol = -1;
        clearPremove();
        updateBoardUI();

        PieceColor currentTurnColor = isWhiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
        if (!hasAnyLegalMove(currentTurnColor)) {
            if (gameTimer != null) gameTimer.stop();
            if (isKingInCheck(currentTurnColor)) showPostGame("Skakmat! " + (isWhiteTurn ? "Hitam" : "Putih") + " Menang.", !isWhiteTurn);
            else showPostGame("Seri! (Pat)", null);
            return;
        }

        if (isKingInCheck(currentTurnColor)) playSound("check");

        if (currentMode == GameMode.PVE) {
            PieceColor botColor = (playerColor == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
            if ((isWhiteTurn && botColor == PieceColor.WHITE) || (!isWhiteTurn && botColor == PieceColor.BLACK)) {
                statusLabel.setText("Giliran: Bot sedang berpikir...");
                Timer botTimer = new Timer(500, e -> makeBotMove());
                botTimer.setRepeats(false); botTimer.start();
            } else {
                statusLabel.setText("Giliran: Kamu");
            }
        } else {
            statusLabel.setText("Giliran: " + (isWhiteTurn ? "Putih" : "Hitam"));
        }
    }

    private boolean isValidMove(int startR, int startC, int endR, int endC) {
        Piece movingPiece = boardState[startR][startC];
        Piece targetPiece = boardState[endR][endC];
        if (targetPiece != null && targetPiece.color == movingPiece.color) return false;

        int dr = Math.abs(endR - startR); int dc = Math.abs(endC - startC);
        
        switch (movingPiece.type) {
            case PAWN:
                int dir = (movingPiece.color == PieceColor.WHITE) ? -1 : 1;
                if (dc == 0 && endR == startR + dir && targetPiece == null) return true;
                if (dc == 0 && targetPiece == null && boardState[startR + dir][startC] == null) {
                    if ((movingPiece.color == PieceColor.WHITE && startR == 6 && endR == 4) || (movingPiece.color == PieceColor.BLACK && startR == 1 && endR == 3)) return true;
                }
                if (dc == 1 && endR == startR + dir && targetPiece != null) return true;
                return false;
            case ROOK: if (dr != 0 && dc != 0) return false; return isPathClear(startR, startC, endR, endC);
            case KNIGHT: return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
            case BISHOP: if (dr != dc) return false; return isPathClear(startR, startC, endR, endC);
            case QUEEN: if (dr != 0 && dc != 0 && dr != dc) return false; return isPathClear(startR, startC, endR, endC);
            case KING:
                if (dr <= 1 && dc <= 1) return true;
                if (dr == 0 && dc == 2 && !movingPiece.hasMoved) {
                    int rookCol = (endC > startC) ? 7 : 0; int passCol = (endC > startC) ? 5 : 3;
                    Piece rook = boardState[startR][rookCol];
                    if (rook != null && rook.type == PieceType.ROOK && !rook.hasMoved) {
                        if (!isPathClear(startR, startC, startR, rookCol)) return false;
                        if (isKingInCheck(movingPiece.color)) return false;
                        PieceColor opponent = (movingPiece.color == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
                        if (isSquareAttacked(startR, passCol, opponent)) return false;
                        return true;
                    }
                }
                return false;
        }
        return false;
    }

    private boolean isPathClear(int startR, int startC, int endR, int endC) {
        int rStep = Integer.compare(endR, startR); int cStep = Integer.compare(endC, startC);
        int r = startR + rStep; int c = startC + cStep;
        while (r != endR || c != endC) {
            if (boardState[r][c] != null) return false;
            r += rStep; c += cStep;
        }
        return true;
    }

    private boolean isLegalMove(int startR, int startC, int endR, int endC) {
        if (!isValidMove(startR, startC, endR, endC)) return false;
        Piece movingPiece = boardState[startR][startC]; Piece targetPiece = boardState[endR][endC];
        boardState[endR][endC] = movingPiece; boardState[startR][startC] = null;
        boolean inCheck = isKingInCheck(movingPiece.color);
        boardState[startR][startC] = movingPiece; boardState[endR][endC] = targetPiece;
        return !inCheck;
    }

    private boolean isKingInCheck(PieceColor color) {
        int kingR = -1, kingC = -1;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = boardState[r][c];
                if (p != null && p.color == color && p.type == PieceType.KING) { kingR = r; kingC = c; break; }
            }
        }
        if (kingR == -1) return false;
        PieceColor opp = (color == PieceColor.WHITE) ? PieceColor.BLACK : PieceColor.WHITE;
        return isSquareAttacked(kingR, kingC, opp);
    }

    private boolean isSquareAttacked(int targetR, int targetC, PieceColor attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = boardState[r][c];
                if (p != null && p.color == attackerColor && isValidMove(r, c, targetR, targetC)) return true;
            }
        }
        return false;
    }

    private boolean hasAnyLegalMove(PieceColor color) {
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = boardState[r][c];
            if (p != null && p.color == color) {
                for (int tr = 0; tr < 8; tr++) for (int tc = 0; tc < 8; tc++) {
                    if (isLegalMove(r, c, tr, tc)) return true;
                }
            }
        }
        return false;
    }

    // --- EVALUASI PAPAN UNTUK BOT HARD ---
    private int evaluateBoard(PieceColor botColor) {
        int score = 0;
        PieceColor oppColor = botColor == PieceColor.WHITE ? PieceColor.BLACK : PieceColor.WHITE;
        for(int r=0; r<8; r++) {
            for(int c=0; c<8; c++) {
                Piece p = boardState[r][c];
                if(p != null) {
                    int val = 0;
                    switch(p.type) {
                        case PAWN: val = 10; break;
                        case KNIGHT: case BISHOP: val = 30; break;
                        case ROOK: val = 50; break;
                        case QUEEN: val = 90; break;
                        case KING: val = 900; break;
                    }
                    if (isSquareAttacked(r, c, p.color == botColor ? oppColor : botColor)) val -= (val / 2); // Hindari area serangan
                    if(p.color == botColor) score += val; else score -= val;
                }
            }
        }
        return score;
    }

    private void makeBotMove() {
        PieceColor botColor = isWhiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
        List<Move> possibleMoves = new ArrayList<>();
        List<Move> captureMoves = new ArrayList<>();

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = boardState[r][c];
                if (piece != null && piece.color == botColor) {
                    for (int tr = 0; tr < 8; tr++) {
                        for (int tc = 0; tc < 8; tc++) {
                            if (isLegalMove(r, c, tr, tc)) {
                                boolean isCapture = (boardState[tr][tc] != null);
                                Move move = new Move(r, c, tr, tc, isCapture);
                                possibleMoves.add(move);
                                if (isCapture) captureMoves.add(move);
                            }
                        }
                    }
                }
            }
        }

        if (!possibleMoves.isEmpty()) {
            Random rand = new Random();
            Move selectedMove = null;

            if (botDifficulty == BotDifficulty.EASY) {
                selectedMove = possibleMoves.get(rand.nextInt(possibleMoves.size()));
            } else if (botDifficulty == BotDifficulty.MEDIUM) {
                selectedMove = !captureMoves.isEmpty() ? captureMoves.get(rand.nextInt(captureMoves.size())) : possibleMoves.get(rand.nextInt(possibleMoves.size()));
            } else if (botDifficulty == BotDifficulty.HARD) {
                int bestScore = Integer.MIN_VALUE;
                for (Move m : possibleMoves) {
                    Piece captured = boardState[m.endR][m.endC];
                    boolean oldMoved = boardState[m.startR][m.startC].hasMoved;
                    
                    boardState[m.endR][m.endC] = boardState[m.startR][m.startC];
                    boardState[m.startR][m.startC] = null;
                    
                    int score = evaluateBoard(botColor);
                    
                    boardState[m.startR][m.startC] = boardState[m.endR][m.endC];
                    boardState[m.endR][m.endC] = captured;
                    boardState[m.startR][m.startC].hasMoved = oldMoved;
                    
                    if (score > bestScore || (score == bestScore && rand.nextBoolean())) {
                        bestScore = score; selectedMove = m;
                    }
                }
            }

            Piece movingP = boardState[selectedMove.startR][selectedMove.startC];
            playSound(selectedMove.isCapture ? "capture" : "move");
            
            String moveStr = movingP.getSymbol() + " " + getChessNotation(selectedMove.startR, selectedMove.startC) + 
                             (selectedMove.isCapture ? " x " : " -> ") + getChessNotation(selectedMove.endR, selectedMove.endC);
            moveHistoryArea.append((botColor == PieceColor.WHITE ? moveCount + ". Bot: " : "   Bot: ") + moveStr + "\n");
            if(botColor == PieceColor.BLACK) moveCount++;

            movingP.hasMoved = true;
            boardState[selectedMove.endR][selectedMove.endC] = boardState[selectedMove.startR][selectedMove.startC];
            boardState[selectedMove.startR][selectedMove.startC] = null;

            if (movingP.type == PieceType.KING && Math.abs(selectedMove.endC - selectedMove.startC) == 2) {
                int rookStartC = (selectedMove.endC == 6) ? 7 : 0; int rookEndC = (selectedMove.endC == 6) ? 5 : 3;
                Piece rook = boardState[selectedMove.startR][rookStartC];
                if(rook != null) { rook.hasMoved = true; boardState[selectedMove.startR][rookEndC] = rook; boardState[selectedMove.startR][rookStartC] = null; }
            }

            if (boardState[selectedMove.endR][selectedMove.endC].type == PieceType.PAWN && (selectedMove.endR == 7 || selectedMove.endR == 0)) {
                boardState[selectedMove.endR][selectedMove.endC].type = PieceType.QUEEN;
            }

            updateBoardUI();

            isWhiteTurn = !isWhiteTurn;
            PieceColor pColor = isWhiteTurn ? PieceColor.WHITE : PieceColor.BLACK;
            
            if (!hasAnyLegalMove(pColor)) {
                if (gameTimer != null) gameTimer.stop();
                if (isKingInCheck(pColor)) showPostGame("Skakmat! Bot " + (botColor == PieceColor.WHITE ? "Putih" : "Hitam") + " Menang.", false);
                else showPostGame("Seri! (Pat)", null);
                return;
            }

            if (isKingInCheck(pColor)) playSound("check");

            if (hasPremove) {
                if (boardState[premoveStartRow][premoveStartCol] != null && 
                    boardState[premoveStartRow][premoveStartCol].color == playerColor &&
                    isLegalMove(premoveStartRow, premoveStartCol, premoveEndRow, premoveEndCol)) {
                    
                    int prStartR = premoveStartRow, prStartC = premoveStartCol;
                    int prEndR = premoveEndRow, prEndC = premoveEndCol;
                    clearPremove();
                    
                    Timer premoveTimer = new Timer(200, e -> executePlayerMove(prStartR, prStartC, prEndR, prEndC));
                    premoveTimer.setRepeats(false); premoveTimer.start();
                    return; 
                } else clearPremove(); 
            }

            if (premoveStartRow != -1 && !hasPremove) {
                selectedRow = premoveStartRow; selectedCol = premoveStartCol; clearPremove();
            }

            statusLabel.setText("Giliran: Kamu");
            updateBoardUI();
        } else {
            if (gameTimer != null) gameTimer.stop();
            showPostGame("Pemain Menang! (Bot menyerah)", true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessGame game = new ChessGame();
            game.setLocationRelativeTo(null);
            game.setVisible(true);
        });
    }
}